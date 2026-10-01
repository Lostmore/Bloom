'use strict';

const selector = document.querySelector('#asyncapi-service');
const status = document.querySelector('#asyncapi-status');
const download = document.querySelector('#asyncapi-download');
const container = document.querySelector('#asyncapi');
let generation = 0;
let documentSchema;
let currentView = 'start';
let rendered = false;

function element(tag, text, className) {
  const node = document.createElement(tag);
  if (text !== undefined) node.textContent = text;
  if (className) node.className = className;
  return node;
}

function resolve(node) {
  const visited = new Set();
  while (node?.$ref) {
    const ref = node.$ref;
    if (!ref.startsWith('#/') || visited.has(ref)) return {};
    visited.add(ref);
    node = ref.slice(2).split('/').reduce((value, key) => value?.[key.replace(/~1/g, '/').replace(/~0/g, '~')], documentSchema);
  }
  return node || {};
}

function showView(view) {
  currentView = view;
  document.querySelectorAll('.view').forEach(node => { node.hidden = node.id !== `view-${view}`; });
  document.querySelectorAll('.section-nav [data-view]').forEach(node => {
    node.setAttribute('aria-pressed', String(node.dataset.view === view));
  });
  if (view === 'spec' && documentSchema && !rendered) {
    const target = element('div');
    container.replaceChildren(target);
    AsyncApiStandalone.render({ schema: documentSchema, config: { show: { sidebar: false } } }, target);
    rendered = true;
  }
}

function renderEvents() {
  const list = document.querySelector('#event-list');
  const detail = document.querySelector('#event-detail');
  list.replaceChildren();
  detail.replaceChildren();
  let count = 0;
  for (const rawOperation of Object.values(documentSchema.operations || {})) {
    const operation = resolve(rawOperation);
    const channel = resolve(operation.channel);
    const messages = operation.messages || Object.values(channel.messages || {});
    for (const reference of messages) {
      const message = resolve(reference);
      const incoming = operation.action === 'send';
      const direction = incoming ? 'ВЫ ПОЛУЧАЕТЕ' : 'ВЫ ОТПРАВЛЯЕТЕ';
      const badgeClass = incoming ? 'direction incoming' : 'direction';
      const button = element('button', undefined, 'event-choice');
      button.type = 'button';
      button.setAttribute('aria-pressed', 'false');
      button.append(element('span', direction, badgeClass), element('strong', message.title || message.name || operation.summary));
      button.addEventListener('click', () => {
        list.querySelectorAll('button').forEach(item => item.setAttribute('aria-pressed', String(item === button)));
        detail.replaceChildren(element('span', direction, badgeClass),
          element('h3', message.title || message.name || operation.summary),
          element('p', operation.summary || '', 'muted'));
        if (message.description) detail.append(element('p', message.description));
        detail.append(element('p', `Канал: ${channel.address || 'см. спецификацию'}`, 'muted'));
        if (message.examples?.length) {
          const header = element('div', undefined, 'code-header');
          const copy = element('button', 'Копировать JSON', 'copy');
          copy.type = 'button';
          copy.dataset.copy = 'event-example';
          header.append(element('span', 'Пример сообщения · JSON'), copy);
          const pre = element('pre');
          const code = element('code', JSON.stringify(message.examples[0].payload, null, 2));
          code.id = 'event-example';
          pre.append(code);
          detail.append(header, pre);
        } else detail.append(element('p', 'Пример ещё не добавлен в контракт.', 'muted'));
        const payload = resolve(message.payload);
        const properties = payload.properties || {};
        if (Object.keys(properties).length) {
          detail.append(element('h3', 'Поля сообщения'));
          const table = element('table', undefined, 'field-table');
          const head = element('tr');
          ['Поле', 'Тип', 'Описание'].forEach(title => head.append(element('th', title)));
          const thead = element('thead'); thead.append(head); table.append(thead);
          const tbody = element('tbody');
          const fields = Object.entries(properties).map(([name, schema]) => [name, schema, payload.required?.includes(name)]);
          const body = resolve(properties.payload);
          for (const [name, schema] of Object.entries(body.properties || {})) {
            fields.push([`payload.${name}`, schema, body.required?.includes(name)]);
          }
          for (const [name, rawProperty, required] of fields) {
            const property = resolve(rawProperty);
            const row = element('tr');
            const field = element('td'); field.append(element('code', name));
            if (required) field.append(element('small', 'Обязательное'));
            row.append(field, element('td', property.type || 'object'), element('td', property.description || 'Подробности в спецификации.'));
            tbody.append(row);
          }
          table.append(tbody); detail.append(table);
        }
        const full = element('button', 'Открыть полную схему', 'secondary');
        full.type = 'button'; full.dataset.view = 'spec'; detail.append(full);
      });
      list.append(button);
      count++;
    }
  }
  document.querySelector('#event-count').textContent = count;
  if (count) list.querySelector('button').click();
  else detail.append(element('p', 'В документе пока нет операций. Открой полную спецификацию.'));
}

