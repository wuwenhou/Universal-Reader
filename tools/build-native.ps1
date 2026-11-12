# Builds the Rust reader-core native libraries into android/app/src/main/jniLibs.
#
# Usage (PowerShell):
#   .\tools\build-native.ps1                 # release, arm64-v8a + x86_64
#   .\tools\build-native.ps1 -Profile debug
#
# On Windows the default host toolchain is the GNU one because it only needs
# MinGW (installed by tools/env.ps1) instead of the Visual C++ build tools.

[CmdletBinding()]
param(
    [ValidateSet('debug', 'release')]
    [string]$Profile = 'release',

    [ValidateSet('arm64-v8a', 'x86_64')]
    [string[]]$Abi = @('arm64-v8a', 'x86_64'),

    [string]$Toolchain = 'stable-x86_64-pc-windows-gnu'
)

$ErrorActionPreference = 'Stop'

& (Join-Path $PSScriptRoot 'env.ps1') | Out-Host

$repoRoot = Split-Path -Parent $PSScriptRoot
$rustDir = Join-Path $repoRoot 'rust'
$outputDir = Join-Path $repoRoot 'android\app\src\main\jniLibs'

$cargoArgs = @("+$Toolchain", 'ndk', '-P', '26')
foreach ($target in $Abi) { $cargoArgs += @('-t', $target) }
$cargoArgs += @('-o', $outputDir, 'build')
if ($Profile -eq 'release') { $cargoArgs += '--release' }

Push-Location $rustDir
try {
    Write-Host "cargo $($cargoArgs -join ' ')"
    & cargo @cargoArgs
    if ($LASTEXITCODE -ne 0) { throw "cargo ndk failed with exit code $LASTEXITCODE" }
}
finally {
    Pop-Location
}

$ndkBin = Join-Path $env:ANDROID_NDK_HOME 'toolchains\llvm\prebuilt\windows-x86_64\bin'
$llvmNm = Join-Path $ndkBin 'llvm-nm.exe'
$llvmReadElf = Join-Path $ndkBin 'llvm-readelf.exe'
$expectedSymbol = 'Java_com_universalreader_app_core_NativeBridge_coreVersion'

foreach ($target in $Abi) {
    $so = Join-Path $outputDir "$target\libreader_core.so"
    if (-not (Test-Path $so)) { throw "Missing native library: $so" }

    if (Test-Path $llvmNm) {
        $symbols = & $llvmNm -D --defined-only $so
        if (-not ($symbols -match [regex]::Escape($expectedSymbol))) {
            throw "JNI symbol $expectedSymbol not found in $so"
        }
    }

    if (Test-Path $llvmReadElf) {
        $loads = & $llvmReadElf -l $so | Select-String 'LOAD'
        if (-not ($loads -match '0x4000')) {
            Write-Warning "16 KB page alignment not detected in $so; check NDK/linker flags before release."
        }
    }

    Write-Host ("built {0} ({1:N1} KB)" -f $so, ((Get-Item $so).Length / 1KB))
}
