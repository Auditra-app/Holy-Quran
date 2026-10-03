#!/usr/bin/env node

/**
 * Holy Quran App - Master Version & App Size Synchronization Tool
 * Automatically synchronizes version code, version name, exact APK file size,
 * download URLs, HTML badges, specs, and Firebase headers.
 */

const fs = require('fs');
const path = require('path');
const { execSync } = require('child_process');

const rootDir = path.resolve(__dirname, '..');

// Parse CLI arguments
const args = process.argv.slice(2);
let targetVersionName = null;
let targetVersionCode = null;
let doIncrement = false;
let doBuild = false;
let doDeploy = false;
let customNotes = null;

for (let i = 0; i < args.length; i++) {
    const arg = args[i];
    if (arg === '--build' || arg === '-b' || arg === '-Build') {
        doBuild = true;
    } else if (arg === '--deploy' || arg === '-d' || arg === '-Deploy') {
        doDeploy = true;
    } else if (arg === '--increment' || arg === '-i' || arg === '-Increment') {
        doIncrement = true;
    } else if (arg === '--version' || arg === '-v' || arg === '-VersionName') {
        targetVersionName = args[++i];
    } else if (arg === '--code' || arg === '-c' || arg === '-VersionCode') {
        targetVersionCode = parseInt(args[++i], 10);
    } else if (arg === '--notes' || arg === '-n' || arg === '-Notes') {
        customNotes = args[++i];
    } else if (!arg.startsWith('-')) {
        if (!targetVersionName) {
            targetVersionName = arg;
        } else if (!targetVersionCode) {
            targetVersionCode = parseInt(arg, 10);
        }
    }
}

console.log("============================================================");
console.log("  Holy Quran App - Master Version & Size Sync Tool");
console.log("============================================================");
console.log(`Working Directory: ${rootDir}\n`);

// -----------------------------------------------------------------------------
// 1. Read Current Configuration from app/build.gradle.kts
// -----------------------------------------------------------------------------
const gradlePath = path.join(rootDir, 'app', 'build.gradle.kts');
let gradleContent = fs.readFileSync(gradlePath, 'utf8');

const codeMatch = gradleContent.match(/versionCode\s*=\s*(\d+)/);
const nameMatch = gradleContent.match(/versionName\s*=\s*"([^"]+)"/);

const currentCode = codeMatch ? parseInt(codeMatch[1], 10) : 11;
const currentName = nameMatch ? nameMatch[1] : '1.2.7';

console.log(`Current Configuration:`);
console.log(`  Current Version Name: v${currentName}`);
console.log(`  Current Version Code: ${currentCode}\n`);

const finalName = targetVersionName ? targetVersionName.replace(/^v/, '').trim() : currentName;
const finalCode = targetVersionCode ? targetVersionCode : (doIncrement ? currentCode + 1 : currentCode);

if (finalName !== currentName || finalCode !== currentCode) {
    console.log(`Updating app/build.gradle.kts to v${finalName} (Build ${finalCode})...`);
    gradleContent = gradleContent.replace(/versionCode\s*=\s*\d+/, `versionCode = ${finalCode}`);
    gradleContent = gradleContent.replace(/versionName\s*=\s*"[^"]+"/, `versionName = "${finalName}"`);
    fs.writeFileSync(gradlePath, gradleContent, 'utf8');
    console.log(`  [OK] app/build.gradle.kts updated.`);
} else {
    console.log(`Version remains v${finalName} (Build ${finalCode}).`);
}

// -----------------------------------------------------------------------------
// 2. Build Release APK (if requested)
// -----------------------------------------------------------------------------
const apkSourcePath = path.join(rootDir, 'app', 'build', 'outputs', 'apk', 'release', 'app-release.apk');
const publicApkPath = path.join(rootDir, 'public', `HolyQuran_v${finalName}.data`);

if (doBuild) {
    console.log(`\nBuilding Release APK via Gradle (assembleRelease)...`);
    execSync('cmd.exe /c gradlew.bat assembleRelease', { cwd: rootDir, stdio: 'inherit' });
    console.log(`  [OK] Release APK built successfully.`);
}

// -----------------------------------------------------------------------------
// 3. Copy APK to public/ and calculate exact size
// -----------------------------------------------------------------------------
if (fs.existsSync(apkSourcePath)) {
    console.log(`\nCopying APK to public/HolyQuran_v${finalName}.data...`);
    fs.copyFileSync(apkSourcePath, publicApkPath);
    console.log(`  [OK] Copied APK to ${publicApkPath}`);
}

