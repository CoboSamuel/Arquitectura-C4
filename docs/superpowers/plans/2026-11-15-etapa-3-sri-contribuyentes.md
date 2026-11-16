# Etapa 3 — SRI contribuyentes Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Consultar el SRI desde `POST /api/v1/consultas/contribuyentes`, aceptar solo personas naturales y devolver una respuesta resumida con errores externos controlados.

**Architecture:** Mantener el controlador y servicio Spring MVC existentes. Añadir un `SRIContribuyenteClient` basado en `RestClient`, DTOs externos tolerantes a cambios y un DTO local de respuesta; mapear las excepciones de integración a errores HTTP estables.

**Tech Stack:** Java 21, Spring Boot 3.5.16, Spring MVC `RestClient`, JUnit 5, Mockito, MockMvc y `MockRestServiceServer`.

**Spec:** `docs/superpowers/specs/2026-11-15-etapa-3-sri-contribuyentes-design.md`

## Global Constraints

- Mantener un solo backend modular Spring Boot y respetar los contenedores C4 existentes.
- Usar Java 21 y Spring Boot 3.5.16; no añadir dependencias para el cliente HTTP.
- No enviar el correo al SRI; sus consultas de contribuyentes reciben el RUC.
- No registrar ni devolver el cuerpo crudo del SRI, RUCs, correos ni datos de representantes en errores.
- Automatizar todas las pruebas con respuestas simuladas; ninguna prueba automatizada depende de la red ni de disponibilidad del SRI.
- Mantener estricta la validación JSON de entrada; tolerar propiedades futuras solo en DTOs de respuesta externa.
- No tocar endpoints de vehículos/licencias, React, ANT, Redis ni diagramas IcePanel en esta etapa.
- No ejecutar `git add`, `git commit` ni `git push`; el usuario conserva esas acciones.

## Review Focus

- `existePorNumeroRuc=false`: responder 404 y no consultar el detalle.
- Detalle SRI vacío (`204` o arreglo vacío): responder 404 sin construir un perfil parcial.
- Tipo distinto de persona natural: responder 422 y no devolver campos del registro.
- Timeout, desconexión, HTTP upstream 429/5xx: responder 503; protocolo inesperado o JSON malformado: responder 502, sin filtrar cuerpo ni identificadores.
- Campo JSON desconocido en la respuesta SRI: tolerarlo; campo desconocido en el JSON entrante del usuario: conservar el rechazo 400 existente.

---

### Task 1: Cliente HTTP del SRI y modelos externos

**Files:**
- Create: `backend/src/main/java/ec/edu/udla/arquitecturac4/consulta/sri/SriProperties.java`
- Create: `backend/src/main/java/ec/edu/udla/arquitecturac4/consulta/sri/SriClientConfiguration.java`
- Create: `backend/src/main/java/ec/edu/udla/arquitecturac4/consulta/sri/SRIContribuyenteClient.java`
- Create: `backend/src/main/java/ec/edu/udla/arquitecturac4/consulta/SriNoDisponibleException.java`
- Create: `backend/src/main/java/ec/edu/udla/arquitecturac4/consulta/SriRespuestaInvalidaException.java`
- Create: `backend/src/main/java/ec/edu/udla/arquitecturac4/consulta/sri/dto/SriContribuyenteResponse.java`
- Create: `backend/src/main/java/ec/edu/udla/arquitecturac4/consulta/sri/dto/SriFechasContribuyente.java`
- Test: `backend/src/test/java/ec/edu/udla/arquitecturac4/consulta/sri/SRIContribuyenteClientTest.java`
- Modify: `backend/src/main/resources/application.yml`

**Interfaces:**
- `SriProperties`: base URL, connection timeout and read timeout, bound under `integrations.sri`.
- `SRIContribuyenteClient.existe(String ruc): boolean` calls `existePorNumeroRuc` and parses a JSON boolean.
- `SRIContribuyenteClient.obtener(String ruc): List<SriContribuyenteResponse>` calls `obtenerPorNumerosRuc`; a 204 returns an empty list.
- Client timeouts, disconnection, and upstream 429/5xx throw `SriNoDisponibleException`; malformed JSON, wrong body types, and unexpected statuses throw `SriRespuestaInvalidaException`.
- External DTOs contain only fields required for the local profile and ignore unknown JSON properties.

- [x] **Step 1: Write failing HTTP client tests** for the existence boolean, detail array mapping, unknown external fields, 204, malformed payload, upstream 5xx, and transport failure.
- [x] **Step 2: Run the new test class** with `mvn -B -ntp -Dtest=SRIContribuyenteClientTest test` from `backend`; confirm it fails because the client/configuration is not implemented.
- [x] **Step 3: Implement properties, timeout-configured RestClient, endpoint client and external DTOs.** Use URI builders for query parameters; do not log request values or response bodies.
- [x] **Step 4: Re-run the client test class** and confirm all cases pass.

### Task 2: Contributor business flow and response DTO

**Files:**
- Create: `backend/src/main/java/ec/edu/udla/arquitecturac4/consulta/dto/ConsultaContribuyenteResponse.java`
- Create: `backend/src/main/java/ec/edu/udla/arquitecturac4/consulta/ContribuyenteNoEncontradoException.java`
- Create: `backend/src/main/java/ec/edu/udla/arquitecturac4/consulta/NoEsPersonaNaturalException.java`
- Test: `backend/src/test/java/ec/edu/udla/arquitecturac4/consulta/ConsultaServiceTest.java`
- Modify: `backend/src/main/java/ec/edu/udla/arquitecturac4/consulta/ConsultaService.java`