function renderGuide() {
  const chat = documentSchema['x-bloom-guide'] === 'chat-websocket-v2';
  document.querySelector('#connection-guide').hidden = !chat;
  document.querySelector('#generic-guide').hidden = chat;
  document.querySelector('#limitations').hidden = !chat;
  if (!chat) return;
  const address = `${location.protocol === 'https:' ? 'wss' : 'ws'}://${location.host}/api/v1/ws`;
  document.querySelector('#connection-url').textContent = `${address}?token=<accessToken>`;
  document.querySelector('#connection-code').textContent = `const accessToken = 'ACCESS_TOKEN_ПОЛЬЗОВАТЕЛЯ';
const roomId = 15; // ID вашей комнаты из HTTP API
const url = new URL('${address}');
url.searchParams.set('token', accessToken);

const socket = new WebSocket(url);
socket.onopen = () => {
  socket.send(JSON.stringify({
    type: 'new_message',
    payload: { room_id: roomId, content: 'Привет!' }
  }));
};
socket.onmessage = ({ data }) => {
  const event = JSON.parse(data);
  if (event.type === 'error') {
    console.error(event.error); // Ошибка без request_id.
    return;
  }
  const message = event.payload;
  switch (event.type) {
    case 'new_message':
      if (message.id === 0) {
        // Обновить список комнат через HTTP, не добавлять в историю.
        return;
      }
      // Обновить переписку message.room_id; убрать дубли по message.id.
      break;
    case 'edit_message':
    case 'delete_message':
    case 'mark_as_read':
      // Обновить message.message_id; точные времена получить из HTTP-истории.
      break;
    case 'typing':
      // Показать message.is_typing для message.user_id, скрыть по таймауту.
      break;
    case 'presence':
      // Обновить message.status для message.user_id.
      break;
  }
};
socket.onclose = () => {
  // Переподключиться с задержкой и загрузить пропущенную историю.
};`;
}

async function loadSpecification() {
  const attempt = ++generation;
  status.hidden = false;
  status.textContent = 'Загружаем описание сервиса…';
  download.hidden = true;
  documentSchema = undefined;
  rendered = false;
  document.querySelector('#document-content').hidden = true;
  container.replaceChildren();
  try {
    const url = new URL(selector.value, location.origin);
    if (url.origin !== location.origin || !url.pathname.startsWith('/docs/asyncapi/specs/')) {
      throw new Error('Недопустимый адрес документации.');
    }
    const response = await fetch(url, { signal: AbortSignal.timeout(10000) });
    if (!response.ok) throw new Error(`Описание сервиса недоступно (HTTP ${response.status}). Можно выбрать другой сервис или повторить загрузку.`);
    const schema = await response.json();
    if (attempt !== generation) return;
    if (!String(schema.asyncapi).startsWith('3.')) throw new Error('Ожидается документ AsyncAPI 3.');
    // Use the browser address, not localhost or an internal Docker hostname.
    if (schema.servers?.gateway && ['ws', 'wss'].includes(schema.servers.gateway.protocol)) {
      schema.servers.gateway.host = location.host;
      schema.servers.gateway.protocol = location.protocol === 'https:' ? 'wss' : 'ws';
      delete schema.servers.gateway.variables;
    }
    documentSchema = schema;
    document.querySelector('#document-title').textContent = schema.info?.title || 'События сервиса';
    document.querySelector('#document-version').textContent = `AsyncAPI ${schema.asyncapi} · v${schema.info?.version || '1'}`;
    renderGuide();
    renderEvents();
    document.querySelector('#document-content').hidden = false;
    showView(currentView);
    download.href = url.pathname;
    download.hidden = false;
    status.hidden = true;
  } catch (error) {
    if (attempt !== generation) return;
    status.textContent = error.message || 'Не удалось загрузить AsyncAPI.';
  }
}

async function initializeAsyncApi() {
  try {
    const response = await fetch('/docs/asyncapi/services', { signal: AbortSignal.timeout(10000) });
    if (!response.ok) throw new Error('Не удалось получить список сервисов.');
    const services = await response.json();
    selector.replaceChildren();
    for (const service of services) {
      const option = document.createElement('option');
      option.value = service.url;
      option.textContent = service.name;
      selector.append(option);
    }
    selector.disabled = services.length === 0;
    if (!services.length) throw new Error('AsyncAPI-документы пока не подключены.');
    await loadSpecification();
  } catch (error) {
    status.hidden = false;
    status.textContent = error.message;
  }
}

selector.addEventListener('change', loadSpecification);
document.addEventListener('click', async event => {
  const viewButton = event.target.closest('[data-view]');
  if (viewButton) showView(viewButton.dataset.view);
  const copyButton = event.target.closest('[data-copy]');
  if (!copyButton) return;
  const source = document.getElementById(copyButton.dataset.copy);
  if (!source) return;
  const text = source.textContent;
  const previous = copyButton.textContent;
  try {
    if (window.isSecureContext && navigator.clipboard) await navigator.clipboard.writeText(text);
    else {
      // LAN HTTP pages cannot use Clipboard API; keep copying available there too.
      const input = document.createElement('textarea');
      input.value = text;
      input.style.position = 'fixed'; input.style.opacity = '0';
      document.body.append(input); input.select();
      const copied = document.execCommand('copy'); input.remove();
      if (!copied) throw new Error('copy failed');
      copyButton.focus();
    }
    copyButton.textContent = 'Скопировано';
    document.querySelector('#copy-status').textContent = 'Скопировано в буфер обмена';
  } catch (_) {
    const selection = window.getSelection();
    const range = document.createRange(); range.selectNodeContents(source);
    selection.removeAllRanges(); selection.addRange(range);
    copyButton.textContent = 'Нажми Ctrl+C';
  }
  setTimeout(() => { copyButton.textContent = previous; }, 2000);
});
document.querySelector('#asyncapi-reload').addEventListener('click', () => {
  if (selector.options.length) loadSpecification(); else initializeAsyncApi();
});
initializeAsyncApi();
