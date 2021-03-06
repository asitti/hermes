package pl.allegro.tech.hermes.consumers.consumer.result;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.common.message.undelivered.UndeliveredMessageLog;
import pl.allegro.tech.hermes.common.metric.Counters;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
import pl.allegro.tech.hermes.common.metric.Meters;
import pl.allegro.tech.hermes.consumers.consumer.Message;
import pl.allegro.tech.hermes.consumers.consumer.offset.SubscriptionOffsetCommitQueues;
import pl.allegro.tech.hermes.consumers.consumer.sender.MessageSendingResult;
import pl.allegro.tech.hermes.tracker.consumers.Trackers;

import java.time.Clock;

import static pl.allegro.tech.hermes.api.SentMessageTrace.createUndeliveredMessage;
import static pl.allegro.tech.hermes.consumers.consumer.message.MessageConverter.toMessageMetadata;

public class DefaultErrorHandler extends AbstractHandler implements ErrorHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultErrorHandler.class);

    private final UndeliveredMessageLog undeliveredMessageLog;
    private final Clock clock;
    private final Trackers trackers;
    private final String cluster;

    public DefaultErrorHandler(SubscriptionOffsetCommitQueues offsetHelper, HermesMetrics hermesMetrics,
                               UndeliveredMessageLog undeliveredMessageLog, Clock clock, Trackers trackers, String cluster) {
        super(offsetHelper, hermesMetrics);
        this.undeliveredMessageLog = undeliveredMessageLog;
        this.clock = clock;
        this.trackers = trackers;
        this.cluster = cluster;
    }

    @Override
    public void handleDiscarded(Message message, Subscription subscription, MessageSendingResult result) {
        if(!result.hasHttpAnswer() && !result.isTimeout()) {
            LOGGER.warn(
                    "Abnormal delivery failure: subscription: {}; cause: {}; endpoint: {}; messageId: {}; partition: {}; offset: {}",
                    subscription.getId(), result.getRootCause(), subscription.getEndpoint(), message.getId(),
                    message.getPartition(), message.getOffset(), result.getFailure()
            );
        }

        offsetHelper.remove(message);

        updateMeters(subscription);
        updateMetrics(Counters.DISCARDED, message, subscription);

        undeliveredMessageLog.add(createUndeliveredMessage(subscription, new String(message.getData()), result.getFailure(), clock.millis(),
                message.getPartition(), message.getOffset(), cluster));

        trackers.get(subscription).logDiscarded(toMessageMetadata(message, subscription), result.getRootCause());
    }

    private void updateMeters(Subscription subscription) {
        hermesMetrics.meter(Meters.DISCARDED_METER).mark();
        hermesMetrics.meter(Meters.DISCARDED_TOPIC_METER, subscription.getTopicName()).mark();
        hermesMetrics.meter(Meters.DISCARDED_SUBSCRIPTION_METER, subscription.getTopicName(), subscription.getName()).mark();
    }

    @Override
    public void handleFailed(Message message, Subscription subscription, MessageSendingResult result) {
        hermesMetrics.meter(Meters.FAILED_METER_SUBSCRIPTION, subscription.getTopicName(), subscription.getName()).mark();
        registerFailureMetrics(subscription, result);
        trackers.get(subscription).logFailed(toMessageMetadata(message, subscription), result.getRootCause());
    }

    private void registerFailureMetrics(Subscription subscription, MessageSendingResult result) {
        if (result.hasHttpAnswer()) {
            hermesMetrics.registerConsumerHttpAnswer(subscription, result.getStatusCode());
        }
        else if (result.isTimeout()) {
            hermesMetrics.consumerErrorsTimeoutMeter(subscription).mark();
        }
        else {
            hermesMetrics.consumerErrorsOtherMeter(subscription).mark();
        }
    }
}
