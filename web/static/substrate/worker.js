// Deployable fallback for the Currency slice.
// `lein build-worker` can replace this file with the compiled xt.substrate worker
// without changing the page's message boundary.

const currencies = [
  { id: "usd", code: "USD", name: "United States dollar", symbol: "$", decimals: 2, region: "North America" },
  { id: "eur", code: "EUR", name: "Euro", symbol: "€", decimals: 2, region: "Europe" },
  { id: "jpy", code: "JPY", name: "Japanese yen", symbol: "¥", decimals: 0, region: "East Asia" },
];

let selectedId = "usd";

function snapshot() {
  return {
    currencies: currencies.map((currency) => ({ ...currency })),
    selectedId,
    page: { space: "statstrade/v1-slim", group: "currency" },
  };
}

function reply(kind, data) {
  self.postMessage({ kind, data });
}

function fail(message, form) {
  self.postMessage({ kind: "error", message, form });
}

self.addEventListener("message", (event) => {
  const message = event.data || {};

  if (message.type === "state" || message.type === "refresh") {
    reply("state", snapshot());
    return;
  }

  if (message.type === "select") {
    if (currencies.some((currency) => currency.id === message.id)) selectedId = message.id;
    reply("state", snapshot());
    return;
  }

  if (message.type === "update") {
    const currency = currencies.find((candidate) => candidate.id === message.id);
    if (!currency) return fail("That currency is no longer available.", "modify-message");
    const patch = message.patch || {};
    const name = String(patch.name || "").trim();
    const symbol = String(patch.symbol || "").trim();
    if (!name || !symbol) return fail("Name and symbol are required.", "modify-message");
    currency.name = name;
    currency.symbol = symbol;
    reply("state", snapshot());
    return;
  }

  if (message.type === "create") {
    const payload = message.payload || {};
    const code = String(payload.code || "").trim().toUpperCase();
    const name = String(payload.name || "").trim();
    const symbol = String(payload.symbol || "").trim();
    const region = String(payload.region || "").trim();
    const decimals = Number(payload.decimals);
    if (!/^[A-Z]{3}$/.test(code) || !name || !symbol || !region || !Number.isInteger(decimals) || decimals < 0 || decimals > 4) {
      return fail("Enter a valid ISO code, name, symbol, region, and 0–4 decimals.", "create-message");
    }
    if (currencies.some((currency) => currency.code === code)) {
      return fail(`${code} is already in this page group.`, "create-message");
    }
    const record = { id: code.toLowerCase(), code, name, symbol, decimals, region };
    currencies.unshift(record);
    selectedId = record.id;
    reply("state", snapshot());
  }
});

self.postMessage({ kind: "ready" });

