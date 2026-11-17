# Etapa 3 — Consulta de contribuyentes del SRI

## Intención

Implementar la integración de contribuyentes del backend para verificar un RUC en el SRI, confirmar que corresponde a una persona natural y devolver un resumen útil al cliente REST. Esta etapa mantiene la aplicación Spring Boot modular y cubre únicamente el flujo de contribuyentes.

## Alcance

- `POST /api/v1/consultas/contribuyentes` consulta los endpoints oficiales de existencia y detalle del SRI.
- El backend obtiene el detalle solo cuando el endpoint de existencia responde `true`.
- Solo se acepta un resultado cuyo `tipoContribuyente` corresponda a persona natural.
- Se devuelve un DTO local con datos mínimos para identificar y describir al contribuyente: RUC, nombre, tipo, estado, actividad económica principal, régimen y fecha de inicio de actividades cuando esté disponible.
- Los fallos externos se traducen a errores estables, sin incluir el RUC, el correo ni el cuerpo crudo del SRI en mensajes o registros.
- El contrato OpenAPI documenta las respuestas de éxito y error.

## Fuera de alcance

- Consultas de vehículos, integración ANT, Redis Cloud y cambios en React.
- Almacenamiento de datos personales, autenticación, reintentos automáticos y caché de respuestas SRI.
- Exponer el JSON crudo del SRI o información de representantes legales.

## Enfoques considerados

1. **RestClient de Spring — elegido por el usuario.** Encaja con Spring MVC y el servicio síncrono actual; evita dependencias nuevas y permite inyectar el cliente para probar solicitudes HTTP controladas.
2. **HttpClient de Java.** No requiere dependencia, pero deja a la aplicación más trabajo manual para construir solicitudes, mapear estados y conectar el transporte con la configuración de Spring.
3. **WebClient.** Permite E/S reactiva, pero el controlador actual es síncrono; adoptarlo aquí añadiría complejidad y una dependencia sin aportar valor al alcance de esta etapa.

## Flujo propuesto

1. Llamar `existePorNumeroRuc` con el RUC y aceptar únicamente un booleano JSON válido.
2. Si responde `false`, devolver `404 CONTRIBUYENTE_NO_ENCONTRADO` y no llamar al endpoint de detalle.
3. Si responde `true`, llamar `obtenerPorNumerosRuc` y deserializar su arreglo JSON a DTOs externos tolerantes a propiedades nuevas.
4. Si el detalle responde `204` o un arreglo vacío, devolver `404 CONTRIBUYENTE_NO_ENCONTRADO`.
5. Si no existe un registro natural en el resultado, devolver `422 NO_ES_PERSONA_NATURAL`.
6. Para una persona natural, devolver `200` con el DTO local resumido; omitir campos de representantes legales y demás datos no necesarios.
7. Traducir fallos de conexión y timeout a `503 SRI_NO_DISPONIBLE`; traducir respuestas externas inválidas a `502 SRI_RESPUESTA_INVALIDA`. Las respuestas no deben filtrar mensajes ni identificadores del upstream.

El correo se conserva como dato de la solicitud del sistema y no se envía al SRI, cuyos endpoints consultados reciben el RUC.

## Configuración y límites

- Usar `RestClient` con el host y rutas oficiales del SRI configurables desde `application.yml` y variables de entorno, sin credenciales.
- Configurar timeouts de conexión y lectura finitos, ajustables sin cambiar código.
- Mantener `fail-on-unknown-properties` para solicitudes del usuario. Marcar los DTOs externos con tolerancia a campos desconocidos para que una ampliación del JSON del SRI no rompa la integración.
- Usar RUC sintéticos en pruebas automatizadas. Las pruebas HTTP mockeadas se identificarán como tales; no guardar respuestas reales con datos personales en fixtures ni evidencias.

## Comprobación previa del servicio

Se hicieron solicitudes GET de solo lectura a ambos endpoints oficiales. La comprobación observó: existencia con `200 application/json` y booleano; detalle inexistente con `204` sin cuerpo; y detalle existente con `200 application/json` en forma de arreglo con campos como `tipoContribuyente`, `razonSocial`, `estadoContribuyenteRuc` y `actividadEconomicaPrincipal`. El arreglo también puede incluir datos personales de representantes legales; esos campos no se guardarán ni se expondrán. La respuesta positiva observada fue de una entidad institucional, no prueba por sí sola el caso de persona natural.

## Criterios de aceptación

- Una respuesta `false` produce 404 y no solicita detalle.
- Una respuesta positiva seguida de persona natural produce 200 y el DTO local esperado.
- Un contribuyente que no sea persona natural produce 422.
- Un detalle ausente produce 404.
- Timeout, desconexión, JSON inválido y estados inesperados se traducen a errores controlados sin filtrar contenido externo.
- La validación existente, las demás rutas y los códigos de error previos continúan funcionando.
- Las pruebas automatizadas no dependen de la disponibilidad del SRI y no contienen RUC de personas reales.
