# Detiene los microservicios y el orquestador (no detiene el servidor de Temporal).
. "$PSScriptRoot\comun.ps1"

foreach ($modulo in $Servicios.Keys) {
    $id = Pid-En-Puerto $Servicios[$modulo]
    if ($id) {
        Stop-Process -Id $id -Force
        Write-Host "Detenido $modulo (PID $id)"
    }
}
