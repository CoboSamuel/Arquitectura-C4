# Etapa 5 ANT + Redis Cloud Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement the ANT license-points endpoint with Redis Cloud caching and a clearly marked stale fallback, following the existing C4.

**Architecture:** `ANTClient` performs a bounded GET and parses a narrow HTML summary. `LicenciaCacheService` checks Redis first, refreshes after freshness expires, and returns the last validated value if ANT is unavailable. `ConsultaService` exposes an allowlisted response and never exposes the submitted identifiers or raw HTML.

**Tech Stack:** Java 21, Spring Boot 3.5.16, Spring Data Redis/Lettuce managed by Spring Boot, jsoup 1.24.1, RestClient, JUnit 5, Mockito, MockMvc, MockRestServiceServer.

**Spec:** `docs/superpowers/specs/2026-11-15-etapa-5-ant-redis-design.md`

## Global Constraints

- Keep the existing three IcePanel C4 diagrams unchanged; implement `ANTClient`, `LicenciaCacheService`, and the Redis Cloud container already shown there.
- Use the request parameters `ps_tipo_identificacion=CED`, `ps_identificacion`, and `ps_placa` from the assignment; send a direct GET and encode each value.
- Use only synthetic identifiers and synthetic HTML in automated tests; do not store/log raw HTML, cédula, or plate.
- Cache only a validated points summary. Freshness defaults to 15 minutes; maximum stale retention defaults to 7 days; both are configurable.
- Keep `REDIS_URL` and `ANT_CACHE_KEY_SECRET` outside Git. Do not ask the user to paste credentials in chat.
- Require `ANT_CACHE_KEY_SECRET` to contain at least 32 bytes; the synthetic test secret may appear only in test code.
- No `git add`, `git commit`, or `git push`; the user commits the completed stage after evidence review.

## Review Focus

1. `Puntos: 0` is a valid value, and decimal comma/dot normalize to the same `BigDecimal`; parser tests cover zero and both decimal forms.
2. An ANT challenge/error page returned with HTTP 200 is invalid, is never cached, and falls back to an available stale value; parser/service tests cover it.
3. Two or multiple summary points values are ambiguous and cannot replace the prior cache value; parser/service tests cover it.
4. Cache identifiers must not expose cédula or plate; key and HTTP response tests assert neither value appears.
5. Redis read/write failure must not discard a valid live ANT response and emits only a generic warning; simultaneous ANT/Redis failure returns 503.
6. A confirmed ANT absence invalidates a previous positive value; a missing HMAC secret returns 503 before ANT is called.

## Trazabilidad con el enunciado y los diagramas C4

- **Contexto del sistema:** el usuario consulta el Sistema Web SRI ANT. Los datos de correo/RUC y la validación de persona natural pertenecen al flujo de contribuyentes ya implementado en la Etapa 3.
- **Contenedores existentes:** se mantienen React Frontend, Backend API Java, servicios externos SRI/Portal ANT y Redis Cloud. La consulta de vehículos SRI ya se implementó en la Etapa 4.
- **Componentes de esta etapa:** el endpoint de licencias entra por `ConsultaController`/`ConsultaService`, consulta `ANTClient`, y `LicenciaCacheService` usa Redis Cloud para caché fresca y fallback. Es el mismo límite y responsabilidad que muestran los diagramas C4.
- **Etapa posterior:** conectar los flujos de contribuyentes, vehículos y licencia desde la interfaz React y verificar el recorrido completo.
- **Cierre de entrega:** prueba de usabilidad con una persona, evidencias/commit manual de cada etapa y comprobación de que el repositorio GitHub está enlazado al proyecto IcePanel. No redibujar ni alterar los tres diagramas ya terminados.

---

### Task 1: ANT HTTP client and synthetic HTML parser

