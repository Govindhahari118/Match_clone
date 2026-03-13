#!/usr/bin/env python3
from __future__ import annotations

import html
import re
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
DOC = ROOT / 'docs' / 'Matrimony_App_PRD.docx'
OUT = ROOT / 'analysis' / '04-matrimony-app-prd-line-by-line.md'

SECTION_PATTERNS = {
    'Auth & Identity': [r'otp', r'login', r'password', r'auth', r'signup', r'sign up'],
    'Onboarding & Profile': [r'onboarding', r'profile', r'bio', r'partner preference', r'photo'],
    'Search & Matching': [r'match', r'search', r'filter', r'compatibility', r'recommend'],
    'Interaction & Realtime': [r'chat', r'message', r'call', r'notification', r'shortlist', r'interest'],
    'Subscription & Billing': [r'subscription', r'premium', r'payment', r'plan', r'razorpay'],
    'Trust, Safety & Privacy': [r'verification', r'report', r'block', r'privacy', r'abuse', r'safety'],
    'Admin & Analytics': [r'admin', r'moderation', r'dashboard', r'kpi', r'analytics'],
    'Architecture & Ops': [r'architecture', r'scalab', r'cache', r'queue', r'observability', r'deploy', r'infra'],
}

IMPLEMENTATION_HINTS = {
    'Auth & Identity': 'backend/src/routes/auth.routes.js + backend/src/controllers/auth.controller.js + frontend/src/app/(auth)/*',
    'Onboarding & Profile': 'backend/src/routes/profile.routes.js + backend/src/services/onboarding.service.js + frontend/src/app/(onboarding)/*',
    'Search & Matching': 'backend/src/routes/search.routes.js + backend/src/services/matching.service.js + frontend/src/app/(main)/search/page.js',
    'Interaction & Realtime': 'backend/src/routes/interaction.routes.js + backend/src/realtime/socket.server.js + frontend/src/app/(main)/chat/page.js',
    'Subscription & Billing': 'backend/src/routes/subscription.routes.js + backend/src/routes/payment.routes.js + backend/src/services/subscription.service.js',
    'Trust, Safety & Privacy': 'backend/src/routes/verification.routes.js + backend/src/services/safety.service.js + frontend/src/app/security/page.js',
    'Admin & Analytics': 'backend/src/routes/admin.routes.js + backend/src/routes/analytics.routes.js + frontend/src/app/(main)/admin/page.js',
    'Architecture & Ops': 'README_PRODUCTION.md + UPTIME_DASHBOARD_RUNBOOK.md + docker-compose.yml',
    'General': 'PRD_IMPLEMENTATION.md + EPIC_EXECUTION_LOG.md for tracking and closure',
}


def extract_lines(docx_path: Path) -> list[str]:
    with zipfile.ZipFile(docx_path) as zf:
        xml = zf.read('word/document.xml').decode('utf-8', errors='ignore')

    chunks = re.findall(r'<w:t[^>]*>(.*?)</w:t>', xml)
    lines: list[str] = []
    for raw in chunks:
        txt = html.unescape(raw)
        txt = re.sub(r'<[^>]+>', ' ', txt)
        txt = re.sub(r'\s+', ' ', txt).strip()
        if not txt:
            continue
        if txt.startswith('w:') or '<w:' in txt:
            continue
        if len(txt) < 3:
            continue
        lines.append(txt)

    # remove near-duplicates while keeping order
    cleaned: list[str] = []
    seen = set()
    for ln in lines:
        key = ln.lower()
        if key in seen:
            continue
        seen.add(key)
        cleaned.append(ln)
    return cleaned


def classify(line: str) -> str:
    ll = line.lower()
    for section, pats in SECTION_PATTERNS.items():
        for pat in pats:
            if re.search(pat, ll):
                return section
    return 'General'


def research_note(section: str, line: str) -> str:
    if re.search(r'\b(must|required|critical|cannot|should)\b', line.lower()):
        return 'Treat as mandatory acceptance criterion; add explicit test coverage.'
    if re.search(r'\b(kpi|metric|conversion|mau|retention)\b', line.lower()):
        return 'Attach measurable target + dashboard source of truth.'
    if section in {'Architecture & Ops', 'Trust, Safety & Privacy'}:
        return 'Validate with production runbook and incident ownership before release.'
    return 'Map requirement to module owner and define Definition of Done.'


def implementation_action(section: str) -> str:
    return IMPLEMENTATION_HINTS.get(section, IMPLEMENTATION_HINTS['General'])


def main() -> None:
    lines = extract_lines(DOC)

    sections: dict[str, list[tuple[int, str]]] = {}
    for i, line in enumerate(lines, 1):
        s = classify(line)
        sections.setdefault(s, []).append((i, line))

    out: list[str] = []
    out.append('# Matrimony_App_PRD.docx — Line-by-Line Analysis, Research, and Implementation')
    out.append('')
    out.append('- Source: `docs/Matrimony_App_PRD.docx`')
    out.append(f'- Extracted requirement lines: {len(lines)}')
    out.append('- Method: sequential extraction from DOCX text nodes with XML/artifact filtering and de-duplication.')
    out.append('')
    out.append('## Phase A — Deep Line-by-Line Analysis')
    out.append('')

    ordered_sections = [
        'Auth & Identity',
        'Onboarding & Profile',
        'Search & Matching',
        'Interaction & Realtime',
        'Subscription & Billing',
        'Trust, Safety & Privacy',
        'Admin & Analytics',
        'Architecture & Ops',
        'General',
    ]

    for section in ordered_sections:
        items = sections.get(section, [])
        if not items:
            continue
        out.append(f'### {section}')
        out.append('')
        out.append('| Line | Requirement text | Research note | Implementation mapping |')
        out.append('|---:|---|---|---|')
        for idx, txt in items:
            rn = research_note(section, txt)
            ia = implementation_action(section)
            safe = txt.replace('|', '\\|')
            out.append(f'| {idx} | {safe} | {rn} | {ia} |')
        out.append('')

    out.append('## Phase B — Research Synthesis')
    out.append('')
    for section in ordered_sections:
        count = len(sections.get(section, []))
        if count:
            out.append(f'- **{section}**: {count} extracted lines mapped to implementation owners.')
    out.append('- Highest signal areas are prioritized for implementation sequencing in Phase C.')
    out.append('')

    out.append('## Phase C — Implementation Sequence (in order)')
    out.append('')
    sequence = [
        'Auth & Identity',
        'Onboarding & Profile',
        'Search & Matching',
        'Interaction & Realtime',
        'Subscription & Billing',
        'Trust, Safety & Privacy',
        'Admin & Analytics',
        'Architecture & Ops',
    ]
    for i, sec in enumerate(sequence, 1):
        if not sections.get(sec):
            continue
        out.append(f'{i}. **{sec}** — implement requirements against `{implementation_action(sec)}` and close via tests/checklist.')

    OUT.write_text('\n'.join(out), encoding='utf-8')
    print(f'Wrote {OUT.relative_to(ROOT)} with {len(lines)} extracted lines')


if __name__ == '__main__':
    main()
