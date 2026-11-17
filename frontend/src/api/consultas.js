const MENSAJE_ERROR_GENERICO = "No se pudo completar la consulta. Inténtelo de nuevo.";
const MENSAJE_RED = "No se pudo conectar con el backend. Compruebe que esté en ejecución.";

export class ConsultaApiError extends Error {
  constructor({ status, code, message, fieldErrors = [] }) {
    super(message);
    this.name = "ConsultaApiError";
    this.status = status;
    this.code = code;
    this.fieldErrors = fieldErrors;
  }
}

async function postJson(path, payload) {
  let response;

  try {
    response = await fetch(path, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(payload),
    });
  } catch {
    throw new ConsultaApiError({
      status: null,
      code: "NETWORK_ERROR",
      message: MENSAJE_RED,
    });
  }

  let data;
  try {
    data = await response.json();
  } catch {
    data = null;
  }

  if (!response.ok) {
    const hasApiError = data && typeof data === "object" && !Array.isArray(data);
    const fieldErrors = Array.isArray(data?.fieldErrors)
      ? data.fieldErrors.filter(
          (item) =>
            item &&
            typeof item.field === "string" &&
            typeof item.message === "string",
        )
      : [];

    throw new ConsultaApiError({
      status: Number.isInteger(data?.status) ? data.status : response.status,
      code: typeof data?.code === "string" ? data.code : `HTTP_${response.status}`,
      message:
        hasApiError && typeof data.message === "string"
          ? data.message
          : MENSAJE_ERROR_GENERICO,
      fieldErrors,
    });
  }

  if (!data || typeof data !== "object" || Array.isArray(data)) {
    throw new ConsultaApiError({
      status: response.status,
      code: "INVALID_RESPONSE",
      message: MENSAJE_ERROR_GENERICO,
    });
  }

  return data;
}

export function consultarContribuyente({ correo, ruc }) {
  return postJson("/api/v1/consultas/contribuyentes", { correo, ruc });
}

export function consultarVehiculo({ placa }) {
  return postJson("/api/v1/consultas/vehiculos", { placa });
}

export function consultarLicencia({ cedula, placa }) {
  return postJson("/api/v1/consultas/licencias", { cedula, placa });
}
