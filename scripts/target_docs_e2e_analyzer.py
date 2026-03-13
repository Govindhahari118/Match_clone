#!/usr/bin/env python3
from __future__ import annotations

import html
import re
import zlib
import zipfile
from dataclasses import dataclass
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
DOCS = [
    ROOT / 'docs' / 'TeluguMatrimony_PRD_v1.docx',
    ROOT / 'docs' / 'Matrimony_Platform_v3.1_Supplementary.pdf',
    ROOT / 'docs' / 'Matrimony_Platform_Build_Spec_v2.pdf',
    ROOT / 'docs' / 'Matrimony_Platform_v3_Complete.pdf',
    ROOT / 'docs' / 'TeluguMatrimony_Production_Architecture_v2.pdf',
]
OUT = ROOT / 'analysis' / '06-target-docs-e2e-analysis-and-implementation.md'

FEATURES = {
    'Auth & Account Security': {
        'patterns': [r'otp', r'auth', r'login', r'password', r'jwt', r'2fa'],
        'targets': ['backend/src/routes/auth.routes.js', 'backend/src/controllers/auth.controller.js', 'frontend/src/app/(auth)/login/page.js'],
    },
    'Onboarding & Profile': {
        'patterns': [r'onboarding', r'profile', r'biodata', r'partner preference', r'photo'],
        'targets': ['backend/src/routes/profile.routes.js', 'backend/src/services/onboarding.service.js', 'frontend/src/app/(onboarding)/step-1/page.js'],
    },
    'Search, Matching & Recommendations': {
        'patterns': [r'search', r'match', r'compatibility', r'recommend', r'filter', r'kundli', r'horoscope'],
        'targets': ['backend/src/routes/search.routes.js', 'backend/src/services/matching.service.js', 'frontend/src/app/(main)/matches/page.js'],
    },
    'Interactions, Chat & Calls': {
        'patterns': [r'interest', r'shortlist', r'chat', r'message', r'voice', r'video', r'call', r'realtime'],
        'targets': ['backend/src/routes/interaction.routes.js', 'backend/src/realtime/socket.server.js', 'backend/src/routes/call.routes.js', 'frontend/src/app/(main)/chat/page.js'],
    },
    'Subscriptions, Entitlements & Payments': {
        'patterns': [r'subscription', r'premium', r'plan', r'pricing', r'payment', r'razorpay', r'entitlement'],
        'targets': ['backend/src/routes/subscription.routes.js', 'backend/src/routes/payment.routes.js', 'backend/src/middleware/entitlement.middleware.js', 'frontend/src/app/(main)/pricing/page.js'],
    },
    'Verification, Safety & Privacy': {
        'patterns': [r'verification', r'ekyc', r'kyc', r'safety', r'privacy', r'block', r'report', r'moderation'],
        'targets': ['backend/src/routes/verification.routes.js', 'backend/src/services/safety.service.js', 'backend/src/services/privacy.service.js', 'frontend/src/app/security/page.js'],
    },
    'Notifications & Engagement': {
        'patterns': [r'notification', r'push', r'email', r'reminder'],
        'targets': ['backend/src/routes/notification.routes.js', 'backend/src/services/notification.service.js', 'frontend/src/app/(main)/notifications/page.js'],
    },
    'Admin, Analytics & Operations': {
        'patterns': [r'admin', r'analytics', r'kpi', r'dashboard', r'sla', r'rpo', r'rto', r'observability', r'uptime', r'deploy', r'architecture'],
        'targets': ['backend/src/routes/admin.routes.js', 'backend/src/routes/analytics.routes.js', 'UPTIME_DASHBOARD_RUNBOOK.md', 'README_PRODUCTION.md'],
    },
}

@dataclass
class FeatureState:
    name: str
    lines: int
    present_targets: int
    target_total: int
    status: str


def extract_docx_lines(path: Path) -> list[str]:
    with zipfile.ZipFile(path) as zf:
        xml = zf.read('word/document.xml').decode('utf-8', errors='ignore')
    chunks = re.findall(r'<w:t[^>]*>(.*?)</w:t>', xml)
    return clean_lines(chunks)


def extract_pdf_lines(path: Path) -> list[str]:
    data = path.read_bytes()
    out_chunks: list[str] = []

    # raw literal strings in file (fallback)
    raw = data.decode('latin-1', errors='ignore')
    out_chunks.extend(re.findall(r'\(([^\)]{4,})\)', raw))

    # flate streams decoding for ReportLab style PDFs
    for match in re.finditer(rb'stream\r?\n(.*?)\r?\nendstream', data, re.S):
        blob = match.group(1)
        try:
            dec = zlib.decompress(blob)
        except Exception:
            continue
        text = dec.decode('latin-1', errors='ignore')
        out_chunks.extend(re.findall(r'\(([^\)]{3,})\)', text))

    return clean_lines(out_chunks)


