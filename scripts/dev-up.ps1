param(
    [switch]$Install
)

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

$Root = Split-Path -Parent $PSScriptRoot
$BackendDir = Join-Path $Root "backend"
$FrontendDir = Join-Path $Root "frontend"
$RuntimeDir = Join-Path $Root "runtime"
$LogsDir = Join-Path $RuntimeDir "logs"
$PidsDir = Join-Path $RuntimeDir "pids"

New-Item -ItemType Directory -Force -Path $LogsDir | Out-Null
New-Item -ItemType Directory -Force -Path $PidsDir | Out-Null

function Assert-LastExit([string]$errorMessage) {
    if ($LASTEXITCODE -ne 0) {
        throw $errorMessage
    }
}

function Invoke-DockerCompose([string[]]$arguments) {
    $savedPreference = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    & docker compose @arguments 2>$null | Out-Null
    $exitCode = $LASTEXITCODE
    $ErrorActionPreference = $savedPreference

    if ($exitCode -ne 0) {
        throw "docker compose $($arguments -join ' ') failed with exit code $exitCode."
    }
}

function Ensure-File([string]$target, [string]$source) {
    if (-not (Test-Path $target)) {
        if (-not (Test-Path $source)) {
            throw "Missing template file: $source"
        }
        Copy-Item $source $target
        Write-Host "Created $target from template."
    }
}

function Test-Url([string]$url) {
    try {
        $response = Invoke-WebRequest -Uri $url -UseBasicParsing -TimeoutSec 10
        return $response.StatusCode
    } catch {
        return $null
    }
}

function Assert-PortFree([int]$port) {
    $listeners = Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue
    if ($listeners) {
        $ownerPid = $listeners[0].OwningProcess
        throw "Port $port is already in use by PID $ownerPid. Run 'npm run dev:down' first."
    }
}

Write-Host "[1/6] Checking Docker engine..."
$savedPreference = $ErrorActionPreference
$ErrorActionPreference = "Continue"
docker info *> $null
$dockerExitCode = $LASTEXITCODE
$ErrorActionPreference = $savedPreference
if ($dockerExitCode -ne 0) {
    throw "Docker Desktop is not running. Start Docker and retry."
}

Write-Host "[2/6] Starting infrastructure containers..."
Invoke-DockerCompose -arguments @("up", "-d", "postgres", "redis")

Write-Host "[3/6] Waiting for postgres and redis health..."
$deadline = (Get-Date).AddMinutes(3)
$healthy = $false
do {
    $statuses = docker ps --format "{{.Names}}|{{.Status}}"
    $postgresHealthy = $statuses -match "^matrimony-postgres\|Up .*\(healthy\)"
    $redisHealthy = $statuses -match "^matrimony-redis\|Up .*\(healthy\)"
    if ($postgresHealthy -and $redisHealthy) {
        $healthy = $true
        break
    }
    Start-Sleep -Seconds 3
} while ((Get-Date) -lt $deadline)

if (-not $healthy) {
    throw "Containers did not reach healthy state within timeout."
}

Write-Host "[4/6] Ensuring environment files..."
Ensure-File -target (Join-Path $BackendDir ".env") -source (Join-Path $BackendDir ".env.example")
Ensure-File -target (Join-Path $FrontendDir ".env.local") -source (Join-Path $FrontendDir ".env.local.example")

if ($Install -or -not (Test-Path (Join-Path $BackendDir "node_modules"))) {
    Write-Host "[5/6] Installing backend dependencies..."
    Push-Location $BackendDir
    try {
        & npm.cmd install
        Assert-LastExit "Backend dependency install failed."
    } finally {
        Pop-Location
    }
}

if ($Install -or -not (Test-Path (Join-Path $FrontendDir "node_modules"))) {
    Write-Host "[5/6] Installing frontend dependencies..."
    Push-Location $FrontendDir
    try {
        & npm.cmd install
        Assert-LastExit "Frontend dependency install failed."
    } finally {
        Pop-Location
    }
}

Write-Host "[5/6] Syncing Prisma schema..."
Push-Location $BackendDir
try {
    & npx.cmd prisma generate
    Assert-LastExit "Prisma generate failed."
    & npx.cmd prisma db push
    Assert-LastExit "Prisma db push failed."
} finally {
    Pop-Location
}

Write-Host "[6/6] Starting backend and frontend dev servers..."
Assert-PortFree -port 4000
Assert-PortFree -port 8000

$backendOut = Join-Path $LogsDir "backend.out.log"
$backendErr = Join-Path $LogsDir "backend.err.log"
$frontendOut = Join-Path $LogsDir "frontend.out.log"
$frontendErr = Join-Path $LogsDir "frontend.err.log"

$backendProcess = Start-Process -FilePath "npm.cmd" -ArgumentList "run", "dev" -WorkingDirectory $BackendDir -RedirectStandardOutput $backendOut -RedirectStandardError $backendErr -PassThru
$frontendProcess = Start-Process -FilePath "npm.cmd" -ArgumentList "run", "dev" -WorkingDirectory $FrontendDir -RedirectStandardOutput $frontendOut -RedirectStandardError $frontendErr -PassThru

Set-Content -Path (Join-Path $PidsDir "backend.pid") -Value $backendProcess.Id -NoNewline
Set-Content -Path (Join-Path $PidsDir "frontend.pid") -Value $frontendProcess.Id -NoNewline

Start-Sleep -Seconds 7
$backendStatus = Test-Url "http://localhost:4000/health"
$frontendStatus = Test-Url "http://localhost:8000"

Write-Host ""
Write-Host "Backend URL:  http://localhost:4000/health (status: $backendStatus)"
Write-Host "Frontend URL: http://localhost:8000 (status: $frontendStatus)"
Write-Host "Backend PID:  $($backendProcess.Id)"
Write-Host "Frontend PID: $($frontendProcess.Id)"
Write-Host "Logs: $LogsDir"

if (($backendStatus -eq 200) -and ($frontendStatus -eq 200)) {
    Write-Host "Local stack is running." -ForegroundColor Green
} else {
    Write-Warning "Stack started but one or more health checks failed. Check logs in $LogsDir."
}
