"""Build a secret-free architecture snapshot from tracked project sources."""

import hashlib
import json
import re
import shutil
import subprocess
from datetime import datetime, timezone
from pathlib import Path

import yaml

ROOT = Path(__file__).resolve().parents[2]
OUTPUT = ROOT / "docs/architecture/data.js"
MODEL = Path(__file__).with_name("model.json")


def read(path):
    return (ROOT / path).read_text(encoding="utf-8")


def evidence(path, needle=""):
    file = ROOT / path
    if not file.is_file():
        return {"file": path, "line": 1, "found": False}
    content = file.read_text(encoding="utf-8")
    offset = content.find(needle) if needle else 0
    return {"file": path, "line": content[:max(offset, 0)].count("\n") + 1,
            "found": offset >= 0}


def java_endpoints(service):
    endpoints = []
    for file in sorted((ROOT / "server" / service / "src/main/java").rglob("*Controller.java")):
        content = file.read_text(encoding="utf-8")
        before_class = content.split("public class", 1)[0]
        prefix = re.search(r'@RequestMapping\("([^"]*)"\)', before_class)
        prefix = prefix.group(1) if prefix else ""
        for match in re.finditer(r'@(Get|Post|Put|Patch|Delete)Mapping(?:\(\s*"([^"]*)"\s*\))?', content):
            path = prefix + (match.group(2) or "")
            if not path.startswith("/"):
                continue
            endpoints.append({"method": match.group(1).upper(), "path": path,
                              "internal": path.startswith("/internal/"),
                              "source": evidence(file.relative_to(ROOT).as_posix(), match.group(0)),
                              "origin": "Spring mapping"})
    return endpoints


def go_endpoints(service):
    endpoints = []
    for source in sorted((ROOT / "server" / service / "internal").rglob("*.go")):
        if source.name.endswith("_test.go"):
            continue
        content = source.read_text(encoding="utf-8")
        for match in re.finditer(r'//\s*@Router\s+(\S+)\s+\[(\w+)\]', content):
            path, method = match.groups()
            endpoints.append({"method": method.upper(), "path": path,
                              "internal": path.startswith("/internal/"), "origin": "Go @Router",
                              "source": evidence(source.relative_to(ROOT).as_posix(), match.group(0))})
    if endpoints:
        return endpoints
    file = ROOT / "server" / service / "docs/swagger.json"
    if not file.exists():
        return []
    spec = json.loads(file.read_text(encoding="utf-8"))
    return [{"method": method.upper(), "path": path, "internal": path.startswith("/internal/"),
             "summary": operation.get("summary", ""), "origin": "OpenAPI snapshot",
             "source": evidence(file.relative_to(ROOT).as_posix(), '"' + path + '"')}
            for path, item in spec.get("paths", {}).items() for method, operation in item.items()
            if method in {"get", "post", "put", "patch", "delete"}]


