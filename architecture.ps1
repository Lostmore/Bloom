$ErrorActionPreference = 'Stop'
Push-Location $PSScriptRoot
try {
    if (-not (Get-Command python -ErrorAction SilentlyContinue)) {
        throw 'Install Python 3.11 or newer, then run: python -m pip install PyYAML==6.0.3'
    }
    python -c 'import yaml'
    if ($LASTEXITCODE -ne 0) {
        throw 'Install the generator dependency: python -m pip install PyYAML==6.0.3'
    }
    python infrastructure/architecture/serve.py --open
    if ($LASTEXITCODE -ne 0) {
        throw 'Architecture server stopped with an error. See the output above.'
    }
}
finally {
    Pop-Location
}
