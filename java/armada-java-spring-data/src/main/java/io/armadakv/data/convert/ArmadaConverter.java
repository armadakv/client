/*
 * Copyright JAMF Software, LLC
 */

package io.armadakv.data.convert;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import io.armadakv.core.api.ByteSequence;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.support.DefaultConversionService;

public class ArmadaConverter implements InitializingBean, ApplicationContextAware {

    private final DefaultConversionService converter = new DefaultConversionService();
    private ApplicationContext applicationContext;

    public ArmadaConverter() {
        ByteSequenceConverters.converters().forEach(converter::addConverter);
    }

    public ByteSequence write(Object source) {
        return converter.convert(source, ByteSequence.class);
    }

    public <T> T read(ByteSequence source, Class<T> clazz) {
        return converter.convert(source, clazz);
    }

    @Override
    public void afterPropertiesSet() {
        this.converter.addConverter(new JsonConverterFactory(
                this.applicationContext
                        .getBeanProvider(ObjectMapper.class)
                        .getIfAvailable(ObjectMapper::new)
        ));
        this.converter.addConverter(new XmlConverterFactory(
                this.applicationContext
                        .getBeanProvider(XmlMapper.class)
                        .getIfAvailable(XmlMapper::new)
        ));
        this.applicationContext.getBeansOfType(Converter.class).values().forEach(converter::addConverter);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
