package rocks.twr.core.app_out;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.lifecycle.Startables;
import org.testcontainers.redpanda.RedpandaContainer;
import org.testcontainers.utility.DockerImageName;

import java.awt.*;
import java.io.IOException;
import java.net.URI;

public class AbstractContainerBaseTest {

    static Network network = Network.newNetwork();
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0.33")
            .withDatabaseName("test")
            .withUsername("cdc")
            .withPassword("cdcpwd")
            .withReuse(true)
            .withNetwork(network)
            .withNetworkAliases("mysql")
        ;
    static RedpandaContainer redpanda = new RedpandaContainer("docker.redpanda.com/redpandadata/redpanda:v23.1.12")
            .withNetwork(network)
            .withNetworkAliases("redpanda")
            .withReuse(true);
    static GenericContainer redpandaconsole = new GenericContainer(DockerImageName.parse("redpandadata/console:v2.2.4"))
            .dependsOn(redpanda)
            .withNetwork(network)
            .withExposedPorts(8080)
            .withNetworkAliases("redpandaconsole")
            .withEnv("KAFKA_BROKERS", "redpanda:29092") // check impl of RedpandaContainer, and the port used internallyis 29092
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
