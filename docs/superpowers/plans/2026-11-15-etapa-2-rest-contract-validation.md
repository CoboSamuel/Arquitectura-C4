# Etapa 2 — Contrato REST y validaciones — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Publicar un contrato REST versionado y validar solicitudes de contribuyentes, vehículos y licencias con errores JSON previsibles.

**Architecture:** `ConsultaController` recibe DTOs validados y delega a `ConsultaService`; `ApiExceptionHandler` convierte validaciones, JSON malformado, integraciones pendientes y errores inesperados en una respuesta común. Las integraciones externas no se invocan en esta etapa.

**Tech Stack:** Java 21, Spring Boot 3.5.16, Spring Web, Jakarta Bean Validation, MockMvc, OpenAPI 3.1.

**Spec:** `docs/superpowers/specs/2026-11-15-etapa-2-rest-contract-design.md` y requisitos del proyecto compartidos por el usuario.

## Global Constraints

- Mantener el backend como una sola aplicación modular, coherente con los componentes de IcePanel.
- Usar JSON y versionar todas las rutas en `/api/v1/`.
- No llamar al SRI ni a la ANT hasta sus etapas designadas.
- No registrar ni devolver valores personales rechazados.
- No ejecutar `git add`, `git commit` ni `git push`; el usuario hará el commit manual después de revisar evidencia y archivos.
- No avanzar a Etapa 3 hasta confirmación del usuario sobre evidencia y commit.
- Mantener `README_BITACORA_PRIVADA.md` local y excluido de Git; las evidencias sí se versionan según la instrucción más reciente del usuario.
- Conservar los commits de Etapa 1 y la evidencia ya registrada; no reescribir historial.

## Review Focus

- Correo/RUC vacío o con formato incorrecto debe producir `400 VALIDATION_ERROR`; comprobar con `ConsultaControllerTest`.
- Placa fuera del patrón alfanumérico/guion de 5–8 caracteres debe producir `400`; comprobar con `ConsultaControllerTest`.
- Cédula distinta de 10 dígitos debe producir `400`; comprobar con `ConsultaControllerTest`.
- JSON truncado no debe filtrar detalles del parser; comprobar `400 INVALID_JSON` en `ConsultaControllerTest`.
- Propiedades JSON desconocidas se rechazan; métodos o tipos de contenido no admitidos conservan `405`/`415`; `Accept` incompatible devuelve `406` sin cuerpo no aceptado.
- Payload válido debe devolver `501 INTEGRACION_PENDIENTE` sin datos personales ni llamadas externas; comprobar los tres endpoints en `ConsultaControllerTest`.

---

### Task 1: Contrato OpenAPI

**Files:**
- Create: `docs/api/openapi.yaml`
- Create: `docs/superpowers/specs/2026-11-15-etapa-2-rest-contract-design.md`

**Interfaces:**
- Consumes: requisitos del enunciado y arquitectura C4 existente.
- Produces: tres operaciones POST, DTO JSON y errores `400`, `501`, `500`.

- [x] **Step 1: Definir rutas y esquemas de entrada**

Documentar `POST /api/v1/consultas/contribuyentes` (`correo`, `ruc`), `/vehiculos` (`placa`) y `/licencias` (`cedula`, `placa`), incluyendo `required`, límites y patrones de la spec.

- [x] **Step 2: Documentar respuestas de error y estado provisional**

Definir `ApiErrorResponse` con `status`, `code`, `message` y `fieldErrors`; describir `400 VALIDATION_ERROR`, `400 INVALID_JSON`, `501 INTEGRACION_PENDIENTE` y `500 INTERNAL_ERROR`. Indicar que las respuestas de éxito se afinan tras inspeccionar respuestas reales en etapas 3–6.

### Task 2: DTOs, controlador y manejo de errores

