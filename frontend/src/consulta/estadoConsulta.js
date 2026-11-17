export const estadoInicialConsulta = Object.freeze({
  paso: 1,
  correo: "",
  ruc: "",
  contribuyente: null,
  placa: "",
  vehiculo: null,
  cedula: "",
  licencia: null,
  cargando: false,
  error: null,
});

const CAMPOS_EDITABLES = new Set(["correo", "ruc", "placa", "cedula"]);

export function reducirConsulta(state, action) {
  switch (action.type) {
    case "campo/cambiado": {
      const { campo, valor } = action;
      if (!CAMPOS_EDITABLES.has(campo)) return state;

      if (campo === "ruc" && valor !== state.ruc) {
        return {
          ...state,
          ruc: valor,
          paso: 1,
          contribuyente: null,
          placa: "",
          vehiculo: null,
          cedula: "",
          licencia: null,
          cargando: false,
          error: null,
        };
      }

      if (campo === "placa" && valor !== state.placa) {
        return {
          ...state,
          placa: valor,
          paso: state.paso > 2 ? 2 : state.paso,
          vehiculo: null,
          licencia: null,
          cargando: false,
          error: null,
        };
      }

      if (campo === "cedula" && valor !== state.cedula) {
        return { ...state, cedula: valor, licencia: null, error: null };
      }

      return { ...state, [campo]: valor, error: null };
    }

    case "solicitud/iniciada":
      if (state.cargando) return state;
      return { ...state, cargando: true, error: null };

    case "contribuyente/recibido":
      return {
        ...state,
        contribuyente: action.resultado,
        paso: 2,
        cargando: false,
        error: null,
      };

    case "vehiculo/recibido":
      return {
        ...state,
        vehiculo: action.resultado,
        paso: 3,
        cargando: false,
        error: null,
      };

    case "licencia/recibida":
      return {
        ...state,
        licencia: action.resultado,
        paso: 3,
        cargando: false,
        error: null,
      };

    case "solicitud/fallida":
      return { ...state, cargando: false, error: action.error };

    case "paso/volver": {
      const canGoBack =
        action.paso === 1 ||
        (action.paso === 2 && state.vehiculo !== null);
      if (!canGoBack || action.paso >= state.paso) return state;
      return { ...state, paso: action.paso, error: null };
    }

    case "consulta/reiniciada":
      return { ...estadoInicialConsulta };

    default:
      return state;
  }
}
