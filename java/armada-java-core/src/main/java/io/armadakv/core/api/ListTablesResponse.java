/*
 * Copyright JAMF Software, LLC
 */

package io.armadakv.core.api;

import java.util.List;

public record ListTablesResponse(List<TableInfo> tables) {
}
