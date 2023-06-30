package rocks.twr.core.app_out;

import rocks.twr.api.out.PublisherService;

import java.util.Map;

public class PublisherServiceImpl implements PublisherService {

    public PublisherServiceImpl(JdbcService jdbcService) {

    }

    @Override
    public void send(String key, Object value, Map<String, String> headers) {

    }
}
