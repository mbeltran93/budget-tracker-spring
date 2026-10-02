# Levanta un Postgres descartable con docker, corre los tests de Maven (unitarios +
# integracion con @SpringBootTest/MockMvc) y lo apaga al terminar, pase lo que pase.
$ErrorActionPreference = "Stop"

$ContainerName = "budget-tracker-test-db"
$DbPort = if ($env:TEST_DB_PORT) { $env:TEST_DB_PORT } else { "55432" }
$DbName = if ($env:TEST_DB_NAME) { $env:TEST_DB_NAME } else { "budget_tracker_test" }
$DbUser = if ($env:TEST_DB_USER) { $env:TEST_DB_USER } else { "test_user" }
$DbPassword = if ($env:TEST_DB_PASSWORD) { $env:TEST_DB_PASSWORD } else { "test_pass" }

function Cleanup {
    docker rm -f $ContainerName 2>$null | Out-Null
}

Cleanup

try {
    Write-Host "Levantando Postgres de test en el puerto $DbPort..."
    docker run -d --rm `
        --name $ContainerName `
        -e POSTGRES_DB=$DbName `
        -e POSTGRES_USER=$DbUser `
        -e POSTGRES_PASSWORD=$DbPassword `
        -p "${DbPort}:5432" `
        postgres:16-alpine | Out-Null

    Write-Host "Esperando a que Postgres este listo..."
    $ready = $false
    for ($i = 0; $i -lt 30; $i++) {
        docker exec $ContainerName pg_isready -U $DbUser -d $DbName 2>$null | Out-Null
        if ($?) { $ready = $true; break }
        Start-Sleep -Seconds 1
    }
    if (-not $ready) { throw "Postgres no quedo listo a tiempo" }
    Write-Host "Postgres listo."

    $env:TEST_DB_PORT = $DbPort
    $env:TEST_DB_NAME = $DbName
    $env:TEST_DB_USER = $DbUser
    $env:TEST_DB_PASSWORD = $DbPassword

    Push-Location (Join-Path $PSScriptRoot "..")
    mvn test
}
finally {
    Pop-Location -ErrorAction SilentlyContinue
    Cleanup
}
