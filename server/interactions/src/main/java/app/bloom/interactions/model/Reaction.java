package app.bloom.interactions.model;

public enum Reaction {
    LIKE,
    SKIP,
    SUPER_INTEREST;

    public boolean positive() {
        return this != SKIP;
    }
}
