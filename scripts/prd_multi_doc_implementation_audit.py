#!/usr/bin/env python3
from __future__ import annotations

import html
import re
import zipfile
from dataclasses import dataclass
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
DOCS = [
    ROOT / 'docs' / 'Matrimony_App_PRD.docx',
    ROOT / 'docs' / 'VivahApp_GapAnalysis_100_100.docx',
    ROOT / 'docs' / 'VivahApp_Ultimate_Blueprint.docx',
    ROOT / 'docs' / 'Matrimony_App_Complete_Documentation.docx',
]
OUT = ROOT / 'analysis' / '05-multi-doc-line-by-line-implementation-audit.md'

SOURCE_FILES = [
    p for p in (ROOT / 'backend').rglob('*.js')
] + [
    p for p in (ROOT / 'frontend').rglob('*.js')
] + [
    p for p in (ROOT / 'frontend').rglob('*.jsx')
] + [
    ROOT / 'README.md', ROOT / 'PRD_IMPLEMENTATION.md', ROOT / 'API_ROUTES.md'
]

FEATURE_RULES = {
    'otp_auth': {
        'label': 'OTP authentication',
        'line_patterns': [r'\botp\b', r'one[- ]time password', r'mobile otp'],
        'code_patterns': [r'otp', r'auth', r'login'],
        'targets': ['backend/src/routes/auth.routes.js', 'backend/src/controllers/auth.controller.js'],
    },
    'profile_onboarding': {
        'label': 'Onboarding/profile wizard',
        'line_patterns': [r'onboarding', r'profile wizard', r'profile creation', r'partner preferences'],
        'code_patterns': [r'onboarding', r'profile', r'step-1', r'step-2'],
        'targets': ['frontend/src/app/(onboarding)/step-1/page.js', 'backend/src/routes/profile.routes.js'],
    },
    'search_filters': {
        'label': 'Search and filter matching',
        'line_patterns': [r'search', r'filter', r'compatibility', r'matchmaking'],
        'code_patterns': [r'search', r'filter', r'match'],
        'targets': ['backend/src/routes/search.routes.js', 'frontend/src/app/(main)/search/page.js'],
    },
    'interest_shortlist': {
        'label': 'Interests/shortlist flow',
        'line_patterns': [r'interest', r'shortlist', r'mutual match', r'like each other'],
        'code_patterns': [r'interest', r'shortlist', r'mutual'],
        'targets': ['backend/src/routes/interaction.routes.js', 'backend/src/routes/shortlist.routes.js'],
    },
    'chat_realtime': {
        'label': 'Realtime chat',
        'line_patterns': [r'chat', r'message', r'realtime', r'presence'],
        'code_patterns': [r'socket', r'chat', r'message'],
        'targets': ['backend/src/realtime/socket.server.js', 'backend/src/routes/chat.routes.js', 'frontend/src/app/(main)/chat/page.js'],
    },
    'voice_video_calls': {
        'label': 'Voice/video calls',
        'line_patterns': [r'voice call', r'video call', r'webrtc', r'masked call'],
        'code_patterns': [r'call', r'voice', r'video'],
        'targets': ['backend/src/routes/call.routes.js', 'backend/src/services/call.service.js'],
    },
    'verification_safety': {
        'label': 'Verification/safety/reporting',
        'line_patterns': [r'verification', r'block', r'report', r'safety', r'privacy'],
        'code_patterns': [r'verification', r'safety', r'report', r'block', r'privacy'],
        'targets': ['backend/src/routes/verification.routes.js', 'backend/src/services/safety.service.js'],
    },
    'subscription_payments': {
        'label': 'Subscriptions and payments',
        'line_patterns': [r'subscription', r'premium', r'payment', r'razorpay', r'plan'],
        'code_patterns': [r'subscription', r'payment', r'plan', r'razorpay'],
        'targets': ['backend/src/routes/subscription.routes.js', 'backend/src/routes/payment.routes.js'],
    },
    'notifications': {
        'label': 'Notifications',
        'line_patterns': [r'notification', r'push'],
        'code_patterns': [r'notification', r'push'],
        'targets': ['backend/src/routes/notification.routes.js', 'backend/src/services/notification.service.js'],
    },
    'admin_analytics': {
        'label': 'Admin and analytics',
        'line_patterns': [r'admin', r'moderation', r'analytics', r'kpi', r'dashboard'],
        'code_patterns': [r'admin', r'analytics', r'moderation', r'kpi'],
        'targets': ['backend/src/routes/admin.routes.js', 'backend/src/routes/analytics.routes.js', 'frontend/src/app/(main)/admin/page.js'],
    },
}

@dataclass
class FeatureStatus:
    key: str
    label: str
    matched_lines: int
    target_exists: int
    target_total: int
    code_hits: int
    status: str


