#!/usr/bin/env bash
set -euo pipefail

# Kairos Dashboard - Raspberry Pi 5 Setup Script
# Run as root or with sudo

echo "=== Kairos Dashboard Pi Setup ==="

# Install OpenJDK 21 + JavaFX runtime
apt-get update
apt-get install -y openjdk-21-jdk libopenjfx-java

# Disable screen blanking and power management
if command -v xset &>/dev/null; then
    xset s off
    xset -dpms
    xset s noblank
fi

# Prevent screen saver via xorg config
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
    cp kairos-dashboard.jar /opt/kairos/
    echo "JAR deployed to /opt/kairos/"
else
    echo "WARNING: kairos-dashboard.jar not found -- copy it manually to /opt/kairos/"
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
chown -R pi:pi /opt/kairos
chmod 600 /opt/kairos/config/service-account.json 2>/dev/null || true

# Install and enable systemd service
cp kairos-dashboard.service /etc/systemd/system/
systemctl daemon-reload
systemctl enable kairos-dashboard.service
echo "Service enabled. Start with: systemctl start kairos-dashboard"

echo "=== Setup complete ==="
