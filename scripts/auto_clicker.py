#!/usr/bin/env python3

# needs evdev to be installed; e.g. sudo pacman -S python-evdev

import time
import random
import argparse
from evdev import UInput, ecodes as e

def run_test(ui, mode, cps, duration):
    delay = 1.0 / cps
    print(f"\n[!] Target: {cps} CPS | Mode: {mode} | Duration: {duration}s")

    total_clicks = int(cps * duration)
    start_time = time.time()
    clicks_sent = 0

    for i in range(total_clicks):
        # Press
        ui.write(e.EV_KEY, e.BTN_LEFT, 1)
        ui.syn()

        # Hold 10ms
        time.sleep(0.01)

        # Release
        ui.write(e.EV_KEY, e.BTN_LEFT, 0)
        ui.syn()

        if mode == "perfect":
            wait = max(0, delay - 0.01)

        elif mode == "randomized":
            # Adds small jitter (±4ms) to try and beat the 4ms stdDev threshold
            jitter = random.uniform(-0.004, 0.004)
            wait = max(0, (delay + jitter) - 0.01)

        elif mode == "laggy":
            # Heavy variance to test "false positive" protection
            if random.random() < 0.15: # Random lag spikes
                wait = delay + random.uniform(0.05, 0.15)
            else:
                wait = max(0.005, delay + random.uniform(-0.02, 0.02) - 0.01)

        else:
            wait = 0

        time.sleep(wait)
        clicks_sent += 1

    end_time = time.time()
    actual_cps = clicks_sent / (end_time - start_time)
    print(f"[✓] Finished. Actual average CPS: {actual_cps:.2f}")

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Linux Anti-Cheat Tester")
    parser.add_argument("--cps", type=int, default=20, help="Target clicks per second")
    parser.add_argument("--duration", type=int, default=5, help="Test duration in seconds")
    parser.add_argument("--mode", type=str, default="perfect",
                        choices=["perfect", "randomized", "laggy"], help="Simulation mode")

    args = parser.parse_args()

    cap = {
        e.EV_KEY: [e.BTN_LEFT, e.BTN_RIGHT],
        e.EV_REL: [e.REL_X, e.REL_Y]
    }

    try:
        with UInput(cap, name='virtual-mouse') as ui:
            print("Device ready. 3s to switch to Minecraft...")
            time.sleep(3)
            run_test(ui, args.mode, args.cps, args.duration)
    except PermissionError:
        print("Error: You must run this script with 'sudo' to access /dev/uinput")
