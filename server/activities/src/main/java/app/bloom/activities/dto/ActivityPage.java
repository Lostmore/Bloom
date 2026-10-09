package app.bloom.activities.dto;

import java.util.List;

public record ActivityPage(List<ActivitySummary> items, Long nextCursor) {}
