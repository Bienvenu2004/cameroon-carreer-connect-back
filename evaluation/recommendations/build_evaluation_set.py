"""Step 1: build the evaluation set.

Reads the active offers of approved companies from the local database (the
same pool the recommendation service draws from) and gives each fictional
profile a pool of POOL_SIZE offers.

Pooling. A pool made only of random offers would, for most profiles, contain
nothing relevant, and every ranker would look equally good. Each pool
therefore first takes up to TARGET_SHARE offers from the profile's own
sector(s), then fills the rest at random from all remaining offers, which can
add further offers of the same sector. Being in the right sector does
not make an offer relevant (a data-engineering post is in telecoms, but it is
not a sales job), so the recruiters' ratings, not the sector, decide
relevance.

Output: data/evaluation_set.json
"""
from __future__ import annotations

import random

from common import DATA, HERE, load_env, query, read_json, write_json

POOL_SIZE = 10
TARGET_SHARE = 5
SEED = 20260917

# Test entries left in the development database, with descriptions of 11 to
# 15 characters. Neither a recruiter nor the model can judge them.
EXCLUDED_TITLES = {"Dish asher", "We need a prompt engineer", "Enseignant"}


def load_offers(env) -> list[dict]:
    rows = query(env, """
        select j.id, j.title, c.name, coalesce(c.industry, ''),
               coalesce(j.city, ''), coalesce(j.region, ''), coalesce(j.country, ''),
               coalesce(j.type, ''), coalesce(j.site, ''), coalesce(j.required_language, ''),
               coalesce(j.salary_min::text, ''), coalesce(j.salary_max::text, ''),
               coalesce(j.salary_currency, ''),
               coalesce(regexp_replace(j.description, E'[\\n\\r]+', ' ', 'g'), ''),
               to_char(j.created_at, 'YYYY-MM-DD"T"HH24:MI:SS.US')
          from jobs j join companies c on c.id = j.company_id
         where j.is_active and not j.deleted
           and c.status = 'APPROVED' and not c.deleted
         order by j.title
    """)
    keys = ["id", "title", "company", "industry", "city", "region", "country", "type", "site",
            "language", "salaryMin", "salaryMax", "salaryCurrency", "description", "createdAt"]
    offers = [dict(zip(keys, r)) for r in rows]
    offers = [o for o in offers if o["title"] not in EXCLUDED_TITLES]
    for i, o in enumerate(offers, 1):
        o["code"] = f"O{i:02d}"
    return offers


def build_pool(profile: dict, offers: list[dict], rng: random.Random) -> list[str]:
    sector = [o for o in offers if o["industry"] in profile["targetIndustries"]]
    rng.shuffle(sector)
    chosen = sector[:TARGET_SHARE]
    others = [o for o in offers if o not in chosen]
    rng.shuffle(others)
    chosen += others[:POOL_SIZE - len(chosen)]
    # The service orders its candidate pool newest first; the prompt keeps that order.
    chosen.sort(key=lambda o: o["createdAt"], reverse=True)
    return [o["code"] for o in chosen]


def main() -> None:
    env = load_env()
    offers = load_offers(env)
    profiles = read_json(HERE / "profiles.json")["profiles"]

    rng = random.Random(SEED)
    pools = {p["code"]: build_pool(p, offers, rng) for p in profiles}

    write_json(DATA / "evaluation_set.json", {
        "seed": SEED,
        "poolSize": POOL_SIZE,
        "excludedTitles": sorted(EXCLUDED_TITLES),
        "offers": offers,
        "profiles": profiles,
        "pools": pools,
    })

    by_code = {o["code"]: o for o in offers}
    print(f"{len(offers)} offers, {len(profiles)} profiles, {POOL_SIZE} offers per pool")
    for p in profiles:
        in_sector = sum(by_code[c]["industry"] in p["targetIndustries"] for c in pools[p["code"]])
        print(f"  {p['code']} {p['targetRole']:<32} pool: {in_sector} in sector, "
              f"{POOL_SIZE - in_sector} outside")


if __name__ == "__main__":
    main()
