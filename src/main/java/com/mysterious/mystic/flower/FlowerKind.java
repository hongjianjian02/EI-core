package com.mysterious.mystic.flower;

public enum FlowerKind {
    BREAD("bread_fanatic", 666600, 0xD8A452),
    BEEF("cow", 666600, 0xA84B43),
    IRON("eat_iron", 666600, 0xB7CFD8),
    AGRICULTURAL("agricultural", 666600, 0x76B94C),
    TREADMILL("running_machine", 100000, 0xE4B84B),
    QUEST("quest", 666600, 0xA875D9);

    public final String id;
    public final int capacity;
    public final int color;

    FlowerKind(String id, int capacity, int color) {
        this.id = id;
        this.capacity = capacity;
        this.color = color;
    }
}