**Interfaces:**
- `ConsultaContribuyenteResponse`: `ruc`, `nombre`, `tipoContribuyente`, `estado`, `actividadEconomicaPrincipal`, `regimen`, and nullable `fechaInicioActividades`.
- `ConsultaService.consultarContribuyente(ConsultaContribuyenteRequest): ConsultaContribuyenteResponse` orchestrates existence, detail, and natural-person validation.
- Keep existing vehicle and license methods returning `INTEGRACION_PENDIENTE`.

- [x] **Step 1: Write failing service tests** for not-found without detail lookup, natural-person mapping, and non-natural rejection.
- [x] **Step 2: Run `mvn -B -ntp -Dtest=ConsultaServiceTest test`** from `backend`; confirm expected failures before implementation.
- [x] **Step 3: Implement the minimal orchestration and local response mapping.** Compare the normalized SRI type with `PERSONA NATURAL`; do not copy representative fields.
- [x] **Step 4: Re-run `ConsultaServiceTest`** and confirm all cases pass.

### Task 3: HTTP error mapping, controller response, and OpenAPI

**Files:**
- Create: `backend/src/main/java/ec/edu/udla/arquitecturac4/consulta/SriNoDisponibleException.java`
- Create: `backend/src/main/java/ec/edu/udla/arquitecturac4/consulta/SriRespuestaInvalidaException.java`
- Modify: `backend/src/main/java/ec/edu/udla/arquitecturac4/consulta/ConsultaController.java`
- Modify: `backend/src/main/java/ec/edu/udla/arquitecturac4/api/ApiExceptionHandler.java`
- Modify: `backend/src/test/java/ec/edu/udla/arquitecturac4/consulta/ConsultaControllerTest.java`
- Modify: `docs/api/openapi.yaml`

**Interfaces:**
- `POST /api/v1/consultas/contribuyentes` returns the local response DTO with HTTP 200.
- Map missing contributor to `404 CONTRIBUYENTE_NO_ENCONTRADO`, non-natural record to `422 NO_ES_PERSONA_NATURAL`, SRI connectivity/timeouts to `503 SRI_NO_DISPONIBLE`, and invalid upstream responses to `502 SRI_RESPUESTA_INVALIDA`.
- Error responses retain the existing `ApiErrorResponse` shape and generic messages.
- `ConsultaControllerTest` injects a mocked `SRIContribuyenteClient` while retaining the real controller, service and exception handler.

- [x] **Step 1: Write failing MockMvc tests** for success DTO, 404, 422, 502, and 503; mock the SRI client and retain the existing request-validation and vehicle/license tests.
- [x] **Step 2: Run the focused API tests** and confirm failures correspond to the unimplemented status/body behavior.
- [x] **Step 3: Implement controller and exception mappings; update the contributor OpenAPI responses and success schema.**
- [x] **Step 4: Re-run focused API tests** and confirm expected status, response fields, and no data leakage.

### Task 4: Full verification and user evidence handoff

**Files:**
- Modify: `README_BITACORA_PRIVADA.md` (local and ignored)
- Create after user capture: `docs/evidencia/EVIDENCIA_ETAPA3_01_Pruebas_Maven.png`
- Create after user capture: `docs/evidencia/EVIDENCIA_ETAPA3_02_SRI_NoEncontrado_404.png`
- Create after user capture: `docs/evidencia/EVIDENCIA_ETAPA3_03_Persona_Natural_Mock.png`

- [x] Run `mvn -B -ntp test` from `backend`; after review fixes, 40 tests passed with 0 failures/errors.
- [x] Check `git diff --check`, inspect `git status`, confirm the private log is ignored, and parse `docs/api/openapi.yaml` with PyYAML.
- [x] Start the backend and perform a live request using safe test data; retain automated natural-person coverage as simulated and verify a real synthetic not-found request. Capture only sanitized output in the user's evidence.
- [x] Re-run the focused regression suite (38 tests) and exercise the configured HTTP transport against a local redirect server.
- [x] Update the private bitácora with files, decisions, tests, observed outcomes, and evidence still pending.
- [ ] Stop for the user to save/confirm evidence and make the manual commit; do not start Stage 4 before confirmation.

**Manual commit after user reviews and saves evidence:** `feat: integrate SRI contributor lookup`

### Review fix pass before evidence handoff

- [x] Require a JSON boolean from the SRI existence endpoint; reject string and numeric coercions.
- [x] Map response-body timeouts to 503, protocol/malformed upstream responses to 502, and reject null entries in the details array.
- [x] Validate finite timeout values, use configurable endpoint paths, and disable automatic redirects.
- [x] Validate all required natural-person response fields and return the canonical `PERSONA NATURAL` value.
- [x] Remove the stale `501` contributor response from OpenAPI; vehicle and license integrations remain pending.
- [x] Add regression tests for the findings and execute the focused suite (38 tests passed).
- [x] Re-run the full Maven suite and parse OpenAPI after the review fixes (40 tests passed; YAML valid).
- [x] Update the private bitácora with final verification and stop for user evidence and manual commit.
