/*
 * Copyright JAMF Software, LLC
 */

package io.armadakv.data.convert;

import io.armadakv.core.api.ByteSequence;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.GenericApplicationContext;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ArmadaConverterTest {

    @ArmadaValueMapping(ArmadaValueMapping.Type.JSON)
    public record JsonEntity(String id, String label, List<String> tags) {
    }

    @ArmadaValueMapping(ArmadaValueMapping.Type.XML)
    public record XmlEntity(String id, String label) {
    }

    private static ArmadaConverter converter(GenericApplicationContext context) {
        context.refresh();
        var converter = new ArmadaConverter();
        converter.setApplicationContext(context);
        converter.afterPropertiesSet();
        return converter;
    }

    @Test
    void jsonRoundTripWithTheDefaultMapper() {
        var converter = converter(new GenericApplicationContext());
        var entity = new JsonEntity("foo", "label", List.of("a", "b"));

        ByteSequence written = converter.write(entity);

        assertThat(written.toString(StandardCharsets.UTF_8)).isEqualTo("{\"id\":\"foo\",\"label\":\"label\",\"tags\":[\"a\",\"b\"]}");
        assertThat(converter.read(written, JsonEntity.class)).isEqualTo(entity);
    }

    @Test
    void xmlRoundTripWithTheDefaultMapper() {
        var converter = converter(new GenericApplicationContext());
        var entity = new XmlEntity("foo", "label");

        ByteSequence written = converter.write(entity);

        assertThat(written.toString(StandardCharsets.UTF_8)).contains("<id>foo</id>").contains("<label>label</label>");
        assertThat(converter.read(written, XmlEntity.class)).isEqualTo(entity);
    }

    @Test
    void usesTheJsonMapperBeanWhenOneIsDefined() {
        var context = new GenericApplicationContext();
        context.registerBean(JsonMapper.class, () -> JsonMapper.builder().enable(SerializationFeature.INDENT_OUTPUT).build());
        var converter = converter(context);

        ByteSequence written = converter.write(new JsonEntity("foo", "label", List.of()));

        assertThat(written.toString(StandardCharsets.UTF_8)).contains("\n");
    }
}
