import React, { useReducer, useRef, useState } from "react";
import {
  consultarContribuyente,
  consultarLicencia,
  consultarVehiculo,
} from "./api/consultas.js";
import {
  estadoInicialConsulta,
  reducirConsulta,
} from "./consulta/estadoConsulta.js";
import {
  normalizarPlaca,
  validarCedula,
  validarCorreo,
  validarPlaca,
  validarRuc,
} from "./consulta/validacion.js";

const PASOS = [
  { id: 1, numero: "01", titulo: "Contribuyente", servicio: "SRI" },
  { id: 2, numero: "02", titulo: "Vehículo", servicio: "SRI" },
  { id: 3, numero: "03", titulo: "Licencia", servicio: "ANT + Redis" },
];

const EXPLICACION_POR_PASO = {
  1: {
    etiqueta: "VERIFICACIÓN TRIBUTARIA",
    texto:
      "React envía el correo y el RUC al backend Java. El backend consulta al SRI y devuelve solo el resumen permitido de una persona natural.",
    destino: "Servicio SRI",
  },
  2: {
    etiqueta: "CONSULTA VEHICULAR",
    texto:
      "El backend Java consulta la matrícula en el SRI. React recibe un resumen con los datos del vehículo necesarios para este flujo.",
    destino: "SRI · Matriculación",
  },
  3: {
    etiqueta: "PUNTOS Y DISPONIBILIDAD",
    texto:
      "El backend consulta a la ANT y guarda respuestas válidas en Redis Cloud. Si la ANT no responde, puede mostrar el último resumen válido guardado.",
    destino: "Portal ANT · Redis Cloud",
  },
};

function mapearErroresDeCampo(fieldErrors = []) {
  return Object.fromEntries(
    fieldErrors
      .filter((item) => item?.field && item?.message)
      .map((item) => [item.field, item.message]),
  );
}

function fechaLegible(value) {
  if (!value) return "No informado";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return new Intl.DateTimeFormat("es-EC", {
    dateStyle: "medium",
    timeStyle: "short",
  }).format(date);
}

function FormField({ id, label, helper, error, value, onChange, ...inputProps }) {
  const helperId = `${id}-helper`;
  const errorId = `${id}-error`;

  return (
    <div className="form-field">
      <label htmlFor={id}>{label}</label>
      <input
        id={id}
        value={value}
        onChange={onChange}
        aria-invalid={Boolean(error)}
        aria-describedby={error ? errorId : helperId}
        {...inputProps}
      />
      {error ? (
        <p className="field-error" id={errorId} role="alert">{error}</p>
      ) : (
        <p className="field-helper" id={helperId}>{helper}</p>
      )}
    </div>
  );
}

function Stepper({ state, onGoToStep }) {
  const completado = [
    state.contribuyente !== null,
    state.vehiculo !== null,
    state.licencia !== null,
  ];

  return (
    <nav className="stepper" aria-label="Progreso de la consulta">
      {PASOS.map((paso, index) => {
        const activo = state.paso === paso.id;
        const listo = completado[index];
        return (
          <div className="stepper-item" key={paso.id}>
            <button
              className={`step-button${activo ? " is-active" : ""}${listo ? " is-complete" : ""}`}
              type="button"
              disabled={!listo || activo || state.cargando}
              aria-current={activo ? "step" : undefined}
              onClick={() => onGoToStep(paso.id)}
            >
              <span className="step-number" aria-hidden="true">{listo ? "✓" : paso.numero}</span>
              <span className="step-copy">
                <span className="step-title">{paso.titulo}</span>
                <span className="step-service">{paso.servicio}</span>
              </span>
            </button>
            {index < PASOS.length - 1 && (
              <span className={`step-connector${listo ? " is-complete" : ""}`} aria-hidden="true" />
            )}
          </div>
        );
      })}
    </nav>
  );
}

function SummaryCard({ title, subtitle, children, onEdit, editLabel, disabled }) {
  return (
    <section className="summary-card">
      <div className="summary-card-heading">
        <span className="summary-check" aria-hidden="true">✓</span>
        <div className="summary-heading-copy">
          <p className="summary-kicker">PASO COMPLETADO</p>
          <h3>{title}</h3>
          {subtitle && <p className="summary-subtitle">{subtitle}</p>}
        </div>
        {onEdit && (
          <button className="text-button" type="button" onClick={onEdit} disabled={disabled}>
            {editLabel || "Editar"}
          </button>
        )}
      </div>
      {children && <div className="summary-details">{children}</div>}
    </section>
  );
}

