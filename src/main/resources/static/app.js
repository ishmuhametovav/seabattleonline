/* Sea Battle Online: frontend for the API and STOMP destinations in seabattleonline.zip.
 * No bundler required; SockJS and @stomp/stompjs are loaded from index.html.
 */
'use strict';

const CFG = Object.freeze({
  api: '/api/v1', socket: '/ws', app: '/app', size: 10,
  pollMs: 2400, commandRefreshMs: 350,
});
const FLEET = Object.freeze({ 4: 1, 3: 2, 2: 3, 1: 4 });
const SHIP_NAMES = { 4: 'Линкор', 3: 'Крейсер', 2: 'Эсминец', 1: 'Катер' };
const COLUMNS = 'АБВГДЕЖЗИК';
const CELL_NAMES = { EMPTY: 'вода', SHIP: 'корабль', HIT: 'попадание', SUNK: 'потоплен', MISS: 'промах' };
const $ = (selector) => document.querySelector(selector);
const $$ = (selector) => [...document.querySelectorAll(selector)];
const show = (selector) => $(selector)?.classList.remove('hidden');
const hide = (selector) => $(selector)?.classList.add('hidden');
const text = (selector, value) => { if ($(selector)) $(selector).textContent = String(value ?? ''); };
const state = {
  tokens: null, me: null, client: null, connected: false,
  view: 'auth', roomId: null, info: null, searching: false,
  ownReady: false, selectedLength: 4, horizontal: true,
  hover: null, subscriptions: [], pollTimer: null,
  pendingShot: false, pendingCommand: false, isBooting: false,
  lastError: '', finishedWinner: null, recentLogs: [],
};
let authBusy = false;
let toastSeq = 0;

