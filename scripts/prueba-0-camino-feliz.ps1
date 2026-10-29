# Referencia: todo sale bien.
. "$PSScriptRoot\comun.ps1"
$Fallos = 0

Write-Host '=== PRUEBA 0: camino feliz ===' -ForegroundColor Magenta
$saga = Iniciar-Saga (Nueva-Solicitud)
$r = Esperar-Resultado $saga
Mostrar-Historial $saga
Mostrar-Bases $saga

Write-Host "`nVerificaciones:"
Verificar ($r.estado -eq 'CONFIRMADA') "Saga CONFIRMADA"
Verificar ((Estado-En 'http://localhost:8081/api/vuelos/reservas' $saga) -eq 'CONFIRMADA') 'Vuelo confirmado'
Verificar ((Estado-En 'http://localhost:8082/api/hoteles/reservas' $saga) -eq 'CONFIRMADA') 'Hotel confirmado'
Verificar ((Estado-En 'http://localhost:8083/api/autos/reservas' $saga) -eq 'CONFIRMADA') 'Auto confirmado'
Verificar ((Estado-En 'http://localhost:8084/api/pagos' $saga) -eq 'APROBADO') 'Pago aprobado'
Verificar ($r.notificacionEnviada) 'Correo de confirmacion enviado'
exit $Fallos