**Files:**
- Modify: `backend/pom.xml` (añadir `spring-boot-starter-validation`)
- Create: `backend/src/main/java/ec/edu/udla/arquitecturac4/consulta/ConsultaController.java`
- Create: `backend/src/main/java/ec/edu/udla/arquitecturac4/consulta/ConsultaService.java`
- Create: `backend/src/main/java/ec/edu/udla/arquitecturac4/consulta/IntegracionPendienteException.java`
- Create: `backend/src/main/java/ec/edu/udla/arquitecturac4/consulta/dto/ConsultaContribuyenteRequest.java`
- Create: `backend/src/main/java/ec/edu/udla/arquitecturac4/consulta/dto/ConsultaVehiculoRequest.java`
- Create: `backend/src/main/java/ec/edu/udla/arquitecturac4/consulta/dto/ConsultaLicenciaRequest.java`
- Create: `backend/src/main/java/ec/edu/udla/arquitecturac4/api/ApiErrorResponse.java`
- Create: `backend/src/main/java/ec/edu/udla/arquitecturac4/api/ApiFieldError.java`
- Create: `backend/src/main/java/ec/edu/udla/arquitecturac4/api/ApiExceptionHandler.java`
- Test: `backend/src/test/java/ec/edu/udla/arquitecturac4/consulta/ConsultaControllerTest.java`
- Test: `backend/src/test/java/ec/edu/udla/arquitecturac4/api/ApiExceptionHandlerTest.java`

**Interfaces:**
- Consumes: DTOs JSON según OpenAPI.
- Produces: tres POST versionados, validación Bean Validation, error común y `501` para integraciones futuras.

- [x] **Step 1: Añadir la dependencia de Bean Validation**

Incluir `spring-boot-starter-validation` en `backend/pom.xml`.

- [x] **Step 2: Escribir primero las pruebas de frontera HTTP**

`ConsultaControllerTest` comprueba: correo/RUC inválidos → 400 sin eco de valores; placa inválida → 400; cédula con longitud incorrecta → 400; JSON malformado o campos extra → `INVALID_JSON`; método GET no admitido → 405; `Accept` incompatible → 406 sin cuerpo; `Content-Type` no admitido → 415; y los tres payloads válidos → 501 `INTEGRACION_PENDIENTE`.

- [x] **Step 3: Ejecutar pruebas para observar RED**

Run desde `backend/`: `mvn test`
Expected: falla por falta de `ConsultaController`/DTOs, no por errores de sintaxis ni dependencias.

- [x] **Step 4: Implementar DTOs y rutas**

Usar `@NotBlank`, `@Email`, `@Size`, `@Pattern` y `@Valid @RequestBody`; rutas exactas de la spec. El servicio arroja una excepción controlada para integraciones aún no implementadas.

- [x] **Step 5: Implementar respuesta común de errores**

Mapear errores de Bean Validation y JSON malformado a 400, integración pendiente a 501, errores HTTP esperados a sus estados y excepción inesperada a 500 genérico. Para `406`, no enviar un cuerpo JSON que el cliente rechazó. Ordenar los errores por nombre de campo y nunca incluir valores rechazados ni mensajes internos.

- [x] **Step 6: Ejecutar pruebas para observar GREEN**

Run desde `backend/`: `mvn test`
Expected: todas las pruebas de Etapa 1 y Etapa 2 pasan; ninguna conexión a SRI/ANT.

### Task 3: Validación final y entrega

**Files:**
- Update: `README_BITACORA_PRIVADA.md`
- Review: `docs/api/openapi.yaml` y rutas/errores del backend.

**Interfaces:**
- Consumes: resultado de Maven y archivos del contrato.
- Produces: estado verificado, evidencia sugerida y comandos del commit manual.

- [x] **Step 1: Verificar compilación, pruebas y whitespace**

Run desde `backend/`: `mvn -B -ntp package`; desde raíz: `git diff --check` y `git status --short`.
Resultado: 13 pruebas pasan; el artefacto ejecutable se empaquetó con un clasificador temporal porque OneDrive bloqueó el renombrado del JAR estándar. OpenAPI parsea correctamente y `git diff --check` no informa errores; no se cambian los commits existentes.

- [x] **Step 2: Actualizar bitácora y entregar evidencia**

Registrar archivos, decisiones, pruebas y resultados; indicar captura de una solicitud inválida mostrando HTTP 400/`VALIDATION_ERROR` y una válida mostrando HTTP 501/`INTEGRACION_PENDIENTE`.

- [x] **Step 3: Dar comandos manuales para el commit**

Listar rutas exactas y sugerir `feat: define REST consultation contract and validation`; detenerse hasta confirmación del usuario.
