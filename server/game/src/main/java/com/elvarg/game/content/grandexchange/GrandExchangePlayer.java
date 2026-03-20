package com.elvarg.game.content.grandexchange;

import com.elvarg.game.definition.ItemDefinition;
import com.elvarg.game.entity.impl.player.Player;
import com.elvarg.game.model.PlayerStatus;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GrandExchangePlayer {

    public static final int MAIN_INTERFACE_ID = 56500;
    public static final int OFFER_INTERFACE_ID = 51000;
    public static final int INVENTORY_OVERLAY_INTERFACE_ID = 46800;
    public static final int COLLECTION_INTERFACE_ID = 51100;
    public static final int SEARCH_INTERFACE_ID = 51020;

    // These child/button ids can differ between client revisions.
    public static final int[] SLOT_BUY_BUTTONS = {51200, 51210, 51220, 51230, 51240, 51250, 51260, 51270};
    public static final int[] SLOT_SELL_BUTTONS = {51201, 51211, 51221, 51231, 51241, 51251, 51261, 51271};
    public static final int[] SLOT_ABORT_BUTTONS = {51202, 51212, 51222, 51232, 51242, 51252, 51262, 51272};
    public static final int CONFIRM_OFFER_BUTTON = 51010;
    public static final int SEARCH_ITEM_BUTTON = 51020;
    public static final int QTY_PLUS_1_BUTTON = 51043;
    public static final int QTY_PLUS_10_BUTTON = 51044;
    public static final int QTY_PLUS_100_BUTTON = 51045;
    public static final int QTY_PLUS_1K_BUTTON = 51046;
    public static final int QTY_SET_BUTTON = 51047;
    public static final int PRICE_MINUS_5_BUTTON = 51048;
    public static final int PRICE_SEARCH_BUTTON = 51049;
    public static final int PRICE_SET_BUTTON = 51050;
    public static final int PRICE_PLUS_5_BUTTON = 51051;
    public static final int PRICE_PLUS_10_BUTTON = 51052;
    public static final int QTY_MINUS_1_BUTTON = 51074;
    public static final int QTY_PLUS_1_EDGE_BUTTON = 51075;
    public static final int PRICE_MINUS_1_BUTTON = 51076;
    public static final int PRICE_PLUS_1_BUTTON = 51077;
    public static final int OPEN_COLLECTION_BUTTON = 56506;
    public static final int COLLECT_BUTTON = 51110;
    public static final int SLOT_WIDGET_BASE = 51300;

    private final Player player;
    private final GrandExchangeManager manager = GrandExchangeManager.getInstance();

    private final Map<Integer, PendingOfferState> pendingOffers = new HashMap<>();

    public GrandExchangePlayer(Player player) {
        this.player = player;
    }

    public void openMainInterface() {
        if (player.busy()) {
            player.getPacketSender().sendInterfaceRemoval();
        }
        player.setStatus(PlayerStatus.GRAND_EXCHANGE);
        player.getPacketSender().sendInterface(MAIN_INTERFACE_ID);
        refreshMain();
    }

    public void openCollectionBox() {
        player.setStatus(PlayerStatus.GRAND_EXCHANGE);
        player.getPacketSender().sendInterface(COLLECTION_INTERFACE_ID);
        List<GrandExchangeCollectionEntry> entries = manager.getCollection(player.getUsername());
        if (entries.isEmpty()) {
            player.getPacketSender().sendMessage("Your collection box is empty.");
            return;
        }
        for (GrandExchangeCollectionEntry entry : entries) {
            String name = ItemDefinition.forId(entry.getItemId()).getName();
            player.getPacketSender().sendMessage(name + " x " + entry.getAmount());
        }
    }

    public boolean handleButton(int button) {
        if (button == OPEN_COLLECTION_BUTTON) {
            openCollectionBox();
            return true;
        }
        if (button == COLLECT_BUTTON) {
            manager.collect(player);
            refreshMain();
            return true;
        }
        if (button == CONFIRM_OFFER_BUTTON) {
            confirmOffer();
            return true;
        }
        if (button == SEARCH_ITEM_BUTTON) {
            promptSearch();
            return true;
        }
        if (handleOfferAdjustmentButton(button)) {
            return true;
        }

        for (int slot = 0; slot < SLOT_BUY_BUTTONS.length; slot++) {
            if (button == SLOT_BUY_BUTTONS[slot]) {
                startOffer(slot, GrandExchangeOfferType.BUY);
                return true;
            }
            if (button == SLOT_SELL_BUTTONS[slot]) {
                startOffer(slot, GrandExchangeOfferType.SELL);
                return true;
            }
            if (button == SLOT_ABORT_BUTTONS[slot]) {
                if (manager.abortOffer(player, slot)) {
                    player.getPacketSender().sendMessage("Offer cancelled.");
                    refreshMain();
                } else {
                    player.getPacketSender().sendMessage("No active offer in that slot.");
                }
                return true;
            }
        }
        return false;
    }

    private boolean handleOfferAdjustmentButton(int button) {
        PendingOfferState state = getCurrentPendingState();
        if (state == null) {
            return false;
        }
        switch (button) {
            case QTY_PLUS_1_BUTTON:
            case QTY_PLUS_1_EDGE_BUTTON:
                state.amount = safeAdd(state.amount, 1);
                openOfferScreen(state);
                return true;
            case QTY_PLUS_10_BUTTON:
                state.amount = safeAdd(state.amount, 10);
                openOfferScreen(state);
                return true;
            case QTY_PLUS_100_BUTTON:
                state.amount = safeAdd(state.amount, 100);
                openOfferScreen(state);
                return true;
            case QTY_PLUS_1K_BUTTON:
                state.amount = safeAdd(state.amount, 1000);
                openOfferScreen(state);
                return true;
            case QTY_MINUS_1_BUTTON:
                state.amount = Math.max(1, state.amount - 1);
                openOfferScreen(state);
                return true;
            case QTY_SET_BUTTON:
                player.setEnteredAmountAction(amount -> {
                    state.amount = Math.max(1, amount);
                    openOfferScreen(state);
                });
                player.getPacketSender().sendEnterAmountPrompt("Enter quantity.");
                return true;
            case PRICE_MINUS_1_BUTTON:
                state.price = Math.max(1, state.price - 1);
                openOfferScreen(state);
                return true;
            case PRICE_PLUS_1_BUTTON:
                state.price = safeAdd(state.price, 1);
                openOfferScreen(state);
                return true;
            case PRICE_MINUS_5_BUTTON:
                state.price = Math.max(1, state.price - Math.max(1, state.price / 20));
                openOfferScreen(state);
                return true;
            case PRICE_PLUS_5_BUTTON:
                state.price = safeAdd(state.price, Math.max(1, state.price / 20));
                openOfferScreen(state);
                return true;
            case PRICE_PLUS_10_BUTTON:
                state.price = safeAdd(state.price, Math.max(1, state.price / 10));
                openOfferScreen(state);
                return true;
            case PRICE_SET_BUTTON:
                player.setEnteredAmountAction(price -> {
                    state.price = Math.max(1, price);
                    openOfferScreen(state);
                });
                player.getPacketSender().sendEnterAmountPrompt("Enter price per item.");
                return true;
            case PRICE_SEARCH_BUTTON:
                promptSearch();
                return true;
            default:
                return false;
        }
    }

    private int safeAdd(int value, int add) {
        long result = (long) Math.max(0, value) + Math.max(0, add);
        return (int) Math.min(Integer.MAX_VALUE, Math.max(1L, result));
    }

    public boolean handleInterfaceAction(int interfaceId, int action) {
        int min = Math.min(MAIN_INTERFACE_ID, Math.min(OFFER_INTERFACE_ID, COLLECTION_INTERFACE_ID));
        int max = Math.max(MAIN_INTERFACE_ID, Math.max(OFFER_INTERFACE_ID, COLLECTION_INTERFACE_ID)) + 4000;
        int childId = interfaceId & 0xFFFF;

        boolean inRange = interfaceId >= min && interfaceId <= max;
        boolean childInRange = childId >= min && childId <= max;
        if (!inRange && !childInRange) {
            return handleButton(childId);
        }
        return handleButton(interfaceId) || handleButton(childId);
    }

    public boolean handleInventoryOverlayItem(int itemId, int slot, int actionIndex) {
        if (player.getStatus() != PlayerStatus.GRAND_EXCHANGE) {
            return false;
        }
        PendingOfferState state = getCurrentPendingState();
        if (state == null || state.type != GrandExchangeOfferType.SELL) {
            return false;
        }
        state.itemId = itemId;
        if (actionIndex == 1) {
            int available = player.getInventory().getAmount(itemId);
            state.amount = Math.max(1, available);
        }
        openOfferScreen(state);
        return true;
    }

    private PendingOfferState getCurrentPendingState() {
        return pendingOffers.values().stream().findFirst().orElse(null);
    }

    private void startOffer(int slot, GrandExchangeOfferType type) {
        if (manager.getOffer(player.getUsername(), slot).isPresent()) {
            player.getPacketSender().sendMessage("This slot already has an active offer.");
            return;
        }
        PendingOfferState state = new PendingOfferState(slot, type);
        pendingOffers.clear();
        pendingOffers.put(slot, state);
        openOfferScreen(state);
        beginOfferWizard(state);
    }

    private void openOfferScreen(PendingOfferState state) {
        player.getPacketSender().sendInterface(OFFER_INTERFACE_ID);
        String itemName = state.itemId > 0 ? ItemDefinition.forId(state.itemId).getName() : "Item not selected";
        player.getPacketSender().sendString(51031, state.type == GrandExchangeOfferType.BUY ? "Buy offer" : "Sell offer");
        player.getPacketSender().sendString(51032, itemName);
        player.getPacketSender().sendString(51033, "Choose item, amount and price.");
        player.getPacketSender().sendString(51055, Math.max(0, state.price) + " coins");
        player.getPacketSender().sendString(51003, Integer.toString(Math.max(1, state.amount)));
        player.getPacketSender().sendString(51004, Math.max(0, state.price) + " coins");
        player.getPacketSender().sendString(51023, ((long) Math.max(1, state.amount) * Math.max(0, state.price)) + " coins");
    }

    private void beginOfferWizard(PendingOfferState state) {
        player.setEnteredSyntaxAction(input -> {
            List<Integer> ids = manager.findItemByName(input, 10);
            if (ids.isEmpty()) {
                player.getPacketSender().sendMessage("No matching items found.");
                return;
            }
            int first = ids.get(0);
            state.itemId = first;
            player.getPacketSender().sendMessage("Selected " + ItemDefinition.forId(first).getName() + ".");
            player.setEnteredAmountAction(amount -> {
                state.amount = amount;
                player.setEnteredAmountAction(price -> {
                    state.price = price;
                    confirmOffer();
                });
                player.getPacketSender().sendEnterAmountPrompt("Enter price per item.");
            });
            player.getPacketSender().sendEnterAmountPrompt("Enter amount.");
        });
        player.getPacketSender().sendEnterInputPrompt(state.type == GrandExchangeOfferType.SELL
                ? "Enter item name to sell."
                : "Enter item name to buy.");
    }

    private void promptSearch() {
        PendingOfferState state = getCurrentPendingState();
        if (state == null) {
            player.getPacketSender().sendMessage("Start an offer first.");
            return;
        }
        player.setEnteredSyntaxAction(input -> {
            List<Integer> ids = manager.findItemByName(input, 10);
            if (ids.isEmpty()) {
                player.getPacketSender().sendMessage("No matching items found.");
                return;
            }
            int first = ids.get(0);
            state.itemId = first;
            player.getPacketSender().sendMessage("Selected " + ItemDefinition.forId(first).getName() + ".");
            openOfferScreen(state);
        });
        player.getPacketSender().sendEnterInputPrompt(state.type == GrandExchangeOfferType.SELL
                ? "Enter item name to sell."
                : "Enter item name to search.");
    }

    private void confirmOffer() {
        PendingOfferState state = getCurrentPendingState();
        if (state == null) {
            player.getPacketSender().sendMessage("Start an offer first.");
            return;
        }
        if (state.itemId <= 0) {
            player.getPacketSender().sendMessage("Select an item first.");
            return;
        }
        if (state.amount <= 0 || state.price <= 0) {
            player.setEnteredAmountAction(amount -> {
                if (state.amount <= 0) {
                    state.amount = amount;
                    player.setEnteredAmountAction(price -> {
                        state.price = price;
                        confirmOffer();
                    });
                    player.getPacketSender().sendEnterAmountPrompt("Enter price per item.");
                } else {
                    state.price = amount;
                    confirmOffer();
                }
            });
            player.getPacketSender().sendEnterAmountPrompt(state.amount <= 0 ? "Enter amount." : "Enter price per item.");
            return;
        }

        boolean placed = state.type == GrandExchangeOfferType.BUY
                ? manager.placeBuyOffer(player, state.slot, state.itemId, state.amount, state.price)
                : manager.placeSellOffer(player, state.slot, state.itemId, state.amount, state.price);
        if (!placed) {
            player.getPacketSender().sendMessage("Unable to place offer. Check funds/items/slot.");
            return;
        }
        pendingOffers.clear();
        player.getPacketSender().sendMessage("Offer placed.");
        openMainInterface();
    }

    public void refreshMain() {
        List<GrandExchangeOffer> offers = manager.getOffersForOwner(player.getUsername());
        boolean hasPendingCollection = manager.hasPendingCollection(player.getUsername());
        for (int slot = 0; slot < 8; slot++) {
            int finalSlot = slot;
            GrandExchangeOffer offer = offers.stream().filter(o -> o.getSlot() == finalSlot).findFirst().orElse(null);
            int slotBase = SLOT_WIDGET_BASE + (slot * 10);
            int grayBar = slotBase + 3;
            int redBar = slotBase + 4;
            int greenBar = slotBase + 5;
            int priceText = slotBase + 6;

            if (offer == null) {
                player.getPacketSender().sendString(56590 + slot, "Empty");
                player.getPacketSender().sendString(priceText, "0 coins");
                player.getPacketSender().sendInterfaceDisplayState(grayBar, false);
                player.getPacketSender().sendInterfaceDisplayState(redBar, true);
                player.getPacketSender().sendInterfaceDisplayState(greenBar, true);
                continue;
            }
            String itemName = ItemDefinition.forId(offer.getItemId()).getName();
            player.getPacketSender().sendString(priceText, offer.getPrice() + " coins");
            player.getPacketSender().sendString(56590 + slot, itemName + " " + offer.getProcessedAmount() + "/" + offer.getAmount());

            if (!offer.isActive() && offer.isComplete() && hasPendingCollection) {
                player.getPacketSender().sendInterfaceDisplayState(grayBar, true);
                player.getPacketSender().sendInterfaceDisplayState(redBar, true);
                player.getPacketSender().sendInterfaceDisplayState(greenBar, false);
            } else if (!offer.isActive() && !offer.isComplete()) {
                player.getPacketSender().sendInterfaceDisplayState(grayBar, true);
                player.getPacketSender().sendInterfaceDisplayState(redBar, false);
                player.getPacketSender().sendInterfaceDisplayState(greenBar, true);
            } else {
                player.getPacketSender().sendInterfaceDisplayState(grayBar, false);
                player.getPacketSender().sendInterfaceDisplayState(redBar, true);
                player.getPacketSender().sendInterfaceDisplayState(greenBar, true);
            }
        }
    }

    private static class PendingOfferState {
        int slot;
        GrandExchangeOfferType type;
        int itemId = -1;
        int amount = 0;
        int price = 0;

        PendingOfferState(int slot, GrandExchangeOfferType type) {
            this.slot = slot;
            this.type = type;
        }
    }
}