**Files:**
- Create: `backend/src/main/java/ec/edu/udla/arquitecturac4/consulta/ant/ANTClient.java`
- Create: `backend/src/main/java/ec/edu/udla/arquitecturac4/consulta/ant/AntHtmlParser.java`
- Create: `backend/src/main/java/ec/edu/udla/arquitecturac4/consulta/ant/AntProperties.java`
- Create: `backend/src/main/java/ec/edu/udla/arquitecturac4/consulta/ant/AntClientConfiguration.java`
- Create: `backend/src/main/java/ec/edu/udla/arquitecturac4/consulta/ant/AntNoDisponibleException.java`
- Create: `backend/src/main/java/ec/edu/udla/arquitecturac4/consulta/ant/AntRespuestaInvalidaException.java`
- Modify: `backend/pom.xml`
- Modify: `backend/src/main/resources/application.yml`
- Test: `backend/src/test/java/ec/edu/udla/arquitecturac4/consulta/ant/AntHtmlParserTest.java`
- Test: `backend/src/test/java/ec/edu/udla/arquitecturac4/consulta/ant/ANTClientTest.java`
- Test: `backend/src/test/java/ec/edu/udla/arquitecturac4/consulta/ant/AntPropertiesTest.java`

**Interfaces:**
- Produces: `AntHtmlParser.parse(String html): Optional<BigDecimal>`; empty means only the explicit synthetic no-record marker, and malformed/ambiguous HTML throws `AntRespuestaInvalidaException`.
- Produces: `ANTClient.consultarPuntos(String cedula, String placa): Optional<BigDecimal>`; transport/timeouts/upstream temporary statuses throw `AntNoDisponibleException`.
- Produces: `AntProperties` with base URL, path, connect timeout, and read timeout under `integrations.ant`.

- [x] **Step 1: Write parser and HTTP client tests first**
  - Parser: synthetic summary `Puntos: 25`, zero, `Puntos: 19,5`, `Puntos: 19.5`, exact no-record marker `No se encontraron registros` (case-insensitive), absent marker, challenge page, and duplicate `Puntos:` summaries.
  - Client: GET query contains `ps_tipo_identificacion=CED`, encoded `ps_identificacion`, and encoded `ps_placa`; 408/429/5xx and timeout map to unavailable; malformed HTTP/HTML maps to invalid response; redirects are not followed.
- [x] **Step 2: Run the focused tests and confirm RED**

Run from `backend`: `mvn -B -ntp '-Dtest=AntHtmlParserTest,ANTClientTest,AntPropertiesTest' test`

Expected: compilation or missing-behavior failures because the parser/client/properties are not implemented.

- [x] **Step 3: Implement the parser, client, configuration, and dependencies**
  - Add `org.jsoup:jsoup:1.24.1`; add the Redis starter in Task 2.
  - Add `AntProperties` and `AntClientConfiguration`; register `AntProperties` with `@EnableConfigurationProperties`, configure base URL `https://consultaweb.ant.gob.ec`, path `/PortalWEB/paginas/clientes/clp_grid_citaciones.jsp`, a finite 3-second connect timeout, 5-second response timeout, and no redirects.
  - `AntHtmlParser` requires exactly one summary label and a non-negative decimal; accept zero and normalize comma or dot decimal separators. Parse the exact synthetic no-record marker `No se encontraron registros` as `Optional.empty()`; do not infer no record from a missing points field.
  - `ANTClient` uses `RestClient` with URI-builder query parameters. Do not log request identifiers, response body, or query URL.
- [x] **Step 4: Run focused tests and confirm GREEN**

Run from `backend`: `mvn -B -ntp '-Dtest=AntHtmlParserTest,ANTClientTest,AntPropertiesTest' test`

Expected: all listed tests pass without external network calls.

### Task 2: Redis Cloud adapter and private cache keys

**Files:**
- Create: `backend/src/main/java/ec/edu/udla/arquitecturac4/consulta/cache/LicenciaCache.java`
- Create: `backend/src/main/java/ec/edu/udla/arquitecturac4/consulta/cache/CachedLicensePoints.java`
- Create: `backend/src/main/java/ec/edu/udla/arquitecturac4/consulta/cache/RedisLicenciaCache.java`
- Create: `backend/src/main/java/ec/edu/udla/arquitecturac4/consulta/cache/CacheKeyFactory.java`
- Create: `backend/src/main/java/ec/edu/udla/arquitecturac4/consulta/cache/LicenciaCacheProperties.java`
- Modify: `backend/pom.xml`
- Modify: `backend/src/main/resources/application.yml`
- Create: `.env.example` with placeholders only.
- Test: `backend/src/test/java/ec/edu/udla/arquitecturac4/consulta/cache/RedisLicenciaCacheTest.java`
- Test: `backend/src/test/java/ec/edu/udla/arquitecturac4/consulta/cache/CacheKeyFactoryTest.java`
- Test: `backend/src/test/java/ec/edu/udla/arquitecturac4/consulta/cache/LicenciaCachePropertiesTest.java`
- Test: `backend/src/test/java/ec/edu/udla/arquitecturac4/consulta/cache/RedisCloudConnectivityIT.java` (explicit opt-in only; not part of default test suite)

