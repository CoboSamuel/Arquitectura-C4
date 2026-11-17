import { DOMINIOS_PERMITIDOS, sugerirCorreccionDominio } from "./dominioCorreo.js";

const CORREO_MAXIMO = 254;
const CORREO_FORMATO = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const RUC_FORMATO = /^\d{13}$/;
const PLACA_FORMATO = /^[A-Za-z0-9]{5,8}$/;
const CEDULA_FORMATO = /^\d{10}$/;

function textoRecortado(value) {
  return typeof value === "string" ? value.trim() : "";
}

export function validarCorreo(value) {
  const correo = textoRecortado(value);
  if (!correo) return "Ingrese el correo electrónico.";
  if (correo.length > CORREO_MAXIMO) {
    return "El correo no puede superar 254 caracteres.";
  }
  if (!CORREO_FORMATO.test(correo)) {
    return "Ingrese un correo electrónico válido.";
  }
  const sugerenciaDominio = sugerirCorreccionDominio(correo);
  if (sugerenciaDominio) return sugerenciaDominio.sugerencia;
  const dominio = correo.split("@")[1].toLowerCase();
  if (!DOMINIOS_PERMITIDOS.includes(dominio)) {
    return "Usa un correo de Gmail, Yahoo, Hotmail, Outlook, iCloud o Proton.";
  }
  return null;
}

export function validarRuc(value) {
  const ruc = textoRecortado(value);
  if (!RUC_FORMATO.test(ruc)) {
    return "El RUC debe contener exactamente 13 dígitos.";
  }
  return null;
}

export function validarPlaca(value) {
  const placa = textoRecortado(value);
  if (!PLACA_FORMATO.test(placa)) {
    return "La placa debe tener de 5 a 8 letras y números, sin espacios ni guiones.";
  }
  return null;
}

export function validarCedula(value) {
  const cedula = textoRecortado(value);
  if (!CEDULA_FORMATO.test(cedula)) {
    return "La cédula debe contener exactamente 10 dígitos.";
  }
  return null;
}

export function normalizarPlaca(value) {
  return textoRecortado(value).toUpperCase();
}
