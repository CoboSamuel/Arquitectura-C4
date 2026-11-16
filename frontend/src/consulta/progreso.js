const TOTAL_CONSULTAS = 3;

// Cuenta los pasos que ya tienen un resultado válido y muestra el avance
// con una frase breve para la persona usuaria.
export function calcularProgreso(state) {
  const completados = [state.contribuyente, state.vehiculo, state.licencia].filter(
    (resultado) => resultado !== null && resultado !== undefined,
  ).length;

  return {
    completados,
    total: TOTAL_CONSULTAS,
    porcentaje: Math.round((completados / TOTAL_CONSULTAS) * 100),
    texto: `Paso ${state.paso} de ${TOTAL_CONSULTAS}`,
  };
}
