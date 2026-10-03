const fs = require('fs');
const path = require('path');

const rootDir = path.resolve(__dirname, '..');

const versionCode = parseInt(process.argv[2] || '23', 10);
const versionName = process.argv[3] || '1.3.9';
const notesAr = process.argv[4] || 'تحديث v1.3.9: تحسين شامل للأداء وسرعة التحميل ودعم خوادم Cloudflare فائق السرعة.';
const notesEn = process.argv[5] || 'Update v1.3.9: Performance and speed improvements with high-speed Cloudflare CDN support.';

const cloudflareUrl = `https://holyquran-download.aelsheikh1987.workers.dev/HolyQuran_v${versionName}.apk`;
const firebaseUrl = `https://holyquranapp.web.app/HolyQuran_v${versionName}.data`;

let existingData = {};
const versionJsonPath = path.join(rootDir, 'public', 'version.json');
try {
    if (fs.existsSync(versionJsonPath)) {
        existingData = JSON.parse(fs.readFileSync(versionJsonPath, 'utf8'));
    }
} catch (e) {}

const versionData = {
    ...existingData,
    latest_version_code: versionCode,
    latest_version_name: versionName,
    apk_download_url: cloudflareUrl,
    cloudflare_download_url: cloudflareUrl,
    firebase_download_url: firebaseUrl,
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