def extract_docx_lines(path: Path) -> list[str]:
    with zipfile.ZipFile(path) as zf:
        xml = zf.read('word/document.xml').decode('utf-8', errors='ignore')
    chunks = re.findall(r'<w:t[^>]*>(.*?)</w:t>', xml)
    lines = []
    for raw in chunks:
        txt = html.unescape(raw)
        txt = re.sub(r'<[^>]+>', ' ', txt)
        txt = re.sub(r'\s+', ' ', txt).strip()
        if not txt:
            continue
        if txt.startswith('w:') or '<w:' in txt:
            continue
        if len(txt) < 4:
            continue
        if re.fullmatch(r'[\d\W_]+', txt):
            continue
        lines.append(txt)
    out = []
    seen = set()
    for ln in lines:
        key = ln.lower()
        if key in seen:
            continue
        seen.add(key)
        out.append(ln)
    return out


def load_code_text() -> str:
    chunks = []
    for path in SOURCE_FILES:
        if path.exists() and path.is_file():
            try:
                chunks.append(path.read_text(encoding='utf-8', errors='ignore').lower())
            except Exception:
                pass
    return '\n'.join(chunks)


def detect_feature_status(all_lines: list[str], code_text: str) -> list[FeatureStatus]:
    statuses: list[FeatureStatus] = []
    for key, cfg in FEATURE_RULES.items():
        m = 0
        for line in all_lines:
            ll = line.lower()
            if any(re.search(p, ll) for p in cfg['line_patterns']):
                m += 1

        target_exists = sum(1 for t in cfg['targets'] if (ROOT / t).exists())
        code_hits = sum(1 for p in cfg['code_patterns'] if re.search(p, code_text))

        if m == 0:
            status = 'not-requested-in-lines'
        elif target_exists == len(cfg['targets']) and code_hits >= max(1, len(cfg['code_patterns']) // 2):
            status = 'implemented'
        elif target_exists > 0:
            status = 'partially-implemented'
        else:
            status = 'missing'

        statuses.append(FeatureStatus(key, cfg['label'], m, target_exists, len(cfg['targets']), code_hits, status))
    return statuses


def classify_line(line: str) -> str:
    ll = line.lower()
    for key, cfg in FEATURE_RULES.items():
        if any(re.search(p, ll) for p in cfg['line_patterns']):
            return cfg['label']
    return 'General / strategy / context'


def write_report(per_doc: dict[str, list[str]], statuses: list[FeatureStatus]) -> None:
    all_lines = [ln for lines in per_doc.values() for ln in lines]

    md: list[str] = []
    md.append('# Multi-Doc Line-by-Line Analysis + Implementation Audit')
    md.append('')
    md.append('Documents covered:')
    for d in per_doc:
        md.append(f'- `{d}`')
    md.append('')
    md.append('## Implemented Features (from current codebase)')
    md.append('')
    md.append('| Feature | Status | Evidence targets present | Requirement lines matched | Code-pattern hits |')
    md.append('|---|---|---:|---:|---:|')
    for s in statuses:
        md.append(f'| {s.label} | **{s.status}** | {s.target_exists}/{s.target_total} | {s.matched_lines} | {s.code_hits} |')
    md.append('')

    implemented = [s for s in statuses if s.status == 'implemented']
    partial = [s for s in statuses if s.status == 'partially-implemented']
    missing = [s for s in statuses if s.status == 'missing']

    md.append('### Implemented feature list')
    md.append('')
    if implemented:
        for s in implemented:
            md.append(f'- {s.label}')
    else:
        md.append('- None detected as fully implemented by heuristic.')

    md.append('')
    md.append('### Partially implemented / follow-up')
    md.append('')
    for s in partial:
        md.append(f'- {s.label}')

    md.append('')
    md.append('### Missing')
    md.append('')
    for s in missing:
        md.append(f'- {s.label}')

    md.append('')
    md.append('## Line-by-line extraction and mapping')
    md.append('')
    for doc_name, lines in per_doc.items():
        md.append(f'### {doc_name}')
        md.append('')
        md.append('| # | Extracted line | Classified feature | Current status |')
        md.append('|---:|---|---|---|')
        for i, ln in enumerate(lines, 1):
            feat = classify_line(ln)
            st = next((s.status for s in statuses if s.label == feat), 'n/a')
            safe = ln.replace('|', '\\|')
            md.append(f"| {i} | {safe} | {feat} | {st} |")
        md.append('')

    md.append('## Implementation order')
    md.append('')
    ordered = sorted([s for s in statuses if s.matched_lines > 0], key=lambda x: (x.status != 'missing', -x.matched_lines))
    for i, s in enumerate(ordered, 1):
        md.append(f'{i}. {s.label} — {s.status} ({s.matched_lines} requirement lines)')

    OUT.write_text('\n'.join(md), encoding='utf-8')


def main() -> None:
    per_doc: dict[str, list[str]] = {}
    for p in DOCS:
        per_doc[p.name] = extract_docx_lines(p)

    all_lines = [ln for v in per_doc.values() for ln in v]
    code_text = load_code_text()
    statuses = detect_feature_status(all_lines, code_text)
    write_report(per_doc, statuses)

    print(f'Wrote {OUT.relative_to(ROOT)}')
    print('Detected implemented features:')
    for s in statuses:
        if s.status == 'implemented':
            print(f'- {s.label}')


if __name__ == '__main__':
    main()
