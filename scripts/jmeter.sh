#!/usr/bin/env bash
# Wrapper around jmeter that filters known harmless startup warnings from stderr.
FILTER="WARNING: package sun.awt.X11|WARN StatusConsoleListener"
jmeter "$@" 2> >(grep -Ev "$FILTER" >&2 || true)
