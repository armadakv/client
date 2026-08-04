/*
 * Copyright JAMF Software, LLC
 */

package io.armadakv.data.configuration;

import org.springframework.data.repository.config.RepositoryBeanDefinitionRegistrarSupport;
import org.springframework.data.repository.config.RepositoryConfigurationExtension;

import java.lang.annotation.Annotation;

public class ArmadaRepositoriesRegistrar extends RepositoryBeanDefinitionRegistrarSupport {

    @Override
    protected Class<? extends Annotation> getAnnotation() {
        return EnableArmadaRepositories.class;
    }

    @Override
    protected RepositoryConfigurationExtension getExtension() {
        return new ArmadaRepositoryConfigurationExtension();
    }

}
