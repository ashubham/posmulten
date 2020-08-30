/**
 *     Posmulten library is an open-source project for the generation
 *     of SQL DDL statements that make it easy for implementation of
 *     Shared Schema Multi-tenancy strategy via the Row Security
 *     Policies in the Postgres database.
 *
 *     Copyright (C) 2020  Szymon Tarnowski
 *
 *     This library is free software; you can redistribute it and/or
 *     modify it under the terms of the GNU Lesser General Public
 *     License as published by the Free Software Foundation; either
 *     version 2.1 of the License, or (at your option) any later version.
 *
 *     This library is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *     Lesser General Public License for more details.
 *
 *     You should have received a copy of the GNU Lesser General Public
 *     License along with this library; if not, write to the Free Software
 *     Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301
 *     USA
 */
package com.github.starnowski.posmulten.postgresql.core.context;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class SharedSchemaContextRequest implements Cloneable{

    /**
     * Name of default schema used during building process.
     */
    private String defaultSchema;
    /**
     * Name of the property that stores the current tenant identifier.
     */
    private String currentTenantIdProperty = "posmulten.tenant_id";
    /**
     * Type of column that stores the tenant identifier.
     * Default value is "VARCHAR(255)".
     */
    private String currentTenantIdPropertyType = "VARCHAR(255)";
    /**
     * Name of the function that returns the current tenant identifier.
     * @see com.github.starnowski.posmulten.postgresql.core.context.enrichers.GetCurrentTenantIdFunctionDefinitionEnricher
     */
    private String getCurrentTenantIdFunctionName;
    /**
     * Name of the function that set the current tenant identifier.
     * @see com.github.starnowski.posmulten.postgresql.core.context.enrichers.SetCurrentTenantIdFunctionDefinitionEnricher
     */
    private String setCurrentTenantIdFunctionName;
    /**
     * Name of the function that checks if passed identifier is equal to the current tenant identifier.
     * @see com.github.starnowski.posmulten.postgresql.core.context.enrichers.TenantHasAuthoritiesFunctionDefinitionEnricher
     */
    private String equalsCurrentTenantIdentifierFunctionName;
    /**
     * Name of the function that checks if the current tenant is allowed to process database table row.
     * @see com.github.starnowski.posmulten.postgresql.core.context.enrichers.TenantHasAuthoritiesFunctionDefinitionEnricher
     */
    private String tenantHasAuthoritiesFunctionName;
    /**
     * Name for column that stores the tenant identifier for table row.
     * @see com.github.starnowski.posmulten.postgresql.core.context.enrichers.TableRLSPolicyEnricher
     */
    private String defaultTenantIdColumn = "tenant_id";
    /**
     * A map that stores information about columns that are required to creation of shared schema multi-tenancy strategy.
     * Information about columns are store for each table that required to have row level security policy.
     * The table identifier ({@link TableKey}) is the map key and  information ({@link ITableColumns}) about columns are its value.
     * @see com.github.starnowski.posmulten.postgresql.core.context.enrichers.IsRecordBelongsToCurrentTenantFunctionDefinitionsEnricher
     * @see com.github.starnowski.posmulten.postgresql.core.context.enrichers.TableRLSPolicyEnricher
     * @see com.github.starnowski.posmulten.postgresql.core.context.enrichers.TenantColumnSQLDefinitionsEnricher
     */
    private Map<TableKey, ITableColumns> tableColumnsList = new HashMap<>();
    /**
     * Collection that stores table identifiers ({@link TableKey}) for which a column for tenant identifier should be added.
     * @see com.github.starnowski.posmulten.postgresql.core.context.enrichers.TenantColumnSQLDefinitionsEnricher
     */
    private Set<TableKey> createTenantColumnTableLists = new HashSet<>();
    /**
     * The toggle, based on which builder is going to <a href="https://www.postgresql.org/docs/9.6/ddl-rowsecurity.html">force row level security for table owner</a>
     * (true) or not (false). The default value is false.
     * @see com.github.starnowski.posmulten.postgresql.core.context.enrichers.TableRLSSettingsSQLDefinitionsEnricher
     */
    private boolean forceRowLevelSecurityForTableOwner;
    /**
     * A map that stores information that describes the row level security policy properties for tables.
     * Properties are store for each table that required to have row level security policy.
     * The table identifier ({@link TableKey}) is the map key and the row level security policy properties ({@link AbstractTableRLSPolicyProperties}) are its value.
     * @see com.github.starnowski.posmulten.postgresql.core.context.enrichers.TableRLSPolicyEnricher
     */
    private Map<TableKey, AbstractTableRLSPolicyProperties> tableRLSPolicies = new HashMap<>();
    /**
     * A map that stores information that are required to the creation of constraint that checks if foreign key in the main table refers to record
     * that exists in the foreign table and which belongs to the current tenant.
     * The map key is an object of type {@link SameTenantConstraintForForeignKey} that contains table keys ({@link TableKey})
     * for main table ({@link SameTenantConstraintForForeignKey#mainTable}) that has foreign key columns and the foreign
     * table ({@link SameTenantConstraintForForeignKey#foreignKeyTable}) which has primary key columns. The map key also
     * contains the set of column names for the foreign key ({@link SameTenantConstraintForForeignKey#foreignKeyColumns}).
     * The object of type {@link ISameTenantConstraintForForeignKeyProperties} is the map value.
     * @see com.github.starnowski.posmulten.postgresql.core.context.enrichers.IsRecordBelongsToCurrentTenantFunctionDefinitionsEnricher
     * @see com.github.starnowski.posmulten.postgresql.core.context.enrichers.IsRecordBelongsToCurrentTenantConstraintSQLDefinitionsEnricher
     */
    private Map<SameTenantConstraintForForeignKey, ISameTenantConstraintForForeignKeyProperties> sameTenantConstraintForForeignKeyProperties = new HashMap<>();
    /**
     * Default grantee for which the row level security should be added.
     * @see com.github.starnowski.posmulten.postgresql.core.context.enrichers.TableRLSPolicyEnricher
     */
    private String grantee;
    /**
     * A map that stores the names for a function that checks if there is a record with a specified identifier that is
     * assigned to the current tenant for the specified table. The map key is a table identifier ({@link TableKey}), and the
     * value is the function name.
     */
    private Map<TableKey, String> functionThatChecksIfRecordExistsInTableNames = new HashMap<>();

    public String getDefaultTenantIdColumn() {
        return defaultTenantIdColumn;
    }

    public void setDefaultTenantIdColumn(String defaultTenantIdColumn) {
        this.defaultTenantIdColumn = defaultTenantIdColumn;
    }

    public String getEqualsCurrentTenantIdentifierFunctionName() {
        return equalsCurrentTenantIdentifierFunctionName;
    }

    public void setEqualsCurrentTenantIdentifierFunctionName(String equalsCurrentTenantIdentifierFunctionName) {
        this.equalsCurrentTenantIdentifierFunctionName = equalsCurrentTenantIdentifierFunctionName;
    }

    public String getTenantHasAuthoritiesFunctionName() {
        return tenantHasAuthoritiesFunctionName;
    }

    public void setTenantHasAuthoritiesFunctionName(String tenantHasAuthoritiesFunctionName) {
        this.tenantHasAuthoritiesFunctionName = tenantHasAuthoritiesFunctionName;
    }

    public String getSetCurrentTenantIdFunctionName() {
        return setCurrentTenantIdFunctionName;
    }

    public void setSetCurrentTenantIdFunctionName(String setCurrentTenantIdFunctionName) {
        this.setCurrentTenantIdFunctionName = setCurrentTenantIdFunctionName;
    }

    public String getGetCurrentTenantIdFunctionName() {
        return getCurrentTenantIdFunctionName;
    }

    public void setGetCurrentTenantIdFunctionName(String getCurrentTenantIdFunctionName) {
        this.getCurrentTenantIdFunctionName = getCurrentTenantIdFunctionName;
    }

    public String getCurrentTenantIdPropertyType() {
        return currentTenantIdPropertyType;
    }

    public void setCurrentTenantIdPropertyType(String currentTenantIdPropertyType) {
        this.currentTenantIdPropertyType = currentTenantIdPropertyType;
    }

    public String getCurrentTenantIdProperty() {
        return currentTenantIdProperty;
    }

    public void setCurrentTenantIdProperty(String currentTenantIdProperty) {
        this.currentTenantIdProperty = currentTenantIdProperty;
    }

    public String getDefaultSchema() {
        return defaultSchema;
    }

    public void setDefaultSchema(String defaultSchema) {
        this.defaultSchema = defaultSchema;
    }

    public Map<TableKey, ITableColumns> getTableColumnsList() {
        return tableColumnsList;
    }

    public Set<TableKey> getCreateTenantColumnTableLists() {
        return createTenantColumnTableLists;
    }

    public boolean isForceRowLevelSecurityForTableOwner() {
        return forceRowLevelSecurityForTableOwner;
    }

    public void setForceRowLevelSecurityForTableOwner(boolean forceRowLevelSecurityForTableOwner) {
        this.forceRowLevelSecurityForTableOwner = forceRowLevelSecurityForTableOwner;
    }

    public String getGrantee() {
        return grantee;
    }

    public void setGrantee(String grantee) {
        this.grantee = grantee;
    }

    public Map<TableKey, AbstractTableRLSPolicyProperties> getTableRLSPolicies() {
        return tableRLSPolicies;
    }

    public Map<SameTenantConstraintForForeignKey, ISameTenantConstraintForForeignKeyProperties> getSameTenantConstraintForForeignKeyProperties() {
        return sameTenantConstraintForForeignKeyProperties;
    }

    public Map<TableKey, String> getFunctionThatChecksIfRecordExistsInTableNames() {
        return functionThatChecksIfRecordExistsInTableNames;
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        return super.clone();
    }
}
