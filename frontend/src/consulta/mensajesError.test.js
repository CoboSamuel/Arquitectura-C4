import assert from "node:assert/strict";
import test from "node:test";
import { describirError } from "./mensajesError.js";

test("describirError muestra instrucciones claras para la persona", () => {
  const info = describirError({
    code: "CONTRIBUYENTE_NO_ENCONTRADO",
    message: "No se encontró un contribuyente con los datos ingresados. Detalle interno: backend REST",
  });

  assert.equal(info.titulo, "No encontramos ese RUC");
  assert.equal(info.mensaje, "Comprueba que tenga 13 dígitos y que esté bien escrito.");
  assert.doesNotMatch(`${info.titulo} ${info.mensaje}`, /backend|REST|JSON/i);
  assert.equal(info.reintentable, false);
});

test("describirError permite reintentar solo en fallos temporales", () => {
  for (const code of ["SRI_NO_DISPONIBLE", "ANT_NO_DISPONIBLE", "NETWORK_ERROR"]) {
    assert.equal(describirError({ code, message: "x" }).reintentable, true, code);
  }
  for (const code of ["VALIDATION_ERROR", "NO_ES_PERSONA_NATURAL", "VEHICULO_NO_ENCONTRADO"]) {
    assert.equal(describirError({ code, message: "x" }).reintentable, false, code);
  }
});

test("describirError usa valores seguros con códigos desconocidos o errores vacíos", () => {
  const desconocido = describirError({ code: "HTTP_418", message: "Detalle interno del backend REST" });
  assert.equal(desconocido.titulo, "No se pudo completar este paso");
  assert.equal(desconocido.mensaje, "No pudimos completar la consulta. Inténtalo de nuevo.");
  assert.doesNotMatch(desconocido.mensaje, /backend|REST|JSON/i);
  assert.equal(desconocido.reintentable, false);

  const vacio = describirError(null);
  assert.match(vacio.mensaje, /No pudimos completar la consulta/);
});
