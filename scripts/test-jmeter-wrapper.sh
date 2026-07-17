#!/usr/bin/env bash

set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
WRAPPER="$PROJECT_ROOT/scripts/jmeter.sh"
TEST_ROOT="$(mktemp -d)"
trap 'rm -r "$TEST_ROOT"' EXIT

create_fake_jmeter() {
  local install_root="$1"
  local executable="$install_root/bin/jmeter"

  mkdir -p "$install_root/bin"
  printf '%s\n' \
    '#!/usr/bin/env bash' \
    'printf "fake-jmeter:%s\n" "$*"' \
    'printf "WARNING: package sun.awt.X11 is not in java.desktop\n" >&2' \
    'printf "real diagnostic\n" >&2' \
    > "$executable"
  chmod +x "$executable"
}

assert_contains() {
  local output="$1"
  local expected="$2"

  if [[ "$output" != *"$expected"* ]]; then
    printf 'Expected output to contain: %s\nActual output:\n%s\n' "$expected" "$output" >&2
    exit 1
  fi
}

assert_not_contains() {
  local output="$1"
  local unexpected="$2"

  if [[ "$output" == *"$unexpected"* ]]; then
    printf 'Expected output not to contain: %s\nActual output:\n%s\n' "$unexpected" "$output" >&2
    exit 1
  fi
}

PATH_INSTALL="$TEST_ROOT/path-install"
create_fake_jmeter "$PATH_INSTALL"
path_output="$(
  PATH="$PATH_INSTALL/bin:/usr/bin:/bin" \
    JMETER_HOME="$TEST_ROOT/missing-home" \
    /bin/bash "$WRAPPER" --version 2>&1
)"
assert_contains "$path_output" 'fake-jmeter:--version'
assert_contains "$path_output" 'real diagnostic'
assert_not_contains "$path_output" 'WARNING: package sun.awt.X11'

HOME_INSTALL="$TEST_ROOT/jmeter-home"
create_fake_jmeter "$HOME_INSTALL"
home_output="$(
  PATH="/usr/bin:/bin" \
    JMETER_HOME="$HOME_INSTALL" \
    /bin/bash "$WRAPPER" --version 2>&1
)"
assert_contains "$home_output" 'fake-jmeter:--version'

set +e
missing_output="$(
  PATH="/usr/bin:/bin" \
    JMETER_HOME="$TEST_ROOT/missing-home" \
    /bin/bash "$WRAPPER" --version 2>&1
)"
missing_status=$?
set -e

if [[ "$missing_status" -eq 0 ]]; then
  printf 'Expected missing JMeter execution to fail.\n' >&2
  exit 1
fi
assert_contains "$missing_output" 'JMeter not found'

printf 'JMeter wrapper tests passed.\n'
