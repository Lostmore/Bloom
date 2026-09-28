"""Create disposable stack credentials/configuration for CI; never edit developer configs."""

import argparse
import secrets
import subprocess
from pathlib import Path

import yaml


root = Path(__file__).resolve().parent.parent
parser = argparse.ArgumentParser(description="Generate isolated Bloom configuration.")
parser.add_argument("--output", choices=(".ci", ".local", ".local-test"), default=".ci")
parser.add_argument("--reuse", action="store_true", help="Preserve existing local credentials.")
args = parser.parse_args()
output = root / args.output
if args.reuse and output.exists() and any(output.iterdir()):
    required = ("identity.yml", "users.yml", "identity-private.pem", "compose.yml")
    if not all((output / name).is_file() for name in required):
        raise SystemExit("Incomplete local configuration. Restore its missing files; credentials were not changed.")
    print("Existing local configuration preserved. No credentials were regenerated.")
    raise SystemExit(0)
output.mkdir(exist_ok=True)
passwords = {name: secrets.token_hex(24) for name in ("admin", "identity", "users", "chat")}
identity_token = secrets.token_hex(32)

private_key = output / "identity-private.pem"
subprocess.run(
    ["openssl", "genpkey", "-algorithm", "RSA", "-pkeyopt", "rsa_keygen_bits:2048", "-out", str(private_key)],
    check=True,
    stdout=subprocess.DEVNULL,
    stderr=subprocess.DEVNULL,
)
public_key = subprocess.check_output(
    ["openssl", "pkey", "-in", str(private_key), "-pubout"], text=True
)

overrides = {"services": {}}
for service in ("identity", "users"):
    source = root / "server" / service / "src/main/resources/application.yml"
    config = yaml.safe_load(source.read_text(encoding="utf-8"))
    config["spring"]["datasource"]["password"] = passwords[service]
    if service == "identity":
        config["bloom"]["internal-token"] = identity_token
        config["bloom"]["jwt"]["private-key"] = private_key.read_text(encoding="utf-8")
        config["bloom"]["jwt"]["public-key"] = public_key
    else:
        config["bloom"]["internal-token"] = secrets.token_hex(32)
        config["bloom"]["identity"]["token"] = identity_token
        config["bloom"]["media"]["token"] = secrets.token_hex(32)
    destination = output / f"{service}.yml"
    destination.write_text(yaml.safe_dump(config, allow_unicode=True, sort_keys=False), encoding="utf-8")
    overrides["services"][service] = {
        "volumes": [{"type": "bind", "source": "./" + destination.relative_to(root).as_posix(), "target": "/app/config/application.yml", "read_only": True}]
    }

overrides["services"]["postgres"] = {"environment": {
    "POSTGRES_PASSWORD": passwords["admin"],
    "IDENTITY_DATABASE_PASSWORD": passwords["identity"],
    "USERS_DATABASE_PASSWORD": passwords["users"],
    "CHAT_DATABASE_PASSWORD": passwords["chat"],
}}
overrides["services"]["chat"] = {"environment": {
    "DATABASE_URL": f"postgres://bloom_chat:{passwords['chat']}@postgres:5432/bloom_chat?sslmode=disable"
}}
(output / "compose.yml").write_text(yaml.safe_dump(overrides, sort_keys=False), encoding="utf-8")
print(f"Stack configuration prepared in {args.output} (credentials not printed).")
