/* No framework, CDN or backend required. data.js is generated from the repository. */
(() => {
  "use strict";
  const $ = (selector) => document.querySelector(selector);
  const esc = (value) => String(value ?? "").replace(/[&<>"']/g, (character) => ({"&":"&amp;","<":"&lt;",">":"&gt;",'"':"&quot;","'":"&#39;"}[character]));
  let data = window.BLOOM_ARCHITECTURE;
  if (!data) { $("#detail").textContent = "Не найден data.js. Запустите генератор карты."; return; }
  let selected = null, tab = "overview", filter = "all", query = "", step = 0, playing = false, timer = null, activeStep = false;
  let scenarioId = data.scenarios[0].id;
  let zoom = 1, pan = {x: 0, y: 0}, drag = null;
  const reduced = matchMedia("(prefers-reduced-motion: reduce)").matches;
  let motion = !reduced;
  const icons = {java:"♧", go:"◇", database:"▱", event:"⌘", storage:"▤", client:"▯"};
  const technologies = {java:"JAVA · SPRING", go:"GO · NET/HTTP", database:"POSTGRESQL", event:"EVENT BROKER", storage:"DOCKER VOLUME", client:"КЛИЕНТ"};
  const positions = {client:[35,345], "api-gateway":[295,345], identity:[590,75], users:[590,255], chat:[590,435], media:[590,615], "db-identity":[960,75], "db-users":[960,255], "db-chat":[960,435], "db-media":[960,615], kafka:[590,820], files:[960,820]};
  const statusLabels = {implemented:"Есть реализация", stub:"Заготовка", concept:"Участник сценария"};
  const layerLabels = {controller:"Контроллеры · HTTP вход", delivery:"Обработчики · HTTP / WS", service:"Бизнес-логика", repository:"Доступ к данным", security:"Авторизация и защита", client:"HTTP-клиенты других сервисов", events:"Публикация событий", worker:"Фоновые задачи", domain:"Доменные типы", model:"Модели", dto:"Запросы и ответы", config:"Конфигурация", application:"Приложение"};
  const scenario = () => data.scenarios.find((item) => item.id === scenarioId) || data.scenarios[0];
  const node = (id) => data.nodes.find((item) => item.id === id);
  const currentEdge = () => activeStep ? scenario().steps[step]?.edge : null;
  const primaryNodes = () => data.nodes.filter((item) => item.state !== "stub");

  function sources(source) {
    if (!source) return "";
    const value = `${source.file}:${source.line}`;
    return `<button class="file-link" data-copy="${esc(value)}" title="Скопировать путь к исходнику">↗ ${esc(value)}${source.found ? "" : " · требует сверки"}</button>`;
  }

  function fit() {
    const viewport = $("#viewport");
    zoom = Math.min((viewport.clientWidth - 35) / 1200, (viewport.clientHeight - 105) / 940, 1.1);
    pan = {x: (viewport.clientWidth - 1200 * zoom) / 2, y: 60};
    transform();
  }

  function transform() {
    $("#world").style.transform = `translate(${pan.x}px, ${pan.y}px) scale(${zoom})`;
    $("#zoom-value").textContent = `${Math.round(zoom * 100)}%`;
  }

  function changeZoom(multiplier) {
    const viewport = $("#viewport");
    const next = Math.max(.25, Math.min(1.8, zoom * multiplier));
    const center = {x: viewport.clientWidth / 2, y: viewport.clientHeight / 2};
    pan = {x: center.x - (center.x - pan.x) * next / zoom, y: center.y - (center.y - pan.y) * next / zoom};
    zoom = next;
    transform();
  }

  function edgePath(edge) {
    const start = positions[edge.from], finish = positions[edge.to];
    if (!start || !finish) return "";
    const sx = start[0] + 190, sy = start[1] + 51, tx = finish[0], ty = finish[1] + 51;
    if (edge.from === "users" && edge.to === "identity") {
      return `M ${start[0]+95} ${start[1]} C ${start[0]+95} ${start[1]-65}, ${finish[0]+95} ${finish[1]+166}, ${finish[0]+95} ${finish[1]+103}`;
    }
    if (edge.from === "users" && edge.to === "media") {
      return `M ${sx} ${sy} C 880 ${sy}, 880 ${ty}, ${finish[0]+190} ${ty}`;
    }
    if (edge.to === "kafka") {
      const index = ["identity", "users", "chat"].indexOf(edge.from);
      const lane = 825 + index * 32;
      const endY = finish[1] + 23 + index * 26;
      return `M ${sx} ${sy+20} H ${lane-12} Q ${lane} ${sy+20} ${lane} ${sy+32} V ${endY-12} Q ${lane} ${endY} ${lane-12} ${endY} H ${finish[0]+190}`;
    }
    if (edge.from === "kafka") {
      return `M ${start[0]+75} ${start[1]} C ${start[0]+75} ${start[1]-40}, ${finish[0]+75} ${finish[1]+143}, ${finish[0]+75} ${finish[1]+103}`;
    }
    if (edge.to === "files") return `M ${sx} ${sy+17} C ${sx+70} ${sy+17}, ${tx-70} ${ty}, ${tx} ${ty}`;
    return `M ${sx} ${sy} C ${sx+65} ${sy}, ${tx-65} ${ty}, ${tx} ${ty}`;
  }

  function renderMap() {
    let extra = 0;
    for (const item of primaryNodes()) if (!positions[item.id]) positions[item.id] = [35, 80 + extra++ * 130];
    $("#nodes").innerHTML = `<span class="lane-title" style="left:35px;top:26px">01 / КЛИЕНТ</span><span class="lane-title" style="left:295px;top:26px">02 / ВХОД</span><span class="lane-title" style="left:590px;top:26px">03 / СЕРВИСЫ</span><span class="lane-title" style="left:960px;top:26px">04 / ДАННЫЕ</span>` + primaryNodes().map((item) => {
      const [x, y] = positions[item.id];
      const warning = data.notes.some((note) => note.service === item.id);
      return `<button class="node ${item.type}" data-node="${esc(item.id)}" style="left:${x}px;top:${y}px" aria-label="${esc(item.title)}: открыть детали"><div class="node-head"><span class="node-icon">${icons[item.type] || "◇"}</span><span><strong>${esc(item.title)}</strong><span class="node-tech">${technologies[item.type] || "SERVICE"}</span></span></div><div class="node-bottom"><span>${item.port ? ":"+item.port : item.type === "database" ? "Логическая база" : item.type === "event" ? "Асинхронно" : item.type === "client" ? "HTTP / WebSocket" : "Постоянные файлы"}</span><span class="${warning ? "node-warning" : ""}">${warning ? "△ Есть нюансы" : item.endpoints?.length ? item.endpoints.length + " API" : "↗"}</span></div></button>`;
    }).join("");
    $("#connections").innerHTML = `<defs><marker id="arrow" markerWidth="6" markerHeight="6" refX="5" refY="3" orient="auto-start-reverse"><path d="M0,0 L6,3 L0,6" fill="#bcabc8"/></marker></defs>` + data.edges.map((edge) => {
      const path = edgePath(edge);
      if (!path) return "";
      return `<g class="edge ${esc(edge.kind)}" data-edge="${esc(edge.id)}"><path id="path-${esc(edge.id)}" class="edge-line" d="${path}" marker-end="url(#arrow)"/><path class="edge-hit" d="${path}" tabindex="0" role="button" aria-label="${esc(edge.label)}: ${esc(node(edge.from)?.title)} → ${esc(node(edge.to)?.title)}"/><text class="edge-label"><textPath href="#path-${esc(edge.id)}" startOffset="49%" text-anchor="middle">${esc(edge.label)}</textPath></text></g>`;
    }).join("");
    updateFocus();
  }

  function updateFocus() {
    const connection = data.edges.find((edge) => edge.id === selected);
    const related = new Set(selected ? [selected] : []);
    for (const edge of data.edges) if (edge.from === selected || edge.to === selected || edge === connection) { related.add(edge.from); related.add(edge.to); }
    const matches = new Set(data.nodes.filter((item) => `${item.title} ${item.description} ${(item.endpoints||[]).map(e=>e.path).join(" ")}`.toLowerCase().includes(query)).map(item=>item.id));
    const current = data.edges.find((edge) => edge.id === currentEdge());
    document.querySelectorAll(".node").forEach((element) => {
      const id = element.dataset.node;
      element.classList.toggle("selected", id === selected);
      element.classList.toggle("step-active", !!current && (current.from === id || current.to === id));
      element.classList.toggle("faded", query ? !matches.has(id) : current ? current.from !== id && current.to !== id : selected ? !related.has(id) : false);
    });
    $("#no-results").hidden = !query || matches.size > 0;
    document.querySelectorAll(".edge").forEach((element) => {
      const edge = data.edges.find((item) => item.id === element.dataset.edge);
      const typeMatches = filter === "all" || (filter === "http" ? ["http","route"].includes(edge.kind) : edge.kind === filter);
      const focused = query ? matches.has(edge.from) || matches.has(edge.to) : current ? edge.id === current.id : !selected || edge.from === selected || edge.to === selected || edge === connection;
      element.classList.toggle("faded", !typeMatches || !focused);
      element.classList.toggle("active", current?.id === edge.id);
      element.classList.toggle("show-label", edge.id === selected);
      element.querySelector(".packet")?.remove();
      if (current?.id === edge.id && motion) {
        const circle = document.createElementNS("http://www.w3.org/2000/svg", "circle");
        circle.setAttribute("r", "4"); circle.setAttribute("class", "packet");
        const reversed = scenario().steps[step].reverse;
        circle.innerHTML = `<animateMotion dur="2.4s" repeatCount="indefinite" ${reversed ? 'keyPoints="1;0" keyTimes="0;1" calcMode="linear"' : ""}><mpath href="#path-${edge.id}"/></animateMotion>`;
        element.append(circle);
      }
    });
    document.querySelectorAll(".service-button").forEach((element) => element.classList.toggle("active", element.dataset.node === selected));
  }

  function renderDetail() {
    const item = node(selected), edge = data.edges.find((entry) => entry.id === selected);
    if (!item && !edge) {
      $("#detail").innerHTML = `<div class="detail-icon">✦</div><h2 class="detail-title">Знакомьтесь, Bloom</h2><span class="pill">Карта системы</span><p class="description">Выберите сервис, чтобы посмотреть его API, слои и исходники. Нажмите на линию, чтобы понять связь.</p><div class="detail-section"><h3>НА ЧТО ОБРАТИТЬ ВНИМАНИЕ</h3>${data.notes.map(note=>`<button class="related" data-node="${esc(note.service)}"><span>△ ${esc(node(note.service)?.title)}</span><span>Открыть ↗</span></button>`).join("")}<p class="description">Внизу — сценарии. Нажмите ▶ и проследите запрос шаг за шагом.</p></div>`;
      return;
    }
    if (edge) {
      $("#detail").innerHTML = `<div class="detail-icon">⇄</div><h2 class="detail-title">${esc(edge.label)}</h2><span class="pill ${edge.kind === "gap" ? "amber" : ""}">${esc(edge.kind.toUpperCase())}</span><p class="description">${esc(node(edge.from)?.title)} → ${esc(node(edge.to)?.title)}</p><p class="description">${esc(edge.description)}</p><div class="detail-section"><h3>ИСТОЧНИК</h3>${sources(edge.source)}${(edge.routes||[]).map(route=>`<h3>${esc(route.id)}</h3><div class="file-link">${route.predicates.map(esc).join("<br>")}${route.filters.length ? "<br>"+route.filters.map(esc).join("<br>") : ""}</div>`).join("")}</div>`;
      return;
    }
    const notes = data.notes.filter(note=>note.service === item.id);
    const tabs = ["overview","api","sources"];
    let content = "";
    if (tab === "overview") {
      content = `<div class="detail-section"><h3>КОНТЕКСТ</h3><div class="detail-row"><span>Технология</span><b>${technologies[item.type]}</b></div>${item.port ? `<div class="detail-row"><span>Внутренний порт</span><b>${item.port}</b></div>` : ""}${item.type === "java" || item.type === "go" ? `<div class="detail-row"><span>В Compose</span><b>${item.inCompose ? item.profiles.length ? "Профиль: " + esc(item.profiles.join(", ")) : "Объявлен" : "Не объявлен"}</b></div><div class="detail-row"><span>Порт на хосте</span><b>${item.published.length ? item.published.map(esc).join("<br>") : "Не опубликован"}</b></div>` : ""}${notes.map(note=>`<div class="notice"><strong>△ ${esc(note.title)}</strong>${esc(note.text)}${note.review ? " Требует повторной сверки с кодом." : ""}</div>`).join("")}<h3>СВЯЗИ</h3>${data.edges.filter(e=>e.from === item.id || e.to === item.id).map(e=>`<button class="related" data-connection="${esc(e.id)}"><span>${esc(e.from === item.id ? "→ " + node(e.to)?.title : "← " + node(e.from)?.title)}</span><span>${esc(e.label)}</span></button>`).join("") || '<p class="empty">Реализованные связи не найдены.</p>'}</div>`;
    } else if (tab === "api") {
      content = `<div class="detail-section"><p class="empty">Пути внутри сервиса. Внешний префикс Gateway — /api/v1. Внутренние методы через него не публикуются.</p>${(item.endpoints||[]).map(endpoint=>`<div class="endpoint"><span class="method ${endpoint.method}">${endpoint.method}</span><code>${esc(endpoint.path)}</code><small>${endpoint.internal ? "ВНУТРЕННИЙ · " : ""}${esc(endpoint.origin)}</small></div>`).join("") || '<p class="empty">REST-методы не найдены. Для Gateway выберите исходящую связь; WebSocket рассматривается отдельно от REST.</p>'}</div>`;
    } else {
      content = `<div class="detail-section"><p class="empty">Нажмите на файл, чтобы скопировать путь. Это навигация по исходникам, а не полный граф вызовов.</p>${sources(item.source)}${Object.entries(item.layers||{}).map(([layer,files])=>`<details class="layer"><summary>${esc(layerLabels[layer]||layer)} · ${files.length}</summary>${files.map(file=>sources(file.source)).join("")}</details>`).join("")}</div>`;
    }
    $("#detail").innerHTML = `<div class="detail-icon">${icons[item.type]||"◇"}</div><h2 class="detail-title">${esc(item.title)}</h2><span class="pill ${item.state === "stub" ? "amber" : ""}">${statusLabels[item.state]}</span><p class="description">${esc(item.description)}</p><div class="detail-tabs">${tabs.map((value,index)=>`<button data-tab="${value}" class="${tab === value ? "active" : ""}">${["Обзор","API","Исходники"][index]}</button>`).join("")}</div>${content}`;
  }

  function select(id) { selected = id; tab = "overview"; activeStep = false; pause(); renderDetail(); updateFocus(); }
  function pause() { playing = false; clearTimeout(timer); $("#play").textContent = "▶"; $("#play").setAttribute("aria-label", "Воспроизвести сценарий"); }
  function showStep() {
    const flow = scenario(), entry = flow.steps[step];
    $("#scenario-subtitle").textContent = flow.subtitle;
    $("#step-counter").textContent = `${String(step+1).padStart(2,"0")} / ${String(flow.steps.length).padStart(2,"0")}`;
    $("#step-title").textContent = entry.title;
    $("#step-description").textContent = entry.text;
    $("#progress").innerHTML = flow.steps.map((value,index)=>`<button data-step="${index}" class="${index <= step ? "active" : ""}" aria-label="Шаг ${index+1}: ${esc(value.title)}"></button>`).join("");
    $("#previous").disabled = step === 0;
    $("#next").disabled = step === flow.steps.length-1;
    if (activeStep) { selected = entry.edge; renderDetail(); }
    updateFocus();
  }
  function tick() { timer = setTimeout(() => { if (step < scenario().steps.length-1) { step++; showStep(); tick(); } else pause(); }, 5200); }
  function reset() { pause(); activeStep = false; step = 0; selected = null; query = ""; $("#search").value = ""; showStep(); renderDetail(); fit(); }

  function render() {
    $("#commit").textContent = data.commit;
    $("#updated").textContent = new Date(data.generated).toLocaleString("ru-RU", {day:"2-digit",month:"short",hour:"2-digit",minute:"2-digit"});
    const services = data.nodes.filter(item=>["java","go"].includes(item.type));
    $("#service-count").textContent = services.length;
    $("#service-list").innerHTML = services.map(item=>`<button class="service-button ${item.state}" data-node="${esc(item.id)}"><span class="service-dot ${item.type}"></span>${esc(item.title)}<small>${item.state === "stub" ? "скоро" : item.type === "java" ? "J" : "Go"}</small></button>`).join("");
    const metrics = [["◈",services.length,"компонентов","Java + Go"],["⇄",services.reduce((sum,item)=>sum+item.endpoints.length,0),"API-операций","из исходников"],["⌘",data.edges.filter(edge=>edge.kind === "event").length,"связей Kafka","из кода"],["△",services.filter(item=>item.state === "stub").length,"заготовки","ещё в работе"]];
    $("#metrics").innerHTML = metrics.map(([icon,value,label,note])=>`<div class="metric"><span class="metric-icon">${icon}</span><div><strong>${value}</strong><small>${note}</small><p>${label}</p></div></div>`).join("");
    $("#scenario").innerHTML = data.scenarios.map(flow=>`<option value="${esc(flow.id)}">${esc(flow.title)}</option>`).join("");
    $("#scenario").value = scenarioId;
    renderMap(); renderDetail(); showStep();
  }

  document.addEventListener("click", async (event) => {
    const service = event.target.closest("[data-node]");
    const connection = event.target.closest("[data-edge],[data-connection]");
    const tabButton = event.target.closest("[data-tab]");
    const filterButton = event.target.closest("[data-filter]");
    const stepButton = event.target.closest("[data-step]");
    const fileButton = event.target.closest("[data-copy]");
    if (service) select(service.dataset.node);
    else if (connection) select(connection.dataset.edge || connection.dataset.connection);
    else if (tabButton) { tab = tabButton.dataset.tab; renderDetail(); }
    else if (filterButton) { filter = filterButton.dataset.filter; document.querySelectorAll("[data-filter]").forEach(button=>button.classList.toggle("selected",button === filterButton)); updateFocus(); }
    else if (stepButton) { pause(); activeStep = true; step = Number(stepButton.dataset.step); showStep(); }
    else if (fileButton) {
      try { await navigator.clipboard.writeText(fileButton.dataset.copy); const old = fileButton.textContent; fileButton.textContent = "✓ Путь скопирован"; setTimeout(()=>fileButton.textContent=old,1500); }
      catch { window.prompt("Путь к файлу", fileButton.dataset.copy); }
    }
  });
  $("#connections").addEventListener("keydown", (event) => { if (["Enter"," "].includes(event.key)) { event.preventDefault(); event.target.closest("[data-edge]")?.dispatchEvent(new MouseEvent("click",{bubbles:true})); } });
  $("#search").addEventListener("input", (event)=>{ query = event.target.value.trim().toLowerCase(); updateFocus(); });
  $("#scenario").addEventListener("change", (event)=>{ pause(); scenarioId = event.target.value; step = 0; activeStep = true; showStep(); });
  $("#play").onclick = () => { if (playing) { pause(); return; } playing = true; activeStep = true; if (step === scenario().steps.length-1) step = 0; $("#play").textContent = "Ⅱ"; $("#play").setAttribute("aria-label","Пауза"); showStep(); tick(); };
  $("#previous").onclick = ()=>{ pause(); activeStep=true; step=Math.max(0,step-1); showStep(); };
  $("#next").onclick = ()=>{ pause(); activeStep=true; step=Math.min(scenario().steps.length-1,step+1); showStep(); };
  $("#stop").onclick = reset;
  $("#reset").onclick = reset;
  $("#close-detail").onclick = reset;
  $("#overview").onclick = reset;
  $(".brand").onclick = (event)=>{ event.preventDefault(); reset(); };
  $("#scenarios-nav").onclick = ()=>{ $(".player").scrollIntoView({behavior:motion?"smooth":"auto",block:"nearest"}); $("#scenario").focus(); };
  $("#zoom-in").onclick = ()=>changeZoom(1.2);
  $("#zoom-out").onclick = ()=>changeZoom(1/1.2);
  $("#fit").onclick = fit;
  const help = ()=>$("#help-dialog").showModal();
  $("#help").onclick = help; $("#about").onclick = help; $("#close-help").onclick = ()=>$("#help-dialog").close();
  function setMotion() { document.body.classList.toggle("no-motion",!motion); document.body.classList.toggle("motion",motion); $("#motion").setAttribute("aria-pressed",String(motion)); $("#motion").textContent=motion?"◌ Анимация":"◌ Без анимации"; updateFocus(); }
  $("#motion").onclick = ()=>{motion=!motion;setMotion();};
  $("#viewport").addEventListener("pointerdown", (event) => {
    if (event.button !== 0 || event.target.closest("button,[data-edge]")) return;
    event.preventDefault();
    drag = {x: event.clientX - pan.x, y: event.clientY - pan.y};
    event.currentTarget.setPointerCapture(event.pointerId);
    event.currentTarget.classList.add("dragging");
  });
  $("#viewport").addEventListener("pointermove", (event)=>{if(!drag)return;pan={x:event.clientX-drag.x,y:event.clientY-drag.y};transform();});
  for (const type of ["pointerup", "pointercancel", "lostpointercapture"]) {
    $("#viewport").addEventListener(type, () => {
      drag = null;
      $("#viewport").classList.remove("dragging");
    });
  }
  $("#viewport").addEventListener("wheel",(event)=>{if(!event.ctrlKey&&!event.metaKey)return;event.preventDefault();changeZoom(event.deltaY<0?1.08:1/1.08);},{passive:false});
  document.addEventListener("keydown",(event)=>{if(event.target.matches("input,select,textarea")||$("#help-dialog").open)return;if(event.key==="/"){event.preventDefault();$("#search").focus();}if(event.key==="Escape"||event.key==="1")reset();if(event.key==="2")$("#scenarios-nav").click();});
  new ResizeObserver(fit).observe($("#viewport"));
  render(); setMotion(); fit();
  // Local watch server regenerates data.js; static HTML continues to work offline.
  if(location.protocol!=="file:")setInterval(()=>{
    const script=document.createElement("script");script.src=`data.js?t=${Date.now()}`;
    script.onload=()=>{const fresh=window.BLOOM_ARCHITECTURE;if(fresh?.revision!==data.revision){pause();data=fresh;step=0;activeStep=false;if(!data.scenarios.some(item=>item.id===scenarioId))scenarioId=data.scenarios[0].id;render();}script.remove();};
    script.onerror=()=>script.remove();document.head.append(script);
  },6000);
})();
