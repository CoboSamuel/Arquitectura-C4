# Etapa 6: Integración del frontend React — Plan de implementación

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax.

**Goal:** Completar el frontend React con el flujo secuencial de consultas de contribuyente, vehículo y licencia ya expuesto por el backend.

**Architecture:** React mantiene en memoria los formularios y resultados, y llama a la Backend API con `fetch`. Un reducer pequeño controla los pasos y limpia los resultados dependientes si se cambia RUC o placa. Vite reenvía `/api` a Spring Boot durante el desarrollo; el navegador no llama directamente al SRI, a la ANT ni a Redis Cloud.

**Tech Stack:** React 19, Vite 7, Node.js >=20.19, Java 21/Spring Boot existente, APIs estándar `fetch` y `node:test`; sin nuevas dependencias.

**Spec:** `docs/superpowers/specs/2026-11-16-etapa-6-frontend-integration-design.md`

## Restricciones globales

- Mantener intactos los tres diagramas C4 de IcePanel.
- Usar exclusivamente `POST /api/v1/consultas/contribuyentes`, `POST /api/v1/consultas/vehiculos` y `POST /api/v1/consultas/licencias` con los cuerpos ya descritos en OpenAPI.
- Solicitar la cédula de 10 dígitos en el paso de licencia; no derivarla del RUC.
- No guardar correo, RUC, cédula o placa en localStorage ni escribirlos en logs/consola.
- No añadir librerías frontend; `node:test` cubre el cliente API, validaciones y transiciones de estado.
- No cambiar la lógica del backend ni afirmar que las respuestas positivas sintéticas validan un resultado real de SRI/ANT.
- El usuario conserva la responsabilidad de guardar evidencias y ejecutar `git add`/`git commit`; Codex no ejecuta operaciones Git de escritura.

## Puntos de revisión

1. Cambiar RUC tras completar pasos posteriores debe borrar resultados de contribuyente, vehículo y licencia, y volver al primer paso.
2. Cambiar placa debe borrar los resultados de vehículo y licencia; cambiar cédula debe borrar el resultado de licencia.
3. Error HTTP con JSON incompleto, cuerpo no JSON o fallo de red debe producir un mensaje seguro y mantener al usuario en el paso actual.
4. El estado `origen=CACHE` y `desactualizado=true` debe mostrarse de forma inequívoca sin afirmar que el dato es actual.
5. Mientras una solicitud está pendiente, el formulario debe impedir envíos duplicados y no dejar avanzar antes de una respuesta exitosa.

---

### Task 1: Cliente API para las tres consultas

**Archivos:**
- Crear: `frontend/src/api/consultas.js`
- Crear: `frontend/src/api/consultas.test.js`
- Modificar: `frontend/package.json` para añadir el script `test: node --test`.

**Interfaces:**
- `consultarContribuyente({ correo, ruc })` envía JSON a `/api/v1/consultas/contribuyentes`.
- `consultarVehiculo({ placa })` envía JSON a `/api/v1/consultas/vehiculos`.
- `consultarLicencia({ cedula, placa })` envía JSON a `/api/v1/consultas/licencias`.
- Todas devuelven el objeto JSON en 2xx y lanzan `ConsultaApiError` con `status`, `code`, `message` y `fieldErrors` para respuestas de error.

- [x] **Paso 1: Escribir pruebas fallidas del cliente API**
  - Verificar método POST, `Content-Type: application/json`, ruta y cuerpo de las tres funciones.
  - Verificar lectura JSON exitosa y propagación estructurada de `ApiErrorResponse`.
  - Verificar respuesta de error no JSON y fallo de red con mensaje genérico seguro.
- [x] **Paso 2: Ejecutar las pruebas para confirmar RED**

Ejecutar desde `frontend`: `node --test src/api/consultas.test.js`

Esperado: fallos porque el módulo de consultas todavía no existe.
- [x] **Paso 3: Implementar `ConsultaApiError`, `postJson` y las tres funciones**
  - Comprobar `response.ok`, analizar JSON con manejo de cuerpo inválido y no registrar solicitudes ni datos personales.
- [x] **Paso 4: Ejecutar las pruebas para confirmar GREEN**

Ejecutar desde `frontend`: `npm test`

Esperado: todas las pruebas API pasan.

### Task 2: Validación y transiciones del flujo

