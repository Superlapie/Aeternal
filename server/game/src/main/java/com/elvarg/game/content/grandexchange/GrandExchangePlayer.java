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
    public static final int INVENTORY_TAB_INTERFACE_ID = 3213;
    public static final int INVENTORY_INTERFACE_ID = 3214;
    public static final int COLLECTION_INTERFACE_ID = 51100;
    public static final int SEARCH_CHATBOX_INTERFACE_ID = 56600;
    public static final int SEARCH_FILTER_TEXT_ID = 56605;
    public static final int SEARCH_CANCEL_BUTTON = 56608;
    public static final int SEARCH_RESULT_START = 56621;
    public static final int SEARCH_RESULT_END = 56720;

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
    public static final int SLOT_PROGRESS_TEXT_BASE = 51550;
    public static final int SLOT_ITEM_WIDGET_BASE = 51400;
    public static final int SLOT_PROGRESS_SEGMENT_BASE = 52000;
    public static final int SLOT_PROGRESS_SEGMENT_COUNT = 20;
    public static final int OFFER_ITEM_WIDGET = 51056;

    private final Player player;
    private final GrandExchangeManager manager = GrandExchangeManager.getInstance();

    private final Map<Integer, PendingOfferState> pendingOffers = new HashMap<>();
    private boolean sellSelectionMode;

    public GrandExchangePlayer(Player player) {
        this.player = player;
    }

    public void openMainInterface() {
        sellSelectionMode = false;
        if (player.busy()) {
            player.getPacketSender().sendInterfaceRemoval();
        }
        player.setStatus(PlayerStatus.GRAND_EXCHANGE);
        player.getPacketSender().sendInterface(MAIN_INTERFACE_ID);
        refreshMain();
    }

    public void openCollectionBox() {
        sellSelectionMode = false;
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
        if (button == SEARCH_CANCEL_BUTTON) {
            player.setEnteredSyntaxAction(null);
            PendingOfferState state = getCurrentPendingState();
            if (state != null) {
                player.getPacketSender().sendChatboxInterface(0);
                openOfferScreen(state);
            } else {
                player.getPacketSender().sendChatboxInterface(0);
            }
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
        if (!sellSelectionMode) {
            return false;
        }
        PendingOfferState state = getCurrentPendingState();
        if (state == null || state.type != GrandExchangeOfferType.SELL) {
            return false;
        }
        state.itemId = itemId;
        if (state.price <= 0) {
            state.price = getDefaultPrice(itemId);
        }
        if (actionIndex == 1) {
            int available = player.getInventory().getAmount(itemId);
            state.amount = Math.max(1, available);
        }
        openOfferScreen(state);
        return true;
    }

    public boolean isSellSelectionMode() {
        return sellSelectionMode;
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
        sellSelectionMode = type == GrandExchangeOfferType.SELL;
        openOfferScreen(state);
        if (type == GrandExchangeOfferType.BUY) {
            promptSearch();
        } else {
            player.getPacketSender().sendMessage("Select an item from your inventory.");
        }
    }

    private void openOfferScreen(PendingOfferState state) {
        if (state.type == GrandExchangeOfferType.SELL) {
            // Use the real inventory sidebar so sell item selection always has clickable items.
            player.getPacketSender().sendInterfaceSet(OFFER_INTERFACE_ID, INVENTORY_TAB_INTERFACE_ID);
        } else {
            player.getPacketSender().sendInterface(OFFER_INTERFACE_ID);
        }
        String itemName = state.itemId > 0 ? ItemDefinition.forId(state.itemId).getName() : "Item not selected";
        int amount = Math.max(1, state.amount);
        int price = Math.max(0, state.price);
        long gross = (long) amount * price;
        int effectiveTaxRate = state.type == GrandExchangeOfferType.SELL && state.itemId > 0
                ? manager.getEffectiveTaxRatePercent(state.itemId)
                : 0;
        int net = (state.type == GrandExchangeOfferType.SELL && state.itemId > 0)
                ? manager.getSellNetTotal(state.itemId, amount, price)
                : (int) Math.min(Integer.MAX_VALUE, gross);

        player.getPacketSender().sendString(51031, state.type == GrandExchangeOfferType.BUY ? "Buy offer" : "Sell offer");
        player.getPacketSender().sendString(51032, itemName);
        player.getPacketSender().sendString(51033, "Choose item, amount and price.");
        player.getPacketSender().sendString(51055, price + " coins");
        player.getPacketSender().sendString(51003, Integer.toString(amount));
        player.getPacketSender().sendString(51004, price + " coins");
        if (state.type == GrandExchangeOfferType.SELL && state.itemId > 0) {
            player.getPacketSender().sendString(51023, net + " coins (" + Math.min(Integer.MAX_VALUE, gross) + " - " + effectiveTaxRate + "%)");
        } else {
            player.getPacketSender().sendString(51023, Math.min(Integer.MAX_VALUE, gross) + " coins");
        }
        if (state.itemId > 0) {
            player.getPacketSender().sendItemOnInterface(OFFER_ITEM_WIDGET, state.itemId, 0, 1);
        } else {
            player.getPacketSender().clearItemOnInterface(OFFER_ITEM_WIDGET);
        }
    }

    private void promptSearch() {
        PendingOfferState state = getCurrentPendingState();
        if (state == null) {
            player.getPacketSender().sendMessage("Start an offer first.");
            return;
        }
        player.setEnteredSyntaxAction(input -> {
            PendingOfferState current = getCurrentPendingState();
            if (current == null) {
                return;
            }
            if (input == null) {
                return;
            }
            String value = input.trim();
            if (value.startsWith("__geitem__:")) {
                try {
                    int itemId = Integer.parseInt(value.substring("__geitem__:".length()));
                    if (itemId > 0) {
                        current.itemId = itemId;
                        if (current.price <= 0) {
                            current.price = getDefaultPrice(itemId);
                        }
                        player.getPacketSender().sendChatboxInterface(0);
                        player.getPacketSender().sendMessage("Selected " + ItemDefinition.forId(itemId).getName() + ".");
                        openOfferScreen(current);
                        return;
                    }
                } catch (Exception ignored) {
                }
            }
            if (value.startsWith("__geitemname__:")) {
                String itemName = value.substring("__geitemname__:".length()).trim();
                Integer itemId = manager.findItemByExactName(itemName);
                if (itemId == null || itemId <= 0) {
                    List<Integer> fallback = manager.findItemByName(itemName, 1);
                    if (!fallback.isEmpty()) {
                        itemId = fallback.get(0);
                    }
                }
                if (itemId != null && itemId > 0) {
                    current.itemId = itemId;
                    if (current.price <= 0) {
                        current.price = getDefaultPrice(itemId);
                    }
                    player.getPacketSender().sendChatboxInterface(0);
                    player.getPacketSender().sendMessage("Selected " + ItemDefinition.forId(itemId).getName() + ".");
                    openOfferScreen(current);
                    return;
                }
            }

            List<Integer> ids = manager.findItemByName(value, 1);
            if (!ids.isEmpty()) {
                int itemId = ids.get(0);
                current.itemId = itemId;
                if (current.price <= 0) {
                    current.price = getDefaultPrice(itemId);
                }
                player.getPacketSender().sendChatboxInterface(0);
                player.getPacketSender().sendMessage("Selected " + ItemDefinition.forId(itemId).getName() + ".");
                openOfferScreen(current);
            }
        });
        openSearchInterface(state);
    }

    private void openSearchInterface(PendingOfferState state) {
        if (state == null) {
            return;
        }
        player.getPacketSender().sendChatboxInterface(SEARCH_CHATBOX_INTERFACE_ID);
        player.getPacketSender().sendString(SEARCH_FILTER_TEXT_ID, "Type to search...");
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
        player.setEnteredAmountAction(null);
        player.setEnteredSyntaxAction(null);
        state.amount = Math.max(1, state.amount);
        state.price = Math.max(1, state.price > 0 ? state.price : getDefaultPrice(state.itemId));

        boolean placed = state.type == GrandExchangeOfferType.BUY
                ? manager.placeBuyOffer(player, state.slot, state.itemId, state.amount, state.price)
                : manager.placeSellOffer(player, state.slot, state.itemId, state.amount, state.price);
        if (!placed) {
            player.getPacketSender().sendMessage("Unable to place offer. Check funds/items/slot.");
            return;
        }
        sellSelectionMode = false;
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
            int progressText = SLOT_PROGRESS_TEXT_BASE + slot;
            int itemWidget = SLOT_ITEM_WIDGET_BASE + (slot * 10) + 2;

            if (offer == null) {
                player.getPacketSender().sendString(56590 + slot, "Empty");
                player.getPacketSender().sendString(progressText, "");
                player.getPacketSender().sendString(priceText, "0 coins");
                player.getPacketSender().clearItemOnInterface(itemWidget);
                player.getPacketSender().sendInterfaceDisplayState(grayBar, false);
                player.getPacketSender().sendInterfaceDisplayState(redBar, true);
                player.getPacketSender().sendInterfaceDisplayState(greenBar, true);
                for (int seg = 0; seg < SLOT_PROGRESS_SEGMENT_COUNT; seg++) {
                    player.getPacketSender().sendInterfaceDisplayState(SLOT_PROGRESS_SEGMENT_BASE + (slot * SLOT_PROGRESS_SEGMENT_COUNT) + seg, true);
                }
                continue;
            }
            String itemName = ItemDefinition.forId(offer.getItemId()).getName();
            player.getPacketSender().sendString(priceText, offer.getPrice() + " coins");
            player.getPacketSender().sendString(56590 + slot, itemName);
            player.getPacketSender().sendString(progressText, offer.getProcessedAmount() + "/" + offer.getAmount());
            player.getPacketSender().sendItemOnInterface(itemWidget, offer.getItemId(), 0, 1);

            if (!offer.isActive() && offer.isComplete() && hasPendingCollection) {
                player.getPacketSender().sendInterfaceDisplayState(grayBar, true);
                player.getPacketSender().sendInterfaceDisplayState(redBar, true);
                player.getPacketSender().sendInterfaceDisplayState(greenBar, false);
                for (int seg = 0; seg < SLOT_PROGRESS_SEGMENT_COUNT; seg++) {
                    player.getPacketSender().sendInterfaceDisplayState(SLOT_PROGRESS_SEGMENT_BASE + (slot * SLOT_PROGRESS_SEGMENT_COUNT) + seg, true);
                }
            } else if (!offer.isActive()) {
                // Any inactive non-collect-complete offer is treated as canceled/aborted and shown red.
                player.getPacketSender().sendInterfaceDisplayState(grayBar, true);
                player.getPacketSender().sendInterfaceDisplayState(redBar, false);
                player.getPacketSender().sendInterfaceDisplayState(greenBar, true);
                for (int seg = 0; seg < SLOT_PROGRESS_SEGMENT_COUNT; seg++) {
                    player.getPacketSender().sendInterfaceDisplayState(SLOT_PROGRESS_SEGMENT_BASE + (slot * SLOT_PROGRESS_SEGMENT_COUNT) + seg, true);
                }
            } else if (offer.getProcessedAmount() > 0 && offer.getProcessedAmount() < offer.getAmount()) {
                player.getPacketSender().sendInterfaceDisplayState(grayBar, true);
                player.getPacketSender().sendInterfaceDisplayState(redBar, true);
                player.getPacketSender().sendInterfaceDisplayState(greenBar, true);
                int visibleSegments = (int) Math.ceil((offer.getProcessedAmount() / (double) offer.getAmount()) * SLOT_PROGRESS_SEGMENT_COUNT);
                visibleSegments = Math.max(1, Math.min(SLOT_PROGRESS_SEGMENT_COUNT, visibleSegments));
                for (int seg = 0; seg < SLOT_PROGRESS_SEGMENT_COUNT; seg++) {
                    boolean hide = seg >= visibleSegments;
                    player.getPacketSender().sendInterfaceDisplayState(SLOT_PROGRESS_SEGMENT_BASE + (slot * SLOT_PROGRESS_SEGMENT_COUNT) + seg, hide);
                }
            } else {
                player.getPacketSender().sendInterfaceDisplayState(grayBar, false);
                player.getPacketSender().sendInterfaceDisplayState(redBar, true);
                player.getPacketSender().sendInterfaceDisplayState(greenBar, true);
                for (int seg = 0; seg < SLOT_PROGRESS_SEGMENT_COUNT; seg++) {
                    player.getPacketSender().sendInterfaceDisplayState(SLOT_PROGRESS_SEGMENT_BASE + (slot * SLOT_PROGRESS_SEGMENT_COUNT) + seg, true);
                }
            }
        }
    }

    private int getDefaultPrice(int itemId) {
        int value = ItemDefinition.forId(itemId).getValue();
        return Math.max(1, value);
    }

    private static class PendingOfferState {
        int slot;
        GrandExchangeOfferType type;
        int itemId = -1;
        int amount = 1;
        int price = 0;

        PendingOfferState(int slot, GrandExchangeOfferType type) {
            this.slot = slot;
            this.type = type;
        }
    }
}
