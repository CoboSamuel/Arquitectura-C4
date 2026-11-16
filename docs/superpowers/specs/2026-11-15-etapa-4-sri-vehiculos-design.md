# Etapa 4 — Consulta de vehículos del SRI

## Estado

Contrato académico simulado, definido por el equipo porque no se dispone de un resultado positivo real. Las pruebas de éxito no validan el esquema positivo oficial del SRI; si el endpoint devuelve otro esquema, se ajustará el adaptador sin exponer el cuerpo crudo.

## Intención

Implementar la consulta de vehículo en el backend Spring Boot usando la placa recibida por `POST /api/v1/consultas/vehiculos` y el servicio oficial BaseVehiculo del SRI. Devolver un resumen local con los datos vehiculares necesarios y omitir información del titular.

## Alcance

- Reemplazar la respuesta provisional `501 INTEGRACION_PENDIENTE` de la ruta de vehículos.
- Consultar `GET /sri-matriculacion-vehicular-recaudacion-servicio-internet/rest/BaseVehiculo/obtenerPorNumeroPlacaOPorNumeroCampvOPorNumeroCpn` con `numeroPlacaCampvCpn`.
- Reutilizar la configuración existente de `RestClient`, timeout finito y política sin redirecciones; configurar la ruta mediante `SRI_VEHICLE_PATH`.
- Interpretar el sobre observado: `data` (arreglo), `objeto` (nullable) y `mensajeServidor` (objeto con `texto`). La comprobación con placa sintética sin coincidencia devolvió HTTP 200, `data: []` y `objeto: null`.
- Mapear cero resultados a `404 VEHICULO_NO_ENCONTRADO`, fallos de conectividad/timeout/upstream temporal a `503 SRI_NO_DISPONIBLE`, y respuesta inválida a `502 SRI_RESPUESTA_INVALIDA`.
- Para resultados positivos simulados, devolver un DTO propio con placa, marca, modelo, año, clase, color y cilindraje; no copiar titular, RUC/cédula, dirección, identificadores de motor/chasis, `mensajeServidor.texto` ni la carga externa completa.
- Actualizar OpenAPI, pruebas y bitácora privada. No cambiar los diagramas IcePanel.

## Fuera de alcance

- ANT, caché cloud/Redis, React, persistencia, autenticación, reintentos y consultas por cédula.
- Enviar o registrar datos del titular.
- Afirmar que la fixture sintética representa el contrato positivo real del SRI.

## Flujo propuesto

1. Aplicar la validación existente de la placa.
2. Consultar el endpoint oficial del SRI con el parámetro de placa configurado.
3. Validar el sobre JSON y la estructura de `data`.
4. Si no hay resultado, devolver `404 VEHICULO_NO_ENCONTRADO` sin exponer el mensaje upstream.
5. Si hay resultado, seleccionar `objeto` o el primer registro de `data` y mapear únicamente las propiedades del contrato simulado: `numeroPlaca`, `marca`, `modelo`, `anioModelo`, `claseVehiculo`, `color` y `cilindraje`.
6. Convertir timeout/desconexión/HTTP 408, 429 o 5xx a 503; protocolo, JSON o esquema inválido a 502.

## Configuración y límites

- Base URL, ruta y timeouts provienen de `integrations.sri` y sus variables de entorno; no se agregan credenciales ni dependencias.
- Usar placa sintética en pruebas automatizadas; no incluir vehículos reales en fixtures ni evidencia.
- Las pruebas de cliente simulan el HTTP y no dependen de la red.
- Mantener estables las rutas ya implementadas de contribuyentes y las validaciones existentes.

## Comprobación de servicio

El navegador no pudo abrir la ruta REST y la búsqueda oficial no encontró documentación pública del JSON positivo. Una solicitud GET de solo lectura con la placa sintética `ZZZ9999` devolvió HTTP 200 y un sobre vacío (`data: []`, `objeto: null`, `mensajeServidor.texto`). Una consulta local del usuario con otra placa también devolvió `data: []` y `objeto: null`; no se registraron ni compartieron la placa o valores.

El usuario indicó que el caso de éxito del proyecto debe construirse con datos ficticios. La fixture sintética usa las claves `numeroPlaca`, `marca`, `modelo`, `anioModelo`, `claseVehiculo`, `color` y `cilindraje` dentro de `objeto` o `data`. Esa fixture verifica el flujo y el filtro de datos, no la interoperabilidad positiva real con SRI.

## Criterios de aceptación

- La solicitud válida llega al endpoint oficial con la placa correctamente codificada como query param.
- Una respuesta de `data` vacío resulta en `404 VEHICULO_NO_ENCONTRADO`.
- Una fixture sintética positiva resulta en `200` con el resumen vehicular acordado y excluye datos del titular. La integración real para placas sin coincidencia se verifica con la respuesta vacía del SRI.
- Timeout y fallos transitorios producen 503; JSON/sobre inválido produce 502, sin contenido crudo del SRI.
- El endpoint de vehículos deja de anunciar 501 en OpenAPI; licencias permanece pendiente.
- La suite completa pasa y las evidencias se guardan en `docs/evidencia/` para el commit manual del usuario.
