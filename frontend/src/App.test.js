import assert from "node:assert/strict";
import test from "node:test";
import React from "react";
import { renderToString } from "react-dom/server";
import { createServer } from "vite";

test("App renderiza el primer paso del flujo React", async (context) => {
  const vite = await createServer({
    appType: "custom",
    logLevel: "silent",
    server: { middlewareMode: true },
  });
  context.after(() => vite.close());

  const { default: App } = await vite.ssrLoadModule("/src/App.jsx");
  const markup = renderToString(React.createElement(App));

  assert.match(markup, /Consulta SRI/);
  assert.match(markup, /Verifica al contribuyente/);
  assert.match(markup, /Correo electrónico/);
});
