/*
 * Copyright JAMF Software, LLC
 */

package io.armadakv.data.convert;

import io.armadakv.core.api.ByteSequence;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.ConditionalGenericConverter;
import tools.jackson.dataformat.xml.XmlMapper;

import java.util.Optional;
import java.util.Set;

public class XmlConverterFactory implements ConditionalGenericConverter {

    private final XmlMapper mapper;

    public XmlConverterFactory(XmlMapper mapper) {
        this.mapper = mapper;
    }

    private static boolean isMarshallableType(TypeDescriptor targetType) {
        return targetType.isAssignableTo(TypeDescriptor.valueOf(byte[].class)) || targetType.isAssignableTo(TypeDescriptor.valueOf(ByteSequence.class));
    }

    private static boolean hasJsonValueType(TypeDescriptor targetType) {
        return Optional.ofNullable(targetType.getType().getAnnotation(ArmadaValueMapping.class))
                .map(ArmadaValueMapping::value)
                .map(type -> type == ArmadaValueMapping.Type.XML)
                .orElse(false);
    }

    @Override
    public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
        return (isMarshallableType(targetType) && hasJsonValueType(sourceType))
                || (isMarshallableType(sourceType) && hasJsonValueType(targetType));
    }

    @Override
    public Set<ConvertiblePair> getConvertibleTypes() {
        return null;
    }

    @Override
    public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
        // Jackson 3 reports failures as unchecked JacksonExceptions.
        if (sourceType.isAssignableTo(TypeDescriptor.valueOf(byte[].class))) {
            return mapper.readValue((byte[]) source, targetType.getObjectType());
        } else if (sourceType.isAssignableTo(TypeDescriptor.valueOf(ByteSequence.class))) {
            return mapper.readValue(((ByteSequence) source).getBytes(), targetType.getObjectType());
        } else if (targetType.isAssignableTo(TypeDescriptor.valueOf(ByteSequence.class))) {
            return ByteSequence.from(mapper.writeValueAsBytes(source));
        }
        return mapper.writeValueAsBytes(source);
    }
}
