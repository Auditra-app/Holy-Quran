const fs = require('fs');
const path = require('path');

const rootDir = path.resolve(__dirname, '..');

const versionCode = parseInt(process.argv[2] || '23', 10);
const versionName = process.argv[3] || '1.3.9';
const notesAr = process.argv[4] || `تحديث v${versionName}: دعم تصدير واستعادة كافة إعدادات التطبيق بالكامل وتحديث قائمة الخيارات.`;
const notesEn = process.argv[5] || `Update v${versionName}: Full app backup and restore for all settings, and direct file picker support.`;

const cloudflareUrl = `https://holyquran-download.aelsheikh1987.workers.dev/HolyQuran_v${versionName}.apk`;
const firebaseUrl = `https://holyquranapp.web.app/HolyQuran_v${versionName}.data`;

let existingData = {};
const versionJsonPath = path.join(rootDir, 'public', 'version.json');
try {
    if (fs.existsSync(versionJsonPath)) {
        existingData = JSON.parse(fs.readFileSync(versionJsonPath, 'utf8'));
    }
} catch (e) {}

// Check APK file size if present
const publicApkPath = path.join(rootDir, 'public', `HolyQuran_v${versionName}.data`);
let fileBytes = existingData.file_size_bytes || 41042074;
let fileMb = existingData.file_size_mb || 39.1;
let fileFormatted = existingData.file_size_formatted || '39.1 MB';
if (fs.existsSync(publicApkPath)) {
    const stats = fs.statSync(publicApkPath);
    fileBytes = stats.size;
    fileMb = Math.round((fileBytes / (1024 * 1024)) * 10) / 10;
    fileFormatted = `${fileMb} MB`;
}

const versionData = {
    ...existingData,
    latest_version_code: versionCode,
    latest_version_name: versionName,
    apk_download_url: cloudflareUrl,
    cloudflare_download_url: cloudflareUrl,
    firebase_download_url: firebaseUrl,
    file_size_bytes: fileBytes,
    file_size_mb: fileMb,
    file_size_formatted: fileFormatted,
    force_update: false,
    update_notes: notesAr,
    update_notes_ar: notesAr,
    update_notes_en: notesEn
};

fs.writeFileSync(versionJsonPath, JSON.stringify(versionData, null, 4), 'utf8');
console.log(`[OK] Written ${versionJsonPath} (UTF-8)`);

const remoteConfigData = {
    parameters: {
        latest_version_code: {
            defaultValue: { value: String(versionCode) },
            description: 'Latest published app version code'
        },
        latest_version_name: {
            defaultValue: { value: versionName },
            description: 'Latest published version string'
        },
        apk_download_url: {
            defaultValue: { value: cloudflareUrl },
            description: 'Direct high-speed download link for APK update (Cloudflare Edge)'
        },
        firebase_download_url: {
            defaultValue: { value: firebaseUrl },
            description: 'Secondary mirror download link (Firebase Hosting)'
        },
        force_update: {
            defaultValue: { value: 'false' },
            description: 'Whether update is mandatory'
        },
        update_notes: {
            defaultValue: { value: notesAr },
            description: 'Release notes shown in update dialog'
        },
        update_notes_ar: {
            defaultValue: { value: notesAr },
            description: 'Arabic release notes shown in update dialog'
        },
        update_notes_en: {
            defaultValue: { value: notesEn },
            description: 'English release notes shown in update dialog'
        }
    }
};

const remoteConfigPath = path.join(rootDir, 'remoteconfig.template.json');
fs.writeFileSync(remoteConfigPath, JSON.stringify(remoteConfigData, null, 4), 'utf8');
console.log(`[OK] Written ${remoteConfigPath} (UTF-8)`);

// Update public/index.html
const indexHtmlPath = path.join(rootDir, 'public', 'index.html');
if (fs.existsSync(indexHtmlPath)) {
    let indexContent = fs.readFileSync(indexHtmlPath, 'utf8');

    // Update appVersionBadge
    indexContent = indexContent.replace(
        /(<div[^>]*id="appVersionBadge"[^>]*>)([\s\S]*?)(<\/div>)/,
        `$1✨ الإصدار الحالي: v${versionName} (Build ${versionCode}) • صدقة جارية لكل من ساهم في إكمال هذا التطبيق ومن ساهم في نشره$3`
    );

    // Update mainDownloadBtnText
    indexContent = indexContent.replace(
        /(<span[^>]*id="mainDownloadBtnText"[^>]*>)([\s\S]*?)(<\/span>)/,
        `$1تحميل التطبيق المباشر (APK) v${versionName} - سيرفر 1 (يُنصح به ⚡)$3`
    );

    // Update server2DownloadBtnText
    indexContent = indexContent.replace(
        /(<span[^>]*id="server2DownloadBtnText"[^>]*>)([\s\S]*?)(<\/span>)/,
        `$1تحميل التطبيق المباشر (APK) v${versionName} - سيرفر 2$3`
    );

    // Update appSpecsText
    indexContent = indexContent.replace(
        /(<p[^>]*id="appSpecsText"[^>]*>)([\s\S]*?)(<\/p>)/,
        `$1\n                حجم الملف: ${fileFormatted} • الإصدار: v${versionName} (Build ${versionCode}) • يدعم أجهزة أندرويد (Android 7.0+)\n            $3`
    );

    // Update all HolyQuran_vX.Y.Z.data and HolyQuran_vX.Y.Z.apk references
    indexContent = indexContent.replace(/HolyQuran_v\d+\.\d+\.\d+\.data/g, `HolyQuran_v${versionName}.data`);
    indexContent = indexContent.replace(/HolyQuran_v\d+\.\d+\.\d+\.apk/g, `HolyQuran_v${versionName}.apk`);

    fs.writeFileSync(indexHtmlPath, indexContent, 'utf8');
    console.log(`[OK] Written ${indexHtmlPath} (UTF-8)`);
}

// Update firebase.json
const firebaseJsonPath = path.join(rootDir, 'firebase.json');
if (fs.existsSync(firebaseJsonPath)) {
    let firebaseContent = fs.readFileSync(firebaseJsonPath, 'utf8');
    const targetHeaderSource = `/HolyQuran_v${versionName}.data`;
    if (!firebaseContent.includes(targetHeaderSource)) {
        const newHeader = `        {\n          "source": "${targetHeaderSource}",\n          "headers": [\n            {\n              "key": "Content-Type",\n              "value": "application/vnd.android.package-archive"\n            },\n            {\n              "key": "Content-Disposition",\n              "value": "attachment; filename=\\"HolyQuran_v${versionName}.apk\\""\n            },\n            {\n              "key": "Cache-Control",\n              "value": "public, max-age=31536000, immutable"\n            },\n            {\n              "key": "Access-Control-Allow-Origin",\n              "value": "*"\n            }\n          ]\n        },`;
        const marker = '"headers": [';
        const idx = firebaseContent.indexOf(marker);
        if (idx !== -1) {
            const insertPos = idx + marker.length;
            firebaseContent = firebaseContent.slice(0, insertPos) + '\n' + newHeader + firebaseContent.slice(insertPos);
            fs.writeFileSync(firebaseJsonPath, firebaseContent, 'utf8');
            console.log(`[OK] Added header rule for ${targetHeaderSource} to firebase.json`);
        }
    }
}
