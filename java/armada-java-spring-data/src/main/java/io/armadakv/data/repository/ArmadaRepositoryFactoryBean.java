/*
 * Copyright JAMF Software, LLC
 */

package io.armadakv.data.repository;

import org.springframework.data.keyvalue.core.KeyValueOperations;
import org.springframework.data.keyvalue.repository.support.KeyValueRepositoryFactoryBean;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.RepositoryQuery;
import org.springframework.data.repository.query.parser.AbstractQueryCreator;

public class ArmadaRepositoryFactoryBean<T extends Repository<S, ID>, S, ID> extends KeyValueRepositoryFactoryBean<T, S, ID> {

    public ArmadaRepositoryFactoryBean(Class<? extends T> repositoryInterface) {
        super(repositoryInterface);
    }

    @Override
    protected ArmadaRepositoryFactory createRepositoryFactory(KeyValueOperations operations,
                                                               Class<? extends AbstractQueryCreator<?, ?>> queryCreator,
                                                               Class<? extends RepositoryQuery> repositoryQueryType) {
        return new ArmadaRepositoryFactory(operations, queryCreator, repositoryQueryType);
    }
}

