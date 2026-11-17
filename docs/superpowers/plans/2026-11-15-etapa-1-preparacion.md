# Etapa 1 — Estructura inicial del sistema — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Crear la estructura React y Spring Boot, comprobar el backend y registrar el avance en una bitácora privada excluida de Git.

**Architecture:** Un solo servicio modular Java Spring Boot en `backend/` y un cliente React mínimo en `frontend/`, de acuerdo con los contenedores C4 ya aprobados. Esta etapa solo valida que ambos esqueletos compilan y que el backend responde en `/api/v1/health`.

**Tech Stack:** Java 21, Maven, Spring Boot 3.5.16, Spring Web, Spring Boot Test/MockMvc, React, Vite, npm.

**Spec:** `docs/superpowers/specs/2026-11-15-etapa-1-backend-spring-boot-design.md` y las instrucciones del proyecto compartidas por el usuario.

## Global Constraints

- Mantener un único proyecto Spring Boot modular, no cuatro aplicaciones Java independientes.
- Usar React, Java Spring Boot, Redis Cloud (Redis se incorpora en la etapa correspondiente), REST y JSON.
- Versionar los futuros endpoints bajo `/api/v1/`.
- No ejecutar `git add`, `git commit` ni `git push`; el usuario realiza el commit manualmente después de guardar evidencias.
- No avanzar a la siguiente etapa hasta que el usuario confirme que guardó las evidencias y realizó el commit.
- Mantener `README_BITACORA_PRIVADA.md` en la raíz, ignorado en `.git/info/exclude`, junto con `evidencias/`.
- No guardar secretos en el código; `.env` queda ignorado y `.env.example` puede versionarse sin secretos.
- Conservar la eliminación preexistente de `README.md` y no modificar los diagramas C4 de IcePanel.

## Review Focus

- `GET /api/v1/health` debe responder HTTP 200 y un JSON estable; cubrirlo con `HealthControllerTest`.
- El arranque debe usar el puerto por defecto 8080 y no depender de APIs externas; comprobar con prueba MockMvc y ejecución local del jar.
- El esqueleto React debe compilar sin crear una funcionalidad de consulta prematura; verificar con `npm run build`.
- Dependencias no descargables o versiones incompatibles deben aparecer como error de build, no reportarse como funcionamiento; ejecutar Maven y npm builds completos.
- La bitácora y la carpeta de evidencias no deben aparecer como archivos rastreables; verificar `git check-ignore` y `git status --short`.

---

### Task 1: Reglas locales, bitácora y protección de archivos privados

**Files:**
- Create: `.gitignore`
- Create: `README_BITACORA_PRIVADA.md` (local y excluido, no se agrega al commit)
- Modify: `.git/info/exclude` (local y excluido del commit)
- Update: `docs/superpowers/specs/2026-11-15-etapa-1-backend-spring-boot-design.md`

**Interfaces:**
- Consumes: instrucciones del proyecto y estado Git inicial revisado.
- Produces: reglas para ignorar `README_BITACORA_PRIVADA.md` y `evidencias/`, exclusiones de builds y secretos, y bitácora de Etapa 1.

- [x] **Step 1: Revisar el estado local y que la bitácora no esté rastreada**

Run: `git status --short; git ls-files --error-unmatch README_BITACORA_PRIVADA.md`
Expected: status muestra solo cambios previos y el segundo comando confirma que la bitácora aún no está rastreada (si ya estuviera rastreada, detenerse y conservar su contenido).

- [x] **Step 2: Crear reglas de `.gitignore`**

Ignorar `/backend/target/`, `/frontend/node_modules/`, `/frontend/dist/`, `.env`, `.env.*`, permitiendo explícitamente `.env.example`.

- [x] **Step 3: Agregar exclusiones locales y crear la bitácora**

Añadir los patrones exactos `README_BITACORA_PRIVADA.md` y `evidencias/` en `.git/info/exclude`. Crear el archivo privado con el registro inicial de Etapa 1, indicando verificaciones y evidencias como pendientes hasta ejecutarlas.

- [x] **Step 4: Verificar las exclusiones**

Run: `git check-ignore -v README_BITACORA_PRIVADA.md evidencias/; git status --short`
Expected: ambos patrones coinciden con `.git/info/exclude`; los archivos privados no aparecen en `git status`.

