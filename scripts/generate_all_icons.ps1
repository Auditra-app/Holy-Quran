$ErrorActionPreference = "Stop"

$sourceLogo = "F:\Holy-Quran\public\holyquran_logo.png"
if (-not (Test-Path $sourceLogo)) {
    throw "Source logo not found at $sourceLogo"
}

$tmpDir = "F:\Holy-Quran\build\tmp_icons"
New-Item -ItemType Directory -Force -Path $tmpDir | Out-Null

Write-Host "1. Cropping emblem tightly (content is 1100x1146 centered at 1024, 1019)..." -ForegroundColor Cyan
$emblemCrop = "$tmpDir\emblem_tight.png"
& magick $sourceLogo -crop 1120x1160+464+436 +repage $emblemCrop

Write-Host "2. Generating app_logo with white background and generous breathing margin..." -ForegroundColor Cyan
# Generate 512x512 app_logo with 370px emblem (~72% fill) so text is 100% visible inside circles
$appLogoMaster = "$tmpDir\app_logo_512.png"
& magick $emblemCrop -resize 370x370 -filter Lanczos -background none -gravity center -extent 512x512 $appLogoMaster

Copy-Item $appLogoMaster "F:\Holy-Quran\public\app_logo.png" -Force
Copy-Item $appLogoMaster "F:\Holy-Quran\app\src\main\res\drawable\app_logo.png" -Force

# WebP version
& magick $appLogoMaster -quality 95 "F:\Holy-Quran\public\app_logo.webp"

# JPEG version with PURE WHITE background
& magick $appLogoMaster -background "#FFFFFF" -flatten -quality 92 "F:\Holy-Quran\public\app_logo.jpg"

Write-Host "3. Generating adaptive icon foregrounds (shrunk to 44% of canvas for full text visibility in circular masks)..." -ForegroundColor Cyan
$foregroundDensities = @{
    "drawable-mdpi" = 108
    "drawable-hdpi" = 162
    "drawable-xhdpi" = 216
    "drawable-xxhdpi" = 324
    "drawable-xxxhdpi" = 432
}

foreach ($density in $foregroundDensities.Keys) {
    $dim = $foregroundDensities[$density]
    $emblemDim = [Math]::Round($dim * 0.44)
    $dest = "F:\Holy-Quran\app\src\main\res\$density\ic_launcher_foreground.png"
    
    & magick $emblemCrop -resize "${emblemDim}x${emblemDim}" -filter Lanczos -background none -gravity center -extent "${dim}x${dim}" $dest
    Write-Host "Generated $dest (canvas $dim x $dim, emblem $emblemDim x $emblemDim)" -ForegroundColor Gray
}

Write-Host "4. Generating full legacy and widget mipmap icons (WHITE background + subtle gold border)..." -ForegroundColor Cyan
$mipmapDensities = @{
    "mipmap-mdpi" = 48
    "mipmap-hdpi" = 72
    "mipmap-xhdpi" = 96
    "mipmap-xxhdpi" = 144
    "mipmap-xxxhdpi" = 192
}

# 512x512 Master Round Template (White base with elegant gold ring and 320px emblem)
$roundBase512 = "$tmpDir\round_base_white.png"
& magick -size 512x512 xc:none -fill "#FFFFFF" -stroke "#D4AF37" -strokewidth 10 -draw "circle 256,256 256,12" $roundBase512

$roundMaster512 = "$tmpDir\round_master_white.png"
$emblem320 = "$tmpDir\emblem_320.png"
& magick $emblemCrop -resize 320x320 -filter Lanczos $emblem320
& magick $roundBase512 $emblem320 -gravity center -composite $roundMaster512

# 512x512 Master Square Template (White rounded-rect with elegant gold ring and 340px emblem)
$squareBase512 = "$tmpDir\square_base_white.png"
& magick -size 512x512 xc:none -fill "#FFFFFF" -stroke "#D4AF37" -strokewidth 10 -draw "roundrectangle 12,12 500,500 70,70" $squareBase512

$squareMaster512 = "$tmpDir\square_master_white.png"
$emblem340 = "$tmpDir\emblem_340.png"
& magick $emblemCrop -resize 340x340 -filter Lanczos $emblem340
& magick $squareBase512 $emblem340 -gravity center -composite $squareMaster512

foreach ($density in $mipmapDensities.Keys) {
    $dim = $mipmapDensities[$density]
    $roundWebp = "F:\Holy-Quran\app\src\main\res\$density\ic_launcher_round.webp"
    & magick $roundMaster512 -resize "${dim}x${dim}" -filter Lanczos -quality 95 $roundWebp
    
    $squareWebp = "F:\Holy-Quran\app\src\main\res\$density\ic_launcher.webp"
    & magick $squareMaster512 -resize "${dim}x${dim}" -filter Lanczos -quality 95 $squareWebp
    
    Write-Host "Generated $density white icons ($dim x $dim)" -ForegroundColor Gray
}

Write-Host "5. Generating SVG Favicon with white background..." -ForegroundColor Cyan
$pngBytes = [System.IO.File]::ReadAllBytes($appLogoMaster)
$b64 = [System.Convert]::ToBase64String($pngBytes)
$svgContent = @"
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 512 512" width="512" height="512">
  <circle cx="256" cy="256" r="248" fill="#FFFFFF" stroke="#D4AF37" stroke-width="8"/>
  <image href="data:image/png;base64,$b64" x="25" y="25" width="462" height="462" />
</svg>
"@
Set-Content -Path "F:\Holy-Quran\public\favicon.svg" -Value $svgContent -Encoding utf8

Write-Host "`nAll icons successfully updated with pure white background and safe text margins!" -ForegroundColor Green
