const MENSAJE_GENERICO = "No pudimos completar la consulta. Inténtalo de nuevo.";

const AYUDA_POR_CODIGO = {
  VALIDATION_ERROR: {
    titulo: "Revisa los datos ingresados",
    mensaje: "Corrige los campos señalados e inténtalo de nuevo.",
    sugerencia: "",
    reintentable: false,
  },
  INVALID_JSON: {
    titulo: "No pudimos procesar la consulta",
    mensaje: "Recarga la página e inténtalo de nuevo.",
    sugerencia: "",
    reintentable: false,
  },
  CONTRIBUYENTE_NO_ENCONTRADO: {
    titulo: "No encontramos ese RUC",
    mensaje: "Comprueba que tenga 13 dígitos y que esté bien escrito.",
    sugerencia: "",
    reintentable: false,
  },
  NO_ES_PERSONA_NATURAL: {
    titulo: "Este RUC no corresponde a una persona natural",
    mensaje: "Revisa el RUC ingresado para continuar.",
    sugerencia: "",
    reintentable: false,
  },
  VEHICULO_NO_ENCONTRADO: {
    titulo: "No encontramos ese vehículo",
    mensaje: "Revisa la placa e inténtalo de nuevo.",
    sugerencia: "",
    reintentable: false,
  },
  LICENCIA_NO_ENCONTRADA: {
    titulo: "No encontramos esa licencia",
    mensaje: "Comprueba la cédula y la placa ingresadas.",
    sugerencia: "",
    reintentable: false,
  },
  SRI_NO_DISPONIBLE: {
    titulo: "No pudimos consultar tus datos ahora",
    mensaje: "Inténtalo de nuevo en unos minutos.",
    sugerencia: "",
    reintentable: true,
  },
  SRI_RESPUESTA_INVALIDA: {
    titulo: "No pudimos consultar tus datos ahora",
    mensaje: "Inténtalo de nuevo en unos minutos.",
    sugerencia: "",
    reintentable: true,
  },
  ANT_NO_DISPONIBLE: {
    titulo: "No pudimos consultar los puntos ahora",
    mensaje: "Inténtalo de nuevo en unos minutos.",
    sugerencia: "",
    reintentable: true,
  },
  ANT_RESPUESTA_INVALIDA: {
    titulo: "No pudimos consultar los puntos ahora",
    mensaje: "Inténtalo de nuevo en unos minutos.",
    sugerencia: "",
    reintentable: true,
  },
  CACHE_NO_CONFIGURADA: {
    titulo: "Los puntos de licencia no están disponibles",
    mensaje: "Inténtalo de nuevo más tarde.",
    sugerencia: "",
    reintentable: false,
  },
  NETWORK_ERROR: {
    titulo: "No hay conexión con el servicio",
    mensaje: "Comprueba tu conexión a internet e inténtalo de nuevo.",
    sugerencia: "",
    reintentable: true,
  },
  INTERNAL_ERROR: {
    titulo: "Ocurrió un problema",
    mensaje: "Inténtalo de nuevo en unos minutos.",
    sugerencia: "",
    reintentable: true,
  },
};

const AYUDA_POR_DEFECTO = {
  titulo: "No se pudo completar este paso",
  mensaje: MENSAJE_GENERICO,
  sugerencia: "",
  reintentable: false,
};

export function describirError(error) {
  const code = typeof error?.code === "string" ? error.code : "";
  return AYUDA_POR_CODIGO[code] ?? AYUDA_POR_DEFECTO;
}
