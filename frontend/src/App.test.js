import assert from "node:assert/strict";
import test from "node:test";
import React from "react";
import { renderToString } from "react-dom/server";
import { createServer } from "vite";

test("App renderiza una portada antes de iniciar la consulta", async (context) => {
  const vite = await createServer({
    appType: "custom",
    logLevel: "silent",
    server: { middlewareMode: true },
  });
  context.after(() => vite.close());

  const { default: App } = await vite.ssrLoadModule("/src/App.jsx");
  const markup = renderToString(React.createElement(App));

  assert.match(markup, /Consulta ciudadana/);
  assert.match(markup, /Consulta tu información tributaria, vehicular y de licencia/);
  assert.match(markup, /¿Qué puedes consultar\?/);
  assert.match(markup, /Iniciar consulta/);
  assert.match(markup, /Datos personales/);
  assert.match(markup, /Saltar al contenido principal/);
  assert.doesNotMatch(
    markup,
    /Demostración académica con datos ficticios|demo@example\.test|0000000000000|ZZZ9999|0000000000|No ingreses información personal real/,
  );
  assert.doesNotMatch(markup, /role="progressbar"/);
  assert.doesNotMatch(markup, /<form/);
  assert.doesNotMatch(markup, /REST|JSON|React|Backend Java|Arquitectura C4|Redis Cloud|Verificación tributaria/i);
  assert.doesNotMatch(markup, /Reintentar consulta/);
});
