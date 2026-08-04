/*
 * Copyright JAMF Software, LLC
 */

package io.armadakv.data.configuration;

import io.armadakv.data.convert.ArmadaConverter;
import io.armadakv.data.core.ArmadaKeyValueAdapter;
import io.armadakv.data.core.ArmadaKeyValueTemplate;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.data.keyvalue.annotation.KeySpace;
import org.springframework.data.keyvalue.repository.config.KeyValueRepositoryConfigurationExtension;
import org.springframework.data.repository.config.RepositoryConfigurationSource;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.Collections;

public class ArmadaRepositoryConfigurationExtension extends KeyValueRepositoryConfigurationExtension {

    private static final String ARMADA_ADAPTER_BEAN_NAME = "armadaKeyValueAdapter";
    private static final String ARMADA_CONVERTER_BEAN_NAME = "armadaConverter";
    private static final String ARMADA_CLIENT_BEAN_NAME = "armadaClient";

    private static AbstractBeanDefinition createArmadaConverterDefinition() {
        return BeanDefinitionBuilder.genericBeanDefinition(ArmadaConverter.class)
                .getBeanDefinition();
    }

    @Override
    public String getModuleName() {
        return "Armada";
    }

    @Override
    protected String getDefaultKeyValueTemplateRef() {
        return "armadaTemplateRef";
    }

    @Override
    protected Collection<Class<? extends Annotation>> getIdentifyingAnnotations() {
        return Collections.singleton(KeySpace.class);
    }

    @Override
    protected AbstractBeanDefinition getDefaultKeyValueTemplateBeanDefinition(RepositoryConfigurationSource configurationSource) {
        return BeanDefinitionBuilder.rootBeanDefinition(ArmadaKeyValueTemplate.class)
                .addConstructorArgReference(ARMADA_ADAPTER_BEAN_NAME)
                .getBeanDefinition();
    }

    @Override
    public void registerBeansForRoot(BeanDefinitionRegistry registry, RepositoryConfigurationSource configuration) {
        registerIfNotAlreadyRegistered(ArmadaRepositoryConfigurationExtension::createArmadaConverterDefinition, registry, ARMADA_CONVERTER_BEAN_NAME,
                configuration.getSource());
        registerIfNotAlreadyRegistered(() -> createArmadaKeyValueAdapter(configuration), registry, ARMADA_ADAPTER_BEAN_NAME,
                configuration.getSource());

        super.registerBeansForRoot(registry, configuration);
    }

    private AbstractBeanDefinition createArmadaKeyValueAdapter(RepositoryConfigurationSource configuration) {
        BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(ArmadaKeyValueAdapter.class)
                .addConstructorArgReference(ARMADA_CLIENT_BEAN_NAME)
                .addConstructorArgReference(ARMADA_CONVERTER_BEAN_NAME);
        return builder.getBeanDefinition();
    }

}
