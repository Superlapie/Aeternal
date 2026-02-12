package com.runescape.util;

import com.runescape.cache.def.ItemDefinition;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

public final class ItemPriceLookup {

    private static final String PRICE_RESOURCE = "/prices/osrs_ge_prices.csv";
    private static final Map<Integer, Integer> GE_PRICES = new HashMap<>();
    private static boolean loaded = false;

    private ItemPriceLookup() {
        // Utility class.
    }

    public static int getGePrice(int itemId) {
        ensureLoaded();
        return GE_PRICES.getOrDefault(itemId, 0);
    }

    public static long getBestPrice(ItemDefinition itemDefinition, int amount) {
        int gePrice = getGePrice(itemDefinition.id);
        int basePrice = gePrice > 0 ? gePrice : Math.max(0, itemDefinition.value);
        return (long) basePrice * Math.max(1, amount);
    }

    private static synchronized void ensureLoaded() {
        if (loaded) {
            return;
        }
        loaded = true;

        try (InputStream in = ItemPriceLookup.class.getResourceAsStream(PRICE_RESOURCE)) {
            if (in == null) {
                System.out.println("ItemPriceLookup: missing " + PRICE_RESOURCE + ", using item definition values.");
                return;
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.isEmpty() || line.charAt(0) == '#') {
                        continue;
                    }
                    int comma = line.indexOf(',');
                    if (comma <= 0 || comma == line.length() - 1) {
                        continue;
                    }

                    int id = Integer.parseInt(line.substring(0, comma));
                    int price = Integer.parseInt(line.substring(comma + 1));
                    if (price > 0) {
                        GE_PRICES.put(id, price);
                    }
                }
            }

            System.out.println("ItemPriceLookup: loaded " + GE_PRICES.size() + " GE prices.");
        } catch (IOException | NumberFormatException e) {
            System.out.println("ItemPriceLookup: failed to load GE prices, using item definition values.");
        }
    }
}
