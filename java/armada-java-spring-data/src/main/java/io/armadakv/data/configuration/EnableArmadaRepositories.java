/*
 * Copyright JAMF Software, LLC
 */

package io.armadakv.data.configuration;

import io.armadakv.data.query.ArmadaQueryCreator;
import io.armadakv.data.repository.ArmadaRepositoryFactoryBean;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.Import;
import org.springframework.data.keyvalue.repository.config.QueryCreatorType;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@Import(ArmadaRepositoriesRegistrar.class)
@QueryCreatorType(ArmadaQueryCreator.class)
public @interface EnableArmadaRepositories {

    String[] value() default {};

    String[] basePackages() default {};

    Class<?>[] basePackageClasses() default {};

    Filter[] includeFilters() default {};

    Filter[] excludeFilters() default {};

    Class<?> repositoryFactoryBeanClass() default ArmadaRepositoryFactoryBean.class;

    String namedQueriesLocation() default "";

    String repositoryImplementationPostfix() default "Impl";

    String keyValueTemplateRef() default "armadaTemplateRef";
}
