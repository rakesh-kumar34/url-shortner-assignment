'use strict';
const $ = id => document.getElementById(id);
let token = '';
let page = 0;
let lastTotal = 0;
let pendingCreation = null;
let connectionRequests = new AbortController();
const pageSize = 5;

function cancelConnectionRequests() {
  connectionRequests.abort();
  connectionRequests = new AbortController();
}

function reportError(error) {
  if (error.name !== 'AbortError') notice(error.message, true);
}

function notice(message, error = false) {
  $('notice').textContent = message;
  $('notice').className = `notice${error ? ' error' : ''}`;
}

async function api(path, options = {}) {
  const signal = connectionRequests.signal;
  const response = await fetch(path, {
    ...options,
    signal,
    headers: { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json', ...options.headers }
  });
  const body = response.status === 204 ? null : await response.json().catch(() => ({}));
  if (signal.aborted) throw new DOMException('Request cancelled', 'AbortError');
  if (!response.ok) {
    throw new Error(body.message || `Request failed (${response.status}). Please try again.`);
  }
  return body;
}

function element(tag, className, text) {
  const node = document.createElement(tag);
  if (className) node.className = className;
  if (text !== undefined) node.textContent = text;
  return node;
}

function setConnected(connected) {
  $('connect-form').classList.toggle('hidden', connected);
  $('disconnect').classList.toggle('hidden', !connected);
  $('connection-title').textContent = connected ? 'Workspace connected' : 'Connect your workspace';
  $('connection-description').textContent = connected
    ? 'Your token is held in memory and cleared when you disconnect or close this tab.'
    : 'Enter your operator token to manage URLs. It stays in this tab only.';
  $('create-button').disabled = !connected;
  $('refresh').disabled = !connected;
}

function urlRow(url) {
  const row = element('article', 'url-row');
  const top = element('div', 'url-top');
  top.append(element('span', 'url-title', url.title || url.code),
    element('span', `badge${url.status === 'ACTIVE' ? '' : ' inactive'}`, url.status));
  const destination = element('span', 'destination', url.url);
  destination.title = url.url;
  const short = element('a', 'short-url', url.shortUrl);
  short.href = url.shortUrl;
  short.target = '_blank';
  short.rel = 'noopener noreferrer';
  const actions = element('div', 'url-actions');
  const copy = element('button', '', 'Copy URL');
  copy.type = 'button';
  copy.addEventListener('click', async () => {
    try { await navigator.clipboard.writeText(url.shortUrl); notice('Short URL copied.'); }
    catch { notice(`Copy this URL: ${url.shortUrl}`); }
  });
  const stats = element('button', '', 'View insights');
  stats.type = 'button';
  stats.addEventListener('click', () => showStats(url));
  const disable = element('button', 'disable', 'Disable');
  disable.type = 'button';
  disable.disabled = url.status !== 'ACTIVE';
  disable.addEventListener('click', async () => {
    if (!window.confirm(`Disable ${url.title || url.code}? This URL will stop redirecting.`)) return;
    disable.disabled = true;
    try { await api(`/api/urls/${encodeURIComponent(url.code)}`, { method: 'DELETE' }); await refresh(); notice('URL disabled. Its history is preserved.'); }
    catch (error) { disable.disabled = false; reportError(error); }
  });
  actions.append(copy, stats, element('span', 'url-clicks', `${url.totalClicks} resolutions`), disable);
  row.append(top, destination, short, actions);
  return row;
}

async function refresh() {
  $('refresh').disabled = true;
  try {
    const data = await api(`/api/urls?page=${page}&size=${pageSize}`);
    lastTotal = data.total;
    $('total').textContent = String(data.total);
    $('url-list').replaceChildren(...data.items.map(urlRow));
    $('empty').classList.toggle('hidden', data.items.length > 0);
    if (data.items.length === 0) $('empty').querySelector('p').textContent = 'Create a URL to start sharing. Your saved URLs will appear here.';
    $('pagination').classList.toggle('hidden', data.total <= pageSize);
    $('page-label').textContent = `Page ${page + 1} of ${Math.max(1, Math.ceil(data.total / pageSize))}`;
    $('previous').disabled = page === 0;
    $('next').disabled = (page + 1) * pageSize >= data.total;
  } finally { $('refresh').disabled = !token; }
}

$('connect-form').addEventListener('submit', async event => {
  event.preventDefault();
  cancelConnectionRequests();
  token = $('token').value.trim();
  try { page = 0; await refresh(); setConnected(true); $('token').value = ''; notice('Connected. Your URLs are ready.'); }
  catch (error) {
    if (error.name === 'AbortError') return;
    token = ''; setConnected(false); reportError(error);
  }
});

$('disconnect').addEventListener('click', () => {
  cancelConnectionRequests();
  token = '';
  pendingCreation = null;
  setConnected(false);
  $('url-list').replaceChildren();
  $('analytics').classList.add('hidden');
  $('pagination').classList.add('hidden');
  $('empty').classList.remove('hidden');
  $('total').textContent = '—';
  notice('Disconnected. Your token has been cleared.');
});

$('create-form').addEventListener('submit', async event => {
  event.preventDefault();
  const payload = { url: $('destination').value, title: $('title').value || null,
    customAlias: $('alias').value || null, expiresAt: $('expiry').value ? new Date($('expiry').value).toISOString() : null };
  const body = JSON.stringify(payload);
  // Keep the same key if a network failure leaves the creation outcome uncertain.
  if (!pendingCreation || pendingCreation.body !== body) pendingCreation = { body, key: crypto.randomUUID() };
  $('create-button').disabled = true;
  $('create-button').textContent = 'Creating…';
  try {
    const url = await api('/api/urls', { method: 'POST', body, headers: { 'Idempotency-Key': pendingCreation.key } });
    pendingCreation = null;
    $('create-form').reset();
    page = 0;
    await refresh();
    notice(`Ready to share: ${url.shortUrl}`);
  } catch (error) { reportError(error); }
  finally { $('create-button').disabled = !token; $('create-button').textContent = 'Create short URL ↗'; }
});

$('refresh').addEventListener('click', () => refresh().catch(reportError));
$('previous').addEventListener('click', () => { if (page > 0) page--; refresh().catch(reportError); });
$('next').addEventListener('click', () => { if ((page + 1) * pageSize < lastTotal) page++; refresh().catch(reportError); });
$('close-stats').addEventListener('click', () => $('analytics').classList.add('hidden'));

async function showStats(url) {
  try {
    const data = await api(`/api/urls/${encodeURIComponent(url.code)}/stats`);
    $('analytics-title').textContent = url.title || url.code;
    $('click-count').textContent = data.totalClicks.toLocaleString();
    const svg = $('chart');
    svg.replaceChildren();
    const maximum = Math.max(1, ...data.daily.map(day => day.clicks));
    for (let i = 0; i < data.daily.length; i++) {
      const day = data.daily[i];
      const rect = document.createElementNS('http://www.w3.org/2000/svg', 'rect');
      const height = day.clicks / maximum * 120;
      for (const [name, value] of Object.entries({ x: i * 21 + 4, y: 130 - height, width: 13, height, rx: 2 })) rect.setAttribute(name, value);
      const title = document.createElementNS('http://www.w3.org/2000/svg', 'title');
      title.textContent = `${day.date}: ${day.clicks} resolutions`;
      rect.append(title);
      svg.append(rect);
    }
    const line = document.createElementNS('http://www.w3.org/2000/svg', 'line');
    for (const [name, value] of Object.entries({ x1: 0, y1: 131, x2: 640, y2: 131 })) line.setAttribute(name, value);
    svg.append(line);
    $('chart-start').textContent = data.daily[0].date;
    $('chart-end').textContent = data.daily[data.daily.length - 1].date;
    $('daily-table').replaceChildren(...data.daily.map(day => {
      const row = element('tr'); row.append(element('td', '', day.date), element('td', '', String(day.clicks))); return row;
    }));
    $('analytics').classList.remove('hidden');
    $('analytics').scrollIntoView({ behavior: 'smooth', block: 'nearest' });
  } catch (error) { reportError(error); }
}

fetch('/actuator/health/readiness').then(response => {
  $('health').textContent = response.ok ? 'Service operational' : 'Service unavailable';
  $('health-dot').classList.toggle('online', response.ok);
}).catch(() => { $('health').textContent = 'Service unavailable'; });
