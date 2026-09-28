package app.bloom.gateway.docs;

import java.net.URI;
import java.util.List;
import java.util.Map;

public record ApiDocService(
        String name,
        URI url,
        List<String> paths,
        Map<String, String> pathMappings
) {
    public ApiDocService {
        paths = paths == null ? List.of() : List.copyOf(paths);
        pathMappings = pathMappings == null ? Map.of() : Map.copyOf(pathMappings);
    }
}
