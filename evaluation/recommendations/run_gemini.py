"""Step 2: ask Gemini for recommendations, exactly as the platform does.

The prompts are rebuilt from the production code rather than paraphrased: the
system prompt is read out of RecommendationPromptBuilder.java, and the user
prompt follows RecommendationPromptBuilder.userPrompt line for line. The
model list, temperature and token limit come from application.yaml, and the
same fallback rule applies (next model on 404, 429 or 5xx). The response is
filtered as AiRecommendationService filters it: items without a valid jobId or
a reason are dropped, ids outside the pool are counted as hallucinations and
dropped, and at most returnSize items are kept.

Each profile is run RUNS times, because the temperature is not zero and a
single run would say nothing about how stable the recommendations are.

Output: data/gemini_runs.json (resumable; --force starts again)
"""
from __future__ import annotations

import json
import re
import sys
import time
import urllib.error
import urllib.request
import uuid

from common import BACKEND, DATA, gemini_settings, load_env, read_json, write_json

RUNS = 3
PAUSE_SECONDS = 4
EXPLANATION_LANGUAGE = "French"   # the recruiters read the explanations in French
JOB_DESC_MAX_CHARS = 600
API = "https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent"


def system_prompt(return_size: int) -> str:
    src = (BACKEND / "src/main/java/com/hostdesign24/jobportal/ai/service/"
           "RecommendationPromptBuilder.java").read_text(encoding="utf-8")
    block = re.search(r'return """\n(.*?)""".formatted', src, re.S).group(1)
    text = re.sub(r"^ {16}", "", block, flags=re.M)
    # formatted(returnSize, explanationLanguage(locale), returnSize)
    for value in (str(return_size), EXPLANATION_LANGUAGE, str(return_size)):
        text = re.sub(r"%[ds]", value, text, count=1)
    return text


def salary(offer: dict) -> str | None:
    lo, hi = offer["salaryMin"] or None, offer["salaryMax"] or None
    if lo is None and hi is None:
        return None
    if lo and hi:
        band = lo if lo == hi else f"{lo}-{hi}"
    else:
        band = f"from {lo}" if lo else f"up to {hi}"
    return f"{band} {offer['salaryCurrency'] or 'XAF'}"


def user_prompt(profile: dict, pool: list[dict]) -> str:
    lines = ["CANDIDATE PROFILE", "-----------------",
             "Skills: " + (", ".join(profile["skills"]) or "(none provided)"),
             "Spoken languages: " + (", ".join(profile["spokenLanguages"]) or "(none provided)")]
    if profile.get("yearsOfExperience") is not None:
        lines.append(f"Years of experience: {profile['yearsOfExperience']}")
    if profile.get("employmentType"):
        lines.append(f"Preferred employment type: {profile['employmentType']}")
    if profile.get("workAuthorization"):
        lines.append(f"Work authorization: {profile['workAuthorization']}")
    if profile.get("preferredRegion"):
        lines.append(f"Preferred region: {profile['preferredRegion']}")
    if profile.get("targetRole"):
        lines.append(f"Target role: {profile['targetRole']}")

    lines += ["", f"CANDIDATE JOBS ({len(pool)})", "-----------------"]
    for o in pool:
        lines.append(f"- jobId: {o['id']}")
        lines.append(f"  title: {o['title']}")
        lines.append(f"  company: {o['company']}")
        if o["industry"]:
            lines.append(f"  industry: {o['industry']}")
        lines.append(f"  location: {', '.join([o['city'], o['region'], o['country']])}")
        for key, label in (("type", "type"), ("site", "site"), ("language", "language")):
            if o[key]:
                lines.append(f"  {label}: {o[key]}")
        band = salary(o)
        if band:
            lines.append(f"  salary: {band}")
        if o["description"].strip():
            desc = o["description"]
            if len(desc) > JOB_DESC_MAX_CHARS:
                desc = desc[:JOB_DESC_MAX_CHARS] + "…"
            lines.append(f"  description: {desc}")
        lines.append("")
    return "\n".join(lines) + "\n"


