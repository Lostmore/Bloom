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


def add_go_identity_credentials(overrides):
    identity = yaml.safe_load((output / "identity.yml").read_text(encoding="utf-8"))
    token = identity["bloom"]["internal-token"]
    for name in ("chat", "media"):
        environment = overrides["services"].setdefault(name, {}).setdefault("environment", {})
        if not environment.get("INTERNAL_TOKEN"):
            environment["INTERNAL_TOKEN"] = token


def add_interactions(overrides):
    path = output / "interactions.yml"
    identity = yaml.safe_load((output / "identity.yml").read_text(encoding="utf-8"))
    users = yaml.safe_load((output / "users.yml").read_text(encoding="utf-8"))
    if not path.exists():
        if "interactions" in overrides["services"]:
            raise SystemExit("Restore the missing interactions.yml; its existing database password was not rotated.")
        defaults = root / "server/interactions/src/main/resources/application.yml"
        config = yaml.safe_load(defaults.read_text(encoding="utf-8"))
        config["spring"]["datasource"]["password"] = secrets.token_hex(24)
        config["bloom"]["internal-token"] = secrets.token_hex(32)
        config["bloom"]["identity"]["token"] = identity["bloom"]["internal-token"]
        config["bloom"]["users"]["token"] = users["bloom"]["internal-token"]
        path.write_text(yaml.safe_dump(config, allow_unicode=True, sort_keys=False), encoding="utf-8")
    config = yaml.safe_load(path.read_text(encoding="utf-8"))
    overrides["services"].setdefault("interactions", {"volumes": [{
        "type": "bind", "source": "./" + path.relative_to(root).as_posix(),
        "target": "/app/config/application.yml", "read_only": True,
    }]})
    overrides["services"].setdefault("interactions-db-init", {"environment": {
        "PGPASSWORD": overrides["services"]["postgres"]["environment"]["POSTGRES_PASSWORD"],
        "INTERACTIONS_DATABASE_PASSWORD": config["spring"]["datasource"]["password"],
    }})


if args.reuse and output.exists() and any(output.iterdir()):
    required = ("identity.yml", "users.yml", "identity-private.pem", "compose.yml")
    if not all((output / name).is_file() for name in required):
        raise SystemExit("Incomplete local configuration. Restore its missing files; credentials were not changed.")
    # Add new Identity settings without rotating existing database/JWT credentials.
    identity_file = output / "identity.yml"
    config = yaml.safe_load(identity_file.read_text(encoding="utf-8"))
    defaults = yaml.safe_load((root / "server/identity/src/main/resources/application.yml").read_text(encoding="utf-8"))
    before = yaml.safe_dump(config, sort_keys=False)
    config["spring"].setdefault("kafka", defaults["spring"]["kafka"])
    config["bloom"].setdefault("events", defaults["bloom"]["events"])
    if "moderation-token" not in config["bloom"]:
        config["bloom"]["moderation-token"] = secrets.token_hex(32)
    if yaml.safe_dump(config, sort_keys=False) != before:
        identity_file.write_text(yaml.safe_dump(config, allow_unicode=True, sort_keys=False), encoding="utf-8")
    compose_file = output / "compose.yml"
    overrides = yaml.safe_load(compose_file.read_text(encoding="utf-8"))
    before_compose = yaml.safe_dump(overrides, sort_keys=False)
    add_interactions(overrides)
    add_go_identity_credentials(overrides)
    if yaml.safe_dump(overrides, sort_keys=False) != before_compose:
        compose_file.write_text(yaml.safe_dump(overrides, sort_keys=False), encoding="utf-8")
    print("Existing credentials preserved; missing service settings added.")
    raise SystemExit(0)
output.mkdir(exist_ok=True)
passwords = {name: secrets.token_hex(24) for name in ("admin", "identity", "users", "chat", "media")}
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
        config["bloom"]["moderation-token"] = secrets.token_hex(32)
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
    "MEDIA_DATABASE_PASSWORD": passwords["media"],
}}
overrides["services"]["chat"] = {"environment": {
    "DATABASE_URL": f"postgres://bloom_chat:{passwords['chat']}@postgres:5432/bloom_chat?sslmode=disable"
}}
overrides["services"]["media"] = {"environment": {
    "DATABASE_URL": f"postgres://bloom_media:{passwords['media']}@postgres:5432/bloom_media?sslmode=disable"
}}
add_interactions(overrides)
add_go_identity_credentials(overrides)
(output / "compose.yml").write_text(yaml.safe_dump(overrides, sort_keys=False), encoding="utf-8")
print(f"Stack configuration prepared in {args.output} (credentials not printed).")