**Interfaces:**
- Produces: `LicenciaCache.get(String key): Optional<CachedLicensePoints>`, `put(String key, CachedLicensePoints value, Duration maxStaleTtl): void`, and `evict(String key): void`.
- Produces: `CachedLicensePoints(BigDecimal puntosDisponibles, Instant actualizadoEn)`.
- Produces: `CacheKeyFactory.create(String cedula, String placa): String`, using HMAC-SHA-256 and `ANT_CACHE_KEY_SECRET`.

- [x] **Step 1: Write cache adapter, key, and properties tests first**
  - Verify JSON serialization round-trip and Redis retention TTL.
  - Verify deterministic HMAC for normalized equivalent inputs, a different key for different identifiers, no raw cédula/plate in the key, and reject a missing or shorter-than-32-byte secret.
  - Verify positive freshness and retention durations and reject retention shorter than freshness.
- [x] **Step 2: Run focused tests and confirm RED**

Run from `backend`: `mvn -B -ntp '-Dtest=RedisLicenciaCacheTest,CacheKeyFactoryTest,LicenciaCachePropertiesTest' test`

Expected: compilation or missing-behavior failures before the adapter is implemented.

- [x] **Step 3: Implement the cache port and Redis adapter**
  - Add Spring Boot's `spring-boot-starter-data-redis`; use the auto-configured `StringRedisTemplate` and `spring.data.redis.url` bound from `REDIS_URL`.
  - Serialize only `CachedLicensePoints` as JSON; set the Redis key TTL to the maximum stale retention.
  - Use `HmacSHA256` over versioned, normalized query material; never use raw identifiers in keys or values.
  - Configure `spring.data.redis.url` from `REDIS_URL` (local fallback `redis://localhost:6379`), 15-minute freshness, 7-day maximum stale retention, and `ANT_CACHE_KEY_SECRET` through environment variables; provide placeholders in `.env.example`, never real credentials.
- [x] **Step 4: Run focused tests and confirm GREEN**

Run from `backend`: `mvn -B -ntp '-Dtest=RedisLicenciaCacheTest,CacheKeyFactoryTest,LicenciaCachePropertiesTest' test`

Expected: all listed tests pass without a running Redis server or cloud credentials.

### Task 3: Fresh-cache, refresh, and stale-fallback orchestration

**Files:**
- Create: `backend/src/main/java/ec/edu/udla/arquitecturac4/consulta/cache/LicenciaCacheService.java`
- Create: `backend/src/main/java/ec/edu/udla/arquitecturac4/consulta/cache/ConsultaLicenciaResult.java`
- Create: `backend/src/main/java/ec/edu/udla/arquitecturac4/consulta/LicenciaNoEncontradaException.java`
- Test: `backend/src/test/java/ec/edu/udla/arquitecturac4/consulta/cache/LicenciaCacheServiceTest.java`

**Interfaces:**
- Consumes: `ANTClient.consultarPuntos(String cedula, String placa): Optional<BigDecimal>` and `LicenciaCache` from Tasks 1–2.
- Produces: `LicenciaCacheService.consultar(String cedula, String placa): ConsultaLicenciaResult` with `puntosDisponibles`, `actualizadoEn`, `origen`, and `desactualizado`.

- [x] **Step 1: Write service tests with fake Clock, mocked ANT, and mocked cache**
  - Fresh hit returns cached result without ANT call.
  - Cache miss + valid ANT response returns `origen=ANT` and writes the result.
  - Stale hit + ANT timeout or invalid HTML returns the stale value with `origen=CACHE` and `desactualizado=true` without overwriting it.
  - Explicit no-record result returns not found and is not cached; zero points is cached as valid.
  - A confirmed no-record result evicts a previous positive summary, so a later ANT outage cannot return it.
  - No usable cache + ANT failure returns unavailable; Redis read failure + ANT success still returns live result; simultaneous Redis/ANT failure returns unavailable.
  - Missing/short HMAC secret fails before ANT or Redis access.
