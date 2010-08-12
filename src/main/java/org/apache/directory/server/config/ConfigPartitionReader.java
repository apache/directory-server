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


import static org.apache.directory.shared.ldap.constants.PasswordPolicySchemaConstants.PWD_ALLOW_USER_CHANGE_AT;
import static org.apache.directory.shared.ldap.constants.PasswordPolicySchemaConstants.PWD_ATTRIBUTE_AT;
import static org.apache.directory.shared.ldap.constants.PasswordPolicySchemaConstants.PWD_CHECK_QUALITY_AT;
import static org.apache.directory.shared.ldap.constants.PasswordPolicySchemaConstants.PWD_EXPIRE_WARNING_AT;
import static org.apache.directory.shared.ldap.constants.PasswordPolicySchemaConstants.PWD_FAILURE_COUNT_INTERVAL_AT;
import static org.apache.directory.shared.ldap.constants.PasswordPolicySchemaConstants.PWD_GRACE_AUTHN_LIMIT_AT;
import static org.apache.directory.shared.ldap.constants.PasswordPolicySchemaConstants.PWD_GRACE_EXPIRE_AT;
import static org.apache.directory.shared.ldap.constants.PasswordPolicySchemaConstants.PWD_IN_HISTORY_AT;
import static org.apache.directory.shared.ldap.constants.PasswordPolicySchemaConstants.PWD_LOCKOUT_AT;
import static org.apache.directory.shared.ldap.constants.PasswordPolicySchemaConstants.PWD_LOCKOUT_DURATION_AT;
import static org.apache.directory.shared.ldap.constants.PasswordPolicySchemaConstants.PWD_MAX_AGE_AT;
import static org.apache.directory.shared.ldap.constants.PasswordPolicySchemaConstants.PWD_MAX_DELAY_AT;
import static org.apache.directory.shared.ldap.constants.PasswordPolicySchemaConstants.PWD_MAX_FAILURE_AT;
import static org.apache.directory.shared.ldap.constants.PasswordPolicySchemaConstants.PWD_MAX_IDLE_AT;
import static org.apache.directory.shared.ldap.constants.PasswordPolicySchemaConstants.PWD_MAX_LENGTH_AT;
import static org.apache.directory.shared.ldap.constants.PasswordPolicySchemaConstants.PWD_MIN_AGE_AT;
import static org.apache.directory.shared.ldap.constants.PasswordPolicySchemaConstants.PWD_MIN_DELAY_AT;
import static org.apache.directory.shared.ldap.constants.PasswordPolicySchemaConstants.PWD_MIN_LENGTH_AT;
import static org.apache.directory.shared.ldap.constants.PasswordPolicySchemaConstants.PWD_MUST_CHANGE_AT;
import static org.apache.directory.shared.ldap.constants.PasswordPolicySchemaConstants.PWD_POLICY_OC;
import static org.apache.directory.shared.ldap.constants.PasswordPolicySchemaConstants.PWD_SAFE_MODIFY_AT;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.naming.directory.SearchControls;

import org.apache.directory.server.changepw.ChangePasswordServer;
import org.apache.directory.server.core.DefaultDirectoryService;
import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.core.authn.AuthenticationInterceptor;
import org.apache.directory.server.core.authn.PasswordPolicyConfiguration;
import org.apache.directory.server.core.changelog.ChangeLog;
import org.apache.directory.server.core.changelog.DefaultChangeLog;
import org.apache.directory.server.core.entry.ClonedServerEntry;
import org.apache.directory.server.core.interceptor.Interceptor;
import org.apache.directory.server.core.journal.DefaultJournal;
import org.apache.directory.server.core.journal.DefaultJournalStore;
import org.apache.directory.server.core.journal.Journal;
import org.apache.directory.server.core.journal.JournalStore;
import org.apache.directory.server.core.partition.Partition;
import org.apache.directory.server.core.partition.impl.btree.jdbm.JdbmIndex;
import org.apache.directory.server.core.partition.impl.btree.jdbm.JdbmPartition;
import org.apache.directory.server.core.partition.ldif.LdifPartition;
import org.apache.directory.server.dhcp.service.DhcpService;
import org.apache.directory.server.dhcp.service.StoreBasedDhcpService;
import org.apache.directory.server.dhcp.store.DhcpStore;
import org.apache.directory.server.dhcp.store.SimpleDhcpStore;
import org.apache.directory.server.dns.DnsServer;
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.server.integration.http.HttpServer;
import org.apache.directory.server.integration.http.WebApp;
import org.apache.directory.server.kerberos.kdc.KdcServer;
import org.apache.directory.server.kerberos.shared.crypto.encryption.EncryptionType;
import org.apache.directory.server.ldap.ExtendedOperationHandler;
import org.apache.directory.server.ldap.LdapServer;
import org.apache.directory.server.ldap.handlers.bind.MechanismHandler;
import org.apache.directory.server.ldap.handlers.bind.ntlm.NtlmMechanismHandler;
import org.apache.directory.server.ldap.replication.ReplicationProvider;
import org.apache.directory.server.ldap.replication.SyncreplConfiguration;
import org.apache.directory.server.ntp.NtpServer;
import org.apache.directory.server.protocol.shared.transport.TcpTransport;
import org.apache.directory.server.protocol.shared.transport.Transport;
import org.apache.directory.server.protocol.shared.transport.UdpTransport;
import org.apache.directory.server.xdbm.ForwardIndexEntry;
import org.apache.directory.server.xdbm.Index;
import org.apache.directory.server.xdbm.IndexCursor;
import org.apache.directory.server.xdbm.search.SearchEngine;
import org.apache.directory.shared.ldap.NotImplementedException;
import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.entry.Entry;
import org.apache.directory.shared.ldap.entry.EntryAttribute;
import org.apache.directory.shared.ldap.entry.StringValue;
import org.apache.directory.shared.ldap.entry.Value;
import org.apache.directory.shared.ldap.filter.EqualityNode;
import org.apache.directory.shared.ldap.filter.PresenceNode;
import org.apache.directory.shared.ldap.filter.SearchScope;
import org.apache.directory.shared.ldap.ldif.LdifEntry;
import org.apache.directory.shared.ldap.ldif.LdifReader;
import org.apache.directory.shared.ldap.message.AliasDerefMode;
import org.apache.directory.shared.ldap.name.DN;
import org.apache.directory.shared.ldap.schema.AttributeType;
import org.apache.directory.shared.ldap.schema.SchemaManager;
import org.apache.directory.shared.ldap.util.StringTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
;

