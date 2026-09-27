#!/usr/bin/env python3
"""Small local redirect benchmark. No external destination is fetched; no SLA claim."""
import concurrent.futures
import json
import os
import statistics
import time
import urllib.error
import urllib.request

origin = os.environ.get("BASE_URL", "http://localhost:8080")
token = os.environ.get("API_TOKEN", "local-demo-token-change-before-deployment")
count = 100
concurrency = 8

class NoRedirect(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, *args, **kwargs):
        return None

req = urllib.request.Request(origin + "/api/urls", data=b'{"url":"https://example.com/benchmark","title":"Benchmark"}',
                             headers={"Authorization": "Bearer " + token, "Content-Type": "application/json"})
with urllib.request.urlopen(req, timeout=10) as response:
    code = json.load(response)["code"]

def hit(_):
    start = time.perf_counter()
    try:
        urllib.request.build_opener(NoRedirect).open(origin + "/s/" + code, timeout=15)
        raise RuntimeError("Expected 302")
    except urllib.error.HTTPError as result:
        if result.code != 302:
            raise
        result.close()
    return (time.perf_counter() - start) * 1000

start = time.perf_counter()
with concurrent.futures.ThreadPoolExecutor(max_workers=concurrency) as pool:
    samples = sorted(pool.map(hit, range(count)))
duration = time.perf_counter() - start
req = urllib.request.Request(origin + f"/api/urls/{code}/stats", headers={"Authorization": "Bearer " + token})
with urllib.request.urlopen(req, timeout=10) as response:
    total = json.load(response)["totalClicks"]
assert total == count, f"Lost counts: {total}/{count}"
print(json.dumps({"requests": count, "concurrency": concurrency, "durationSeconds": round(duration, 3),
                  "throughputPerSecond": round(count / duration, 1), "medianMs": round(statistics.median(samples), 2),
                  "p95Ms": round(samples[int(count * .95) - 1], 2), "recordedClicks": total,
                  "note": "Local single-process measurement, warm JVM, no destination fetch; not a production capacity claim"}, indent=2))
