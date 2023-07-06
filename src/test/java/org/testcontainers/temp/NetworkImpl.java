package org.testcontainers.temp;

import com.github.dockerjava.api.command.CreateNetworkCmd;
import com.github.dockerjava.api.exception.ConflictException;
import org.junit.rules.ExternalResource;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.Network;
import org.testcontainers.utility.ResourceReaper;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

// https://github.com/testcontainers/testcontainers-java/issues/7261
public class NetworkImpl extends ExternalResource implements Network {

    private final String name;
    private Boolean enableIpv6;
    private String driver;
    private String id;

    private final AtomicBoolean initialized = new AtomicBoolean();

    public NetworkImpl(String name) {
        this.name = name;
    }

    @Override
    public synchronized String getId() {
        if (initialized.compareAndSet(false, true)) {
            boolean success = false;
            try {
                id = create();
                success = true;
            } finally {
                if (!success) {
                    initialized.set(false);
                }
            }
        }
        return id;
    }

    private String create() {
        CreateNetworkCmd createNetworkCmd = DockerClientFactory.instance().client().createNetworkCmd();

        createNetworkCmd.withName(name);
        createNetworkCmd.withCheckDuplicate(true);

        if (enableIpv6 != null) {
            createNetworkCmd.withEnableIpv6(enableIpv6);
        }

        if (driver != null) {
            createNetworkCmd.withDriver(driver);
        }

        Map<String, String> labels = createNetworkCmd.getLabels();
        labels = new HashMap<>(labels != null ? labels : Collections.emptyMap());
        labels.putAll(DockerClientFactory.DEFAULT_LABELS);
        //noinspection deprecation
        labels.putAll(ResourceReaper.instance().getLabels());
        createNetworkCmd.withLabels(labels);

        try {
            return createNetworkCmd.exec().getId();
        } catch (ConflictException e) {
            List<com.github.dockerjava.api.model.Network> networks = DockerClientFactory
                    .instance()
                    .client()
                    .listNetworksCmd()
                    .withNameFilter(name)
                    .exec();
            if(networks.size() >= 1) {
                return networks.get(0).getId();
            }
            throw new IllegalStateException("Unable to create network with name " + name
                    + " due to conflict, but no existing network found with the same name", e);
        }
    }

    @Override
    protected void after() {
        close();
    }

    @Override
    public synchronized void close() {
        if (initialized.getAndSet(false)) {
            ResourceReaper.instance().removeNetworkById(id);
        }
    }
}
