# Ejecutar SRI real y ANT simulada

Spring usa `live` por defecto. La aplicación consulta por HTTP real los tres servicios disponibles del SRI. La consulta ANT conserva sus entradas y su contrato, pero usa un proveedor simulado porque la página comprobada responde `502 Bad Gateway`; la interfaz lo identifica claramente.

## Servicios conectados

- [SRI: verificar RUC](https://srienlinea.sri.gob.ec/sri-catastro-sujeto-servicio-internet/rest/ConsolidadoContribuyente/existePorNumeroRuc?numeroRuc=)
- [SRI: obtener datos del contribuyente](https://srienlinea.sri.gob.ec/sri-catastro-sujeto-servicio-internet/rest/ConsolidadoContribuyente/obtenerPorNumerosRuc?&ruc=)
- [SRI: buscar vehículo por placa](https://srienlinea.sri.gob.ec/sri-matriculacion-vehicular-recaudacion-servicio-internet/rest/BaseVehiculo/obtenerPorNumeroPlacaOPorNumeroCampvOPorNumeroCpn?numeroPlacaCampvCpn=)
- [ANT: contrato de consulta de puntos](https://consultaweb.ant.gob.ec/PortalWEB/paginas/clientes/clp_grid_citaciones.jsp?ps_tipo_identificacion=CED&ps_identificacion=XXXXXXXX&ps_placa=) (simulado por defecto en la entrega)

El flujo manda el RUC al SRI y muestra únicamente personas naturales; luego manda la placa al SRI. En el último paso envía cédula y placa al proveedor ANT simulado, que devuelve el mismo contrato de puntos. Una respuesta válida se guarda en Redis Cloud. Ante una falla, el sistema devuelve la última respuesta guardada que aún esté dentro de la retención configurada.

El resultado de ANT muestra `Servicio ANT simulado`. Si se sirve el último valor desde Redis, muestra `Última consulta disponible` y una advertencia de posible desactualización.

## 1. Preparar Redis Cloud en PowerShell

Para probar únicamente el recorrido simulado de puntos no necesitas Redis Cloud ni configurar un secreto: con el perfil `live`, la aplicación usa ANT simulado, 30 puntos por defecto y un secreto local de desarrollo. Redis Cloud y `ANT_CACHE_KEY_SECRET` son necesarios para demostrar la recuperación de un dato anterior después de una falla y para conservar la caché entre reinicios.

Abre una terminal **PowerShell** en Visual Studio Code desde la raíz del repositorio. En Redis Cloud pulsa **Connect** y copia la URI de conexión de la base. Si copiaste una línea `redis-cli -u ...`, pega solo la URI que viene después de `redis-cli -u `.

Ejecuta lo siguiente; en el primer prompt pega la URI completa. PowerShell ocultará el texto:

```powershell
$redisUriSegura = Read-Host "Pega la URI completa de conexión de Redis Cloud" -AsSecureString
$env:REDIS_URL = [System.Net.NetworkCredential]::new("", $redisUriSegura).Password
Remove-Variable redisUriSegura
$env:SPRING_PROFILES_ACTIVE = "live"
$env:ANT_CACHE_KEY_SECRET = [Convert]::ToBase64String([System.Security.Cryptography.RandomNumberGenerator]::GetBytes(32))
$env:ANT_CACHE_FRESHNESS = "15m"
$env:ANT_CACHE_MAX_STALE_TTL = "7d"
$env:ANT_SIMULATION_POINTS = "30"
$env:ANT_SIMULATION_AVAILABLE = "true"
```

`default` es el usuario de la base Redis; no es la contraseña. Usa la contraseña que forma parte de la URI de conexión de esa base, no la contraseña de inicio de sesión de Redis Cloud. No pegues la URI ni el secreto en el chat, `.env.example`, capturas o Git. Mantén abierta esta terminal: esas variables solo duran mientras exista la sesión.

La plantilla `.env.example` no se carga automáticamente. No necesitas Docker ni instalar Redis local.

## 2. Comprobar la conexión a Redis Cloud

Desde la misma terminal ejecuta:

```powershell
Set-Location .\backend
mvn -B -ntp '-Dtest=RedisCloudConnectivityIT' test
```

La prueba escribe y lee una clave temporal aleatoria en Redis y la deja expirar en 60 segundos. No llama al SRI ni a la ANT.

## 3. Iniciar Java y React

En esa terminal, inicia el backend:

```powershell
mvn spring-boot:run
```

Abre otra terminal PowerShell en la raíz del repositorio e inicia React:

```powershell
Set-Location .\frontend
npm run dev
```

Abre la dirección que muestra Vite, normalmente `http://localhost:5173`.

## 4. Probar el recorrido

Ingresa tus datos válidos directamente en tu computador: correo y RUC, luego la placa del vehículo y finalmente la cédula. No me envíes esos datos. El correo es obligatorio para el flujo; las dos consultas de contribuyente y la consulta de vehículo salen al SRI real. La última consulta usa el contrato de ANT con el proveedor simulado.

Si una respuesta del SRI no llega o no tiene el formato esperado, la página muestra un mensaje entendible y permite reintentar los fallos temporales. Para la evidencia de ANT, captura por separado el `502 Bad Gateway` del sitio externo con fecha, hora y URL, y luego muestra en la aplicación el resultado `Servicio ANT simulado`.

## 5. Probar la recuperación desde Redis

Esta prueba guarda primero una respuesta válida del proveedor ANT simulado y después fuerza su falla. Usa el mismo Redis y el mismo secreto HMAC durante todo el procedimiento:

1. Completa una consulta de licencia y confirma que aparece `Servicio ANT simulado`.
2. Detén el backend con `Ctrl+C`. En la misma terminal configura la frescura de prueba en 5 segundos y reinicia:

   ```powershell
   $env:ANT_CACHE_FRESHNESS = "5s"
   $env:ANT_SIMULATION_AVAILABLE = "true"
   mvn spring-boot:run
   ```

3. Espera al menos 6 segundos desde la respuesta anterior y detén el backend.
4. En esa misma terminal desactiva el proveedor simulado y reinicia:

   ```powershell
   $env:ANT_SIMULATION_AVAILABLE = "false"
   mvn spring-boot:run
   ```

5. En el navegador inicia una nueva consulta y repite la misma combinación de cédula y placa. La pantalla debe indicar que muestra una consulta anterior.
6. Cuando termines, detén el backend y restaura la simulación y la frescura normal:

   ```powershell
   $env:ANT_CACHE_FRESHNESS = "15m"
   $env:ANT_SIMULATION_AVAILABLE = "true"
   mvn spring-boot:run
   ```

La cédula y la placa que generan la clave Redis se protegen con HMAC. El valor almacenado contiene puntos y fecha de consulta; no guarda correo ni RUC.

## 6. Activar el cliente ANT real cuando se recupere

No es necesario para la entrega mientras la página siga respondiendo `502`. Cuando vuelva a estar disponible, detén el backend y activa ambos perfiles para conservar SRI real y sustituir la simulación por el cliente HTTP ANT:

```powershell
$env:SPRING_PROFILES_ACTIVE = "live,ant-real"
Remove-Item Env:ANT_SIMULATION_AVAILABLE -ErrorAction SilentlyContinue
mvn spring-boot:run
```

Con `live` solo, el proveedor simulado es el único activo. Con `live,ant-real`, el cliente HTTP real es el único proveedor ANT activo.

## Evidencia

Antes de capturar, oculta correo, RUC, cédula, placa y cualquier URI o contraseña. Captura las tres respuestas reales del SRI, el `502` de ANT con fecha/hora/URL, el resultado ANT simulado, el estado desde caché, pruebas y build; no subas datos personales.
