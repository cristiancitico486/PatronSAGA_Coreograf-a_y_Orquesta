# Escenario 1: el hotel no tiene cupo (error de negocio en el 2.o paso).
# Esperado: no se reintenta, se cancela el vuelo ya reservado, nunca se cobra.
. "$PSScriptRoot\comun.ps1"
$Fallos = 0

Write-Host '=== PRUEBA 1: hotel sin cupo ===' -ForegroundColor Magenta
$saga = Iniciar-Saga (Nueva-Solicitud -Hotel 'HOTEL-LLENO')
$r = Esperar-Resultado $saga
Mostrar-Historial $saga
Mostrar-Bases $saga

Write-Host "`nVerificaciones:"
Verificar ($r.estado -eq 'CANCELADA') "Saga CANCELADA ($($r.motivo))"
Verificar ($r.motivo -like '*HOTEL_SIN_CUPO*') 'Motivo: hotel sin cupo'
Verificar ((Estado-En 'http://localhost:8081/api/vuelos/reservas' $saga) -eq 'CANCELADA') 'Vuelo compensado (sin reserva huerfana)'
Verificar (-not (Estado-En 'http://localhost:8083/api/autos/reservas' $saga)) 'Nunca se reservo auto'
Verificar (-not (Estado-En 'http://localhost:8084/api/pagos' $saga)) 'Nunca se cobro'
exit $Fallos
