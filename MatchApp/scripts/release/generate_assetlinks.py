#!/usr/bin/env python3
"""Generate and validate Android Digital Asset Links for the Matree production app."""
from __future__ import annotations
import argparse, json, pathlib, re, sys

FINGERPRINT_RE = re.compile(r"^(?:[0-9A-F]{2}:){31}[0-9A-F]{2}$")
PACKAGE_RE = re.compile(r"^[A-Za-z][A-Za-z0-9_]*(?:\.[A-Za-z][A-Za-z0-9_]*){2,}$")

def main() -> int:
    p = argparse.ArgumentParser()
    p.add_argument("--package", required=True)
    p.add_argument("--sha256", required=True, action="append", help="Play signing SHA-256 fingerprint; repeat for rotations")
    p.add_argument("--output", required=True)
    args = p.parse_args()

    package = args.package.strip()
    if package == "com.match.app" or not PACKAGE_RE.fullmatch(package):
        print("assetlinks generation FAILED: final production package required", file=sys.stderr)
        return 1

    fingerprints = []
    for raw in args.sha256:
        value = raw.strip().upper()
        if not FINGERPRINT_RE.fullmatch(value):
            print(f"assetlinks generation FAILED: invalid SHA-256 fingerprint: {raw}", file=sys.stderr)
            return 1
        if value not in fingerprints:
            fingerprints.append(value)

    payload = [{
        "relation": ["delegate_permission/common.handle_all_urls"],
        "target": {
            "namespace": "android_app",
            "package_name": package,
            "sha256_cert_fingerprints": fingerprints,
        },
    }]
    out = pathlib.Path(args.output)
    out.parent.mkdir(parents=True, exist_ok=True)
    out.write_text(json.dumps(payload, indent=2) + "\n", encoding="utf-8")
    print(f"Wrote {out}")
    return 0

if __name__ == "__main__":
    raise SystemExit(main())
