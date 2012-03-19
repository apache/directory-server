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

    public final static String SCHEMA_NAME = "apache";

    // ---- ObjectClasses -----------------------------------------------------
    // ApacheCatalogEntry
    public final static String APACHE_CATALOG_ENTRY_OC = "apacheCatalogEntry";
    public final static String APACHE_CATALOG_ENTRY_OC_OID = "1.3.6.1.4.1.18060.0.4.1.3.5";

    // apacheFactoryConfiguration
    public final static String APACHE_FACTORY_CONFIGURATION_OC = "apacheFactoryConfiguration";
    public final static String APACHE_FACTORY_CONFIGURATION_OC_OID = "1.3.6.1.4.1.18060.0.4.1.3.4";

    // ApacheServiceConfiguration
    public final static String APACHE_SERVICE_CONFIGURATION_OC = "apacheServiceConfiguration";
    public final static String APACHE_SERVICE_CONFIGURATION_OC_OID = "1.3.6.1.4.1.18060.0.4.1.3.3";

    // ApacheSubschema
    public final static String APACHE_SUBSCHEMA_OC = "apacheSubschema";
    public final static String APACHE_SUBSCHEMA_OC_OID = "1.3.6.1.4.1.18060.0.4.1.3.9";

    // JavaClass
    public final static String JAVA_CLASS_OC = "javaClass";
    public final static String JAVA_CLASS_OC_OID = "1.3.6.1.4.1.18060.0.4.1.3.8";

    // JavaStoredProcUnit
    public final static String JAVA_STORED_PROC_UNIT_OC = "javaStoredProcUnit";
    public final static String JAVA_STORED_PROC_UNIT_OC_OID = "1.3.6.1.4.1.18060.0.4.1.5.5";

    // JavaxScriptStoredProcUnit
    public final static String JAVAX_SCRIPT_STORED_PROC_UNIT_OC = "javaxScriptStoredProcUnit";
    public final static String JAVAX_SCRIPT_STORED_PROC_UNIT_OC_OID = "1.3.6.1.4.1.18060.0.4.1.5.8";

    // PrefNode
    public final static String PREF_NODE_OC = "prefNode";
    public final static String PREF_NODE_OC_OID = "1.3.6.1.4.1.18060.0.4.1.3.1";

    // SchemaModificationAttributes
    public final static String SCHEMA_MODIFICATION_ATTRIBUTES_OC = "schemaModificationAttributes";
    public final static String SCHEMA_MODIFICATION_ATTRIBUTES_OC_OID = "1.3.6.1.4.1.18060.0.4.1.3.10";

    // StoredProcUnit
    public final static String STORED_PROC_UNIT_OC = "storedProcUnit";
    public final static String STORED_PROC_UNIT_OC_OID = "1.3.6.1.4.1.18060.0.4.1.5.3";

    // TriggerExecutionSubentry
    public final static String TRIGGER_EXECUTION_SUBENTRY_OC = "triggerExecutionSubentry";
    public final static String TRIGGER_EXECUTION_SUBENTRY_OC_OID = "1.3.6.1.4.1.18060.0.4.1.2.28";

    // UnixFile
    public final static String UNIX_FILE_OC = "unixFile";
    public final static String UNIX_FILE_OC_OID = "1.3.6.1.4.1.18060.0.4.1.3.7";

    // WindowsFile
    public final static String WINDOWS_FILE_OC = "windowsFile";
    public final static String WINDOWS_FILE_OC_OID = "1.3.6.1.4.1.18060.0.4.1.3.6";

    // ---- AttributeType ----------------------------------------------------------
    // ApachePresence
    public final static String APACHE_PRESENCE_AT = "ApachePresence";
    public final static String APACHE_PRESENCE_AT_OID = "1.3.6.1.4.1.18060.0.4.1.2.3";

    // ApacheOneLevel
    public final static String APACHE_ONE_LEVEL_AT = "apacheOneLevel";
    public final static String APACHE_ONE_LEVEL_AT_OID = "1.3.6.1.4.1.18060.0.4.1.2.4";

    // ApacheOneAlias
    public final static String APACHE_ONE_ALIAS_AT = "apacheOneAlias";
    public final static String APACHE_ONE_ALIAS_AT_OID = "1.3.6.1.4.1.18060.0.4.1.2.5";

    // ApacheSubAlias
    public final static String APACHE_SUB_ALIAS_AT = "apacheSubAlias";
    public final static String APACHE_SUB_ALIAS_AT_OID = "1.3.6.1.4.1.18060.0.4.1.2.6";

    // ApacheAlias
    public final static String APACHE_ALIAS_AT = "apacheAlias";
    public final static String APACHE_ALIAS_AT_OID = "1.3.6.1.4.1.18060.0.4.1.2.7";

    // PrefNodeName
    public final static String PREF_NODE_NAME_AT = "prefNodeName";
    public final static String PREF_NODE_NAME_AT_OID = "1.3.6.1.4.1.18060.0.4.1.2.8";

    // CatalogEntryName
    public final static String APACHE_CATALOGUE_ENTRY_NAME_AT = "apacheCatalogEntryName";
    public final static String APACHE_CATALOGUE_ENTRY_NAME_AT_OID = "1.3.6.1.4.1.18060.0.4.1.2.17";

    public final static String APACHE_CATALOGUE_ENTRY_BASE_DN_AT = "apacheCatalogEntryBaseDn";
    public final static String APACHE_CATALOGUE_ENTRY_BASE_DN_AT_OID = "1.3.6.1.4.1.18060.0.4.1.2.18";

    // WindowsFilePath
    public final static String WINDOWS_FILE_AT = "windowsFilePath";
    public final static String WINDOWS_FILE_AT_OID = "1.3.6.1.4.1.18060.0.4.1.2.19";

    // WindowsFilePath
    public final static String UNIX_FILE_AT = "unixFilePath";
    public final static String UNIX_FILE_AT_OID = "1.3.6.1.4.1.18060.0.4.1.2.20";

    // entryDeleted
    public final static String ENTRY_DELETED_AT = "entryDeleted";
    public final static String ENTRY_DELETED_AT_OID = "1.3.6.1.4.1.18060.0.4.1.2.31";

    // SchemaModifyTimestamp
    public final static String SCHEMA_MODIFY_TIMESTAMP_AT = "schemaModifyTimestamp";
    public final static String SCHEMA_MODIFY_TIMESTAMP_AT_OID = "1.3.6.1.4.1.18060.0.4.1.2.35";

    // SchemaModifiersName
    public final static String SCHEMA_MODIFIERS_NAME_AT = "schemaModifiersName";
    public final static String SCHEMA_MODIFIERS_NAME_AT_OID = "1.3.6.1.4.1.18060.0.4.1.2.36";

    // SubschemaSubentryName
    public final static String SUBSCHEMA_SUBENTRY_NAME_AT = "subschemaSubentryName";
    public final static String SUBSCHEMA_SUBENTRY_NAME_AT_OID = "1.3.6.1.4.1.18060.0.4.1.2.37";

    // apacheSubLevel
    public final static String APACHE_SUB_LEVEL_AT = "apacheSubLevel";
    public final static String APACHE_SUB_LEVEL_AT_OID = "1.3.6.1.4.1.18060.0.4.1.2.43";

    // apacheRdn
    public final static String APACHE_RDN_AT = "apacheRdn";
    public final static String APACHE_RDN_AT_OID = "1.3.6.1.4.1.18060.0.4.1.2.50";
}
