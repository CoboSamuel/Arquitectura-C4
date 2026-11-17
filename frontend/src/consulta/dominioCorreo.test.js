import assert from "node:assert/strict";
import test from "node:test";
import { sugerirCorreccionDominio } from "./dominioCorreo.js";

test("sugiere gmail.com cuando el dominio parece gmiol.com", () => {
  assert.deepEqual(sugerirCorreccionDominio("samuelcobo29@gmiol.com"), {
    dominio: "gmiol.com",
    sugerencia: "Revisa el dominio. ¿Quizá quisiste escribir gmail.com?",
  });
});

test("sugiere outlook.com para un error común del dominio", () => {
  assert.deepEqual(sugerirCorreccionDominio("persona@outlok.com"), {
    dominio: "outlok.com",
    sugerencia: "Revisa el dominio. ¿Quizá quisiste escribir outlook.com?",
  });
});

test("no propone una corrección para dominios institucionales", () => {
  assert.equal(sugerirCorreccionDominio("persona@universidad.edu.ec"), null);
});

test("no sugiere cambios para un dominio personalizado desconocido", () => {
  assert.equal(sugerirCorreccionDominio("persona@empresa.local"), null);
});