function Detail({ label, value }) {
  const displayValue = value === null || value === undefined || value === "" ? "No informado" : value;
  return (
    <div className="detail-item">
      <dt>{label}</dt>
      <dd>{displayValue}</dd>
    </div>
  );
}

function ContributorSummary({ contributor, onEdit, disabled }) {
  return (
    <SummaryCard
      title={contributor.nombre}
      subtitle={`RUC ${contributor.ruc}`}
      onEdit={onEdit}
      editLabel="Editar RUC"
      disabled={disabled}
    >
      <dl className="detail-grid">
        <Detail label="Tipo" value={contributor.tipoContribuyente} />
        <Detail label="Estado" value={contributor.estado} />
        <Detail label="Actividad principal" value={contributor.actividadEconomicaPrincipal} />
        <Detail label="Régimen" value={contributor.regimen} />
        <Detail label="Inicio de actividades" value={contributor.fechaInicioActividades} />
      </dl>
    </SummaryCard>
  );
}

function VehicleSummary({ vehicle, onEdit, disabled }) {
  return (
    <SummaryCard
      title={`${vehicle.marca} ${vehicle.modelo}`}
      subtitle={`Placa ${vehicle.placa} · ${vehicle.anioModelo}`}
      onEdit={onEdit}
      editLabel="Editar placa"
      disabled={disabled}
    >
      <dl className="detail-grid">
        <Detail label="Clase" value={vehicle.claseVehiculo} />
        <Detail label="Color" value={vehicle.color} />
        <Detail label="Cilindraje" value={vehicle.cilindraje} />
      </dl>
    </SummaryCard>
  );
}

function ErrorNotice({ error }) {
  return (
    <div className="api-error" role="alert">
      <span className="error-icon" aria-hidden="true">!</span>
      <div>
        <strong>No se pudo completar este paso</strong>
        <p>{error.message}</p>
      </div>
    </div>
  );
}

function LicenseResult({ result }) {
  const points = new Intl.NumberFormat("es-EC", {
    maximumFractionDigits: 2,
  }).format(result.puntosDisponibles);
  const fromCache = result.origen === "CACHE";

  return (
    <section className={`license-result${result.desactualizado ? " is-stale" : ""}`} aria-live="polite">
      <div className="license-result-topline">
        <span className="result-overline">RESUMEN DE LICENCIA</span>
        <span className={`origin-badge${fromCache ? " is-cache" : ""}`}>
          <span aria-hidden="true">●</span>
          {fromCache ? "Redis Cloud · caché" : "Consulta ANT"}
        </span>
      </div>
      <div className="points-display">
        <strong>{points}</strong>
        <span>puntos disponibles</span>
      </div>
      <p className="updated-at">Actualizado: {fechaLegible(result.actualizadoEn)}</p>
      {result.desactualizado && (
        <div className="stale-notice" role="status">
          ANT no respondió. Se muestran los últimos puntos válidos guardados en caché.
        </div>
      )}
    </section>
  );
}

