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
public interface ConfigSchemaConstants
{
    String ADS_LDAP_SERVER_OC = "ads-ldapServer";

    String ADS_KERBEROS_SERVER_OC = "ads-kerberosServer";

    String ADS_SERVER_ID = "ads-serverId";

    String ADS_TRANSPORTS = "ads-transports";

    String ADS_KRB_ALLOWABLE_CLOCKSKEW = "ads-krbAllowableClockSkew";

    String ADS_KRB_ENCRYPTION_TYPES = "ads-krbEncryptionTypes";

    String ADS_KRB_EMPTY_ADDRESSES_ALLOWED = "ads-krbEmptyAddressesAllowed";

    String ADS_KRB_FORWARDABLE_ALLOWED = "ads-krbForwardableAllowed";

    String ADS_KRB_PAENC_TIMESTAMP_REQUIRED = "ads-krbPaEncTimestampRequired";

    String ADS_KRB_POSTDATED_ALLOWED = "ads-krbPostdatedAllowed";

    String ADS_KRB_PROXIABLE_ALLOWED = "ads-krbProxiableAllowed";

    String ADS_KRB_RENEWABLE_ALLOWED = "ads-krbRenewableAllowed";

    String ADS_KRB_KDC_PRINCIPAL = "ads-krbKdcPrincipal";

    String ADS_KRB_MAXIMUM_RENEWABLE_LIFETIME = "ads-krbMaximumRenewableLifetime";

    String ADS_KRB_MAXIMUM_TICKET_LIFETIME = "ads-krbMaximumTicketLifetime";

    String ADS_KRB_PRIMARY_REALM = "ads-krbPrimaryRealm";

    String ADS_KRB_BODY_CHECKSUM_VERIFIED = "ads-krbBodyChecksumVerified";

    String ADS_DNS_SERVER_OC = "ads-dnsServer";

    String ADS_DHCP_SERVER_OC = "ads-dhcpServer";

    String ADS_NTP_SERVER_OC = "ads-ntpServer";

    String ADS_HTTP_SERVER_OC = "ads-httpServer";

    String ADS_HTTP_CONFFILE = "ads-httpConfFile";

    String ADS_DIRECTORYSERVICE_ID = "ads-directoryServiceId";

    String ADS_DS_REPLICA_ID = "ads-dsReplicaId";

    String ADS_DSINTERCEPTORS = "ads-dsInterceptors";

    String ADS_DSPARTITIONS = "ads-dsPartitions";

    String ADS_DS_ACCESSCONTROL_ENABLED = "ads-dsAccessControlEnabled";

    String ADS_DS_ALLOW_ANONYMOUS_ACCESS = "ads-dsAllowAnonymousAccess";

    String ADS_DSCHANGELOG = "ads-dsChangeLog";

    String ADS_DS_DENORMALIZE_OPATTRS_ENABLED = "ads-dsDenormalizeOpAttrsEnabled";

    String ADS_DSJOURNAL = "ads-dsJournal";

    String ADS_DS_MAXPDU_SIZE = "ads-dsMaxPDUSize";

    String ADS_DS_PASSWORD_HIDDEN = "ads-dsPasswordHidden";

    String ADS_DS_REPLICATION = "ads-dsReplication";

    String ADS_DS_SYNCPERIOD_MILLIS = "ads-dsSyncPeriodMillis";

    String ADS_DS_TEST_ENTRIES = "ads-dsTestEntries";

    String ADS_INTERCEPTOR_ID = "ads-interceptorId";

    String ADS_INTERCEPTOR_CLASSNAME = "ads-interceptorClassName";

    String ADS_INTERCEPTOR_ORDER = "ads-interceptorOrder";

    String ADS_JDBMPARTITION = "ads-jdbmPartition";

    String ADS_PARTITION_ID = "ads-partitionId";

    String ADS_PARTITION_SUFFIX = "ads-partitionSuffix";

    String ADS_PARTITION_CACHE_SIZE = "ads-partitionCacheSize";

    String ADS_JDBM_PARTITION_OPTIMIZER_ENABLED = "ads-jdbmPartitionOptimizerEnabled";

    String ADS_PARTITION_SYNCONWRITE = "ads-partitionSyncOnWrite";

    String ADS_PARTITION_INDEXED_ATTRIBUTES = "ads-partitionIndexedAttributes";

    String ADS_INDEX_ATTRIBUTE_ID = "ads-indexAttributeId";

    String ADS_JDBMINDEX = "ads-jdbmIndex";

    String ADS_INDEX_CACHESIZE = "ads-indexCacheSize";
    
    String ADS_INDEX_NUM_DUP_LIMIT = "ads-indexNumDupLimit";

