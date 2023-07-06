package rocks.twr.core.app_out;

import com.github.dockerjava.api.command.InspectContainerResponse;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.lifecycle.Startables;
import org.testcontainers.redpanda.RedpandaContainer;
import org.testcontainers.temp.NetworkImpl;
import org.testcontainers.utility.DockerImageName;

import java.awt.*;
import java.io.IOException;
import java.net.URI;

public class AbstractContainerBaseTest {

    static Network network = new NetworkImpl("twr-core"); //Network.newNetwork();
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0.33")
            .withDatabaseName("test")
            .withUsername("cdc")
            .withPassword("cdcpwd")
            .withReuse(true)
            .withNetwork(network)
            .withNetworkAliases("mysql")
        ;
    static RedpandaContainer redpanda = new RedpandaContainer("docker.redpanda.com/redpandadata/redpanda:v23.1.12") {
                // https://github.com/testcontainers/testcontainers-java/issues/7302
                @Override
                protected void containerIsStarting(InspectContainerResponse containerInfo) {
                    //super.containerIsStarting(containerInfo);
                    String command = "#!/bin/bash\n";
                    command = command + "/usr/bin/rpk redpanda start --mode dev-container --smp 1 --memory 1G ";
                    command = command + "--kafka-addr PLAINTEXT://0.0.0.0:29092,OUTSIDE://0.0.0.0:9092 ";
                    command = command + "--advertise-kafka-addr PLAINTEXT://redpanda:29092,OUTSIDE://" + this.getHost() + ":" + this.getMappedPort(9092);
                    this.copyFileToContainer(Transferable.of(command, 511), "/testcontainers_start.sh");
                }
            }
            .withNetwork(network)
            .withNetworkAliases("redpanda")
            .withReuse(true);
    static GenericContainer redpandaconsole = new GenericContainer(DockerImageName.parse("redpandadata/console:v2.2.4"))
            .dependsOn(redpanda)
            .withNetwork(network)
            .withExposedPorts(8080)
            .withNetworkAliases("redpandaconsole")
            .withEnv("KAFKA_BROKERS", "redpanda:29092")
            .withEnv("KAFKA_TLS_ENABLED", "false")
            .withEnv("KAFKA_SASL_ENABLED", "false")
            .withEnv("CONNECT_ENABLED", "false")
            .withReuse(true);

    static {
        Startables.deepStart(mysql, redpanda, redpandaconsole).join();
        logAndOpenRedpandaConsoleInBrowser();
    }

    private static void logAndOpenRedpandaConsoleInBrowser() {
        String url = ("http://localhost:" + redpandaconsole.getMappedPort(8080));
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop desktop = Desktop.getDesktop();
                if (desktop.isSupported(Desktop.Action.BROWSE)) {
                    desktop.browse(URI.create(url));
                }
            }
        } catch (IOException | InternalError e) {
            e.printStackTrace();
        }
        System.out.println("Redpanda Console running on " + url);
    }

}
