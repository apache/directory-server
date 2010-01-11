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
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public interface ApacheSchemaConstants
{
    String SCHEMA_NAME = "apache";

    // ---- ObjectClasses -----------------------------------------------------
    // ApacheCatalogEntry
    String APACHE_CATALOG_ENTRY_OC                  = "apacheCatalogEntry";
    String APACHE_CATALOG_ENTRY_OC_OID              = "1.3.6.1.4.1.18060.0.4.1.3.5";

    // apacheFactoryConfiguration
    String APACHE_FACTORY_CONFIGURATION_OC          = "apacheFactoryConfiguration";
    String APACHE_FACTORY_CONFIGURATION_OC_OID      = "1.3.6.1.4.1.18060.0.4.1.3.4";

    // ApacheServiceConfiguration
    String APACHE_SERVICE_CONFIGURATION_OC          = "apacheServiceConfiguration";
    String APACHE_SERVICE_CONFIGURATION_OC_OID      = "1.3.6.1.4.1.18060.0.4.1.3.3";

    // ApacheSubschema
    String APACHE_SUBSCHEMA_OC                      = "apacheSubschema";
    String APACHE_SUBSCHEMA_OC_OID                  = "1.3.6.1.4.1.18060.0.4.1.3.9";

    // JavaClass
    String JAVA_CLASS_OC                            = "javaClass";
    String JAVA_CLASS_OC_OID                        = "1.3.6.1.4.1.18060.0.4.1.3.8";

    // JavaStoredProcUnit
    String JAVA_STORED_PROC_UNIT_OC                 = "javaStoredProcUnit";
    String JAVA_STORED_PROC_UNIT_OC_OID             = "1.3.6.1.4.1.18060.0.4.1.5.5";

    // JavaxScriptStoredProcUnit
    String JAVAX_SCRIPT_STORED_PROC_UNIT_OC         = "javaxScriptStoredProcUnit";
    String JAVAX_SCRIPT_STORED_PROC_UNIT_OC_OID     = "1.3.6.1.4.1.18060.0.4.1.5.8";

    // PrefNode
    String PREF_NODE_OC                             = "prefNode";
    String PREF_NODE_OC_OID                         = "1.3.6.1.4.1.18060.0.4.1.3.1";

    // SchemaModificationAttributes
    String SCHEMA_MODIFICATION_ATTRIBUTES_OC        = "schemaModificationAttributes";
    String SCHEMA_MODIFICATION_ATTRIBUTES_OC_OID    = "1.3.6.1.4.1.18060.0.4.1.3.10";

    // StoredProcUnit
    String STORED_PROC_UNIT_OC                      = "storedProcUnit";
    String STORED_PROC_UNIT_OC_OID                  = "1.3.6.1.4.1.18060.0.4.1.5.3";

    // TriggerExecutionSubentry
    String TRIGGER_EXECUTION_SUBENTRY_OC            = "triggerExecutionSubentry";
    String TRIGGER_EXECUTION_SUBENTRY_OC_OID        = "1.3.6.1.4.1.18060.0.4.1.2.28";

    // UnixFile
    String UNIX_FILE_OC                             = "unixFile";
    String UNIX_FILE_OC_OID                         = "1.3.6.1.4.1.18060.0.4.1.3.7";

    // WindowsFile
    String WINDOWS_FILE_OC                          = "windowsFile";
    String WINDOWS_FILE_OC_OID                      = "1.3.6.1.4.1.18060.0.4.1.3.6";

    // ---- AttributeType ----------------------------------------------------------
    // ApacheNdn
    String APACHE_N_DN_AT                           = "apacheNdn";
    String APACHE_N_DN_AT_OID                       = "1.3.6.1.4.1.18060.0.4.1.2.1";
    
    // ApacheUpdn
    String APACHE_UP_DN_AT                          = "apacheUpdn";
    String APACHE_UP_DN_AT_OID                      = "1.3.6.1.4.1.18060.0.4.1.2.2";
    
    // ApacheExistence
    String APACHE_EXISTENCE_AT                      = "apacheExistence";
    String APACHE_EXISTENCE_AT_OID                  = "1.3.6.1.4.1.18060.0.4.1.2.3";
    
    // ApacheOneLevel
    String APACHE_ONE_LEVEL_AT                      = "apacheOneLevel";
    String APACHE_ONE_LEVEL_AT_OID                  = "1.3.6.1.4.1.18060.0.4.1.2.4";
    
    // ApacheOneAlias
    String APACHE_ONE_ALIAS_AT                      = "apacheOneAlias";
    String APACHE_ONE_ALIAS_AT_OID                  = "1.3.6.1.4.1.18060.0.4.1.2.5";
    
    // ApacheSubAlias
    String APACHE_SUB_ALIAS_AT                      = "apacheSubAlias";
    String APACHE_SUB_ALIAS_AT_OID                  = "1.3.6.1.4.1.18060.0.4.1.2.6";

    // ApacheAlias
    String APACHE_ALIAS_AT                          = "apacheAlias";
    String APACHE_ALIAS_AT_OID                      = "1.3.6.1.4.1.18060.0.4.1.2.7";

    // PrefNodeName
    String PREF_NODE_NAME_AT                        = "prefNodeName";
    String PREF_NODE_NAME_AT_OID                    = "1.3.6.1.4.1.18060.0.4.1.2.8";
    
    // CatalogEntryName
    String APACHE_CATALOGUE_ENTRY_NAME_AT           = "apacheCatalogEntryName";
    String APACHE_CATALOGUE_ENTRY_NAME_AT_OID       = "1.3.6.1.4.1.18060.0.4.1.2.17";
    
    String APACHE_CATALOGUE_ENTRY_BASE_DN_AT        = "apacheCatalogEntryBaseDn";
    String APACHE_CATALOGUE_ENTRY_BASE_DN_AT_OID    = "1.3.6.1.4.1.18060.0.4.1.2.18";

    // WindowsFilePath
    String WINDOWS_FILE_AT                          = "windowsFilePath";
    String WINDOWS_FILE_AT_OID                      = "1.3.6.1.4.1.18060.0.4.1.2.19";

    // WindowsFilePath
    String UNIX_FILE_AT                             = "unixFilePath";
    String UNIX_FILE_AT_OID                         = "1.3.6.1.4.1.18060.0.4.1.2.20";

    // entryDeleted
    String ENTRY_DELETED_AT                         = "entryDeleted";
    String ENTRY_DELETED_AT_OID                     = "1.3.6.1.4.1.18060.0.4.1.2.31";
    
    // SchemaModifyTimestamp
    String SCHEMA_MODIFY_TIMESTAMP_AT               = "schemaModifyTimestamp";
    String SCHEMA_MODIFY_TIMESTAMP_AT_OID           = "1.3.6.1.4.1.18060.0.4.1.2.35";

    // SchemaModifiersName
    String SCHEMA_MODIFIERS_NAME_AT                 = "schemaModifiersName";
    String SCHEMA_MODIFIERS_NAME_AT_OID             = "1.3.6.1.4.1.18060.0.4.1.2.36";
    
    // SubschemaSubentryName
    String SUBSCHEMA_SUBENTRY_NAME_AT               = "subschemaSubentryName";
    String SUBSCHEMA_SUBENTRY_NAME_AT_OID           = "1.3.6.1.4.1.18060.0.4.1.2.37";
    
    // apacheSubLevel
    String APACHE_SUB_LEVEL_AT                      = "apacheSubLevel";
    String APACHE_SUB_LEVEL_AT_OID                  = "1.3.6.1.4.1.18060.0.4.1.2.43";
}
