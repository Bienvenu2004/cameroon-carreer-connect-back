"""Helpers shared by the recommendation evaluation scripts.

Everything is read from the backend's own configuration (.env and
application.yaml), so the experiment runs against the same database, key and
model list as the application itself. Nothing secret is ever printed.
"""
from __future__ import annotations

import json
import os
import re
import subprocess
from pathlib import Path

HERE = Path(__file__).resolve().parent
BACKEND = HERE.parents[1]
DATA = HERE / "data"
FORMS = HERE / "forms"
RESPONSES = HERE / "responses"
RESULTS = HERE / "results"

PSQL = os.environ.get("PSQL", r"C:\Program Files\PostgreSQL\18\bin\psql.exe")


def load_env() -> dict[str, str]:
    env: dict[str, str] = {}
    for line in (BACKEND / ".env").read_text(encoding="utf-8").splitlines():
        m = re.match(r"^([A-Z_]+)=(.*)$", line.strip())
        if m:
            env[m.group(1)] = m.group(2).strip().strip('"').strip("'")
    return env


def database(env: dict[str, str]) -> tuple[str, str, str]:
    """Host, port and database name from DB_URL. Refuses anything but a local database."""
    m = re.match(r"jdbc:postgresql://([^/:?]+)(?::(\d+))?/([^?]+)", env.get("DB_URL", ""))
    if not m:
        raise SystemExit("DB_URL in .env is not a PostgreSQL JDBC URL")
    host, port, name = m.group(1), m.group(2) or "5432", m.group(3)
    if host not in ("localhost", "127.0.0.1"):
        raise SystemExit(f"refusing to read a non-local database ({host})")
    return host, port, name


def query(env: dict[str, str], sql: str) -> list[list[str]]:
    """Run a read-only query and return rows of strings (unit separator delimited)."""
    host, port, name = database(env)
    proc = subprocess.run(
        [PSQL, "-h", host, "-p", port, "-U", env["DB_USERNAME"], "-d", name,
         "-v", "ON_ERROR_STOP=1", "-At", "-F", "\x1f",
         "-c", "SET default_transaction_read_only = on;", "-c", sql],
        env={**os.environ, "PGPASSWORD": env["DB_PASSWORD"]},
        capture_output=True, text=True, encoding="utf-8", timeout=120,
    )
    if proc.returncode != 0:
        raise SystemExit(f"query failed: {proc.stderr.strip()[:400]}")
    lines = [l for l in proc.stdout.splitlines() if l and l != "SET"]
    return [l.split("\x1f") for l in lines]


def gemini_settings() -> dict:
    """Model list, temperature and token limit, read from application.yaml."""
    yaml = (BACKEND / "src/main/resources/application.yaml").read_text(encoding="utf-8")
    block = yaml[yaml.index("gemini:"):]
    model = re.search(r"^\s+model:\s*(\S+)", block, re.M).group(1)
    fallbacks_block = re.search(r"fallback-models:\s*\n((?:\s+-\s*\S+\s*\n)+)", block)
    fallbacks = re.findall(r"-\s*(\S+)", fallbacks_block.group(1)) if fallbacks_block else []
    return {
        "models": [model] + [f for f in fallbacks if f != model],
        "temperature": float(re.search(r"^\s+temperature:\s*([\d.]+)", block, re.M).group(1)),
        "maxTokens": int(re.search(r"^\s+max-tokens:\s*(\d+)", block, re.M).group(1)),
        "returnSize": int(re.search(r"return-size:\s*(\d+)", yaml).group(1)),
    }


def read_json(path: Path):
    return json.loads(path.read_text(encoding="utf-8"))


def write_json(path: Path, data) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
