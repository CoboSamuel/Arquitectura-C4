import React, { useEffect, useReducer, useRef, useState } from "react";
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
import { describirError } from "./consulta/mensajesError.js";
import { calcularProgreso } from "./consulta/progreso.js";

const PASOS = [
  { id: 1, numero: "01", titulo: "Datos personales" },
  { id: 2, numero: "02", titulo: "Vehículo" },
  { id: 3, numero: "03", titulo: "Licencia" },
];

const AYUDA_POR_PASO = {
  1: {
    etiqueta: "Antes de empezar",
    texto: "Ten a mano estos datos para completar la consulta.",
    items: ["Correo electrónico", "RUC de persona natural", "Placa del vehículo", "Cédula del titular"],
  },
  2: {
    etiqueta: "Sobre la placa",
    texto: "Escríbela tal como aparece en la matrícula. Puedes volver y corregirla antes de continuar.",
    items: ["No incluyas espacios", "Ejemplo: ABC1234"],
  },
  3: {
    etiqueta: "Sobre los puntos",
    texto: "Ingresa la cédula del titular. Si no se puede consultar en este momento, te mostraremos la información disponible de una consulta anterior.",
    items: ["Se usará junto con la placa ingresada"],
  },
};

const ORDEN_CAMPOS = ["correo", "ruc", "placa", "cedula"];

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

