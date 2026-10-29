# Compila (si hace falta) y levanta los cinco microservicios y el orquestador.
# Requisito: el servidor de desarrollo de Temporal ya corriendo (temporal server start-dev).
param([switch]$Compilar)

. "$PSScriptRoot\comun.ps1"

if (-not (Pid-En-Puerto 7233)) {
    Write-Warning 'No se detecta Temporal en localhost:7233. Ejecute antes: temporal server start-dev'
}

if ($Compilar -or -not (Test-Path (Join-Path $Raiz 'orquestador\target\orquestador-1.0.0.jar'))) {
    Push-Location $Raiz
    mvn -q -B package -DskipTests
    Pop-Location
}

New-Item -ItemType Directory -Force (Join-Path $Raiz 'logs') | Out-Null

foreach ($modulo in $Servicios.Keys) {
    $puerto = $Servicios[$modulo]
    if (Pid-En-Puerto $puerto) {
        Write-Host "$modulo ya esta corriendo en el puerto $puerto"
        continue
    }
    if ($modulo -eq 'orquestador') {
        Iniciar-Orquestador | Out-Null
    } else {
        $jar = Join-Path $Raiz "$modulo\target\$modulo-1.0.0.jar"
        Start-Process -FilePath 'java' -ArgumentList '-jar', "`"$jar`"" `
            -WorkingDirectory (Join-Path $Raiz $modulo) -WindowStyle Minimized `
            -RedirectStandardOutput (Join-Path $Raiz "logs\$modulo.log") `
            -RedirectStandardError (Join-Path $Raiz "logs\$modulo.err.log") | Out-Null
    }
    Write-Host "Iniciando $modulo en el puerto $puerto..."
}

foreach ($modulo in $Servicios.Keys) {
    Esperar-Puerto $Servicios[$modulo]
    Write-Host "  $modulo listo" -ForegroundColor Green
}
Write-Host "`nTodo arriba. Temporal UI: http://localhost:8233  |  Logs en .\logs" -ForegroundColor Cyan
