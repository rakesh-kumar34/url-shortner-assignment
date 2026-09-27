#!/usr/bin/env python3
"""Exercise the real browser console against a disposable running application."""
import base64
import json
import os
from pathlib import Path
import uuid
from playwright.sync_api import sync_playwright, expect

origin = os.environ.get("BASE_URL", "http://localhost:8080").rstrip("/")
token = os.environ.get("API_TOKEN", "local-demo-token-change-before-deployment")
output = Path("target/browser-smoke")
output.mkdir(parents=True, exist_ok=True)
alias = "browser-" + uuid.uuid4().hex[:10]
title = "Product documentation"

with sync_playwright() as playwright:
    browser = playwright.chromium.launch()
    context = browser.new_context(viewport={"width": 1440, "height": 1000})
    page = context.new_page()
    errors = []
    page.on("pageerror", lambda error: errors.append(str(error)))
    page.goto(origin)
    expect(page).to_have_title("URL Shortener · URL workspace")
    page.locator("#token").fill("invalid-test-token")
    page.get_by_role("button", name="Connect", exact=False).click()
    expect(page.locator("#notice")).to_contain_text("valid bearer token")
    expect(page.locator("#create-button")).to_be_disabled()
    page.locator("#token").fill(token)
    page.get_by_role("button", name="Connect", exact=False).click()
    expect(page.locator("#connection-title")).to_have_text("Workspace connected")
    expect(page.locator("#token")).to_have_value("")
    assert page.evaluate("localStorage.length + sessionStorage.length") == 0
    page.locator("#destination").fill("https://example.com/docs?q=browser#overview")
    page.locator("#title").fill(title)
    page.locator("#alias").fill(alias)
    page.locator("#create-button").click()
    row = page.locator(".url-row").filter(has_text=alias)
    expect(row).to_be_visible()
    expect(row.locator(".badge")).to_have_text("ACTIVE")
    response = context.request.get(origin + "/s/" + alias, max_redirects=0)
    assert response.status == 302
    assert response.headers["location"] == "https://example.com/docs?q=browser#overview"
    row.get_by_role("button", name="View insights").click()
    expect(page.locator("#click-count")).to_have_text("1")
    expect(page.locator("#daily-table tr")).to_have_count(30)
    page.screenshot(path=str(output / "desktop.png"), full_page=True)
    for width in (390, 768):
        page.set_viewport_size({"width": width, "height": 844})
        assert page.evaluate("document.documentElement.scrollWidth <= innerWidth"), f"Overflow at {width}px"
        expect(page.locator("#create-button")).to_be_visible()
        if width == 390:
            page.screenshot(path=str(output / "mobile.png"), full_page=True)
    # A delayed private response must be cancelled when the operator disconnects.
    pending = []
    stats_path = "**/api/urls/" + alias + "/stats"
    page.route(stats_path, lambda route: pending.append(route))
    with page.expect_request(lambda request: request.url.endswith("/" + alias + "/stats")):
        row.get_by_role("button", name="View insights").click()
    with page.expect_event("requestfailed", predicate=lambda request: request.url.endswith("/" + alias + "/stats")):
        page.get_by_role("button", name="Disconnect").click()
    expect(page.locator("#analytics")).to_be_hidden()
    expect(page.locator(".url-row")).to_have_count(0)
    page.unroute(stats_path)
    page.locator("#token").fill(token)
    page.get_by_role("button", name="Connect", exact=False).click()
    expect(row).to_be_visible()
    page.once("dialog", lambda dialog: dialog.accept())
    row.get_by_role("button", name="Disable", exact=True).click()
    expect(row.locator(".badge")).to_have_text("DISABLED")
    assert context.request.get(origin + "/s/" + alias, max_redirects=0).status == 410
    page.get_by_role("button", name="Disconnect").click()
    page.reload()
    expect(page.locator("#create-button")).to_be_disabled()
    assert not errors, errors
    browser.close()

checks = ["invalid token rejected", "connect/create/list", "query and fragment preserved",
          "redirect and analytics", "390px/768px no overflow", "in-flight request cancelled on disconnect",
          "disable returns 410", "no persistent token", "no browser exceptions"]
(output / "result.json").write_text(json.dumps({"status": "passed", "checks": checks}, indent=2))
print("PASS: " + "; ".join(checks))
if os.environ.get("EMIT_REVIEW_SCREENSHOTS") == "1":
    # Synthetic example.com data only; allows review where artifact downloads are unavailable.
    print("::group::Review screenshots (base64)")
    for name in ("desktop", "mobile"):
        print("REVIEW_SCREENSHOT_" + name.upper() + ":" + base64.b64encode((output / (name + ".png")).read_bytes()).decode())
    print("::endgroup::")
