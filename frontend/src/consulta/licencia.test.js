import assert from "node:assert/strict";
import test from "node:test";
import { describirOrigenLicencia } from "./licencia.js";

test("identifica una respuesta simulada de ANT sin presentarla como actual", () => {
  assert.equal(describirOrigenLicencia("SIMULADO"), "Servicio ANT simulado");
  assert.notEqual(describirOrigenLicencia("SIMULADO"), "Actualizado ahora");
});

test("identifica el estado de una respuesta simulada o cacheada", async () => {
  const { describirEstadoLicencia } = await import("./licencia.js");

  assert.equal(
    describirEstadoLicencia({ origen: "SIMULADO", desactualizado: false }),
    "Resultado simulado",
  );
  assert.equal(
    describirEstadoLicencia({ origen: "CACHE", desactualizado: false }),
    "Disponible en caché",
  );
  assert.equal(
    describirEstadoLicencia({ origen: "CACHE", desactualizado: true }),
    "Consulta anterior",
  );
});

test("conserva las etiquetas de caché y consulta real", () => {
  assert.equal(describirOrigenLicencia("CACHE"), "Última consulta disponible");
  assert.equal(describirOrigenLicencia("ANT"), "Actualizado ahora");
});