**Archivos:**
- Crear: `frontend/src/consulta/validacion.js`
- Crear: `frontend/src/consulta/validacion.test.js`
- Crear: `frontend/src/consulta/estadoConsulta.js`
- Crear: `frontend/src/consulta/estadoConsulta.test.js`

**Interfaces:**
- `validarCorreo`, `validarRuc`, `validarPlaca` y `validarCedula` devuelven mensaje de error o `null`.
- `estadoInicialConsulta` define inputs, resultados, paso, estado de carga y error.
- `reducirConsulta(state, action)` maneja cambios de campo, inicio/éxito/error de solicitudes, navegación atrás y reinicio.

- [x] **Paso 1: Escribir pruebas fallidas de formatos y estado**
  - Cubrir correo con formato básico, RUC de 13 dígitos, placa de 5–8 caracteres y cédula de 10 dígitos.
  - El correo debe ser válido y no superar 254 caracteres; los otros límites reflejan los patrones exactos publicados en OpenAPI.
  - Cubrir éxito que habilita el siguiente paso, error que conserva el paso, volver a editar, reiniciar y estados de carga.
  - Cubrir invalidación: cambio de RUC elimina resultados y datos posteriores; cambio de placa elimina vehículo/licencia; cambio de cédula elimina licencia.
- [x] **Paso 2: Ejecutar pruebas para confirmar RED**

Ejecutar desde `frontend`: `npm test`

Esperado: fallos por ausencia de los módulos y transiciones.
- [x] **Paso 3: Implementar validadores y reducer puros**
  - Aceptar solo formatos requeridos por OpenAPI; normalizar la placa a mayúsculas antes del envío.
  - No validar identidad ni duplicar la lógica de SRI/ANT en el navegador.
- [x] **Paso 4: Ejecutar `npm test` para confirmar GREEN**

Esperado: pruebas API, validación y estado pasan.

### Task 3: Interfaz secuencial React y estilos

**Archivos:**
- Modificar: `frontend/src/App.jsx`
- Modificar: `frontend/src/main.jsx`
- Crear: `frontend/src/styles.css`
- Modificar: `frontend/index.html` solo para título/metadatos si hace falta.

**Interfaces:**
- `App` usa `useReducer(reducirConsulta, estadoInicialConsulta)` y las funciones API de Task 1.
- Cada formulario se valida antes de llamar la API; solo una respuesta exitosa habilita el avance.

- [x] **Paso 1: Implementar los tres pasos y las explicaciones breves**
  - Contribuyente: correo/RUC, explicación React → backend Java → SRI y tarjeta con campos allowlisted del contribuyente.
  - Contribuyente: mostrar RUC, nombre, tipo, estado, actividad económica principal, régimen y fecha de inicio; si la fecha llega nula, mostrar “No informado”.
  - Vehículo: placa, explicación de consulta SRI y tarjeta con placa, marca, modelo, año y clase; color y cilindraje cuando existan.
  - Licencia: cédula/placa, explicación ANT y Redis Cloud; tarjeta de puntos con fecha formateada en español, origen y aviso de dato desactualizado.
- [x] **Paso 2: Implementar navegación de regreso, edición y limpieza dependiente**
  - El usuario puede editar un paso completado. La navegación y los datos siguen las reglas probadas por el reducer.
  - Mantener en pantalla los resultados previos no afectados; al cambiar RUC/placa/cédula, limpiar solo lo que define el reducer.
- [x] **Paso 3: Implementar carga, errores y reintento por paso**
  - Deshabilitar envío durante una solicitud, conservar el paso ante errores y mostrar el mensaje seguro recibido o un mensaje genérico de red.
- [x] **Paso 4: Aplicar CSS responsive, etiquetas accesibles y estados visuales**
  - Usar encabezado, indicador de tres pasos, formularios y tarjetas de resultados; no agregar componentes externos.
- [x] **Paso 5: Ejecutar `npm test` y `npm run build`**

Esperado: todas las pruebas pasan y Vite genera `frontend/dist` sin error.

### Task 4: Proxy Vite y verificación integrada local

**Archivos:**
- Crear: `frontend/vite.config.js`
- Modificar: pruebas existentes solo si el contrato del cliente API lo requiere.

