#!/usr/bin/env python3
"""
Verification script for the OSRS Mining implementation
Checks that all required files exist and are properly integrated
"""

import os
import glob

def check_file_exists(filepath, description):
    """Check if a file exists and report status"""
    if os.path.exists(filepath):
        print(f"✅ {description}: {filepath}")
        return True
    else:
        print(f"❌ {description}: {filepath} - MISSING")
        return False

def check_integration():
    """Check integration points"""
    print("=== Integration Verification ===")
    
    # Check core mining files
    mining_files = [
        ("server/game/src/main/java/com/elvarg/game/content/skill/mining/Mining.java", "Main mining entry point"),
        ("server/game/src/main/java/com/elvarg/game/content/skill/mining/MiningTask.java", "Mining task implementation"),
        ("server/game/src/main/java/com/elvarg/game/content/skill/mining/MiningRockType.java", "Rock type definitions"),
        ("server/game/src/main/java/com/elvarg/game/content/skill/mining/MiningRockRegistry.java", "Cache-driven registry"),
        ("server/game/src/main/java/com/elvarg/game/content/skill/mining/PickaxeData.java", "Pickaxe definitions"),
        ("server/game/src/main/java/com/elvarg/game/content/skill/mining/MiningFormula.java", "Mining formulas"),
        ("server/game/src/main/java/com/elvarg/game/content/skill/mining/MiningRespawnManager.java", "Rock respawn system"),
        ("server/game/src/main/java/com/elvarg/game/content/skill/mining/MiningGemTable.java", "Gem drop table"),
        ("server/game/src/main/java/com/elvarg/game/content/skill/mining/ProspectService.java", "Prospect service"),
    ]
    
    # Check cache integration files
    cache_files = [
        ("server/game/src/main/java/com/elvarg/game/content/skill/cache/ObjectActionScanner.java", "Object action scanner"),
        ("server/game/src/main/java/com/elvarg/game/content/skill/cache/CacheSkillObjectLoader.java", "Cache skill loader"),
    ]
    
    all_files = mining_files + cache_files
    
    success_count = 0
    for filepath, description in all_files:
        if check_file_exists(filepath, description):
            success_count += 1
    
    print(f"\nFiles: {success_count}/{len(all_files)} present")
    
    # Check integration in ObjectActionPacketListener
    object_action_file = "server/game/src/main/java/com/elvarg/net/packet/impl/ObjectActionPacketListener.java"
    if os.path.exists(object_action_file):
        with open(object_action_file, 'r') as f:
            content = f.read()
            if "Mining.startMining" in content:
                print("✅ Mining integration in ObjectActionPacketListener")
                success_count += 1
            else:
                print("❌ Mining integration missing in ObjectActionPacketListener")
            
            if "Mining.prospectRock" in content:
                print("✅ Prospect integration in ObjectActionPacketListener")
                success_count += 1
            else:
                print("❌ Prospect integration missing in ObjectActionPacketListener")
    
    # Check server startup integration
    game_builder_file = "server/game/src/main/java/com/elvarg/game/GameBuilder.java"
    if os.path.exists(game_builder_file):
        with open(game_builder_file, 'r') as f:
            content = f.read()
            if "CacheSkillObjectLoader" in content:
                print("✅ Cache loader integration in GameBuilder")
                success_count += 1
            else:
                print("❌ Cache loader integration missing in GameBuilder")
    
    print(f"\nTotal checks: {success_count}/{len(all_files) + 3} passed")
    
    if success_count >= len(all_files) + 2:
        print("🎉 Implementation appears to be complete!")
        return True
    else:
        print("⚠️  Some integration points may be missing")
        return False

def check_file_structure():
    """Check that the file structure is correct"""
    print("\n=== File Structure Check ===")
    
    expected_dirs = [
        "server/game/src/main/java/com/elvarg/game/content/skill/mining",
        "server/game/src/main/java/com/elvarg/game/content/skill/cache",
    ]
    
    for dirpath in expected_dirs:
        if os.path.exists(dirpath):
            print(f"✅ Directory exists: {dirpath}")
        else:
            print(f"❌ Directory missing: {dirpath}")

def main():
    """Main verification function"""
    print("OSRS Mining Implementation Verification")
    print("=" * 50)
    
    check_file_structure()
    success = check_integration()
    
    print("\n=== Next Steps ===")
    if success:
        print("✅ Implementation is ready for testing!")
        print("1. Start the Elvarg server")
        print("2. Use ::object 2090 to spawn a copper rock")
        print("3. Test mining with different pickaxes")
        print("4. Test prospecting with right-click")
        print("5. Check server logs for rock registration")
    else:
        print("⚠️  Please complete the missing integration points")
        print("Review the failed checks above and fix any issues")

if __name__ == "__main__":
    main()
