#!/bin/bash
# Post-edit lint hook for Kairos project

set -e

case "$CLAUDE_EDITED_FILE" in
  *.kt)
    echo "Running ktlint on Kotlin file..."
    ./gradlew ktlintFormat
    ;;
  *.kts)
    echo "Running ktlint on Gradle KTS file..."
    ./gradlew ktlintFormat
    ;;
esac
