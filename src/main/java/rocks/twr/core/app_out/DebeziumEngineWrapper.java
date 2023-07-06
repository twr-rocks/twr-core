package rocks.twr.core.app_out;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.debezium.embedded.EmbeddedEngine;
import io.debezium.engine.ChangeEvent;
import io.debezium.engine.DebeziumEngine;
import io.debezium.engine.format.Json;
import io.debezium.relational.HistorizedRelationalDatabaseConnectorConfig;
import io.debezium.storage.kafka.history.KafkaSchemaHistory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rocks.twr.api.DebeziumRawMapper;
import rocks.twr.api.out.DatabaseType;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class DebeziumEngineWrapper implements Runnable, AutoCloseable, DebeziumEngine.ConnectorCallback, DebeziumEngine.CompletionCallback {

    private final Logger log = LoggerFactory.getLogger(this.getClass());
    private final DebeziumEngine<ChangeEvent<String, String>> engine;
    private CountDownLatch startupLatch;
    private DebeziumRawMapper mapper;

    // TODO rename prefix to be more useful
    public DebeziumEngineWrapper(DatabaseType dbType, String prefix, Properties props, ObjectMapper om,
                                 Consumer<DebeziumRawMapper.MappedEvent> changeHandler) {

        startupLatch = new CountDownLatch(1);

        props.setProperty("name", "twr-engine");
        props.setProperty(EmbeddedEngine.OFFSET_STORAGE.name(), org.apache.kafka.connect.storage.KafkaOffsetBackingStore.class.getName());

        if(DatabaseType.Mysql.equals(dbType)) {
            props.setProperty(EmbeddedEngine.CONNECTOR_CLASS.name(), "io.debezium.connector.mysql.MySqlConnector");
        } else {
            throw new IllegalArgumentException("unexpected database type " + dbType + " - please contact https://twr.rocks");
        }

        if(!props.containsKey(EmbeddedEngine.OFFSET_FLUSH_INTERVAL_MS_PROP)) {
            props.setProperty(EmbeddedEngine.OFFSET_FLUSH_INTERVAL_MS_PROP, "60000");
        }

        props.setProperty("topic.prefix", prefix); // TODO what effect does this actually have?
        props.setProperty(HistorizedRelationalDatabaseConnectorConfig.SCHEMA_HISTORY.name(), KafkaSchemaHistory.class.getName());

        engine = DebeziumEngine.create(Json.class)
                .using(props)
                .using((DebeziumEngine.CompletionCallback) this)
                .using((DebeziumEngine.ConnectorCallback) this)
                .notifying(record -> {
                    log.debug("TWRD_CDCDEB_01 receiving change {}", record); // TODO is destination or headers ever relevant? or only when you use debezium transformers?
                    if(mapper == null) {
                        mapper = new DefaultDebeziumRawMapper(om);
                    }
//TODO continue here                    changeHandler.accept(mapper.map(record.key(), record.value()));
                    changeHandler.accept(new DebeziumRawMapper.MappedEvent("asdf", null, "TODO".getBytes(StandardCharsets.UTF_8), null));
                })
                .build();
    }

    public void run() {
        engine.run();
    }

    @Override
    public void close() throws IOException, InterruptedException {
        engine.close();

        // and now it would be nice to await actual completion, but debezium doesn't offer that at the moment
        // https://issues.redhat.com/browse/DBZ-6629
        // DebeziumEngine e = engine;
        // ((EmbeddedEngine)e).await(10, TimeUnit.SECONDS);
    }

    @Override
    public void connectorStarted() {
        log.info("TWRI_CDCDEB_02 connector started");
    }

    @Override
    public void connectorStopped() {
        log.info("TWRI_CDCDEB_12 connector stopped");
        startupLatch = new CountDownLatch(1);
    }

    @Override
    public void taskStarted() {
        log.info("TWRI_CDCDEB_03 task started");
        startupLatch.countDown();
    }

    @Override
    public void taskStopped() {
        log.info("TWRI_CDCDEB_11 task stopped");
    }

    public void awaitUntilRunning(long timeout, TimeUnit unit) throws InterruptedException {
        startupLatch.await(timeout, unit);
    }

    @Override
    public void handle(boolean success, String message, Throwable error) {
        if(success) {
            log.info("TWRI_CDCDEB_14 connector completed successfully. {}. exception: {}", message, error);
        } else {
            log.warn("TWRW_CDCDEB_15 connector completed with a failure. {}. exception: {}", message, error);
        }
    }
}
