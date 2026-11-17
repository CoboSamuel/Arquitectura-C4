# Etapa 8 — SRI real, ANT simulada y caché Redis Cloud

## Objetivo

La aplicación debe consultar mediante HTTP real los tres servicios del SRI que están disponibles en el enunciado: validar el RUC, obtener los datos de la persona natural y consultar el vehículo. La consulta de puntos de licencia de ANT debe conservar el contrato real de entrada y salida, pero utilizar una implementación simulada por defecto porque la página externa comprobada responde `502 Bad Gateway`.

La implementación simulada debe permitir demostrar el mismo comportamiento de resiliencia que tendría la integración real: una respuesta válida se guarda en Redis Cloud y, ante una falla configurable del proveedor ANT, se devuelve el último dato guardado con una indicación explícita de que puede estar desactualizado. El cliente HTTP real de ANT se conserva detrás del mismo contrato para poder activarlo cuando el upstream se recupere.

## Decisiones

- `live` es el perfil Spring predeterminado para SRI real y ANT simulado. No habrá fixtures sintéticas para las respuestas del SRI.
- Se conservan los clientes HTTP Java existentes y las rutas/contratos del enunciado. React mantiene el flujo de correo/RUC, placa y cédula sin mostrar nombres técnicos como REST o JSON.
- La implementación simulada de ANT acepta la misma cédula y placa que el cliente real y devuelve el mismo resumen de puntos, fecha, origen y estado de frescura que consume la interfaz.
- Redis Cloud continúa usando `REDIS_URL` y el secreto HMAC local. La caché guarda únicamente puntos y fecha; el correo y los identificadores no se incluyen en su valor.
- Las pruebas automatizadas sustituyen los upstreams con respuestas controladas; la evidencia manual mostrará respuestas reales del SRI y la simulación documentada de ANT.
- La interfaz debe distinguir una respuesta simulada de una respuesta real y debe advertir cuando se muestra un valor recuperado desde caché.

## Contratos externos definidos por el enunciado

- SRI existencia de RUC: `GET https://srienlinea.sri.gob.ec/sri-catastro-sujeto-servicio-internet/rest/ConsolidadoContribuyente/existePorNumeroRuc?numeroRuc={ruc}`.
- SRI detalle de contribuyente: `GET https://srienlinea.sri.gob.ec/sri-catastro-sujeto-servicio-internet/rest/ConsolidadoContribuyente/obtenerPorNumerosRuc?ruc={ruc}`.
- SRI vehículo: `GET https://srienlinea.sri.gob.ec/sri-matriculacion-vehicular-recaudacion-servicio-internet/rest/BaseVehiculo/obtenerPorNumeroPlacaOPorNumeroCampvOPorNumeroCpn?numeroPlacaCampvCpn={placa}`.
- ANT puntos: `GET https://consultaweb.ant.gob.ec/PortalWEB/paginas/clientes/clp_grid_citaciones.jsp?ps_tipo_identificacion=CED&ps_identificacion={cedula}&ps_placa={placa}`. En la entrega se invoca el proveedor simulado con esos mismos parámetros de contrato; el cliente HTTP permanece disponible para reactivarlo cuando el sitio deje de responder `502`.

## Decisiones cerradas para la implementación

- Si falta `ANT_CACHE_KEY_SECRET` o es demasiado corto, la consulta de licencia responde `503 CACHE_NO_CONFIGURADA`; no se llama al proveedor ni se accede a Redis.
- La ventana de frescura es 15 minutos y la retención máxima del último dato es 7 días. Un dato fuera de la retención se trata como expirado y no se muestra.
- La clave de caché se deriva de cédula y placa normalizadas mediante HMAC; el valor cacheado contiene solo puntos y fecha.
- Si ANT simulado falla y existe un dato retenido, se responde `200` con `origen=CACHE` y `desactualizado=true`; sin dato retenido se responde `503`.
- La simulación tendrá un resultado válido estable y un control de fallo configurable para reproducir el caso de indisponibilidad en pruebas y evidencia, sin usar datos personales.
- El enlace de repositorio que debe mantenerse en IcePanel es `https://github.com/CoboSamuel/Arquitectura-C4.git`; esta tarea no modifica el proyecto ni los diagramas de IcePanel.
- Se conserva el diseño C4 existente y el enlace al repositorio queda en la descripción de la raíz de contexto de IcePanel.

## Alternativas consideradas

1. **SRI real + ANT simulado por defecto — propuesta seleccionada.** Cumple la instrucción del amigo: los tres servicios disponibles se consultan realmente y el cuarto conserva el contrato sin depender de la página ANT caída. El coste es mantener dos implementaciones detrás del proveedor ANT.
2. SRI y ANT reales por defecto con caché — descartada para esta entrega porque contradice la instrucción de simular el cuarto servicio mientras ANT no esté disponible.
3. Todo simulado — descartada porque no demuestra las integraciones reales del SRI solicitadas por el deber.

## Fuera de alcance

- Guardar credenciales o datos personales reales en el repositorio, evidencia o chat.
- Provisionar o borrar la base Redis Cloud.
- Redibujar o cambiar componentes/conexiones de los tres diagramas C4.
- Presentar como real una respuesta proveniente de la implementación simulada de ANT.

## Criterios de aceptación

1. Sin variables de perfil, Spring inicia con `live`, registra los dos clientes reales de SRI y selecciona el proveedor simulado de ANT.
2. Las consultas de RUC/persona natural y vehículo salen a los endpoints oficiales del SRI; la UI no muestra fixtures del SRI, credenciales ni datos inventados.
3. El proveedor ANT simulado acepta la entrada real del flujo y entrega el contrato de puntos esperado; su estado se identifica como simulado.
4. Una respuesta válida de ANT se guarda en Redis; una falla del proveedor devuelve la última respuesta dentro de la retención configurada y la UI muestra que puede estar desactualizada.
5. El cliente HTTP real de ANT queda disponible mediante configuración, sin ser necesario para la entrega mientras el upstream siga en `502`.
6. `.env.example` contiene solo marcadores vacíos para secretos y la guía explica PowerShell sin pedir que se compartan valores.
7. Suites Java/React y build pasan; la verificación real de SRI y Redis Cloud y la evidencia de ANT simulado se capturan desde el entorno privado del usuario.