function setView(view) {
  state.view = view;
  for (const name of ['auth', 'lobby', 'game']) {
    $(`#view-${name}`).classList.toggle('hidden', name !== view);
  }
}
function log(message, kind = 'info') {
  // Never log Authorization headers or JWTs.
  const line = `[${new Date().toLocaleTimeString('ru-RU')}] [${kind}] ${message}`;
  state.recentLogs.push(line);
  if (state.recentLogs.length > 160) state.recentLogs.shift();
  text('#log-list', state.recentLogs.join('\n'));
  const root = $('#log-list'); if (root) root.scrollTop = root.scrollHeight;
}
function toast(message, kind = 'info') {
  const root = $('#toast-container'); if (!root) return;
  const el = document.createElement('div');
  el.className = `toast ${kind}`;
  el.setAttribute('role', kind === 'error' ? 'alert' : 'status');
  el.textContent = message;
  el.dataset.toast = String(++toastSeq);
  root.append(el);
  setTimeout(() => el.remove(), 4600);
}
function addFeed(message) {
  const root = $('#battle-feed');
  if (!root) return;
  root.querySelector('.empty-feed')?.remove();
  const item = document.createElement('div'); item.className = 'feed-entry';
  const time = document.createElement('time'); time.textContent = new Date().toLocaleTimeString('ru-RU', {hour: '2-digit', minute: '2-digit'});
  const line = document.createElement('span'); line.textContent = message;
  item.append(time, line); root.prepend(item);
  while (root.childElementCount > 16) root.lastElementChild.remove();
}
function setConnection(connected, message) {
  state.connected = connected;
  const badge = $('#connection-badge');
  badge.classList.toggle('is-on', connected);
  badge.classList.toggle('is-off', !connected);
  text('#connection-text', message || (connected ? 'На связи' : 'Нет соединения'));
  $('#btn-find').disabled = !connected || state.searching;
  $('#btn-cancel').disabled = !connected;
  updateControls();
}
function showError(message) {
  const msg = String(message || 'Неизвестная ошибка');
  state.lastError = msg;
  log(msg, 'error'); toast(msg, 'error');
}
function parseApiError(res, body) {
  if (body && typeof body === 'object') return body.message || body.error || `HTTP ${res.status}`;
  if (typeof body === 'string' && body.length < 160) return `HTTP ${res.status}: ${body}`;
  return `Ошибка сервера: HTTP ${res.status}`;
}
async function request(path, {method = 'GET', body, authorized = true, headers = {}} = {}) {
  const h = { ...headers };
  if (body !== undefined && !h['Content-Type']) h['Content-Type'] = 'application/json';
  if (authorized && state.tokens?.accessToken) h.Authorization = `Bearer ${state.tokens.accessToken}`;
  const resp = await fetch(`${CFG.api}${path}`, { method, headers: h, body: body === undefined ? undefined : JSON.stringify(body) });
  const raw = await resp.text(); let result = raw;
  try { if (raw) result = JSON.parse(raw); } catch { /* server may respond with plaintext */ }
  if (!resp.ok) throw new Error(parseApiError(resp, result));
  return result;
}
function parseJwt(token) {
  try {
    const payload = token.split('.')[1];
    const encoded = payload.replace(/-/g, '+').replace(/_/g, '/');
    return JSON.parse(atob(encoded.padEnd(Math.ceil(encoded.length / 4) * 4, '=')));
  } catch { return null; }
}
function tokenWillExpire() {
  const expiry = parseJwt(state.tokens?.accessToken || '')?.exp;
  return typeof expiry === 'number' ? (expiry * 1000 < Date.now() + 120000) : false;
}
function saveAuth(tokens) {
  if (!tokens?.accessToken || !tokens?.username) throw new Error('Сервер прислал неполные данные авторизации');
  state.tokens = tokens; state.me = { id: tokens.id, name: tokens.username };
  // Session storage isolates each tab; avoid keeping a JWT beyond the browser tab's lifetime.
  sessionStorage.setItem('sb_auth_v2', JSON.stringify(tokens));
}
function restoreAuth() {
  try {
    let raw = sessionStorage.getItem('sb_auth_v2');
    // Migrate tokens from the original frontend (which used localStorage).
    if (!raw) raw = localStorage.getItem('sb_tokens');
    if (!raw) return false;
    saveAuth(JSON.parse(raw));
    localStorage.removeItem('sb_tokens');
    return true;
  } catch { return false; }
}
function clearAuth() {
  state.tokens = null; state.me = null;
  sessionStorage.removeItem('sb_auth_v2'); sessionStorage.removeItem('sb_room_v2');
  localStorage.removeItem('sb_tokens');
}
async function refreshAuth() {
  if (!state.tokens?.refreshToken) throw new Error('Необходимо снова войти в аккаунт');
  // AuthController.refresh uses a plain String @RequestBody, not {refreshToken:...}.
  const response = await fetch(`${CFG.api}/auth/refresh`, {
    method: 'POST', headers: { 'Content-Type': 'text/plain;charset=UTF-8' },
    body: state.tokens.refreshToken,
  });
  const raw = await response.text();
  if (!response.ok) throw new Error(`Не удалось обновить авторизацию (HTTP ${response.status})`);
  saveAuth(JSON.parse(raw)); log('Токены обновлены', 'ok');
}
async function ensureAuthFresh() {
  if (tokenWillExpire()) await refreshAuth();
}
async function authorize(loginResult) {
  saveAuth(loginResult);
  enterLobby();
  try { await connect(); } catch (err) { showError(`Не удалось подключиться: ${err.message}`); }
}
function setAuthTab(tab) {
  for (const btn of $$('[data-auth-tab]')) {
    const active = btn.dataset.authTab === tab;
    btn.classList.toggle('active', active); btn.setAttribute('aria-selected', String(active));
  }
  $('#form-login').classList.toggle('hidden', tab !== 'login');
  $('#form-register').classList.toggle('hidden', tab !== 'register');
  text('#auth-error', '');
}
async function loginSubmit(e) {
  e.preventDefault(); if (authBusy) return;
  const data = new FormData(e.target); authBusy = true;
  $('#btn-login').disabled = true; text('#auth-error', '');
  try {
    const auth = await request('/auth/login', {method:'POST',body: {username:String(data.get('username')).trim(),password:data.get('password')},authorized:false});
    await authorize(auth);
  } catch (err) { text('#auth-error', err.message); }
  finally { authBusy = false; $('#btn-login').disabled = false; }
}
async function registerSubmit(e) {
  e.preventDefault(); if (authBusy) return;
  const data = new FormData(e.target);
  if (data.get('password') !== data.get('passwordConfirmation')) { text('#auth-error', 'Пароли не совпадают'); return; }
  authBusy = true; $('#btn-register').disabled = true; text('#auth-error', '');
  try {
    const name = String(data.get('name')).trim();
    await request('/auth/register', {method:'POST',body:{id:null,name,email:String(data.get('email')).trim() || null,password:data.get('password'),passwordConfirmation:data.get('passwordConfirmation')},authorized:false});
    const auth = await request('/auth/login', {method:'POST',body:{username:name,password:data.get('password')},authorized:false});
    await authorize(auth);
    toast(`Добро пожаловать, ${name}!`, 'success');
  } catch (err) { text('#auth-error', err.message); }
  finally { authBusy = false; $('#btn-register').disabled = false; }
}

