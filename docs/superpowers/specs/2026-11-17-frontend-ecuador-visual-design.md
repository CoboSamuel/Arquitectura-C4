# Diseño UX/UI: identidad ecuatoriana para Consulta ciudadana

## Intención

Dar al frontend una presencia de página de servicio real: más memorable, más cálida y con una identidad visual relacionada con Ecuador, sin parecer un panel técnico ni afirmar que es un portal oficial.

## Objetivos

- Rediseñar la portada y el flujo de consulta con una paleta inspirada en Ecuador: azul noche para confianza, terracota para acción y dorado como acento.
- Incorporar una ilustración editorial de Quito/Ecuador como recurso visual principal, claramente decorativa y sin atribuirla a una institución pública.
- Mantener la consulta en tres pasos, pero presentar el formulario dentro de una composición más editorial y fácil de escanear.
- Detectar errores frecuentes en dominios de correo populares —por ejemplo `gmiol.com`— y sugerir la corrección antes de permitir continuar.
- Conservar accesibilidad: foco visible, `aria-invalid`, mensajes asociados al campo y mensajes comprensibles para teclado y lectores de pantalla.

## No objetivos

- No cambiar las APIs, el backend, los contratos de consulta ni la lógica de negocio.
- No bloquear dominios institucionales o personalizados solo porque no pertenecen a una lista de proveedores conocidos.
- No usar logos oficiales del SRI, ANT, Municipio de Quito o Gobierno del Ecuador.
- No presentar la imagen como fotografía documental ni como aval institucional.

## Anti-objetivos

- No usar una bandera como decoración dominante ni una estética turística que distraiga de completar la consulta.
- No añadir carruseles, animaciones pesadas, tarjetas repetitivas o texto promocional.
- No aceptar silenciosamente un error conocido de digitación en un dominio común.

## Restricciones

- El proyecto usa React/Vite y CSS existente en `frontend/src`.
- La imagen debe funcionar como asset local del frontend o como ilustración embebida estable; la pantalla no debe depender de una API externa para renderizarse.
- La validación conserva los mensajes actuales para campos vacíos y formatos inválidos, añadiendo una regla específica para sugerencias de dominio.

## Enfoques considerados

1. **Portal institucional sobrio.** Blanco, azul petróleo y franja tricolor; alta claridad, pero poca personalidad y un cambio visual menor respecto a la pantalla actual.
2. **Portal ciudadano ecuatoriano con ilustración editorial (elegido).** Azul noche, terracota y dorado; una imagen de Quito en la portada y el formulario integrado en una tarjeta clara. Equilibra confianza, identidad local y foco en la tarea.
3. **Landing turístico/digital.** Imagen protagonista y colores más vivos; puede llamar la atención, pero se aleja del tono de servicio que necesita una consulta de datos.

Se elige el enfoque 2 porque la página necesita verse propia y cercana sin perder seriedad. La imagen será ilustrativa, no una fotografía oficial, y tendrá texto alternativo descriptivo.

## Diseño propuesto

- La cabecera usa azul noche, marca `Consulta ciudadana`, navegación breve y una línea de acento dorado/terracota.
- La portada se divide en una columna editorial con el mensaje de servicio y una imagen panorámica de Quito; el botón **Iniciar consulta** es terracota y queda visible sin desplazamiento en escritorio.
- La sección de consulta usa una tarjeta blanca con encabezado, progreso y formulario; la ayuda se convierte en una columna de contexto con iconos simples y no en una segunda interfaz.
- Los campos mantienen el foco teal y los errores usan rojo cálido con una frase corta. Un correo con dominio sospechoso muestra una sugerencia específica, por ejemplo: `Revisa el dominio. ¿Quizá quisiste escribir gmail.com?`; la consulta no continúa hasta corregirlo.
- En móvil la ilustración pasa arriba o se recorta a una franja breve, el formulario queda primero y la acción primaria ocupa el ancho disponible.

## Pendientes para implementación

- Generar/integrar la ilustración local de Quito y añadir texto alternativo.
- Ajustar `App.jsx` para la nueva composición sin tocar consultas existentes.
- Añadir detección reusable de errores de dominio y sus pruebas.
- Reescribir los overrides visuales para evitar que las reglas antiguas de `styles.css` se contradigan.
- Actualizar pruebas de render y ejecutar tests/build.
