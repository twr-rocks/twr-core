package rocks.twr.core.app_out;

import io.debezium.embedded.EmbeddedEngine;
import io.debezium.engine.ChangeEvent;
import io.debezium.engine.DebeziumEngine;
import io.debezium.engine.format.Json;
import io.debezium.relational.HistorizedRelationalDatabaseConnectorConfig;
import io.debezium.storage.kafka.history.KafkaSchemaHistory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rocks.twr.api.out.DatabaseType;

import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class DebeziumEngineWrapper implements Runnable, AutoCloseable, DebeziumEngine.ConnectorCallback {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private final DebeziumEngine<ChangeEvent<String, String>> engine;
    private CountDownLatch startupLatch;

    // TODO rename prefix to be more useful
    public DebeziumEngineWrapper(DatabaseType dbType, String prefix, Properties props, Consumer<ChangeEvent<String, String>> changeHandler) {

        startupLatch = new CountDownLatch(1);

        props.setProperty("name", "twr-engine");
        props.setProperty(EmbeddedEngine.OFFSET_STORAGE.name(), org.apache.kafka.connect.storage.KafkaOffsetBackingStore.class.getName());

        if(DatabaseType.Mysql.equals(dbType)) {
            props.setProperty(EmbeddedEngine.CONNECTOR_CLASS.name(), "io.debezium.connector.mysql.MySqlConnector");
        } else {
            throw new IllegalArgumentException("unexpected database type " + dbType + " - please contact https://twr.rocks");
        }

        // TODO doesnt it need topic names?
        // TODO props.setProperty("config.storage", org.apache.kafka.connect.storage.KafkaConfigBackingStore.class.getName());
        // TODO props.setProperty("status.storage", org.apache.kafka.connect.storage.KafkaStatusBackingStore.class.getName());

        props.setProperty(EmbeddedEngine.OFFSET_FLUSH_INTERVAL_MS_PROP, "60000"); // TODO do we need this to be smaler?

        props.setProperty("topic.prefix", prefix); // TODO which constant?
        props.setProperty(HistorizedRelationalDatabaseConnectorConfig.SCHEMA_HISTORY.name(), KafkaSchemaHistory.class.getName());

        // TODO doesnt it need topic names?


        engine = DebeziumEngine.create(Json.class)
                .using(props)
                // TODO add other stuff here...
                .notifying(record -> {
                    log.info("got a record: {}", record);
                    changeHandler.accept(record);
                })
                .using(this)
                .build();
    }

    public void run() {
        engine.run();
    }

    public void close() throws IOException {
        engine.close();
    }

    public void connectorStarted() {
    }

    public void connectorStopped() {
        startupLatch = new CountDownLatch(1);
    }

    public void taskStarted() {
        startupLatch.countDown();
    }

    public void taskStopped() {
    }

    public void awaitUntilRunning(long timeout, TimeUnit unit) throws InterruptedException {
        startupLatch.await(timeout, unit);
    }

}
