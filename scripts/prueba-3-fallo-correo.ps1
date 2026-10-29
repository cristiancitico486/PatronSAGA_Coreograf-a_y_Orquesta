# Escenario 3: el servidor de correo falla temporalmente (503) los primeros 3 envios.
# Esperado: Temporal reintenta con backoff, el correo sale al 4.o intento y la reserva NO se compensa.
. "$PSScriptRoot\comun.ps1"
$Fallos = 0

Write-Host '=== PRUEBA 3: fallo temporal del correo ===' -ForegroundColor Magenta
Invoke-RestMethod -Method Post -Uri 'http://localhost:8085/api/notificaciones/simular-fallos?veces=3' | Out-Null
Write-Host 'Los proximos 3 envios de correo responderan 503'

$saga = Iniciar-Saga (Nueva-Solicitud)
$r = Esperar-Resultado $saga
Mostrar-Historial $saga
Mostrar-Bases $saga

$intentos = @(Select-String -Path (Join-Path $Raiz 'logs\orquestador.log') -Pattern "\[$saga\] enviarNotificacion").Count

Write-Host "`nVerificaciones:"
Verificar ($r.estado -eq 'CONFIRMADA') 'Saga CONFIRMADA (el correo no deshace la reserva)'
Verificar ($r.notificacionEnviada) 'Correo enviado finalmente'
Verificar ($intentos -eq 4) "4 intentos de envio: 3 fallidos + 1 exitoso (intentos: $intentos)"
Verificar ((Estado-En 'http://localhost:8084/api/pagos' $saga) -eq 'APROBADO') 'Pago sigue APROBADO'
exit $Fallos