### Task 2: Backend Spring Boot y contrato técnico de salud

**Files:**
- Create: `backend/pom.xml`
- Create: `backend/src/main/java/ec/edu/udla/arquitecturac4/ArquitecturaC4Application.java`
- Create: `backend/src/main/java/ec/edu/udla/arquitecturac4/health/HealthController.java`
- Create: `backend/src/main/resources/application.yml`
- Test: `backend/src/test/java/ec/edu/udla/arquitecturac4/health/HealthControllerTest.java`

**Interfaces:**
- Consumes: paquetes Maven bajo `ec.edu.udla.arquitecturac4`.
- Produces: `GET /api/v1/health` → HTTP 200, JSON `{"status":"UP","application":"sri-ant-backend"}`.

- [x] **Step 1: Escribir la prueba HTTP que describa el endpoint de salud**

`HealthControllerTest` usa `@WebMvcTest(HealthController.class)` y MockMvc para afirmar GET `/api/v1/health`, estado HTTP 200, `$.status` igual a `UP` y `$.application` igual a `sri-ant-backend`.

- [x] **Step 2: Ejecutar la prueba y comprobar RED**

Run desde `backend/`: `mvn test`
Expected: falla porque `HealthController` todavía no existe (no por errores de dependencias o sintaxis de la prueba).

- [x] **Step 3: Crear el proyecto Maven y la aplicación mínima**

Usar Spring Boot 3.5.16, Java 21, `spring-boot-starter-web` y `spring-boot-starter-test`; crear la clase principal, el controlador REST y configuración del puerto 8080 y nombre de aplicación.

- [x] **Step 4: Ejecutar la prueba y comprobar GREEN**

Run desde `backend/`: `mvn test`
Expected: exit code 0; prueba de salud pasa.

- [x] **Step 5: Empaquetar y comprobar el endpoint en proceso real**

Run desde `backend/`: `mvn package`; luego iniciar el jar generado y consultar `http://localhost:8080/api/v1/health` con `Invoke-RestMethod`.
Expected: build exitoso, respuesta HTTP 200 y JSON con `status = UP` y `application = sri-ant-backend`.

### Task 3: Estructura React mínima

**Files:**
- Create: `frontend/package.json`
- Create: `frontend/package-lock.json` (generated by `npm install`)
- Create: `frontend/index.html`
- Create: `frontend/src/main.jsx`
- Create: `frontend/src/App.jsx`

**Interfaces:**
- Consumes: React y Vite.
- Produces: página inicial que muestra el nombre del sistema y sirve como punto de entrada futuro; no realiza llamadas de consulta.

- [x] **Step 1: Crear el manifiesto y los archivos mínimos de React**

Definir scripts `dev` y `build`, dependencias React y Vite, y un punto de entrada que renderice un mensaje de bienvenida del Sistema Web SRI ANT.

- [x] **Step 2: Instalar dependencias y verificar el build**

Run desde `frontend/`: `npm install` y `npm run build`.
Expected: instalación exitosa y build Vite sin errores.

### Task 4: Revisión de entrega de Etapa 1

**Files:**
- Update: `README_BITACORA_PRIVADA.md`
- Review: todos los archivos de las Tasks 1–3.

**Interfaces:**
- Consumes: verificaciones observadas de backend y frontend.
- Produces: bitácora actualizada, lista precisa de evidencias y comandos de commit manual.

- [x] **Step 1: Actualizar la bitácora con cambios y resultados reales**

Registrar archivos, dependencias, comandos, resultados, dificultades, evidencias pendientes y el mensaje de commit propuesto. No registrar credenciales ni datos personales.

- [x] **Step 2: Revisar el estado y la lista de archivos de commit**

Run: `git status --short; git diff --check; git diff --stat`
Expected: se muestran los archivos públicos de Etapa 1, la eliminación preexistente de `README.md`, y no aparecen la bitácora ni `evidencias/`; sin errores de whitespace.

- [x] **Step 3: Entregar evidencia y comandos para el commit del usuario**

Indicar captura del backend ejecutándose y la respuesta de `/api/v1/health`; proporcionar comandos PowerShell con rutas exactas y mensaje `feat: initialize React and Spring Boot project structure`. Esperar confirmación del usuario antes de iniciar otra etapa.
