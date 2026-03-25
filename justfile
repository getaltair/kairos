# Kairos development commands

# Run all lint checks
lint:
    @echo "=== ktlint ==="
    ktlint -R .tools/ktlint-compose-rules.jar --relative
    @echo ""
    @echo "=== detekt ==="
    java -jar .tools/detekt-cli.jar \
        --config config/detekt.yml \
        --plugins .tools/detekt-compose-rules.jar \
        --input "core/,feature/,app/src/,sync/src/"
    @echo ""
    @echo "✅ All clean."

# Auto-fix formatting issues
lint-fix:
    @echo "=== ktlint auto-fix ==="
    ktlint -R .tools/ktlint-compose-rules.jar --format --relative
    @echo "✅ Formatting fixed. Run 'just lint' to check for remaining issues."

# Run detekt only
detekt:
    java -jar .tools/detekt-cli.jar \
        --config config/detekt.yml \
        --plugins .tools/detekt-compose-rules.jar \
        --input "core/,feature/,app/src/,sync/src/"

# Run ktlint only
ktlint:
    ktlint -R .tools/ktlint-compose-rules.jar --relative

# Build debug APK
build:
    ./gradlew assembleDebug

# Clean build
clean:
    ./gradlew clean

# Run all checks (lint + build)
check: lint build
