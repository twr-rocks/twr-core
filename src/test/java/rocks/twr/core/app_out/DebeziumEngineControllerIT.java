package rocks.twr.core.app_out;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.CommonClientConfigs;
import org.junit.jupiter.api.Test;
import rocks.twr.api.out.DatabaseType;

import java.util.Properties;

class DebeziumEngineControllerIT extends AbstractContainerBaseTest {

    @Test
    void test() {
        Properties props = new Properties();
        props.put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, redpanda.getBootstrapServers());

        DebeziumEngineWrapper debeziumEngineWrapper = new DebeziumEngineWrapper(DatabaseType.Mysql, "prefix", props, new ObjectMapper(), me -> {
            System.out.println("HERE: " + me);
        });

        DebeziumEngineController sut = new DebeziumEngineController("debezium-schema-history-internal", "yourAppName", props, debeziumEngineWrapper);
    }

}