def clean_lines(chunks: list[str]) -> list[str]:
    lines: list[str] = []
    for c in chunks:
        t = html.unescape(c)
        t = re.sub(r'<[^>]+>', ' ', t)
        t = re.sub(r'\\[nrt]', ' ', t)
        t = re.sub(r'\s+', ' ', t).strip()
        if len(t) < 4:
            continue
        if t.startswith('w:') or '<w:' in t:
            continue
        if re.fullmatch(r'[\W\d_]+', t):
            continue
        lines.append(t)

    deduped: list[str] = []
    seen = set()
    for ln in lines:
        key = ln.lower()
        if key in seen:
            continue
        seen.add(key)
        deduped.append(ln)
    return deduped


def classify_feature(line: str) -> str:
    ll = line.lower()
    for name, cfg in FEATURES.items():
        if any(re.search(p, ll) for p in cfg['patterns']):
            return name
    return 'General / Business Context'


def feature_states(all_lines: list[str]) -> list[FeatureState]:
    states: list[FeatureState] = []
    for name, cfg in FEATURES.items():
        count = sum(1 for ln in all_lines if any(re.search(p, ln.lower()) for p in cfg['patterns']))
        present = sum(1 for t in cfg['targets'] if (ROOT / t).exists())
        if count == 0:
            status = 'not-requested'
        elif present == len(cfg['targets']):
            status = 'implemented-in-repo'
        elif present > 0:
            status = 'partial'
        else:
            status = 'missing'
        states.append(FeatureState(name, count, present, len(cfg['targets']), status))
    return states


def implementation_tasks(states: list[FeatureState]) -> list[str]:
    tasks: list[str] = []
    for s in states:
        if s.status == 'implemented-in-repo':
            tasks.append(f"{s.name}: harden with e2e and load tests; validate acceptance criteria against extracted lines.")
        elif s.status == 'partial':
            tasks.append(f"{s.name}: complete missing modules and wire route-controller-service coverage end-to-end.")
        elif s.status == 'missing':
            tasks.append(f"{s.name}: implement backend routes/services + frontend page flow + telemetry and runbook entry.")
    return tasks


def main() -> None:
    per_doc: dict[str, list[str]] = {}
    for path in DOCS:
        if path.suffix.lower() == '.docx':
            lines = extract_docx_lines(path)
            method = 'DOCX XML extraction'
        else:
            lines = extract_pdf_lines(path)
            method = 'PDF stream/literal extraction'
        per_doc[path.name] = [f"[method] {method}"] + lines

    all_lines = [ln for arr in per_doc.values() for ln in arr if not ln.startswith('[method]')]
    states = feature_states(all_lines)
    tasks = implementation_tasks(states)

    md: list[str] = []
    md.append('# Target Docs End-to-End Analysis and Implementation Status')
    md.append('')
    md.append('## Files analyzed line by line')
    for p in DOCS:
        md.append(f'- `{p.name}`')
    md.append('')

    md.append('## Feature implementation status (end-to-end repository audit)')
    md.append('')
    md.append('| Feature | Requirement lines matched | Repository targets present | Status |')
    md.append('|---|---:|---:|---|')
    for s in states:
        md.append(f'| {s.name} | {s.lines} | {s.present_targets}/{s.target_total} | **{s.status}** |')
    md.append('')

    md.append('## Implemented features list')
    md.append('')
    implemented = [s.name for s in states if s.status == 'implemented-in-repo']
    if implemented:
        for item in implemented:
            md.append(f'- {item}')
    else:
        md.append('- None')
    md.append('')

    md.append('## End-to-end implementation actions')
    md.append('')
    for i, t in enumerate(tasks, 1):
        md.append(f'{i}. {t}')
    md.append('')

    md.append('## Line-by-line mapping by file')
    md.append('')
    for doc, lines in per_doc.items():
        method = lines[0].replace('[method] ', '')
        body = lines[1:]
        md.append(f'### {doc}')
        md.append(f'- Extraction method: {method}')
        md.append(f'- Extracted lines: {len(body)}')
        md.append('')
        md.append('| # | Line | Feature bucket | Status |')
        md.append('|---:|---|---|---|')
        for i, ln in enumerate(body, 1):
            feat = classify_feature(ln)
            st = next((s.status for s in states if s.name == feat), 'n/a')
            safe = ln.replace('|', '\\|')
            md.append(f'| {i} | {safe} | {feat} | {st} |')
        md.append('')

    OUT.write_text('\n'.join(md), encoding='utf-8')
    print(f'Wrote {OUT.relative_to(ROOT)}')


if __name__ == '__main__':
    main()
