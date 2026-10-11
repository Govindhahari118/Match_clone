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

    def test_mismatched_tracker_and_evidence_fail(self):
        self.assertTrue(any("disagreement" in e for e in scan.validate(tracker(), {"MAT-001": {"status": "CI_VERIFIED"}})))


if __name__ == "__main__":
    unittest.main()