- [x] **Step 2: Run the service tests and confirm RED**

Run from `backend`: `mvn -B -ntp '-Dtest=LicenciaCacheServiceTest' test`

Expected: missing class/behavior failures before orchestration is implemented.

- [x] **Step 3: Implement `LicenciaCacheService`**
  - Read cache first and compare `actualizadoEn` against freshness TTL using an injected `Clock`.
  - Return fresh cache immediately. Otherwise invoke ANT; cache only a parsed, validated positive points value with maximum stale retention.
  - Invalidate a prior value when ANT explicitly confirms no license. Missing/short HMAC configuration is an explicit configuration error, not a cache bypass.
  - On ANT unavailable/invalid response, return a retained stale value if one was read; otherwise preserve the typed ANT error.
  - Treat Redis access errors as cache misses and log only generic warnings. A successful live ANT result remains usable if Redis read/write fails.
- [x] **Step 4: Run service tests and confirm GREEN**

Run from `backend`: `mvn -B -ntp '-Dtest=LicenciaCacheServiceTest' test`

Expected: all service tests pass and demonstrate fresh, refresh, stale, and failure behavior.

### Task 4: License endpoint, API errors, and OpenAPI

**Files:**
- Create: `backend/src/main/java/ec/edu/udla/arquitecturac4/consulta/dto/ConsultaLicenciaResponse.java`
- Modify: `backend/src/main/java/ec/edu/udla/arquitecturac4/consulta/ConsultaService.java`
- Modify: `backend/src/main/java/ec/edu/udla/arquitecturac4/consulta/ConsultaController.java`
- Modify: `backend/src/main/java/ec/edu/udla/arquitecturac4/api/ApiExceptionHandler.java`
- Modify: `backend/src/test/java/ec/edu/udla/arquitecturac4/consulta/ConsultaControllerTest.java`
- Modify: `backend/src/test/java/ec/edu/udla/arquitecturac4/consulta/ConsultaServiceTest.java`
- Modify: `backend/src/test/java/ec/edu/udla/arquitecturac4/api/ApiExceptionHandlerTest.java` if required for the new errors.
- Modify: `docs/api/openapi.yaml`

**Interfaces:**
- Consumes: `LicenciaCacheService.consultar(cedula, placa): ConsultaLicenciaResult` from Task 3.
- Produces: `POST /api/v1/consultas/licencias` returns `ConsultaLicenciaResponse` with `puntosDisponibles`, `actualizadoEn`, `origen`, and `desactualizado`.

- [x] **Step 1: Write MockMvc and service tests first**
  - Valid synthetic live result returns 200 with `origen=ANT`.
  - Stale fallback returns 200 with `origen=CACHE` and `desactualizado=true`.
  - Explicit no license maps to 404 `LICENCIA_NO_ENCONTRADA`; invalid ANT HTML without stale cache maps to 502; ANT unavailable or missing secure cache configuration maps to 503.
  - Assert neither cédula, plate nor upstream HTML is present in the response.
- [x] **Step 2: Run the focused endpoint tests and confirm RED**

Run from `backend`: `mvn -B -ntp '-Dtest=ConsultaControllerTest,ConsultaServiceTest,ApiExceptionHandlerTest' test`

Expected: existing license test fails because the endpoint still returns 501 and the response/errors are missing.

- [x] **Step 3: Wire the service, controller, errors, and OpenAPI**
  - Change `ConsultaController.consultarLicencia` from `void` to the response DTO and delegate to the cache service through `ConsultaService`.
  - Map the typed ANT/cache exceptions to generic 404/502/503 responses without external content; missing HMAC configuration uses `CACHE_NO_CONFIGURADA`.
  - Replace the license `501` in OpenAPI with 200/404/502/503 schemas and describe `origen`/`desactualizado`.
- [x] **Step 4: Run endpoint tests and confirm GREEN**

Run from `backend`: `mvn -B -ntp '-Dtest=ConsultaControllerTest,ConsultaServiceTest,ApiExceptionHandlerTest' test`

Expected: all listed tests pass and earlier contributor/vehicle endpoint behavior remains unchanged.

### Task 5: Final verification and user handoff

