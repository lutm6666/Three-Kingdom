"""Host regression: python tools/test_roster_loader.py --json-jar /path/to/json.jar.
Requires JDK 17. Optional --compiler-jar supports an ECJ compiler on JRE hosts.
Android stubs are test-only; this does not replace an APK/device test.
"""
import argparse
from pathlib import Path
import subprocess
import tempfile

parser = argparse.ArgumentParser()
parser.add_argument('--json-jar', required=True)
parser.add_argument('--compiler-jar')
args = parser.parse_args()
root = Path(__file__).resolve().parent.parent
sources = list((root / 'tools/tests/roster').rglob('*.java'))
package = root / 'app/src/main/java/com/openai/threekingdoms'
sources += [package / f'{name}.java' for name in
            ('DataProvenance', 'GameData', 'ReconstructedMasterData')]
with tempfile.TemporaryDirectory() as output:
    compiler = (['java', '-jar', str(Path(args.compiler_jar).resolve()), '-17', '-proc:none']
                if args.compiler_jar else ['javac', '--release', '17'])
    subprocess.run(compiler + ['-cp', str(Path(args.json_jar).resolve()), '-d', output]
                   + [str(p) for p in sources], check=True)
    import os
    subprocess.run(['java', '-cp', os.pathsep.join([str(Path(args.json_jar).resolve()), output]),
                    'Check', str(root / 'app/src/main/assets/data/reconstructed_master_v1.json')], check=True)
