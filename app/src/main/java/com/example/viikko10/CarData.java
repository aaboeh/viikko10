package com.example.viikko10;

public class CarData {
    protected String type;
    protected int amount;

    public CarData(String type, int amount) {
        this.type = type;
        this.amount = amount;
    }
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

}
