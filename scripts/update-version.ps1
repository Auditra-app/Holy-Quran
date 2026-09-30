<#
.SYNOPSIS
    Master Version and App Size Synchronization Script for Holy Quran App.
.DESCRIPTION
    Automates updating the app version and exact APK file size across all project files.
#>
[CmdletBinding()]
param(
    [Parameter(Position = 0)]
    [string]$VersionName,

    [Parameter(Position = 1)]
    [int]$VersionCode = 0,

    [switch]$Increment,
    [string]$Notes,
    [switch]$Build,
    [switch]$Deploy
)

$rootDir = (Get-Item $PSScriptRoot).Parent.FullName

# 1. Update versions and sizes in files
$nodeArgs = @("$PSScriptRoot\update-version.js")
if (![string]::IsNullOrWhiteSpace($VersionName)) { $nodeArgs += @("-v", $VersionName) }
if ($VersionCode -gt 0) { $nodeArgs += @("-c", $VersionCode.ToString()) }
if ($Increment) { $nodeArgs += "-i" }
if (![string]::IsNullOrWhiteSpace($Notes)) { $nodeArgs += @("-n", $Notes) }

& node @nodeArgs
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

# 2. Build Release APK (if requested)
if ($Build) {
    Write-Host "`n[Build] Building Release APK via Gradle..." -ForegroundColor Cyan
    Push-Location $rootDir
    try {
        & .\gradlew.bat assembleRelease
        if ($LASTEXITCODE -ne 0) {
            throw "Gradle assembleRelease failed with code $LASTEXITCODE"
        }
    } finally {
        Pop-Location
    }
    # Re-sync file size after build
    & node @nodeArgs
}

# 3. Deploy to Firebase Hosting (if requested)
if ($Deploy) {
    Write-Host "`n[Deploy] Deploying hosting to Firebase..." -ForegroundColor Cyan
    Push-Location $rootDir
    try {
        & firebase.cmd deploy --only hosting
        if ($LASTEXITCODE -ne 0) {
            throw "Firebase deploy failed with code $LASTEXITCODE"
        }
    } finally {
        Pop-Location
    }
    Write-Host "`n[OK] Live Deployment Complete: https://holyquranapp.web.app`n" -ForegroundColor Green
}
