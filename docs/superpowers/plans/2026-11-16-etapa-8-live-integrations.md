# Etapa 8 — integraciones live — Plan de implementación

> Alcance corregido por instrucción explícita del usuario el 2026-11-16: SRI real, ANT simulada por defecto y caché Redis para demostrar la recuperación ante fallas. El plan detallado vigente es `2026-11-16-sri-real-ant-simulated.md`.

**Objetivo:** Activar las consultas SRI reales, mantener ANT con su contrato simulado mientras el upstream responde 502 y usar Redis Cloud como respaldo del último dato válido.

**Arquitectura:** Los clientes HTTP de contribuyente y vehículo SRI quedan activos con perfil predeterminado `live`. `SimulatedAntClient` queda activo por defecto; `ANTClient` se habilita con `live,ant-real`. Las pruebas usan respuestas controladas y la interfaz identifica explícitamente la simulación.

**Tech Stack:** Spring Boot/Java 21, React/Vite, Redis Cloud.

**Spec:** `docs/superpowers/specs/2026-11-16-etapa-8-live-integrations-design.md`.

## Global Constraints

- No `git add`, `git commit` ni `git push` por Codex.
- No guardar ni mostrar datos personales o secretos en archivos/capturas/chat.
- No cambiar los objetos, relaciones o disposición de los diagramas C4 existentes.
- Redis se conecta solo mediante configuración local; no borrar ni provisionar bases.

## Review Focus

- Arranque sin `SPRING_PROFILES_ACTIVE`: los dos clientes SRI reales y el proveedor ANT simulado están disponibles.
- Perfil `live,ant-real`: el cliente ANT HTTP reemplaza al simulador sin registrar dos proveedores.
- Una caída o respuesta inválida de ANT no reemplaza el último dato bueno en Redis.
- Las consultas del SRI mantienen la validación de contribuyente natural y no filtran el RUC en errores/logs.
- La UI no muestra datos sintéticos ni códigos técnicos.
- La URI Redis y `ANT_CACHE_KEY_SECRET` no se agregan al repo ni a capturas.

---

### Task 1: Hacer `live` el arranque normal

**Files:** `backend/src/main/resources/application.yml`, `.env.example`, `backend/src/main/java/ec/edu/udla/arquitecturac4/consulta/cache/CacheKeyFactory.java`, tests de contexto.

- [x] Crear `LiveApplicationContextTest` sin `@ActiveProfiles`; afirmar perfil predeterminado `live` y beans de `SRIContribuyenteClient`, `SRIVehiculoClient` y `SimulatedAntClient`; cubrir `ANTClient` con `live,ant-real`.
- [x] Correr el test primero y confirmar que falla porque el perfil predeterminado anterior era `demo`.
- [x] Cambiar el perfil predeterminado y la plantilla local a `live`; conservar el formato de clave HMAC que ya usa Redis.
- [x] Ejecutar el test focalizado y luego el conjunto backend.

### Task 2: Quitar la experiencia de demostración

**Files:** `frontend/src/App.jsx`, `frontend/src/App.test.js`, `frontend/src/styles.css`, proveedores/tests bajo `consulta/demo/`, `docs/CONFIGURACION_LOCAL_DEMO.md`.

- [x] Cambiar la prueba de render para exigir texto normal de consulta y que no aparezcan valores ficticios ni aviso demo; comprobar RED.
- [x] Eliminar el callout y sus estilos, conservar el flujo existente y el aviso de proyecto académico/no oficial.
- [x] Eliminar proveedores sintéticos, propiedad `demo`, tests y documentación de modo demo.
- [x] Repetir prueba de UI, `npm test` y `npm run build`.

### Task 3: Mantener las integraciones SRI, simular ANT y conservar el caché

**Files:** clientes HTTP, `ConsultaService`, `LicenciaCacheService`, pruebas de cliente/cache y guía local.

- [x] Confirmar que los tests HTTP cubren las rutas y parámetros del SRI con requests simulados; el proveedor ANT simulado cubre el contrato mientras el sitio está caído.
- [x] Mantener fallback de caché solo para fallas/respuestas inválidas del proveedor ANT; una respuesta válida sin registros se informa como “no encontrado” y no se presenta como un dato vigente.
- [x] Documentar cómo configurar URI Redis y secreto HMAC en PowerShell sin mostrarlos; añadir el comando de humo de Redis Cloud.
- [x] Ejecutar `mvn -B -ntp test` y pruebas de caché (111 pruebas, sin fallos).

### Task 4: Verificación y entrega

- [x] Ejecutar `npm test` (32 aprobadas), `npm run build`, `git diff --check` y verificar que la bitácora sigue excluida.
- [ ] Indicar las capturas para respuesta SRI de persona natural, vehículo, ANT/cache y pruebas/build, con identificadores tapados.
- [ ] Entregar los comandos de staging/commit al usuario; no avanzar otra etapa hasta su confirmación.

La conectividad real con Redis Cloud y las respuestas actuales del SRI quedan para la comprobación manual del usuario, ya que requieren su URI/secreto y datos de consulta. La evidencia de ANT debe conservar el `502` observado y la pantalla de `Servicio ANT simulado`; el cliente HTTP ANT se podrá activar con `live,ant-real` cuando el upstream se recupere. Las pruebas automáticas verifican los contratos mediante HTTP controlado y no afirman disponibilidad de los upstreams.
