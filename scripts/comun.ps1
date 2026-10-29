# Funciones compartidas por los scripts de prueba.
$ErrorActionPreference = 'Stop'

$Raiz = Split-Path -Parent $PSScriptRoot
$Orquestador = 'http://localhost:8080/api/reservas'

$Servicios = [ordered]@{
    'servicio-vuelos'         = 8081
    'servicio-hoteles'        = 8082
    'servicio-autos'          = 8083
    'servicio-pagos'          = 8084
    'servicio-notificaciones' = 8085
    'orquestador'             = 8080
}

function Nueva-Solicitud {
    param(
        [string]$Hotel = 'HOTEL-CENTRO',
        [string]$Tarjeta = '4111111111111111',
        [int]$Pausa = 0
    )
    @{
        cliente                 = 'Ana Torres'
        email                   = 'ana@correo.com'
        origen                  = 'PPN'
        destino                 = 'CTG'
        fechaIda                = '2026-12-01'
        fechaRegreso            = '2026-12-05'
        codigoHotel             = $Hotel
        numeroTarjeta           = $Tarjeta
        monto                   = 2500000
        pausaEntrePasosSegundos = $Pausa
    }
}

function Iniciar-Saga($Solicitud) {
    $r = Invoke-RestMethod -Method Post -Uri $Orquestador -ContentType 'application/json' `
        -Body ($Solicitud | ConvertTo-Json)
    Write-Host "Saga iniciada: $($r.sagaId)" -ForegroundColor Cyan
    $r.sagaId
}

function Esperar-Resultado([string]$SagaId, [int]$Segundos = 120) {
    $limite = (Get-Date).AddSeconds($Segundos)
    while ((Get-Date) -lt $limite) {
        try {
            $r = Invoke-WebRequest -UseBasicParsing -Uri "$Orquestador/$SagaId/resultado?segundos=20"
            if ($r.StatusCode -eq 200) { return ($r.Content | ConvertFrom-Json) }
        } catch {
            Start-Sleep -Seconds 2   # orquestador reiniciando
        }
    }
    throw "La saga $SagaId no termino en $Segundos s"
}

function Mostrar-Historial([string]$SagaId) {
    $e = Invoke-RestMethod -Uri "$Orquestador/$SagaId"
    Write-Host "`nHistorial de la saga:" -ForegroundColor Yellow
    $e.historial | ForEach-Object { Write-Host "  - $_" }
}

# Muestra lo que quedo en la base de datos H2 de cada microservicio para esta saga
function Mostrar-Bases([string]$SagaId) {
    Write-Host "`nEstado en cada base de datos:" -ForegroundColor Yellow
    $consultas = [ordered]@{
        'Vuelos'         = 'http://localhost:8081/api/vuelos/reservas'
        'Hoteles'        = 'http://localhost:8082/api/hoteles/reservas'
        'Autos'          = 'http://localhost:8083/api/autos/reservas'
        'Pagos'          = 'http://localhost:8084/api/pagos'
        'Notificaciones' = 'http://localhost:8085/api/notificaciones'
    }
    foreach ($nombre in $consultas.Keys) {
        $filas = @(Filas-De $consultas[$nombre] $SagaId)
        if (-not $filas) {
            Write-Host ("  {0,-15}: (sin registros)" -f $nombre)
            continue
        }
        foreach ($f in $filas) {
            $detalle = if ($f.tipo) { $f.tipo } elseif ($f.motivo) { "$($f.estado) ($($f.motivo))" } else { $f.estado }
            Write-Host ("  {0,-15}: {1}" -f $nombre, $detalle)
        }
    }
}

function Verificar([bool]$Condicion, [string]$Mensaje) {
    if ($Condicion) {
        Write-Host "  [OK]    $Mensaje" -ForegroundColor Green
    } else {
        Write-Host "  [FALLO] $Mensaje" -ForegroundColor Red
        $script:Fallos++
    }
}

# Filas de un listado REST que pertenecen a la saga. En PowerShell 5.1 Invoke-RestMethod
# entrega el arreglo JSON como un solo objeto: se asigna a una variable para desenrollarlo.
function Filas-De([string]$Url, [string]$SagaId) {
    $todas = Invoke-RestMethod -Uri $Url
    $todas | Where-Object { $_.sagaId -eq $SagaId }
}

function Estado-En([string]$Url, [string]$SagaId) {
    (@(Filas-De $Url $SagaId) | Select-Object -First 1).estado
}

function Iniciar-Orquestador {
    $jar = Join-Path $Raiz 'orquestador\target\orquestador-1.0.0.jar'
    Start-Process -FilePath 'java' -ArgumentList '-jar', "`"$jar`"" `
        -WorkingDirectory (Join-Path $Raiz 'orquestador') -WindowStyle Minimized `
        -RedirectStandardOutput (Join-Path $Raiz 'logs\orquestador.log') `
        -RedirectStandardError (Join-Path $Raiz 'logs\orquestador.err.log') -PassThru
}

function Esperar-Puerto([int]$Puerto, [int]$Segundos = 90) {
    $limite = (Get-Date).AddSeconds($Segundos)
    while ((Get-Date) -lt $limite) {
        try {
            Invoke-WebRequest -UseBasicParsing -Uri "http://localhost:$Puerto/" -TimeoutSec 2 | Out-Null
            return
        } catch {
            if ($_.Exception.Response) { return }   # responde (aunque sea 404): ya esta arriba
            Start-Sleep -Milliseconds 500
        }
    }
    throw "Nada escucha en el puerto $Puerto"
}

function Pid-En-Puerto([int]$Puerto) {
    (Get-NetTCPConnection -LocalPort $Puerto -State Listen -ErrorAction SilentlyContinue |
        Select-Object -First 1).OwningProcess
}
