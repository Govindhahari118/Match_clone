param(
    [switch]$KeepInfra
)

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

$Root = Split-Path -Parent $PSScriptRoot
$PidsDir = Join-Path $Root "runtime\pids"

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

function Stop-ProcessByPort([int]$port) {
    $listeners = Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue
    foreach ($listener in $listeners) {
        $targetPid = $listener.OwningProcess
        if ($targetPid -and ($targetPid -ne $PID)) {
            try {
                Stop-Process -Id $targetPid -Force -ErrorAction Stop
                Write-Host "Stopped PID $targetPid on port $port."
            } catch {
                Write-Warning "Failed stopping PID ${targetPid} on port ${port}: $($_.Exception.Message)"
            }
        }
    }
}

function Stop-ProcessByPidFile([string]$path) {
    if (-not (Test-Path $path)) {
        return
    }

    $pidValue = Get-Content -Path $path -Raw
    $parsedPid = 0
    if (-not [int]::TryParse($pidValue, [ref]$parsedPid)) {
        Remove-Item $path -Force
        return
    }

    $targetPid = $parsedPid
    $proc = Get-Process -Id $targetPid -ErrorAction SilentlyContinue
    if ($proc) {
        try {
            Stop-Process -Id $targetPid -Force -ErrorAction Stop
            Write-Host "Stopped PID $targetPid from $path."
        } catch {
            Write-Warning "Failed stopping PID ${targetPid} from ${path}: $($_.Exception.Message)"
        }
    }

    Remove-Item $path -Force
}

Write-Host "[1/2] Stopping local dev servers..."
Stop-ProcessByPort -port 4000
Stop-ProcessByPort -port 8000

if (Test-Path $PidsDir) {
    Stop-ProcessByPidFile -path (Join-Path $PidsDir "backend.pid")
    Stop-ProcessByPidFile -path (Join-Path $PidsDir "frontend.pid")
}

if (-not $KeepInfra) {
    Write-Host "[2/2] Stopping postgres and redis containers..."
    Invoke-DockerCompose -arguments @("stop", "postgres", "redis")
} else {
    Write-Host "[2/2] Keeping infrastructure running as requested."
}

Write-Host "Local services stopped."