function subscribe(destination, handler) {
  if (!state.client?.connected) return;
  const sub = state.client.subscribe(destination, msg => {
    try { handler(JSON.parse(msg.body)); }
    catch (err) { log(`Некорректный ответ ${destination}: ${err.message}`, 'error'); }
  });
  state.subscriptions.push(sub);
  log(`SUBSCRIBE ${destination}`);
}
function resetSubscriptions() {
  for (const sub of state.subscriptions) try { sub.unsubscribe(); } catch { /* already disconnected */ }
  state.subscriptions = [];
}
function publish(destination, body = {}) {
  if (!state.client?.connected) { toast('Нет соединения с сервером. Подожди переподключения.', 'error'); return false; }
  state.client.publish({ destination, body: JSON.stringify(body), headers: {'content-type':'application/json'} });
  log(`SEND ${destination}`);
  return true;
}
function roomPath(action) { return `${CFG.app}/room/${state.roomId}/${action}`; }
function requestGameInfo() {
  if (state.roomId === null || state.finishedWinner !== null) return;
  publish(roomPath('get-game-info'));
}
function scheduleRefresh() {
  if (state.roomId === null) return;
  setTimeout(() => requestGameInfo(), CFG.commandRefreshMs);
  setTimeout(() => requestGameInfo(), 1250);
}
function onWsError(body) {
  const msg = typeof body === 'object' ? body?.message : String(body || 'Ошибка обработки STOMP');
  showError(msg || 'Сервер отклонил команду');
  state.pendingCommand = false; state.pendingShot = false;
  if (state.roomId !== null) {
    if (state.finishedWinner !== null && /room id.*does not exist/i.test(msg || '')) return;
    if (/already ready|not all ships|ship.*placed|готов/i.test(msg || '')) state.ownReady = false;
    updateControls(); scheduleRefresh();
    // Room id lost after a restart or after the other player won.
    if (/room id.*does not exist/i.test(msg || '') && !state.finishedWinner) {
      toast('Комната недоступна. Можно вернуться в лобби.', 'error');
    }
  } else if (state.searching) {
    // Do not assume what happened to the queue after a server error.
    setLobbyStatus('Проверь журнал ошибки; состояние очереди неизвестно.', true);
  }
}
function connectionSubscriptions() {
  resetSubscriptions();
  subscribe('/user/queue/errors', onWsError);
  // Matchmaking subscription remains active during a match: reconnect can safely restore it.
  subscribe('/user/queue/matchmaking/event', onMatchEvent);
  subscribe('/user/queue/matchmaking/roomid', onRoomId);
  if (state.roomId !== null) {
    subscribe(`/user/room/${state.roomId}/game-info`, onGameInfo);
    subscribe(`/user/room/${state.roomId}/shot-result`, onShotResult);
    requestGameInfo();
  }
}
async function connect() {
  if (state.client?.active) return;
  if (!window.SockJS || !window.StompJs?.Client) {
    throw new Error('Не загрузились библиотеки SockJS/STOMP. Проверь подключение к CDN.');
  }
  await ensureAuthFresh();
  setConnection(false, 'Подключаемся…');
  return new Promise((resolve, reject) => {
    let firstConnect = true;
    const timeout = setTimeout(() => { if (firstConnect) { firstConnect = false; reject(new Error('Сервер не ответил за 15 секунд')); } }, 15000);
    const client = new StompJs.Client({
      webSocketFactory: () => new SockJS(CFG.socket),
      reconnectDelay: 4000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
      connectionTimeout: 12000,
      beforeConnect: async () => {
        await ensureAuthFresh();
        client.connectHeaders = { Authorization: `Bearer ${state.tokens.accessToken}` };
      },
      onConnect: () => {
        log('STOMP CONNECTED', 'ok');
        setConnection(true); connectionSubscriptions();
        if (state.searching && state.roomId === null) setLobbyStatus('Соединение восстановлено. Состояние очереди неизвестно — попробуй отменить поиск.', true);
        if (firstConnect) { firstConnect = false; clearTimeout(timeout); resolve(); }
      },
      onWebSocketClose: () => { resetSubscriptions(); setConnection(false, 'Переподключаемся…'); log('WebSocket отключён', 'warning'); },
      onWebSocketError: () => log('Ошибка WebSocket-соединения', 'error'),
      onStompError: frame => {
        const message = frame.headers.message || 'STOMP error';
        showError(message);
        if (firstConnect) { firstConnect = false; clearTimeout(timeout); reject(new Error(message)); }
      },
    });
    state.client = client;
    client.activate();
  });
}
async function disconnect() {
  stopPolling(); resetSubscriptions();
  const client = state.client; state.client = null;
  setConnection(false, 'Не подключено');
  if (client) try { await client.deactivate(); } catch (err) { log(err.message, 'warning'); }
}

