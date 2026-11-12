# Sets up the local toolchain for Universal Reader.
#
# Usage (PowerShell):
#   . .\tools\env.ps1
#
# The script only affects the current PowerShell session and never overwrites
# values that are already configured.

$ErrorActionPreference = 'Stop'

function First-ExistingPath {
    param([string[]]$Candidates)
    foreach ($candidate in $Candidates) {
        if ($candidate -and (Test-Path $candidate)) { return $candidate }
    }
    return $null
}

if (-not $env:JAVA_HOME) {
    $env:JAVA_HOME = First-ExistingPath @(
        $env:UR_JAVA_HOME,
        'E:\VScode_Opencode\Tools\jdk',
        (Join-Path $env:LOCALAPPDATA 'Programs\Android Studio\jbr'),
        'C:\Program Files\Android\Android Studio\jbr'
    )
}

if (-not $env:ANDROID_HOME) {
    $env:ANDROID_HOME = First-ExistingPath @(
        $env:ANDROID_SDK_ROOT,
        $env:UR_ANDROID_SDK,
        (Join-Path $env:LOCALAPPDATA 'Android\Sdk'),
        (Join-Path $env:USERPROFILE 'android-sdk')
    )
}

if ($env:ANDROID_HOME) {
    $env:ANDROID_SDK_ROOT = $env:ANDROID_HOME
    if (-not $env:ANDROID_NDK_HOME) {
        $ndk = Get-ChildItem (Join-Path $env:ANDROID_HOME 'ndk') -Directory -ErrorAction SilentlyContinue |
            Sort-Object Name -Descending | Select-Object -First 1
        if ($ndk) { $env:ANDROID_NDK_HOME = $ndk.FullName }
    }
}

$mingwBin = First-ExistingPath @(
    $env:UR_MINGW_BIN,
    'E:\VScode_Opencode\Tools\mingw64\mingw64\bin',
    'C:\msys64\mingw64\bin'
)
if ($mingwBin -and (($env:Path -split ';') -notcontains $mingwBin)) {
    $env:Path = "$mingwBin;$env:Path"
}

if ($env:JAVA_HOME) {
    $javaBin = Join-Path $env:JAVA_HOME 'bin'
    if (($env:Path -split ';') -notcontains $javaBin) { $env:Path = "$javaBin;$env:Path" }
}

if ($env:ANDROID_HOME) {
    $platformTools = Join-Path $env:ANDROID_HOME 'platform-tools'
    if (($env:Path -split ';') -notcontains $platformTools) { $env:Path = "$platformTools;$env:Path" }
}

Write-Host "JAVA_HOME       = $env:JAVA_HOME"
Write-Host "ANDROID_HOME    = $env:ANDROID_HOME"
Write-Host "ANDROID_NDK_HOME= $env:ANDROID_NDK_HOME"
Write-Host "mingw           = $mingwBin"
