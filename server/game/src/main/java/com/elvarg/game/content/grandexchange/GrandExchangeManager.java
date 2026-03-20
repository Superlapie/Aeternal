package com.elvarg.game.content.grandexchange;

import com.elvarg.game.definition.ItemDefinition;
import com.elvarg.game.entity.impl.player.Player;
import com.elvarg.game.model.Item;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class GrandExchangeManager {

    public static final int COINS_ID = 995;
    public static final int CURRENT_TAX_PERCENT = 2;
    public static final int LEGACY_TAX_PERCENT = 1;
    public static final int MAX_TAX_PER_ITEM = 5_000_000;
    public static final long BUY_LIMIT_WINDOW_MILLIS = 4L * 60L * 60L * 1000L;

    private static final Path SAVE_PATH = Path.of("../data/grand_exchange.json");
    private static final Path BUY_LIMITS_PATH = Path.of("../data/ge_buy_limits.json");
    private static final Path GE_TRADEABLE_LIST_PATH = Path.of("../GETradeable.txt");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final GrandExchangeManager INSTANCE = new GrandExchangeManager();

    private final List<GrandExchangeOffer> activeOffers = new ArrayList<>();
    private final Map<String, List<GrandExchangeCollectionEntry>> collectionBox = new HashMap<>();
    private final Map<String, Map<Integer, BuyLimitWindow>> buyLimitWindows = new HashMap<>();
    private final Map<Integer, Integer> buyLimits = new HashMap<>();
    private final Set<Integer> taxExemptItemIds = new HashSet<>();
    private final Set<String> geTradeableNames = new HashSet<>();
    private int nextOfferId = 1;

    private GrandExchangeManager() {
    }

    public static GrandExchangeManager getInstance() {
        return INSTANCE;
    }

    public synchronized void load() {
        try {
            File file = SAVE_PATH.toFile();
            if (file.exists()) {
                try (Reader reader = new FileReader(file, StandardCharsets.UTF_8)) {
                    Type type = new TypeToken<GrandExchangeSaveState>() {
                    }.getType();
                    GrandExchangeSaveState state = GSON.fromJson(reader, type);
                    if (state != null) {
                        nextOfferId = Math.max(1, state.nextOfferId);
                        activeOffers.clear();
                        if (state.activeOffers != null) {
                            activeOffers.addAll(state.activeOffers);
                        }
                        for (GrandExchangeOffer offer : activeOffers) {
                            if (offer.getTaxRatePercent() <= 0) {
                                offer.setTaxRatePercent(LEGACY_TAX_PERCENT);
                            }
                        }
                        collectionBox.clear();
                        if (state.collectionBox != null) {
                            collectionBox.putAll(state.collectionBox);
                        }
                        buyLimitWindows.clear();
                        if (state.buyLimitWindows != null) {
                            buyLimitWindows.putAll(state.buyLimitWindows);
                        }
                    }
                }
            }
            loadBuyLimits();
            loadTaxExemptItems();
            loadGeTradeableNames();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public synchronized void save() {
        try {
            Files.createDirectories(SAVE_PATH.getParent());
            GrandExchangeSaveState state = new GrandExchangeSaveState();
            state.nextOfferId = nextOfferId;
            state.activeOffers = new ArrayList<>(activeOffers);
            state.collectionBox = new HashMap<>(collectionBox);
            state.buyLimitWindows = new HashMap<>(buyLimitWindows);

            Path tmp = SAVE_PATH.resolveSibling("grand_exchange.json.tmp");
            try (BufferedWriter writer = Files.newBufferedWriter(tmp, StandardCharsets.UTF_8)) {
                writer.write(GSON.toJson(state));
            }
            Files.move(tmp, SAVE_PATH, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public synchronized Optional<GrandExchangeOffer> getOffer(String owner, int slot) {
        return activeOffers.stream()
                .filter(o -> o.getOwner().equalsIgnoreCase(owner))
                .filter(o -> o.getSlot() == slot)
                .findFirst();
    }

    public synchronized List<GrandExchangeOffer> getOffersForOwner(String owner) {
        return activeOffers.stream()
                .filter(o -> o.getOwner().equalsIgnoreCase(owner))
                .collect(Collectors.toList());
    }

    public synchronized boolean placeBuyOffer(Player player, int slot, int itemId, int amount, int price) {
        if (amount <= 0 || price <= 0) {
            return false;
        }
        if (!ItemDefinition.definitions.containsKey(itemId)) {
            return false;
        }
        if (!isTradeableInGe(itemId)) {
            player.getPacketSender().sendMessage("That item cannot be traded on the Grand Exchange.");
            return false;
        }
        int buyLimit = getBuyLimit(itemId);
        int remainingLimit = getRemainingBuyLimit(player.getUsername(), itemId, buyLimit);
        if (remainingLimit <= 0) {
            player.getPacketSender().sendMessage("You've reached the 4-hour buy limit for this item.");
            return false;
        }
        if (amount > remainingLimit) {
            player.getPacketSender().sendMessage("You can only buy " + remainingLimit + " more of this item right now.");
            return false;
        }
        long requiredCoins = (long) amount * price;
        if (requiredCoins <= 0 || requiredCoins > Integer.MAX_VALUE) {
            return false;
        }
        if (player.getInventory().getAmount(COINS_ID) < requiredCoins) {
            return false;
        }
        if (getOffer(player.getUsername(), slot).isPresent()) {
            return false;
        }

        player.getInventory().delete(COINS_ID, (int) requiredCoins);
        GrandExchangeOffer newOffer = new GrandExchangeOffer(nextOfferId++, player.getUsername(), slot, itemId, amount, price, CURRENT_TAX_PERCENT, GrandExchangeOfferType.BUY);
        activeOffers.add(newOffer);
        matchOffer(newOffer);
        save();
        return true;
    }

    public synchronized boolean placeSellOffer(Player player, int slot, int itemId, int amount, int price) {
        if (amount <= 0 || price <= 0) {
            return false;
        }
        if (!ItemDefinition.definitions.containsKey(itemId)) {
            return false;
        }
        if (!isTradeableInGe(itemId)) {
            player.getPacketSender().sendMessage("That item cannot be traded on the Grand Exchange.");
            return false;
        }
        if (player.getInventory().getAmount(itemId) < amount) {
            return false;
        }
        if (getOffer(player.getUsername(), slot).isPresent()) {
            return false;
        }

        player.getInventory().delete(itemId, amount);
        GrandExchangeOffer newOffer = new GrandExchangeOffer(nextOfferId++, player.getUsername(), slot, itemId, amount, price, CURRENT_TAX_PERCENT, GrandExchangeOfferType.SELL);
        activeOffers.add(newOffer);
        matchOffer(newOffer);
        save();
        return true;
    }

    public synchronized boolean abortOffer(Player player, int slot) {
        Optional<GrandExchangeOffer> offerOpt = getOffer(player.getUsername(), slot);
        if (offerOpt.isEmpty()) {
            return false;
        }
        GrandExchangeOffer offer = offerOpt.get();
        offer.setActive(false);
        int remaining = offer.getRemainingAmount();
        if (remaining > 0) {
            if (offer.getType() == GrandExchangeOfferType.BUY) {
                long remainingCoins = (long) remaining * offer.getPrice();
                if (remainingCoins > 0 && remainingCoins <= Integer.MAX_VALUE) {
                    addToCollection(offer.getOwner(), COINS_ID, (int) remainingCoins);
                }
            } else {
                addToCollection(offer.getOwner(), offer.getItemId(), remaining);
            }
        }
        save();
        return true;
    }

    public synchronized void collect(Player player) {
        List<GrandExchangeCollectionEntry> items = collectionBox.get(player.getUsername().toLowerCase());
        if (items == null || items.isEmpty()) {
            player.getPacketSender().sendMessage("There is nothing to collect.");
            return;
        }

        for (GrandExchangeCollectionEntry entry : items) {
            if (entry.getAmount() <= 0) {
                continue;
            }
            int fit = getFittableAmount(player, entry.getItemId(), entry.getAmount());
            if (fit <= 0) {
                continue;
            }
            player.getInventory().add(entry.getItemId(), fit);
            entry.setAmount(entry.getAmount() - fit);
        }

        items.removeIf(e -> e.getAmount() <= 0);
        if (items.isEmpty()) {
            collectionBox.remove(player.getUsername().toLowerCase());
            clearInactiveOffers(player.getUsername());
        }
        save();
    }

    public synchronized List<GrandExchangeCollectionEntry> getCollection(String owner) {
        List<GrandExchangeCollectionEntry> entries = collectionBox.get(owner.toLowerCase());
        if (entries == null) {
            return List.of();
        }
        return entries.stream()
                .map(e -> new GrandExchangeCollectionEntry(e.getItemId(), e.getAmount()))
                .collect(Collectors.toList());
    }

    public synchronized boolean hasPendingCollection(String owner) {
        List<GrandExchangeCollectionEntry> entries = collectionBox.get(owner.toLowerCase());
        return entries != null && entries.stream().anyMatch(e -> e.getAmount() > 0);
    }

    public List<Integer> findItemByName(String syntax, int maxResults) {
        String normalized = syntax == null ? "" : syntax.trim().toLowerCase();
        if (normalized.isEmpty()) {
            return List.of();
        }
        List<Integer> results = new ArrayList<>();
        Set<String> seenNames = new HashSet<>();
        List<Map.Entry<Integer, ItemDefinition>> entries = new ArrayList<>(ItemDefinition.definitions.entrySet());
        entries.sort(Comparator.comparingInt(Map.Entry::getKey));
        for (Map.Entry<Integer, ItemDefinition> defEntry : entries) {
            int itemId = defEntry.getKey();
            ItemDefinition def = defEntry.getValue();
            if (def == null || def.getName() == null || def.getName().trim().isEmpty()) {
                continue;
            }
            if (!isTradeableInGe(itemId)) {
                continue;
            }
            // Prefer canonical unnoted entries in search to avoid duplicates such as Shark.
            if (def.isNoted()) {
                continue;
            }
            String name = def.getName().trim();
            if (!name.toLowerCase().contains(normalized)) {
                continue;
            }
            String key = name.toLowerCase();
            if (!seenNames.add(key)) {
                continue;
            }
            results.add(itemId);
            if (results.size() >= maxResults) {
                break;
            }
        }
        return results;
    }

    public Integer findItemByExactName(String name) {
        if (name == null) {
            return null;
        }
        String normalized = name.trim().toLowerCase();
        if (normalized.isEmpty()) {
            return null;
        }
        Integer notedFallback = null;
        List<Map.Entry<Integer, ItemDefinition>> entries = new ArrayList<>(ItemDefinition.definitions.entrySet());
        entries.sort(Comparator.comparingInt(Map.Entry::getKey));
        for (Map.Entry<Integer, ItemDefinition> defEntry : entries) {
            int itemId = defEntry.getKey();
            ItemDefinition def = defEntry.getValue();
            if (def == null || def.getName() == null) {
                continue;
            }
            if (!def.getName().trim().equalsIgnoreCase(normalized)) {
                continue;
            }
            if (!isTradeableInGe(itemId)) {
                continue;
            }
            if (!def.isNoted()) {
                return itemId;
            }
            if (notedFallback == null) {
                notedFallback = itemId;
            }
        }
        return notedFallback;
    }

    private int getFittableAmount(Player player, int itemId, int amount) {
        if (amount <= 0) {
            return 0;
        }
        boolean stackable = ItemDefinition.forId(itemId).isStackable();
        if (stackable) {
            int current = player.getInventory().getAmount(itemId);
            if (current > 0) {
                long maxAdd = (long) Integer.MAX_VALUE - current;
                return (int) Math.min(amount, Math.max(0L, maxAdd));
            }
            if (player.getInventory().getFreeSlots() <= 0) {
                return 0;
            }
            return amount;
        }
        return Math.min(amount, player.getInventory().getFreeSlots());
    }

    private void matchOffer(GrandExchangeOffer offer) {
        while (offer.isActive() && offer.getRemainingAmount() > 0) {
            GrandExchangeOffer bestMatch = findBestMatch(offer);
            if (bestMatch == null) {
                break;
            }

            int tradedAmount = Math.min(offer.getRemainingAmount(), bestMatch.getRemainingAmount());
            int tradePrice = bestMatch.getPrice(); // Existing offer price has priority.
            long total = (long) tradedAmount * tradePrice;
            if (total <= 0 || total > Integer.MAX_VALUE) {
                break;
            }

            settleTrade(offer, bestMatch, tradedAmount, tradePrice, (int) total);

            offer.setProcessedAmount(offer.getProcessedAmount() + tradedAmount);
            bestMatch.setProcessedAmount(bestMatch.getProcessedAmount() + tradedAmount);

            if (bestMatch.isComplete()) {
                bestMatch.setActive(false);
            }
        }

        if (offer.isComplete()) {
            offer.setActive(false);
        }
    }

    private GrandExchangeOffer findBestMatch(GrandExchangeOffer incoming) {
        if (incoming.getType() == GrandExchangeOfferType.BUY) {
            return activeOffers.stream()
                    .filter(GrandExchangeOffer::isActive)
                    .filter(o -> o.getType() == GrandExchangeOfferType.SELL)
                    .filter(o -> o.getItemId() == incoming.getItemId())
                    .filter(o -> o.getPrice() <= incoming.getPrice())
                    .sorted(Comparator.comparingInt(GrandExchangeOffer::getPrice).thenComparingInt(GrandExchangeOffer::getId))
                    .findFirst()
                    .orElse(null);
        }
        return activeOffers.stream()
                .filter(GrandExchangeOffer::isActive)
                .filter(o -> o.getType() == GrandExchangeOfferType.BUY)
                .filter(o -> o.getItemId() == incoming.getItemId())
                .filter(o -> o.getPrice() >= incoming.getPrice())
                .sorted(Comparator.comparingInt(GrandExchangeOffer::getPrice).reversed().thenComparingInt(GrandExchangeOffer::getId))
                .findFirst()
                .orElse(null);
    }

    private void settleTrade(GrandExchangeOffer incoming, GrandExchangeOffer existing, int qty, int tradePrice, int total) {
        if (incoming.getType() == GrandExchangeOfferType.BUY) {
            addToCollection(incoming.getOwner(), incoming.getItemId(), qty);
            recordBoughtAmount(incoming.getOwner(), incoming.getItemId(), qty);
            int buyerLimitPrice = incoming.getPrice();
            int refundPerItem = Math.max(0, buyerLimitPrice - tradePrice);
            long refund = (long) refundPerItem * qty;
            if (refund > 0 && refund <= Integer.MAX_VALUE) {
                addToCollection(incoming.getOwner(), COINS_ID, (int) refund);
            }

            int sellerProceeds = applyTax(existing, tradePrice, qty, total);
            addToCollection(existing.getOwner(), COINS_ID, sellerProceeds);
        } else {
            addToCollection(incoming.getOwner(), COINS_ID, applyTax(incoming, tradePrice, qty, total));
            addToCollection(existing.getOwner(), existing.getItemId(), qty);
            recordBoughtAmount(existing.getOwner(), existing.getItemId(), qty);

            int buyerLimitPrice = existing.getPrice();
            int refundPerItem = Math.max(0, buyerLimitPrice - tradePrice);
            long refund = (long) refundPerItem * qty;
            if (refund > 0 && refund <= Integer.MAX_VALUE) {
                addToCollection(existing.getOwner(), COINS_ID, (int) refund);
            }
        }
    }

    private int applyTax(GrandExchangeOffer sellOffer, int tradePrice, int qty, int gross) {
        if (sellOffer == null || taxExemptItemIds.contains(sellOffer.getItemId())) {
            return gross;
        }
        int rate = Math.max(0, sellOffer.getTaxRatePercent());
        if (rate <= 0) {
            return gross;
        }
        int perItemTax = (tradePrice * rate) / 100;
        if (perItemTax <= 0) {
            return gross;
        }
        perItemTax = Math.min(MAX_TAX_PER_ITEM, perItemTax);
        long totalTax = (long) perItemTax * qty;
        int tax = (int) Math.min((long) gross, totalTax);
        return gross - Math.max(0, tax);
    }

    private void addToCollection(String owner, int itemId, int amount) {
        if (amount <= 0) {
            return;
        }
        String key = owner.toLowerCase();
        List<GrandExchangeCollectionEntry> entries = collectionBox.computeIfAbsent(key, k -> new ArrayList<>());
        GrandExchangeCollectionEntry existing = entries.stream()
                .filter(e -> e.getItemId() == itemId)
                .findFirst()
                .orElse(null);
        if (existing == null) {
            entries.add(new GrandExchangeCollectionEntry(itemId, amount));
        } else {
            long newAmount = (long) existing.getAmount() + amount;
            existing.setAmount((int) Math.min(Integer.MAX_VALUE, newAmount));
        }
    }

    private void clearInactiveOffers(String owner) {
        activeOffers.removeIf(o -> o.getOwner().equalsIgnoreCase(owner) && !o.isActive());
    }

    public synchronized int getEffectiveTaxRatePercent(int itemId) {
        return taxExemptItemIds.contains(itemId) ? 0 : CURRENT_TAX_PERCENT;
    }

    public synchronized int getSellNetTotal(int itemId, int qty, int pricePerItem) {
        qty = Math.max(0, qty);
        pricePerItem = Math.max(0, pricePerItem);
        long grossLong = (long) qty * pricePerItem;
        if (grossLong <= 0) {
            return 0;
        }
        int gross = (int) Math.min(Integer.MAX_VALUE, grossLong);
        GrandExchangeOffer temp = new GrandExchangeOffer(-1, "preview", 0, itemId, qty, pricePerItem, CURRENT_TAX_PERCENT, GrandExchangeOfferType.SELL);
        return applyTax(temp, pricePerItem, qty, gross);
    }

    private void loadBuyLimits() {
        buyLimits.clear();
        try {
            File file = BUY_LIMITS_PATH.toFile();
            if (!file.exists()) {
                return;
            }
            try (Reader reader = new FileReader(file, StandardCharsets.UTF_8)) {
                JsonObject root = GSON.fromJson(reader, JsonObject.class);
                if (root == null) {
                    return;
                }
                for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
                    int itemId = Integer.parseInt(entry.getKey());
                    int limit = entry.getValue().getAsInt();
                    if (limit > 0) {
                        buyLimits.put(itemId, limit);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private int getBuyLimit(int itemId) {
        return buyLimits.getOrDefault(itemId, Integer.MAX_VALUE);
    }

    private int getRemainingBuyLimit(String owner, int itemId, int limit) {
        if (limit == Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        long now = Instant.now().toEpochMilli();
        BuyLimitWindow window = getOrCreateBuyWindow(owner, itemId, now);
        if (now - window.windowStartMs >= BUY_LIMIT_WINDOW_MILLIS) {
            window.windowStartMs = now;
            window.boughtAmount = 0;
        }
        return Math.max(0, limit - window.boughtAmount);
    }

    private void recordBoughtAmount(String owner, int itemId, int qty) {
        if (qty <= 0) {
            return;
        }
        int limit = getBuyLimit(itemId);
        if (limit == Integer.MAX_VALUE) {
            return;
        }
        long now = Instant.now().toEpochMilli();
        BuyLimitWindow window = getOrCreateBuyWindow(owner, itemId, now);
        if (now - window.windowStartMs >= BUY_LIMIT_WINDOW_MILLIS) {
            window.windowStartMs = now;
            window.boughtAmount = 0;
        }
        long newAmount = (long) window.boughtAmount + qty;
        window.boughtAmount = (int) Math.min(Integer.MAX_VALUE, newAmount);
    }

    private BuyLimitWindow getOrCreateBuyWindow(String owner, int itemId, long now) {
        Map<Integer, BuyLimitWindow> ownerWindows = buyLimitWindows.computeIfAbsent(owner.toLowerCase(), k -> new HashMap<>());
        return ownerWindows.computeIfAbsent(itemId, k -> {
            BuyLimitWindow w = new BuyLimitWindow();
            w.windowStartMs = now;
            return w;
        });
    }

    private boolean isTradeableInGe(int itemId) {
        ItemDefinition def = ItemDefinition.forId(itemId);
        if (def == null || def == ItemDefinition.DEFAULT || !def.isTradeable()) {
            return false;
        }
        if (geTradeableNames.isEmpty()) {
            return true;
        }
        String name = def.getName() == null ? "" : def.getName().trim().toLowerCase();
        return !name.isEmpty() && geTradeableNames.contains(name);
    }

    private void loadGeTradeableNames() {
        geTradeableNames.clear();
        try {
            File file = GE_TRADEABLE_LIST_PATH.toFile();
            if (!file.exists()) {
                System.out.println("GrandExchange whitelist not found: " + GE_TRADEABLE_LIST_PATH.toAbsolutePath());
                return;
            }
            String json = Files.readString(GE_TRADEABLE_LIST_PATH, StandardCharsets.UTF_8);
            JsonElement root = GSON.fromJson(json, JsonElement.class);
            if (root == null || !root.isJsonArray()) {
                System.out.println("GrandExchange whitelist format invalid (expected JSON array).");
                return;
            }
            JsonArray array = root.getAsJsonArray();
            for (JsonElement element : array) {
                if (element == null || !element.isJsonObject()) {
                    continue;
                }
                JsonObject obj = element.getAsJsonObject();
                if (!obj.has("name")) {
                    continue;
                }
                String name = obj.get("name").getAsString();
                if (name == null) {
                    continue;
                }
                String normalized = name.trim().toLowerCase();
                if (!normalized.isEmpty()) {
                    geTradeableNames.add(normalized);
                }
            }
            System.out.println("GrandExchange whitelist loaded: " + geTradeableNames.size() + " names.");
        } catch (Exception e) {
            System.out.println("Failed to load GrandExchange whitelist: " + e.getMessage());
        }
    }

    private void loadTaxExemptItems() {
        taxExemptItemIds.clear();
        addTaxExemptByName("Old school bond");
        addTaxExemptByName("Chisel");
        addTaxExemptByName("Gardening trowel");
        addTaxExemptByName("Glassblowing pipe");
        addTaxExemptByName("Hammer");
        addTaxExemptByName("Needle");
        addTaxExemptByName("Pestle and mortar");
        addTaxExemptByName("Rake");
        addTaxExemptByName("Saw");
        addTaxExemptByName("Secateurs");
        addTaxExemptByName("Seed dibber");
        addTaxExemptByName("Shears");
        addTaxExemptByName("Spade");
        addTaxExemptByName("Watering can(0)");
    }

    private void addTaxExemptByName(String name) {
        for (Map.Entry<Integer, ItemDefinition> entry : ItemDefinition.definitions.entrySet()) {
            ItemDefinition def = entry.getValue();
            if (def != null && name.equalsIgnoreCase(def.getName())) {
                taxExemptItemIds.add(entry.getKey());
            }
        }
    }

    private static class GrandExchangeSaveState {
        int nextOfferId = 1;
        List<GrandExchangeOffer> activeOffers = new ArrayList<>();
        Map<String, List<GrandExchangeCollectionEntry>> collectionBox = new HashMap<>();
        Map<String, Map<Integer, BuyLimitWindow>> buyLimitWindows = new HashMap<>();
    }

    private static class BuyLimitWindow {
        long windowStartMs;
        int boughtAmount;
    }
}