**Interfaces:**
- El navegador usa rutas `/api/...` del mismo origen Vite.
- Vite dirige `/api` a `VITE_BACKEND_URL`, con valor predeterminado `http://localhost:8080`.

- [x] **Paso 1: Configurar proxy de desarrollo `/api`**
  - Leer `VITE_BACKEND_URL` mediante `loadEnv`; no guardar credenciales ni habilitar acceso directo a servicios externos.
- [x] **Paso 2: Ejecutar pruebas de frontend**

Ejecutar desde `frontend`: `npm test`

Esperado: suite frontend verde.
- [x] **Paso 3: Ejecutar compilación de producción**

Ejecutar desde `frontend`: `npm run build`

Esperado: `vite build` termina correctamente.
- [ ] **Paso 4: Verificar manualmente el flujo completo en navegador**
  - Iniciar Spring Boot y Vite; completar los pasos con entradas sintéticas autorizadas, comprobar que el proxy llega al backend, que errores no avanzan el flujo, que volver/editar invalida resultados dependientes y que la respuesta de licencia muestra origen/antigüedad.
  - Si el backend usa otro puerto, configurar `VITE_BACKEND_URL` solo en el entorno local de Vite.
  - Capturar las evidencias de la interfaz y sus resultados sin mostrar datos personales ni credenciales.

  Verificado parcialmente: la interfaz carga; el proxy Vite → Spring responde HTTP 200 en `/api/v1/health`; al probar el formulario con datos sintéticos, el servicio SRI respondió que no estaba disponible y React conservó el paso y mostró el error seguro. Queda pendiente observar una respuesta positiva en pantalla para completar vehículo y licencia; no se debe simular como respuesta real del SRI.
- [x] **Paso 5: Ejecutar regresión del backend**

Ejecutar desde `backend`: `mvn -B -ntp test`

Esperado: suite backend con `BUILD SUCCESS`; los endpoints existentes no cambian.

### Entrega de Etapa 6

- Actualizar `README_BITACORA_PRIVADA.md` y comprobar que continúa excluido por `.git/info/exclude`.
- Entregar nombres de capturas sugeridos, revisión de `git status` y comandos exactos para el commit manual del usuario.
- Esperar la confirmación del usuario sobre evidencias guardadas y commit antes de iniciar otra etapa.

### Resultado de implementación y verificación — 2026-11-16

- El frontend React ya implementa el flujo de tres pasos, validación, navegación para editar, limpieza de resultados dependientes, estados de carga/error y aviso cuando los puntos provienen de Redis Cloud o son datos en caché desactualizados.
- `npm test`: 25 pruebas, 0 fallos. `npm run build`: compilación Vite exitosa. Se agregó una prueba SSR que detecta que `App` se renderice correctamente y evita regresión de `React is not defined`.
- `mvn -B -ntp test` desde `backend`: 106 pruebas, 0 fallos, 0 errores, 0 omitidas.
- Verificación local: React carga en Chrome. Se ingresaron solo valores sintéticos (`prueba@example.com` y `9999999999999`); la integración respondió con error de disponibilidad del SRI y la interfaz conservó el paso y los datos, mostrando un mensaje seguro para reintentar. No se validó un flujo positivo real del SRI.
- Se inició Spring Boot en `localhost:8080` y Vite en `localhost:5174`. `GET http://127.0.0.1:5174/api/v1/health` devolvió HTTP 200 y `{"status":"UP","application":"sri-ant-backend"}`. Vite seleccionó 5174 porque 5173 ya estaba ocupado por otro proceso local.
- Se capturaron `docs/evidencia/EVIDENCIA_ETAPA6_01_Interfaz_Inicial_React.png` y `docs/evidencia/EVIDENCIA_ETAPA6_02_Error_SRI_Conserva_Formulario.png` usando únicamente datos sintéticos.
- No se modificaron los diagramas C4. El entorno de esta verificación no permitió confirmar el flujo positivo ANT/Redis desde la pantalla; la integración y fallback están cubiertos por las pruebas y evidencias de Etapa 5. La conectividad Redis Cloud ya fue validada en Etapa 5.
- `.git/info/exclude` mantiene excluida esta bitácora. No se ejecutaron `git add`, `git commit` ni `git push`. Pendiente: revisión final por el usuario, añadir evidencia de pruebas/build si la desea, y commit manual de Etapa 6.
