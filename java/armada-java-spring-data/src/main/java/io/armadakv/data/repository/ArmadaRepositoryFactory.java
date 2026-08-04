/*
 * Copyright JAMF Software, LLC
 */

package io.armadakv.data.repository;

import io.armadakv.data.query.ArmadaQueryCreator;
import org.springframework.data.keyvalue.core.KeyValueOperations;
import org.springframework.data.keyvalue.repository.query.KeyValuePartTreeQuery;
import org.springframework.data.keyvalue.repository.support.KeyValueRepositoryFactory;
import org.springframework.data.repository.query.RepositoryQuery;
import org.springframework.data.repository.query.parser.AbstractQueryCreator;

public class ArmadaRepositoryFactory extends KeyValueRepositoryFactory {

    public ArmadaRepositoryFactory(KeyValueOperations keyValueOperations) {
        this(keyValueOperations, ArmadaQueryCreator.class);
    }

    public ArmadaRepositoryFactory(KeyValueOperations keyValueOperations,
                                    Class<? extends AbstractQueryCreator<?, ?>> queryCreator) {
        this(keyValueOperations, queryCreator, KeyValuePartTreeQuery.class);
    }

    public ArmadaRepositoryFactory(KeyValueOperations keyValueOperations,
                                    Class<? extends AbstractQueryCreator<?, ?>> queryCreator, Class<? extends RepositoryQuery> repositoryQueryType) {
        super(keyValueOperations, queryCreator, repositoryQueryType);
    }
}
