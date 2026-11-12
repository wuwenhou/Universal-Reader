# Verifies the debug APK contains the expected native libraries and JNI symbol.
#
# Usage:
#   .\tools\verify-apk.ps1 [-Apk <path>]

[CmdletBinding()]
param(
    [string]$Apk = ''
)

$ErrorActionPreference = 'Stop'
& (Join-Path $PSScriptRoot 'env.ps1') | Out-Null

$repoRoot = Split-Path -Parent $PSScriptRoot
if (-not $Apk) {
    $Apk = Join-Path $repoRoot 'android\app\build\outputs\apk\debug\app-debug.apk'
}
if (-not (Test-Path $Apk)) { throw "APK not found: $Apk" }

Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [System.IO.Compression.ZipFile]::OpenRead($Apk)
try {
    $nativeEntries = @($zip.Entries | Where-Object { $_.FullName -like 'lib/*/libreader_core.so' })
}
finally {
    $zip.Dispose()
}

$expected = @('lib/arm64-v8a/libreader_core.so', 'lib/x86_64/libreader_core.so')
foreach ($entry in $expected) {
    if (-not ($nativeEntries.FullName -contains $entry)) {
        throw "Missing $entry in $Apk"
    }
}

$ndkBin = Join-Path $env:ANDROID_NDK_HOME 'toolchains\llvm\prebuilt\windows-x86_64\bin'
$llvmNm = Join-Path $ndkBin 'llvm-nm.exe'
$llvmReadElf = Join-Path $ndkBin 'llvm-readelf.exe'
$expectedSymbols = @(
    'Java_com_universalreader_app_core_NativeBridge_coreVersion',
    'Java_com_universalreader_app_core_NativeBridge_normalizeParagraphs',
    'Java_com_universalreader_app_core_NativeBridge_ttsChunks',
    'Java_com_universalreader_app_core_NativeBridge_canonicalizeUrl'
)

$extractRoot = Join-Path ([System.IO.Path]::GetTempPath()) "universal-reader-apk-$PID"
if (Test-Path $extractRoot) { Remove-Item -Recurse -Force $extractRoot }
New-Item -ItemType Directory -Force $extractRoot | Out-Null

try {
    [System.IO.Compression.ZipFile]::ExtractToDirectory($Apk, $extractRoot)
    foreach ($entry in $expected) {
        $so = Join-Path $extractRoot ($entry -replace '/', '\')
        if (Test-Path $llvmNm) {
            $symbols = & $llvmNm -D --defined-only $so
            foreach ($expectedSymbol in $expectedSymbols) {
                if (-not ($symbols -match [regex]::Escape($expectedSymbol))) {
                    throw "JNI symbol $expectedSymbol missing from $entry"
                }
            }
        }
        if (Test-Path $llvmReadElf) {
            $loads = & $llvmReadElf -l $so | Select-String 'LOAD'
            if (-not ($loads -match '0x4000')) {
                throw "16 KB page alignment missing from $entry"
            }
        }
        Write-Host "OK  $entry"
    }
}
finally {
    Remove-Item -Recurse -Force $extractRoot -ErrorAction SilentlyContinue
}

Write-Host "APK OK: $Apk"
