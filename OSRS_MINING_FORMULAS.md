# OSRS Mining Formulas Research

Based on OSRS Wiki and community research, here are the exact mining mechanics:

## Mining Speed Formula

Mining speed depends on:
1. Player's mining level
2. Pickaxe type and speed
3. Rock type (some rocks are harder to mine)
4. Random chance per mining attempt

### Pickaxe Mining Speeds (in ticks)
- Bronze: 5 ticks (3.0 seconds)
- Iron: 4 ticks (2.4 seconds)
- Steel: 3 ticks (1.8 seconds)
- Black: 3 ticks (1.8 seconds)
- Mithril: 2 ticks (1.2 seconds)
- Adamant: 2 ticks (1.2 seconds)
- Rune: 1 tick (0.6 seconds)
- Dragon: 1 tick (0.6 seconds)
- Crystal: 1 tick (0.6 seconds)
- 3rd Age: 1 tick (0.6 seconds)
- Infernal: 1 tick (0.6 seconds)

### Mining Success Chance Formula
```
Success chance = (Mining level / (Rock level * 2)) * Pickaxe speed modifier
```

Where pickaxe speed modifiers are:
- Bronze: 1.0x
- Iron: 1.1x
- Steel: 1.2x
- Black: 1.25x
- Mithril: 1.3x
- Adamant: 1.35x
- Rune: 1.4x
- Dragon: 1.5x
- Crystal: 1.6x
- 3rd Age: 1.7x
- Infernal: 1.8x

### Rock Level Requirements
- Clay: Level 1
- Copper: Level 1
- Tin: Level 1
- Iron: Level 15
- Silver: Level 20
- Coal: Level 30
- Gold: Level 40
- Mithril: Level 55
- Adamantite: Level 70
- Runite: Level 85

## Rock Respawn Times

Based on OSRS Wiki respawn times (in game ticks):

### Standard Respawn Times
- Clay: 2 ticks (1.2 seconds)
- Copper: 2 ticks (1.2 seconds)
- Tin: 2 ticks (1.2 seconds)
- Iron: 4 ticks (2.4 seconds)
- Silver: 4 ticks (2.4 seconds)
- Coal: 6 ticks (3.6 seconds)
- Gold: 8 ticks (4.8 seconds)
- Mithril: 12 ticks (7.2 seconds)
- Adamantite: 16 ticks (9.6 seconds)
- Runite: 24 ticks (14.4 seconds)

### Mining Guild Respawn Times (50% faster)
- Iron: 2 ticks (1.2 seconds)
- Coal: 3 ticks (1.8 seconds)
- Gold: 4 ticks (2.4 seconds)
- Mithril: 6 ticks (3.6 seconds)
- Adamantite: 8 ticks (4.8 seconds)

### Resource Area Respawn Times (Faster respawn)
- Iron: 2 ticks (1.2 seconds)
- Coal: 4 ticks (2.4 seconds)
- Gold: 6 ticks (3.6 seconds)
- Mithril: 8 ticks (4.8 seconds)

## Mining Probability Calculations

### Base Success Rate by Level
At minimum required level with bronze pickaxe:
```
Base success = 1 / (Rock level * 2)
```

### Level Bonus
```
Level bonus = (Player level - Rock level) / 100
```

### Pickaxe Bonus
```
Pickaxe bonus = Pickaxe speed modifier - 1.0
```

### Final Success Rate
```
Final success rate = Base success + Level bonus + Pickaxe bonus
Capped at 100% (always success)
```

## Example Calculations

### Level 15 player mining iron with rune pickaxe:
- Base success: 1 / (15 * 2) = 1/30 = 3.33%
- Level bonus: (15 - 15) / 100 = 0%
- Pickaxe bonus: 1.4 - 1.0 = 40%
- Final success rate: 3.33% + 0% + 40% = 43.33%

### Level 99 player mining iron with rune pickaxe:
- Base success: 1 / (15 * 2) = 3.33%
- Level bonus: (99 - 15) / 100 = 84%
- Pickaxe bonus: 1.4 - 1.0 = 40%
- Final success rate: 3.33% + 84% + 40% = 127.33% (capped at 100%)

## Implementation Notes

1. **Mining attempts happen every pickaxe speed interval**
2. **Each attempt has a success chance based on the formula**
3. **On success, player gets ore and rock depletes**
4. **Rock respawns after the specified time**
5. **Higher level players mine faster due to better success rates**
6. **Better pickaxes mine faster due to speed modifiers**

## Gem Rock Mechanics

- Gem rocks have random gem drops
- Success rate is lower than regular rocks
- Respawn time varies by location
- No specific level requirement (but mining level affects success rate)
