package com.elvarg.game.content.grandexchange;

public class GrandExchangeOffer {

    private int id;
    private String owner;
    private int slot;
    private int itemId;
    private int amount;
    private int processedAmount;
    private int price;
    private int taxRatePercent;
    private GrandExchangeOfferType type;
    private boolean active;

    public GrandExchangeOffer() {
    }

    public GrandExchangeOffer(int id, String owner, int slot, int itemId, int amount, int price, GrandExchangeOfferType type) {
        this.id = id;
        this.owner = owner;
        this.slot = slot;
        this.itemId = itemId;
        this.amount = amount;
        this.price = price;
        this.type = type;
        this.active = true;
    }

    public GrandExchangeOffer(int id, String owner, int slot, int itemId, int amount, int price, int taxRatePercent, GrandExchangeOfferType type) {
        this(id, owner, slot, itemId, amount, price, type);
        this.taxRatePercent = Math.max(0, taxRatePercent);
    }

    public int getRemainingAmount() {
        return Math.max(0, amount - processedAmount);
    }

    public boolean isComplete() {
        return processedAmount >= amount;
    }

    public int getId() {
        return id;
    }

    public String getOwner() {
        return owner;
    }

    public int getSlot() {
        return slot;
    }

    public int getItemId() {
        return itemId;
    }

    public int getAmount() {
        return amount;
    }

    public int getProcessedAmount() {
        return processedAmount;
    }

    public void setProcessedAmount(int processedAmount) {
        this.processedAmount = processedAmount;
    }

    public int getPrice() {
        return price;
    }

    public int getTaxRatePercent() {
        return taxRatePercent;
    }

    public void setTaxRatePercent(int taxRatePercent) {
        this.taxRatePercent = Math.max(0, taxRatePercent);
    }

    public GrandExchangeOfferType getType() {
        return type;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