/**
 * A class used for reading the configuration present in a Partition
 * and instantiate the necessary objects like DirectoryService, Interceptors etc.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class ConfigPartitionReader
{
    private static final Logger LOG = LoggerFactory.getLogger( ConfigPartitionReader.class );

    /** the partition which holds the configuration data */
    private LdifPartition configPartition;

    /** the search engine of the partition */
    private SearchEngine<Entry, Long> se;

    /** the schema manager set in the config partition */
    private SchemaManager schemaManager;

    /** A reference to the ObjectClass AT */
    private static AttributeType OBJECT_CLASS_AT;

    /** the parent directory of the config partition's working directory */
    private File workDir;

    /** LDIF file filter */
    private FilenameFilter ldifFilter = new FilenameFilter()
    {
        public boolean accept( File file, String name )
        {
            if ( file.isDirectory() )
            {
                return true;
            }

            return file.getName().toLowerCase().endsWith( ".ldif" );
        }
    };


    /**
     * 
     * Creates a new instance of ConfigPartitionReader.
     *
     * @param configPartition the non null config partition
     */
    public ConfigPartitionReader( LdifPartition configPartition )
    {
        if ( configPartition == null )
        {
            throw new IllegalArgumentException( I18n.err( I18n.ERR_503 ) );
        }

        if ( !configPartition.isInitialized() )
        {
            throw new IllegalStateException( I18n.err( I18n.ERR_504 ) );
        }

        this.configPartition = configPartition;
        se = configPartition.getSearchEngine();
        this.schemaManager = configPartition.getSchemaManager();
        workDir = configPartition.getPartitionDir().getParentFile();
        
        // setup ObjectClass attribute type value
        OBJECT_CLASS_AT = schemaManager.getAttributeType( SchemaConstants.OBJECT_CLASS_AT );
    }


    /**
     * reads the LDAP server configuration and instantiates without setting a DirectoryService 
     *
     * @return the LdapServer instance without a DirectoryService
     * @throws Exception
     */
    public LdapServer getLdapServer() throws Exception
    {
        EqualityNode<String> filter = new EqualityNode<String>( OBJECT_CLASS_AT, new StringValue(
            ConfigSchemaConstants.ADS_LDAP_SERVER_OC ) );
        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.SUBTREE_SCOPE );

        IndexCursor<Long, Entry, Long> cursor = se.cursor( configPartition.getSuffix(),
            AliasDerefMode.NEVER_DEREF_ALIASES, filter, controls );

        if ( !cursor.next() )
        {
            LOG.warn( "No LDAP server was configured under the DN {}", configPartition.getSuffix() );
            return null;
        }

        ForwardIndexEntry<Long, Entry, Long> forwardEntry = ( ForwardIndexEntry<Long, Entry, Long> ) cursor
            .get();
        cursor.close();

        ClonedServerEntry ldapServerEntry = configPartition.lookup( forwardEntry.getId() );
        LOG.debug( "LDAP Server Entry {}", ldapServerEntry );

        if ( !isEnabled( ldapServerEntry ) )
        {
            return null;
        }

        LdapServer server = new LdapServer();
        server.setServiceId( getString( ConfigSchemaConstants.ADS_SERVER_ID, ldapServerEntry ) );

        Transport[] transports = getTransports( ldapServerEntry.getDn() );
        server.setTransports( transports );

        EntryAttribute replProvImplAttr = ldapServerEntry.get( ConfigSchemaConstants.ADS_REPL_PROVIDER_IMPL );
        if( replProvImplAttr != null )
        {
            String fqcn = replProvImplAttr.getString();
            try
            {
                Class<?> replProvImplClz = Class.forName( fqcn );
                ReplicationProvider rp = ( ReplicationProvider ) replProvImplClz.newInstance();
                server.setReplicationProvider( rp );
            }
            catch( ClassNotFoundException e )
            {
                LOG.error( "Failed to load and instantiate ReplicationProvider implementation", e );
                throw e;
            }
        }
        
        server.setReplProviderConfigs( getReplProviderConfigs() );
        
        EntryAttribute searchBaseAttr = ldapServerEntry.get( ConfigSchemaConstants.ADS_SEARCH_BASE );
        if( searchBaseAttr != null )
        {
            server.setSearchBaseDn( searchBaseAttr.getString() );
        }
        
        // read the SASL mechanism handlers' configuration
        filter = new EqualityNode<String>( OBJECT_CLASS_AT, new StringValue(
            ConfigSchemaConstants.ADS_LDAP_SERVER_SASL_MECH_HANDLER_OC ) );
        cursor = se.cursor( ldapServerEntry.getDn(), AliasDerefMode.NEVER_DEREF_ALIASES, filter, controls );
        
        while( cursor.next() )
        {
            ForwardIndexEntry<Long, Entry, Long> forwardSaslMechEntry = ( ForwardIndexEntry<Long, Entry, Long> ) cursor.get();
            Entry saslMechHandlerEntry = configPartition.lookup( forwardSaslMechEntry.getId() );
            if( isEnabled( saslMechHandlerEntry ) )
            {
                String mechanism = getString( ConfigSchemaConstants.ADS_LDAP_SERVER_SASL_MECH_NAME, saslMechHandlerEntry );
                server.addSaslMechanismHandler( mechanism, getSaslMechHandler( saslMechHandlerEntry ) );
            }
        }
        
        cursor.close();
        
        // read the extnded operation handlers' config
        filter = new EqualityNode<String>( OBJECT_CLASS_AT, new StringValue(
            ConfigSchemaConstants.ADS_LDAP_SERVER_EXT_OP_HANDLER_OC ) );
        cursor = se.cursor( ldapServerEntry.getDn(), AliasDerefMode.NEVER_DEREF_ALIASES, filter, controls );
        
        while( cursor.next() )
        {
            ForwardIndexEntry<Long, Entry, Long> forwardExtOpEntry = ( ForwardIndexEntry<Long, Entry, Long> ) cursor.get();
            Entry extOpHandlerEntry = configPartition.lookup( forwardExtOpEntry.getId() );
            
            if( !isEnabled( extOpHandlerEntry ) )
            {
                continue;
            }
            
            Class<?> extendedOpClass = Class.forName( extOpHandlerEntry.get( ConfigSchemaConstants.ADS_LDAP_SERVER_EXT_OP_HANDLER_FQCN ).getString() );
            ExtendedOperationHandler extOpHandler = ( ExtendedOperationHandler ) extendedOpClass.newInstance();
            server.addExtendedOperationHandler( extOpHandler );
        }
        
        cursor.close();
        
        EntryAttribute keyStoreAttr = ldapServerEntry.get( ConfigSchemaConstants.ADS_LDAP_SERVER_KEYSTORE_FILE );
        if( keyStoreAttr != null )
        {
            server.setKeystoreFile( keyStoreAttr.getString() );
            
            EntryAttribute certPwdAttr = ldapServerEntry.get( ConfigSchemaConstants.ADS_LDAP_SERVER_CERT_PASSWORD );
            if( certPwdAttr != null )
            {
                server.setCertificatePassword( certPwdAttr.getString() );
            }
        }
        
        
        return server;
    }


    public KdcServer getKdcServer() throws Exception
    {
        EqualityNode<String> filter = new EqualityNode<String>( OBJECT_CLASS_AT, new StringValue(
            ConfigSchemaConstants.ADS_KERBEROS_SERVER_OC ) );
        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.SUBTREE_SCOPE );

        IndexCursor<Long, Entry, Long> cursor = se.cursor( configPartition.getSuffix(),
            AliasDerefMode.NEVER_DEREF_ALIASES, filter, controls );

        if ( !cursor.next() )
        {
            LOG.warn( "No kerberos server was configured under the DN {}", configPartition.getSuffix() );
            return null;
        }

        ForwardIndexEntry<Long, Entry, Long> forwardEntry = ( ForwardIndexEntry<Long, Entry, Long> ) cursor
            .get();
        cursor.close();

        ClonedServerEntry kdcEntry = configPartition.lookup( forwardEntry.getId() );
        LOG.debug( "kerberos server entry {}", kdcEntry );

        if ( !isEnabled( kdcEntry ) )
        {
            return null;
        }

        KdcServer kdcServer = new KdcServer();

        kdcServer.setServiceId( getString( ConfigSchemaConstants.ADS_SERVER_ID, kdcEntry ) );

        Transport[] transports = getTransports( kdcEntry.getDn() );
        kdcServer.setTransports( transports );

        // MAY attributes
        EntryAttribute clockSkewAttr = kdcEntry.get( ConfigSchemaConstants.ADS_KRB_ALLOWABLE_CLOCKSKEW );

        if ( clockSkewAttr != null )
        {
            kdcServer.setAllowableClockSkew( Long.parseLong( clockSkewAttr.getString() ) );
        }

        EntryAttribute encryptionTypeAttr = kdcEntry.get( ConfigSchemaConstants.ADS_KRB_ENCRYPTION_TYPES );

        if ( encryptionTypeAttr != null )
        {
            EncryptionType[] encryptionTypes = new EncryptionType[encryptionTypeAttr.size()];
            Iterator<Value<?>> itr = encryptionTypeAttr.getAll();
            int count = 0;

            while ( itr.hasNext() )
            {
                Value<?> val = itr.next();
                encryptionTypes[count++] = EncryptionType.getByName( val.getString() );
            }

            kdcServer.setEncryptionTypes( encryptionTypes );
        }

        EntryAttribute emptyAddrAttr = kdcEntry.get( ConfigSchemaConstants.ADS_KRB_EMPTY_ADDRESSES_ALLOWED );

        if ( emptyAddrAttr != null )
        {
            kdcServer.setEmptyAddressesAllowed( Boolean.parseBoolean( emptyAddrAttr.getString() ) );
        }

        EntryAttribute fwdAllowedAttr = kdcEntry.get( ConfigSchemaConstants.ADS_KRB_FORWARDABLE_ALLOWED );

        if ( fwdAllowedAttr != null )
        {
            kdcServer.setForwardableAllowed( Boolean.parseBoolean( fwdAllowedAttr.getString() ) );
        }

        EntryAttribute paEncTmstpAttr = kdcEntry.get( ConfigSchemaConstants.ADS_KRB_PAENC_TIMESTAMP_REQUIRED );

        if ( paEncTmstpAttr != null )
        {
            kdcServer.setPaEncTimestampRequired( Boolean.parseBoolean( paEncTmstpAttr.getString() ) );
        }

        EntryAttribute posdtAllowedAttr = kdcEntry.get( ConfigSchemaConstants.ADS_KRB_POSTDATED_ALLOWED );

        if ( posdtAllowedAttr != null )
        {
            kdcServer.setPostdatedAllowed( Boolean.parseBoolean( posdtAllowedAttr.getString() ) );
        }

        EntryAttribute prxyAllowedAttr = kdcEntry.get( ConfigSchemaConstants.ADS_KRB_PROXIABLE_ALLOWED );

        if ( prxyAllowedAttr != null )
        {
            kdcServer.setProxiableAllowed( Boolean.parseBoolean( prxyAllowedAttr.getString() ) );
        }

        EntryAttribute rnwAllowedAttr = kdcEntry.get( ConfigSchemaConstants.ADS_KRB_RENEWABLE_ALLOWED );

        if ( rnwAllowedAttr != null )
        {
            kdcServer.setRenewableAllowed( Boolean.parseBoolean( rnwAllowedAttr.getString() ) );
        }

        EntryAttribute kdcPrncplAttr = kdcEntry.get( ConfigSchemaConstants.ADS_KRB_KDC_PRINCIPAL );

        if ( kdcPrncplAttr != null )
        {
            kdcServer.setKdcPrincipal( kdcPrncplAttr.getString() );
        }

        EntryAttribute maxRnwLfTimeAttr = kdcEntry.get( ConfigSchemaConstants.ADS_KRB_MAXIMUM_RENEWABLE_LIFETIME );

        if ( maxRnwLfTimeAttr != null )
        {
            kdcServer.setMaximumRenewableLifetime( Long.parseLong( maxRnwLfTimeAttr.getString() ) );
        }

        EntryAttribute maxTcktLfTimeAttr = kdcEntry.get( ConfigSchemaConstants.ADS_KRB_MAXIMUM_TICKET_LIFETIME );

        if ( maxTcktLfTimeAttr != null )
        {
            kdcServer.setMaximumTicketLifetime( Long.parseLong( maxTcktLfTimeAttr.getString() ) );
        }

        EntryAttribute prmRealmAttr = kdcEntry.get( ConfigSchemaConstants.ADS_KRB_PRIMARY_REALM );

        if ( prmRealmAttr != null )
        {
            kdcServer.setPrimaryRealm( prmRealmAttr.getString() );
        }

        EntryAttribute bdyCkhsmVerifyAttr = kdcEntry.get( ConfigSchemaConstants.ADS_KRB_BODY_CHECKSUM_VERIFIED );

        if ( bdyCkhsmVerifyAttr != null )
        {
            kdcServer.setBodyChecksumVerified( Boolean.parseBoolean( bdyCkhsmVerifyAttr.getString() ) );
        }

        EntryAttribute searchBaseAttr = kdcEntry.get( ConfigSchemaConstants.ADS_SEARCH_BASE );
        if( searchBaseAttr != null )
        {
            kdcServer.setSearchBaseDn( searchBaseAttr.getString() );
        }
        
        return kdcServer;
    }


    public DnsServer getDnsServer() throws Exception
    {
        EqualityNode<String> filter = new EqualityNode<String>( OBJECT_CLASS_AT, new StringValue(
            ConfigSchemaConstants.ADS_DNS_SERVER_OC ) );
        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.SUBTREE_SCOPE );

        IndexCursor<Long, Entry, Long> cursor = se.cursor( configPartition.getSuffix(),
            AliasDerefMode.NEVER_DEREF_ALIASES, filter, controls );

        if ( !cursor.next() )
        {
            LOG.warn( "No DNS server was configured under the DN {}", configPartition.getSuffix() );
            return null;
        }

        ForwardIndexEntry<Long, Entry, Long> forwardEntry = ( ForwardIndexEntry<Long, Entry, Long> ) cursor
            .get();
        cursor.close();

        ClonedServerEntry dnsEntry = configPartition.lookup( forwardEntry.getId() );
        LOG.debug( "DNS server entry {}", dnsEntry );

        if ( !isEnabled( dnsEntry ) )
        {
            return null;
        }

        DnsServer dnsServer = new DnsServer();

        dnsServer.setServiceId( getString( ConfigSchemaConstants.ADS_SERVER_ID, dnsEntry ) );

        Transport[] transports = getTransports( dnsEntry.getDn() );
        dnsServer.setTransports( transports );

        return dnsServer;
    }


    //TODO making this method invisible cause there is no DhcpServer exists as of now
    private DhcpService getDhcpServer() throws Exception
    {
        EqualityNode<String> filter = new EqualityNode<String>( OBJECT_CLASS_AT, new StringValue(
            ConfigSchemaConstants.ADS_DHCP_SERVER_OC ) );
        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.SUBTREE_SCOPE );

        IndexCursor<Long, Entry, Long> cursor = se.cursor( configPartition.getSuffix(),
            AliasDerefMode.NEVER_DEREF_ALIASES, filter, controls );

        if ( !cursor.next() )
        {
            LOG.warn( "No DHCP server was configured under the DN {}", configPartition.getSuffix() );
            return null;
        }

        ForwardIndexEntry<Long, Entry, Long> forwardEntry = ( ForwardIndexEntry<Long, Entry, Long> ) cursor
            .get();
        cursor.close();

        ClonedServerEntry dhcpEntry = configPartition.lookup( forwardEntry.getId() );
        LOG.debug( "DHCP server entry {}", dhcpEntry );

        if ( !isEnabled( dhcpEntry ) )
        {
            return null;
        }

        DhcpStore dhcpStore = new SimpleDhcpStore();
        DhcpService dhcpService = new StoreBasedDhcpService( dhcpStore );

        return dhcpService;
    }


    public NtpServer getNtpServer() throws Exception
    {
        EqualityNode<String> filter = new EqualityNode<String>( OBJECT_CLASS_AT, new StringValue(
            ConfigSchemaConstants.ADS_NTP_SERVER_OC ) );
        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.SUBTREE_SCOPE );

        IndexCursor<Long, Entry, Long> cursor = se.cursor( configPartition.getSuffix(),
            AliasDerefMode.NEVER_DEREF_ALIASES, filter, controls );

        if ( !cursor.next() )
        {
            LOG.warn( "No NTP server was configured under the DN {}", configPartition.getSuffix() );
            return null;
        }

        ForwardIndexEntry<Long, Entry, Long> forwardEntry = ( ForwardIndexEntry<Long, Entry, Long> ) cursor
            .get();
        cursor.close();

        ClonedServerEntry ntpEntry = configPartition.lookup( forwardEntry.getId() );
        LOG.debug( "NTP server entry {}", ntpEntry );

        if ( !isEnabled( ntpEntry ) )
        {
            return null;
        }

        NtpServer ntpServer = new NtpServer();

        ntpServer.setServiceId( getString( ConfigSchemaConstants.ADS_SERVER_ID, ntpEntry ) );

        Transport[] transports = getTransports( ntpEntry.getDn() );
        ntpServer.setTransports( transports );

        return ntpServer;
    }

    
    public ChangePasswordServer getChangePwdServer() throws Exception
    {
        EqualityNode<String> filter = new EqualityNode<String>( OBJECT_CLASS_AT, new StringValue(
            ConfigSchemaConstants.ADS_CHANGEPWD_SERVER_OC ) );
        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.SUBTREE_SCOPE );

        IndexCursor<Long, Entry, Long> cursor = se.cursor( configPartition.getSuffix(),
            AliasDerefMode.NEVER_DEREF_ALIASES, filter, controls );

        if ( !cursor.next() )
        {
            LOG.warn( "No ChangePassword server was configured under the DN {}", configPartition.getSuffix() );
            return null;
        }
        
        ForwardIndexEntry<Long, Entry, Long> forwardEntry = ( ForwardIndexEntry<Long, Entry, Long> ) cursor.get();
        cursor.close();

        ClonedServerEntry chgPwdEntry = configPartition.lookup( forwardEntry.getId() );
        LOG.debug( "Changepassword server entry {}", chgPwdEntry );
    
        if ( !isEnabled( chgPwdEntry ) )
        {
            return null;
        }

        ChangePasswordServer chgPwdServer = new ChangePasswordServer();

        chgPwdServer.setServiceId( getString( ConfigSchemaConstants.ADS_SERVER_ID, chgPwdEntry ) );

        Transport[] transports = getTransports( chgPwdEntry.getDn() );
        chgPwdServer.setTransports( transports );

        // MAY attributes
        EntryAttribute clockSkewAttr = chgPwdEntry.get( ConfigSchemaConstants.ADS_KRB_ALLOWABLE_CLOCKSKEW );

        if ( clockSkewAttr != null )
        {
            chgPwdServer.setAllowableClockSkew( Long.parseLong( clockSkewAttr.getString() ) );
        }

        EntryAttribute encryptionTypeAttr = chgPwdEntry.get( ConfigSchemaConstants.ADS_KRB_ENCRYPTION_TYPES );

        if ( encryptionTypeAttr != null )
        {
            EncryptionType[] encryptionTypes = new EncryptionType[encryptionTypeAttr.size()];
            Iterator<Value<?>> itr = encryptionTypeAttr.getAll();
            int count = 0;

            while ( itr.hasNext() )
            {
                Value<?> val = itr.next();
                encryptionTypes[count++] = EncryptionType.getByName( val.getString() );
            }

            chgPwdServer.setEncryptionTypes( encryptionTypes );
        }

        EntryAttribute emptyAddrAttr = chgPwdEntry.get( ConfigSchemaConstants.ADS_KRB_EMPTY_ADDRESSES_ALLOWED );

        if ( emptyAddrAttr != null )
        {
            chgPwdServer.setEmptyAddressesAllowed( Boolean.parseBoolean( emptyAddrAttr.getString() ) );
        }

        EntryAttribute prmRealmAttr = chgPwdEntry.get( ConfigSchemaConstants.ADS_KRB_PRIMARY_REALM );

        if ( prmRealmAttr != null )
        {
            chgPwdServer.setPrimaryRealm( prmRealmAttr.getString() );
        }
        
        EntryAttribute policyCatCount = chgPwdEntry.get( ConfigSchemaConstants.ADS_CHANGEPWD_POLICY_CATEGORY_COUNT );
        if( policyCatCount != null )
        {
            chgPwdServer.setPolicyCategoryCount( getInt( ConfigSchemaConstants.ADS_CHANGEPWD_POLICY_CATEGORY_COUNT, chgPwdEntry ) );
        }

        EntryAttribute policyPwdLen = chgPwdEntry.get( ConfigSchemaConstants.ADS_CHANGEPWD_POLICY_PASSWORD_LENGTH );
        
        if( policyPwdLen != null )
        {
            chgPwdServer.setPolicyPasswordLength( getInt( ConfigSchemaConstants.ADS_CHANGEPWD_POLICY_PASSWORD_LENGTH, chgPwdEntry ) );
        }
        
        EntryAttribute policyTokenSize = chgPwdEntry.get( ConfigSchemaConstants.ADS_CHANGEPWD_POLICY_TOKEN_SIZE );
        
        if( policyTokenSize != null )
        {
            chgPwdServer.setPolicyTokenSize( getInt( ConfigSchemaConstants.ADS_CHANGEPWD_POLICY_TOKEN_SIZE, chgPwdEntry ) );
        }
        
        EntryAttribute servicePrincipal = chgPwdEntry.get( ConfigSchemaConstants.ADS_CHANGEPWD_SERVICE_PRINCIPAL );
        
        if( servicePrincipal != null )
        {
            chgPwdServer.setServicePrincipal( servicePrincipal.getString() );
        }
        
        return chgPwdServer;
    }

    
    public HttpServer getHttpServer() throws Exception
    {
        EqualityNode<String> filter = new EqualityNode<String>( OBJECT_CLASS_AT, new StringValue(
            ConfigSchemaConstants.ADS_HTTP_SERVER_OC ) );
        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.SUBTREE_SCOPE );

        IndexCursor<Long, Entry, Long> cursor = se.cursor( configPartition.getSuffix(),
            AliasDerefMode.NEVER_DEREF_ALIASES, filter, controls );

        if ( !cursor.next() )
        {
            LOG.warn( "No HTTP server was configured under the DN {}", configPartition.getSuffix() );
            return null;
        }

        ForwardIndexEntry<Long, Entry, Long> forwardEntry = ( ForwardIndexEntry<Long, Entry, Long> ) cursor
            .get();
        cursor.close();

        ClonedServerEntry httpEntry = configPartition.lookup( forwardEntry.getId() );
        LOG.debug( "HTTP server entry {}", httpEntry );

        if ( !isEnabled( httpEntry ) )
        {
            return null;
        }

        HttpServer httpServer = new HttpServer();

        EntryAttribute portAttr = httpEntry.get( ConfigSchemaConstants.ADS_SYSTEM_PORT );

        if ( portAttr != null )
        {
            httpServer.setPort( Integer.parseInt( portAttr.getString() ) );
        }

        EntryAttribute confFileAttr = httpEntry.get( ConfigSchemaConstants.ADS_HTTP_CONFFILE );

        if ( confFileAttr != null )
        {
            httpServer.setConfFile( confFileAttr.getString() );
        }

        DN webAppsDN = new DN( httpEntry.getDn().getName(), schemaManager );

        Set<WebApp> webApps = getWebApps( webAppsDN );
        httpServer.setWebApps( webApps );

        return httpServer;
    }


    /**
     * 
     * instantiates a DirectoryService based on the configuration present in the partition 
     *
     * @throws Exception
     */
    public DirectoryService getDirectoryService() throws Exception
    {
        AttributeType adsDirectoryServiceidAt = schemaManager.getAttributeType( ConfigSchemaConstants.ADS_DIRECTORYSERVICE_ID );
        PresenceNode filter = new PresenceNode( adsDirectoryServiceidAt );
        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.SUBTREE_SCOPE );

        IndexCursor<Long, Entry, Long> cursor = se.cursor( configPartition.getSuffix(),
            AliasDerefMode.NEVER_DEREF_ALIASES, filter, controls );

        if ( !cursor.next() )
        {
            // the DirectoryService is mandatory so throwing exception
            throw new Exception( "No directoryService instance was configured under the DN "
                + configPartition.getSuffix() );
        }

        ForwardIndexEntry<Long, Entry, Long> forwardEntry = ( ForwardIndexEntry<Long, Entry, Long> ) cursor
            .get();
        cursor.close();

        ClonedServerEntry dsEntry = configPartition.lookup( forwardEntry.getId() );

        LOG.debug( "directory service entry {}", dsEntry );

        DirectoryService dirService = new DefaultDirectoryService();
        // MUST attributes
        dirService.setInstanceId( getString( ConfigSchemaConstants.ADS_DIRECTORYSERVICE_ID, dsEntry ) );
        dirService.setReplicaId( getInt( ConfigSchemaConstants.ADS_DS_REPLICA_ID, dsEntry ) );

        List<Interceptor> interceptors = getInterceptors( dsEntry.getDn() );
        dirService.setInterceptors( interceptors );
        
        AuthenticationInterceptor authnInterceptor = ( AuthenticationInterceptor ) dirService.getInterceptor( AuthenticationInterceptor.class.getName() );
        authnInterceptor.setPwdPolicyConfig( getPwdPolicyConfig( dsEntry.getDn() ) );

        Map<String, Partition> partitions = getPartitions( dsEntry.getDn() );

        Partition systemPartition = partitions.remove( "system" );

        if ( systemPartition == null )
        {
            throw new Exception( I18n.err( I18n.ERR_505 ) );
        }

        dirService.setSystemPartition( systemPartition );
        dirService.setPartitions( new HashSet<Partition>( partitions.values() ) );

        // MAY attributes
        EntryAttribute acEnabledAttr = dsEntry.get( ConfigSchemaConstants.ADS_DS_ACCESSCONTROL_ENABLED );

        if ( acEnabledAttr != null )
        {
            dirService.setAccessControlEnabled( Boolean.parseBoolean( acEnabledAttr.getString() ) );
        }

        EntryAttribute anonAccessAttr = dsEntry.get( ConfigSchemaConstants.ADS_DS_ALLOW_ANONYMOUS_ACCESS );

        if ( anonAccessAttr != null )
        {
            dirService.setAllowAnonymousAccess( Boolean.parseBoolean( anonAccessAttr.getString() ) );
        }

        EntryAttribute changeLogAttr = dsEntry.get( ConfigSchemaConstants.ADS_DSCHANGELOG );

        if ( changeLogAttr != null )
        {
            DN clDN = new DN( changeLogAttr.getString(), schemaManager );
            ChangeLog cl = getChangeLog( clDN );
            dirService.setChangeLog( cl );
        }

        EntryAttribute denormAttr = dsEntry.get( ConfigSchemaConstants.ADS_DS_DENORMALIZE_OPATTRS_ENABLED );

        if ( denormAttr != null )
        {
            dirService.setDenormalizeOpAttrsEnabled( Boolean.parseBoolean( denormAttr.getString() ) );
        }

        EntryAttribute journalAttr = dsEntry.get( ConfigSchemaConstants.ADS_DSJOURNAL );

        if ( journalAttr != null )
        {
            DN journalDN = new DN( journalAttr.getString(), schemaManager );
            dirService.setJournal( getJournal( journalDN ) );
        }

        EntryAttribute maxPduAttr = dsEntry.get( ConfigSchemaConstants.ADS_DS_MAXPDU_SIZE );

        if ( maxPduAttr != null )
        {
            dirService.setMaxPDUSize( Integer.parseInt( maxPduAttr.getString() ) );
        }

        EntryAttribute passwordHidAttr = dsEntry.get( ConfigSchemaConstants.ADS_DS_PASSWORD_HIDDEN );

        if ( passwordHidAttr != null )
        {
            dirService.setPasswordHidden( Boolean.parseBoolean( passwordHidAttr.getString() ) );
        }

        EntryAttribute replAttr = dsEntry.get( ConfigSchemaConstants.ADS_DS_REPLICATION );

        if ( replAttr != null )
        {
            // configure replication
        }

        EntryAttribute syncPeriodAttr = dsEntry.get( ConfigSchemaConstants.ADS_DS_SYNCPERIOD_MILLIS );

        if ( syncPeriodAttr != null )
        {
            dirService.setSyncPeriodMillis( Long.parseLong( syncPeriodAttr.getString() ) );
        }

        EntryAttribute testEntryAttr = dsEntry.get( ConfigSchemaConstants.ADS_DS_TEST_ENTRIES );

        if ( testEntryAttr != null )
        {
            String entryFilePath = testEntryAttr.getString();
            dirService.setTestEntries( getTestEntries( entryFilePath ) );
        }

        if ( !isEnabled( dsEntry ) )
        {
            // will only be useful if we ever allow more than one DS to be configured and
            // switch between them
            // decide which one to use based on this flag
        }

        return dirService;
    }


    private List<SyncreplConfiguration> getReplProviderConfigs() throws Exception
    {
        EqualityNode<String> filter = new EqualityNode<String>( OBJECT_CLASS_AT, new StringValue(
            ConfigSchemaConstants.ADS_REPL_PROVIDER_OC ) );

        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.SUBTREE_SCOPE );

        IndexCursor<Long, Entry, Long> cursor = se.cursor( configPartition.getSuffix(),
            AliasDerefMode.NEVER_DEREF_ALIASES, filter, controls );

        List<SyncreplConfiguration> syncReplConfigLst = new ArrayList<SyncreplConfiguration>();

        if ( !cursor.next() )
        {
            return syncReplConfigLst;
        }
     
        do
        {
            ForwardIndexEntry<Long, Entry, Long> forwardEntry = ( ForwardIndexEntry<Long, Entry, Long> ) cursor.get();
        
            ClonedServerEntry entry = configPartition.lookup( forwardEntry.getId() );

            LOG.debug( "syncrepl configuration entry {}", entry );
            
            if( !isEnabled( entry ) )
            {
                continue;
            }

            SyncreplConfiguration config = new SyncreplConfiguration();
            
            // mandatory attribues
            config.setReplicaId( getInt( ConfigSchemaConstants.ADS_DS_REPLICA_ID, entry ) );
            config.setProviderHost( entry.get( ConfigSchemaConstants.ADS_REPL_PROV_HOST_NAME ).getString() );
            config.setBaseDn( entry.get( ConfigSchemaConstants.ADS_SEARCH_BASE ).getString() );
            
            // optional attributes
            
            EntryAttribute aliasDerefAttr = entry.get( ConfigSchemaConstants.ADS_REPL_ALIAS_DEREF_MODE );
            if( aliasDerefAttr != null )
            {
                config.setAliasDerefMode( AliasDerefMode.getDerefMode( getInt( aliasDerefAttr ) ) );
            }
            
            EntryAttribute replSrchAtAttr = entry.get( ConfigSchemaConstants.ADS_REPL_ATTRIBUTE );
            if( replSrchAtAttr != null )
            {
                int size = replSrchAtAttr.size();
                String[] attrNames = new String[ size ];
                for( int i=0; i< size; i++ )
                {
                    attrNames[i] = replSrchAtAttr.get( i ).getString();
                }
                
                config.setAttributes( attrNames );
            }
            
            EntryAttribute provPortAttr = entry.get( ConfigSchemaConstants.ADS_REPL_PROV_PORT );
            if( provPortAttr != null )
            {
                config.setPort( getInt( provPortAttr ) );
            }
            
            EntryAttribute refreshIntAttr = entry.get( ConfigSchemaConstants.ADS_REPL_REFRESH_INTERVAL );
            if( refreshIntAttr != null )
            {
                config.setRefreshInterval( getInt( refreshIntAttr) );
            }
            
            EntryAttribute refNPersistAttr = entry.get( ConfigSchemaConstants.ADS_REPL_REFRESH_N_PERSIST );
            if( refNPersistAttr != null )
            {
                config.setRefreshNPersist( Boolean.parseBoolean( refNPersistAttr.getString() ) );
            }
            
            EntryAttribute searchScopeAttr = entry.get( ConfigSchemaConstants.ADS_REPL_SEARCH_SCOPE );
            if( searchScopeAttr != null )
            {
                config.setSearchScope( SearchScope.getSearchScope( getInt( searchScopeAttr ) ) );
            }
            
            EntryAttribute searchFilterAttr = entry.get( ConfigSchemaConstants.ADS_REPL_SEARCH_FILTER );
            if( searchFilterAttr != null )
            {
                config.setFilter( searchFilterAttr.getString() );
            }

            EntryAttribute searchSizeAttr = entry.get( ConfigSchemaConstants.ADS_REPL_SEARCH_SIZE_LIMIT );
            if( searchSizeAttr != null )
            {
                config.setSearchSizeLimit( getInt( searchSizeAttr ) );
            }
            
            EntryAttribute searchTimeAttr = entry.get( ConfigSchemaConstants.ADS_REPL_SEARCH_TIMEOUT );
            if( searchTimeAttr != null )
            {
                config.setSearchTimeout( getInt( searchTimeAttr ) );
            }

            
            EntryAttribute replUserAttr = entry.get( ConfigSchemaConstants.ADS_REPL_USER_DN );
            if( replUserAttr != null )
            {
                config.setReplUserDn( replUserAttr.getString() );
            }
            
            EntryAttribute replUserPwdAttr = entry.get( ConfigSchemaConstants.ADS_REPL_USER_PASSWORD );
            if( replUserPwdAttr != null )
            {
                config.setReplUserPassword( replUserPwdAttr.getBytes() );
            }
            
            EntryAttribute replCookieAttr = entry.get( ConfigSchemaConstants.ADS_REPL_COOKIE );
            if( replCookieAttr != null )
            {
                config.setCookie( replCookieAttr.getBytes() );
            }
            
            syncReplConfigLst.add( config );
        }
        while( cursor.next() );
        
        cursor.close();
        
        return syncReplConfigLst;
    }
    
    
    /**
     * reads the Interceptor configuration and instantiates them in the order specified
     *
     * @param dirServiceDN the DN under which interceptors are configured
     * @return a list of instantiated Interceptor objects
     * @throws Exception
     */
    private List<Interceptor> getInterceptors( DN dirServiceDN ) throws Exception
    {
        AttributeType adsInterceptorIdAt = schemaManager.getAttributeType( ConfigSchemaConstants.ADS_INTERCEPTOR_ID );
        PresenceNode filter = new PresenceNode( adsInterceptorIdAt );
        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.SUBTREE_SCOPE );
        IndexCursor<Long, Entry, Long> cursor = se.cursor( dirServiceDN, AliasDerefMode.NEVER_DEREF_ALIASES,
            filter, controls );

        Set<InterceptorConfig> set = new TreeSet<InterceptorConfig>();

        while ( cursor.next() )
        {
            ForwardIndexEntry<Long, Entry, Long> forwardEntry = ( ForwardIndexEntry<Long, Entry, Long> ) cursor
                .get();
            Entry interceptorEntry = configPartition.lookup( forwardEntry.getId() );
            
            if( ! isEnabled( interceptorEntry ) )
            {
                continue;
            }

            String id = getString( ConfigSchemaConstants.ADS_INTERCEPTOR_ID, interceptorEntry );
            String fqcn = getString( ConfigSchemaConstants.ADS_INTERCEPTOR_CLASSNAME, interceptorEntry );
            int order = getInt( ConfigSchemaConstants.ADS_INTERCEPTOR_ORDER, interceptorEntry );

            InterceptorConfig intConfig = new InterceptorConfig( id, fqcn, order );
            set.add( intConfig );
        }

        cursor.close();

        List<Interceptor> interceptors = new ArrayList<Interceptor>();

        for ( InterceptorConfig iconfig : set )
        {
            try
            {
                LOG.debug( "loading the interceptor class {} and instantiating", iconfig.getFqcn() );
                Interceptor ic = ( Interceptor ) Class.forName( iconfig.getFqcn() ).newInstance();
                interceptors.add( ic );
            }
            catch ( Exception e )
            {
                throw e;
            }
        }

        return interceptors;
    }


    private Map<String, Partition> getPartitions( DN dirServiceDN ) throws Exception
    {
        AttributeType adsPartitionIdeAt = schemaManager.getAttributeType( ConfigSchemaConstants.ADS_PARTITION_ID );
        PresenceNode filter = new PresenceNode( adsPartitionIdeAt );
        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.SUBTREE_SCOPE );
        IndexCursor<Long, Entry, Long> cursor = se.cursor( dirServiceDN, AliasDerefMode.NEVER_DEREF_ALIASES,
            filter, controls );

        Map<String, Partition> partitions = new HashMap<String, Partition>();

        while ( cursor.next() )
        {
            ForwardIndexEntry<Long, Entry, Long> forwardEntry = ( ForwardIndexEntry<Long, Entry, Long> ) cursor
                .get();
            Entry partitionEntry = configPartition.lookup( forwardEntry.getId() );

            if ( !isEnabled( partitionEntry ) )
            {
                continue;
            }
            EntryAttribute ocAttr = partitionEntry.get( OBJECT_CLASS_AT );

            if ( ocAttr.contains( ConfigSchemaConstants.ADS_JDBMPARTITION ) )
            {
                JdbmPartition partition = getJdbmPartition( partitionEntry );
                partitions.put( partition.getId(), partition );
            }
            else
            {
                throw new NotImplementedException( I18n.err( I18n.ERR_506 ) );
            }
        }

        cursor.close();

        return partitions;
    }


    private JdbmPartition getJdbmPartition( Entry partitionEntry ) throws Exception
    {
        JdbmPartition partition = new JdbmPartition();
        partition.setSchemaManager( schemaManager );

        partition.setId( getString( ConfigSchemaConstants.ADS_PARTITION_ID, partitionEntry ) );
        partition.setPartitionDir( new File( workDir, partition.getId() ) );

        DN systemDn = new DN( getString( ConfigSchemaConstants.ADS_PARTITION_SUFFIX, partitionEntry ), schemaManager );
        partition.setSuffix( systemDn );

        EntryAttribute cacheAttr = partitionEntry.get( ConfigSchemaConstants.ADS_PARTITION_CACHE_SIZE );

        if ( cacheAttr != null )
        {
            partition.setCacheSize( Integer.parseInt( cacheAttr.getString() ) );
        }

        EntryAttribute optimizerAttr = partitionEntry.get( ConfigSchemaConstants.ADS_JDBM_PARTITION_OPTIMIZER_ENABLED );

        if ( optimizerAttr != null )
        {
            partition.setOptimizerEnabled( Boolean.parseBoolean( optimizerAttr.getString() ) );
        }

        EntryAttribute syncAttr = partitionEntry.get( ConfigSchemaConstants.ADS_PARTITION_SYNCONWRITE );

        if ( syncAttr != null )
        {
            partition.setSyncOnWrite( Boolean.parseBoolean( syncAttr.getString() ) );
        }

        Set<Index<?, Entry, Long>> indexedAttributes = getIndexes( partitionEntry.getDn() );
        partition.setIndexedAttributes( indexedAttributes );

        return partition;
    }


    private Set<Index<?, Entry, Long>> getIndexes( DN partitionDN ) throws Exception
    {
        AttributeType adsIndexAttributeIdAt = schemaManager.getAttributeType( ConfigSchemaConstants.ADS_INDEX_ATTRIBUTE_ID );
        PresenceNode filter = new PresenceNode( adsIndexAttributeIdAt );
        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.SUBTREE_SCOPE );
        IndexCursor<Long, Entry, Long> cursor = se.cursor( partitionDN, AliasDerefMode.NEVER_DEREF_ALIASES, filter,
            controls );

        Set<Index<?, Entry, Long>> indexes = new HashSet<Index<?, Entry, Long>>();

        while ( cursor.next() )
        {
            ForwardIndexEntry<Long, Entry, Long> forwardEntry = ( ForwardIndexEntry<Long, Entry, Long> ) cursor
                .get();
            Entry indexEntry = configPartition.lookup( forwardEntry.getId() );

            if ( !isEnabled( indexEntry ) )
            {
                continue;
            }

            EntryAttribute ocAttr = indexEntry.get( OBJECT_CLASS_AT );

            if ( ocAttr.contains( ConfigSchemaConstants.ADS_JDBMINDEX ) )
            {
                indexes.add( getJdbmIndex( indexEntry ) );
            }
            else
            {
                throw new NotImplementedException( I18n.err( I18n.ERR_506 ) );
            }
        }

        return indexes;
    }


    private JdbmIndex<?, Entry> getJdbmIndex( Entry indexEntry ) throws Exception
    {
        JdbmIndex<String, Entry> index = new JdbmIndex<String, Entry>();
        index.setAttributeId( getString( ConfigSchemaConstants.ADS_INDEX_ATTRIBUTE_ID, indexEntry ) );
        EntryAttribute cacheAttr = indexEntry.get( ConfigSchemaConstants.ADS_INDEX_CACHESIZE );

        if ( cacheAttr != null )
        {
            index.setCacheSize( Integer.parseInt( cacheAttr.getString() ) );
        }

        EntryAttribute numDupAttr = indexEntry.get( ConfigSchemaConstants.ADS_INDEX_NUM_DUP_LIMIT );

        if ( numDupAttr != null )
        {
            index.setNumDupLimit( Integer.parseInt( numDupAttr.getString() ) );
        }

        return index;
    }


    private Transport[] getTransports( DN adsServerDN ) throws Exception
    {
        AttributeType adsTransportIdAt = schemaManager.getAttributeType( ConfigSchemaConstants.ADS_TRANSPORT_ID );
        PresenceNode filter = new PresenceNode( adsTransportIdAt );
        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.SUBTREE_SCOPE );
        IndexCursor<Long, Entry, Long> cursor = se.cursor( adsServerDN, AliasDerefMode.NEVER_DEREF_ALIASES,
            filter, controls );

        List<Transport> transports = new ArrayList<Transport>();

        while ( cursor.next() )
        {
            ForwardIndexEntry<Long, Entry, Long> forwardEntry = ( ForwardIndexEntry<Long, Entry, Long> ) cursor
                .get();
            Entry transportEntry = configPartition.lookup( forwardEntry.getId() );

            if ( !isEnabled( transportEntry ) )
            {
                continue;
            }

            transports.add( getTransport( transportEntry ) );
        }

        return transports.toArray( new Transport[]
            {} );
    }


    //This will suppress PMD.AvoidUsingHardCodedIP warnings in this class
    @SuppressWarnings("PMD.AvoidUsingHardCodedIP")
    private Transport getTransport( Entry transportEntry ) throws Exception
    {
        Transport transport = null;

        EntryAttribute ocAttr = transportEntry.get( OBJECT_CLASS_AT );

        if ( ocAttr.contains( ConfigSchemaConstants.ADS_TCP_TRANSPORT ) )
        {
            transport = new TcpTransport();
        }
        else if ( ocAttr.contains( ConfigSchemaConstants.ADS_UDP_TRANSPORT ) )
        {
            transport = new UdpTransport();
        }

        transport.setPort( getInt( ConfigSchemaConstants.ADS_SYSTEM_PORT, transportEntry ) );
        EntryAttribute addressAttr = transportEntry.get( ConfigSchemaConstants.ADS_TRANSPORT_ADDRESS );

        if ( addressAttr != null )
        {
            transport.setAddress( addressAttr.getString() );
        }
        else
        {
            transport.setAddress( "0.0.0.0" );
        }

        EntryAttribute backlogAttr = transportEntry.get( ConfigSchemaConstants.ADS_TRANSPORT_BACKLOG );

        if ( backlogAttr != null )
        {
            transport.setBackLog( Integer.parseInt( backlogAttr.getString() ) );
        }

        EntryAttribute sslAttr = transportEntry.get( ConfigSchemaConstants.ADS_TRANSPORT_ENABLE_SSL );

        if ( sslAttr != null )
        {
            transport.setEnableSSL( Boolean.parseBoolean( sslAttr.getString() ) );
        }

        EntryAttribute nbThreadsAttr = transportEntry.get( ConfigSchemaConstants.ADS_TRANSPORT_NBTHREADS );

        if ( nbThreadsAttr != null )
        {
            transport.setNbThreads( Integer.parseInt( nbThreadsAttr.getString() ) );
        }

        return transport;
    }


    private ChangeLog getChangeLog( DN changelogDN ) throws Exception
    {
        long id = configPartition.getEntryId( changelogDN );
        Entry clEntry = configPartition.lookup( id );

        ChangeLog cl = new DefaultChangeLog();
        EntryAttribute clEnabledAttr = clEntry.get( ConfigSchemaConstants.ADS_CHANGELOG_ENABLED );

        if ( clEnabledAttr != null )
        {
            cl.setEnabled( Boolean.parseBoolean( clEnabledAttr.getString() ) );
        }

        EntryAttribute clExpAttr = clEntry.get( ConfigSchemaConstants.ADS_CHANGELOG_EXPOSED );

        if ( clExpAttr != null )
        {
            cl.setExposed( Boolean.parseBoolean( clExpAttr.getString() ) );
        }

        return cl;
    }


    private Journal getJournal( DN journalDN ) throws Exception
    {
        long id = configPartition.getEntryId( journalDN );
        Entry jlEntry = configPartition.lookup( id );

        Journal journal = new DefaultJournal();
        JournalStore store = new DefaultJournalStore();

        store.setFileName( getString( ConfigSchemaConstants.ADS_JOURNAL_FILENAME, jlEntry ) );

        EntryAttribute jlWorkDirAttr = jlEntry.get( ConfigSchemaConstants.ADS_JOURNAL_WORKINGDIR );

        if ( jlWorkDirAttr != null )
        {
            store.setWorkingDirectory( jlWorkDirAttr.getString() );
        }

        EntryAttribute jlRotAttr = jlEntry.get( ConfigSchemaConstants.ADS_JOURNAL_ROTATION );

        if ( jlRotAttr != null )
        {
            journal.setRotation( Integer.parseInt( jlRotAttr.getString() ) );
        }

        EntryAttribute jlEnabledAttr = jlEntry.get( ConfigSchemaConstants.ADS_JOURNAL_ENABLED );

        if ( jlEnabledAttr != null )
        {
            journal.setEnabled( Boolean.parseBoolean( jlEnabledAttr.getString() ) );
        }

        journal.setJournalStore( store );
        return journal;
    }


    private List<LdifEntry> getTestEntries( String entryFilePath ) throws Exception
    {
        List<LdifEntry> entries = new ArrayList<LdifEntry>();

        File file = new File( entryFilePath );

        if ( !file.exists() )
        {
            LOG.warn( "LDIF test entry file path doesn't exist {}", entryFilePath );
        }
        else
        {
            LOG.debug( "parsing the LDIF file(s) present at the path {}", entryFilePath );
            loadEntries( file, entries );
        }

        return entries;
    }


    private void loadEntries( File ldifFile, List<LdifEntry> entries ) throws Exception
    {
        if ( ldifFile.isDirectory() )
        {
            File[] files = ldifFile.listFiles( ldifFilter );

            for ( File f : files )
            {
                loadEntries( f, entries );
            }
        }
        else
        {
            LdifReader reader = new LdifReader();
            entries.addAll( reader.parseLdifFile( ldifFile.getAbsolutePath() ) );
            reader.close();
        }
    }


    private Set<WebApp> getWebApps( DN webAppsDN ) throws Exception
    {
        AttributeType adsHttpWarFileAt = schemaManager.getAttributeType( ConfigSchemaConstants.ADS_HTTP_WARFILE );
        PresenceNode filter = new PresenceNode( adsHttpWarFileAt );
        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.SUBTREE_SCOPE );
        IndexCursor<Long, Entry, Long> cursor = se.cursor( webAppsDN, AliasDerefMode.NEVER_DEREF_ALIASES, filter,
            controls );

        Set<WebApp> webApps = new HashSet<WebApp>();

        while ( cursor.next() )
        {
            ForwardIndexEntry<Long, Entry, Long> forwardEntry = ( ForwardIndexEntry<Long, Entry, Long> ) cursor
                .get();
            Entry webAppEntry = configPartition.lookup( forwardEntry.getId() );

            WebApp app = new WebApp();
            app.setWarFile( getString( ConfigSchemaConstants.ADS_HTTP_WARFILE, webAppEntry ) );

            EntryAttribute ctxPathAttr = webAppEntry.get( ConfigSchemaConstants.ADS_HTTP_APP_CTX_PATH );

            if ( ctxPathAttr != null )
            {
                app.setContextPath( ctxPathAttr.getString() );
            }

            webApps.add( app );
        }

        return webApps;
    }

    
    /**
     * Loads and instantiates a MechanismHandler from the configuration entry
     *
     * @param saslMechHandlerEntry the entry of OC type {@link ConfigSchemaConstants#ADS_LDAP_SERVER_SASL_MECH_HANDLER_OC}
     * @return an instance of the MechanismHandler type
     * @throws Exception
     */
    private MechanismHandler getSaslMechHandler( Entry saslMechHandlerEntry ) throws Exception
    {
        String mechClassName = saslMechHandlerEntry.get( ConfigSchemaConstants.ADS_LDAP_SERVER_SASL_MECH_CLASS_NAME ).getString();
        
        Class<?> mechClass = Class.forName( mechClassName );
        
        MechanismHandler handler = ( MechanismHandler ) mechClass.newInstance();
        
        if( mechClass == NtlmMechanismHandler.class )
        {
            EntryAttribute ntlmHandlerAttr = saslMechHandlerEntry.get( ConfigSchemaConstants.ADS_LDAP_SERVER_NTLM_MECH_PROVIDER );
            if( ntlmHandlerAttr != null )
            {
                NtlmMechanismHandler ntlmHandler = ( NtlmMechanismHandler ) handler;
                ntlmHandler.setNtlmProviderFqcn( ntlmHandlerAttr.getString() );
            }
        }
        
        return handler;
    }
    
    
    /**
     * creates the PassworddPolicyConfiguration object after reading the config entry containing pwdpolicy OC
     * under the directory service config DN.
     *
     * @param dirServiceDN the DN of the diretcory service configuration entry
     * @return the {@link PasswordPolicyConfiguration} object, null if the pwdpolicy entry is not present or disabled
     * @throws Exception
     */
    private PasswordPolicyConfiguration getPwdPolicyConfig( DN dirServiceDN ) throws Exception
    {
        AttributeType ocAt = schemaManager.lookupAttributeTypeRegistry( SchemaConstants.OBJECT_CLASS_AT );
        EqualityNode<String> filter = new EqualityNode<String>( ocAt, new StringValue( PWD_POLICY_OC ) );
        
        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.ONELEVEL_SCOPE );
        IndexCursor<Long, Entry, Long> cursor = se.cursor( dirServiceDN, AliasDerefMode.NEVER_DEREF_ALIASES, filter, controls );

        
        if ( ! cursor.next() )
        {
            return null;
        }

        ForwardIndexEntry<Long, Entry, Long> forwardEntry = ( ForwardIndexEntry<Long, Entry, Long> ) cursor.get();
        Entry entry = configPartition.lookup( forwardEntry.getId() );//pwdPolicyEntry

        if ( ! isEnabled( entry ) )
        {
            return null;
        }
        
        PasswordPolicyConfiguration policyConfig = new PasswordPolicyConfiguration();
        
        String pwdAttrVal = entry.get( PWD_ATTRIBUTE_AT ).getString();
        
        // check if this is a valid attribute name
        try
        {
            schemaManager.lookupAttributeTypeRegistry( pwdAttrVal );
            policyConfig.setPwdAttribute( pwdAttrVal );
        }
        catch( Exception e )
        {
            LOG.error( "invalid password attribute name '{}' set in password policy configuration", pwdAttrVal );
            throw e;
        }
        
        EntryAttribute pwdMinAgeAttr = entry.get( PWD_MIN_AGE_AT );
        if( pwdMinAgeAttr != null )
        {
            policyConfig.setPwdMinAge( getInt( pwdMinAgeAttr ) );
        }
        
        EntryAttribute pwdMaxAgeAttr = entry.get( PWD_MAX_AGE_AT );
        if( pwdMaxAgeAttr != null )
        {
            policyConfig.setPwdMaxAge( getInt( pwdMaxAgeAttr ) );
        }
        
        EntryAttribute pwdInHistoryAttr = entry.get( PWD_IN_HISTORY_AT );
        if( pwdInHistoryAttr != null )
        {
            policyConfig.setPwdInHistory( getInt( pwdInHistoryAttr ) );
        }
        
        EntryAttribute pwdCheckQualityAttr = entry.get( PWD_CHECK_QUALITY_AT );
        if( pwdCheckQualityAttr != null )
        {
            policyConfig.setPwdCheckQuality( getInt( pwdCheckQualityAttr ) );
        }
        
        EntryAttribute pwdMinLengthAttr = entry.get( PWD_MIN_LENGTH_AT );
        if( pwdMinLengthAttr != null )
        {
            policyConfig.setPwdMinLength( getInt( pwdMinLengthAttr ) );
        }
        
        EntryAttribute pwdMaxLengthAttr = entry.get( PWD_MAX_LENGTH_AT );
        if( pwdMaxLengthAttr != null )
        {
            policyConfig.setPwdMaxLength( getInt( pwdMaxLengthAttr ) );
        }
        
        EntryAttribute pwdExpireWarningAttr = entry.get( PWD_EXPIRE_WARNING_AT );
        if( pwdExpireWarningAttr != null )
        {
            policyConfig.setPwdExpireWarning( getInt( pwdExpireWarningAttr ) );
        }
        
        EntryAttribute pwdGraceAuthNLimitAttr = entry.get( PWD_GRACE_AUTHN_LIMIT_AT );
        if( pwdGraceAuthNLimitAttr != null )
        {
            policyConfig.setPwdGraceAuthNLimit( getInt( pwdGraceAuthNLimitAttr ) );
        }
        
        EntryAttribute pwdGraceExpireAttr = entry.get( PWD_GRACE_EXPIRE_AT );
        if( pwdGraceExpireAttr != null )
        {
            policyConfig.setPwdGraceExpire( getInt( pwdGraceExpireAttr ) );
        }
        
        EntryAttribute pwdLockoutAttr = entry.get( PWD_LOCKOUT_AT );
        if( pwdLockoutAttr != null )
        {
            policyConfig.setPwdLockout( Boolean.parseBoolean( pwdLockoutAttr.getString() ) );
        }
        
        EntryAttribute pwdLockoutDurationAttr = entry.get( PWD_LOCKOUT_DURATION_AT );
        if( pwdLockoutDurationAttr != null )
        {
            policyConfig.setPwdLockoutDuration( getInt( pwdLockoutDurationAttr ) );
        }
        
        EntryAttribute pwdMaxFailureAttr = entry.get( PWD_MAX_FAILURE_AT );
        if( pwdMaxFailureAttr != null )
        {
            policyConfig.setPwdMaxFailure( getInt( pwdMaxFailureAttr ) );
        }
        
        EntryAttribute pwdFailureCountIntervalAttr = entry.get( PWD_FAILURE_COUNT_INTERVAL_AT );
        if( pwdFailureCountIntervalAttr != null )
        {
            policyConfig.setPwdFailureCountInterval( getInt( pwdFailureCountIntervalAttr ) );
        }
        
        EntryAttribute pwdMustChangeAttr = entry.get( PWD_MUST_CHANGE_AT );
        if( pwdMustChangeAttr != null )
        {
            policyConfig.setPwdMustChange( Boolean.parseBoolean( pwdMustChangeAttr.getString() ) );
        }
        
        EntryAttribute pwdAllowUserChangeAttr = entry.get( PWD_ALLOW_USER_CHANGE_AT );
        if( pwdAllowUserChangeAttr != null )
        {
            policyConfig.setPwdAllowUserChange( Boolean.parseBoolean( pwdAllowUserChangeAttr.getString() ) );
        }
        
        EntryAttribute pwdSafeModifyAttr = entry.get( PWD_SAFE_MODIFY_AT );
        if( pwdSafeModifyAttr != null )
        {
            policyConfig.setPwdSafeModify( Boolean.parseBoolean( pwdSafeModifyAttr.getString() ) );
        }
        
        EntryAttribute pwdMinDelayAttr = entry.get( PWD_MIN_DELAY_AT );
        if( pwdMinDelayAttr != null )
        {
            policyConfig.setPwdMinDelay( getInt( pwdMinDelayAttr ) );
        }
        
        EntryAttribute pwdMaxDelayAttr = entry.get( PWD_MAX_DELAY_AT );
        if( pwdMaxDelayAttr != null )
        {
            policyConfig.setPwdMaxDelay( getInt( pwdMaxDelayAttr ) );
        }
        
        EntryAttribute pwdMaxIdleAttr = entry.get( PWD_MAX_IDLE_AT );
        if( pwdMaxIdleAttr != null )
        {
            policyConfig.setPwdMaxIdle( getInt( pwdMaxIdleAttr ) );
        }
        
        policyConfig.validate();
        
        return policyConfig;
    }
    
    
    /**
     * internal class used for holding the Interceptor classname and order configuration
     */
    private class InterceptorConfig implements Comparable<InterceptorConfig>
    {
        private String id;
        private String fqcn;
        private int order;


        public InterceptorConfig( String id, String fqcn, int order )
        {
            if ( order < 1 )
            {
                throw new IllegalArgumentException( I18n.err( I18n.ERR_507 ) );
            }

            this.id = id;
            this.fqcn = fqcn;
            this.order = order;
        }


        public int compareTo( InterceptorConfig o )
        {
            if ( order > o.order )
            {
                return 1;
            }
            else if ( order < o.order )
            {
                return -1;
            }

            return 0;
        }


        /**
         * @return the fqcn
         */
        public String getFqcn()
        {
            return fqcn;
        }

    }


    private String getString( String attrName, Entry entry ) throws Exception
    {
        return entry.get( attrName ).getString();
    }


    private int getInt( String attrName, Entry entry ) throws Exception
    {
        return Integer.parseInt( entry.get( attrName ).getString() );
    }


    private int getInt( EntryAttribute attr ) throws Exception
    {
        return Integer.parseInt( attr.getString() );
    }

    
    private boolean isEnabled( Entry entry ) throws Exception
    {
        EntryAttribute enabledAttr = entry.get( ConfigSchemaConstants.ADS_ENABLED );
        if ( enabledAttr != null )
        {
            return Boolean.parseBoolean( enabledAttr.getString() );
        }
        else
        {
            return true;
        }
    }
}
