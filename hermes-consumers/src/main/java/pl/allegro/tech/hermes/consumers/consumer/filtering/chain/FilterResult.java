package pl.allegro.tech.hermes.consumers.consumer.filtering.chain;

import java.util.Optional;

public class FilterResult {

    public static final FilterResult PASS = new FilterResult(false, Optional.empty(), Optional.empty());

    public static FilterResult failed(final String filterType, String message) {
        return new FilterResult(true, Optional.of(filterType), Optional.ofNullable(message));
    }

    public final boolean filtered;
    public final Optional<String> filterType;
    public final Optional<String> message;

    private FilterResult(final boolean filtered, final Optional<String> filterType, Optional<String> message) {
        this.filtered = filtered;
        this.filterType = filterType;
        this.message = message;
    }
}
