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

  assert.match(markup, /Consulta ciudadana/);
  assert.match(markup, /Tus datos/);
  assert.match(markup, /Correo electrónico/);
  assert.match(markup, /Saltar al contenido principal/);
  assert.match(markup, /role="progressbar"/);
  assert.match(markup, /aria-valuenow="0"/);
  assert.match(markup, /Paso 1 de 3/);
  assert.doesNotMatch(markup, /REST|JSON|React|Backend Java|Arquitectura C4|Redis Cloud|Verificación tributaria/i);
  assert.doesNotMatch(markup, /Reintentar consulta/);
});