    String ADS_TRANSPORT_ID = "ads-transportId";

    String ADS_TCP_TRANSPORT = "ads-tcpTransport";

    String ADS_UDP_TRANSPORT = "ads-udpTransport";

    String ADS_SYSTEM_PORT = "ads-systemPort";

    String ADS_TRANSPORT_ADDRESS = "ads-transportAddress";

    String ADS_TRANSPORT_BACKLOG = "ads-transportBacklog";

    String ADS_TRANSPORT_ENABLE_SSL = "ads-transportEnableSSL";

    String ADS_TRANSPORT_NBTHREADS = "ads-transportNbThreads";

    String ADS_CHANGELOG_ENABLED = "ads-changeLogEnabled";

    String ADS_CHANGELOG_EXPOSED = "ads-changeLogExposed";

    String ADS_JOURNAL_FILENAME = "ads-journalFileName";

    String ADS_JOURNAL_WORKINGDIR = "ads-journalWorkingDir";

    String ADS_JOURNAL_ROTATION = "ads-journalRotation";

    String ADS_JOURNAL_ENABLED = "ads-journalEnabled";

    String ADS_HTTP_WARFILE = "ads-httpWarFile";

    String ADS_HTTP_APP_CTX_PATH = "ads-httpAppCtxPath";

    String ADS_ENABLED = "ads-enabled";
    
    String ADS_CHANGEPWD_POLICY_CATEGORY_COUNT = "ads-chgPwdPolicyCategoryCount";
    
    String ADS_CHANGEPWD_POLICY_PASSWORD_LENGTH = "ads-chgPwdPolicyPasswordLength";
    
    String ADS_CHANGEPWD_POLICY_TOKEN_SIZE = "ads-chgPwdPolicyTokenSize";
    
    String ADS_CHANGEPWD_SERVICE_PRINCIPAL = "ads-chgPwdServicePrincipal";
    
    String ADS_CHANGEPWD_SERVER_OC = "ads-changePasswordServer";
    
    String ADS_REPL_SEARCH_FILTER = "ads-replSearchFilter";
    
    String ADS_REPL_LAST_SENT_CSN = "ads-replLastSentCsn";
    
    String ADS_REPL_ALIAS_DEREF_MODE = "ads-replAliasDerefMode";
    
    String ADS_SEARCH_BASE = "ads-searchBaseDN";
    
    String ADS_REPL_SEARCH_SCOPE = "ads-replSearchScope";
    
    String ADS_REPL_REFRESH_N_PERSIST = "ads-replRefreshNPersist";
    
    String ADS_REPL_PROV_HOST_NAME = "ads-replProvHostName";
    
    String ADS_REPL_PROV_PORT = "ads-replProvPort";
    
    String ADS_REPL_USER_DN = "ads-replUserDn";
    
    String ADS_REPL_USER_PASSWORD = "ads-replUserPassword";
    
    String ADS_REPL_REFRESH_INTERVAL = "ads-replRefreshInterval";
    
    String ADS_REPL_ATTRIBUTE = "ads-replAttribute";
    
    String ADS_REPL_SEARCH_SIZE_LIMIT = "ads-replSearchSizeLimit";
    
    String ADS_REPL_SEARCH_TIMEOUT = "ads-replSearchTimeOut";
    
    String ADS_REPL_COOKIE = "ads-replCookie";
    
    String ADS_REPL_CONSUMER_OC = "ads-replConsumer";
    
    String ADS_REPL_PROVIDER_OC = "ads-replProvider";
    
    String ADS_REPL_PROVIDER_IMPL = "ads-replProviderImpl";
    
    String ADS_REPL_ENABLE_PROVIDER = "ads-enableReplProvider";
    
    String ADS_LDAP_SERVER_SASL_MECH_HANDLER_OC = "ads-ldapServerSaslMechanismHandler";
    
    String ADS_LDAP_SERVER_SASL_MECH_NAME = "ads-ldapServerSaslMechName";
    
    String ADS_LDAP_SERVER_SASL_MECH_CLASS_NAME = "ads-ldapServerSaslMechClassName";
    
    String ADS_LDAP_SERVER_NTLM_MECH_PROVIDER = "ads-ldapServerNtlmMechProvider";
    
    String ADS_LDAP_SERVER_EXT_OP_HANDLER_OC = "ads-ldapServerExtendedOpHandler";
    
    String ADS_LDAP_SERVER_EXT_OP_HANDLER_FQCN = "ads-ldapServerExtendedOpHandlerClass";
    
    String ADS_LDAP_SERVER_KEYSTORE_FILE = "ads-ldapserverkeystorefile";
    
    String ADS_LDAP_SERVER_CERT_PASSWORD = "ads-ldapServerCertificatePassword";
}