let fileBytes = 41025666;
let fileMb = 39.1;
let fileFormatted = '39.1 MB';

if (fs.existsSync(publicApkPath)) {
    const stats = fs.statSync(publicApkPath);
    fileBytes = stats.size;
    fileMb = Math.round((fileBytes / (1024 * 1024)) * 10) / 10;
    fileFormatted = `${fileMb} MB`;
    console.log(`\nExact APK File Size:`);
    console.log(`  Size (Bytes):     ${fileBytes}`);
    console.log(`  Size (Formatted): ${fileFormatted}`);
} else {
    console.log(`\n[Notice] File ${publicApkPath} not found yet, using default size: ${fileFormatted}`);
}

// -----------------------------------------------------------------------------
// 4. Update public/version.json
// -----------------------------------------------------------------------------
const versionJsonPath = path.join(rootDir, 'public', 'version.json');
console.log(`\nUpdating public/version.json...`);
let versionJson = {};
if (fs.existsSync(versionJsonPath)) {
    try {
        versionJson = JSON.parse(fs.readFileSync(versionJsonPath, 'utf8'));
    } catch (e) {
        versionJson = {};
    }
}

versionJson.latest_version_code = finalCode;
versionJson.latest_version_name = finalName;
versionJson.apk_download_url = `https://holyquranapp.web.app/HolyQuran_v${finalName}.data`;
versionJson.file_size_bytes = fileBytes;
versionJson.file_size_mb = fileMb;
versionJson.file_size_formatted = fileFormatted;
versionJson.force_update = false;

if (customNotes) {
    versionJson.update_notes = customNotes;
    versionJson.update_notes_ar = customNotes;
    versionJson.update_notes_en = customNotes;
}

fs.writeFileSync(versionJsonPath, JSON.stringify(versionJson, null, 4), 'utf8');
console.log(`  [OK] public/version.json updated.`);

// Update remoteconfig.template.json
const remoteConfigPath = path.join(rootDir, 'remoteconfig.template.json');
if (fs.existsSync(remoteConfigPath)) {
    const remoteConfigData = {
        parameters: {
            latest_version_code: {
                defaultValue: { value: String(finalCode) },
                description: 'Latest published app version code'
            },
            latest_version_name: {
                defaultValue: { value: finalName },
                description: 'Latest published version string'
            },
            apk_download_url: {
                defaultValue: { value: `https://holyquranapp.web.app/HolyQuran_v${finalName}.data` },
                description: 'Direct download link for APK update'
            },
            force_update: {
                defaultValue: { value: 'false' },
                description: 'Whether update is mandatory'
            },
            update_notes: {
                defaultValue: { value: versionJson.update_notes || '' },
                description: 'Release notes shown in update dialog'
            },
            update_notes_ar: {
                defaultValue: { value: versionJson.update_notes_ar || versionJson.update_notes || '' },
                description: 'Arabic release notes shown in update dialog'
            },
            update_notes_en: {
                defaultValue: { value: versionJson.update_notes_en || '' },
                description: 'English release notes shown in update dialog'
            }
        }
    };
    fs.writeFileSync(remoteConfigPath, JSON.stringify(remoteConfigData, null, 4), 'utf8');
    console.log(`  [OK] remoteconfig.template.json updated.`);
}

// -----------------------------------------------------------------------------
// 5. Update public/index.html
// -----------------------------------------------------------------------------
const indexHtmlPath = path.join(rootDir, 'public', 'index.html');
console.log(`\nUpdating public/index.html...`);
let indexContent = fs.readFileSync(indexHtmlPath, 'utf8');

// Update appVersionBadge
indexContent = indexContent.replace(
    /(<div[^>]*id="appVersionBadge"[^>]*>)([\s\S]*?)(<\/div>)/,
    `$1✨ الإصدار الحالي: v${finalName} (Build ${finalCode}) • صدقة جارية لكل من ساهم في إكمال هذا التطبيق ومن ساهم في نشره$3`
);

// Update mainDownloadBtnText
indexContent = indexContent.replace(
    /(<span[^>]*id="mainDownloadBtnText"[^>]*>)([\s\S]*?)(<\/span>)/,
    `$1تحميل التطبيق المباشر (APK) v${finalName}$3`
);

