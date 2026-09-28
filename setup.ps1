$ErrorActionPreference = 'Stop'
$projectRoot = $PSScriptRoot

if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
    throw 'Install Docker Desktop and start it in Linux containers mode first.'
}

Push-Location $projectRoot
try {
    docker info --format '{{.OSType}}'
    if ($LASTEXITCODE -ne 0) {
        throw 'Docker is unavailable. Start Docker Desktop with Linux containers enabled.'
    }

    docker compose version
    if ($LASTEXITCODE -ne 0) {
        throw 'Docker Compose is unavailable.'
    }

    if (-not (Test-Path -LiteralPath '.local/compose.yml')) {
        $volumes = docker volume ls --filter 'name=^bloom_postgres-data$' --format '{{.Name}}'
        if ($LASTEXITCODE -ne 0) {
            throw 'Cannot check existing database volumes.'
        }
        if ($volumes -contains 'bloom_postgres-data') {
            throw 'An existing Bloom database was found. Restore its matching .local configuration or continue using its original configuration. New passwords were not generated.'
        }
    }

    docker build -t bloom-local-setup:local infrastructure/setup
    if ($LASTEXITCODE -ne 0) {
        throw 'Could not build the configuration generator.'
    }

    docker run --rm --mount "type=bind,source=$projectRoot,target=/workspace" bloom-local-setup:local
    if ($LASTEXITCODE -ne 0) {
        throw 'Configuration generation failed. Existing credentials were not intentionally replaced.'
    }

    docker compose -f compose.yml -f .local/compose.yml config --quiet
    if ($LASTEXITCODE -ne 0) {
        throw 'Generated Compose configuration did not pass validation.'
    }

    Write-Host ''
    Write-Host 'Ready. Run from the Bloom directory:'
    Write-Host 'docker compose -f compose.yml -f .local/compose.yml up -d --build --wait --wait-timeout 240'
    Write-Host 'Keep .local between restarts. Do not commit or share its credentials.'
}
finally {
    Pop-Location
}
