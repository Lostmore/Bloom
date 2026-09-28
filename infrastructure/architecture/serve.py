"""Serve the architecture explorer locally and regenerate when sources change."""

import argparse
import os
import threading
import webbrowser
from functools import partial
from http.server import SimpleHTTPRequestHandler, ThreadingHTTPServer

from generate import ROOT, generate
import yaml


def inputs():
    files = [ROOT / "compose.yml", ROOT / "infrastructure/architecture/model.json"]
    files.extend((ROOT / "assets/brand").glob("*.svg"))
    files.extend((ROOT / "assets/brand").glob("*.png"))
    for directory, folders, names in os.walk(ROOT / "server"):
        folders[:] = [name for name in folders if name not in {"build", ".gradle", "node_modules", ".git", "uploads", "bin"}]
        for name in names:
            path = ROOT / directory / name
            if path.suffix in {".java", ".go", ".json", ".yml", ".yaml", ".properties", ".mod"}:
                files.append(path)
    return tuple((str(path), path.stat().st_mtime_ns, path.stat().st_size) for path in sorted(files) if path.exists())


class Handler(SimpleHTTPRequestHandler):
    def end_headers(self):
        self.send_header("Cache-Control", "no-store")
        super().end_headers()

    def log_message(self, message, *args):
        if args and str(args[1]) != "200":
            super().log_message(message, *args)


def watch(stop):
    previous = inputs()
    while not stop.wait(2):
        try:
            current = inputs()
            if current != previous:
                generate()
                previous = current
        except (OSError, ValueError, yaml.YAMLError) as error:
            print(f"Could not refresh snapshot; keeping the last version: {error}", flush=True)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--port", type=int, default=8800)
    parser.add_argument("--open", action="store_true", help="Open the explorer in a browser")
    args = parser.parse_args()
    generate()
    server = ThreadingHTTPServer(("127.0.0.1", args.port), partial(Handler, directory=str(ROOT / "docs/architecture")))
    stop = threading.Event()
    threading.Thread(target=watch, args=(stop,), daemon=True).start()
    url = f"http://127.0.0.1:{args.port}"
    print(f"Bloom architecture: {url}\nWatching project sources. Ctrl+C to stop.", flush=True)
    if args.open:
        webbrowser.open(url)
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        pass
    finally:
        stop.set()
        server.server_close()


if __name__ == "__main__":
    main()
