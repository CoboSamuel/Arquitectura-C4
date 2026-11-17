import {
  validarCedula,
  validarCorreo,
  validarPlaca,
  validarRuc,
} from "./validacion.js";

const VALIDADORES = {
  correo: validarCorreo,
  ruc: validarRuc,
  placa: validarPlaca,
  cedula: validarCedula,
};

export function errorVisibleCampo(campo, valor, tocado) {
  const tieneContenido = typeof valor === "string" && valor.trim() !== "";
  if (!tocado && !tieneContenido) return null;
  return VALIDADORES[campo]?.(valor) ?? null;
}
