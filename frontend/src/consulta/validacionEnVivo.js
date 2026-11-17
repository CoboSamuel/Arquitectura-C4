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
  if (!tocado) return null;
  return VALIDADORES[campo]?.(valor) ?? null;
}
