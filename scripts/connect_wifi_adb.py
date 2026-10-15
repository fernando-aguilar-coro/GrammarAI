#!/usr/bin/env python3
"""
Zero-Click Wireless ADB Auto-Connect
Discovers Android devices advertising Wireless Debugging via mDNS on your Wi-Fi network
and connects to them automatically without Android Studio.
"""

import subprocess
import re
import sys
import time

# Ensure UTF-8 output on Windows console
if sys.stdout.encoding and sys.stdout.encoding.lower() != 'utf-8':
    try:
        sys.stdout.reconfigure(encoding='utf-8', errors='replace')
    except Exception:
        pass

def run_cmd(cmd):
    try:
        res = subprocess.run(cmd, capture_output=True, text=True, shell=True)
        return res.stdout.strip()
    except Exception as e:
        return f"Error: {e}"

def clean_offline_devices():
    output = run_cmd("adb devices")
    for line in output.splitlines():
        if "offline" in line:
            dev = line.split()[0]
            print(f"[*] Cleaning up offline connection: {dev}")
            run_cmd(f"adb disconnect {dev}")

def find_mdns_devices(retries=4):
    ip_port_pattern = re.compile(r"(\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}):(\d{4,5})")
    for attempt in range(1, retries + 1):
        print(f"[*] Searching for wireless Android devices via mDNS (attempt {attempt}/{retries})...")
        output = run_cmd("adb mdns services")
        found = []
        for line in output.splitlines():
            # Look for active connection services (_adb-tls-connect or _adb)
            if "_adb-tls-connect" in line or "_adb." in line:
                match = ip_port_pattern.search(line)
                if match:
                    found.append((line.split()[0], match.group(0)))
        if found:
            return found
        time.sleep(1.2)
    return []

def main():
    print("=" * 60)
    print("   [+] GrammarAI Wireless ADB Auto-Connector")
    print("=" * 60)

    # 1. Clean stale connections
    clean_offline_devices()

    # 2. Check if a device is already active
    devices_output = run_cmd("adb devices")
    active_devices = [line.split()[0] for line in devices_output.splitlines() if line.endswith("device")]

    if active_devices:
        print(f"[+] Active devices detected: {', '.join(active_devices)}")

    # 3. Discover dynamic ports via mDNS
    discovered = find_mdns_devices()

    if not discovered:
        print("\n[!] No devices found automatically via mDNS.")
        print("Tips:")
        print("  1. Make sure your phone and PC are on the SAME Wi-Fi network.")
        print("  2. In your phone's Developer Options, turn 'Wireless debugging' OFF and ON once.")
        print("  3. Run this script again.\n")
        sys.exit(1)

    for name, addr in discovered:
        print(f"[+] Discovered device '{name}' at {addr}")
        res = run_cmd(f"adb connect {addr}")
        print(f"   -> {res}")

    # 4. Final Verification
    print("\n--- Current ADB Devices ---")
    final_devices = run_cmd("adb devices")
    print(final_devices)

    if "device\n" in final_devices or final_devices.endswith("device"):
        print("\n[SUCCESS] Successfully connected to wireless Android device!")
        
        # Check if user wants to build and launch immediately
        if "--run" in sys.argv or "-r" in sys.argv:
            print("\n[*] Building and launching app on device...")
            subprocess.run(
                '.\\gradlew installDebug && adb shell am start -n "com.app.grammarai/com.app.grammarai.MainActivity"',
                shell=True
            )
    else:
        print("\n[!] Could not establish an active connection. Check pairing permissions on phone.")

if __name__ == "__main__":
    main()
