#!/usr/bin/env python3
"""Publish built SenseESP jars to Modrinth for a given release tag."""

import glob
import json
import os
import sys
import urllib.error
import urllib.request
import uuid

PROJECT_ID = "qKVmawz6"

TARGET_MATRIX = {
    "1.21":    ["1.21", "1.21.1"],
    "1.21.2":  ["1.21.2", "1.21.3"],
    "1.21.4":  ["1.21.4"],
    "1.21.5":  ["1.21.5"],
    "1.21.6":  ["1.21.6", "1.21.7", "1.21.8"],
    "1.21.9":  ["1.21.9"],
    "1.21.10": ["1.21.10"],
    "1.21.11": ["1.21.11"],
    "26.1":    ["26.1", "26.1.1", "26.1.2"],
    "26.2":    ["26.2"],
    "26.3":    ["26.3"],
}

DEPENDENCIES = [
    {"project_id": "P7dR8mSH", "dependency_type": "required"},  # Fabric API
    {"project_id": "9s6osm5g", "dependency_type": "required"},  # Cloth Config
    {"project_id": "mOgUt4GM", "dependency_type": "optional"},  # Mod Menu
]


def existing_versions(token: str) -> set[str]:
    req = urllib.request.Request(
        f"https://api.modrinth.com/v2/project/{PROJECT_ID}/version",
        headers={"Authorization": token, "User-Agent": "sooqwess/SenseESP-publisher"},
    )
    with urllib.request.urlopen(req, timeout=30) as resp:
        return {v["version_number"] for v in json.loads(resp.read().decode())}


def upload_version(token: str, jar_path: str, version_number: str, game_versions: list[str], changelog: str) -> str:
    boundary = f"----SenseESPBoundary{uuid.uuid4().hex}"
    filename = os.path.basename(jar_path)

    payload = {
        "name": f"SenseESP {version_number}",
        "version_number": version_number,
        "changelog": changelog,
        "dependencies": DEPENDENCIES,
        "game_versions": game_versions,
        "version_type": "release",
        "loaders": ["fabric"],
        "featured": version_number.endswith("+26.3") or version_number.endswith("+1.21.11"),
        "project_id": PROJECT_ID,
        "file_parts": ["file"],
        "primary_file": "file",
    }

    with open(jar_path, "rb") as f:
        jar_bytes = f.read()

    body = bytearray()
    body.extend(f"--{boundary}\r\n".encode())
    body.extend(b'Content-Disposition: form-data; name="data"\r\n')
    body.extend(b"Content-Type: application/json\r\n\r\n")
    body.extend(json.dumps(payload).encode())
    body.extend(b"\r\n")

    body.extend(f"--{boundary}\r\n".encode())
    body.extend(f'Content-Disposition: form-data; name="file"; filename="{filename}"\r\n'.encode())
    body.extend(b"Content-Type: application/java-archive\r\n\r\n")
    body.extend(jar_bytes)
    body.extend(f"\r\n--{boundary}--\r\n".encode())

    req = urllib.request.Request(
        "https://api.modrinth.com/v2/version",
        data=bytes(body),
        method="POST",
        headers={
            "Authorization": token,
            "User-Agent": "sooqwess/SenseESP-publisher",
            "Content-Type": f"multipart/form-data; boundary={boundary}",
        },
    )
    with urllib.request.urlopen(req, timeout=60) as resp:
        res = json.loads(resp.read().decode())
        return res["id"]


def main() -> int:
    token = os.environ.get("MODRINTH_TOKEN", "").strip()
    if not token:
        print("MODRINTH_TOKEN is not set", file=sys.stderr)
        return 1

    jar_dir = sys.argv[1] if len(sys.argv) > 1 else "dist"
    changelog_path = sys.argv[2] if len(sys.argv) > 2 else "CHANGELOG.md"
    changelog_raw = open(changelog_path, encoding="utf-8").read().strip() if os.path.exists(changelog_path) else ""
    parts = changelog_raw.split("\n## ")
    if len(parts) > 1 and parts[1].strip():
        changelog = "## " + parts[1].strip()
    else:
        changelog = changelog_raw

    already = existing_versions(token)
    jars = sorted(glob.glob(os.path.join(jar_dir, "espmod-*.jar")))
    jars = [j for j in jars if not j.endswith("-sources.jar") and not j.endswith("-dev.jar")]
    if not jars:
        print(f"No jars found in {jar_dir}", file=sys.stderr)
        return 1

    for jar in jars:
        fname = os.path.basename(jar)
        ver_num = fname.removeprefix("espmod-").removesuffix(".jar")
        suffix = ver_num.split("+", 1)[1]
        game_vers = TARGET_MATRIX.get(suffix)
        if not game_vers:
            print(f"Skipping unknown target suffix: {fname}")
            continue
        if ver_num in already:
            print(f"Already published on Modrinth: {ver_num} (skipping)")
            continue
        vid = upload_version(token, jar, ver_num, game_vers, changelog)
        print(f"Published {ver_num} for MC {game_vers} -> id={vid}")

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
