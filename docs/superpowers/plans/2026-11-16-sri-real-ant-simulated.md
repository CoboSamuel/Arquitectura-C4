# SRI real y ANT simulada Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Cumplir el deber con consultas reales al SRI, mantener la consulta ANT con su contrato pero simulada por su indisponibilidad, y demostrar el fallback de Redis Cloud.

**Architecture:** Los clientes `SRIContribuyenteClient` y `SRIVehiculoClient` seguirán llamando los endpoints oficiales del SRI bajo el perfil `live`. El puerto `PuntosLicenciaProvider` tendrá un `SimulatedAntClient` activo por defecto y el `ANTClient` HTTP real disponible bajo el perfil adicional `ant-real`; `LicenciaCacheService` seguirá encapsulando frescura, retención y fallback.

**Tech Stack:** Java 21, Spring Boot 3.5, Spring RestClient, Spring Data Redis, Jsoup, React 19, Vite y Node test runner.

**Spec:** `docs/superpowers/specs/2026-11-16-etapa-8-live-integrations-design.md`

## Global Constraints

- Los tres servicios SRI se consultan por HTTP real con los endpoints oficiales definidos en el enunciado.
- ANT simulado por defecto: el request usa cédula y placa, y la respuesta mantiene el contrato local de puntos, fecha, origen y frescura.
- El cliente HTTP ANT se activa solo con `live,ant-real`; no se presenta como real cuando está activa la simulación.
- Frescura Redis: `15m`; retención máxima del último dato: `7d`.
- La clave Redis usa HMAC-SHA-256 de cédula y placa normalizadas; el valor solo guarda puntos y fecha.
- No guardar secretos, identificadores personales, URI Redis ni respuestas reales en el repositorio o en el chat.
- No modificar los diagramas C4 ni el enlace existente de IcePanel al repositorio `https://github.com/CoboSamuel/Arquitectura-C4.git`.
- No ejecutar `git add`, `git commit` ni `git push`; el usuario revisará y hará el staging/commit.

## Review Focus

- Arranque sin perfil activo: SRI real y `SimulatedAntClient` deben quedar disponibles, sin dos proveedores ANT ambiguos.
- `ANT_SIMULATION_AVAILABLE=false`: una consulta ANT fallida debe usar la última entrada Redis válida y marcarla como desactualizada.
- Sin secreto HMAC o sin caché utilizable: debe responder el error público `503` definido, sin llamar al proveedor ni filtrar identificadores.
- Respuesta válida del SRI con varios registros: debe seleccionarse el RUC/placa solicitados y rechazarse una respuesta ambigua.
- La UI debe decir “Servicio ANT simulado” para `SIMULADO` y “Última consulta disponible” para `CACHE`, sin presentar ambos como datos actuales de ANT.

### Task 1: Proveedor ANT simulado y procedencia

**Files:**
- Create: `backend/src/main/java/ec/edu/udla/arquitecturac4/consulta/ant/AntSimulationProperties.java`
- Create: `backend/src/main/java/ec/edu/udla/arquitecturac4/consulta/ant/SimulatedAntClient.java`
- Modify: `backend/src/main/java/ec/edu/udla/arquitecturac4/consulta/ant/AntClientConfiguration.java`
- Modify: `backend/src/main/java/ec/edu/udla/arquitecturac4/consulta/spi/PuntosLicenciaProvider.java`
- Modify: `backend/src/main/java/ec/edu/udla/arquitecturac4/consulta/cache/ConsultaLicenciaResult.java`
- Modify: `backend/src/test/java/ec/edu/udla/arquitecturac4/consulta/live/LiveApplicationContextTest.java`
- Create: `backend/src/test/java/ec/edu/udla/arquitecturac4/consulta/ant/SimulatedAntClientTest.java`

**Interfaces:**
- `PuntosLicenciaProvider.consultarPuntos(String cedula, String placa)` conserva `Optional<BigDecimal>` y expone `Fuente fuente()`, donde `Fuente` es `REAL` o `SIMULADA`.
- `SimulatedAntClient` implementa el puerto, devuelve por defecto un valor no personal de prueba, reporta `Fuente.SIMULADA` y lanza `AntNoDisponibleException` cuando `integrations.ant.simulation.available=false`.
- `ConsultaLicenciaResult.Origen` agrega `SIMULADO`; los valores `ANT` y `CACHE` existentes conservan su significado.

- [ ] **Step 1: Write the failing tests** — agregar prueba de puntos configurados y fuente `SIMULADA`, prueba de indisponibilidad configurable y prueba de contexto que espere el proveedor simulado bajo `live`.
- [ ] **Step 2: Run tests to verify they fail** — ejecutar `mvn -B -ntp -Dtest=SimulatedAntClientTest,LiveApplicationContextTest test`; esperar fallos de clase/bean/enum inexistentes.
- [ ] **Step 3: Implement the minimal provider model** — crear propiedades `points`/`available` con valores seguros de prueba, el cliente simulado y `PuntosLicenciaProvider.Fuente`; conservar `Optional<BigDecimal>` para evitar un cambio innecesario en los consumidores existentes.
- [ ] **Step 4: Run focused tests to verify they pass** — repetir el comando focalizado y confirmar que el proveedor simulado es el único activo en `live`.

### Task 2: Wiring de perfiles y caché de licencia