function unirTexto(...partes) {
  return partes
    .filter((parte) => typeof parte === "string" && parte.trim() !== "")
    .join(" ");
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
            <p className="summary-kicker">DATOS CONFIRMADOS</p>
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

function ProgressBar({ state }) {
  const progreso = calcularProgreso(state);

  return (
    <div className="progress-summary">
      <div
        className="progress-track"
        role="progressbar"
        aria-label="Avance del recorrido"
        aria-valuemin={0}
        aria-valuemax={progreso.total}
        aria-valuenow={progreso.completados}
        aria-valuetext={progreso.texto}
      >
        <span className="progress-fill" style={{ width: `${progreso.porcentaje}%` }} />
      </div>
      <p className="progress-label">{progreso.texto}</p>
    </div>
  );
}

function StatusRegion({ cargando, texto }) {
  // La región existe siempre para que los lectores de pantalla anuncien el cambio.
  return (
    <div className="status-region" role="status">
      {cargando && (
        <div className="loading-status">
          <span className="spinner" aria-hidden="true" />
          <div>
            <strong>{texto}</strong>
            <p>Espera un momento mientras buscamos la información.</p>
          </div>
        </div>
      )}
    </div>
  );
}

function EstadoPill({ value }) {
  return <span className="status-pill">{value}</span>;
}

function ContributorSummary({ contributor, onEdit, disabled }) {
  return (
    <SummaryCard
      title={unirTexto(contributor.nombre) || "Titular consultado"}
      subtitle={contributor.ruc ? `RUC ${contributor.ruc}` : undefined}
      onEdit={onEdit}
      editLabel="Editar RUC"
      disabled={disabled}
    >
      <dl className="detail-grid">
        <Detail label="Tipo" value={contributor.tipoContribuyente} />
        <Detail label="Estado" value={contributor.estado ? <EstadoPill value={contributor.estado} /> : null} />
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
      title={unirTexto(vehicle.marca, vehicle.modelo) || "Vehículo consultado"}
      subtitle={vehicle.anioModelo ? `Año modelo ${vehicle.anioModelo}` : undefined}
      onEdit={onEdit}
      editLabel="Editar placa"
      disabled={disabled}
    >
      {vehicle.placa && (
        <p className="plate-chip">
          <span className="plate-chip-label">Placa</span>
          <strong>{vehicle.placa}</strong>
        </p>
      )}
      <dl className="detail-grid">
        <Detail label="Clase" value={vehicle.claseVehiculo} />
        <Detail label="Color" value={vehicle.color} />
        <Detail label="Cilindraje" value={vehicle.cilindraje} />
      </dl>
    </SummaryCard>
  );
}

function ErrorNotice({ error, onRetry, ref }) {
  const info = describirError(error);

  return (
    <div className="api-error" role="group" aria-labelledby="error-titulo" tabIndex={-1} ref={ref}>
      <span className="error-icon" aria-hidden="true">!</span>
      <div className="api-error-body">
        <strong id="error-titulo">{info.titulo}</strong>
        <p>{info.mensaje}</p>
        {info.sugerencia && <p className="api-error-hint">{info.sugerencia}</p>}
        {info.reintentable && onRetry && (
          <button className="retry-button" type="button" onClick={onRetry}>
            Reintentar consulta
          </button>
        )}
      </div>
    </div>
  );
}

function LicenseResult({ result, ref }) {
  const points = new Intl.NumberFormat("es-EC", {
    maximumFractionDigits: 2,
  }).format(result.puntosDisponibles);
  const fromCache = result.origen === "CACHE";

  return (
    <section
      className={`license-result${result.desactualizado ? " is-stale" : ""}`}
      aria-labelledby="license-result-title"
      tabIndex={-1}
      ref={ref}
    >
      <div className="license-result-topline">
        <h3 className="result-overline" id="license-result-title">PUNTOS DE TU LICENCIA</h3>
        <span className={`origin-badge${fromCache ? " is-cache" : ""}`}>
          <span aria-hidden="true">●</span>
          {fromCache ? "Última consulta disponible" : "Actualizado ahora"}
        </span>
      </div>
      <div className="points-display">
        <strong>{points}</strong>
        <span>puntos disponibles</span>
      </div>
      <dl className="detail-grid result-facts">
        <Detail label="Fecha de consulta" value={fechaLegible(result.actualizadoEn)} />
        <Detail
          label="Estado"
          value={result.desactualizado ? "Consulta anterior" : "Actualizado ahora"}
        />
      </dl>
      {result.desactualizado && (
        <div className="stale-notice" role="status">
          No fue posible consultar los puntos en este momento. Te mostramos la información disponible de una consulta anterior.
        </div>
      )}
    </section>
  );
}

export default function App() {
  const [state, dispatch] = useReducer(reducirConsulta, estadoInicialConsulta);
  const [fieldErrors, setFieldErrors] = useState({});
  const requestInFlight = useRef(false);
  const headingRef = useRef(null);
  const errorRef = useRef(null);
  const resultRef = useRef(null);
  const pasoPrevio = useRef(state.paso);
  const enfocarCampoPendiente = useRef(false);

  // Al cambiar de paso, el foco pasa al título para que teclado y lector de pantalla sigan el avance.
  useEffect(() => {
    if (pasoPrevio.current !== state.paso) {
      pasoPrevio.current = state.paso;
      headingRef.current?.focus();
    }
  }, [state.paso]);

  // Si la consulta falla, el foco pasa al aviso de error.
  useEffect(() => {
    if (state.error) errorRef.current?.focus();
  }, [state.error]);

  // Cuando llega el resultado de licencia, el foco pasa al resultado.
  useEffect(() => {
    if (state.licencia) resultRef.current?.focus();
  }, [state.licencia]);

  // Tras un envío con campos inválidos, el foco va al primer campo con error.
  useEffect(() => {
    if (!enfocarCampoPendiente.current || state.cargando) return;
    enfocarCampoPendiente.current = false;
    const primero = ORDEN_CAMPOS.find((campo) => fieldErrors[campo]);
    if (primero) document.getElementById(primero)?.focus();
  }, [fieldErrors, state.cargando]);

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
      enfocarCampoPendiente.current = true;
    } finally {
      requestInFlight.current = false;
    }
  }

  function submitContribuyente(event) {
    event?.preventDefault();
    const errors = {
      correo: validarCorreo(state.correo),
      ruc: validarRuc(state.ruc),
    };
    const foundErrors = Object.fromEntries(
      Object.entries(errors).filter(([, message]) => message),
    );
    setFieldErrors(foundErrors);
    if (Object.keys(foundErrors).length > 0) {
      enfocarCampoPendiente.current = true;
      return;
    }

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
    event?.preventDefault();
    const placaError = validarPlaca(state.placa);
    setFieldErrors(placaError ? { placa: placaError } : {});
    if (placaError) {
      enfocarCampoPendiente.current = true;
      return;
    }

    const placa = normalizarPlaca(state.placa);
    if (placa !== state.placa) {
      dispatch({ type: "campo/cambiado", campo: "placa", valor: placa });
    }
    void ejecutarConsulta(() => consultarVehiculo({ placa }), "vehiculo/recibido");
  }

  function submitLicencia(event) {
    event?.preventDefault();
    const cedulaError = validarCedula(state.cedula);
    setFieldErrors(cedulaError ? { cedula: cedulaError } : {});
    if (cedulaError) {
      enfocarCampoPendiente.current = true;
      return;
    }

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

  const ayuda = AYUDA_POR_PASO[state.paso];
  const tieneResultados = Boolean(state.contribuyente || state.vehiculo || state.licencia);

  return (
    <div className="app-shell">
      <a className="skip-link" href="#inicio">Saltar al contenido principal</a>
      <header className="topbar">
        <a className="brand" href="#inicio" aria-label="Consulta ciudadana, inicio">
          <span className="brand-mark" aria-hidden="true">EC</span>
          <span className="brand-name">Consulta ciudadana</span>
        </a>
        <nav className="topnav" aria-label="Secciones de la página">
          <a href="#consulta">Iniciar consulta</a>
          <a href="#ayuda">Ayuda</a>
        </nav>
        <div className="topbar-note">
          <span className="topbar-dot" aria-hidden="true" />
          RECORRIDO EN 3 PASOS
        </div>
      </header>

      <main className="page-content" id="inicio" tabIndex={-1}>
        <section className="page-heading">
          <div>
            <p className="eyebrow">CONSULTA CIUDADANA</p>
            <h1>Consulta tus datos</h1>
            <p className="page-intro">
              Revisa la información de tu RUC, tu vehículo y tu licencia paso a paso.
            </p>
            <ul className="prep-list" aria-label="Datos que necesitará">
              <li>Correo electrónico y RUC</li>
              <li>Placa del vehículo</li>
              <li>Cédula del titular</li>
            </ul>
          </div>
        </section>

        <section className="workspace-card" id="consulta" aria-label="Consulta paso a paso">
          <Stepper state={state} onGoToStep={goToStep} />
          <ProgressBar state={state} />

          <div className="workspace-grid">
            <section className="workflow-panel" aria-labelledby="step-heading">
              <div className="workflow-title-row">
                <div>
                  <p className="eyebrow">PASO {state.paso} DE 3</p>
                  <h2 id="step-heading" ref={headingRef} tabIndex={-1}>
                    {state.paso === 1 && "Tus datos"}
                    {state.paso === 2 && "Tu vehículo"}
                    {state.paso === 3 && "Puntos de tu licencia"}
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
                <form className="consultation-form" onSubmit={submitContribuyente} aria-busy={state.cargando} noValidate>
                  <p className="form-intro">Para comenzar, ingresa tu correo electrónico y el RUC del titular.</p>
                  <FormField
                    id="correo"
                    label="Correo electrónico"
                    type="email"
                    autoComplete="email"
                    placeholder="nombre@ejemplo.com"
                    maxLength={254}
                    helper="Lo usaremos solo para esta consulta."
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
                    helper="Ingresa los 13 dígitos del RUC de una persona natural."
                    disabled={state.cargando}
                    value={state.ruc}
                    error={fieldErrors.ruc}
                    onChange={(event) => changeField("ruc", event.target.value)}
                  />
                  {state.error && (
                    <ErrorNotice error={state.error} onRetry={() => submitContribuyente()} ref={errorRef} />
                  )}
                  <StatusRegion cargando={state.cargando} texto="Buscando tus datos…" />
                  <button className="primary-button" type="submit" disabled={state.cargando}>
                    {state.cargando ? "Buscando tus datos…" : "Continuar"}
                    {state.cargando ? <span className="spinner" aria-hidden="true" /> : <span aria-hidden="true">→</span>}
                  </button>
                </form>
              )}

              {state.paso === 2 && (
                <form className="consultation-form" onSubmit={submitVehiculo} aria-busy={state.cargando} noValidate>
                  <p className="form-intro">Escribe la placa del vehículo que deseas consultar.</p>
                  <FormField
                    id="placa"
                    label="Placa del vehículo"
                    type="text"
                    inputMode="text"
                    autoComplete="off"
                    placeholder="Ej. ABC1234"
                    autoCapitalize="characters"
                    spellCheck={false}
                    maxLength={8}
                    helper="Escríbela tal como aparece en la matrícula."
                    disabled={state.cargando}
                    value={state.placa}
                    error={fieldErrors.placa}
                    onChange={(event) => changeField("placa", event.target.value)}
                  />
                  {state.error && (
                    <ErrorNotice error={state.error} onRetry={() => submitVehiculo()} ref={errorRef} />
                  )}
                  <StatusRegion cargando={state.cargando} texto="Buscando el vehículo…" />
                  <button className="primary-button" type="submit" disabled={state.cargando}>
                    {state.cargando ? "Buscando el vehículo…" : "Continuar"}
                    {state.cargando ? <span className="spinner" aria-hidden="true" /> : <span aria-hidden="true">→</span>}
                  </button>
                </form>
              )}

              {state.paso === 3 && (
                <>
                  <form className="consultation-form license-form" onSubmit={submitLicencia} aria-busy={state.cargando} noValidate>
                    <p className="form-intro">Ingresa la cédula del titular para consultar los puntos de su licencia.</p>
                    <div className="plate-reference">
                      <span className="plate-icon" aria-hidden="true">EC</span>
                      <span><small>PLACA DEL VEHÍCULO</small><strong>{state.placa}</strong></span>
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
                      helper="Escribe los 10 dígitos de la cédula del titular."
                      disabled={state.cargando}
                      value={state.cedula}
                      error={fieldErrors.cedula}
                      onChange={(event) => changeField("cedula", event.target.value)}
                    />
                    {state.error && (
                      <ErrorNotice error={state.error} onRetry={() => submitLicencia()} ref={errorRef} />
                    )}
                    <StatusRegion cargando={state.cargando} texto="Buscando los puntos…" />
                    <button className="primary-button" type="submit" disabled={state.cargando}>
                      {state.cargando ? "Buscando los puntos…" : "Consultar puntos"}
                      {state.cargando ? <span className="spinner" aria-hidden="true" /> : <span aria-hidden="true">→</span>}
                    </button>
                  </form>
                  {state.licencia && <LicenseResult result={state.licencia} ref={resultRef} />}
                </>
              )}
            </section>

            <aside className="explanation-panel" id="ayuda" aria-label="Información útil">
              <div className="explanation-label">
                <span className="explanation-icon" aria-hidden="true">i</span>
                <span>INFORMACIÓN ÚTIL</span>
              </div>
              <h3>{ayuda.etiqueta}</h3>
              <p className="explanation-copy">{ayuda.texto}</p>
              <ul className="help-list">
                {ayuda.items.map((item) => <li key={item}>{item}</li>)}
              </ul>
              <div className="explanation-divider" />
              <p className="privacy-note" id="privacidad">
                <span aria-hidden="true">◇</span>
                Tus datos no se guardan en esta página. Este es un proyecto académico y no un portal oficial.
              </p>
            </aside>
          </div>
        </section>

        <footer className="page-footer">
          <span className="footer-disclaimer">
            Proyecto académico de demostración · No es un portal oficial.
          </span>
        </footer>
      </main>
    </div>
  );
}
