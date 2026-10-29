# Escenario 2: la pasarela rechaza la tarjeta (402, NO reintentable).
# Esperado: un solo intento de cobro y compensacion de auto, hotel y vuelo en orden inverso.
. "$PSScriptRoot\comun.ps1"
$Fallos = 0

Write-Host '=== PRUEBA 2: pago rechazado ===' -ForegroundColor Magenta
$saga = Iniciar-Saga (Nueva-Solicitud -Tarjeta '4000-0000-0000-0002')
$r = Esperar-Resultado $saga
Mostrar-Historial $saga
Mostrar-Bases $saga

$intentos = @(Select-String -Path (Join-Path $Raiz 'logs\orquestador.log') -Pattern "\[$saga\] cobrarPago").Count

Write-Host "`nVerificaciones:"
Verificar ($r.estado -eq 'CANCELADA') "Saga CANCELADA ($($r.motivo))"
Verificar ($r.motivo -like '*PAGO_RECHAZADO*') 'Motivo: pago rechazado'
Verificar ($intentos -eq 1) "Cobro intentado una sola vez (intentos: $intentos)"
Verificar ((Estado-En 'http://localhost:8084/api/pagos' $saga) -eq 'RECHAZADO') 'Pago queda RECHAZADO (sin cobro)'
Verificar ((Estado-En 'http://localhost:8083/api/autos/reservas' $saga) -eq 'CANCELADA') 'Auto compensado'
Verificar ((Estado-En 'http://localhost:8082/api/hoteles/reservas' $saga) -eq 'CANCELADA') 'Hotel compensado'
Verificar ((Estado-En 'http://localhost:8081/api/vuelos/reservas' $saga) -eq 'CANCELADA') 'Vuelo compensado'
exit $Fallos
