import assert from "node:assert/strict";
import test from "node:test";
import React from "react";
import { renderToString } from "react-dom/server";
import { createServer } from "vite";

test("la consulta usa la identidad visual de Quito en sus vistas internas", async (context) => {
  const vite = await createServer({
    appType: "custom",
    logLevel: "silent",
    server: { middlewareMode: true },
  });
  context.after(() => vite.close());

  const appModule = await vite.ssrLoadModule("/src/App.jsx");
  assert.equal(typeof appModule.ConsultaBanner, "function");
  assert.equal(typeof appModule.ConsultaAside, "function");

  const banner = renderToString(
    React.createElement(appModule.ConsultaBanner, { paso: 1 }),
  );
  const aside = renderToString(
    React.createElement(appModule.ConsultaAside, {
      ayuda: {
        etiqueta: "Antes de empezar",
        texto: "Ten a mano estos datos.",
        items: ["Correo electrónico"],
      },
    }),
  );

  assert.match(banner, /consulta-banner/);
  assert.match(banner, /quito-hero\.png/);
  assert.match(banner, /QUITO.*ECUADOR/i);
  assert.match(aside, /aside-landscape/);
  assert.match(aside, /quito-hero\.png/);
});
