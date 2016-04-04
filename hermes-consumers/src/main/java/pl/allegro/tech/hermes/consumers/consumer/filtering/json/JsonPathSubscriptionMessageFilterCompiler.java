package pl.allegro.tech.hermes.consumers.consumer.filtering.json;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import pl.allegro.tech.hermes.api.ContentType;
import pl.allegro.tech.hermes.api.MessageFilterSpecification;
import pl.allegro.tech.hermes.consumers.consumer.Message;
import pl.allegro.tech.hermes.consumers.consumer.filtering.FilteringException;
import pl.allegro.tech.hermes.consumers.consumer.filtering.SubscriptionMessageFilterCompiler;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import static com.jayway.jsonpath.Configuration.defaultConfiguration;
import static pl.allegro.tech.hermes.consumers.consumer.filtering.FilteringException.assertTrue;

public class JsonPathSubscriptionMessageFilterCompiler implements SubscriptionMessageFilterCompiler {
    private Configuration configuration = defaultConfiguration().addOptions(Option.ALWAYS_RETURN_LIST, Option.SUPPRESS_EXCEPTIONS);
    private String path;
    private Pattern matcher;

    @Override
    public String getType() {
        return "jsonpath";
    }

    @Override
    public Predicate<Message> compile(MessageFilterSpecification specification) {
        this.matcher = Pattern.compile(specification.getMatcher());
        this.path = specification.getPath();
        return this::test;
    }

    private boolean test(Message message) {
        assertTrue(message.getContentType() == ContentType.JSON, "This filter supports only JSON contentType.");
        try {
            List<Object> result = JsonPath.parse(new ByteArrayInputStream(message.getData()), configuration).read(path);
            return !result.isEmpty() && result.stream()
                    .map(Objects::toString)
                    .allMatch(o -> matcher.matcher(o).matches());
        } catch (Exception ex) {
            throw new FilteringException(ex);
        }
    }
}
