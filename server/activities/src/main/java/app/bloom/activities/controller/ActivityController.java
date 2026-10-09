package app.bloom.activities.controller;

import app.bloom.activities.dto.*;
import app.bloom.activities.model.ActivityStatus;
import app.bloom.activities.model.Category;
import app.bloom.activities.service.ActivityService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/activities")
public class ActivityController {
    private final ActivityService service;
    public ActivityController(ActivityService service) { this.service = service; }

    @GetMapping("/categories")
    @Operation(summary = "Категории активностей с русскими названиями")
    public List<CategoryView> categories() {
        return Arrays.stream(Category.values()).map(c -> new CategoryView(c, c.label())).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Создать встречу", description = "requestId — UUID клиента. Повтор с тем же телом возвращает ту же встречу. Организатор занимает одно место.")
    public ActivityView create(@AuthenticationPrincipal UUID user, @Valid @RequestBody CreateActivity request) {
        return service.create(user, request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Изменить встречу до её начала", description = "Полная замена редактируемых полей. version берётся из последнего ответа сервера.")
    public ActivityView update(@AuthenticationPrincipal UUID user, @PathVariable UUID id,
            @Valid @RequestBody UpdateActivity request) {
        return service.update(user, id, request);
    }

    @GetMapping("/nearby")
    @Operation(summary = "Предстоящие встречи рядом", description = "По сохранённой геопозиции своей анкеты. distanceKm кратно 5. from включительно, to исключительно, ISO-8601 с часовым поясом. Порядок — новые сначала. Продолжать по nextCursor, даже если страница пуста из-за блокировок.")
    public ActivityPage nearby(@AuthenticationPrincipal UUID user,
            @RequestParam(defaultValue = "25") @Min(5) @Max(200) int distanceKm,
            @RequestParam(required = false) Category category,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(required = false) @Positive Long cursor,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int limit) {
        return service.nearby(user, distanceKm, category, from, to, cursor, limit);
    }

    @GetMapping("/mine")
    @Operation(summary = "Мои созданные встречи и участие, включая историю")
    public ActivityPage mine(@AuthenticationPrincipal UUID user,
            @RequestParam(required = false) ActivityStatus status,
            @RequestParam(required = false) @Positive Long cursor,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int limit) {
        return service.mine(user, cursor, limit, status);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Встреча и доступные участники", description = "Участники возвращаются как UUID. Имена и фото запрашиваются через Users с его правилами приватности.")
    public ActivityView get(@AuthenticationPrincipal UUID user, @PathVariable UUID id) {
        return service.get(user, id);
    }

    @PostMapping("/{id}/join")
    @Operation(summary = "Присоединиться к открытой встрече")
    public ActivityView join(@AuthenticationPrincipal UUID user, @PathVariable UUID id) { return service.join(user, id); }

    @PostMapping("/{id}/leave")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Выйти из встречи", description = "Организатор вместо выхода отменяет встречу. Повторный выход не меняет счётчик.")
    public void leave(@AuthenticationPrincipal UUID user, @PathVariable UUID id) { service.leave(user, id); }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Отменить встречу", description = "Только организатор. История сохраняется со статусом CANCELLED.")
    public void cancel(@AuthenticationPrincipal UUID user, @PathVariable UUID id) { service.cancel(user, id); }

    @PostMapping("/{id}/finish")
    @Operation(summary = "Досрочно завершить начавшуюся встречу", description = "Только организатор. Без этого запроса встреча автоматически завершается по endsAt.")
    public ActivityView finish(@AuthenticationPrincipal UUID user, @PathVariable UUID id) { return service.finish(user, id); }

    public record CategoryView(Category id, String label) {}
}
