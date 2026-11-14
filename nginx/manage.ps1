param(
    [ValidateSet('Validate', 'Start', 'Reload', 'Stop', 'Status')]
    [string]$Action = 'Status',
    [string]$NginxExe = 'D:\develop\nginx-1.22.0-web\nginx.exe'
)

$ErrorActionPreference = 'Stop'
$nginxPrefix = $PSScriptRoot.Replace('\', '/') + '/'
$pidFile = Join-Path $PSScriptRoot 'logs/nginx.pid'

if (-not (Test-Path -LiteralPath $NginxExe -PathType Leaf)) {
    throw "Nginx executable not found: $NginxExe"
}
foreach ($directory in @('logs', 'temp')) {
    New-Item -ItemType Directory -Path (Join-Path $PSScriptRoot $directory) -Force | Out-Null
}

function Get-ProjectNginxProcess {
    if (Test-Path -LiteralPath $pidFile) {
        $pidText = Get-Content -LiteralPath $pidFile -Raw
        if ([string]::IsNullOrWhiteSpace($pidText)) { return }
        $nginxProcessId = [int]$pidText.Trim()
        return Get-Process -Id $nginxProcessId -ErrorAction SilentlyContinue |
            Where-Object ProcessName -EQ 'nginx'
    }
}

if ($Action -eq 'Status') {
    $nginxProcess = Get-ProjectNginxProcess
    if ($nginxProcess) {
        Write-Output "ZeroX Nginx is running. Master PID: $($nginxProcess.Id)"
    } else {
        Write-Output 'ZeroX Nginx is not running.'
    }
    return
}

if ($Action -eq 'Stop') {
    if (-not (Get-ProjectNginxProcess)) {
        Write-Output 'ZeroX Nginx is not running.'
        return
    }
    & $NginxExe -p $nginxPrefix -c conf/nginx.conf -s quit
    if ($LASTEXITCODE -ne 0) { throw 'Nginx graceful shutdown failed.' }
    return
}

& $NginxExe -p $nginxPrefix -c conf/nginx.conf -t
if ($LASTEXITCODE -ne 0) { throw 'Nginx configuration validation failed.' }
if ($Action -eq 'Validate') { return }

if ($Action -eq 'Reload') {
    if (-not (Get-ProjectNginxProcess)) { throw 'Start ZeroX Nginx before reloading.' }
    & $NginxExe -p $nginxPrefix -c conf/nginx.conf -s reload
    if ($LASTEXITCODE -ne 0) { throw 'Nginx reload failed.' }
    return
}

if (Get-ProjectNginxProcess) {
    Write-Output 'ZeroX Nginx is already running. Use -Action Reload after configuration changes.'
    return
}
Start-Process -FilePath $NginxExe -WorkingDirectory $PSScriptRoot -WindowStyle Hidden `
    -ArgumentList @('-p', ('"{0}"' -f $nginxPrefix), '-c', 'conf/nginx.conf') | Out-Null
Write-Output 'ZeroX Nginx launch requested. Use -Action Status and open the deployed URL to verify.'
