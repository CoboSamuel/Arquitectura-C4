# Etapa 6: Integración del frontend React — Diseño

**Estado:** aprobado por el usuario el 2026-11-16.

## Intención

Completar la pantalla inicial de React con el recorrido de consulta del Sistema Web SRI ANT. La interfaz debe verse cuidada y completa, seguir siendo fácil de usar y permitir explicar qué servicio se consulta en cada paso.

## Enfoque recomendado

Un flujo guiado de tres pasos en una sola página. El enunciado ordena las consultas: primero identifica al contribuyente, luego consulta el vehículo y finalmente consulta los puntos de licencia. Una pantalla con pasos visibles conserva ese orden y hace clara la relación React → Backend API → servicios SRI/ANT y Redis Cloud del C4.

También se consideró un panel con tres consultas independientes. Se descarta para esta etapa porque permite saltarse la verificación del contribuyente y no refleja el recorrido pedido. No se añaden rutas, autenticación ni dependencias de interfaz.

## Decisiones confirmadas por el usuario

- La experiencia debe ser fácil de usar, visualmente pulida y suficientemente completa para explicar el proceso en la exposición.
- Mostrar una explicación técnica breve en cada paso, incluyendo el rol de React, el backend Java y los servicios externos; en licencia, explicar el uso de Redis como respaldo.
- Permitir regresar a pasos anteriores para corregir los datos.
- Pedir la cédula de 10 dígitos al llegar al paso de licencia, por separado del RUC.

## Recorrido

1. **Contribuyente:** ingresar correo y RUC de 13 dígitos; enviar `POST /api/v1/consultas/contribuyentes` con `{ correo, ruc }`. Solo con respuesta 200 se muestran los campos allowlisted de persona natural y se habilita el siguiente paso. Los errores de RUC inexistente o de persona no natural detienen el recorrido y muestran un mensaje comprensible.
2. **Vehículo:** ingresar la placa y enviar `POST /api/v1/consultas/vehiculos` con `{ placa }`. Mostrar placa, marca, modelo, año y clase; color y cilindraje cuando estén disponibles. El backend sigue siendo la única parte que llama al SRI.
3. **Licencia:** pedir en este paso la cédula de 10 dígitos, como eligió el usuario, y usar la placa ingresada. Enviar `POST /api/v1/consultas/licencias` con `{ cedula, placa }`. Mostrar puntos disponibles, fecha de actualización y si el origen es ANT o caché. Cuando `desactualizado` sea true, mostrar un aviso visible de dato guardado previamente.

## Presentación e interacción

- Encabezado claro con el nombre del sistema y una breve explicación.
- Indicador de progreso con los tres pasos numerados y el paso actual resaltado.
- Cada paso incluye una explicación corta de qué envía React y qué hace el Backend API con SRI, ANT o Redis Cloud, en palabras que el estudiante pueda explicar.
- Formularios dentro de tarjetas, resultados en paneles separados y etiquetas explícitas para cada dato.
- Estilo visual pulido, responsive y de alto contraste, con acentos violeta y azul oscuro coherentes con la presentación del proyecto. CSS propio, sin librerías nuevas.
- Validación inmediata de formato para correo, RUC, placa y cédula; la validación del backend sigue siendo la autoridad.
- En cada envío, mostrar estado de carga, deshabilitar el botón y evitar solicitudes duplicadas. Mostrar mensajes seguros para errores HTTP y fallos de conexión, con opción de reintentar el paso.
- Botón para iniciar una consulta nueva y limpiar el estado en memoria.
- Permitir regresar a los pasos completados. Si se cambia el RUC, borrar el resultado del contribuyente y todos los datos/resultados posteriores de vehículo y licencia. Si se cambia la placa, borrar el resultado del vehículo y de licencia. Así no se muestra un resultado asociado a datos anteriores.
- No guardar correo, RUC, cédula o placa en localStorage ni imprimirlos en consola.

## Integración y arquitectura

- React se comunica únicamente con la Backend API mediante los tres endpoints REST documentados en OpenAPI; nunca llama a SRI, ANT o Redis desde el navegador.
- Añadir un proxy de desarrollo Vite para `/api` hacia Spring Boot, con `http://localhost:8080` como destino predeterminado y una variable local configurable para cuando el backend use otro puerto. No exponer secretos en Vite.
- Reutilizar el contrato `ApiErrorResponse` (`status`, `code`, `message`, `fieldErrors`) para mostrar errores sin enseñar stack traces ni contenido externo.
- Los datos mostrados se limitan a los campos declarados en los DTOs y OpenAPI. La interfaz no afirmará que una respuesta sintética de prueba proviene de una consulta positiva real.
- Mantener intactos los tres diagramas C4 y el backend, salvo que la revisión encuentre una incompatibilidad concreta de contrato.

## Archivos previstos

- Modificar `frontend/src/App.jsx` para el estado y los pasos del recorrido.
- Crear `frontend/src/api/consultas.js` para las llamadas POST y el manejo común de errores.
- Crear `frontend/src/styles.css` para el diseño responsive e importarlo desde `frontend/src/main.jsx`.
- Crear `frontend/vite.config.js` para el proxy local.
- Añadir pruebas con `node:test` para serialización de solicitudes, respuestas JSON, errores del contrato y fallos de red; actualizar `frontend/package.json` con un script `test` sin instalar dependencias.
- Modificar `frontend/index.html` solo para metadatos/título si la implementación lo requiere.

## Fuera de alcance

- Cambiar contratos REST, reglas de negocio, caché o diagramas C4.
- Crear cuentas, persistir consultas o enviar correos.
- Añadir servicios externos directamente al frontend.
- Afirmar que la integración positiva con la ANT o el esquema positivo vehicular real del SRI quedó validado en vivo.

## Verificación prevista

- Ejecutar `npm test` y `npm run build` desde `frontend`.
- Verificar manualmente con React y Spring Boot activos que se puede completar el recorrido, que los errores impiden avanzar indebidamente y que el resultado de licencia distingue ANT de caché.
- Guardar las evidencias de Etapa 6 en `docs/evidencia/`; el usuario realizará el commit manual siguiendo las reglas del proyecto.
