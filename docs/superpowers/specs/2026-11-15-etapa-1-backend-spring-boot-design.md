# Etapa 1 — Estructura inicial del sistema — Diseño

## Intención

Construir la estructura inicial del sistema web SRI/ANT respetando los contenedores **Frontend Web React** y **Backend API Java Spring Boot** definidos en los diagramas C4. El resultado permitirá ejecutar y verificar el backend, compilar el esqueleto frontend y registrar el avance en una bitácora local excluida de Git.

## Alcance

### Objetivos

- Crear carpetas independientes `backend/` y `frontend/`.
- Crear un proyecto Maven con Java 21 y Spring Boot dentro de `backend/`.
- Crear una base mínima React + Vite dentro de `frontend/`.
- Establecer la estructura convencional de código y pruebas del backend bajo `src/main` y `src/test`.
- Configurar el nombre de la aplicación y el puerto mediante `application.yml`.
- Exponer un endpoint técnico `GET /api/v1/health` para verificar que el backend está disponible.
- Probar el endpoint con Spring Boot Test/MockMvc.
- Crear `README_BITACORA_PRIVADA.md` en la raíz y excluirlo junto con `evidencias/` mediante `.git/info/exclude`.
- Añadir `.gitignore` para artefactos de build y archivos de entorno con secretos, permitiendo `.env.example`.
- Documentar comandos de compilación, pruebas y ejecución para capturar evidencia.

### Fuera de alcance

- Integración real con las APIs del SRI.
- Consulta real de vehículo o de puntos de licencia en la ANT.
- Cache cloud/Redis y política de último dato válido.
- Flujos e interfaz funcional del frontend; en esta etapa solo se crea un esqueleto compilable.
- Contrato funcional definitivo de consultas, validaciones de RUC/placa, paginación o idempotencia.
- Gestión de secretos, autenticación o despliegue cloud.

## Restricciones conocidas

- El repositorio remoto correcto es `https://github.com/CoboSamuel/Arquitectura-C4.git`.
- No se ejecutarán `git add`, `git commit` ni `git push` desde Codex.
- La bitácora privada `README_BITACORA_PRIVADA.md` debe mantenerse fuera de Git mediante `.git/info/exclude`.
- Los diagramas C4 de IcePanel son la referencia arquitectónica y no se modificarán desde el código.
- El estado inicial observado está en la rama `feature/v0.1-arquitectura`, alineada con `main` en el mismo commit, y tiene `README.md` eliminado en el working tree; esa eliminación se conserva.

## Enfoques considerados

1. **Maven + Spring Boot 3.5.x + Java 21 — seleccionado.** Usa la estructura habitual para el curso, ofrece compatibilidad con Java 21 y mantiene una base estable para añadir posteriormente clientes HTTP, resiliencia y Redis. Se fija una versión concreta de Spring Boot en `pom.xml` para que las compilaciones sean reproducibles.
2. **Maven + Spring Boot 4.x + Java 21.** Es una alternativa técnicamente viable, pero introduce el salto mayor de plataforma antes de que el proyecto necesite sus capacidades y puede aumentar fricción con ejemplos o dependencias de las siguientes etapas.
3. **Gradle + Spring Boot.** También es viable, pero añade una decisión de herramienta que no aporta valor a esta etapa y se aparta del flujo Maven recomendado para una base académica simple.

## Decisiones de diseño

- **Backend build:** Maven, con `spring-boot-starter-parent`, `spring-boot-starter-web` y `spring-boot-starter-test`.
- **Frontend build:** Vite con React y scripts `npm run dev` y `npm run build`; sin integración con el backend todavía.
- **Java:** versión 21, disponible en el equipo y compatible con la línea seleccionada de Spring Boot.
- **Paquete base:** `ec.edu.udla.arquitecturac4`, coherente con el contexto académico del proyecto y estable para las etapas posteriores.
- **Endpoint técnico:** `GET /api/v1/health`, con respuesta JSON pequeña y estable que indique el estado `UP` y el nombre de la aplicación.
- **Prueba:** `@WebMvcTest` para verificar el contrato HTTP del endpoint sin depender de servicios externos.
- **Configuración:** `backend/src/main/resources/application.yml`, sin URLs, tokens ni contraseñas.
- **Archivos locales:** `.git/info/exclude` ignorará `README_BITACORA_PRIVADA.md` y `evidencias/`; `.gitignore` excluirá `.env` y artefactos compilados.
- **Evidencia manual:** después de las pruebas, ejecutar el jar y consultar el endpoint desde PowerShell con `Invoke-RestMethod`.

## Estructura propuesta

```text
backend/
  pom.xml
  src/main/java/ec/edu/udla/arquitecturac4/
    ArquitecturaC4Application.java
    health/HealthController.java
  src/main/resources/application.yml
  src/test/java/ec/edu/udla/arquitecturac4/health/HealthControllerTest.java
frontend/
  package.json
  index.html
  src/main.jsx
  src/App.jsx
README_BITACORA_PRIVADA.md  # excluido localmente de Git
```

## Criterio de aceptación de la Etapa 1

- `cd backend; mvn test` termina con código 0 y la prueba del endpoint pasa.
- `cd backend; mvn package` termina con código 0 y produce un jar ejecutable.
- `cd frontend; npm run build` termina con código 0.
- La aplicación inicia con `java -jar` y `GET http://localhost:8080/api/v1/health` devuelve HTTP 200 con estado `UP`.
- `git check-ignore` confirma que la bitácora privada y `evidencias/` quedan fuera de Git.
- No se agregan llamadas a SRI/ANT, caché cloud, flujos frontend ni secretos.
- El usuario recibe la lista de evidencias a capturar y los comandos exactos para su commit manual.

## Criterio de pausa

Al terminar esta etapa se detiene el trabajo. No se implementará la Etapa 2 hasta que el usuario confirme que guardó las evidencias y realizó el commit manual.
