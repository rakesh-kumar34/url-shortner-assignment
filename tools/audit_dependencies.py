#!/usr/bin/env python3
"""Check resolved Maven coordinates against OSV; sends package names/versions only.

First run: ./mvnw dependency:tree -DoutputFile=target/dependencies.txt
Then: python3 tools/audit_dependencies.py
Network failure is an error, never a clean security result.
"""
import json
from pathlib import Path
import re
import urllib.request

coordinates = set()
for line in Path("target/dependencies.txt").read_text().splitlines():
    match = re.search(r"([\w.-]+):([\w.-]+):(?:jar|pom):(?:[\w.-]+:)?([\w.+-]+):(compile|runtime)\b", line)
    if match:
        coordinates.add((f"{match[1]}:{match[2]}", match[3]))
if not coordinates:
    raise SystemExit("No resolved runtime coordinates; generate target/dependencies.txt first")
packages = sorted(coordinates)
payload = {"queries": [{"package": {"ecosystem": "Maven", "name": name}, "version": version}
                       for name, version in packages]}
request = urllib.request.Request("https://api.osv.dev/v1/querybatch", data=json.dumps(payload).encode(),
                                 headers={"Content-Type": "application/json"})
with urllib.request.urlopen(request, timeout=60) as response:
    results = json.load(response)["results"]
findings = [{"package": name, "version": version, "vulnerabilities": result["vulns"]}
            for (name, version), result in zip(packages, results) if result.get("vulns")]
report = {"source": "OSV", "packagesChecked": len(packages), "findings": findings}
Path("target/dependency-audit.json").write_text(json.dumps(report, indent=2) + "\n")
print(json.dumps(report, indent=2))
raise SystemExit(1 if findings else 0)
