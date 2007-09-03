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


    
    
    
    // SchemaModifiersName
    String SCHEMA_MODIFIERS_NAME_AT         = "schemaModifiersName";
    String SCHEMA_MODIFIERS_NAME_AT_OID     = "";
    
    // SchemaModifyTimestamp
    String SCHEMA_MODIFY_TIMESTAMP_AT = "schemaModifyTimestamp";
    String SCHEMA_MODIFY_TIMESTAMP_AT_OID = "";
    
    // SubschemaSubentryName
    String SUBSCHEMA_SUBENTRY_NAME_AT = "subschemaSubentryName";
    String SUBSCHEMA_SUBENTRY_NAME_AT_OID = "";
}
