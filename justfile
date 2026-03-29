# Kairos development commands

# Run all lint checks
lint:
    @echo "=== spotless ==="
    ./gradlew spotlessCheck
    @echo ""
    @echo "=== detekt ==="
    ./gradlew detekt
    @echo ""
    @echo "All clean."

# Auto-fix formatting issues
lint-fix:
    @echo "=== spotless auto-fix ==="
    ./gradlew spotlessApply
    @echo "Formatting fixed. Run 'just lint' to check for remaining issues."

# Run detekt only
detekt:
    ./gradlew detekt

# Run spotless (ktlint) only
ktlint:
    ./gradlew spotlessCheck

# Build debug APK
build:
    ./gradlew assembleDebug

# Clean build
clean:
    ./gradlew clean

# Run all checks (lint + build)
check: lint build
