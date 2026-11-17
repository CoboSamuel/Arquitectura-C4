import assert from "node:assert/strict";
import { test } from "node:test";
import {
  validarCedula,
  validarCorreo,
  validarPlaca,
  validarRuc,
  normalizarPlaca,
} from "./validacion.js";

test("acepta un correo válido y elimina espacios externos", () => {
  assert.equal(validarCorreo("  demo@example.test  "), null);
});

test("acepta correo de hasta 254 caracteres", () => {
  const correo = `${"a".repeat(247)}@x.test`;
  assert.equal(correo.length, 254);
  assert.equal(validarCorreo(correo), null);
});

test("rechaza correo vacío, mal formado o mayor de 254 caracteres", () => {
  assert.notEqual(validarCorreo(""), null);
  assert.notEqual(validarCorreo("no-es-correo"), null);
  assert.notEqual(validarCorreo(`${"a".repeat(248)}@x.test`), null);
});

test("valida el RUC por sus 13 dígitos sin convertirlo a número", () => {
  assert.equal(validarRuc("0999999999999"), null);
  assert.notEqual(validarRuc("999999999999"), null);
  assert.notEqual(validarRuc("099999999999A"), null);
});

test("acepta placas de 5 a 8 letras, números o guiones", () => {
  assert.equal(validarPlaca("ZZZ9999"), null);
  assert.equal(validarPlaca("AB-12"), null);
  assert.notEqual(validarPlaca("AB 123"), null);
  assert.notEqual(validarPlaca("AB1"), null);
  assert.notEqual(validarPlaca("ABCDEFGHI"), null);
});

test("normaliza placa recortando espacios y usando mayúsculas", () => {
  assert.equal(normalizarPlaca("  ab-1234 "), "AB-1234");
});

test("exige exactamente 10 dígitos para cédula", () => {
  assert.equal(validarCedula("0000000000"), null);
  assert.notEqual(validarCedula("123456789"), null);
  assert.notEqual(validarCedula("123456789A"), null);
});
