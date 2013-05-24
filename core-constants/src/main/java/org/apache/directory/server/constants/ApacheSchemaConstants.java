/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 * 
 *    http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 * 
 */
package org.apache.directory.server.constants;


/**
 * Constants from the Apache schema.
 * Final reference -> class shouldn't be extended
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public final class ApacheSchemaConstants
{

    /**
     *  Ensures no construction of this class, also ensures there is no need for final keyword above
     *  (Implicit super constructor is not visible for default constructor),
     *  but is still self documenting.
     */
    private ApacheSchemaConstants()
    {
    }

    public static final String SCHEMA_NAME = "apache";

    // ---- ObjectClasses -----------------------------------------------------
    // ApacheCatalogEntry
    public static final String APACHE_CATALOG_ENTRY_OC = "apacheCatalogEntry";
    public static final String APACHE_CATALOG_ENTRY_OC_OID = "1.3.6.1.4.1.18060.0.4.1.3.5";

    // apacheFactoryConfiguration
    public static final String APACHE_FACTORY_CONFIGURATION_OC = "apacheFactoryConfiguration";
    public static final String APACHE_FACTORY_CONFIGURATION_OC_OID = "1.3.6.1.4.1.18060.0.4.1.3.4";

    // ApacheServiceConfiguration
    public static final String APACHE_SERVICE_CONFIGURATION_OC = "apacheServiceConfiguration";
    public static final String APACHE_SERVICE_CONFIGURATION_OC_OID = "1.3.6.1.4.1.18060.0.4.1.3.3";

    // ApacheSubschema
    public static final String APACHE_SUBSCHEMA_OC = "apacheSubschema";
    public static final String APACHE_SUBSCHEMA_OC_OID = "1.3.6.1.4.1.18060.0.4.1.3.9";

    // JavaClass
    public static final String JAVA_CLASS_OC = "javaClass";
    public static final String JAVA_CLASS_OC_OID = "1.3.6.1.4.1.18060.0.4.1.3.8";

    // JavaStoredProcUnit
    public static final String JAVA_STORED_PROC_UNIT_OC = "javaStoredProcUnit";
    public static final String JAVA_STORED_PROC_UNIT_OC_OID = "1.3.6.1.4.1.18060.0.4.1.5.5";

    // JavaxScriptStoredProcUnit
    public static final String JAVAX_SCRIPT_STORED_PROC_UNIT_OC = "javaxScriptStoredProcUnit";
    public static final String JAVAX_SCRIPT_STORED_PROC_UNIT_OC_OID = "1.3.6.1.4.1.18060.0.4.1.5.8";

    // PrefNode
    public static final String PREF_NODE_OC = "prefNode";
    public static final String PREF_NODE_OC_OID = "1.3.6.1.4.1.18060.0.4.1.3.1";

    // SchemaModificationAttributes
    public static final String SCHEMA_MODIFICATION_ATTRIBUTES_OC = "schemaModificationAttributes";
    public static final String SCHEMA_MODIFICATION_ATTRIBUTES_OC_OID = "1.3.6.1.4.1.18060.0.4.1.3.10";

    // StoredProcUnit
    public static final String STORED_PROC_UNIT_OC = "storedProcUnit";
    public static final String STORED_PROC_UNIT_OC_OID = "1.3.6.1.4.1.18060.0.4.1.5.3";

    // TriggerExecutionSubentry
    public static final String TRIGGER_EXECUTION_SUBENTRY_OC = "triggerExecutionSubentry";
    public static final String TRIGGER_EXECUTION_SUBENTRY_OC_OID = "1.3.6.1.4.1.18060.0.4.1.2.28";

    // UnixFile
    public static final String UNIX_FILE_OC = "unixFile";
    public static final String UNIX_FILE_OC_OID = "1.3.6.1.4.1.18060.0.4.1.3.7";

    // WindowsFile
    public static final String WINDOWS_FILE_OC = "windowsFile";
    public static final String WINDOWS_FILE_OC_OID = "1.3.6.1.4.1.18060.0.4.1.3.6";

    // ---- AttributeType ----------------------------------------------------------
    // ApachePresence
    public static final String APACHE_PRESENCE_AT = "ApachePresence";
    public static final String APACHE_PRESENCE_AT_OID = "1.3.6.1.4.1.18060.0.4.1.2.3";

    // ApacheOneAlias
    public static final String APACHE_ONE_ALIAS_AT = "apacheOneAlias";
    public static final String APACHE_ONE_ALIAS_AT_OID = "1.3.6.1.4.1.18060.0.4.1.2.5";

    // ApacheSubAlias
    public static final String APACHE_SUB_ALIAS_AT = "apacheSubAlias";
    public static final String APACHE_SUB_ALIAS_AT_OID = "1.3.6.1.4.1.18060.0.4.1.2.6";

    // ApacheAlias
    public static final String APACHE_ALIAS_AT = "apacheAlias";
    public static final String APACHE_ALIAS_AT_OID = "1.3.6.1.4.1.18060.0.4.1.2.7";

    // PrefNodeName
    public static final String PREF_NODE_NAME_AT = "prefNodeName";
    public static final String PREF_NODE_NAME_AT_OID = "1.3.6.1.4.1.18060.0.4.1.2.8";

    // CatalogEntryName
    public static final String APACHE_CATALOGUE_ENTRY_NAME_AT = "apacheCatalogEntryName";
    public static final String APACHE_CATALOGUE_ENTRY_NAME_AT_OID = "1.3.6.1.4.1.18060.0.4.1.2.17";

    public static final String APACHE_CATALOGUE_ENTRY_BASE_DN_AT = "apacheCatalogEntryBaseDn";
    public static final String APACHE_CATALOGUE_ENTRY_BASE_DN_AT_OID = "1.3.6.1.4.1.18060.0.4.1.2.18";

    // WindowsFilePath
    public static final String WINDOWS_FILE_AT = "windowsFilePath";
    public static final String WINDOWS_FILE_AT_OID = "1.3.6.1.4.1.18060.0.4.1.2.19";

    // WindowsFilePath
    public static final String UNIX_FILE_AT = "unixFilePath";
    public static final String UNIX_FILE_AT_OID = "1.3.6.1.4.1.18060.0.4.1.2.20";

    // entryDeleted
    public static final String ENTRY_DELETED_AT = "entryDeleted";
    public static final String ENTRY_DELETED_AT_OID = "1.3.6.1.4.1.18060.0.4.1.2.31";

    // SchemaModifyTimestamp
    public static final String SCHEMA_MODIFY_TIMESTAMP_AT = "schemaModifyTimestamp";
    public static final String SCHEMA_MODIFY_TIMESTAMP_AT_OID = "1.3.6.1.4.1.18060.0.4.1.2.35";

    // SchemaModifiersName
    public static final String SCHEMA_MODIFIERS_NAME_AT = "schemaModifiersName";
    public static final String SCHEMA_MODIFIERS_NAME_AT_OID = "1.3.6.1.4.1.18060.0.4.1.2.36";

    // SubschemaSubentryName
    public static final String SUBSCHEMA_SUBENTRY_NAME_AT = "subschemaSubentryName";
    public static final String SUBSCHEMA_SUBENTRY_NAME_AT_OID = "1.3.6.1.4.1.18060.0.4.1.2.37";

    // apacheRdn
    public static final String APACHE_RDN_AT = "apacheRdn";
    public static final String APACHE_RDN_AT_OID = "1.3.6.1.4.1.18060.0.4.1.2.50";
}
