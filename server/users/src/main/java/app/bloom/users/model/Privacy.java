package app.bloom.users.model;

public record Privacy(
        boolean discoverable,
        boolean showDistance,
        boolean showLastSeen
) {
}
