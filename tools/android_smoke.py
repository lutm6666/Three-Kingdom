"""Android UI/save regression. Runs only on an explicitly selected emulator.
The emulator app data is cleared; physical-device serials are refused.
Usage: python tools/android_smoke.py --serial emulator-5554 --apk path.apk
"""
import argparse
import re
import subprocess
import time
import xml.etree.ElementTree as ET

parser = argparse.ArgumentParser()
parser.add_argument('--serial', required=True)
parser.add_argument('--apk', required=True)
args = parser.parse_args()
if not args.serial.startswith('emulator-'):
    raise SystemExit('This destructive test is restricted to emulator serials.')
package = 'com.openai.threekingdoms'

def adb(*cmd):
    return subprocess.check_output(['adb', '-s', args.serial, *cmd], text=True, timeout=40)

def launch():
    adb('shell', 'input', 'keyevent', 'KEYCODE_WAKEUP')
    adb('shell', 'wm', 'dismiss-keyguard')
    result = adb('shell', 'am', 'start', '-W', '-n', package + '/.MainActivity')
    assert 'Error:' not in result, result
    # Poll without scrolling: scrolling a not-yet-ready homepage can hide its title.
    visible = []
    for _ in range(10):
        visible = [node.attrib.get('text', '') for node in tree().iter('node')]
        if '三國志拼圖大戰重建' in visible:
            return
        time.sleep(0.5)
    logs = adb('logcat', '-d', '-s', 'AndroidRuntime:E', 'MasterData:E')
    raise AssertionError('Homepage did not become ready: ' + repr(visible) + '\n' + logs)

def tree():
    for _ in range(3):
        try:
            adb('shell', 'uiautomator', 'dump', '/sdcard/sgpz-ui.xml')
            return ET.fromstring(adb('exec-out', 'cat', '/sdcard/sgpz-ui.xml'))
        except (subprocess.SubprocessError, ET.ParseError):
            time.sleep(1)
    raise AssertionError('Cannot inspect app UI')

def scroll(up=False):
    x = width - 6  # outside the board, inside the scroll container
    start, end = (height // 4, height * 3 // 4) if up else (height * 3 // 4, height // 4)
    adb('shell', 'input', 'swipe', str(x), str(start), str(x), str(end), '250')

def top():
    for _ in range(6):
        scroll(up=True)

def bounds(node):
    return list(map(int, re.findall(r'\d+', node.attrib.get('bounds', ''))))

def find(predicate):
    for _ in range(16):
        for node in tree().iter('node'):
            box = bounds(node)
            if predicate(node) and len(box) == 4 and box[2] > box[0] and box[3] > box[1]:
                return node
        scroll()
    raise AssertionError('UI element not found')

def click(text, prefix=False):
    node = find(lambda n: n.attrib.get('class') == 'android.widget.Button'
                and n.attrib.get('enabled') == 'true'
                and (n.attrib.get('text', '').startswith(text) if prefix
                     else n.attrib.get('text') == text))
    x1, y1, x2, y2 = bounds(node)
    adb('shell', 'input', 'tap', str((x1+x2)//2), str((y1+y2)//2))
    time.sleep(0.3)

def prefs(name):
    # SharedPreferences.apply() writes asynchronously; the first file may not exist yet.
    last = ''
    for _ in range(20):
        try:
            last = adb('exec-out', 'run-as', package, 'cat', 'shared_prefs/'+name+'.xml')
            root = ET.fromstring(last)
            if root.tag == 'map':
                return root
        except (subprocess.SubprocessError, ET.ParseError) as error:
            last = str(error) + ': ' + last[:300]
        time.sleep(0.25)
    raise AssertionError('Cannot read preferences ' + name + ': ' + last[:300])

def ints(root):
    return {n.attrib['name']: int(n.attrib['value']) for n in root.findall('int')}

adb('install', '-r', args.apk)
adb('shell', 'pm', 'clear', package)
adb('logcat', '-c')
width, height = map(int, re.findall(r'(\d+)x(\d+)', adb('shell', 'wm', 'size'))[-1])
launch()
find(lambda n: n.attrib.get('text') == '三國志拼圖大戰重建')
click('Master 資料核心')
find(lambda n: n.attrib.get('text') == '武將資料載入：成功')
find(lambda n: n.attrib.get('text') == '關卡資料載入：成功')
click('返回首頁')
click('隊伍編成')
click('【已上陣】 關羽', prefix=True)  # swaps default leader with slot 2
saved = ints(prefs('game'))
assert saved['team_0'] == 1 and saved['team_1'] == 0, saved
click('完成編隊／返回首頁')
click('進入關卡')
click('開始：', prefix=True)
find(lambda n: n.attrib.get('text', '').startswith('Wave 1 /'))
node = find(lambda n: n.attrib.get('content-desc') == '拼圖棋盤')
x1,y1,x2,y2 = bounds(node)
adb('shell', 'input', 'swipe', str(x1+(x2-x1)//5), str((y1+y2)//2),
    str(x1+(x2-x1)*4//5), str((y1+y2)//2), '400')
time.sleep(1)
top()
result = find(lambda n: 'Combo：' in n.attrib.get('text', '')).attrib['text']
assert '戰鬥開始' not in result, result
assert ints(prefs('game')) == saved

# Restart the actual app process without clearing data.
adb('shell', 'am', 'force-stop', package)
launch()
assert ints(prefs('game')) == saved
find(lambda n: n.attrib.get('text', '').startswith('主將：關羽'))

# Simulate a legacy level above the current Master cap, then load without
# rewriting that stored level. The displayed/effective level must be capped.
adb('shell', 'am', 'force-stop', package)
progress = prefs('progress')
entry = next((n for n in progress.findall('int') if n.attrib['name'] == 'level_1'), None)
if entry is None:
    entry = ET.SubElement(progress, 'int', {'name': 'level_1'})
entry.set('value', '150')
subprocess.run(['adb', '-s', args.serial, 'shell', 'run-as', package, 'sh', '-c',
                "'cat > shared_prefs/progress.xml'"], input=ET.tostring(progress, encoding='unicode'),
               text=True, check=True, timeout=20)
launch()
click('隊伍編成')
find(lambda n: '位置 1' in n.attrib.get('text', '') and '關羽 Lv.99' in n.attrib.get('text', ''))
assert ints(prefs('progress'))['level_1'] == 150, 'Original save level was overwritten'
assert ints(prefs('game')) == saved
logs = adb('logcat', '-d', '-s', 'AndroidRuntime:E', 'MasterData:E')
assert 'FATAL EXCEPTION' not in logs and 'MasterData' not in logs, logs
print('PASS: launch, canonical load, team swap, battle drag, process restart, saved team, level cap')
