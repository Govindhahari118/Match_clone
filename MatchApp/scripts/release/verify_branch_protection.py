#!/usr/bin/env python3
"""Verify production branch-protection requirements through the GitHub REST API.

Requires a token that can read repository administration settings.
"""
from __future__ import annotations
import argparse, json, os, sys, urllib.error, urllib.request

def get(url: str, token: str):
    req = urllib.request.Request(
        url,
        headers={
            "Accept": "application/vnd.github+json",
            "Authorization": f"Bearer {token}",
            "X-GitHub-Api-Version": "2022-11-28",
            "User-Agent": "matree-release-governance-check",
        },
    )
    with urllib.request.urlopen(req, timeout=20) as response:
        return json.load(response)

def main() -> int:
    p = argparse.ArgumentParser()
    p.add_argument("--repo", required=True, help="owner/name")
    p.add_argument("--branch", default="main")
    p.add_argument("--required-check", default="Production CI")
    args = p.parse_args()
    token = os.environ.get("GITHUB_TOKEN", "").strip()
    if not token:
        print("branch protection verification FAILED: GITHUB_TOKEN is required", file=sys.stderr)
        return 1

    url = f"https://api.github.com/repos/{args.repo}/branches/{args.branch}/protection"
    try:
        data = get(url, token)
    except urllib.error.HTTPError as exc:
        print(f"branch protection verification FAILED: GitHub returned HTTP {exc.code}", file=sys.stderr)
        return 1

    failures = []
    required = data.get("required_status_checks") or {}
    contexts = set(required.get("contexts") or [])
    checks = {x.get("context") for x in required.get("checks") or [] if isinstance(x, dict)}
    if args.required_check not in contexts | checks:
        failures.append(f"required status check missing: {args.required_check}")

    reviews = data.get("required_pull_request_reviews")
    if not reviews:
        failures.append("pull-request reviews are not required")
    elif int(reviews.get("required_approving_review_count") or 0) < 1:
        failures.append("at least one approving review is not required")

    if not data.get("required_conversation_resolution", {}).get("enabled", False):
        failures.append("conversation resolution is not required")
    if data.get("allow_force_pushes", {}).get("enabled", False):
        failures.append("force pushes are allowed")
    if data.get("allow_deletions", {}).get("enabled", False):
        failures.append("branch deletion is allowed")

    if failures:
        print("branch protection verification FAILED")
        for item in failures:
            print(f"- {item}")
        return 1

    print("branch protection verification passed")
    print(json.dumps({
        "repository": args.repo,
        "branch": args.branch,
        "requiredStatusCheck": args.required_check,
        "requiredApprovingReviews": reviews.get("required_approving_review_count"),
        "requiredConversationResolution": True,
        "forcePushesAllowed": False,
        "deletionsAllowed": False,
    }, indent=2))
    return 0

if __name__ == "__main__":
    raise SystemExit(main())