// Update appSpecsText with exact file size and version
indexContent = indexContent.replace(
    /(<p[^>]*id="appSpecsText"[^>]*>)([\s\S]*?)(<\/p>)/,
    `$1\n                حجم الملف: ${fileFormatted} • الإصدار: v${finalName} (Build ${finalCode}) • يدعم أجهزة أندرويد (Android 7.0+)\n            $3`
);

// Update all HolyQuran_vX.Y.Z.data and HolyQuran_vX.Y.Z.apk references
indexContent = indexContent.replace(/HolyQuran_v\d+\.\d+\.\d+\.data/g, `HolyQuran_v${finalName}.data`);
indexContent = indexContent.replace(/HolyQuran_v\d+\.\d+\.\d+\.apk/g, `HolyQuran_v${finalName}.apk`);

// Update analytics version reference
indexContent = indexContent.replace(
    /version:\s*'[^']+',(\s*source:\s*'landing_page_direct_btn')/g,
    `version: '${finalName}',$1`
);

fs.writeFileSync(indexHtmlPath, indexContent, 'utf8');
console.log(`  [OK] public/index.html updated.`);

// -----------------------------------------------------------------------------
// 6. Update firebase.json headers
// -----------------------------------------------------------------------------
const firebaseJsonPath = path.join(rootDir, 'firebase.json');
console.log(`\nUpdating firebase.json headers...`);
let firebaseContent = fs.readFileSync(firebaseJsonPath, 'utf8');

const targetHeaderSource = `/HolyQuran_v${finalName}.data`;
if (!firebaseContent.includes(targetHeaderSource)) {
    const newHeader = `      {\n        "source": "${targetHeaderSource}",\n        "headers": [\n          {\n            "key": "Content-Type",\n            "value": "application/vnd.android.package-archive"\n          },\n          {\n            "key": "Content-Disposition",\n            "value": "attachment; filename=\\"HolyQuran_v${finalName}.apk\\""\n          }\n        ]\n      },`;
    const marker = '"headers": [';
    const idx = firebaseContent.indexOf(marker);
    if (idx !== -1) {
        const insertPos = idx + marker.length;
        firebaseContent = firebaseContent.slice(0, insertPos) + '\n' + newHeader + firebaseContent.slice(insertPos);
        fs.writeFileSync(firebaseJsonPath, firebaseContent, 'utf8');
        console.log(`  [OK] Added header rule for ${targetHeaderSource} to firebase.json`);
    }
} else {
    console.log(`  [OK] Header rule for ${targetHeaderSource} already present in firebase.json`);
}

// -----------------------------------------------------------------------------
// 7. Deploy to Firebase (if requested)
// -----------------------------------------------------------------------------
if (doDeploy) {
    console.log(`\nDeploying hosting to Firebase...`);
    try {
        const deployOut = execSync('firebase.cmd deploy --only hosting', { cwd: rootDir, encoding: 'utf8' });
        console.log(deployOut);
        console.log(`  [OK] Firebase Hosting deployed successfully!`);
    } catch (e) {
        console.error("Firebase deploy error:", e.stdout || e.message);
        throw e;
    }

    console.log(`\nDeploying APK to Cloudflare R2...`);
    try {
        execSync(`npx.cmd wrangler r2 object put "holy-quran-releases/HolyQuran_v${finalName}.apk" --file="${targetApkPath}" --content-type="application/vnd.android.package-archive" --remote`, { cwd: rootDir, stdio: 'inherit' });
        console.log(`  [OK] Cloudflare R2 upload complete!`);
    } catch (e) {
        console.warn("  [WARN] Cloudflare R2 upload warning:", e.message);
    }
}

console.log("\n============================================================");
console.log("  Synchronization Complete!");
console.log("============================================================");
console.log(`  Version Name:    v${finalName}`);
console.log(`  Version Code:    ${finalCode}`);
console.log(`  APK File Size:   ${fileFormatted} (${fileBytes} bytes)`);
console.log(`  APK File Target: public/HolyQuran_v${finalName}.data`);
console.log(`  Download URL:    https://holyquranapp.web.app/HolyQuran_v${finalName}.data`);
console.log(`  Landing Page:    https://holyquranapp.web.app`);
console.log("============================================================\n");
