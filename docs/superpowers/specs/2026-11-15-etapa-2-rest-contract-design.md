# Etapa 2 — Contrato REST y validaciones — Diseño

## Intención

Definir y comprobar la frontera REST del backend SRI/ANT antes de conectar servicios externos. Esta etapa valida formato de solicitudes, documenta respuestas de error y deja explícito que una solicitud válida aún no consulta el SRI o la ANT.

## Objetivos

- Documentar OpenAPI 3.1 para los endpoints versionados bajo `/api/v1/`.
- Crear los tres endpoints de consulta como frontera HTTP con DTOs dedicados.
- Validar correo, RUC, placa y cédula sin consultar servicios externos.
- Devolver errores JSON estables, sin repetir los datos privados enviados por el usuario.
- Responder `501 INTEGRACION_PENDIENTE` para solicitudes válidas hasta que cada integración se implemente en sus etapas.
- Manejar JSON malformado con `400` y fallos inesperados con `500` genérico.

## Fuera de alcance

- Llamadas reales a SRI o ANT, interpretación de sus payloads y validación de persona natural contra SRI.
- Redis, caché, resiliencia y respuestas `503` por fallos externos.
- Sesiones persistentes de consulta, autenticación o persistencia en base de datos.

## Enfoques considerados

1. **POST por tipo de consulta — seleccionado.** Los identificadores viajan en el cuerpo JSON, los recursos quedan bajo `/api/v1/consultas/` y cada endpoint recibe un DTO específico. Sigue el flujo del deber y evita exponer RUC, cédula o placa en URLs e historiales de navegación.
2. **GET con identificadores como query parameters.** Es sencillo para consultas, pero pone datos personales en URLs, historial y registros de servidor.
3. **Un único POST con todos los campos.** Evita varias rutas, pero mezcla pasos que el enunciado presenta en secuencia y obliga a enviar la placa antes de que el usuario la ingrese.

## Contrato propuesto

- `POST /api/v1/consultas/contribuyentes`: `{ "correo": "persona@example.com", "ruc": "0999999999001" }`.
- `POST /api/v1/consultas/vehiculos`: `{ "placa": "ABC1234" }`.
- `POST /api/v1/consultas/licencias`: `{ "cedula": "1710034065", "placa": "ABC1234" }`.
- `400 VALIDATION_ERROR`: solicitud JSON válida, pero uno o más campos no cumplen formato.
- `400 INVALID_JSON`: cuerpo JSON malformado o incompatible con el DTO.
- `501 INTEGRACION_PENDIENTE`: campos válidos, pero la integración correspondiente aún no existe.
- `500 INTERNAL_ERROR`: mensaje genérico; nunca exponer stack trace, excepción ni valores enviados.
- Las respuestas de éxito se concretarán al inspeccionar las respuestas reales del SRI y la ANT en las etapas 3–6.

## Reglas de validación de entrada

- `correo`: requerido, dirección con formato de correo y máximo 254 caracteres.
- `ruc`: requerido, exactamente 13 dígitos. Esta es validación sintáctica; el SRI confirma existencia y que corresponda a una persona natural.
- `cedula`: requerido, exactamente 10 dígitos. Esta es validación sintáctica; no sustituye la verificación de identidad del SRI.
- `placa`: requerida, 5–8 caracteres alfanuméricos o guion; el patrón local no pretende validar matrícula ni existencia vehicular.

## Decisiones de respuesta

- Errores JSON con `status`, `code`, `message` y `fieldErrors`.
- Cada elemento de `fieldErrors` incluye `field` y `message`; no se incluirán valores rechazados.
- Los mensajes de validación serán breves y en español.
- Rechazar propiedades JSON que no pertenezcan al DTO; preservar códigos HTTP `405` y `415` para método o tipo de contenido no admitidos.
- Las APIs aceptan JSON. No se registrarán cuerpos de solicitud ni identificadores personales.

## Criterio de aceptación

- OpenAPI describe los tres métodos, campos, validaciones y errores observables.
- Solicitudes inválidas dan `400` y señalan los campos sin eco de RUC, cédula, correo o placa.
- JSON malformado da `400 INVALID_JSON`.
- Las tres solicitudes válidas llegan al servicio y reciben `501 INTEGRACION_PENDIENTE` hasta sus respectivas etapas de integración.
- `mvn test` pasa y no se realizan llamadas de red a SRI/ANT.