def call(env, settings, system: str, user: str) -> dict:
    body = json.dumps({
        "systemInstruction": {"parts": [{"text": system}]},
        "contents": [{"role": "user", "parts": [{"text": user}]}],
        "generationConfig": {"temperature": settings["temperature"],
                             "maxOutputTokens": settings["maxTokens"],
                             "responseMimeType": "application/json"},
    }).encode()
    failures = []
    for model in settings["models"]:
        req = urllib.request.Request(API.format(model=model), data=body, method="POST", headers={
            "Content-Type": "application/json", "x-goog-api-key": env["GEMINI_API_KEY"]})
        started = time.time()
        try:
            with urllib.request.urlopen(req, timeout=120) as r:
                data = json.load(r)
        except urllib.error.HTTPError as e:
            failures.append(f"{model}: HTTP {e.code}")
            if e.code == 404 or e.code == 429 or e.code >= 500:
                continue
            raise SystemExit(f"Gemini refused the request ({model}: HTTP {e.code}); stopping")
        except OSError as e:
            failures.append(f"{model}: {type(e).__name__}")
            continue
        cand = (data.get("candidates") or [{}])[0]
        usage = data.get("usageMetadata", {})
        return {
            "model": model,
            "modelVersion": data.get("modelVersion"),
            "latencyMs": round((time.time() - started) * 1000),
            "finishReason": cand.get("finishReason"),
            "tokensIn": usage.get("promptTokenCount"),
            "tokensOut": usage.get("candidatesTokenCount"),
            "raw": "".join(p.get("text", "") for p in cand.get("content", {}).get("parts", [])
                           if not p.get("thought")),
            "unavailable": failures,
        }
    return {"model": None, "unavailable": failures, "raw": ""}


def parse(raw: str, pool_ids: dict[str, str], return_size: int) -> tuple[list[dict], int]:
    """Mirror of AiRecommendationService.parse plus the hallucination filter."""
    try:
        root = json.loads(raw)
    except ValueError:
        return [], 0
    items = root if isinstance(root, list) else root.get("recommendations") if isinstance(root, dict) else None
    if not isinstance(items, list):
        return [], 0
    picks, hallucinated = [], 0
    for item in items:
        if not isinstance(item, dict) or not item.get("jobId"):
            continue
        try:
            job_id = str(uuid.UUID(str(item["jobId"])))
        except ValueError:
            continue
        reason = str(item.get("reason") or "").strip()
        if not reason:
            continue
        if job_id not in pool_ids:
            hallucinated += 1
            continue
        try:
            score = float(item.get("score") or 0.0)
        except (TypeError, ValueError):
            score = 0.0
        picks.append({"code": pool_ids[job_id], "jobId": job_id,
                      "score": min(1.0, max(0.0, score)), "reason": reason})
        if len(picks) >= return_size:
            break
    return picks, hallucinated


def main() -> None:
    force = "--force" in sys.argv
    env, settings = load_env(), gemini_settings()
    evalset = read_json(DATA / "evaluation_set.json")
    offers = {o["code"]: o for o in evalset["offers"]}
    out_path = DATA / "gemini_runs.json"
    out = {} if force or not out_path.exists() else read_json(out_path)
    out.setdefault("settings", {**settings, "runs": RUNS, "explanationLanguage": EXPLANATION_LANGUAGE})
    out.setdefault("runs", {})

    system = system_prompt(settings["returnSize"])
    print("models, in order:", " -> ".join(settings["models"]))
    for profile in evalset["profiles"]:
        code = profile["code"]
        done = out["runs"].get(code, [])
        if len(done) >= RUNS:
            print(f"  {code}: already has {len(done)} runs, skipped")
            continue
        pool = [offers[c] for c in evalset["pools"][code]]
        pool_ids = {o["id"]: o["code"] for o in pool}
        user = user_prompt(profile, pool)
        while len(done) < RUNS:
            result = call(env, settings, system, user)
            picks, hallucinated = parse(result["raw"], pool_ids, settings["returnSize"])
            result.update({"picks": picks, "hallucinated": hallucinated})
            done.append(result)
            out["runs"][code] = done
            write_json(out_path, out)
            print(f"  {code} run {len(done)}: {result['model'] or 'NO MODEL AVAILABLE'} "
                  f"{result.get('latencyMs', '-')}ms -> {[p['code'] for p in picks]}"
                  + (f" ({hallucinated} hallucinated)" if hallucinated else "")
                  + (f" [skipped: {', '.join(result['unavailable'])}]" if result["unavailable"] else ""))
            time.sleep(PAUSE_SECONDS)


if __name__ == "__main__":
    main()
