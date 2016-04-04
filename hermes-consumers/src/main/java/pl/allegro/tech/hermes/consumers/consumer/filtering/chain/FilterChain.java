package pl.allegro.tech.hermes.consumers.consumer.filtering.chain;

import pl.allegro.tech.hermes.consumers.consumer.Message;
import pl.allegro.tech.hermes.consumers.consumer.filtering.MessageFilter;

import java.util.List;

public class FilterChain {

    public FilterChain(final List<MessageFilter> messageFilters) {
        this.messageFilters = messageFilters;
    }

    private List<MessageFilter> messageFilters;

    public FilterResult apply(final Message message) {
        for (MessageFilter filter : messageFilters) {
            try {
                if (!filter.test(message)) {
                    return FilterResult.failed(filter.getType(), "logical");
                }
            } catch (Exception ex) {
                return FilterResult.failed(filter.getType(), ex.getMessage());
            }
        }
        return FilterResult.PASS;
    }
}
