package com.elvarg.game.content.grandexchange;

import com.elvarg.game.definition.ItemDefinition;
import com.elvarg.game.entity.impl.player.Player;
import com.elvarg.game.model.Item;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class GrandExchangeManager {

    public static final int COINS_ID = 995;
    public static final int TAX_PERCENT = 1;

    private static final Path SAVE_PATH = Path.of("../data/grand_exchange.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final GrandExchangeManager INSTANCE = new GrandExchangeManager();

    private final List<GrandExchangeOffer> activeOffers = new ArrayList<>();
    private final Map<String, List<GrandExchangeCollectionEntry>> collectionBox = new HashMap<>();
    private int nextOfferId = 1;

    private GrandExchangeManager() {
    }

    public static GrandExchangeManager getInstance() {
        return INSTANCE;
    }

    public synchronized void load() {
        try {
            File file = SAVE_PATH.toFile();
            if (!file.exists()) {
                return;
            }
            try (Reader reader = new FileReader(file, StandardCharsets.UTF_8)) {
                Type type = new TypeToken<GrandExchangeSaveState>() {
                }.getType();
                GrandExchangeSaveState state = GSON.fromJson(reader, type);
                if (state == null) {
                    return;
                }
                nextOfferId = Math.max(1, state.nextOfferId);
                activeOffers.clear();
                if (state.activeOffers != null) {
                    activeOffers.addAll(state.activeOffers);
                }
                collectionBox.clear();
                if (state.collectionBox != null) {
                    collectionBox.putAll(state.collectionBox);
                }
            }
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
        GrandExchangeOffer newOffer = new GrandExchangeOffer(nextOfferId++, player.getUsername(), slot, itemId, amount, price, GrandExchangeOfferType.BUY);
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
        if (player.getInventory().getAmount(itemId) < amount) {
            return false;
        }
        if (getOffer(player.getUsername(), slot).isPresent()) {
            return false;
        }

        player.getInventory().delete(itemId, amount);
        GrandExchangeOffer newOffer = new GrandExchangeOffer(nextOfferId++, player.getUsername(), slot, itemId, amount, price, GrandExchangeOfferType.SELL);
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
        for (Map.Entry<Integer, ItemDefinition> defEntry : ItemDefinition.definitions.entrySet()) {
            ItemDefinition def = defEntry.getValue();
            if (def == null || def.getName() == null) {
                continue;
            }
            if (def.getName().toLowerCase().contains(normalized)) {
                results.add(defEntry.getKey());
                if (results.size() >= maxResults) {
                    break;
                }
            }
        }
        return results;
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
            int buyerLimitPrice = incoming.getPrice();
            int refundPerItem = Math.max(0, buyerLimitPrice - tradePrice);
            long refund = (long) refundPerItem * qty;
            if (refund > 0 && refund <= Integer.MAX_VALUE) {
                addToCollection(incoming.getOwner(), COINS_ID, (int) refund);
            }

            int sellerProceeds = applyTax(total);
            addToCollection(existing.getOwner(), COINS_ID, sellerProceeds);
        } else {
            addToCollection(incoming.getOwner(), COINS_ID, applyTax(total));
            addToCollection(existing.getOwner(), existing.getItemId(), qty);

            int buyerLimitPrice = existing.getPrice();
            int refundPerItem = Math.max(0, buyerLimitPrice - tradePrice);
            long refund = (long) refundPerItem * qty;
            if (refund > 0 && refund <= Integer.MAX_VALUE) {
                addToCollection(existing.getOwner(), COINS_ID, (int) refund);
            }
        }
    }

    private int applyTax(int gross) {
        int tax = (gross * TAX_PERCENT) / 100;
        return gross - tax;
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

    private static class GrandExchangeSaveState {
        int nextOfferId = 1;
        List<GrandExchangeOffer> activeOffers = new ArrayList<>();
        Map<String, List<GrandExchangeCollectionEntry>> collectionBox = new HashMap<>();
    }
}
