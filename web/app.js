const state = {
  currencies: [],
  selectedId: null,
  filter: "",
  connected: false,
};

const byId = (id) => document.getElementById(id);
const worker = new Worker("/static/substrate/worker.js", { name: "v1-slim-substrate" });

function escapeHtml(value) {
  return String(value)
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#039;");
}

function selectedCurrency() {
  return state.currencies.find((currency) => currency.id === state.selectedId) || null;
}

function setTransport(connected, label = connected ? "worker ready" : "worker offline") {
  state.connected = connected;
  const dot = byId("transport-dot");
  dot.className = `status-dot ${connected ? "is-ready" : "is-error"}`;
  byId("transport-label").textContent = label;
  byId("transport-metric").textContent = connected ? "worker" : "offline";
}

function formatSyncTime() {
  return new Intl.DateTimeFormat(undefined, { hour: "2-digit", minute: "2-digit", second: "2-digit" }).format(new Date());
}

function renderList() {
  const filter = state.filter.trim().toLowerCase();
  const visible = state.currencies.filter((currency) => {
    const haystack = `${currency.code} ${currency.name} ${currency.region}`.toLowerCase();
    return haystack.includes(filter);
  });
  const list = byId("currency-list");
  byId("currency-count").textContent = String(state.currencies.length);
  byId("currency-count-metric").textContent = String(state.currencies.length).padStart(2, "0");

  if (!visible.length) {
    list.innerHTML = '<div class="list-empty">No currency matches that filter.</div>';
    return;
  }

  list.innerHTML = visible.map((currency) => `
    <button class="currency-row ${currency.id === state.selectedId ? "is-selected" : ""}" type="button" data-currency-id="${escapeHtml(currency.id)}">
      <span class="row-symbol">${escapeHtml(currency.symbol)}</span>
      <span class="row-copy"><strong>${escapeHtml(currency.code)}</strong><small>${escapeHtml(currency.name)}</small></span>
      <span class="row-arrow" aria-hidden="true">↗</span>
    </button>
  `).join("");
}

function renderDetail() {
  const currency = selectedCurrency();
  const empty = byId("detail-empty");
  const detail = byId("detail-content");
  byId("detail-badge").textContent = currency ? currency.code : "—";

  if (!currency) {
    empty.hidden = false;
    detail.hidden = true;
    return;
  }

  empty.hidden = true;
  detail.hidden = false;
  byId("detail-symbol").textContent = currency.symbol;
  byId("detail-code").textContent = currency.code;
  byId("detail-iso").textContent = currency.code;
  byId("detail-name").textContent = currency.name;
  byId("detail-region").textContent = currency.region;
  byId("detail-decimals").textContent = String(currency.decimals);
  byId("modify-name").value = currency.name;
  byId("modify-symbol").value = currency.symbol;
}

function render() {
  renderList();
  renderDetail();
}

function requestState() {
  worker.postMessage({ type: "state" });
}

worker.addEventListener("message", (event) => {
  const message = event.data || {};
  if (message.kind === "ready") {
    setTransport(true);
    requestState();
    return;
  }
  if (message.kind === "state") {
    state.currencies = message.data.currencies || [];
    state.selectedId = message.data.selectedId || state.currencies[0]?.id || null;
    byId("last-sync").textContent = `last synced ${formatSyncTime()}`;
    render();
    return;
  }
  if (message.kind === "error") {
    showMessage(message.form || "create-message", message.message, true);
  }
});

worker.addEventListener("error", () => {
  setTransport(false);
  byId("last-sync").textContent = "the worker could not be started";
});

function showMessage(id, message, error = false) {
  const node = byId(id);
  node.textContent = message || "";
  node.className = `form-message ${error ? "is-error" : "is-success"}`;
  if (message) {
    window.setTimeout(() => {
      node.textContent = "";
      node.className = "form-message";
    }, 3000);
  }
}

byId("currency-filter").addEventListener("input", (event) => {
  state.filter = event.target.value;
  renderList();
});

byId("currency-list").addEventListener("click", (event) => {
  const row = event.target.closest("[data-currency-id]");
  if (!row) return;
  state.selectedId = row.dataset.currencyId;
  worker.postMessage({ type: "select", id: state.selectedId });
  render();
});

byId("refresh-button").addEventListener("click", () => {
  worker.postMessage({ type: "refresh" });
});

byId("modify-form").addEventListener("submit", (event) => {
  event.preventDefault();
  const currency = selectedCurrency();
  if (!currency) return;
  const form = new FormData(event.currentTarget);
  worker.postMessage({
    type: "update",
    id: currency.id,
    patch: { name: form.get("name"), symbol: form.get("symbol") },
  });
  showMessage("modify-message", "Change sent to worker");
});

byId("create-form").addEventListener("submit", (event) => {
  event.preventDefault();
  const form = new FormData(event.currentTarget);
  worker.postMessage({
    type: "create",
    payload: {
      code: form.get("code"),
      name: form.get("name"),
      symbol: form.get("symbol"),
      decimals: form.get("decimals"),
      region: form.get("region"),
    },
  });
  event.currentTarget.reset();
  event.currentTarget.elements.decimals.value = "2";
  showMessage("create-message", "Create request sent to worker");
});

