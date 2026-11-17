# Diseño UX/UI: inicio y validación de Consulta ciudadana

## Intención

Convertir la consulta actual en una experiencia ciudadana más clara y confiable. La persona debe entender qué puede consultar, qué datos necesita y cómo avanzar antes de encontrarse con el primer formulario.

## Objetivos

- Añadir una portada de inicio con propósito, alcance, datos necesarios y un botón visible **Iniciar consulta**.
- Conservar el flujo funcional de tres pasos y sus consultas existentes.
- Hacer que los errores de formato aparezcan mientras la persona escribe, sin esperar al envío.
- Reducir la apariencia de prototipo mediante jerarquía tipográfica sobria, superficies blancas, color institucional contenido y copys directos.
- Mantener navegación por teclado, foco visible y anuncios accesibles para errores y estados de carga.

## No objetivos

- No cambiar contratos de API, servicios del backend ni reglas de negocio.
- No convertir el proyecto académico en un portal oficial ni añadir autenticación, persistencia o trámites nuevos.
- No copiar literalmente la identidad visual del SRI.

## Restricciones

- Se trabaja sobre React/Vite y los componentes actuales de `frontend/src/App.jsx`.
- Los cambios deben convivir con modificaciones previas del repositorio.
- La validación de cliente seguirá usando `frontend/src/consulta/validacion.js`.

## Enfoques considerados

1. **Ajuste incremental del formulario actual.** Menos riesgo, pero mantiene la entrada directa y la sensación de prototipo.
2. **Portada ciudadana + flujo actual refinado (elegido).** Añade contexto antes de la acción, conserva la lógica existente y mejora los estados de campo sin reescribir el dominio.
3. **Rediseño completo tipo portal institucional.** Podría verse más oficial, pero ampliaría el alcance y añadiría navegación que no ayuda a completar esta consulta.

Se elige el enfoque 2 porque responde directamente a la necesidad de entender la página antes de llenar datos y mantiene el comportamiento ya implementado.

## Diseño propuesto

- La pantalla inicial muestra una cabecera limpia, una franja de contexto, el título **Consulta ciudadana** y una explicación breve de que reúne información tributaria, vehicular y de licencia en un recorrido de tres pasos.
- Tres bloques cortos explican qué se consulta, qué se necesita y cuánto tarda. La acción primaria **Iniciar consulta** cambia al flujo existente y lleva el foco al título del paso 1.
- El formulario conserva sus campos y botones, pero cada campo maneja un estado `tocado`: al entrar y salir se valida; después del primer intento o de una modificación posterior, el error se actualiza en cada cambio. Un valor inválido muestra borde, texto de error y `aria-invalid`; un valor válido muestra confirmación discreta sin ruido visual.
- La ayuda lateral se mantiene, pero usa lenguaje de servicio: qué tener a mano, cómo escribir cada identificador y qué ocurre si una fuente no está disponible.
- En móvil la portada se apila, la acción primaria ocupa el ancho disponible y el paso activo sigue siendo el foco visual.

## Pendientes para implementación

- Añadir estado local de portada iniciada y botón de entrada.
- Añadir validación incremental por campo sin modificar las funciones de validación existentes.
- Ajustar copys, clases y CSS responsive.
- Actualizar pruebas de renderizado para la portada y añadir pruebas de validación en vivo si la estructura lo permite.
