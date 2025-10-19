package com.example.bossapp.data.model;

public class Boss {
    private int bossNumber;
    private int hp;
    private boolean defeated;

    public Boss() {}

    public Boss(int bossNumber, int hp, boolean defeated) {
        this.bossNumber = bossNumber;
        this.hp = hp;
        this.defeated = defeated;
    }

    public int getBossNumber() { return bossNumber; }
    public void setBossNumber(int bossNumber) { this.bossNumber = bossNumber; }

    public int getHp() { return hp; }
    public void setHp(int hp) { this.hp = hp; }

    public boolean isDefeated() { return defeated; }
    public void setDefeated(boolean defeated) { this.defeated = defeated; }

    public static int calculateBossHp(int bossNumber) {
        if (bossNumber <= 1) return 200;

        int hp = 200;
        for (int i = 2; i <= bossNumber; i++) {
            hp = (int) Math.ceil(hp * 2.5 / 100.0) * 100;
        }
        return hp;
    }

}