**Files:**
- Modify: `backend/src/main/java/ec/edu/udla/arquitecturac4/consulta/ant/ANTClient.java`
- Modify: `backend/src/main/java/ec/edu/udla/arquitecturac4/consulta/cache/LicenciaCacheService.java`
- Modify: `backend/src/main/resources/application.yml`
- Modify: `backend/src/main/java/ec/edu/udla/arquitecturac4/consulta/dto/ConsultaLicenciaResponse.java`
- Modify: `backend/src/test/java/ec/edu/udla/arquitecturac4/consulta/cache/LicenciaCacheServiceTest.java`
- Modify: `backend/src/test/java/ec/edu/udla/arquitecturac4/consulta/ant/ANTClientTest.java`
- Modify: `backend/src/test/java/ec/edu/udla/arquitecturac4/consulta/live/LiveApplicationContextTest.java`
- Create: `backend/src/test/java/ec/edu/udla/arquitecturac4/consulta/live/AntRealApplicationContextTest.java`

**Interfaces:**
- `ANTClient` conserva la ruta, parámetros, timeout y parser actuales, cambia a perfil `ant-real` y usa la fuente predeterminada `REAL` del puerto.
- `LicenciaCacheService` consume `Optional<BigDecimal>`, consulta `PuntosLicenciaProvider.fuente()`, persiste solo puntos/fecha y traduce `Fuente.REAL` a `Origen.ANT` y `Fuente.SIMULADA` a `Origen.SIMULADO`.
- `ConsultaLicenciaResponse.origen` admite `ANT`, `SIMULADO` y `CACHE`; el fallback conserva `CACHE` y `desactualizado=true`.

- [ ] **Step 1: Write the failing tests** — cubrir que una respuesta simulada válida se guarda y retorna `SIMULADO`, que una falla simulada con caché retenida retorna `CACHE` desactualizada, y que el cliente HTTP ANT no se registra en `live` pero sí con `live,ant-real`.
- [ ] **Step 2: Run tests to verify they fail** — ejecutar `mvn -B -ntp -Dtest=LicenciaCacheServiceTest,ANTClientTest,LiveApplicationContextTest,AntRealApplicationContextTest test`; esperar fallos por la procedencia y el perfil.
- [ ] **Step 3: Implement the minimal wiring** — ajustar `LicenciaCacheService`, perfiles y propiedades; conservar la política de 15 minutos/7 días, el `503` por secreto faltante y el fallback existente sin guardar identificadores.
- [ ] **Step 4: Run focused tests to verify they pass** — repetir el comando y confirmar que Redis solo almacena puntos/fecha y que las respuestas ANT reales siguen cubiertas por HTTP simulado.

### Task 3: Interfaz, contrato OpenAPI y guía de operación

**Files:**
- Create: `frontend/src/consulta/licencia.js`
- Create: `frontend/src/consulta/licencia.test.js`
- Modify: `frontend/src/App.jsx`
- Modify: `frontend/src/App.test.js`
- Modify: `docs/api/openapi.yaml`
- Modify: `docs/CONFIGURACION_LOCAL.md`
- Modify: `.env.example`

**Interfaces:**
- `describirOrigenLicencia(origen)` devuelve etiquetas de UI para `SIMULADO`, `CACHE` y `ANT`.
- La pantalla conserva el flujo de tres pasos; para `SIMULADO` muestra que ANT está simulada y para `CACHE` mantiene la advertencia de consulta anterior.
- OpenAPI elimina la descripción de “fixture sintética” del vehículo y agrega `SIMULADO` al enum de origen de licencia.
- La guía explica que SRI es real, ANT simulada por defecto, cómo activar `live,ant-real` cuando se recupere, y cómo demostrar el fallback sin exponer secretos.

- [ ] **Step 1: Write the failing test** — agregar `licencia.test.js` para exigir la etiqueta “Servicio ANT simulado” y una prueba de render/flujo que no permita presentar `SIMULADO` como “Actualizado ahora”.
- [ ] **Step 2: Run tests to verify they fail** — ejecutar `npm test -- --test-name-pattern="licencia|simulad"`; esperar que la función no exista o que la etiqueta no aparezca.
- [ ] **Step 3: Implement the minimal UI and documentation changes** — centralizar el mapeo de etiquetas, usarlo en `LicenseResult`, y actualizar OpenAPI, `.env.example` y la guía con los comandos PowerShell y evidencias sanitizadas.
- [ ] **Step 4: Run frontend tests and build** — ejecutar `npm test` y `npm run build`; confirmar que el flujo inicial y la indicación de simulación pasan.

### Task 4: Verificación completa y cierre de trazabilidad

**Files:**
- Modify: `docs/superpowers/plans/2026-11-16-etapa-8-live-integrations.md`
- Modify: `docs/superpowers/specs/2026-11-16-etapa-8-live-integrations-design.md`

- [ ] **Step 1: Re-read the acceptance criteria** — comprobar SRI real, ANT simulado por defecto, proveedor real opt-in, caché, advertencias, OpenAPI, guía y C4 sin cambios.
- [ ] **Step 2: Run the backend suite** — `Set-Location backend; mvn -B -ntp test`; esperar `BUILD SUCCESS` y cero fallos.
- [ ] **Step 3: Run the frontend suite and build** — `Set-Location frontend; npm test; npm run build`; esperar todas las pruebas aprobadas y build exitoso.
- [ ] **Step 4: Run repository checks** — desde la raíz ejecutar `git diff --check` y revisar que no aparezcan secretos, datos personales, URI Redis ni cambios en IcePanel.
- [ ] **Step 5: Document remaining manual evidence** — actualizar la bitácora/guía con las capturas que el usuario debe tomar: respuesta real del SRI, respuesta de vehículo, ANT simulado, fallback Redis y pruebas/build; no capturar credenciales ni identificadores.

