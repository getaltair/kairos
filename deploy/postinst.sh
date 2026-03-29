#!/bin/bash
set -e
mkdir -p /opt/kairos/config
systemctl daemon-reload
systemctl enable kairos-dashboard
