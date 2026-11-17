# Etapa 4 — SRI vehículos Implementation Plan

> Plan de trabajo sujeto a la confirmación de las claves del resultado positivo. No ejecutar `git add`, `git commit` ni `git push`.

**Goal:** Implementar `POST /api/v1/consultas/vehiculos` con consulta al endpoint BaseVehiculo del SRI y una respuesta local con campos vehiculares permitidos.

**Architecture:** Reutilizar el `RestClient` oficial y sus límites de timeout/redirección. Añadir `SRIVehiculoClient`, DTO externo para el sobre, DTO local de salida y manejo de errores. Mantener ANT, Redis y React para etapas posteriores.

**Tech Stack:** Java 21, Spring Boot 3.5.16, `RestClient`, JUnit 5, Mockito, MockMvc y `MockRestServiceServer`.

**Spec:** `docs/superpowers/specs/2026-11-15-etapa-4-sri-vehiculos-design.md`

## Contrato sintético del caso positivo

- [x] El usuario indicó que el caso exitoso es ficticio para el demo; definir fixture sintética sin placas ni datos reales.
- [x] DTO local limitado a placa, marca, modelo, año, clase, color y cilindraje; excluir campos de titular, identificación, dirección, motor y chasis.
- [x] Documentar en OpenAPI y esta especificación que la prueba positiva es simulada y no confirma el esquema real del SRI.

## Task 1: Cliente SRI y sobre externo

- [x] Escribir pruebas HTTP de respuesta sin resultados, respuesta positiva con fixture sintética, JSON inválido, error upstream y timeout.
- [x] Confirmar RED en `SRIVehiculoClientTest` al faltar cliente y DTOs.
- [x] Añadir propiedad `vehicle-path`, DTO de salida y `SRIVehiculoClient` con query param seguro y lista allowlist.
- [x] Confirmar GREEN sin llamadas externas en las pruebas focalizadas.

## Task 2: Flujo vehicular y contrato API

- [x] Probar orquestación: sin resultados 404; éxito con DTO local; error inválido del upstream 502 y SRI no disponible 503.
- [x] Implementar `ConsultaService.consultarVehiculo` y el DTO de respuesta.
- [x] Actualizar `ConsultaController`, excepciones/API handler y `docs/api/openapi.yaml`; retirar 501 solo del endpoint vehicular.
- [x] Confirmar pruebas MockMvc de éxito, errores, validación y ausencia de datos del titular.

## Task 3: Verificación y entrega

- [x] Ejecutar `mvn -B -ntp test` desde `backend`: 52 pruebas, 0 fallos, 0 errores.
- [x] Parsear OpenAPI, inspeccionar `git diff --check` y `git status`, y confirmar que la bitácora está excluida.
- [x] Verificar localmente la respuesta no encontrada con la placa ficticia `ZZZ9999` (HTTP 404); la respuesta positiva se cubre solo con la fixture sintética automatizada. Evidencias 01, 02 y 03 recibidas y guardadas; pendiente el commit manual.
- [x] Actualizar `README_BITACORA_PRIVADA.md`; detenerse para que el usuario guarde evidencias y haga el commit manual.

**Commit sugerido:** `feat: integrate SRI vehicle lookup`
