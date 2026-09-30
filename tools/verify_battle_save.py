"""Verify the emulator's on-disk rewards after force-stop, including every loot item."""
import json
from pathlib import Path
import sys
import xml.etree.ElementTree as ET

result = Path(sys.argv[1]).read_text()
assert 'INSTRUMENTATION_CODE: -1' in result, 'instrumentation did not pass'
lines = [line for line in result.splitlines()
         if line.startswith('INSTRUMENTATION_RESULT: expectedLoot=')]
assert len(lines) == 1, 'missing or ambiguous expected loot'
expected_loot = json.loads(lines[0].split('=', 1)[1])
assert expected_loot and all(type(v) is int and v >= 0 for v in expected_loot.values())
saved = {node.attrib['name']: int(node.attrib['value'])
         for node in ET.parse(sys.argv[2]).getroot().findall('int')}
assert saved['coins'] == 1760, 'coins did not persist'
assert saved['highest_unlocked_stage'] == 2, 'unlock did not persist'
for general_id in range(5):
    assert saved['level_' + str(general_id)] == 12, 'level did not persist'
    assert saved['exp_' + str(general_id)] == 160, 'EXP did not persist'
for key, count in expected_loot.items():
    assert saved.get(key, 0) == count, 'loot did not persist: ' + key
assert all(key in expected_loot for key in saved if key.startswith('loot_')), 'unexpected loot'
print('PASS: three-stage coins, levels, EXP, unlock and loot persisted after process stop')
