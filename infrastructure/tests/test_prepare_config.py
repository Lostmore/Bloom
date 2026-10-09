"""Regression checks for local setup upgrades; requires PyYAML and OpenSSL."""

import shutil
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path

import yaml


class PrepareConfigTest(unittest.TestCase):
    def setUp(self):
        self.directory = tempfile.TemporaryDirectory()
        self.addCleanup(self.directory.cleanup)
        self.root = Path(self.directory.name)
        scripts = self.root / "infrastructure"
        scripts.mkdir()
        shutil.copyfile(Path(__file__).resolve().parents[1] / "prepare-ci.py", scripts / "prepare-ci.py")
        for name in ("identity", "users", "interactions", "activities"):
            target = self.root / "server" / name / "src/main/resources/application.yml"
            target.parent.mkdir(parents=True)
            config = {
                "spring": {"datasource": {"password": ""}, "kafka": {"bootstrap-servers": "localhost:9092"}},
                "bloom": {
                    "internal-token": "", "jwt": {}, "identity": {}, "media": {}, "users": {},
                    "events": {"topic": "bloom.identity.v1", "scheduling-enabled": True},
                },
            }
            target.write_text(yaml.safe_dump(config), encoding="utf-8")

    def generate(self):
        return subprocess.run(
            [sys.executable, str(self.root / "infrastructure/prepare-ci.py"), "--output", ".local", "--reuse"],
            capture_output=True,
            text=True,
        )

    def test_upgrade_adds_settings_without_rotating_credentials(self):
        self.assertEqual(self.generate().returncode, 0)
        output = self.root / ".local"
        identity = output / "identity.yml"
        original = yaml.safe_load(identity.read_text(encoding="utf-8"))
        preserved = {name: (output / name).read_bytes() for name in (
            "identity-private.pem", "users.yml", "compose.yml",
        )}
        old = yaml.safe_load(identity.read_text(encoding="utf-8"))
        del old["spring"]["kafka"]
        del old["bloom"]["events"]
        del old["bloom"]["moderation-token"]
        identity.write_text(yaml.safe_dump(old), encoding="utf-8")

        self.assertEqual(self.generate().returncode, 0)
        updated = yaml.safe_load(identity.read_text(encoding="utf-8"))
        self.assertEqual(updated["spring"]["datasource"], original["spring"]["datasource"])
        self.assertEqual(updated["bloom"]["jwt"], original["bloom"]["jwt"])
        self.assertEqual(updated["bloom"]["internal-token"], original["bloom"]["internal-token"])
        self.assertEqual(updated["bloom"]["events"]["topic"], "bloom.identity.v1")
        self.assertGreaterEqual(len(updated["bloom"]["moderation-token"]), 32)
        for name, content in preserved.items():
            self.assertEqual((output / name).read_bytes(), content)
        before = identity.read_bytes()
        self.assertEqual(self.generate().returncode, 0)
        self.assertEqual(identity.read_bytes(), before)

    def test_partial_configuration_is_not_overwritten(self):
        output = self.root / ".local"
        output.mkdir()
        private_key = output / "identity-private.pem"
        private_key.write_text("existing key", encoding="utf-8")
        self.assertNotEqual(self.generate().returncode, 0)
        self.assertEqual(private_key.read_text(encoding="utf-8"), "existing key")
        self.assertFalse((output / "compose.yml").exists())

    def test_upgrade_supplies_identity_token_to_go_without_rotating_passwords(self):
        self.assertEqual(self.generate().returncode, 0)
        output = self.root / ".local"
        compose_file = output / "compose.yml"
        compose = yaml.safe_load(compose_file.read_text(encoding="utf-8"))
        passwords = {}
        for name in ("chat", "media"):
            environment = compose["services"][name]["environment"]
            passwords[name] = environment["DATABASE_URL"]
            del environment["INTERNAL_TOKEN"]
        compose_file.write_text(yaml.safe_dump(compose), encoding="utf-8")
        self.assertEqual(self.generate().returncode, 0)
        updated = yaml.safe_load(compose_file.read_text(encoding="utf-8"))
        token = yaml.safe_load((output / "identity.yml").read_text(encoding="utf-8"))["bloom"]["internal-token"]
        for name in ("chat", "media"):
            self.assertEqual(updated["services"][name]["environment"]["INTERNAL_TOKEN"], token)
            self.assertEqual(updated["services"][name]["environment"]["DATABASE_URL"], passwords[name])

    def test_upgrade_adds_interactions_to_an_existing_stack(self):
        self.assertEqual(self.generate().returncode, 0)
        output = self.root / ".local"
        original = {name: (output / name).read_bytes() for name in ("identity.yml", "users.yml", "identity-private.pem")}
        compose_file = output / "compose.yml"
        compose = yaml.safe_load(compose_file.read_text(encoding="utf-8"))
        admin = compose["services"]["postgres"]["environment"]["POSTGRES_PASSWORD"]
        del compose["services"]["interactions"]
        del compose["services"]["interactions-db-init"]
        compose_file.write_text(yaml.safe_dump(compose), encoding="utf-8")
        (output / "interactions.yml").unlink()
        self.assertEqual(self.generate().returncode, 0)
        config = yaml.safe_load((output / "interactions.yml").read_text(encoding="utf-8"))
        updated = yaml.safe_load(compose_file.read_text(encoding="utf-8"))
        self.assertEqual(updated["services"]["interactions-db-init"]["environment"]["PGPASSWORD"], admin)
        self.assertEqual(config["bloom"]["users"]["token"], yaml.safe_load(original["users.yml"])["bloom"]["internal-token"])
        for name, content in original.items():
            self.assertEqual((output / name).read_bytes(), content)


if __name__ == "__main__":
    unittest.main()
