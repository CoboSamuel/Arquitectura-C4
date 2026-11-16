import assert from "node:assert/strict";
import test from "node:test";
import { estadoInicialConsulta } from "./estadoConsulta.js";
import { calcularProgreso } from "./progreso.js";

test("calcularProgreso empieza en cero", () => {
  const progreso = calcularProgreso(estadoInicialConsulta);
  assert.deepEqual(progreso, {
    completados: 0,
    total: 3,
    porcentaje: 0,
    texto: "Paso 1 de 3",
  });
});

test("calcularProgreso cuenta solo resultados recibidos", () => {
  const parcial = calcularProgreso({
    ...estadoInicialConsulta,
    contribuyente: { ruc: "0999999999999" },
    vehiculo: { placa: "ZZZ9999" },
  });
  assert.equal(parcial.completados, 2);
  assert.equal(parcial.porcentaje, 67);

  const completo = calcularProgreso({
    ...estadoInicialConsulta,
    contribuyente: {},
    vehiculo: {},
    licencia: {},
  });
  assert.equal(completo.porcentaje, 100);
});
