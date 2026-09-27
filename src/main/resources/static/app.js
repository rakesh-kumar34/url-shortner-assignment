'use strict';
const $ = id => document.getElementById(id);
let token = '';
let page = 0;
let lastTotal = 0;
let pendingCreation = null;
const pageSize = 5;

function notice(message, error = false) {
  $('notice').textContent = message;
  $('notice').className = `notice${error ? ' error' : ''}`;
}

async function api(path, options = {}) {
  const response = await fetch(path, {
    ...options,
    headers: { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json', ...options.headers }
  });
  if (!response.ok) {
    const body = await response.json().catch(() => ({}));
    throw new Error(body.message || `Request failed (${response.status}). Please try again.`);
  }
  return response.status === 204 ? null : response.json();
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
    : 'Enter your operator token to manage links. It stays in this tab only.';
  $('create-button').disabled = !connected;
  $('refresh').disabled = !connected;
}

function linkRow(link) {
  const row = element('article', 'link-row');
  const top = element('div', 'link-top');
  top.append(element('span', 'link-title', link.title || link.code),
    element('span', `badge${link.status === 'ACTIVE' ? '' : ' inactive'}`, link.status));
  const destination = element('span', 'destination', link.url);
  destination.title = link.url;
  const short = element('a', 'short-link', link.shortUrl);
  short.href = link.shortUrl;
  short.target = '_blank';
  short.rel = 'noopener noreferrer';
  const actions = element('div', 'link-actions');
  const copy = element('button', '', 'Copy link');
  copy.type = 'button';
  copy.addEventListener('click', async () => {
    try { await navigator.clipboard.writeText(link.shortUrl); notice('Short link copied.'); }
    catch { notice(`Copy this link: ${link.shortUrl}`); }
  });
  const stats = element('button', '', 'View insights');
  stats.type = 'button';
  stats.addEventListener('click', () => showStats(link));
  const disable = element('button', 'disable', 'Disable');
  disable.type = 'button';
  disable.disabled = link.status !== 'ACTIVE';
  disable.addEventListener('click', async () => {
    if (!window.confirm(`Disable ${link.title || link.code}? This link will stop redirecting.`)) return;
    disable.disabled = true;
    try { await api(`/api/links/${encodeURIComponent(link.code)}`, { method: 'DELETE' }); await refresh(); notice('Link disabled. Its history is preserved.'); }
    catch (error) { disable.disabled = false; notice(error.message, true); }
  });
  actions.append(copy, stats, element('span', 'link-clicks', `${link.totalClicks} resolutions`), disable);
  row.append(top, destination, short, actions);
  return row;
}

async function refresh() {
  $('refresh').disabled = true;
  try {
    const data = await api(`/api/links?page=${page}&size=${pageSize}`);
    lastTotal = data.total;
    $('total').textContent = String(data.total);
    $('link-list').replaceChildren(...data.items.map(linkRow));
    $('empty').classList.toggle('hidden', data.items.length > 0);
    if (data.items.length === 0) $('empty').querySelector('p').textContent = 'Create a link to start sharing. Your saved links will appear here.';
    $('pagination').classList.toggle('hidden', data.total <= pageSize);
    $('page-label').textContent = `Page ${page + 1} of ${Math.max(1, Math.ceil(data.total / pageSize))}`;
    $('previous').disabled = page === 0;
    $('next').disabled = (page + 1) * pageSize >= data.total;
  } finally { $('refresh').disabled = !token; }
}

$('connect-form').addEventListener('submit', async event => {
  event.preventDefault();
  token = $('token').value.trim();
  try { page = 0; await refresh(); setConnected(true); $('token').value = ''; notice('Connected. Your links are ready.'); }
  catch (error) { token = ''; setConnected(false); notice(error.message, true); }
});

$('disconnect').addEventListener('click', () => {
  token = '';
  pendingCreation = null;
  setConnected(false);
  $('link-list').replaceChildren();
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
    const link = await api('/api/links', { method: 'POST', body, headers: { 'Idempotency-Key': pendingCreation.key } });
    pendingCreation = null;
    $('create-form').reset();
    page = 0;
    await refresh();
    notice(`Ready to share: ${link.shortUrl}`);
  } catch (error) { notice(error.message, true); }
  finally { $('create-button').disabled = !token; $('create-button').textContent = 'Create short link ↗'; }
});

$('refresh').addEventListener('click', () => refresh().catch(error => notice(error.message, true)));
$('previous').addEventListener('click', () => { if (page > 0) page--; refresh().catch(error => notice(error.message, true)); });
$('next').addEventListener('click', () => { if ((page + 1) * pageSize < lastTotal) page++; refresh().catch(error => notice(error.message, true)); });
$('close-stats').addEventListener('click', () => $('analytics').classList.add('hidden'));

async function showStats(link) {
  try {
    const data = await api(`/api/links/${encodeURIComponent(link.code)}/stats`);
    $('analytics-title').textContent = link.title || link.code;
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
  } catch (error) { notice(error.message, true); }
}

fetch('/actuator/health/readiness').then(response => {
  $('health').textContent = response.ok ? 'Service operational' : 'Service unavailable';
  $('health-dot').classList.toggle('online', response.ok);
}).catch(() => { $('health').textContent = 'Service unavailable'; });
