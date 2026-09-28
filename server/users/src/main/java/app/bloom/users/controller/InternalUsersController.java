package app.bloom.users.controller;

import app.bloom.users.dto.AccessResponse;
import app.bloom.users.dto.InteractionAccessRequest;
import app.bloom.users.dto.PhotoAccessRequest;
import app.bloom.users.dto.ProfileBatchRequest;
import app.bloom.users.dto.PublicProfile;
import app.bloom.users.model.Profile;
import app.bloom.users.service.VisibilityService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/users")
public class InternalUsersController {
    private final VisibilityService visibility;

    public InternalUsersController(VisibilityService visibility) {
        this.visibility = visibility;
    }

    @PostMapping("/can-interact")
    public AccessResponse canInteract(@Valid @RequestBody InteractionAccessRequest request) {
        return new AccessResponse(visibility.canInteract(request.viewerId(), request.targetId()));
    }

    @PostMapping("/profiles")
    public List<PublicProfile> profiles(@Valid @RequestBody ProfileBatchRequest request) {
        return visibility.batch(request.viewerId(), request.userIds());
    }

    @PostMapping("/can-view-photo")
    public AccessResponse canViewPhoto(@Valid @RequestBody PhotoAccessRequest request) {
        return new AccessResponse(visibility.canViewPhoto(request.viewerId(), request.ownerId(), request.mediaId()));
    }

    @GetMapping("/{id}/snapshot")
    public Profile snapshot(@PathVariable UUID id) {
        return visibility.snapshot(id);
    }
}
