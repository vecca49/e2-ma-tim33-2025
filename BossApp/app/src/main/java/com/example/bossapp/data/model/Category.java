package com.example.bossapp.data.model;

import com.google.firebase.firestore.DocumentId;

public class Category {
    @DocumentId
    private String id;
    private String name;
    private String colorHex;
    private String ownerId;

    public Category() {}

    public Category(String name, String colorHex, String ownerId) {
        this.name = name;
        this.colorHex = colorHex;
        this.ownerId = ownerId;
    }

    @Override
    public String toString() {
        return name;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }

    public void setName(String name) { this.name = name; }
    public String getColorHex() { return colorHex; }
    public void setColorHex(String colorHex) { this.colorHex = colorHex; }
    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }
}