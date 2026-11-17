export const DOMINIOS_PERMITIDOS = [
  "gmail.com",
  "googlemail.com",
  "yahoo.com",
  "yahoo.es",
  "ymail.com",
  "hotmail.com",
  "hotmail.es",
  "outlook.com",
  "outlook.es",
  "live.com",
  "live.es",
  "msn.com",
  "icloud.com",
  "me.com",
  "mac.com",
  "proton.me",
  "protonmail.com",
  "aol.com",
  "mail.com",
  "zoho.com",
  "gmx.com",
  "gmx.es",
];

const ERRORES_COMUNES = {
  "gmiol.com": "gmail.com",
  "gmial.com": "gmail.com",
  "gmal.com": "gmail.com",
  "outlok.com": "outlook.com",
  "hotmial.com": "hotmail.com",
  "yaho.com": "yahoo.com",
};

function distanciaEdicion(origen, destino) {
  const filas = Array.from({ length: origen.length + 1 }, (_, index) => [index]);
  for (let columna = 0; columna <= destino.length; columna += 1) {
    filas[0][columna] = columna;
  }

  for (let fila = 1; fila <= origen.length; fila += 1) {
    for (let columna = 1; columna <= destino.length; columna += 1) {
      const costo = origen[fila - 1] === destino[columna - 1] ? 0 : 1;
      filas[fila][columna] = Math.min(
        filas[fila - 1][columna] + 1,
        filas[fila][columna - 1] + 1,
        filas[fila - 1][columna - 1] + costo,
      );
    }
  }

  return filas[origen.length][destino.length];
}

export function sugerirCorreccionDominio(value) {
  const correo = typeof value === "string" ? value.trim().toLowerCase() : "";
  const match = correo.match(/^[^\s@]+@([^\s@]+)$/);
  if (!match) return null;

  const dominio = match[1];
  if (DOMINIOS_PERMITIDOS.includes(dominio)) return null;

  const candidato = ERRORES_COMUNES[dominio] ?? DOMINIOS_PERMITIDOS.find(
    (conocido) => distanciaEdicion(dominio, conocido) <= 1,
  );
  if (!candidato) return null;

  return {
    dominio,
    sugerencia: `Revisa el dominio. ¿Quizá quisiste escribir ${candidato}?`,
  };
}
