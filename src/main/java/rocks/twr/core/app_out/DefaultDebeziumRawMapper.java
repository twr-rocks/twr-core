package rocks.twr.core.app_out;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import rocks.twr.api.DebeziumRawMapper;

import javax.inject.Inject;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static java.util.Collections.emptyMap;

public class DefaultDebeziumRawMapper implements DebeziumRawMapper {

    enum Op {
        r, c, u, d
    }

    record Value(Payload payload){}
    record Payload(String ddl, String schemaName, String databaseName, long ts_ms, Source source, List<TableChange> tableChanges, Op op, JsonNode before, JsonNode after){}
    record TableChange(){}
    record Source(String name, String snapshot, String db, String table){}

    private final ObjectMapper om;

    @Inject
    public DefaultDebeziumRawMapper(ObjectMapper objectMapper) {
        this.om = objectMapper;
    }

    @Override
    public MappedEvent map(String jsonKey, String jsonValue) {
        if (jsonValue != null) {
            Value value;
            try {
                value = om.readValue(jsonValue, Value.class);
            } catch (JsonProcessingException e) {
                throw new IllegalArgumentException("TWREX1201 unable to process incoming json value: " + jsonValue, e);
            }
            if (value.payload != null) {
                Payload payload = value.payload;
                if (payload.op != null) {
                    String op = payload.op.name();
                    if (payload.source != null) {
                        Source src = payload.source;
                        if (src.db != null) {
                            if (src.table != null) {
/*
                                we are going to be opinionated here and assume that we are reading an outbox table.
                                if it contains a column with a topic name, we send the value to that topic, using the
                                aggregateId as the key and the destination column as the topic name.
                                we are going to default to json, but allow you to add a mapper to turn it into avro at the next stage.

                                because we are nice, we could also let you map from your own table into our standard outbox dto

                                ObjectNode record = getOutputRecord(v, payload, op, src.get(DB).asText(), src.get(TABLE).asText());
                                fixCamelCase(record);
                                setType(v, payload, op, record);
                                return Collections.singleton(new KeyValue<>(new Value(record).getKey(), record));
 */
                                return new MappedEvent(null, jsonKey.getBytes(StandardCharsets.UTF_8), jsonValue.getBytes(StandardCharsets.UTF_8), emptyMap()/*TODO*/);
                            } else {
                                throw new IllegalArgumentException("TWREX0020 record with no table field: " + jsonValue);
                            }
                        } else {
                            throw new IllegalArgumentException("TWREX0014 record with no db field: " + jsonValue);
                        }
                    } else {
                        throw new IllegalArgumentException("TWREX0012 record with no source: " + jsonValue);
                    }
                } else {
                    throw new IllegalArgumentException("TWREX0011 record with no op: " + jsonValue);
                }
            } else {
                throw new IllegalArgumentException("TWREX0010 record value with no payload: " + jsonValue);
            }
        } else {
            return handleTombstone();
        }
    }

    private MappedEvent handleTombstone() {
        // tombstone sent by debezium -> we'd like to pass it on, but cannot, coz the aggregateId is missing
        // the key is like this, and value is null (since tombstone):
        // {"schema":{"type":"struct",
        //              "fields":[{"type":"int32","optional":false,"field":"id"}],
        //              "optional":false,
        //              "name":"connect-cdc-ant_kutschera_gmail_comchcdg_refimplextended_jarbjbhh-T_TXOUTBOX.Key"},
        //              "payload":{"id":1}
        // }
        // so, ignore and flatMap to nowt
        return null; // TODO what do we want to do here? in the poc we returned an empty set. ew MappedEvent(?, key, null) handleTombstone();
    }

}
