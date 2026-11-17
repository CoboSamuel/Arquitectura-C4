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

test("muestra errores en vivo para cualquier campo con contenido inválido", () => {
  assert.equal(
    errorVisibleCampo("correo", "persona@dominio-no-permitido.test", false),
    "Usa un correo de Gmail, Yahoo, Hotmail, Outlook, iCloud o Proton.",
  );
  assert.equal(
    errorVisibleCampo("ruc", "123", false),
    "El RUC debe contener exactamente 13 dígitos.",
  );
  assert.equal(
    errorVisibleCampo("placa", "AB 12", false),
    "La placa debe tener de 5 a 8 letras y números, sin espacios ni guiones.",
  );
  assert.equal(
    errorVisibleCampo("cedula", "123", false),
    "La cédula debe contener exactamente 10 dígitos.",
  );
});

test("retira el error cuando se corrige un campo tocado", () => {
  assert.equal(errorVisibleCampo("correo", "persona@gmail.com", true), null);
});
