import assert from "node:assert/strict";
import { afterEach, test } from "node:test";
import {
  ConsultaApiError,
  consultarContribuyente,
  consultarLicencia,
  consultarVehiculo,
} from "./consultas.js";

const originalFetch = globalThis.fetch;

afterEach(() => {
  globalThis.fetch = originalFetch;
});

const requests = [
  {
    name: "contribuyente",
    run: consultarContribuyente,
    path: "/api/v1/consultas/contribuyentes",
    body: { correo: "demo@example.test", ruc: "0999999999999" },
  },
  {
    name: "vehículo",
    run: consultarVehiculo,
    path: "/api/v1/consultas/vehiculos",
    body: { placa: "ZZZ9999" },
  },
  {
    name: "licencia",
    run: consultarLicencia,
    path: "/api/v1/consultas/licencias",
    body: { cedula: "0000000000", placa: "ZZZ9999" },
  },
];

for (const request of requests) {
  test(`consulta de ${request.name}: envía POST JSON y devuelve el resumen`, async () => {
    const responseBody = { resultado: "sintetico" };
    let capturedRequest;
    globalThis.fetch = async (url, options) => {
      capturedRequest = { url, options };
      return new Response(JSON.stringify(responseBody), {
        status: 200,
        headers: { "Content-Type": "application/json" },
      });
    };

    const result = await request.run(request.body);

    assert.deepEqual(result, responseBody);
    assert.equal(capturedRequest.url, request.path);
    assert.equal(capturedRequest.options.method, "POST");
    assert.equal(
      capturedRequest.options.headers["Content-Type"],
      "application/json",
    );
    assert.equal(capturedRequest.options.body, JSON.stringify(request.body));
  });
}

test("conserva el contrato ApiErrorResponse de Spring en errores HTTP", async () => {
  globalThis.fetch = async () =>
    new Response(
      JSON.stringify({
        status: 422,
        code: "NO_ES_PERSONA_NATURAL",
        message: "El RUC no corresponde a una persona natural.",
        fieldErrors: [{ field: "ruc", message: "Revise el RUC." }],
      }),
      { status: 422, headers: { "Content-Type": "application/json" } },
    );

  await assert.rejects(
    consultarContribuyente({ correo: "demo@example.test", ruc: "0999999999999" }),
    (error) =>
      error instanceof ConsultaApiError &&
      error.status === 422 &&
      error.code === "NO_ES_PERSONA_NATURAL" &&
      error.message === "El RUC no corresponde a una persona natural." &&
      error.fieldErrors[0].field === "ruc",
  );
});

test("una respuesta HTTP no JSON produce un error genérico sin exponer el cuerpo", async () => {
  globalThis.fetch = async () =>
    new Response("upstream secret details", {
      status: 503,
      headers: { "Content-Type": "text/plain" },
    });

  await assert.rejects(
    consultarLicencia({ cedula: "0000000000", placa: "ZZZ9999" }),
    (error) =>
      error instanceof ConsultaApiError &&
      error.status === 503 &&
      !error.message.includes("upstream secret details"),
  );
});

test("un fallo de red se convierte en error genérico sin exponer detalles", async () => {
  globalThis.fetch = async () => {
    throw new Error("socket failure with sensitive detail");
  };

  await assert.rejects(
    consultarVehiculo({ placa: "ZZZ9999" }),
    (error) =>
      error instanceof ConsultaApiError &&
      error.status === null &&
      !error.message.includes("sensitive detail"),
  );
});
