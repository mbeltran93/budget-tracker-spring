# Unico comando para correr la suite de Playwright de punta a punta:
# levanta app + Postgres con docker compose (build incluido), espera a que el
# login responda, instala los browsers de Playwright si hace falta, corre los
# tests, y al final apaga y borra los contenedores + el volumen, pase lo que pase.
$ErrorActionPreference = "Stop"

$RootDir = Join-Path $PSScriptRoot ".."
Push-Location $RootDir

function Cleanup {
    Write-Host "Apagando app + base de datos..."
    docker compose down -v 2>$null | Out-Null
}

try {
    Write-Host "Levantando app + Postgres (docker compose up --build)..."
    docker compose up -d --build

    $BaseUrl = if ($env:BASE_URL) { $env:BASE_URL } else { "http://localhost:8080" }
    Write-Host "Esperando a que la app responda en $BaseUrl/login..."
    $ready = $false
    for ($i = 0; $i -lt 60; $i++) {
        try {
            $response = Invoke-WebRequest -Uri "$BaseUrl/login" -UseBasicParsing -TimeoutSec 2
            if ($response.StatusCode -eq 200) { $ready = $true; break }
        } catch {}
        Start-Sleep -Seconds 1
    }
    if (-not $ready) { throw "La app no respondio a tiempo" }
    Write-Host "App lista."

    Push-Location (Join-Path $RootDir "e2e")
    if (-not (Test-Path node_modules)) {
        Write-Host "Instalando dependencias de Playwright..."
        npm install
    }
    npx playwright install --with-deps chromium

    $env:BASE_URL = $BaseUrl
    npx playwright test
    Pop-Location
}
finally {
    Cleanup
    Pop-Location -ErrorAction SilentlyContinue
}
