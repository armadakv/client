/*
 * Copyright JAMF Software, LLC
 */

package io.armadakv.data.example.entity;

import io.armadakv.data.convert.ArmadaValueMapping;
import org.springframework.data.annotation.Id;
import org.springframework.data.keyvalue.annotation.KeySpace;

import java.util.List;

@KeySpace("armada-test")
@ArmadaValueMapping(ArmadaValueMapping.Type.JSON)
public record TestEntity(@Id String id, String label, List<AdditionalInfo> additionalInfo) {
}
