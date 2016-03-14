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
 * Final reference -&gt; class shouldn't be extended
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
    
    // ApacheOneLevel
    public static final String APACHE_ONE_LEVEL_AT = "apacheOneLevel";
    public static final String APACHE_ONE_LEVEL_AT_OID = "1.3.6.1.4.1.18060.0.4.1.2.4";

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

    // ApacheSamType
    public static final String APACHE_SAM_TYPE_AT = "apacheSamType";
    public static final String APACHE_SAM_TYPE_AT_OID = "1.3.6.1.4.1.18060.0.4.1.2.9";

    // AutonomousAreaSubentry
    public static final String AUTONOMOUS_AREA_SUBENTRY_AT = "autonomousAreaSubentry";
    public static final String AUTONOMOUS_AREA_SUBENTRY_AT_OID = "1.3.6.1.4.1.18060.0.4.1.2.10";

    // AccessControlSubentries
    public static final String ACCESS_CONTROL_SUBENTRIES_AT = "accessControlSubentries";
    public static final String ACCESS_CONTROL_SUBENTRIES_AT_OID = "1.3.6.1.4.1.18060.0.4.1.2.11";

    // NbChildren
    public static final String NB_CHILDREN_AT = "nbChildren";
    public static final String NB_CHILDREN_OID = "1.3.6.1.4.1.18060.0.4.1.2.12";

    // NbSubordinates
    public static final String NB_SUBORDINATES_AT = "nbSubordinates";
    public static final String NB_SUBORDINATES_OID = "1.3.6.1.4.1.18060.0.4.1.2.13";

    // ApacheServicePid
    public static final String APACHE_SERVICE_PID_AT = "apacheServicePid";
    public static final String APACHE_SERVICE_PID_OID = "1.3.6.1.4.1.18060.0.4.1.2.15";

    // ApacheServiceFactoryPid
    public static final String APACHE_SERVICE_FACTORYPID_AT = "apacheServiceFactoryPid";
    public static final String APACHE_SERVICE_FACTORYPID_OID = "1.3.6.1.4.1.18060.0.4.1.2.16";

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

    // FullyQualifiedJavaClassName
    public static final String FULLY_QUALIFIED_JAVA_CLASS_NAME_AT = "fullyQualifiedJavaClassName";
    public static final String FULLY_QUALIFIED_JAVA_CLASS_NAME_OID = "1.3.6.1.4.1.18060.0.4.1.2.21";

    // JavaClassByteCode
    public static final String JAVA_CLASS_BYTE_CODE_AT = "javaClassByteCode";
    public static final String JAVA_CLASS_BYTE_CODE_OID = "1.3.6.1.4.1.18060.0.4.1.2.22";

    // ClassLoaderDefaultSearchContext
    public static final String CLASS_LOADER_DEFAULT_SEARCH_CONTEXT_AT = "classLoaderDefaultSearchContext";
    public static final String CLASS_LOADER_DEFAULT_SEARCH_CONTEXT_OID = "1.3.6.1.4.1.18060.0.4.1.2.23";

    // PrescriptiveTriggerSpecification
    public static final String PRESCRIPTIVE_TRIGGER_SPECIFICATION_AT = "prescriptiveTriggerSpecification";
    public static final String PRESCRIPTIVE_TRIGGER_SPECIFICATION_OID = "1.3.6.1.4.1.18060.0.4.1.2.25";

    // EntryTriggerSpecification
    public static final String ENTRY_TRIGGER_SPECIFICATION_AT = "entryTriggerSpecification";
    public static final String ENTRY_TRIGGER_SPECIFICATION_OID = "1.3.6.1.4.1.18060.0.4.1.2.26";
    
    // TriggerExecutionSubentries
    public static final String TRIGGER_EXECUTION_SUBENTRIES_AT = "triggerExecutionSubentries";
    public static final String TRIGGER_EXECUTION_SUBENTRIES_AT_OID = "1.3.6.1.4.1.18060.0.4.1.2.27";

    // entryDeleted
    public static final String ENTRY_DELETED_AT = "entryDeleted";
    public static final String ENTRY_DELETED_AT_OID = "1.3.6.1.4.1.18060.0.4.1.2.31";

    // Comparators
    public static final String COMPARATORS_AT = "comparators";
    public static final String COMPARATORS_AT_OID = "1.3.6.1.4.1.18060.0.4.1.2.32";

    // Normalizers
    public static final String NORMALIZERS_AT = "normalizers";
    public static final String NORMALIZERS_AT_OID = "1.3.6.1.4.1.18060.0.4.1.2.33";

    // SyntaxCheckers
    public static final String SYNTAX_CHECKERS_AT = "syntaxCheckers";
    public static final String SYNTAX_CHECKERS_AT_OID = "1.3.6.1.4.1.18060.0.4.1.2.34";

    // SchemaModifyTimestamp
    public static final String SCHEMA_MODIFY_TIMESTAMP_AT = "schemaModifyTimestamp";
    public static final String SCHEMA_MODIFY_TIMESTAMP_AT_OID = "1.3.6.1.4.1.18060.0.4.1.2.35";

    // SchemaModifiersName
    public static final String SCHEMA_MODIFIERS_NAME_AT = "schemaModifiersName";
    public static final String SCHEMA_MODIFIERS_NAME_AT_OID = "1.3.6.1.4.1.18060.0.4.1.2.36";

    // SubschemaSubentryName
    public static final String SUBSCHEMA_SUBENTRY_NAME_AT = "subschemaSubentryName";
    public static final String SUBSCHEMA_SUBENTRY_NAME_AT_OID = "1.3.6.1.4.1.18060.0.4.1.2.37";

    // PrivateKeyFormat
    public static final String PRIVATE_KEY_FORMAT_AT = "privateKeyFormat";
    public static final String PRIVATE_KEY_FORMAT_OID = "1.3.6.1.4.1.18060.0.4.1.2.38";

    // KeyAlgorithm
    public static final String KEY_ALGORITHM_AT = "keyAlgorithm";
    public static final String KEY_ALGORITHM_OID = "1.3.6.1.4.1.18060.0.4.1.2.39";

    // PrivateKey
    public static final String PRIVATE_KEY_AT = "privateKey";
    public static final String PRIVATE_KEY_OID = "1.3.6.1.4.1.18060.0.4.1.2.40";

    // PublicKeyFormat
    public static final String PUBLIC_KEY_FORMAT_AT = "publicKeyFormat";
    public static final String PUBLIC_KEY_FORMAT_OID = "1.3.6.1.4.1.18060.0.4.1.2.41";

    // PublicKey
    public static final String PUBLIC_KEY_AT = "publicKey";
    public static final String PUBLIC_KEY_OID = "1.3.6.1.4.1.18060.0.4.1.2.42";

    // ApacheSubLevel
    public static final String APACHE_SUB_LEVEL_AT = "apacheSubLevel";
    public static final String APACHE_SUB_LEVEL_OID = "1.3.6.1.4.1.18060.0.4.1.2.43";

    // Revisions
    public static final String REVISIONS_AT = "revisions";
    public static final String REVISIONS_OID = "1.3.6.1.4.1.18060.0.4.1.2.44";

    // ChangeTime
    public static final String CHANGE_TIME_AT = "changeTime";
    public static final String CHANGE_TIME_OID = "1.3.6.1.4.1.18060.0.4.1.2.45";

    // ChangeType
    public static final String CHANGE_TYPE_AT = "changeType";
    public static final String CHANGE_TYPE_OID = "1.3.6.1.4.1.18060.0.4.1.2.46";

    // EventId
    public static final String EVENT_ID_AT = "eventId";
    public static final String EVENT_ID_OID = "1.3.6.1.4.1.18060.0.4.1.2.47";

    // Committer
    public static final String COMMITTER_AT = "committer";
    public static final String COMMITTER_OID = "1.3.6.1.4.1.18060.0.4.1.2.48";

    // ChangeLogContext
    public static final String CHANGELOG_CONTEXT_AT = "changeLogContext";
    public static final String CHANGELOG_CONTEXT_AT_OID = "1.3.6.1.4.1.18060.0.4.1.2.49";

    // apacheRdn
    public static final String APACHE_RDN_AT = "apacheRdn";
    public static final String APACHE_RDN_AT_OID = "1.3.6.1.4.1.18060.0.4.1.2.50";

    // entryParentId
    public static final String ENTRY_PARENT_ID_AT = "entryParentId";
    public static final String ENTRY_PARENT_ID_OID = "1.3.6.1.4.1.18060.0.4.1.2.51";
}
