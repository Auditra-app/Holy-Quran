const fs = require('fs');
const path = require('path');

const rootDir = path.resolve(__dirname, '..');

const versionCode = parseInt(process.argv[2] || '8', 10);
const versionName = process.argv[3] || '1.2.4';
const notesAr = process.argv[4] || 'تحديث v1.2.4: تحسين شامل لمعاني الكلمات باللغة العربية لكل كلمة بدقة وتفصيل وتفسير لغوي خاص.';
const notesEn = process.argv[5] || 'Update v1.2.4: Comprehensive word-by-word meanings in Arabic with accurate linguistic definitions and tafsir enrichment.';

const versionData = {
    latest_version_code: versionCode,
    latest_version_name: versionName,
    apk_download_url: 'https://holy-quran-app-live.web.app/HolyQuran_v1.1.0.data',
    force_update: false,
    update_notes: notesAr,
    update_notes_ar: notesAr,
    update_notes_en: notesEn
};

const versionJsonPath = path.join(rootDir, 'public', 'version.json');
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
            defaultValue: { value: 'https://holy-quran-app-live.web.app/HolyQuran_v1.1.0.data' },
            description: 'Direct download link for APK update'
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
