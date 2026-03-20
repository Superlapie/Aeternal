package com.elvarg.game.content.grandexchange;

public class GrandExchangeCollectionEntry {

    private int itemId;
    private int amount;

    public GrandExchangeCollectionEntry() {
    }

    public GrandExchangeCollectionEntry(int itemId, int amount) {
        this.itemId = itemId;
        this.amount = amount;
    }

    public int getItemId() {
        return itemId;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }
}
