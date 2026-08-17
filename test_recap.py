#!/usr/bin/env python3
import glob
import sys
import xml.etree.ElementTree as ET

log_file = sys.argv[1] if len(sys.argv) > 1 else ''
exit_code = int(sys.argv[2]) if len(sys.argv) > 2 else 0

reports = sorted(glob.glob('target/surefire-reports/TEST-*.xml'))

passed = []
failed = []
errored = []
skipped = []

for r in reports:
    try:
        tree = ET.parse(r)
        root = tree.getroot()
        for tc in root.findall('testcase'):
            name = tc.attrib.get('name', 'unknown')
            cls = tc.attrib.get('classname', 'unknown')
            simple_cls = cls.split('.')[-1]
            time_val = tc.attrib.get('time', '0')

            fail_node = tc.find('failure')
            err_node = tc.find('error')
            skp_node = tc.find('skipped')

            item = {
                'name': name,
                'class': cls,
                'simple_class': simple_cls,
                'time': time_val,
            }

            if fail_node is not None or err_node is not None:
                node = fail_node if fail_node is not None else err_node
                err_type = node.attrib.get('type', 'Error')
                msg = (node.attrib.get('message') or '').strip()
                txt = node.text or ''

                stack_lines = []
                file_line_info = ''
                for line in txt.splitlines():
                    line_str = line.strip()
                    if line_str.startswith('at '):
                        if not file_line_info and '(' in line_str and ')' in line_str:
                            if not any(pkg in line_str for pkg in ['org.junit.', 'org.apache.maven.', 'jdk.internal.', 'java.base.', 'org.springframework.test.']):
                                file_line_info = line_str[line_str.find('(')+1 : line_str.find(')')]
                        if any(pkg in line_str for pkg in ['com.example', 'cours_test']) or not any(pkg in line_str for pkg in ['org.junit.', 'org.apache.', 'jdk.internal.', 'java.base.', 'org.springframework.test.']):
                            if len(stack_lines) < 3:
                                stack_lines.append(line_str)

                if not msg and txt:
                    first_lines = [l.strip() for l in txt.splitlines() if l.strip() and not l.strip().startswith('at ')]
                    if first_lines:
                        msg = ' '.join(first_lines[:2])

                item['type'] = err_type
                item['message'] = msg
                item['location'] = file_line_info
                item['stack'] = stack_lines

                if fail_node is not None:
                    failed.append(item)
                else:
                    errored.append(item)
            elif skp_node is not None:
                skipped.append(item)
            else:
                passed.append(item)
    except Exception:
        pass

total = len(passed) + len(failed) + len(errored) + len(skipped)

RED = '\033[0;31m'
GREEN = '\033[0;32m'
YELLOW = '\033[1;33m'
BLUE = '\033[0;34m'
CYAN = '\033[0;36m'
BOLD = '\033[1m'
DIM = '\033[2m'
NC = '\033[0m'

if total == 0:
    if exit_code != 0:
        print(f'{RED}{BOLD}  ╔══════════════════════════════════════════════════════════╗{NC}')
        print(f'{RED}{BOLD}  ║  ✗ ÉCHEC : Erreur lors de la compilation des tests.      ║{NC}')
        print(f'{RED}{BOLD}  ╚══════════════════════════════════════════════════════════╝{NC}')
    else:
        print(f'{YELLOW}  ⚠ Aucun test n\'a été exécuté.{NC}')
    sys.exit(0)

print('')
print(f'{BLUE}{BOLD}══════════════════════════════════════════════════════════════{NC}')
print(f'{BLUE}{BOLD}             📊 RÉCAPITULATIF DES TESTS                       {NC}')
print(f'{BLUE}{BOLD}══════════════════════════════════════════════════════════════{NC}')
print(f'  Total exécutés : {BOLD}{total}{NC}  │  Passés : {GREEN}{BOLD}{len(passed)} 🟢{NC}  │  Échoués : {RED}{BOLD}{len(failed) + len(errored)} 🔴{NC}  │  Ignorés : {YELLOW}{BOLD}{len(skipped)} 🟡{NC}')
print(f'{BLUE}──────────────────────────────────────────────────────────────{NC}')

if passed:
    print(f'\n{GREEN}{BOLD}── 🟢 TESTS PASSÉS ({len(passed)}) ─────────────────────────────────────────{NC}')
    for p in passed:
        print(f'{GREEN}  ✓ {p["simple_class"]} > {p["name"]}{NC} {DIM}({p["time"]}s){NC}')

if failed or errored:
    bad_tests = failed + errored
    print(f'\n{RED}{BOLD}── 🔴 TESTS ÉCHOUÉS OU EN ERREUR ({len(bad_tests)}) ─────────────────────────{NC}')
    for i, f in enumerate(bad_tests, 1):
        print(f'{RED}{BOLD}  [{i}] {f["simple_class"]} > {f["name"]}{NC} {DIM}({f["time"]}s){NC}')
        if f.get('location'):
            print(f'{RED}      📍 Localisation : {BOLD}{f["location"]}{NC}')
        print(f'{RED}      ❌ Exception    : {f["type"]}{NC}')
        if f.get('message'):
            msg_lines = f['message'].splitlines()
            print(f'{YELLOW}      💬 Message      : {msg_lines[0]}{NC}')
            for extra_line in msg_lines[1:]:
                print(f'{YELLOW}                        {extra_line}{NC}')
        if f.get('stack'):
            print(f'{DIM}      🔍 Trace        : {f["stack"][0]}{NC}')
            for st in f['stack'][1:]:
                print(f'{DIM}                        {st}{NC}')
        print(f'{RED}      ────────────────────────────────────────────────────────{NC}')

print('')
if len(failed) + len(errored) == 0 and exit_code == 0:
    print(f'{GREEN}{BOLD}  ╔══════════════════════════════════════════════════════════╗{NC}')
    print(f'{GREEN}{BOLD}  ║  ✓ SUCCÈS : Tous les tests ont réussi !                 ║{NC}')
    print(f'{GREEN}{BOLD}  ╚══════════════════════════════════════════════════════════╝{NC}')
else:
    print(f'{RED}{BOLD}  ╔══════════════════════════════════════════════════════════╗{NC}')
    print(f'{RED}{BOLD}  ║  ✗ ÉCHEC : {len(failed) + len(errored)} test(s) ont échoué.                     ║{NC}')
    print(f'{RED}{BOLD}  ╚══════════════════════════════════════════════════════════╝{NC}')
