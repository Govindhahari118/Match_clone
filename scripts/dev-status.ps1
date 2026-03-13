$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

function Test-DockerEngine {
    if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
        return $false
    }
    $savedPreference = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    docker info *> $null
    $exitCode = $LASTEXITCODE
    $ErrorActionPreference = $savedPreference
    return ($exitCode -eq 0)
}

function Test-Url([string]$url) {
    try {
        $response = Invoke-WebRequest -Uri $url -UseBasicParsing -TimeoutSec 8
        return $response.StatusCode
    } catch {
        return $null
    }
}

function Process-OnPort([int]$port) {
    $listeners = Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue
    if (-not $listeners) {
        return $null
    }

    $procPid = $listeners[0].OwningProcess
    $proc = Get-Process -Id $procPid -ErrorAction SilentlyContinue
    if ($proc) {
        return "$($proc.ProcessName) (PID $procPid)"
    }

    return "PID $procPid"
}

$backendProc = Process-OnPort -port 4000
$frontendProc = Process-OnPort -port 8000
$backendHealth = Test-Url "http://localhost:4000/health"
$frontendHealth = Test-Url "http://localhost:8000"
$dockerAvailable = Test-DockerEngine
$containers = $null
if ($dockerAvailable) {
    $containers = docker ps --filter "name=matrimony-postgres" --filter "name=matrimony-redis" --format "{{.Names}} | {{.Status}}"
}

Write-Host "Frontend:"
if ($frontendProc) {
    Write-Host "  Port 8000: up ($frontendProc), HTTP $frontendHealth"
} else {
    Write-Host "  Port 8000: down"
}

Write-Host "Backend:"
if ($backendProc) {
    Write-Host "  Port 4000: up ($backendProc), HTTP $backendHealth"
} else {
    Write-Host "  Port 4000: down"
}

Write-Host "Infrastructure:"
if (-not $dockerAvailable) {
    Write-Host "  Docker not available; skipping container status"
} elseif ($containers) {
    $containers | ForEach-Object { Write-Host "  $_" }
} else {
    Write-Host "  postgres/redis containers not running"
}
