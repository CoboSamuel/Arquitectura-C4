import assert from "node:assert/strict";
import test from "node:test";
import { errorVisibleCampo } from "./validacionEnVivo.js";

test("no muestra error antes de tocar un campo", () => {
  assert.equal(errorVisibleCampo("correo", "", false), null);
});

test("muestra el error cuando un campo tocado tiene un formato inválido", () => {
  assert.equal(
    errorVisibleCampo("correo", "correo-sin-formato", true),
    "Ingrese un correo electrónico válido.",
  );
});

test("retira el error cuando se corrige un campo tocado", () => {
  assert.equal(errorVisibleCampo("correo", "persona@ejemplo.com", true), null);
});
