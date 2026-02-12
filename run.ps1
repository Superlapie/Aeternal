param(
    [ValidateSet("server", "client", "all", "stop")]
    [string]$Mode = "all"
)

$ErrorActionPreference = "Stop"

$root = $PSScriptRoot
$serverDir = Join-Path $root "server"
$clientDir = Join-Path $root "client"
$serverArgs = @(":game:run", "-x", ":plugin:compileKotlin", "-x", ":plugin:jar", "--console", "plain")

function Stop-RspsPorts {
    $patterns = @(":43595", ":43580")
    $matches = netstat -ano | Select-String ($patterns -join "|")
    $pids = @()
    foreach ($line in $matches) {
        $parts = ($line.ToString() -split "\s+") | Where-Object { $_ -ne "" }
        if ($parts.Count -ge 5) {
            $pid = $parts[$parts.Count - 1]
            if ($pid -match "^\d+$" -and $pid -ne "0") {
                $pids += [int]$pid
            }
        }
    }
    $pids | Sort-Object -Unique | ForEach-Object {
        Stop-Process -Id $_ -Force -ErrorAction SilentlyContinue
    }
}

function Invoke-Gradle {
    param(
        [Parameter(Mandatory = $true)][string]$WorkingDirectory,
        [Parameter(Mandatory = $true)][string[]]$Arguments
    )

    Push-Location $WorkingDirectory
    try {
        & ".\gradlew.bat" @Arguments
    } finally {
        Pop-Location
    }
}

switch ($Mode) {
    "server" {
        Invoke-Gradle -WorkingDirectory $serverDir -Arguments $serverArgs
    }
    "client" {
        Invoke-Gradle -WorkingDirectory $clientDir -Arguments @("run", "--console", "plain")
    }
    "all" {
        Start-Process -FilePath ".\gradlew.bat" -ArgumentList $serverArgs -WorkingDirectory $serverDir | Out-Null
        Start-Sleep -Seconds 4
        Invoke-Gradle -WorkingDirectory $clientDir -Arguments @("run", "--console", "plain")
    }
    "stop" {
        Invoke-Gradle -WorkingDirectory $serverDir -Arguments @("--stop")
        Invoke-Gradle -WorkingDirectory $clientDir -Arguments @("--stop")
        Stop-RspsPorts
    }
}
