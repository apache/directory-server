/*
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 *
 */

package org.apache.directory.server.config;


/**
 * Constants defined for the elements of config schema
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public enum ConfigSchemaConstants
{
    //------------------------------------------------------------------------------------
    // The ObjectClasses
    //------------------------------------------------------------------------------------
    // ads-directoryService objectClass
    ADS_DIRECTORY_SERVICE_OC( "ads-directoryService", "1.3.6.1.4.1.18060.0.4.1.3.100" ),

    ADS_CONFIG_ID( "ads-configId", "" ),
    
    ADS_COMPOSITE_ELEMENT_AT( "ads-compositeElement", "1.3.6.1.4.1.18060.0.4.1.2.0" ),

    ADS_LDAP_SERVER_OC( "ads-ldapServer", "" ),

    ADS_KERBEROS_SERVER_OC( "ads-kdcServer", "1.3.6.1.4.1.18060.0.4.1.3.400" ),

    ADS_SERVER_ID( "ads-serverId", "" ),

    ADS_TRANSPORTS( "ads-transports", "" ),

    ADS_KRB_ALLOWABLE_CLOCKSKEW( "ads-krbAllowableClockSkew", "" ),

    ADS_KRB_ENCRYPTION_TYPES( "ads-krbEncryptionTypes", "" ),

    ADS_KRB_EMPTY_ADDRESSES_ALLOWED( "ads-krbEmptyAddressesAllowed", "" ),

    ADS_KRB_FORWARDABLE_ALLOWED( "ads-krbForwardableAllowed", "" ),

    ADS_KRB_PAENC_TIMESTAMP_REQUIRED( "ads-krbPaEncTimestampRequired", "" ),

    ADS_KRB_POSTDATED_ALLOWED( "ads-krbPostdatedAllowed", "" ),

    ADS_KRB_PROXIABLE_ALLOWED( "ads-krbProxiableAllowed", "" ),

    ADS_KRB_RENEWABLE_ALLOWED( "ads-krbRenewableAllowed", "" ),

    ADS_KRB_KDC_PRINCIPAL( "ads-krbKdcPrincipal", "" ),

    ADS_KRB_MAXIMUM_RENEWABLE_LIFETIME( "ads-krbMaximumRenewableLifetime", "" ),

    ADS_KRB_MAXIMUM_TICKET_LIFETIME( "ads-krbMaximumTicketLifetime", "" ),

    ADS_KRB_PRIMARY_REALM( "ads-krbPrimaryRealm", "" ),

    ADS_KRB_BODY_CHECKSUM_VERIFIED( "ads-krbBodyChecksumVerified", "" ),

    ADS_DNS_SERVER_OC( "ads-dnsServer", "" ),

    ADS_DHCP_SERVER_OC( "ads-dhcpServer", "" ),

    ADS_NTP_SERVER_OC( "ads-ntpServer", "" ),

    ADS_HTTP_SERVER_OC( "ads-httpServer", "" ),

    ADS_HTTP_CONFFILE( "ads-httpConfFile", "" ),

    ADS_DIRECTORYSERVICE_ID( "ads-directoryServiceId", "" ),

    ADS_DS_REPLICA_ID( "ads-dsReplicaId", "" ),

    ADS_DSINTERCEPTORS( "ads-dsInterceptors", "" ),

    ADS_DSPARTITIONS( "ads-dsPartitions", "" ),

    ADS_DS_ACCESSCONTROL_ENABLED( "ads-dsAccessControlEnabled", "" ),

    ADS_DS_ALLOW_ANONYMOUS_ACCESS( "ads-dsAllowAnonymousAccess", "" ),

    ADS_DSCHANGELOG( "ads-dsChangeLog", "" ),

    ADS_DS_DENORMALIZE_OPATTRS_ENABLED( "ads-dsDenormalizeOpAttrsEnabled", "" ),

    ADS_DSJOURNAL( "ads-dsJournal", "" ),

    ADS_DS_MAXPDU_SIZE( "ads-dsMaxPDUSize", "" ),

    ADS_DS_PASSWORD_HIDDEN( "ads-dsPasswordHidden", "" ),

    //ADS_DS_REPLICATION( "ads-dsReplication", "" ),

    ADS_DS_SYNCPERIOD_MILLIS( "ads-dsSyncPeriodMillis", "" ),

    ADS_DS_TEST_ENTRIES( "ads-dsTestEntries", "" ),

    ADS_INTERCEPTOR_ID( "ads-interceptorId", "" ),

    ADS_INTERCEPTOR_CLASSNAME( "ads-interceptorClassName", "" ),

    ADS_INTERCEPTOR_ORDER( "ads-interceptorOrder", "" ),

    ADS_JDBMPARTITION( "ads-jdbmPartition", "" ),

    ADS_PARTITION_ID( "ads-partitionId", "" ),

    ADS_PARTITION_SUFFIX( "ads-partitionSuffix", "" ),

    ADS_PARTITION_CACHE_SIZE( "ads-partitionCacheSize", "" ),

    ADS_JDBM_PARTITION_OPTIMIZER_ENABLED( "ads-jdbmPartitionOptimizerEnabled", "" ),

    ADS_PARTITION_SYNCONWRITE( "ads-partitionSyncOnWrite", "" ),

    ADS_PARTITION_INDEXED_ATTRIBUTES( "ads-partitionIndexedAttributes", "" ),

    ADS_INDEX_ATTRIBUTE_ID( "ads-indexAttributeId", "" ),

    ADS_JDBMINDEX( "ads-jdbmIndex", "" ),

    ADS_INDEX_CACHESIZE( "ads-indexCacheSize", "" ),
    
    ADS_INDEX_NUM_DUP_LIMIT( "ads-indexNumDupLimit", "" ),

    ADS_TRANSPORT_ID( "ads-transportId", "" ),

    ADS_TCP_TRANSPORT( "ads-tcpTransport", "" ),

    ADS_UDP_TRANSPORT( "ads-udpTransport", "" ),

    ADS_SYSTEM_PORT( "ads-systemPort", "" ),

    ADS_TRANSPORT_ADDRESS( "ads-transportAddress", "" ),

    ADS_TRANSPORT_BACKLOG( "ads-transportBacklog", "" ),

    ADS_TRANSPORT_ENABLE_SSL( "ads-transportEnableSSL", "" ),

    ADS_TRANSPORT_NBTHREADS( "ads-transportNbThreads", "" ),

    ADS_CHANGELOG_ENABLED( "ads-changeLogEnabled", "" ),

    ADS_CHANGELOG_EXPOSED( "ads-changeLogExposed", "" ),

    ADS_JOURNAL_FILENAME( "ads-journalFileName", "" ),

    ADS_JOURNAL_WORKINGDIR( "ads-journalWorkingDir", "" ),

    ADS_JOURNAL_ROTATION( "ads-journalRotation", "" ),

    ADS_JOURNAL_ENABLED( "ads-journalEnabled", "" ),

    ADS_HTTP_WARFILE( "ads-httpWarFile", "" ),

    ADS_HTTP_APP_CTX_PATH( "ads-httpAppCtxPath", "" ),

    ADS_ENABLED( "ads-enabled", "" ),
    
    ADS_CHANGEPWD_POLICY_CATEGORY_COUNT( "ads-chgPwdPolicyCategoryCount", "" ),
    
    ADS_CHANGEPWD_POLICY_PASSWORD_LENGTH( "ads-chgPwdPolicyPasswordLength", "" ),
    
    ADS_CHANGEPWD_POLICY_TOKEN_SIZE( "ads-chgPwdPolicyTokenSize", "" ),
    
    ADS_CHANGEPWD_SERVICE_PRINCIPAL( "ads-chgPwdServicePrincipal", "" ),
    
    ADS_CHANGEPWD_SERVER_OC( "ads-changePasswordServer", "" ),
    
    ADS_REPL_SEARCH_FILTER( "ads-replSearchFilter", "" ),
    
    ADS_REPL_LAST_SENT_CSN( "ads-replLastSentCsn", "" ),
    
    ADS_REPL_ALIAS_DEREF_MODE( "ads-replAliasDerefMode", "" ),
    
    ADS_SEARCH_BASE( "ads-searchBaseDN", "" ),
    
    ADS_REPL_SEARCH_SCOPE( "ads-replSearchScope", "" ),
    
    ADS_REPL_REFRESH_N_PERSIST( "ads-replRefreshNPersist", "" ),
    
    ADS_REPL_PROV_HOST_NAME( "ads-replProvHostName", "" ),
    
    ADS_REPL_PROV_PORT( "ads-replProvPort", "" ),
    
    ADS_REPL_USER_DN( "ads-replUserDn", "" ),
    
    ADS_REPL_USER_PASSWORD( "ads-replUserPassword", "" ),
    
    ADS_REPL_REFRESH_INTERVAL( "ads-replRefreshInterval", "" ),
    
    ADS_REPL_ATTRIBUTE( "ads-replAttribute", "" ),
    
    ADS_REPL_SEARCH_SIZE_LIMIT( "ads-replSearchSizeLimit", "" ),
    
    ADS_REPL_SEARCH_TIMEOUT( "ads-replSearchTimeOut", "" ),
    
    ADS_REPL_COOKIE( "ads-replCookie", "" ),
    
    ADS_REPL_CONSUMER_OC( "ads-replConsumer", "" ),
    
    ADS_REPL_PROVIDER_OC( "ads-replProvider", "" ),
    
    ADS_REPL_PROVIDER_IMPL( "ads-replProviderImpl", "" ),
    
    ADS_REPL_ENABLE_PROVIDER( "ads-enableReplProvider", "" ),
    
    ADS_REPL_PEER_CERTIFICATE( "ads-replPeerCertificate", "" ),
    
    ADS_REPL_USE_TLS( "ads-replUseTls", "" ),
    
    ADS_LDAP_SERVER_SASL_MECH_HANDLER_OC( "ads-ldapServerSaslMechanismHandler", "" ),
    
    ADS_LDAP_SERVER_SASL_MECH_NAME( "ads-ldapServerSaslMechName", "" ),
    
    ADS_LDAP_SERVER_SASL_MECH_CLASS_NAME( "ads-ldapServerSaslMechClassName", "" ),
    
    ADS_LDAP_SERVER_NTLM_MECH_PROVIDER( "ads-ldapServerNtlmMechProvider", "" ),
    
    ADS_LDAP_SERVER_EXT_OP_HANDLER_OC( "ads-ldapServerExtendedOpHandler", "" ),
    
    ADS_LDAP_SERVER_EXT_OP_HANDLER_FQCN( "ads-ldapServerExtendedOpHandlerClass", "" ),
    
    ADS_LDAP_SERVER_KEYSTORE_FILE( "ads-ldapserverkeystorefile", "" ),
    
    ADS_LDAP_SERVER_CERT_PASSWORD( "ads-ldapServerCertificatePassword", "" );
    
    /** The interned value */
    private String value;
    
    /** The associated OID */
    private String oid;
    
    /** A private constructor */
    private ConfigSchemaConstants( String value, String oid )
    {
        this.value = value;
        this.oid = oid;
    }
    
    
    /**
     * @return The interned String
     */
    public String getValue()
    {
        return value;
    }
    
    
    /**
     * @return The associated OID
     */
    public String getOid()
    {
        return oid;
    }
}
