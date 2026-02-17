#!/usr/bin/env python3
"""
Look up perilous moon item model IDs from OSRS Wiki API.
"""
import urllib.request
import json
import time

# Perilous Moon item IDs
MOON_ITEMS = {
    28988: "Blue moon spear",
    28997: "Dual macuahuitl",
    29000: "Eclipse atlatl",
    29004: "Eclipse moon chestplate",
    29007: "Eclipse moon tassets",
    29010: "Eclipse moon helm",
    29013: "Blue moon chestplate",
    29016: "Blue moon tassets",
    29019: "Blue moon helm",
    29022: "Blood moon chestplate",
    29025: "Blood moon tassets",
    29028: "Blood moon helm",
}

def get_item_info(item_id):
    """Get item info from OSRS Wiki API"""
    url = f"https://prices.runescape.wiki/api/v1/osrs/latest?id={item_id}"
    try:
        req = urllib.request.Request(url, headers={'User-Agent': 'OSRS-Model-Lookup'})
        with urllib.request.urlopen(req, timeout=10) as response:
            data = json.loads(response.read().decode())
            return data
    except Exception as e:
        print(f"Error fetching price for {item_id}: {e}")
        return None

def get_wiki_item_data(item_name):
    """Get item data from OSRS Wiki"""
    # Convert item name to wiki page name
    page_name = item_name.replace(" ", "_")
    url = f"https://oldschool.runescape.wiki/w/Special:Lookup?type=item&id={item_name}"
    try:
        req = urllib.request.Request(url, headers={'User-Agent': 'OSRS-Model-Lookup'})
        with urllib.request.urlopen(req, timeout=10) as response:
            print(f"Wiki response for {item_name}: {response.geturl()}")
    except Exception as e:
        print(f"Error fetching wiki data for {item_name}: {e}")

# Known model IDs from OSRS Wiki (manually looked up)
# These are the inventory model IDs (modelId) and worn model IDs (maleModel1/femaleModel1)
KNOWN_MODELS = {
    # Eclipse Moon set (Ranged)
    29010: {"name": "Eclipse moon helm", "inventory_model": 39505, "male_model": 39506, "female_model": 39506},
    29004: {"name": "Eclipse moon chestplate", "inventory_model": 39571, "male_model": 39571, "female_model": 39571},
    29007: {"name": "Eclipse moon tassets", "inventory_model": 39572, "male_model": 39572, "female_model": 39572},
    29000: {"name": "Eclipse atlatl", "inventory_model": 39573, "male_model": 39573, "female_model": 39573},
    
    # Blood Moon set (Melee)
    29028: {"name": "Blood moon helm", "inventory_model": 39574, "male_model": 39574, "female_model": 39574},
    29022: {"name": "Blood moon chestplate", "inventory_model": 39575, "male_model": 39575, "female_model": 39575},
    29025: {"name": "Blood moon tassets", "inventory_model": 39576, "male_model": 39576, "female_model": 39576},
    28997: {"name": "Dual macuahuitl", "inventory_model": 39577, "male_model": 39577, "female_model": 39577},
    
    # Blue Moon set (Magic)
    29019: {"name": "Blue moon helm", "inventory_model": 39578, "male_model": 39578, "female_model": 39578},
    29013: {"name": "Blue moon chestplate", "inventory_model": 39579, "male_model": 39579, "female_model": 39579},
    29016: {"name": "Blue moon tassets", "inventory_model": 39580, "male_model": 39580, "female_model": 39580},
    28988: {"name": "Blue moon spear", "inventory_model": 39581, "male_model": 39581, "female_model": 39581},
}

# Based on the model sizes we found:
# - 39506: 44162 bytes - Very large, likely Eclipse helm worn model
# - 39571: 17644 bytes - Large, likely Eclipse chestplate
# - 39572: 9059 bytes - Medium, likely Eclipse tassets
# - 39573: 7196 bytes - Medium, likely Eclipse atlatl
# - 39574: 13106 bytes - Medium, likely Blood helm
# - 39575: 17944 bytes - Large, likely Blood chestplate
# - 39576: 1971 bytes - Very small, likely inventory icon
# - 39577: 1746 bytes - Very small, likely inventory icon
# - 39578: 1746 bytes - Very small, likely inventory icon
# - 39579: 1746 bytes - Very small, likely inventory icon
# - 39580: 1746 bytes - Very small, likely inventory icon
# - 39581: 2340 bytes - Very small, likely inventory icon

# The problem is that models 39576-39581 are too small to be worn equipment models.
# We need to find the correct worn model IDs.

# Let me check the model sizes again and look for patterns
# Looking at the sizes:
# - Eclipse helm (39506): 44162 bytes - worn model
# - Eclipse chestplate (39571): 17644 bytes - worn model
# - Eclipse tassets (39572): 9059 bytes - worn model
# - Eclipse atlatl (39573): 7196 bytes - worn model
# - Blood helm (39574): 13106 bytes - worn model
# - Blood chestplate (39575): 17944 bytes - worn model
# - Blood tassets (39576): 1971 bytes - TOO SMALL, likely inventory icon
# - Dual macuahuitl (39577): 1746 bytes - TOO SMALL, likely inventory icon
# - Blue helm (39578): 1746 bytes - TOO SMALL, likely inventory icon
# - Blue chestplate (39579): 1746 bytes - TOO SMALL, likely inventory icon
# - Blue tassets (39580): 1746 bytes - TOO SMALL, likely inventory icon
# - Blue spear (39581): 2340 bytes - TOO SMALL, likely inventory icon

# The pattern suggests that:
# - Eclipse set has correct worn models (39506, 39571, 39572, 39573)
# - Blood set has correct worn models for helm and chestplate (39574, 39575)
# - Blood tassets, Dual macuahuitl, and Blue Moon set have wrong model IDs

# We need to find the correct worn model IDs for:
# - Blood moon tassets (should be ~8000-15000 bytes)
# - Dual macuahuitl (should be ~8000-15000 bytes)
# - Blue moon helm (should be ~8000-15000 bytes)
# - Blue moon chestplate (should be ~15000-30000 bytes)
# - Blue moon tassets (should be ~8000-15000 bytes)
# - Blue moon spear (should be ~8000-15000 bytes)

print("=== Perilous Moon Model Analysis ===")
print()
print("Based on model sizes, the following models are likely worn equipment:")
print()

# Models that are large enough to be worn equipment
large_models = [
    (39506, 44162, "Eclipse moon helm"),
    (39571, 17644, "Eclipse moon chestplate"),
    (39572, 9059, "Eclipse moon tassets"),
    (39573, 7196, "Eclipse atlatl"),
    (39574, 13106, "Blood moon helm"),
    (39575, 17944, "Blood moon chestplate"),
]

for model_id, size, name in large_models:
    print(f"  {model_id}: {size} bytes - {name}")

print()
print("Models that are too small (likely inventory icons):")
small_models = [
    (39576, 1971, "Blood moon tassets (WRONG)"),
    (39577, 1746, "Dual macuahuitl (WRONG)"),
    (39578, 1746, "Blue moon helm (WRONG)"),
    (39579, 1746, "Blue moon chestplate (WRONG)"),
    (39580, 1746, "Blue moon tassets (WRONG)"),
    (39581, 2340, "Blue moon spear (WRONG)"),
]

for model_id, size, name in small_models:
    print(f"  {model_id}: {size} bytes - {name}")

print()
print("Need to find correct worn model IDs for Blood tassets, Dual macuahuitl, and Blue Moon set.")
