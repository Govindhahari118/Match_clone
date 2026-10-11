import importlib.util
from pathlib import Path
import unittest

MODULE = Path(__file__).with_name("matree_feature_evidence_scan.py")
spec = importlib.util.spec_from_file_location("matree_feature_evidence_scan", MODULE)
assert spec and spec.loader
scan = importlib.util.module_from_spec(spec)
spec.loader.exec_module(scan)


def tracker(status="AUDIT_REQUIRED"):
    return "\n".join(f"| MAT-{i:03d} | Domain | Feature {i} | {status} | pending |" for i in range(1, 101))


class EvidenceScanTest(unittest.TestCase):
    def test_full_unverified_ledger_is_not_falsely_done(self):
        rows = {f"MAT-{i:03d}": {"status": "AUDIT_REQUIRED"} for i in range(1, 101)}
        self.assertEqual([], scan.validate(tracker(), rows))

    def test_missing_and_duplicate_rows_fail(self):
        self.assertTrue(any("missing tracker row" in e for e in scan.validate(tracker().replace("| MAT-100 |", "| MAT-099 |"), {})))

    def test_unverified_done_requires_real_evidence(self):
        errors = scan.validate(tracker("IMPLEMENTED_VERIFIED"), {"MAT-001": {"status": "IMPLEMENTED_VERIFIED"}})
        self.assertTrue(any("mergedMainSha" in e for e in errors))
        self.assertTrue(any("qaSignoff" in e for e in errors))
        self.assertTrue(any("deviceEvidenceUrl" in e for e in errors))

    def test_evidence_ledger_cannot_contain_extra_or_missing_entries(self):
        rows = {f"MAT-{i:03d}": {"status": "AUDIT_REQUIRED"} for i in range(1, 101)}
        rows["UNLISTED"] = {"status": "AUDIT_REQUIRED"}
        errors = scan.validate(tracker(), rows)
        self.assertTrue(any("exactly 100" in e for e in errors))
        self.assertTrue(any("unknown evidence ID UNLISTED" in e for e in errors))

    def test_all_100_evidence_records_are_mandatory(self):
        errors = scan.validate(tracker(), {})
        self.assertTrue(any("missing evidence record MAT-001" in e for e in errors))
        self.assertTrue(any("missing evidence record MAT-100" in e for e in errors))

    def test_out_of_scope_requires_approval_and_cannot_hide_p0(self):
        rows = {f"MAT-{i:03d}": {"status": "OUT_OF_SCOPE_APPROVED"} for i in range(1, 101)}
        rows["MAT-001"]["priority"] = "P0"
        errors = scan.validate(tracker("OUT_OF_SCOPE_APPROVED"), rows)
        self.assertTrue(any("explicitly P2" in e for e in errors))
        self.assertTrue(any("scopeReason" in e for e in errors))
        self.assertTrue(any("scopeApprovedBy" in e for e in errors))
        self.assertTrue(any("scopeDecisionUrl" in e for e in errors))

    def test_unknown_priority_cannot_bypass_launch_requirements(self):
        rows = {f"MAT-{i:03d}": {"status": "OUT_OF_SCOPE_APPROVED", "scopeReason": "Deferred", "scopeApprovedBy": "owner", "scopeDecisionUrl": "https://example.org/decision"} for i in range(1, 101)}
        self.assertTrue(any("MAT-001: only explicitly P2" in e for e in scan.validate(tracker("OUT_OF_SCOPE_APPROVED"), rows)))

    def test_noncritical_explicit_scope_deferral_requires_no_false_done_evidence(self):
        rows = {
            f"MAT-{i:03d}": {
                "status": "OUT_OF_SCOPE_APPROVED",
                "priority": "P2",
                "scopeReason": "Explicit deferred launch feature",
                "scopeApprovedBy": "release-owner",
                "scopeDecisionUrl": "https://github.com/example/repository/issues/123",
            } for i in range(1, 101)
        }
        self.assertEqual([], scan.validate(tracker("OUT_OF_SCOPE_APPROVED"), rows))

    def test_mismatched_tracker_and_evidence_fail(self):
        self.assertTrue(any("disagreement" in e for e in scan.validate(tracker(), {"MAT-001": {"status": "CI_VERIFIED"}})))


if __name__ == "__main__":
    unittest.main()