export default function App() {
  const [state, dispatch] = useReducer(reducirConsulta, estadoInicialConsulta);
  const [fieldErrors, setFieldErrors] = useState({});
  const requestInFlight = useRef(false);

  function goToStep(paso) {
    dispatch({ type: "paso/volver", paso });
    setFieldErrors({});
  }

  function changeField(campo, valor) {
    dispatch({ type: "campo/cambiado", campo, valor });
    setFieldErrors((current) => {
      const next = { ...current };
      const toClear =
        campo === "ruc"
          ? ["correo", "ruc", "placa", "cedula"]
          : campo === "placa"
            ? ["placa", "cedula"]
            : [campo];
      toClear.forEach((key) => delete next[key]);
      return next;
    });
  }

  async function ejecutarConsulta(consulta, resultadoAction) {
    if (requestInFlight.current) return;
    requestInFlight.current = true;
    dispatch({ type: "solicitud/iniciada" });
    setFieldErrors({});

    try {
      const resultado = await consulta();
      dispatch({ type: resultadoAction, resultado });
    } catch (error) {
      const safeError = {
        code: typeof error?.code === "string" ? error.code : "REQUEST_FAILED",
        message:
          typeof error?.message === "string"
            ? error.message
            : "No se pudo completar la consulta. Inténtelo de nuevo.",
      };
      dispatch({ type: "solicitud/fallida", error: safeError });
      setFieldErrors(mapearErroresDeCampo(error?.fieldErrors));
    } finally {
      requestInFlight.current = false;
    }
  }

  function submitContribuyente(event) {
    event.preventDefault();
    const errors = {
      correo: validarCorreo(state.correo),
      ruc: validarRuc(state.ruc),
    };
    const foundErrors = Object.fromEntries(
      Object.entries(errors).filter(([, message]) => message),
    );
    setFieldErrors(foundErrors);
    if (Object.keys(foundErrors).length > 0) return;

    void ejecutarConsulta(
      () =>
        consultarContribuyente({
          correo: state.correo.trim(),
          ruc: state.ruc.trim(),
        }),
      "contribuyente/recibido",
    );
  }

  function submitVehiculo(event) {
    event.preventDefault();
    const placaError = validarPlaca(state.placa);
    setFieldErrors(placaError ? { placa: placaError } : {});
    if (placaError) return;

    const placa = normalizarPlaca(state.placa);
    if (placa !== state.placa) {
      dispatch({ type: "campo/cambiado", campo: "placa", valor: placa });
    }
    void ejecutarConsulta(() => consultarVehiculo({ placa }), "vehiculo/recibido");
  }

  function submitLicencia(event) {
    event.preventDefault();
    const cedulaError = validarCedula(state.cedula);
    setFieldErrors(cedulaError ? { cedula: cedulaError } : {});
    if (cedulaError) return;

    void ejecutarConsulta(
      () =>
        consultarLicencia({
          cedula: state.cedula.trim(),
          placa: normalizarPlaca(state.placa),
        }),
      "licencia/recibida",
    );
  }

  function reiniciarConsulta() {
    dispatch({ type: "consulta/reiniciada" });
    setFieldErrors({});
  }

  const explicacion = EXPLICACION_POR_PASO[state.paso];
  const tieneResultados = Boolean(state.contribuyente || state.vehiculo || state.licencia);

  return (
    <div className="app-shell">
      <header className="topbar">
        <a className="brand" href="#inicio" aria-label="Sistema SRI ANT, inicio">
          <span className="brand-mark" aria-hidden="true">S</span>
          <span className="brand-name">SRI <span>×</span> ANT</span>
        </a>
        <div className="topbar-note">
          <span className="topbar-dot" aria-hidden="true" />
          CONSULTA GUIADA
        </div>
      </header>

      <main className="page-content" id="inicio">
        <section className="page-heading">
          <div>
            <p className="eyebrow">SERVICIOS CIUDADANOS · ECUADOR</p>
            <h1>Consulta SRI <span>y</span> ANT</h1>
            <p className="page-intro">
              Verifica un contribuyente, consulta su vehículo y revisa los puntos de su licencia en un solo recorrido.
            </p>
          </div>
          <div className="heading-stamp" aria-hidden="true">
            <span>REST</span><span className="stamp-divider" /><span>JSON</span>
          </div>
        </section>

        <section className="workspace-card" aria-label="Flujo de consultas">
          <Stepper state={state} onGoToStep={goToStep} />

          <div className="workspace-grid">
            <section className="workflow-panel" aria-labelledby="step-heading">
              <div className="workflow-title-row">
                <div>
                  <p className="eyebrow">PASO {String(state.paso).padStart(2, "0")} DE 03</p>
                  <h2 id="step-heading">
                    {state.paso === 1 && "Verifica al contribuyente"}
                    {state.paso === 2 && "Consulta el vehículo"}
                    {state.paso === 3 && "Revisa los puntos de licencia"}
                  </h2>
                </div>
                {tieneResultados && (
                  <button className="reset-button" type="button" onClick={reiniciarConsulta} disabled={state.cargando}>
                    Nueva consulta
                  </button>
                )}
              </div>

              {state.paso > 1 && state.contribuyente && (
                <ContributorSummary
                  contributor={state.contribuyente}
                  onEdit={() => goToStep(1)}
                  disabled={state.cargando}
                />
              )}

              {state.paso > 1 && state.paso === 2 && state.vehiculo && (
                <VehicleSummary vehicle={state.vehiculo} onEdit={() => goToStep(2)} disabled={state.cargando} />
              )}

              {state.paso === 3 && state.vehiculo && (
                <VehicleSummary vehicle={state.vehiculo} onEdit={() => goToStep(2)} disabled={state.cargando} />
              )}

              {state.paso === 1 && (
                <form className="consultation-form" onSubmit={submitContribuyente} noValidate>
                  <p className="form-intro">Ingresa el correo y el RUC de una persona natural.</p>
                  <FormField
                    id="correo"
                    label="Correo electrónico"
                    type="email"
                    autoComplete="email"
                    placeholder="nombre@ejemplo.com"
                    maxLength={254}
                    helper="Usaremos este dato para identificar la consulta."
                    disabled={state.cargando}
                    value={state.correo}
                    error={fieldErrors.correo}
                    onChange={(event) => changeField("correo", event.target.value)}
                  />
                  <FormField
                    id="ruc"
                    label="RUC"
                    type="text"
                    inputMode="numeric"
                    autoComplete="off"
                    placeholder="13 dígitos"
                    maxLength={13}
                    helper="El SRI confirmará que corresponda a una persona natural."
                    disabled={state.cargando}
                    value={state.ruc}
                    error={fieldErrors.ruc}
                    onChange={(event) => changeField("ruc", event.target.value)}
                  />
                  {state.error && <ErrorNotice error={state.error} />}
                  <button className="primary-button" type="submit" disabled={state.cargando}>
                    {state.cargando ? "Consultando al SRI…" : "Verificar contribuyente"}
                    {!state.cargando && <span aria-hidden="true">→</span>}
                  </button>
                </form>
              )}

              {state.paso === 2 && (
                <form className="consultation-form" onSubmit={submitVehiculo} noValidate>
                  <p className="form-intro">Ingresa la matrícula para obtener el resumen del vehículo.</p>
                  <FormField
                    id="placa"
                    label="Placa del vehículo"
                    type="text"
                    inputMode="text"
                    autoComplete="off"
                    placeholder="Ej. ABC1234"
                    maxLength={8}
                    helper="De 5 a 8 letras, números o guiones."
                    disabled={state.cargando}
                    value={state.placa}
                    error={fieldErrors.placa}
                    onChange={(event) => changeField("placa", event.target.value)}
                  />
                  {state.error && <ErrorNotice error={state.error} />}
                  <button className="primary-button" type="submit" disabled={state.cargando}>
                    {state.cargando ? "Consultando al SRI…" : "Consultar vehículo"}
                    {!state.cargando && <span aria-hidden="true">→</span>}
                  </button>
                </form>
              )}

              {state.paso === 3 && (
                <>
                  <form className="consultation-form license-form" onSubmit={submitLicencia} noValidate>
                    <p className="form-intro">La consulta de puntos requiere la cédula del titular y la placa registrada.</p>
                    <div className="plate-reference">
                      <span className="plate-icon" aria-hidden="true">EC</span>
                      <span><small>PLACA ASOCIADA</small><strong>{state.placa}</strong></span>
                      <span className="reference-check" aria-label="Vehículo consultado">✓</span>
                    </div>
                    <FormField
                      id="cedula"
                      label="Cédula"
                      type="text"
                      inputMode="numeric"
                      autoComplete="off"
                      placeholder="10 dígitos"
                      maxLength={10}
                      helper="La cédula se envía al backend junto con la placa."
                      disabled={state.cargando}
                      value={state.cedula}
                      error={fieldErrors.cedula}
                      onChange={(event) => changeField("cedula", event.target.value)}
                    />
                    {state.error && <ErrorNotice error={state.error} />}
                    <button className="primary-button" type="submit" disabled={state.cargando}>
                      {state.cargando ? "Consultando ANT…" : "Consultar puntos"}
                      {!state.cargando && <span aria-hidden="true">→</span>}
                    </button>
                  </form>
                  {state.licencia && <LicenseResult result={state.licencia} />}
                </>
              )}
            </section>

            <aside className="explanation-panel" aria-live="polite">
              <div className="explanation-label">
                <span className="explanation-icon" aria-hidden="true">↗</span>
                <span>ASÍ FUNCIONA</span>
              </div>
              <h3>{explicacion.etiqueta}</h3>
              <p className="explanation-copy">{explicacion.texto}</p>
              <div className="request-route" aria-label={`Ruta de consulta: React, backend Java y ${explicacion.destino}`}>
                <div className="route-node"><span className="route-node-icon">R</span><span>React</span></div>
                <span className="route-arrow" aria-hidden="true">→</span>
                <div className="route-node"><span className="route-node-icon backend-icon">J</span><span>Backend Java</span></div>
                <span className="route-arrow" aria-hidden="true">→</span>
                <div className="route-destination">{explicacion.destino}</div>
              </div>
              <div className="explanation-divider" />
              <p className="privacy-note">
                <span aria-hidden="true">◇</span>
                El navegador conserva el progreso durante esta sesión. La caché guarda únicamente el resumen de puntos permitido.
              </p>
            </aside>
          </div>
        </section>

        <footer className="page-footer">
          <span>Arquitectura C4 · Integraciones externas</span>
          <span>REST <i /> Spring Boot <i /> React</span>
        </footer>
      </main>
    </div>
  );
}