def generate():
    model = json.loads(MODEL.read_text(encoding="utf-8"))
    compose = yaml.safe_load(read("compose.yml"))
    gateway = yaml.safe_load(read("server/api-gateway/src/main/resources/application.yml"))
    routes = gateway["spring"]["cloud"]["gateway"]["server"]["webflux"]["routes"]
    catalog = gateway["bloom"]["api-docs"]["services"]
    nodes, edges = [], []
    service_ids = list(dict.fromkeys(["api-gateway", *catalog,
                                    *[p.name for p in (ROOT / "server").iterdir()
                                      if p.is_dir() and ((p / "go.mod").exists() or (p / "build.gradle").exists())]]))
    for name in service_ids:
        service = compose["services"].get(name, {})
        java = (ROOT / "server" / name / "build.gradle").exists()
        endpoints = java_endpoints(name) if java else go_endpoints(name)
        source_root = ROOT / "server" / name
        source_files = sorted(p for p in source_root.rglob("*.java") if "/src/main/" in p.as_posix()) if java else sorted(
            p for p in source_root.rglob("*.go") if not p.name.endswith("_test.go") and "/docs/" not in p.as_posix())
        state = "stub" if not endpoints and name != "api-gateway" else "implemented"
        published = service.get("ports", [])
        port = 8080 if name == "api-gateway" else None
        url = catalog.get(name, {}).get("url", "")
        match = re.search(r'localhost:(\d+)', url)
        if match:
            port = int(match.group(1))
        layers = {}
        for p in source_files:
            relative = p.relative_to(source_root).as_posix()
            layer = next((part for part in ["controller", "delivery", "service", "repository", "security", "client", "events", "worker", "domain", "model", "dto", "config"] if part in p.parts), "application")
            layers.setdefault(layer, []).append({"name": p.name, "source": evidence(p.relative_to(ROOT).as_posix())})
        nodes.append({"id": name, "title": "API Gateway" if name == "api-gateway" else name.title(),
                      "type": "java" if java else "go", "state": state, "port": port,
                      "description": model["descriptions"].get(name, "Сервис проекта; описание ещё не добавлено."),
                      "published": published, "inCompose": bool(service), "profiles": service.get("profiles", []),
                      "endpoints": endpoints, "layers": layers, "files": len(source_files),
                      "source": evidence("compose.yml", "  " + name + ":") if service else evidence("server/" + name + "/go.mod")})
        if name != "api-gateway":
            matching = [r for r in routes if (name.upper() + "_URL:") in r.get("uri", "") or (name == "chat" and "CHAT_WS_URL" in r.get("uri", ""))]
            if matching:
                edges.append({"id": "gateway-" + name, "from": "api-gateway", "to": name,
                              "kind": "route" if state == "implemented" else "planned", "label": "HTTP / WS" if name == "chat" else "HTTP",
                              "description": "Маршруты объявлены в Gateway. " + ("Сервис пока заготовка." if state == "stub" else "Наличие маршрута не проверяет доступность сервиса."),
                              "routes": [{"id": r["id"], "predicates": r.get("predicates", []), "filters": r.get("filters", [])} for r in matching],
                              "source": evidence("server/api-gateway/src/main/resources/application.yml", "- id: " + matching[0]["id"])})
        # Only extract the database name, never the URL/credentials.
        config_text = "\n".join(service.get("command", [])) + "\n" + str(service.get("environment", {}).get("DATABASE_URL", ""))
        db = re.search(r'postgres(?:ql)?://[^\s]+/(bloom_[\w]+)', config_text)
        if db:
            database = "db-" + name
            nodes.append({"id": database, "title": db.group(1), "type": "database", "state": "implemented",
                          "description": "Отдельная база сервиса " + name + " в общем PostgreSQL-контейнере. Не отдельный сервер PostgreSQL.",
                          "source": evidence("compose.yml", db.group(1)), "endpoints": [], "layers": {}})
            edges.append({"id": name + "-db", "from": name, "to": database, "kind": "sql", "label": "SQL",
                          "description": "Подключение к собственной базе PostgreSQL из конфигурации Compose.", "source": evidence("compose.yml", db.group(1))})
    nodes.extend([
        {"id": "client", "title": "Приложение", "type": "client", "state": "concept", "description": "Условный мобильный клиент или Postman. В этой карте это участник сценария, а не проверенная реализация фронтенда.", "endpoints": [], "layers": {}},
        {"id": "kafka", "title": "Kafka", "type": "event", "state": "implemented", "description": "Асинхронная доставка событий. Топики и подписки показаны только там, где они найдены в просмотренном коде.", "source": evidence("compose.yml", "  kafka:"), "endpoints": [], "layers": {}},
        {"id": "files", "title": "Media storage", "type": "storage", "state": "implemented", "description": "Файлы в /app/uploads, Docker volume media-data. Это локальный диск, не S3.", "source": evidence("compose.yml", "media-data:/app/uploads"), "endpoints": [], "layers": {}}
    ])
    edges.extend([
        {"id": "client-gateway", "from": "client", "to": "api-gateway", "kind": "route", "label": ":8080 · HTTP / WS", "description": "Публичная точка входа приложения. Прямой introspect из Postman использует отдельный опубликованный порт Identity.", "source": evidence("compose.yml", "8080:8080")},
        {"id": "media-files", "from": "media", "to": "files", "kind": "sql", "label": "Файлы", "description": "Media сохраняет содержимое файла на диск.", "source": evidence("server/media/internal/service/media_service.go", "os.WriteFile")}
    ])
    for connection in model["connections"]:
        connection["source"] = evidence(connection.pop("file"), connection.pop("needle"))
        if not connection["source"]["found"]:
            connection["kind"] = "planned"
            connection["description"] = "Требует сверки: исходный участок кода изменился. " + connection["description"]
        edges.append(connection)
    for note in model["notes"]:
        note["source"] = evidence(note.pop("file"), note.pop("needle"))
        note["review"] = not note["source"]["found"]
    try:
        commit = subprocess.check_output(["git", "rev-parse", "--short", "HEAD"], cwd=ROOT, text=True).strip()
    except (subprocess.CalledProcessError, FileNotFoundError):
        commit = "local"
    payload = {"commit": commit, "nodes": nodes, "edges": edges, "notes": model["notes"], "scenarios": model["scenarios"]}
    digest = hashlib.sha256(json.dumps(payload, sort_keys=True, ensure_ascii=False).encode()).hexdigest()[:12]
    payload.update({"revision": digest, "generated": datetime.now(timezone.utc).isoformat(),
                    "notice": "Статический анализ конфигурации и исходников; не мониторинг запущенных сервисов. Сценарии описаны вручную и требуют сверки после изменений логики."})
    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    brand_output = OUTPUT.parent / "assets/brand"
    brand_output.mkdir(parents=True, exist_ok=True)
    for asset in (ROOT / "assets/brand").glob("*"):
        if asset.suffix in {".svg", ".png"}:
            shutil.copyfile(asset, brand_output / asset.name)
    temporary = OUTPUT.with_suffix(".tmp")
    temporary.write_text("window.BLOOM_ARCHITECTURE = " + json.dumps(payload, ensure_ascii=False, indent=2) + ";\n", encoding="utf-8")
    temporary.replace(OUTPUT)
    print(f"Architecture: {len(nodes)} nodes, {len(edges)} connections -> {OUTPUT}")
    return payload


if __name__ == "__main__":
    generate()
