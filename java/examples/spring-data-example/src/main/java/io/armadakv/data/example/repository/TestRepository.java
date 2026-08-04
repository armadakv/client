/*
 * Copyright JAMF Software, LLC
 */

package io.armadakv.data.example.repository;

import io.armadakv.data.example.entity.TestEntity;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.ListPagingAndSortingRepository;

import java.util.List;

public interface TestRepository extends ListCrudRepository<TestEntity, String>, ListPagingAndSortingRepository<TestEntity, String> {

    List<TestEntity> findByLabel(String name);

}
