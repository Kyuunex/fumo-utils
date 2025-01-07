package moe.kyuunex.fumo_utils.enums;

public enum YDirection {
    NORTH(-1),
    CENTER(0),
    SOUTH(1);

    private final int value;

    YDirection(int value) {
        this.value = value;
    }

    public int getRaw() {
        return value;
    }
}
