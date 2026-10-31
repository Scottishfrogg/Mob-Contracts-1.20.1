package net.scottishfrogg.mobcontracts.rank;

public enum HunterRank {
    NONE(0, "None", 0xAAAAAA),
    ROOKIE(10, "Rookie", 0x55AAFF),
    SCOUT(20, "Scout", 0x55FF55),
    TRACKER(30, "Tracker", 0xFFFF55),
    MERCENARY(40, "Mercenary", 0xFFAA00),
    BOUNTY_HUNTER(50, "Bounty Hunter", 0x55FFFF),
    VETERAN(60, "Veteran", 0x5555FF),
    ELITE_HUNTER(70, "Elite Hunter", 0xAA00AA),
    MASTER_HUNTER(80, "Master Hunter", 0xFF5555),
    LEGENDARY_HUNTER(90, "Legendary Hunter", 0xFF55FF),
    GRAND_HUNTER(100, "Grand Hunter", 0xAA0000);
    private final int level;
    private final String title;
    private final int color;
    HunterRank(int level, String title, int color) {
        this.level = level;
        this.title = title;
        this.color = color;
    }
    public int level() { return level; }
    public String title() { return title; }
    public int color() { return color; }
    public static HunterRank of(int completed) {
        HunterRank best = NONE;
        for (HunterRank rank : values()) {
            if (completed >= rank.level) best = rank;
        }
        return best;
    }
}
