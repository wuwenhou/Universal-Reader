# One-command local build: native libraries -> unit tests -> debug APK -> verification.
#
# Usage:
#   .\tools\build-all.ps1
#   .\tools\build-all.ps1 -SkipNative   # when jniLibs are already up to date

[CmdletBinding()]
param(
    [switch]$SkipNative
)

$ErrorActionPreference = 'Stop'

& (Join-Path $PSScriptRoot 'env.ps1') | Out-Host

if (-not $SkipNative) {
    & (Join-Path $PSScriptRoot 'build-native.ps1')
}

$androidDir = Join-Path (Split-Path -Parent $PSScriptRoot) 'android'
Push-Location $androidDir
try {
    & .\gradlew.bat :app:testDebugUnitTest :app:assembleDebug --console=plain
    if ($LASTEXITCODE -ne 0) { throw "Gradle failed with exit code $LASTEXITCODE" }
}
finally {
    Pop-Location
}

& (Join-Path $PSScriptRoot 'verify-apk.ps1')
