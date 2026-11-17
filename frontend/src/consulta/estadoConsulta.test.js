import assert from "node:assert/strict";
import { test } from "node:test";
import { estadoInicialConsulta, reducirConsulta } from "./estadoConsulta.js";

function estadoCompleto() {
  return {
    ...estadoInicialConsulta,
    paso: 3,
    correo: "demo@example.test",
    ruc: "0999999999999",
    contribuyente: { ruc: "0999999999999", nombre: "PERSONA SINTÉTICA" },
    placa: "ZZZ9999",
    vehiculo: { placa: "ZZZ9999", marca: "MARCA SINTÉTICA" },
    cedula: "0000000000",
    licencia: { puntosDisponibles: 25, origen: "ANT", desactualizado: false },
    cargando: false,
    error: null,
  };
}

test("cambiar RUC limpia los resultados y datos posteriores y vuelve al paso uno", () => {
  const next = reducirConsulta(estadoCompleto(), {
    type: "campo/cambiado",
    campo: "ruc",
    valor: "1799999999001",
  });

  assert.equal(next.paso, 1);
  assert.equal(next.ruc, "1799999999001");
  assert.equal(next.contribuyente, null);
  assert.equal(next.placa, "");
  assert.equal(next.vehiculo, null);
  assert.equal(next.cedula, "");
  assert.equal(next.licencia, null);
});

test("cambiar placa limpia vehículo y licencia, conserva cédula y vuelve al paso dos", () => {
  const next = reducirConsulta(estadoCompleto(), {
    type: "campo/cambiado",
    campo: "placa",
    valor: "ABC1234",
  });

  assert.equal(next.paso, 2);
  assert.equal(next.placa, "ABC1234");
  assert.equal(next.vehiculo, null);
  assert.equal(next.cedula, "0000000000");
  assert.equal(next.licencia, null);
  assert.notEqual(next.contribuyente, null);
});

test("cambiar cédula limpia solo el resultado de licencia", () => {
  const next = reducirConsulta(estadoCompleto(), {
    type: "campo/cambiado",
    campo: "cedula",
    valor: "1111111111",
  });

  assert.equal(next.cedula, "1111111111");
  assert.equal(next.licencia, null);
  assert.notEqual(next.contribuyente, null);
  assert.notEqual(next.vehiculo, null);
  assert.equal(next.paso, 3);
});

test("una respuesta válida de contribuyente avanza al paso de vehículo", () => {
  const next = reducirConsulta(estadoInicialConsulta, {
    type: "contribuyente/recibido",
    resultado: { ruc: "0999999999999", nombre: "PERSONA SINTÉTICA" },
  });

  assert.equal(next.paso, 2);
  assert.equal(next.contribuyente.nombre, "PERSONA SINTÉTICA");
  assert.equal(next.cargando, false);
  assert.equal(next.error, null);
});

test("iniciar solicitud marca carga y bloquea un segundo envío", () => {
  const pending = reducirConsulta(estadoInicialConsulta, {
    type: "solicitud/iniciada",
  });
  const duplicate = reducirConsulta(pending, { type: "solicitud/iniciada" });

  assert.equal(pending.cargando, true);
  assert.strictEqual(duplicate, pending);
});

test("una respuesta válida de vehículo avanza al paso de licencia", () => {
  const state = { ...estadoInicialConsulta, paso: 2, contribuyente: { nombre: "SINTÉTICO" } };
  const next = reducirConsulta(state, {
    type: "vehiculo/recibido",
    resultado: { placa: "ZZZ9999", marca: "SINTÉTICA" },
  });

  assert.equal(next.paso, 3);
  assert.equal(next.vehiculo.marca, "SINTÉTICA");
  assert.equal(next.cargando, false);
});

test("un error conserva el paso y los resultados ya válidos", () => {
  const state = {
    ...estadoInicialConsulta,
    paso: 2,
    contribuyente: { ruc: "0999999999999" },
    cargando: true,
  };
  const next = reducirConsulta(state, {
    type: "solicitud/fallida",
    error: { code: "SRI_NO_DISPONIBLE", message: "Intente nuevamente." },
  });

  assert.equal(next.paso, 2);
  assert.deepEqual(next.contribuyente, state.contribuyente);
  assert.equal(next.cargando, false);
  assert.equal(next.error.code, "SRI_NO_DISPONIBLE");
});

test("solo permite regresar a pasos que ya tienen su requisito validado", () => {
  const initial = reducirConsulta(estadoInicialConsulta, {
    type: "paso/volver",
    paso: 2,
  });
  assert.equal(initial.paso, 1);

  const withContributor = {
    ...estadoInicialConsulta,
    paso: 2,
    contribuyente: { ruc: "0999999999999" },
  };
  const back = reducirConsulta(withContributor, { type: "paso/volver", paso: 1 });
  const ahead = reducirConsulta(withContributor, { type: "paso/volver", paso: 3 });
  assert.equal(back.paso, 1);
  assert.equal(ahead.paso, 2);
});

test("regresar al paso de vehículo requiere que exista un resultado vehicular", () => {
  const missingVehicle = {
    ...estadoInicialConsulta,
    paso: 3,
    contribuyente: { ruc: "0999999999999" },
  };
  const blocked = reducirConsulta(missingVehicle, {
    type: "paso/volver",
    paso: 2,
  });
  assert.strictEqual(blocked, missingVehicle);

  const withVehicle = { ...missingVehicle, vehiculo: { placa: "ZZZ9999" } };
  const allowed = reducirConsulta(withVehicle, { type: "paso/volver", paso: 2 });
  assert.equal(allowed.paso, 2);
});

test("una acción de campo no puede sustituir un resultado del servicio", () => {
  const state = estadoCompleto();
  const next = reducirConsulta(state, {
    type: "campo/cambiado",
    campo: "licencia",
    valor: { puntosDisponibles: 999 },
  });

  assert.strictEqual(next, state);
});

test("reiniciar devuelve el flujo al estado inicial", () => {
  const next = reducirConsulta(estadoCompleto(), { type: "consulta/reiniciada" });
  assert.deepEqual(next, estadoInicialConsulta);
  assert.notEqual(next, estadoInicialConsulta);
});
