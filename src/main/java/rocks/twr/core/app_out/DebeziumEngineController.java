package rocks.twr.core.app_out;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Properties;

public class DebeziumEngineController implements ConsumerRebalanceListener {

    private final DebeziumEngineWrapper debeziumEngineWrapper;

    public DebeziumEngineController(String topicToUseForLeaderElection, String groupId, Properties kafkaConsumerProps, DebeziumEngineWrapper debeziumEngineWrapper) {
        this.debeziumEngineWrapper = debeziumEngineWrapper;
        kafkaConsumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        kafkaConsumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        kafkaConsumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        kafkaConsumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        KafkaConsumer consumer = new KafkaConsumer<>(kafkaConsumerProps);
        consumer.subscribe(List.of(topicToUseForLeaderElection), this);
    }

    @Override
    public void onPartitionsRevoked(Collection<TopicPartition> partitions) {
        try {
            debeziumEngineWrapper.close();
        } catch (IOException | InterruptedException e) {
            throw new IllegalStateException("unable to close debezium engine", e);
        }
    }

    @Override
    public void onPartitionsAssigned(Collection<TopicPartition> partitions) {

    }

    // not needed, as default calls onPartitionsRevoked:
    // @Override public void onPartitionsLost(Collection<TopicPartition> partitions) {
}
