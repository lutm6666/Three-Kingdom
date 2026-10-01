"""Bundle exactly the APK exercised by this successful CI run, plus traceable reports."""
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import zipfile

root = Path(__file__).resolve().parent.parent
reports = {
    'save-result.txt': 'PASS: save round-trip, ownership, equipment, loot, legacy level, invalid import unchanged, restored startup',
    'battle-test-result.txt': 'PASS: three stages, all waves, coins, EXP level-up, unlock, drops, duplicate guard, replay, defeat',
    'playability-result.txt': 'PASS: natural-board diagnostic completed',
    'skill-result.txt': 'PASS: heal, delay, conversion, free move, damage, ignore defense, AoE, cooldown, seal, timeout, reset',
}
for name, marker in reports.items():
    report = (root / name).read_text()
    if marker not in report or 'INSTRUMENTATION_CODE: -1' not in report:
        raise RuntimeError('Missing passing report: ' + name)
ui_report = root / 'ui-save-result.txt'
if 'PASS: native document export, picker cancellation, restore cancellation, confirmed restore, restored restart' not in ui_report.read_text():
    raise RuntimeError('Missing passing native document picker report')
apk = root / 'app/build/outputs/apk/debug/app-debug.apk'
with zipfile.ZipFile(apk) as package:
    if 'classes.dex' not in package.namelist():
        raise RuntimeError('APK missing DEX')
    master = json.loads(package.read('assets/data/reconstructed_master_v1.json'))
    if master['art_assets']['identities_verified'] != 0:
        raise RuntimeError('Unexpected artwork identity claim')
build = (root / 'app/build.gradle.kts').read_text()
version = re.search(r'versionName\s*=\s*"([\w.]+)"', build).group(1)
version_code = int(re.search(r'versionCode\s*=\s*(\d+)', build).group(1))
output = root / 'tested-apk'
output.mkdir(exist_ok=True)
filename = 'ThreeKingdom-' + version + '-debug.apk'
shutil.copyfile(apk, output / filename)
digest = hashlib.sha256(apk.read_bytes()).hexdigest()
(output / 'SHA256SUMS').write_text(digest + '  ' + filename + '\n')
shutil.copyfile(ui_report, output / ui_report.name)
for name in reports:
    shutil.copyfile(root / name, output / name)
metadata = {
    'application_id': 'com.openai.threekingdoms',
    'version_name': version,
    'version_code': version_code,
    'build_type': 'debug',
    'checkout_sha': subprocess.check_output(['git', 'rev-parse', 'HEAD'], cwd=root, text=True).strip(),
    'source_head_sha': os.environ.get('SOURCE_HEAD_SHA', ''),
    'run_url': os.environ.get('GITHUB_SERVER_URL', 'https://github.com') + '/' + os.environ['GITHUB_REPOSITORY'] + '/actions/runs/' + os.environ['GITHUB_RUN_ID'],
    'apk_sha256': digest,
    'min_android_api': 23,
    'tested_emulator_api': 35,
    'artwork_identities_verified': 0,
    'reports': list(reports) + [ui_report.name],
}
(output / 'build-info.json').write_text(json.dumps(metadata, ensure_ascii=False, indent=2) + '\n')
(output / 'README.txt').write_text(
    'Three Kingdom reconstruction - tested Android debug build\n'
    'Install the .apk on Android 6.0/API 23 or newer. This package cannot run on iOS.\n'
    'This is a development build, not a store release or original game APK.\n'
    'SHA256SUMS identifies the exact APK used in this CI run.\n'
    'build-info.json records the source, checkout and test run.\n'
    'Included reports cover rewards, natural-board diagnostics, skills and save backup.\n'
    'Human difficulty/playability and physical devices have not been validated.\n'
    'No original commercial artwork is included.\n'
    'Debug signing keys may differ between CI runs; Android may reject replacing an older install.\n'
    'Do not remove an existing installation unless you accept losing its local save.\n')
print('PASS: tested APK bundle created: ' + filename + ' sha256=' + digest)
