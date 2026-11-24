"""Build independent consumers of the locally staged AAR/JAR with pinned toolchains."""
import argparse
import datetime
import json
import os
from pathlib import Path
import shutil
import subprocess
import sys
import time

FIXTURE = Path(__file__).resolve().parent
ROOT = FIXTURE.parent.parent
OUTPUT = ROOT / '.gradle' / 'compatibility'
PROFILES = json.loads((FIXTURE / 'profiles.json').read_text())


def run(command, cwd, log):
    print(f"Running {' '.join(map(str, command))}", flush=True)
    start = time.monotonic()
    with log.open('w', encoding='utf-8') as stream:
        result = subprocess.run(command, cwd=cwd, stdout=stream, stderr=subprocess.STDOUT)
    if result.returncode:
        print(log.read_text(encoding='utf-8', errors='replace')[-14000:], flush=True)
        raise RuntimeError(f'Exit {result.returncode}; full log: {log}')
    return round(time.monotonic() - start, 1)


def verify_lint_check(wrapper, project, name):
    """Require the embedded custom check to detect a deliberately broken scene."""
    probe = project / 'src/main/kotlin/composium/compatibility/UnusedPaddingProbe.kt'
    log = OUTPUT / f'{name}-lint-probe.log'
    probe.write_text('package composium.compatibility\n'
                     'import oleginvoke.com.composium.scene\n'
                     'val UnusedPaddingProbe by scene(thumbnail = null) { }\n', encoding='utf-8')
    try:
        with log.open('w', encoding='utf-8') as stream:
            result = subprocess.run([wrapper, 'lintDebug', '--no-daemon', '--console=plain'],
                                    cwd=project, stdout=stream, stderr=subprocess.STDOUT)
        detected = result.returncode != 0 and '[UnusedComposiumContentPaddingParameter' in log.read_text(encoding='utf-8', errors='replace')
    finally:
        probe.unlink(missing_ok=True)
    run([wrapper, 'lintDebug', '--no-daemon', '--console=plain'], project, OUTPUT / f'{name}-lint-restored.log')
    if not detected:
        raise RuntimeError(f'The embedded lint check did not detect unused scene padding; see {log}')


def main():
    # Windows terminals can otherwise fail while printing a compiler's Unicode diagnostic.
    sys.stdout.reconfigure(encoding='utf-8', errors='replace')
    sys.stderr.reconfigure(encoding='utf-8', errors='replace')
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--profiles', nargs='+', choices=PROFILES, default=list(PROFILES))
    parser.add_argument('--skip-stage', action='store_true')
    parser.add_argument('--connected', action='store_true', help='Also run UI smoke tests on the connected emulator/device')
    parser.add_argument('--lint-probe', action='store_true', help='Verify the embedded lint check with a deliberately invalid scene')
    args = parser.parse_args()
    OUTPUT.mkdir(parents=True, exist_ok=True)
    wrapper = 'gradlew.bat' if os.name == 'nt' else './gradlew'
    if not args.skip_stage:
        run([str(ROOT / wrapper.removeprefix('./')), '--init-script', str(FIXTURE / 'stage.init.gradle'),
             'stageCompatibilityArtifacts', '--console=plain'], ROOT, OUTPUT / 'stage.log')
    version = next(line.split('=', 1)[1].strip() for line in (ROOT / 'gradle.properties').read_text().splitlines()
                   if line.startswith('VERSION_NAME='))
    results = []
    for name in args.profiles:
        profile = PROFILES[name]
        project = OUTPUT / name
        project.mkdir(exist_ok=True)
        for filename in ('settings.gradle', 'build.gradle'):
            shutil.copyfile(FIXTURE / filename, project / filename)
        shutil.copytree(FIXTURE / 'src', project / 'src', dirs_exist_ok=True)
        if (ROOT / 'local.properties').exists():
            shutil.copyfile(ROOT / 'local.properties', project / 'local.properties')
        wrapper_dir = project / 'gradle' / 'wrapper'
        wrapper_dir.mkdir(parents=True, exist_ok=True)
        shutil.copyfile(ROOT / 'gradle/wrapper/gradle-wrapper.jar', wrapper_dir / 'gradle-wrapper.jar')
        (wrapper_dir / 'gradle-wrapper.properties').write_text(
            f'distributionUrl=https\\://services.gradle.org/distributions/gradle-{profile["gradle"]}-bin.zip\n'
            'networkTimeout=60000\nvalidateDistributionUrl=true\n', encoding='utf-8')
        for filename in ('gradlew', 'gradlew.bat'):
            shutil.copy2(ROOT / filename, project / filename)
        properties = {'compat.' + key: str(value).lower() if isinstance(value, bool) else value
                      for key, value in profile.items() if key != 'gradle'}
        properties.update({'compat.repo': (OUTPUT / 'repository').as_posix(), 'compat.version': version,
                           'android.useAndroidX': 'true', 'ksp.useKSP2': 'true',
                           'org.gradle.jvmargs': '-Xmx1536m -Dfile.encoding=UTF-8', 'org.gradle.workers.max': '2'})
        (project / 'gradle.properties').write_text(''.join(f'{k}={v}\n' for k, v in properties.items()), encoding='utf-8')
        tasks = ['assembleDebug', 'assembleRelease', 'lintDebug', 'verifyCompatibilityGraph']
        if profile['agp'].startswith('8.'):
            tasks.append('testDebugUnitTest')
        if args.connected:
            tasks.append('connectedDebugAndroidTest')
        elapsed = run([str(project / wrapper.removeprefix('./')), *tasks, '--no-daemon', '--console=plain'],
                      project, OUTPUT / f'{name}.log')
        if args.lint_probe:
            verify_lint_check(str(project / wrapper.removeprefix('./')), project, name)
        result = {'profile': name, **profile, 'seconds': elapsed, 'tasks': tasks, 'lintProbe': args.lint_probe, 'status': 'passed'}
        results.append(result)
        (OUTPUT / f'{name}-result.json').write_text(json.dumps(result, indent=2) + '\n', encoding='utf-8')
        print(f'{name}: passed in {elapsed}s', flush=True)
    report = {'date': datetime.date.today().isoformat(), 'version': version, 'results': results}
    (OUTPUT / 'results.json').write_text(json.dumps(report, indent=2) + '\n', encoding='utf-8')


if __name__ == '__main__':
    main()
