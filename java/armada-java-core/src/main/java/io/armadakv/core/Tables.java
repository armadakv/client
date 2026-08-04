/*
 * Copyright JAMF Software, LLC
 */

package io.armadakv.core;

import io.armadakv.core.api.CreateTableResponse;
import io.armadakv.core.api.DeleteTableResponse;
import io.armadakv.core.api.ListTablesResponse;
import io.armadakv.core.options.TableOption;

public interface Tables extends CloseableClient {

	CreateTableResponse createTable(String name);

	CreateTableResponse createTable(String name, TableOption option);

	DeleteTableResponse deleteTable(String name);

	DeleteTableResponse deleteTable(String name, TableOption option);

	ListTablesResponse listTables();

	ListTablesResponse listTables(TableOption option);
}