function setLobbyStatus(message = '', unknown = false) {
  text('#lobby-status', message);
  const searching = state.searching;
  $('#btn-find').classList.toggle('hidden', searching);
  $('#btn-cancel').classList.toggle('hidden', !searching);
  $('#lobby-search').classList.toggle('hidden', !searching);
  text('#search-title', unknown ? 'Требуется проверка' : 'Поиск соперника');
  text('#search-subtitle', unknown ? 'Очередь могла сохраниться на сервере.' : 'Ожидаем второго игрока…');
  $('#btn-find').disabled = !state.connected || searching;
}
function enterLobby() {
  state.roomId = null; state.info = null; state.finishedWinner = null;
  state.ownReady = false; state.searching = false; state.pendingCommand = false;
  sessionStorage.removeItem('sb_room_v2'); stopPolling();
  setView('lobby');
  text('#lobby-name', state.me?.name || 'Игрок');
  text('#lobby-avatar', (state.me?.name || 'И')[0].toLocaleUpperCase('ru-RU'));
  setLobbyStatus('Нажми «Найти соперника», чтобы начать.');
}
function startSearch() {
  if (!state.connected || state.searching) return;
  state.searching = true;
  setLobbyStatus('Ищем другого игрока…');
  if (!publish(`${CFG.app}/matchmaking/find-match`)) { state.searching = false; setLobbyStatus('Нет соединения'); }
}
function cancelSearch() {
  if (!state.connected || !state.searching) return;
  publish(`${CFG.app}/matchmaking/disconnect`);
  text('#lobby-status', 'Отменяем поиск…');
}
function onMatchEvent(event) {
  const type = event?.eventType;
  log(`Matchmaking: ${type || 'unknown'}`, 'info');
  if (type === 'IN_QUEUE') { state.searching = true; setLobbyStatus('Ты в очереди. Ждём второго игрока…'); }
  if (type === 'CONNECTION') { state.searching = false; if (state.view === 'lobby') setLobbyStatus('Соперник найден! Создаём комнату…'); }
  if (type === 'DISCONNECTION') { state.searching = false; if (state.view === 'lobby') setLobbyStatus('Поиск отменён.'); }
}
function onRoomId(value) {
  const id = Number(value);
  if (!Number.isInteger(id) || id < 0) { showError('Сервер прислал некорректный roomId'); return; }
  log(`Получена комната #${id}`, 'ok');
  enterRoom(id);
}
function clearGameUI() {
  state.info = null; state.hover = null; state.finishedWinner = null; state.ownReady = false;
  state.pendingShot = false; state.pendingCommand = false; state.selectedLength = 4; state.horizontal = true;
  text('#battle-feed', '');
  const empty = document.createElement('div'); empty.className = 'empty-feed'; empty.textContent = 'События боя появятся здесь.';
  $('#battle-feed').appendChild(empty);
  buildBoard('my'); buildBoard('enemy');
}
function enterRoom(id, restoring = false) {
  const different = state.roomId !== id;
  state.roomId = id; state.searching = false;
  sessionStorage.setItem('sb_room_v2', String(id));
  if (different || !state.info) clearGameUI();
  setView('game');
  text('#room-id', `#${id}`);
  text('#players-line', `${state.me?.name || 'Ты'} против соперника`);
  if (restoring) addFeed('Восстанавливаем состояние комнаты после перезагрузки.');
  if (state.client?.connected) connectionSubscriptions();
  startPolling(); drawGame();
}
function startPolling() {
  stopPolling();
  // Backend currently does NOT push updated GameInfo after place-ship, mark-ready, or clear-board.
  state.pollTimer = setInterval(() => {
    if (state.connected && state.roomId !== null && state.finishedWinner === null) requestGameInfo();
  }, CFG.pollMs);
}
function stopPolling() { if (state.pollTimer) clearInterval(state.pollTimer); state.pollTimer = null; }
function winnerName(info) {
  const value = info?.winner;
  if (typeof value === 'string' && value) return value;
  if (value && typeof value === 'object') {
    if (typeof value.value === 'string') return value.value;
    if (value.present && typeof value.get === 'string') return value.get;
  }
  return null;
}
function onGameInfo(info) {
  if (state.roomId === null || !info?.gridInfo || !Array.isArray(info.gridInfo.player1Grid) || !Array.isArray(info.gridInfo.player2Grid)) {
    log('Получен неполный GameInfo', 'warning'); return;
  }
  if (state.finishedWinner !== null) return; // do not overwrite final result
  state.info = info;
  const winner = winnerName(info);
  if (winner) finishGame(winner);
  else drawGame();
}
function onShotResult(payload) {
  const x = payload?.coords?.x, y = payload?.coords?.y;
  const point = Number.isInteger(x) && Number.isInteger(y) ? `${COLUMNS[x]}${y+1}` : '—';
  const label = { SHOT: 'Попадание!', MISS: 'Мимо.', ALREADY_SHOT: 'По этой клетке уже стреляли.' }[payload?.result] || `Результат: ${payload?.result}`;
  addFeed(`${point}: ${label}`);
  state.pendingShot = false;
  const winner = typeof payload?.winner === 'string' ? payload.winner : winnerName(payload);
  if (winner) finishGame(winner);
  else { scheduleRefresh(); drawGame(); }
}
function finishGame(winner) {
  if (state.finishedWinner !== null) return;
  state.finishedWinner = winner;
  stopPolling();
  sessionStorage.removeItem('sb_room_v2');
  const won = winner === state.me?.name;
  addFeed(won ? 'Победа! Вражеский флот уничтожен.' : `Матч завершён. Победитель: ${winner}`);
  toast(won ? 'Победа! Все корабли соперника уничтожены.' : `Матч завершён. Победил ${winner}.`, won ? 'success' : 'info');
  drawGame();
}
function cellIndex(cells) {
  const cellsByPos = new Map();
  for (const cell of cells || []) if (Number.isInteger(cell.x) && Number.isInteger(cell.y)) cellsByPos.set(`${cell.x}:${cell.y}`, cell);
  return cellsByPos;
}
function countFleet(cells) {
  const byShip = new Map();
  for (const cell of cells || []) {
    if (cell.state !== 'SHIP' && cell.state !== 'HIT' && cell.state !== 'SUNK') continue;
    if (!Number.isInteger(cell.shipId) || cell.shipId < 0) continue;
    byShip.set(cell.shipId, (byShip.get(cell.shipId) || 0) + 1);
  }
  const counts = { 1:0, 2:0, 3:0, 4:0 };
  for (const length of byShip.values()) if (length in counts) counts[length]++;
  return counts;
}
function countsPlaced(cells) {
  // In SHIP_PLACEMENT, all my ship cells are visible and no shots have been fired.
  return countFleet(cells);
}
function totalPlaced(counts) { return Object.values(counts).reduce((a,b) => a+b,0); }
function fleetComplete(counts) { return Object.entries(FLEET).every(([size, quantity]) => counts[size] === quantity); }
function positionPreview(cells, x, y, length, horizontal) {
  const grid = cellIndex(cells);
  const coords = Array.from({length}, (_, i) => ({x:x+(horizontal ? i:0), y:y+(horizontal ? 0:i)}));
  let valid = true;
  for (const cell of coords) {
    if (cell.x < 0 || cell.y < 0 || cell.x >= CFG.size || cell.y >= CFG.size) { valid = false; continue; }
    for (let dx=-1;dx<=1;dx++) for(let dy=-1;dy<=1;dy++) {
      if (grid.get(`${cell.x+dx}:${cell.y+dy}`)?.state === 'SHIP') valid = false;
    }
  }
  return { valid, coords };
}
function buildBoard(side) {
  const root = $(`#board-${side}`); root.textContent = '';
  const corner = document.createElement('div'); corner.className = 'axis corner'; root.append(corner);
  for (const letter of COLUMNS) { const el=document.createElement('div');el.className='axis';el.textContent=letter;root.append(el); }
  for(let y=0;y<CFG.size;y++) {
    const label=document.createElement('div');label.className='axis';label.textContent=String(y+1);root.append(label);
    for(let x=0;x<CFG.size;x++) {
      const el=document.createElement('button');el.type='button';el.className='cell';
      el.dataset.x=String(x);el.dataset.y=String(y);
      el.setAttribute('role','gridcell');el.setAttribute('aria-label', `${side==='my'?'Своё':'Вражеское'} поле ${COLUMNS[x]}${y+1}, вода`);
      root.append(el);
    }
  }
}
function updateBoard(side, cells, interactive) {
  const root = $(`#board-${side}`), grid = cellIndex(cells);
  for (const el of root.querySelectorAll('.cell')) {
    const x=Number(el.dataset.x),y=Number(el.dataset.y);
    const status=grid.get(`${x}:${y}`)?.state || 'EMPTY';
    el.className=`cell ${status.toLowerCase()}`;
    el.disabled=!interactive || (side==='enemy' && status !== 'EMPTY');
    el.classList.toggle('can-click', !el.disabled);
    el.setAttribute('aria-label', `${side==='my'?'Своё':'Вражеское'} поле ${COLUMNS[x]}${y+1}: ${CELL_NAMES[status] || status}`);
  }
}
function paintPreview() {
  const root=$('#board-my');
  for (const el of root.querySelectorAll('.cell')) el.classList.remove('preview-good','preview-bad');
  if (!state.hover || state.info?.gameStatus !== 'SHIP_PLACEMENT' || state.ownReady || !state.connected) return;
  const preview=positionPreview(state.info.gridInfo.player1Grid,state.hover.x,state.hover.y,state.selectedLength,state.horizontal);
  for (const point of preview.coords) {
    if (point.x < 0 || point.y < 0 || point.x >= CFG.size || point.y >= CFG.size) continue;
    root.querySelector(`.cell[data-x="${point.x}"][data-y="${point.y}"]`)?.classList.add(preview.valid?'preview-good':'preview-bad');
  }
}
function drawFleetPicker() {
  const picker = $('#fleet-picker'), placed=countsPlaced(state.info?.gridInfo?.player1Grid || []);
  picker.textContent='';
  if(placed[state.selectedLength] >= FLEET[state.selectedLength]) {
    const next = [4,3,2,1].find(length => placed[length] < FLEET[length]);
    if(next) state.selectedLength = next;
  }
  for (const length of [4,3,2,1]) {
    const remaining=Math.max(0,FLEET[length]-(placed[length]||0));
    const btn=document.createElement('button');btn.type='button';btn.className='fleet-item';
    if(state.selectedLength===length)btn.classList.add('selected');
    if(!remaining)btn.classList.add('exhausted');
    btn.disabled=!remaining || state.ownReady || !state.connected || state.info?.gameStatus!=='SHIP_PLACEMENT';
    btn.setAttribute('aria-pressed',String(state.selectedLength===length));
    const bars=document.createElement('span');bars.className='ship-bars';
    for(let j=0;j<length;j++) bars.appendChild(document.createElement('i'));
    const title=document.createElement('span');title.className='fleet-name';title.textContent=SHIP_NAMES[length];
    const qty=document.createElement('span');qty.className='fleet-remaining';qty.textContent=`${remaining} / ${FLEET[length]}`;
    btn.append(bars,title,qty);btn.addEventListener('click',()=>{state.selectedLength=length;drawFleetPicker();paintPreview();});
    picker.append(btn);
  }
  const placedTotal=totalPlaced(placed);
  text('#my-fleet-count',`${placedTotal} / 10 кораблей`);
  text('#placement-note',fleetComplete(placed)?'Флот готов. Можно начать бой.':`Размещено ${placedTotal} из 10 кораблей.`);
  return placed;
}
function updateControls(counts = countsPlaced(state.info?.gridInfo?.player1Grid || [])) {
  const placement=state.info?.gameStatus==='SHIP_PLACEMENT' && state.finishedWinner===null;
  $('#placement-panel').classList.toggle('hidden',!placement);
  $('#btn-ready').classList.toggle('hidden',state.ownReady);
  $('#btn-unready').classList.toggle('hidden',!state.ownReady);
  const canAct=state.connected && placement && !state.pendingCommand;
  $('#btn-ready').disabled=!canAct || !fleetComplete(counts) || state.ownReady;
  $('#btn-unready').disabled=!canAct || !state.ownReady;
  $('#btn-clear').disabled=!canAct || state.ownReady;
  $('#btn-rotate').disabled=!canAct || state.ownReady;
  text('#orientation-label',state.horizontal?'Горизонтально':'Вертикально');
  text('#tip-text',placement?'Подсветка зелёная — корабль можно поставить. Красная — нарушает правила.':'Попадание даёт ещё один выстрел; промах передаёт ход сопернику.');
}
function drawGame() {
  const info=state.info;
  const placement=!info || info.gameStatus==='SHIP_PLACEMENT';
  const inProgress=info?.gameStatus==='IN_PROGRESS';
  const finished=state.finishedWinner!==null || info?.gameStatus==='FINISHED';
  const counts=drawFleetPicker();
  if(info) {
    const players=Array.isArray(info.players)?info.players:[];
    const opponent=players.find(p=>p!==state.me?.name);
    text('#players-line',`${state.me?.name || 'Ты'}  ×  ${opponent || 'соперник'}`);
    const mine=info.gridInfo?.player1Grid||[];const enemy=info.gridInfo?.player2Grid||[];
    updateBoard('my',mine,placement&&!state.ownReady&&state.connected);
    updateBoard('enemy',enemy,inProgress&&!state.pendingShot&&state.connected);
    const fired=enemy.filter(c=>['HIT','SUNK','MISS'].includes(c.state)).length;
    text('#enemy-shots',`${fired} выстрелов`);
  } else {
    updateBoard('my',[],false);updateBoard('enemy',[],false);text('#enemy-shots','0 выстрелов');
  }
  let title='Подготовка к бою', detail='Расставь свой флот на поле слева.',phase='Подготовка';
  if(!info){title='Получаем данные комнаты…';detail='Ожидаем состояние матча от сервера.';phase='Соединение';}
  if(placement&&state.ownReady){title='Флот готов к бою';detail='Ждём, пока второй капитан завершит расстановку.';phase='Ожидание';}
  if(inProgress){title='Бой начался!';detail='Выбери клетку на вражеском поле. Сервер проверит, чей сейчас ход.';phase='Бой';}
  if(inProgress&&typeof info?.currentTurn==='string') detail=info.currentTurn===state.me?.name?'Твой ход — атакуй поле противника.':'Ход соперника — ожидай выстрела.';
  if(finished){const won=state.finishedWinner===state.me?.name;title=won?'Победа!':'Матч завершён';detail=state.finishedWinner?`Победитель — ${state.finishedWinner}.`:'Бой завершён.';phase='Финиш';}
  text('#mission-title',title);text('#mission-subtitle',detail);text('#mission-indicator',phase.toUpperCase());text('#game-phase',phase);
  updateControls(counts);paintPreview();
}
function tryPlace(x,y) {
  if(state.info?.gameStatus!=='SHIP_PLACEMENT'||state.ownReady||!state.connected||state.pendingCommand)return;
  const placed=countsPlaced(state.info.gridInfo.player1Grid);
  if(placed[state.selectedLength]>=FLEET[state.selectedLength]){toast('Корабли этой длины уже размещены.','error');return;}
  const preview=positionPreview(state.info.gridInfo.player1Grid,x,y,state.selectedLength,state.horizontal);
  if(!preview.valid){toast('Нельзя разместить корабль здесь: клетки заняты, соседствуют с другим кораблём или выходят за границу.','error');return;}
  if(publish(roomPath('place-ship'),{position:{x,y},length:state.selectedLength,horizontal:state.horizontal})){
    state.hover=null;commandCooldown();addFeed(`Размещён ${SHIP_NAMES[state.selectedLength].toLowerCase()} (${state.selectedLength} клетки) с ${COLUMNS[x]}${y+1}.`);scheduleRefresh();
  }
}
function commandCooldown() {state.pendingCommand=true;updateControls();setTimeout(()=>{state.pendingCommand=false;updateControls();},650);}
function ready() {
  const placed=countsPlaced(state.info?.gridInfo?.player1Grid||[]);
  if(!fleetComplete(placed)){toast('Сначала размести весь флот.','error');return;}
  if(publish(roomPath('mark-ready'))){state.ownReady=true;commandCooldown();addFeed('Готовность отправлена серверу.');drawGame();scheduleRefresh();}
}
function unready(){if(publish(roomPath('unmark-ready'))){state.ownReady=false;commandCooldown();drawGame();scheduleRefresh();}}
function clearBoard(){if(!state.info||!confirm('Убрать все размещённые корабли?'))return;if(publish(roomPath('clear-board'))){commandCooldown();scheduleRefresh();addFeed('Запрошена очистка поля.');}}
function shoot(x,y) {
  if(state.info?.gameStatus!=='IN_PROGRESS'||state.finishedWinner!==null||state.pendingShot||!state.connected)return;
  const cell=cellIndex(state.info.gridInfo.player2Grid).get(`${x}:${y}`);
  if(cell?.state&&cell.state!=='EMPTY')return;
  if(state.info.currentTurn&&state.info.currentTurn!==state.me?.name){toast('Сейчас ход соперника.','error');return;}
  if(publish(roomPath('make-turn'),{position:{x,y}})){
    state.pendingShot=true;drawGame();
    // Backend broadcasts shot-result to BOTH players; allow retry on lost message.
    setTimeout(()=>{state.pendingShot=false;drawGame();},1300);
  }
}
function leaveRoom() {
  if(!confirm('Сервер пока не поддерживает выход из активной комнаты. Вернуться только в лобби? Матч на сервере может продолжаться.'))return;
  const oldRoomId=state.roomId;
  enterLobby();
  if(state.connected) connectionSubscriptions();
  log(`Локальный выход из комнаты #${oldRoomId}. Серверный матч НЕ завершён.`, 'warning');
  toast('Ты вернулся в лобби. Серверный матч не был завершён.', 'info');
}
function handleBoardClick(side,event) {
  const target=event.target.closest('.cell');if(!target)return;
  const x=Number(target.dataset.x),y=Number(target.dataset.y);
  if(side==='my')tryPlace(x,y);else shoot(x,y);
}
function handleBoardHover(e) {
  const target=e.target.closest('.cell');
  if(target){state.hover={x:Number(target.dataset.x),y:Number(target.dataset.y)};paintPreview();}
}
async function logout() {
  if(state.searching&&state.connected) publish(`${CFG.app}/matchmaking/disconnect`);
  await disconnect(); clearAuth(); state.roomId=null; state.searching=false;setView('auth');
}
function bindUI() {
  $$('[data-auth-tab]').forEach(btn=>btn.addEventListener('click',()=>setAuthTab(btn.dataset.authTab)));
  $('#form-login').addEventListener('submit',loginSubmit);
  $('#form-register').addEventListener('submit',registerSubmit);
  $('#btn-find').addEventListener('click',startSearch);
  $('#btn-cancel').addEventListener('click',cancelSearch);
  $('#btn-logout').addEventListener('click',logout);
  $('#btn-rotate').addEventListener('click',()=>{state.horizontal=!state.horizontal;updateControls();paintPreview();});
  $('#btn-ready').addEventListener('click',ready);
  $('#btn-unready').addEventListener('click',unready);
  $('#btn-clear').addEventListener('click',clearBoard);
  $('#btn-return-lobby').addEventListener('click',leaveRoom);
  $('#board-my').addEventListener('click',e=>handleBoardClick('my',e));
  $('#board-enemy').addEventListener('click',e=>handleBoardClick('enemy',e));
  $('#board-my').addEventListener('pointerover',handleBoardHover);
  $('#board-my').addEventListener('focusin',handleBoardHover);
  $('#board-my').addEventListener('pointerleave',()=>{state.hover=null;paintPreview();});
  document.addEventListener('keydown',e=>{
    if((e.key==='r'||e.key==='R'||e.key==='к'||e.key==='К')&&state.view==='game'&&state.info?.gameStatus==='SHIP_PLACEMENT'&&!state.ownReady&&!['INPUT','TEXTAREA','SELECT'].includes(document.activeElement?.tagName)){
      e.preventDefault();state.horizontal=!state.horizontal;updateControls();paintPreview();
    }
    if(e.key==='Escape')hide('#log-overlay');
  });
  $('#btn-logs').addEventListener('click',()=>show('#log-overlay'));
  $('#btn-close-logs').addEventListener('click',()=>hide('#log-overlay'));
  $('#log-overlay').addEventListener('click',e=>{if(e.target===$('#log-overlay'))hide('#log-overlay');});
  $('#btn-clear-logs').addEventListener('click',()=>{state.recentLogs=[];text('#log-list','');});
  $('#btn-copy-logs').addEventListener('click',async()=>{try{await navigator.clipboard.writeText(state.recentLogs.join('\n'));toast('Журнал скопирован.','success');}catch{toast('Не удалось скопировать журнал.','error');}});
}
async function init() {
  bindUI();buildBoard('my');buildBoard('enemy');setView('auth');
  if(!restoreAuth())return;
  try {
    await ensureAuthFresh();
    enterLobby();
    const savedRoom = sessionStorage.getItem('sb_room_v2');
    if(savedRoom!==null&&/^\d+$/.test(savedRoom))enterRoom(Number(savedRoom),true);
    await connect();
  }catch(err){
    log(`Восстановление сессии: ${err.message}`,'error');
    if(!state.tokens){setView('auth');return;}
    toast(`Проверь соединение: ${err.message}`,'error');
  }
}
if(typeof window!=='undefined')window.addEventListener('DOMContentLoaded',init);
// Visibility changes do not attempt to cancel a game: backend lacks room leave/disconnect.
// Export pure helpers only for dependency-free Node tests; no effect in the browser.
if(typeof module!=='undefined'&&module.exports){module.exports={cellIndex,countFleet,countsPlaced,fleetComplete,positionPreview,winnerName};}
