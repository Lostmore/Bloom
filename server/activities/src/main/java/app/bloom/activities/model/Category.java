package app.bloom.activities.model;

public enum Category {
    COFFEE("Кофе"), WALK("Прогулка"), CINEMA("Кино"), DINNER("Ужин"), SPORT("Спорт"),
    CONCERT("Концерт"), EXHIBITION("Выставка"), GAMES("Игры"), OTHER("Другое");

    private final String label;

    Category(String label) { this.label = label; }

    public String label() { return label; }
}
