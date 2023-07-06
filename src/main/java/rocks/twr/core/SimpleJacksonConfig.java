package rocks.twr.core;

import com.fasterxml.jackson.core.json.JsonWriteFeature;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

// TODO clean this up
public class SimpleJacksonConfig {

    private static final ObjectMapper OM = new ObjectMapper();

    static {
        new SimpleJacksonConfig().customize(OM);
    }

    public ObjectMapper om() {
        return OM;
    }

    public void customize(ObjectMapper mapper) {

        /*
        //allow deser of anysubclasses
        val bptv = BasicPolymorphicTypeValidator.builder()
                //.allowIfSubTypeIsArray()
                .allowIfBaseType("dev.abstratium")
                .allowIfSubType("dev.abstratium")
                .build()
        var typer: TypeResolverBuilder<*> = ObjectMapper.DefaultTypeResolverBuilder.construct(
                ObjectMapper.DefaultTyping.NON_CONCRETE_AND_ARRAYS, bptv)
        typer = typer.init(JsonTypeInfo.Id.MINIMAL_CLASS, null)
        typer = typer.inclusion(JsonTypeInfo.As.PROPERTY)
        typer = typer.typeProperty("c*c")
        */

        mapper.enable(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT)
                .enable(JsonWriteFeature.ESCAPE_NON_ASCII.mappedFeature())
                .disable(SerializationFeature.INDENT_OUTPUT)
                .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .registerModule(new Jdk8Module())
                .registerModule(new JavaTimeModule());
//                .setDefaultTyping(typer)
//                .activateDefaultTypingAsProperty(PTV(), ObjectMapper.DefaultTyping.OBJECT_AND_NON_CONCRETE, "@c")
    }

    public static ObjectMapper getOM() {
        return OM;
    }
}