**Files:**
- Modify: `docs/api/openapi.yaml` only if final validation finds a contract mismatch.
- Modify: `README_BITACORA_PRIVADA.md` locally; it must remain excluded by `.git/info/exclude`.
- The user saves three sanitized evidence screenshots in `docs/evidencia/`:
  - `EVIDENCIA_ETAPA5_01_Pruebas_Maven.png`: full backend suite showing 106 tests and `BUILD SUCCESS`.
  - `EVIDENCIA_ETAPA5_02_Respuesta_Licencia_Sintetica.png`: focused MockMvc synthetic response test showing `BUILD SUCCESS`.
  - `EVIDENCIA_ETAPA5_03_Fallback_Cache.png`: focused stale-cache fallback test showing `BUILD SUCCESS`.
  - `EVIDENCIA_ETAPA5_04_Ausencia_Invalida_Cache.png`: confirmed absence evicts old positive value before a later ANT outage.
  - `EVIDENCIA_ETAPA5_05_Configuracion_Cache_503.png`: missing HMAC configuration returns a generic 503 without calling ANT.
  - Optional after cloud setup: `EVIDENCIA_ETAPA5_06_RedisCloud.png` showing the Redis Cloud smoke test passed. Never include environment values in a screenshot.

**Interfaces:**
- Consumes: completed ANT client, Redis cache, orchestration, endpoint, and OpenAPI from Tasks 1–4.
- Produces: verified Etapa 5 working tree ready for the user's single manual stage commit.

- [x] **Step 1: Run the complete backend suite after review fixes**

Run from `backend`: `mvn -B -ntp test` (in this managed test environment, use the installed JDK 21 and Mockito core as a test JVM agent; the user's VS Code PowerShell run used the standard command).

Result: `BUILD SUCCESS`, 106 tests, 0 failures, 0 errors, 0 skipped. One earlier run inside the restricted sandbox could not bind the existing `127.0.0.1` redirect-test server; rerunning the same suite with loopback permission passed.

- [x] **Step 2: Validate OpenAPI, whitespace, configuration, and Git state**

Run from repository root: parse `docs/api/openapi.yaml`, run `git diff --check`, inspect `git status`, confirm no credential values are present, and confirm `README_BITACORA_PRIVADA.md` is ignored.

Result: OpenAPI YAML parses; the license route declares 200/400/404/502/503/405/406/415/500 and no 501; `git diff --check` passes with only normal LF/CRLF notices; Redis variables are absent without reading values; `README_BITACORA_PRIVADA.md` remains ignored; the working tree has no staged changes.

- [x] **Step 3: Update private bitácora and prepare evidence instructions**

Record test results and the unverified production HTML contract in `README_BITACORA_PRIVADA.md`. When the user has configured their Redis Cloud `REDIS_URL` and `ANT_CACHE_KEY_SECRET` privately in the shell, run the opt-in cloud adapter smoke test:

```powershell
mvn -B -ntp '-Dtest=RedisCloudConnectivityIT' test
```

Verified in this session: variables absent, so the explicit smoke test compiled and skipped (1 skipped) without connecting. Before claiming cloud connectivity, configure both variables locally and rerun it. Evidence screenshots are still for the user to capture into `docs/evidencia/` before their manual commit.

Expected: one synthetic cache value under a unique HMAC key is written to and read from Redis Cloud with a one-minute TTL. The entry expires automatically. Never print, capture, or commit environment values. If Redis Cloud is not configured, report that cloud connectivity is not yet verified and request local setup instructions before claiming full cloud operation.

Capture in VS Code's PowerShell terminal from `backend` using these commands, then save a screenshot of each final `BUILD SUCCESS` result under the exact names above:

```powershell
mvn -B -ntp test
mvn -B -ntp '-Dtest=ConsultaControllerTest#validLicenseRequestReturnsAllowlistedAntSummary' test
mvn -B -ntp '-Dtest=LicenciaCacheServiceTest#staleCacheIsReturnedWhenAntIsUnavailableWithoutReplacingIt' test
mvn -B -ntp '-Dtest=LicenciaCacheServiceTest#explicitNoRecordsPreventsOldPositiveValueFromReturningAfterLaterAntOutage' test
mvn -B -ntp '-Dtest=ConsultaControllerTest#missingLicenseCacheConfigurationMapsToGeneric503' test
```

Stop for the user to add/commit the code and evidence manually; do not run Git staging or commit commands.

**Suggested user commit:** `feat: integrate ANT license points with Redis cache`
