[CmdletBinding()]
param (
    [Parameter(Mandatory=$false)]
    [int]$VersionCode = 0,

    [Parameter(Mandatory=$false)]
    [string]$VersionName = "",

    [Parameter(Mandatory=$false)]
    [string]$UpdateNotes = "",

    [switch]$SkipDeploy,
    [switch]$SkipGitPush
)

$ErrorActionPreference = "Stop"

$rootDir = Split-Path -Parent $PSScriptRoot
Set-Location $rootDir

Write-Host "==========================================" -ForegroundColor Cyan
Write-Host " Holy Quran Automated Release Workflow   " -ForegroundColor Cyan
Write-Host "==========================================" -ForegroundColor Cyan

# 1. Read Current Versions if not provided
$buildGradle = Get-Content "$rootDir\app\build.gradle.kts" -Raw
if ($VersionCode -le 0) {
    if ($buildGradle -match 'versionCode\s*=\s*(\d+)') {
        $VersionCode = [int]$matches[1]
    } else {
        throw "Could not determine versionCode from app/build.gradle.kts"
    }
}

if ([string]::IsNullOrWhiteSpace($VersionName)) {
    if ($buildGradle -match 'versionName\s*=\s*"([^"]+)"') {
        $VersionName = $matches[1]
    } else {
        throw "Could not determine versionName from app/build.gradle.kts"
    }
}

Write-Host "[1/6] Target Release: Version $VersionName (Build $VersionCode)" -ForegroundColor Green

# 2. Synchronize Version in app/build.gradle.kts
$buildGradleUpdated = $buildGradle -replace 'versionCode\s*=\s*\d+', "versionCode = $VersionCode"
$buildGradleUpdated = $buildGradleUpdated -replace 'versionName\s*=\s*"[^"]+"', "versionName = `"$VersionName`""
Set-Content -Path "$rootDir\app\build.gradle.kts" -Value $buildGradleUpdated -NoNewline

# 3. Synchronize public/version.json & remoteconfig.template.json via Node (guaranteed pure UTF-8 without corruption)
$notesToUse = if (-not [string]::IsNullOrWhiteSpace($UpdateNotes)) { $UpdateNotes } else { "تحديث v$VersionName: تحسينات شاملة في الأداء والسرعة وتحديث واجهة التطبيق." }
node "$rootDir\scripts\sync_release_json.js" $VersionCode "$VersionName" "$notesToUse"
Write-Host "[2/6] Synchronized public/version.json and remoteconfig.template.json (UTF-8)" -ForegroundColor Green

# 5. Build APK via Gradle
Write-Host "[4/6] Compiling APK (:app:assembleDebug)..." -ForegroundColor Yellow
$buildStartTime = Get-Date

$gradleCmd = if (Test-Path "$rootDir\gradlew.bat") { "$rootDir\gradlew.bat" } else { "gradle.bat" }
& $gradleCmd :app:assembleDebug
if ($LASTEXITCODE -ne 0) {
    throw "Gradle build failed with exit code $LASTEXITCODE"
}

# Verify output APK
$outputApk = "$rootDir\app\build\outputs\apk\debug\app-debug.apk"
if (-not (Test-Path $outputApk)) {
    throw "Build completed but output APK not found at $outputApk"
}

$apkItem = Get-Item $outputApk
if ($apkItem.LastWriteTime -lt $buildStartTime.AddSeconds(-2)) {
    throw "Output APK was not updated! Timestamp ($($apkItem.LastWriteTime)) is older than build start ($buildStartTime)"
}

Write-Host "       Built APK successfully: $([math]::Round($apkItem.Length / 1MB, 2)) MB (Timestamp: $($apkItem.LastWriteTime))" -ForegroundColor Green

# 6. Copy APK to public/HolyQuran_v1.1.0.data
$destData = "$rootDir\public\HolyQuran_v1.1.0.data"
Copy-Item -Path $outputApk -Destination $destData -Force

$copiedItem = Get-Item $destData
if ($copiedItem.Length -ne $apkItem.Length) {
    throw "Verification failed: Copied APK size ($($copiedItem.Length)) does not match source ($($apkItem.Length))"
}
Write-Host "[5/6] Verified & Copied APK to $destData" -ForegroundColor Green

# 7. Deploy to Firebase
if (-not $SkipDeploy) {
    Write-Host "[6/6] Deploying to Firebase (Hosting & Remote Config)..." -ForegroundColor Yellow
    firebase deploy --only "remoteconfig,hosting"
    if ($LASTEXITCODE -ne 0) {
        throw "Firebase deploy failed with exit code $LASTEXITCODE"
    }
    Write-Host "       Firebase deployment complete!" -ForegroundColor Green

    Write-Host "       Deploying APK to Cloudflare R2 via Wrangler..." -ForegroundColor Yellow
    npx wrangler r2 object put "holy-quran-releases/HolyQuran_v$VersionName.apk" --file="$destData" --content-type="application/vnd.android.package-archive" --remote
    if ($LASTEXITCODE -eq 0) {
        Write-Host "       Cloudflare R2 deployment complete!" -ForegroundColor Green
    }
} else {
    Write-Host "[6/6] Skipping Firebase deploy (--SkipDeploy flag set)" -ForegroundColor Gray
}

# 8. Git Commit & Push
if (-not $SkipGitPush) {
    Write-Host "Committing and pushing changes to GitHub..." -ForegroundColor Yellow
    git add app/build.gradle.kts public/version.json remoteconfig.template.json public/HolyQuran_v1.1.0.data scripts/
    git commit -m "release: v$VersionName (Build $VersionCode) - update APK and sync version configuration"
    git push origin main
    Write-Host "Git push complete!" -ForegroundColor Green
}

Write-Host "==========================================" -ForegroundColor Green
Write-Host " Release v$VersionName (Build $VersionCode) Complete! " -ForegroundColor Green
Write-Host "==========================================" -ForegroundColor Green
