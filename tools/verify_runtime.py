#!/usr/bin/env python3
"""Run smoke + benchmark against a packaged JAR and prove replay across restart."""
import json
import os
from pathlib import Path
import socket
import subprocess
import sys
import tempfile
import time
import urllib.error
import urllib.request

jar = Path("target/shortline-1.0.0.jar").resolve()
if not jar.exists():
    raise SystemExit("Build the JAR first with ./mvnw verify")
with socket.socket() as sock:
    sock.bind(("127.0.0.1", 0))
    port = sock.getsockname()[1]
origin = f"http://127.0.0.1:{port}"
env = dict(os.environ, BASE_URL=origin, PUBLIC_ORIGIN=origin,
           API_TOKEN="runtime-test-token-with-32-characters", SPRING_PROFILES_ACTIVE="local")
opener = urllib.request.build_opener(urllib.request.ProxyHandler({}))

def create():
    request = urllib.request.Request(origin + "/api/links", data=b'{"url":"https://example.com/restart"}',
                                     headers={"Authorization": "Bearer " + env["API_TOKEN"],
                                              "Idempotency-Key": "restart-proof", "Content-Type": "application/json"})
    with opener.open(request, timeout=10) as response:
        return response.status, json.load(response)["code"]

with tempfile.TemporaryDirectory(prefix="shortline-runtime-") as folder:
    log_path = Path(folder) / "server.log"
    def start():
        log = log_path.open("ab")
        process = subprocess.Popen(["java", "-jar", str(jar), "--server.address=127.0.0.1", f"--server.port={port}",
                                    f"--spring.datasource.url=jdbc:h2:file:{folder}/db;MODE=PostgreSQL;DB_CLOSE_ON_EXIT=FALSE"],
                                   env=env, stdout=log, stderr=subprocess.STDOUT)
        log.close()
        deadline = time.monotonic() + 45
        while time.monotonic() < deadline:
            if process.poll() is not None:
                raise RuntimeError("Server exited: " + log_path.read_text()[-2000:])
            try:
                with opener.open(origin + "/actuator/health/readiness", timeout=1) as response:
                    if response.status == 200:
                        return process
            except (urllib.error.URLError, TimeoutError):
                time.sleep(.2)
        process.terminate()
        process.wait(timeout=10)
        raise RuntimeError("Readiness timed out")

    process = start()
    try:
        subprocess.run([sys.executable, "tools/smoke.py"], env=env, check=True)
        subprocess.run([sys.executable, "tools/benchmark.py"], env=env, check=True)
        status, original_code = create()
        assert status == 201
        process.terminate()
        process.wait(timeout=10)
        process = start()
        status, replay_code = create()
        assert status == 200 and replay_code == original_code
        print("PASS: restart preserves the link and idempotency result")
        for secret in [env["API_TOKEN"], "https://example.com/restart", "https://example.com/docs?q=demo"]:
            assert secret not in log_path.read_text(), "Sensitive request value found in logs"
        print("PASS: runtime logs omit token and destination URLs")
    finally:
        process.terminate()
        process.wait(timeout=10)
