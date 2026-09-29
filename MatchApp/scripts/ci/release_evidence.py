#!/usr/bin/env python3
"""Emit exact-SHA repository release evidence without fabricating external completion."""
from __future__ import annotations
import argparse, hashlib, pathlib, re

ROOT = pathlib.Path(__file__).resolve().parents[2]
def main() -> int:
    p=argparse.ArgumentParser(); p.add_argument("--sha", required=True); p.add_argument("--output", required=True, type=pathlib.Path); a=p.parse_args()
    gradle=(ROOT/"app/build.gradle.kts").read_text(encoding="utf-8")
    def grab(pattern, default="UNKNOWN"):
        m=re.search(pattern, gradle); return m.group(1) if m else default
    aabs=list((ROOT/"app/build/outputs/bundle/release").glob("*.aab"))
    artifact="EXTERNAL EVIDENCE REQUIRED — production Firebase/signing configuration not present in CI"
    checksum="N/A"
    if aabs:
        f=aabs[0]; artifact=str(f.relative_to(ROOT)); checksum=hashlib.sha256(f.read_bytes()).hexdigest()
    rows=[
      ("Git SHA",a.sha),("Version",grab(r'versionName\s*=\s*"([^"]+)"')),
      ("Version code",grab(r"versionCode\s*=\s*(\d+)")),("Application ID",grab(r'applicationId\s*=\s*"([^"]+)"')),
      ("AAB artifact",artifact),("AAB SHA-256",checksum),
      ("Repository release scan","PASS when this workflow step is green"),
      ("Unit tests","PASS only when Android unit-test step is green"),
      ("Lint","PASS only when Android lint step is green"),
      ("R8/release compile","PASS only when release bundle step is green"),
      ("Firebase rules","PASS only when firebase-rules job is green"),
      ("Functions","PASS only when functions job is green"),
      ("Room migrations","PASS only when room-migrations job is green"),
      ("Physical-device E2E","EXTERNAL EVIDENCE REQUIRED"),
      ("Play Billing licensed purchase","EXTERNAL EVIDENCE REQUIRED"),
      ("Play Integrity/App Check enforcement","EXTERNAL EVIDENCE REQUIRED"),
      ("App Links domain verification","EXTERNAL EVIDENCE REQUIRED"),
      ("Play Console/Data Safety/legal approval","EXTERNAL EVIDENCE REQUIRED"),
    ]
    a.output.parent.mkdir(parents=True,exist_ok=True)
    a.output.write_text("# Matree Release Evidence\n\nExact-SHA evidence; no historical run certifies a newer commit.\n\n"+"\n".join(f"- **{k}:** {v}" for k,v in rows)+"\n",encoding="utf-8")
    print(a.output)
    return 0
if __name__=="__main__": raise SystemExit(main())
