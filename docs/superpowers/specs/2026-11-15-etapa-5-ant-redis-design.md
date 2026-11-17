# Etapa 5 — consulta ANT y caché Redis Cloud

## Intención

Implementar el flujo de consulta de puntos de licencia que ya existe en el contrato (`POST /api/v1/consultas/licencias`) y respetar el contenedor `Redis Cloud` y los componentes `ANTClient` / `LicenciaCacheService` del C4. El usuario debe recibir el último dato válido cuando la página de la ANT no esté disponible.

## Contexto observado

- El request existente acepta cédula y placa; el servicio aún devuelve `501 INTEGRACION_PENDIENTE`.
- La ANT se consulta mediante una página JSP, no mediante un contrato JSON documentado. La herramienta de navegación no pudo recuperar la URL indicada y devolvió 502; no se verificó la disponibilidad del servicio desde la aplicación. La página oficial de consulta de licencias aparece como formulario en el índice público ([ANT — Consulta de Citaciones](https://consultaweb.ant.gob.ec/PortalWEB/paginas/clientes/clp_criterio_consulta.jsp?ps_param_tip_serv=LIC)).
- El enunciado especifica una consulta GET con `ps_tipo_identificacion=CED`, `ps_identificacion` y `ps_placa`. Se asume una petición directa sin sesión; si ANT devuelve una pantalla de sesión, CAPTCHA o bloqueo, se tratará como fallo del upstream y se usará el caché disponible. No se intentará evadir controles.
- La implementación debe tolerar HTML inválido o cambios del sitio sin guardar una respuesta dudosa como buena.
- Los diagramas C4 existentes son la referencia y no se modificarán.

## Enfoques considerados

1. **Spring Data Redis sobre el protocolo Redis estándar (seleccionado).** Conecta con Redis Cloud configurado mediante URL/variables de entorno, conserva el diseño del C4 y evita atar el código a un proveedor.
2. **API REST de un proveedor Redis específico.** Puede simplificar conectividad de salida, pero introduce un contrato y credenciales propios del proveedor que el C4 no especifica.

El usuario indicó que se debe seguir siempre el C4; se elige el primer enfoque.

## Alcance

- Añadir un `ANTClient` HTTP con base URL y ruta configurables, parámetros URL codificados, timeout finito y redirecciones desactivadas.
- Analizar el HTML con Jsoup y extraer solo el resumen identificado por la etiqueta `Puntos:`. La respuesta local no incluirá nombre, cédula, citaciones, placas de terceros, texto HTML ni otros datos personales.
- Exigir exactamente un valor de puntos en el resumen; aceptar cero como valor válido y normalizar punto/coma decimal a `BigDecimal`. Si falta el resumen, aparecen valores ambiguos o el HTML no es reconocible, clasificarlo como respuesta inválida; no inferir que significa cero.
- Usar un fixture sintético con el marcador explícito `No se encontraron registros` (sin distinguir mayúsculas/minúsculas) para cubrir 404. Ese marcador no está confirmado contra la página real; cualquier HTML sin marcador conocido y sin resumen de puntos se tratará como inválido, no como ausencia confirmada.
- Completar `POST /api/v1/consultas/licencias` con una respuesta de puntos y metadatos de procedencia/actualización.
- Añadir un puerto de caché y un adaptador Redis Cloud para `LicenciaCacheService`; serializar únicamente la respuesta local, nunca HTML externo.
- Consultar caché primero. Si el valor está fresco, responder desde Redis sin invocar ANT. En miss o expiración de frescura, consultar ANT y actualizar la caché solo después de validar respuesta y puntos.
- Mantener por separado la ventana de frescura y el tiempo máximo de retención de un valor anterior. Valores iniciales configurables: frescura 15 minutos y retención de último dato válido 7 días.
- Si ANT falla o entrega HTML no interpretable y hay un valor anterior retenido, devolver ese valor indicando `origen=CACHE` y `desactualizado=true`. Si no existe valor utilizable, responder con error genérico 503; no revelar el error externo.
- Un resultado válido que indique ausencia de licencia se responde como 404, no se almacena como dato positivo e invalida cualquier resumen positivo previo de esa clave. Una respuesta inválida nunca reemplaza la última respuesta válida.
- Si ANT responde bien pero Redis no permite leer/escribir, devolver la respuesta actual de ANT y registrar solo un aviso genérico sin datos sensibles; si ANT falla y Redis también está inaccesible, devolver 503.
- Construir la clave de caché como HMAC-SHA-256 de tipo de consulta, cédula y placa normalizadas. `ANT_CACHE_KEY_SECRET` tendrá al menos 32 bytes y `REDIS_URL` se configura fuera del repositorio. Si el secreto no existe o es demasiado corto, devolver 503 antes de llamar a ANT; no registrar identificadores ni contenido HTML.
- Documentar configuración segura con variables de entorno y ejemplos sin secretos reales.

## Contrato local propuesto

`ConsultaLicenciaResponse` expone solamente:

- `puntosDisponibles`: decimal no negativo, para no asumir que ANT siempre presenta puntos enteros.
- `actualizadoEn`: instante UTC de la última respuesta válida de ANT.
- `origen`: `ANT` o `CACHE`.
- `desactualizado`: booleano que permite a la interfaz mostrar que se sirvió un valor anterior.

El identificador consultado no se devuelve ni se guarda en el valor de caché.

## Configuración inicial (valores ajustables)

- `ANT_BASE_URL` y `ANT_PATH`.
- `ANT_CONNECT_TIMEOUT` (3 s) y `ANT_READ_TIMEOUT` (5 s).
- `REDIS_URL` para el Redis Cloud del proyecto.
- `ANT_CACHE_KEY_SECRET` (mínimo 32 bytes), entregado localmente o en el entorno de despliegue, nunca en Git.
- `ANT_CACHE_FRESHNESS` (15 min) y `ANT_CACHE_MAX_STALE_TTL` (7 días).

La aplicación de pruebas no necesita red ni credenciales cloud: cliente ANT y almacén Redis se simulan. La ejecución conectada a Redis Cloud requiere que el usuario configure sus variables de entorno localmente; no se solicitarán credenciales en el chat.

Los fixtures de éxito, cero puntos, decimal, sin registros y pantalla de error son contratos sintéticos del demo. No prueban que el HTML de producción tenga exactamente el mismo marcado; la primera respuesta real disponible deberá validar el supuesto sin guardar identificadores ni cuerpos HTML.

## Errores

- `404 LICENCIA_NO_ENCONTRADA`: ANT confirma explícitamente que no hay una licencia para la consulta. Un valor válido de cero puntos no es un 404.
- `502 ANT_RESPUESTA_INVALIDA`: HTML no reconocido y no hay dato anterior válido.
- `503 ANT_NO_DISPONIBLE`: timeout, desconexión o error temporal sin dato anterior utilizable.
- `503 CACHE_NO_CONFIGURADA`: falta el secreto HMAC requerido para proteger la clave de caché.
- Si se pudo usar un dato anterior, responder 200 y señalar su origen/desactualización en el contrato local.
- Nunca copiar cuerpos HTML ni mensajes externos a errores públicos o logs.

## Fuera de alcance

- Cambios a React, provisionamiento de un proveedor Redis, despliegue cloud, autenticación, otras consultas de citaciones, persistencia SQL y cambios a IcePanel.
- Validar el parser con datos o identificadores personales reales. El caso positivo de prueba usará HTML sintético y se documentará como tal.

## Criterios de aceptación

- Fresco en Redis produce respuesta desde caché y no llama a ANT.
- Respuesta sintética válida de ANT se convierte al contrato local y se almacena sin PII ni HTML.
- Error ANT con dato anterior retenido responde 200 con marca de caché antigua; sin valor previo responde 503.
- HTML inválido no reemplaza el dato anterior; sin valor previo responde 502.
- Redis caído no bloquea una respuesta ANT válida; si ANT y Redis fallan, responde 503.
- La respuesta de ausencia confirmada invalida el dato positivo anterior; un fallo ANT posterior no debe devolver ese dato.
- La ausencia del secreto HMAC produce 503 sin consultar ANT ni acceder a Redis.
- Claves y respuestas no exponen cédula/placa; errores no incluyen datos externos.
- OpenAPI describe 200/404/502/503 y deja de anunciar 501 para licencias.
- Pruebas con fixtures sintéticas, suite completa verde, evidencias sanitizadas guardadas para el commit manual.
