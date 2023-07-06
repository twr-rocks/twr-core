package rocks.twr.core.app_out;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.debezium.relational.HistorizedRelationalDatabaseConnectorConfig;
import org.apache.kafka.connect.runtime.distributed.DistributedConfig;
import org.junit.jupiter.api.Test;
import rocks.twr.api.out.DatabaseType;
import rocks.twr.core.SimpleJacksonConfig;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class DebeziumEngineWrapperIT extends AbstractContainerBaseTest {

    @Test
    void testEngine() throws IOException, InterruptedException, SQLException, ClassNotFoundException {

        Properties props = new Properties();
        props.setProperty("database.hostname", "localhost");
        props.setProperty("database.port", String.valueOf(mysql.getMappedPort(3306)));

        // TODO grant the following MySQL permissions according to https://debezium.io/documentation/reference/2.3/connectors/mysql.html:
        //      SELECT, RELOAD, SHOW DATABASES, REPLICATION SLAVE, REPLICATION CLIENT
        props.setProperty("database.user", "root"); // testcontainers sets the same password for the root user. debezium needs some hefty privileges. for testing, this is the simplest solution rather than using: mysql.getUsername()
        props.setProperty("database.password", mysql.getPassword());
        props.setProperty("database.server.id", "85744");
        props.setProperty("database.include.list", "test");
        props.setProperty("table.include.list", "tasks");
        props.setProperty("bootstrap.servers", "localhost:" + redpanda.getMappedPort(9092));
        props.setProperty(DistributedConfig.OFFSET_STORAGE_TOPIC_CONFIG, "debezium-offset-storage");
        props.setProperty(DistributedConfig.OFFSET_STORAGE_PARTITIONS_CONFIG, "25");
        props.setProperty(DistributedConfig.OFFSET_STORAGE_REPLICATION_FACTOR_CONFIG, "1");

        props.setProperty(DistributedConfig.OFFSET_STORAGE_REPLICATION_FACTOR_CONFIG, "1");
        props.setProperty(HistorizedRelationalDatabaseConnectorConfig.SCHEMA_HISTORY.name() + ".kafka.topic", "debezium-schema-history-internal");
        props.setProperty(HistorizedRelationalDatabaseConnectorConfig.SCHEMA_HISTORY.name() + ".kafka.bootstrap.servers", "localhost:" + redpanda.getMappedPort(9092));

        /* TODO
            "schema.history.internal.consumer.security.protocol": "SASL_PLAINTEXT",
            "schema.history.internal.consumer.sasl.mechanism": "SCRAM-SHA-256",
            "schema.history.internal.consumer.sasl.jaas.config": "org.apache.kafka.common.security.scram.ScramLoginModule required username=\"cdc\" password=\"dk32kdcnk392dk\";",
            "schema.history.internal.producer.security.protocol": "SASL_PLAINTEXT",
            "schema.history.internal.producer.sasl.mechanism": "SCRAM-SHA-256",
            "schema.history.internal.producer.sasl.jaas.config": "org.apache.kafka.common.security.scram.ScramLoginModule required username=\"cdc\" password=\"dk32kdcnk392dk\";",
            "schema.history.internal.kafka.bootstrap.servers": "abstratium-redpanda:9092",
            "schema.history.internal.kafka.topic": "connect-cdc-schema-changes"
         */

        JdbcService jdbcService = new JdbcService("com.mysql.cj.jdbc.Driver", mysql.getJdbcUrl(), mysql.getUsername(), mysql.getPassword());

        List<String> collectedRecords = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);
        ObjectMapper om = SimpleJacksonConfig.getOM();
        try(DebeziumEngineWrapper wrapper = new DebeziumEngineWrapper(DatabaseType.Mysql, "test", props, om, changeEvent -> {
            System.out.println("HERE: " + changeEvent);
            collectedRecords.add(new String(changeEvent.value()));
            latch.countDown();
        })) {
            Executors.newFixedThreadPool(1).execute(wrapper);

            wrapper.awaitUntilRunning(60, TimeUnit.SECONDS);
            System.out.println("CREATING TASKS TABLE");
            jdbcService.executeUpdate("create table if not exists tasks(id varchar(255), aggregate varchar(255) )");
            System.out.println("INSERTING TASK");
            jdbcService.executeUpdate("insert into tasks(id, aggregate) values ('" + System.currentTimeMillis() + "', '" + """
                    {
                        "name": "fred"
                    }
                    """ + "')");

            latch.await();

            assertEquals(1, collectedRecords.size());
            fail("TODO more assertions");
        }
    }
}
