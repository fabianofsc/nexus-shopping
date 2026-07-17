#!/usr/bin/env bash
# Wrapper around jmeter that filters known harmless startup warnings from stderr.
FILTER="WARNING: package sun.awt.X11|WARN StatusConsoleListener"

if command -v jmeter >/dev/null 2>&1; then
  JMETER_BIN="$(command -v jmeter)"
elif [[ -n "${JMETER_HOME:-}" && -x "$JMETER_HOME/bin/jmeter" ]]; then
  JMETER_BIN="$JMETER_HOME/bin/jmeter"
elif [[ -x /opt/jmeter/bin/jmeter ]]; then
  JMETER_BIN="/opt/jmeter/bin/jmeter"
else
  printf '%s\n' \
    'JMeter not found. Install it or set JMETER_HOME to its installation directory.' \
    >&2
  exit 127
fi

"$JMETER_BIN" "$@" 2> >(grep -Ev "$FILTER" >&2 || true)
