#!/usr/bin/env bash
set -euo pipefail

# Kairos Dashboard - Raspberry Pi 5 Setup Script
# Run as root or with sudo
# Set KAIROS_USER env var if your Pi uses a different username

KAIROS_USER="${KAIROS_USER:-pi}"

echo "=== Kairos Dashboard Pi Setup ==="
echo "Using system user: ${KAIROS_USER}"

# Install OpenJDK 21 + JavaFX runtime
apt-get update
apt-get install -y openjdk-21-jdk libopenjfx-java

# Disable screen blanking and power management
# NOTE: xset only works from the user's X session (not from sudo/root shell).
# The xorg.conf approach below is the reliable fallback for persistent config.
if command -v xset &>/dev/null; then
    xset s off
    xset -dpms
    xset s noblank
fi

# Prevent screen saver via xorg config (reliable method that persists across reboots)
mkdir -p /etc/X11/xorg.conf.d
cat > /etc/X11/xorg.conf.d/10-blanking.conf << 'EOF'
Section "ServerFlags"
    Option "BlankTime" "0"
    Option "StandbyTime" "0"
    Option "SuspendTime" "0"
    Option "OffTime" "0"
EndSection
EOF

# Create deployment directory
mkdir -p /opt/kairos/config

# Deploy JAR (assumes it's in the current directory)
if [ -f "kairos-dashboard.jar" ]; then
    cp kairos-dashboard.jar /opt/kairos/dashboard.jar
    echo "JAR deployed to /opt/kairos/dashboard.jar"
else
    echo "WARNING: kairos-dashboard.jar not found -- copy it manually to /opt/kairos/dashboard.jar"
fi

# Deploy config
if [ -f "dashboard.properties" ]; then
    cp dashboard.properties /opt/kairos/
    echo "Config deployed to /opt/kairos/"
else
    echo "WARNING: dashboard.properties not found -- create /opt/kairos/dashboard.properties"
    cat > /opt/kairos/dashboard.properties << 'EOF'
firebase.service_account_path=/opt/kairos/config/service-account.json
firebase.user_id=REPLACE_WITH_YOUR_FIREBASE_USER_ID
dashboard.fullscreen=true
dashboard.width=1920
dashboard.height=1080
server.port=8888
EOF
fi

# Set permissions
chown -R "${KAIROS_USER}:${KAIROS_USER}" /opt/kairos

if [ -f "/opt/kairos/config/service-account.json" ]; then
    chmod 600 /opt/kairos/config/service-account.json
    echo "Service account permissions secured"
else
    echo "NOTE: /opt/kairos/config/service-account.json not found yet."
    echo "      After deploying it, run: chmod 600 /opt/kairos/config/service-account.json"
fi

# Install and enable systemd service
if [ ! -f "kairos-dashboard.service" ]; then
    echo "ERROR: kairos-dashboard.service not found in the current directory."
    echo "       Run this script from the deploy/ directory or copy the service file here."
    exit 1
fi

cp kairos-dashboard.service /etc/systemd/system/
systemctl daemon-reload
systemctl enable kairos-dashboard.service
echo "Service enabled. Start with: systemctl start kairos-dashboard"

echo "=== Setup complete ==="
