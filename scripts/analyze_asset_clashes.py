import os
import sys
import glob

PROJECT_ROOT = os.path.abspath('.')
modules = ['app'] + glob.glob('audio_pack_*')
assets_by_rel_path = {}

for m in modules:
    asset_dir = os.path.join(PROJECT_ROOT, m, 'src', 'main', 'assets')
    if not os.path.exists(asset_dir):
        continue
    for root, _, filenames in os.walk(asset_dir):
        for fn in filenames:
            full_path = os.path.join(root, fn)
            rel_path = os.path.relpath(full_path, asset_dir).replace('\\', '/')
            assets_by_rel_path.setdefault(rel_path, []).append((m, full_path))

clashes = {rel: locs for rel, locs in assets_by_rel_path.items() if len(locs) > 1}

print("=" * 60)
print(f"Asset Clash Analysis across {len(modules)} modules")
print(f"Total Unique Relative Assets: {len(assets_by_rel_path)}")
print(f"Total Clashing Paths: {len(clashes)}")
print("=" * 60)

if clashes:
    print("\n[FAIL] Conflicting asset paths found:")
    for rel, locs in sorted(clashes.items()):
        print(f"  {rel}: {[m for m, _ in locs]}")
    sys.exit(1)
else:
    print("\n[SUCCESS] No conflicting asset paths found across modules.")
    sys.exit(0)
