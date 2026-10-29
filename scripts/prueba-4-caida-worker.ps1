# Escenario 4: el worker (proceso orquestador) muere a mitad de la saga.
# La saga hace una pausa durable despues de cada paso; se mata el proceso cuando el hotel
# ya esta reservado y se vuelve a levantar. Esperado: la saga continua donde iba, sin
# repetir el vuelo ni el hotel, y termina CONFIRMADA.
. "$PSScriptRoot\comun.ps1"
$Fallos = 0

Write-Host '=== PRUEBA 4: caida del worker a mitad de la saga ===' -ForegroundColor Magenta
$saga = Iniciar-Saga (Nueva-Solicitud -Pausa 8)

do {
    Start-Sleep -Seconds 1
    $paso = (Invoke-RestMethod -Uri "$Orquestador/$saga").paso
    Write-Host "  paso actual: $paso"
} until ($paso -eq 'HOTEL_RESERVADO')

$id = Pid-En-Puerto 8080
Stop-Process -Id $id -Force
Write-Host "`n*** Worker ELIMINADO (PID $id) con la saga en $paso ***" -ForegroundColor Red
Start-Sleep -Seconds 10
Write-Host "Mientras tanto en las bases: vuelo=$(Estado-En 'http://localhost:8081/api/vuelos/reservas' $saga), auto=$(Estado-En 'http://localhost:8083/api/autos/reservas' $saga)"

Write-Host 'Reiniciando el worker...' -ForegroundColor Yellow
# El log nuevo reemplaza al anterior: solo contendra lo que ejecute el worker reiniciado
Iniciar-Orquestador | Out-Null
Esperar-Puerto 8080

$r = Esperar-Resultado $saga
Mostrar-Historial $saga
Mostrar-Bases $saga

$log = Join-Path $Raiz 'logs\orquestador.log'
$repetidos = @(Select-String -Path $log -Pattern "\[$saga\] reservar(Vuelo|Hotel)").Count
$continuados = @(Select-String -Path $log -Pattern "\[$saga\] (reservarAuto|cobrarPago)").Count

Write-Host "`nVerificaciones:"
Verificar ($r.estado -eq 'CONFIRMADA') 'Saga CONFIRMADA tras reiniciar el worker'
Verificar ($repetidos -eq 0) 'El worker nuevo NO repitio vuelo ni hotel (replay del historial)'
Verificar ($continuados -ge 2) 'El worker nuevo continuo con auto y pago'
Verificar (@(Filas-De 'http://localhost:8081/api/vuelos/reservas' $saga).Count -eq 1) 'Una sola reserva de vuelo'
Verificar ((Estado-En 'http://localhost:8084/api/pagos' $saga) -eq 'APROBADO') 'Un unico cobro APROBADO'
exit $Fallos
