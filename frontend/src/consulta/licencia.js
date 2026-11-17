const ETIQUETAS_ORIGEN = {
  ANT: "Actualizado ahora",
  CACHE: "Última consulta disponible",
  SIMULADO: "Servicio ANT simulado",
};

export function describirOrigenLicencia(origen) {
  return ETIQUETAS_ORIGEN[origen] ?? "Origen no confirmado";
}

export function describirEstadoLicencia({ origen, desactualizado } = {}) {
  if (origen === "SIMULADO") return "Resultado simulado";
  if (origen === "CACHE") {
    return desactualizado ? "Consulta anterior" : "Disponible en caché";
  }
  return "Actualizado ahora";
}
