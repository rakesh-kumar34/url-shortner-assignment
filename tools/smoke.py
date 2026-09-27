#!/usr/bin/env python3
"""Exercise a running server without following redirects or fetching destinations."""
import json
import os
import urllib.error
import urllib.request
import uuid

origin = os.environ.get("BASE_URL", "http://localhost:8080")
token = os.environ.get("API_TOKEN", "local-demo-token-change-before-deployment")

class NoRedirect(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, *args, **kwargs):
        return None

opener = urllib.request.build_opener(NoRedirect)

def request(method, path, body=None, key=None):
    headers = {"Authorization": "Bearer " + token, "Content-Type": "application/json"}
    if key:
        headers["Idempotency-Key"] = key
    req = urllib.request.Request(origin + path, method=method, headers=headers,
                                 data=json.dumps(body).encode() if body is not None else None)
    try:
        response = opener.open(req, timeout=10)
    except urllib.error.HTTPError as error:
        response = error
    data = response.read()
    return response.status, response.headers, json.loads(data) if data and response.headers.get_content_type() == "application/json" else None

key = str(uuid.uuid4())
status, _, link = request("POST", "/api/urls", {"url": "https://example.com/docs?q=demo#intro", "title": "Smoke test"}, key)
assert status == 201, (status, link)
code = link["code"]
status, _, replay = request("POST", "/api/urls", {"url": "https://example.com/docs?q=demo#intro", "title": "Smoke test"}, key)
assert status == 200 and replay["code"] == code
status, headers, _ = request("GET", "/s/" + code)
assert status == 302 and headers["Location"] == "https://example.com/docs?q=demo#intro"
assert request("HEAD", "/s/" + code)[0] == 302
status, _, stats = request("GET", f"/api/urls/{code}/stats")
assert status == 200 and stats["totalClicks"] == 1 and len(stats["daily"]) == 30
assert request("DELETE", "/api/urls/" + code)[0] == 204
assert request("GET", "/s/" + code)[0] == 410
print("PASS: create, durable retry, redirect, HEAD exclusion, analytics, disable, 410")
