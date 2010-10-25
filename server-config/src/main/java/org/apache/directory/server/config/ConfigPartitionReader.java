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


import java.io.File;
import java.io.FilenameFilter;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.naming.directory.SearchControls;

import org.apache.directory.server.config.beans.AdsBaseBean;
import org.apache.directory.server.config.beans.ChangeLogBean;
import org.apache.directory.server.config.beans.ConfigBean;
import org.apache.directory.server.config.beans.DnsServerBean;
import org.apache.directory.server.config.beans.InterceptorBean;
import org.apache.directory.server.config.beans.JdbmIndexBean;
import org.apache.directory.server.config.beans.JdbmPartitionBean;
import org.apache.directory.server.config.beans.JournalBean;
import org.apache.directory.server.config.beans.KdcServerBean;
import org.apache.directory.server.config.beans.NtpServerBean;
import org.apache.directory.server.config.beans.PartitionBean;
import org.apache.directory.server.config.beans.TcpTransportBean;
import org.apache.directory.server.config.beans.TransportBean;
import org.apache.directory.server.config.beans.UdpTransportBean;
import org.apache.directory.server.core.authn.PasswordPolicyConfiguration;
import org.apache.directory.server.core.changelog.ChangeLog;
import org.apache.directory.server.core.changelog.DefaultChangeLog;
import org.apache.directory.server.core.interceptor.Interceptor;
import org.apache.directory.server.core.journal.DefaultJournal;
import org.apache.directory.server.core.journal.DefaultJournalStore;
import org.apache.directory.server.core.journal.Journal;
import org.apache.directory.server.core.journal.JournalStore;
import org.apache.directory.server.core.partition.Partition;
import org.apache.directory.server.core.partition.impl.btree.jdbm.JdbmIndex;
import org.apache.directory.server.core.partition.impl.btree.jdbm.JdbmPartition;
import org.apache.directory.server.core.partition.ldif.AbstractLdifPartition;
import org.apache.directory.server.dhcp.service.DhcpService;
import org.apache.directory.server.dhcp.service.StoreBasedDhcpService;
import org.apache.directory.server.dhcp.store.DhcpStore;
import org.apache.directory.server.dhcp.store.SimpleDhcpStore;
import org.apache.directory.server.dns.DnsServer;
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.server.integration.http.WebApp;
import org.apache.directory.server.kerberos.kdc.KdcServer;
import org.apache.directory.server.ldap.handlers.bind.MechanismHandler;
import org.apache.directory.server.ldap.handlers.bind.ntlm.NtlmMechanismHandler;
import org.apache.directory.server.ntp.NtpServer;
import org.apache.directory.server.protocol.shared.transport.TcpTransport;
import org.apache.directory.server.protocol.shared.transport.Transport;
import org.apache.directory.server.protocol.shared.transport.UdpTransport;
import org.apache.directory.server.xdbm.ForwardIndexEntry;
import org.apache.directory.server.xdbm.Index;
import org.apache.directory.server.xdbm.IndexCursor;
import org.apache.directory.server.xdbm.search.SearchEngine;
import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.entry.Entry;
import org.apache.directory.shared.ldap.entry.EntryAttribute;
import org.apache.directory.shared.ldap.entry.StringValue;
import org.apache.directory.shared.ldap.entry.Value;
import org.apache.directory.shared.ldap.exception.LdapException;
import org.apache.directory.shared.ldap.exception.LdapInvalidDnException;
import org.apache.directory.shared.ldap.filter.EqualityNode;
import org.apache.directory.shared.ldap.filter.PresenceNode;
import org.apache.directory.shared.ldap.filter.SearchScope;
import org.apache.directory.shared.ldap.ldif.LdifEntry;
import org.apache.directory.shared.ldap.ldif.LdifReader;
import org.apache.directory.shared.ldap.message.AliasDerefMode;
import org.apache.directory.shared.ldap.name.DN;
import org.apache.directory.shared.ldap.schema.AttributeType;
import org.apache.directory.shared.ldap.schema.ObjectClass;
import org.apache.directory.shared.ldap.schema.SchemaManager;
import org.apache.directory.shared.ldap.util.StringTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A class used for reading the configuration present in a Partition
 * and instantiate the necessary objects like DirectoryService, Interceptors etc.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class ConfigPartitionReader
{
    /** The logger for this class */
    private static final Logger LOG = LoggerFactory.getLogger( ConfigPartitionReader.class );

    /** the partition which holds the configuration data */
    private AbstractLdifPartition configPartition;

    /** the search engine of the partition */
    private SearchEngine<Entry, Long> se;

    /** the schema manager set in the config partition */
    private SchemaManager schemaManager;

    /** A reference to the ObjectClass AT */
    private static AttributeType OBJECT_CLASS_AT;

    /** the parent directory of the config partition's working directory */
    private File workDir;
    
    /** The prefix for all the configuration ObjectClass names */
    private static final String ADS_PREFIX = "ads-";

    /** The suffix for the bean */
    private static final String ADS_SUFFIX = "Bean";

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
    
    /** Those two flags are used to tell the reader if an element of configuration is mandatory or not */
    private static final boolean MANDATORY = true;
    private static final boolean OPTIONNAL = false;


    /**
     * 
     * Creates a new instance of ConfigPartitionReader.
     *
     * @param configPartition the non null config partition
     * @param partitionsDir the directory where all the partitions' data is stored
     */
    public ConfigPartitionReader( AbstractLdifPartition configPartition, File partitionsDir )
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
        this.workDir = partitionsDir;
        
        // setup ObjectClass attribute type value
        OBJECT_CLASS_AT = schemaManager.getAttributeType( SchemaConstants.OBJECT_CLASS_AT );
    }


    /**
     * reads the LDAP server configuration and instantiates without setting a DirectoryService 
     *
     * @return the LdapServer instance without a DirectoryService
     * @throws Exception
     *
    public LdapServer createLdapServer() throws Exception
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

        Entry ldapServerEntry = configPartition.lookup( forwardEntry.getId() );
        LOG.debug( "LDAP Server Entry {}", ldapServerEntry );

        if ( !isEnabled( ldapServerEntry ) )
        {
            return null;
        }

        LdapServer server = new LdapServer();
        server.setServiceId( getString( ConfigSchemaConstants.ADS_SERVER_ID, ldapServerEntry ) );

        Transport[] transports = createTransports( ldapServerEntry.getDn() );
        server.setTransports( transports );

        EntryAttribute replEnableProvAttr = ldapServerEntry.get( ConfigSchemaConstants.ADS_REPL_ENABLE_PROVIDER );
        
        if( replEnableProvAttr != null )
        {
            if( Boolean.parseBoolean( replEnableProvAttr.getString() ) )
            {
                EntryAttribute replProvImplAttr = ldapServerEntry.get( ConfigSchemaConstants.ADS_REPL_PROVIDER_IMPL );
                
                String fqcn = null;
                
                if ( replProvImplAttr != null )
                {
                    fqcn = replProvImplAttr.getString();
                }
                else
                {
                    // default replication provider
                    fqcn = SyncReplProvider.class.getName();
                }
                
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
        }
        
        server.setReplProviderConfigs( createReplProviderConfigs() );
        
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
                server.addSaslMechanismHandler( mechanism, createSaslMechHandler( saslMechHandlerEntry ) );
            }
        }
        
        cursor.close();
        
        // read the extended operation handlers' config
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


    /**
     * Create an instance of KdcServer reading its configuration in the DIT
     * 
     * @return An instance of a KdcServer
     * @throws Exception If the instance cannot be created
     */
    public KdcServer createKdcServer( KdcServerBean kdcServerBean ) throws Exception
    {
        if ( kdcServerBean == null )
        {
            return null;
        }
        
        KdcServer kdcServer = new KdcServer();
        
        for ( TransportBean transportBean : kdcServerBean.getTransports() )
        {
            Transport transport = createTransport( transportBean );
            
            kdcServer.addTransports( transport );
        }
        
        kdcServer.setServiceId( kdcServerBean.getServerId() );
        kdcServer.setAllowableClockSkew( kdcServerBean.getKrbAllowableClockSkew() );
        kdcServer.setEncryptionTypes( kdcServerBean.getKrbEncryptionTypes() );
        kdcServer.setEmptyAddressesAllowed( kdcServerBean.isKrbEmptyAddressesAllowed() );
        kdcServer.setForwardableAllowed( kdcServerBean.isKrbForwardableAllowed() );
        kdcServer.setPaEncTimestampRequired( kdcServerBean.isKrbPaEncTimestampRequired() );
        kdcServer.setPostdatedAllowed( kdcServerBean.isKrbPostdatedAllowed() );
        kdcServer.setProxiableAllowed( kdcServerBean.isKrbProxiableAllowed() );
        kdcServer.setRenewableAllowed( kdcServerBean.isKrbRenewableAllowed() );
        kdcServer.setKdcPrincipal( kdcServerBean.getKrbKdcPrincipal().getName() );
        kdcServer.setMaximumRenewableLifetime( kdcServerBean.getKrbMaximumRenewableLifetime() );
        kdcServer.setMaximumTicketLifetime( kdcServerBean.getKrbMaximumTicketLifetime() );
        kdcServer.setPrimaryRealm( kdcServerBean.getKrbPrimaryRealm() );
        kdcServer.setBodyChecksumVerified( kdcServerBean.isKrbBodyChecksumVerified() );
        kdcServer.setSearchBaseDn( kdcServerBean.getSearchBaseDn() );
        
        return kdcServer;
    }

    
    /**
     * Create an instance of DnsServer reading its configuration in the DIT
     * 
     * @return An instance of a DnsServer
     * @throws Exception If the instance cannot be created
     */
    public DnsServer createDnsServer( DnsServerBean dnsServerBean ) throws Exception
    {
        if ( dnsServerBean == null )
        {
            return null;
        }
        
        DnsServer dnsServer = new DnsServer();
        
        for ( TransportBean transportBean : dnsServerBean.getTransports() )
        {
            Transport transport = createTransport( transportBean );
            
            dnsServer.addTransports( transport );
        }
        
        dnsServer.setServiceId( dnsServerBean.getServerId() );


        return dnsServer;
    }

    //TODO making this method invisible cause there is no DhcpServer exists as of now
    private DhcpService createDhcpServer() throws Exception
    {
        DhcpStore dhcpStore = new SimpleDhcpStore();
        DhcpService dhcpService = new StoreBasedDhcpService( dhcpStore );

        return dhcpService;
    }


    
    /**
     * Create the NtpServer instance from configuration in the DIT
     * 
     * @return An instance of NtpServer
     * @throws Exception If the configuration cannot be read
     */
    public NtpServer createNtpServer( NtpServerBean ntpServerBean ) throws Exception
    {
        if ( ntpServerBean == null )
        {
            return null;
        }
        
        NtpServer ntpServer = new NtpServer();
        
        for ( TransportBean transportBean : ntpServerBean.getTransports() )
        {
            Transport transport = createTransport( transportBean );
            
            ntpServer.addTransports( transport );
        }
        
        ntpServer.setServiceId( ntpServerBean.getServerId() );
        
        return ntpServer;
    }

    
    /*
    public ChangePasswordServer createChangePwdServer() throws Exception
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

        Transport[] transports = createTransports( chgPwdEntry.getDn() );
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

        EntryAttribute searchBaseAttr = chgPwdEntry.get( ConfigSchemaConstants.ADS_SEARCH_BASE );

        if ( searchBaseAttr != null )
        {
            chgPwdServer.setSearchBaseDn( searchBaseAttr.getString() );
        }
        
        return chgPwdServer;
    }

    
    public HttpServer createHttpServer() throws Exception
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

        Entry httpEntry = configPartition.lookup( forwardEntry.getId() );
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

        Set<WebApp> webApps = createWebApps( webAppsDN );
        httpServer.setWebApps( webApps );

        return httpServer;
    }
    */
    
    
    /**
     * Fnd the upper objectclass in a hierarchy. All the inherited ObjectClasses
     * will be removed.
     */
    private ObjectClass findObjectClass( EntryAttribute objectClass ) throws Exception
    {
        Set<ObjectClass> candidates = new HashSet<ObjectClass>();
        
        try
        {
            // Create the set of candidates
            for ( Value<?> ocValue : objectClass )
            {
                String ocName = ocValue.getString();
                String ocOid = schemaManager.getObjectClassRegistry().getOidByName( ocName );
                ObjectClass oc = (ObjectClass)schemaManager.getObjectClassRegistry().get( ocOid );
                
                if ( oc.isStructural() )
                {
                    candidates.add( oc );
                }
            }
        }
        catch ( Exception e )
        {
            e.printStackTrace();
            throw e;
        }
        
        // Now find the parent OC
        for ( Value<?> ocValue : objectClass )
        {
            String ocName = ocValue.getString();
            String ocOid = schemaManager.getObjectClassRegistry().getOidByName( ocName );
            ObjectClass oc = (ObjectClass)schemaManager.getObjectClassRegistry().get( ocOid );
            
            for ( ObjectClass superior : oc.getSuperiors() )
            {
                if ( oc.isStructural() )
                {
                    if ( candidates.contains( superior ) )
                    {
                        candidates.remove( superior );
                    }
                }
            }
        }
        
        // The remaining OC in the candidates set is the one we are looking for
        ObjectClass result = candidates.toArray( new ObjectClass[]{} )[0];
        
        LOG.debug( "The top level object class is {}", result.getName() );
        return result;
    }
    
    
    /** 
     * Create the base Bean from the ObjectClass name. 
     * The bean name is constructed using the OjectClass name, by
     * removing the ADS prefix, upper casing the first letter and adding "Bean" at the end.
     * 
     * For instance, ads-directoryService wil become DirectoryServiceBean
     */
    private AdsBaseBean createBean( ObjectClass objectClass ) throws ConfigurationException
    {
        // The remaining OC in the candidates set is the one we are looking for
        String objectClassName = objectClass.getName();

        // Now, let's instanciate the associated bean. Get rid of the 'ads-' in front of the name,
        // and uppercase the first letter. Finally add "Bean" at the end and add the package.
        //String beanName = this.getClass().getPackage().getName() + "org.apache.directory.server.config.beans." + Character.toUpperCase( objectClassName.charAt( 4 ) ) + objectClassName.substring( 5 ) + "Bean";
        String beanName = this.getClass().getPackage().getName() + ".beans." + 
            Character.toUpperCase( objectClassName.charAt( ADS_PREFIX.length() ) ) + 
            objectClassName.substring( ADS_PREFIX.length() + 1 ) + ADS_SUFFIX;
        
        try
        {
            Class<?> clazz = Class.forName( beanName );
            Constructor<?> constructor = clazz.getConstructor();
            AdsBaseBean bean = (AdsBaseBean)constructor.newInstance();
            
            LOG.debug( "Bean {} created for ObjectClass {}", beanName, objectClassName );
            
            return bean;
        } 
        catch ( ClassNotFoundException cnfe )
        {
            String message = "Cannot find a Bean class for the ObjectClass name " + objectClassName;
            LOG.error( message );
            throw new ConfigurationException( message );
        }
        catch ( SecurityException se )
        {
            String message = "Cannot access to the class " + beanName;
            LOG.error( message );
            throw new ConfigurationException( message );
        } 
        catch ( NoSuchMethodException nsme )
        {
            String message = "Cannot find a constructor for the class " + beanName;
            LOG.error( message );
            throw new ConfigurationException( message );
        }
        catch ( InvocationTargetException ite )
        {
            String message = "Cannot invoke the class " + beanName + ", " + ite.getMessage();
            LOG.error( message );
            throw new ConfigurationException( message );
        }
        catch ( IllegalAccessException iae )
        {
            String message = "Cannot access to the constructor for class " + beanName;
            LOG.error( message );
            throw new ConfigurationException( message );
        }
        catch ( InstantiationException ie )
        {
            String message = "Cannot instanciate the class " + beanName + ", " + ie.getMessage();
            LOG.error( message );
            throw new ConfigurationException( message );
        }
    }
    

    /**
     * Retrieve the Field associated with an AttributeType name, if any.
     */
    private static Field getField( Class<?> clazz, String attributeName ) throws ConfigurationException 
    {
        // We will check all the fields, as the AT name is case insentitive
        // when the field is case sensitive
        Field[] fields = clazz.getDeclaredFields();
        
        for ( Field field : fields )
        {
            String fieldName = field.getName();
            
            if ( fieldName.equalsIgnoreCase( attributeName ) )
            {
                return field;
            }
        }
        
        // May be in the paren'ts class ?
        if ( clazz.getSuperclass() != null )
        {
            return getField( clazz.getSuperclass(), attributeName );
        }
        
        String message = "Cannot find a field named " + attributeName + " in class " + clazz.getName();
        LOG.error( message );
        throw new ConfigurationException( message );
    }
    
    
    private static boolean isBaseAdsBeanChild( Class<?> clazz )
    {
        if ( clazz == null )
        {
            return false;
        }
        
        if ( clazz == AdsBaseBean.class )
        {
            return true;
        }
        else
        {
            return isBaseAdsBeanChild( clazz.getSuperclass() );
        }
    }
    
    
    /**
     * Read the single entry value for an AttributeType, and feed the Bean field with this value
     */
    private void readSingleValueField( AdsBaseBean bean, Field beanField, EntryAttribute fieldAttr, boolean mandatory ) throws ConfigurationException
    {
        if ( fieldAttr == null )
        {
            return;
        }
        
        Value<?> value = fieldAttr.get();
        String valueStr = value.getString();
        Class<?> type = beanField.getType();
        
        // Process the value accordingly to its type.
        try
        {
            if ( type == String.class )
            {
                beanField.set( bean, value.getString() );
            }
            else if ( type == int.class )
            {
                beanField.setInt( bean, Integer.parseInt( valueStr )  );
            }
            else if ( type == long.class )
            {
                beanField.setLong( bean, Long.parseLong( valueStr )  );
            }
            else if ( type == boolean.class )
            {
                beanField.setBoolean( bean, Boolean.parseBoolean( valueStr ) );
            }
            else if ( type == DN.class )
            {
                try
                {
                    DN dn = new DN( valueStr );
                    beanField.set( bean, dn );
                }
                catch ( LdapInvalidDnException lide )
                {
                    String message = "The DN '" + valueStr + "' for attribute " + fieldAttr.getId() + " is not a valid DN";
                    LOG.error( message );
                    throw new ConfigurationException( message );
                }
            }
        }
        catch ( IllegalArgumentException iae )
        {
            String message = "Cannot store '" + valueStr + "' into attribute " + fieldAttr.getId(); ;
            LOG.error( message );
            throw new ConfigurationException( message );
        }
        catch ( IllegalAccessException e )
        {
            String message = "Cannot store '" + valueStr + "' into attribute " + fieldAttr.getId(); ;
            LOG.error( message );
            throw new ConfigurationException( message );
        }
    }
    
    
    /**
     * Read the multiple entry value for an AttributeType, and feed the Bean field with this value
     */
    private void readMultiValuedField( AdsBaseBean bean, Field beanField, EntryAttribute fieldAttr, boolean mandatory ) throws ConfigurationException
    {
        if ( fieldAttr == null )
        {
            return;
        }

        Class<?> type = beanField.getType();
        
        // loop on the values and inject them in the bean
        for ( Value<?> value : fieldAttr )
        {
            String valueStr = value.getString();
            
            try
            {
                if ( type == String.class )
                {
                    beanField.set( bean, value.getString() );
                }
                else if ( type == int.class )
                {
                    beanField.setInt( bean, Integer.parseInt( valueStr )  );
                }
                else if ( type == long.class )
                {
                    beanField.setLong( bean, Long.parseLong( valueStr )  );
                }
                else if ( type == boolean.class )
                {
                    beanField.setBoolean( bean, Boolean.parseBoolean( valueStr ) );
                }
                else if ( type == DN.class )
                {
                    try
                    {
                        DN dn = new DN( valueStr );
                        beanField.set( bean, dn );
                    }
                    catch ( LdapInvalidDnException lide )
                    {
                        String message = "The DN '" + valueStr + "' for attribute " + fieldAttr.getId() + " is not a valid DN";
                        LOG.error( message );
                        throw new ConfigurationException( message );
                    }
                }
                else if ( type == Set.class )
                {
                    Type genericFieldType = beanField.getGenericType();
                    Class<?> fieldArgClass = null;
                        
                    if ( genericFieldType instanceof ParameterizedType ) 
                    {
                        ParameterizedType parameterizedType = (ParameterizedType) genericFieldType;
                        Type[] fieldArgTypes = parameterizedType.getActualTypeArguments();
                        
                        for ( Type fieldArgType : fieldArgTypes )
                        {
                            fieldArgClass = (Class<?>) fieldArgType;
                        }
                    }
    
                    Method method = bean.getClass().getMethod( "add" + beanField.getName(), Array.newInstance( fieldArgClass, 0 ).getClass() );
    
                    method.invoke( bean, new Object[]{ new String[]{valueStr} } );
                }
                else if ( type == List.class )
                {
                    Type genericFieldType = beanField.getGenericType();
                    Class<?> fieldArgClass = null;
                        
                    if ( genericFieldType instanceof ParameterizedType ) 
                    {
                        ParameterizedType parameterizedType = (ParameterizedType) genericFieldType;
                        Type[] fieldArgTypes = parameterizedType.getActualTypeArguments();
                        
                        for ( Type fieldArgType : fieldArgTypes )
                        {
                            fieldArgClass = (Class<?>) fieldArgType;
                        }
                    }
    
                    Method method = bean.getClass().getMethod( "add" + beanField.getName(), Array.newInstance( fieldArgClass, 0 ).getClass() );
    
                    method.invoke( bean, new Object[]{ new String[]{valueStr} } );
                }
            }
            catch ( IllegalArgumentException iae )
            {
                String message = "Cannot store '" + valueStr + "' into attribute " + fieldAttr.getId(); ;
                LOG.error( message );
                throw new ConfigurationException( message );
            }
            catch ( IllegalAccessException e )
            {
                String message = "Cannot store '" + valueStr + "' into attribute " + fieldAttr.getId(); ;
                LOG.error( message );
                throw new ConfigurationException( message );
            }
            catch ( SecurityException se )
            {
                String message = "Cannot access to the class " + bean.getClass().getName();
                LOG.error( message );
                throw new ConfigurationException( message );
            } 
            catch ( NoSuchMethodException nsme )
            {
                String message = "Cannot find a constructor for the class " + bean.getClass().getName();
                LOG.error( message );
                throw new ConfigurationException( message );
            }
            catch ( InvocationTargetException ite )
            {
                String message = "Cannot invoke the class " + bean.getClass().getName() + ", " + ite.getMessage();
                LOG.error( message );
                throw new ConfigurationException( message );
            }
            catch ( NegativeArraySizeException nase )
            {
                // No way that can happen...
            }
        }
    }

    
    /**
     * Read all the required fields (AttributeTypes) for a given Entry.
     */
    private void readFields( AdsBaseBean bean, Entry entry, Set<AttributeType> attributeTypes, boolean mandatory ) throws NoSuchFieldException, IllegalAccessException, Exception
    {
        for ( AttributeType attributeType : attributeTypes )
        {
            String fieldName = attributeType.getName();
            String beanFieldName = fieldName;
            
            // Remove the "ads-" from the beginning of the field name
            if ( fieldName.startsWith( ADS_PREFIX ) )
            {
                beanFieldName = fieldName.substring( ADS_PREFIX.length() );
            }
            
            // Get the field
            Field beanField = getField( bean.getClass(), StringTools.toLowerCase( beanFieldName ) );
            
            // The field is private, we need to modify it to be able to access it.
            beanField.setAccessible( true );
            
            // Get the entry attribute for this field
            EntryAttribute fieldAttr = entry.get( fieldName );
            
            if ( ( fieldAttr == null ) && ( mandatory ) )
            {
                String message = "Attribute " + fieldName + " is mandatory and is not present for the Entry " + entry.getDn();
                LOG.error( message );
                throw new ConfigurationException( message );
            }
            
            // Get the associated AttributeType
            AttributeType beanAT = schemaManager.getAttributeType( fieldName );
            
            // Check if this AT has the ads-compositeElement as a superior
            AttributeType superior = beanAT.getSuperior();
            
            if ( ( superior != null ) && superior.getOid().equals( ConfigSchemaConstants.ADS_COMPOSITE_ELEMENT_AT.getOid() ) )
            {
                // This is a composite element, we have to go one level down to read it.
                // First, check if it's a SingleValued element
                if ( beanAT.isSingleValued() )
                {
                    // Yes : get the first element
                    List<AdsBaseBean> beans = read( entry.getDn(), fieldName, SearchScope.ONELEVEL, mandatory );

                    // We may not have found an element, but if the attribute is mandatory,
                    // this is an error
                    if ( ( beans == null ) || ( beans.size() == 0 ) )
                    {
                        if ( mandatory )
                        {
                            // This is an error !
                            String message = "The composite " + beanAT.getName() + " is mandatory, and was not found under the "
                                + "configuration entry " + entry.getDn();
                            LOG.error( message );
                            throw new ConfigurationException( message );
                        }
                    }
                    else
                    { 
                        // We must take the first element
                        AdsBaseBean readBean = beans.get( 0 );
                        
                        if ( beans.size() > 1 )
                        {
                            // Not allowed as the AT is singled-valued
                            String message = "We have more than one entry for " + beanAT.getName() + " under " + entry.getDn();
                            LOG.error( message );
                            throw new ConfigurationException( message );
                        }
                        
                        beanField.set( bean, readBean );
                    }
                }
                else
                {
                    // No : we have to loop recursively on all the elements which are
                    // under the ou=<element-name> branch
                    DN newBase = entry.getDn().add( "ou=" + beanFieldName );
                    
                    // We have to remove the 's' at the end of the field name
                    String attributeName = fieldName.substring( 0, fieldName.length() - 1 );
                    
                    // Sometime, the plural of a noun takes 'es'
                    if ( !schemaManager.getObjectClassRegistry().contains( attributeName ) )
                    {
                        // Try by removing 'es'
                        attributeName = fieldName.substring( 0, fieldName.length() - 2 );
                        
                        if ( !schemaManager.getObjectClassRegistry().contains( attributeName ) )
                        {
                            String message = "Cannot find the ObjectClass named " + attributeName + " in the schema";
                            LOG.error(  message  );
                            throw new ConfigurationException( message );
                        }
                    }
                    
                    // This is a multi-valued element, it can be a Set or a List
                    Collection<AdsBaseBean> beans = read( newBase, attributeName, SearchScope.ONELEVEL, mandatory );
                    
                    if ( ( beans == null ) || ( beans.size() == 0 ) )
                    {
                        // If the element is mandatory, this is an error
                        if ( mandatory )
                        {
                            String message = "The composite " + beanAT.getName() + " is mandatory, and was not found under the "
                                + "configuration entry " + entry.getDn();
                            LOG.error( message );
                            throw new ConfigurationException( message );
                        }
                    }
                    else
                    {
                        // Update the field
                        beanField.set( bean, beans );
                    }
                }
            }
            else // A standard AttributeType (ie, boolean, long, int or String)
            {
                // Process the field accordingly to its cardinality
                if ( beanAT.isSingleValued() )
                {
                    readSingleValueField( bean, beanField, fieldAttr, mandatory );
                }
                else
                {
                    readMultiValuedField( bean, beanField, fieldAttr, mandatory );
                }
            }
        }
    }
    
    
    private Set<AttributeType> getAllMusts( ObjectClass objectClass )
    {
        Set<AttributeType> musts = new HashSet<AttributeType>();
        
        // First, gets the direct MUST
        musts.addAll( objectClass.getMustAttributeTypes() );
        
        // then add all the superiors MUST (recursively)
        List<ObjectClass> superiors = objectClass.getSuperiors();
        
        if ( superiors != null )
        {
            for ( ObjectClass superior : superiors )
            {
                musts.addAll( getAllMusts( superior) );
            }
        }
        
        return musts;
    }
    
    
    private Set<AttributeType> getAllMays( ObjectClass objectClass )
    {
        Set<AttributeType> mays = new HashSet<AttributeType>();
        
        // First, gets the direct MAY
        mays.addAll( objectClass.getMayAttributeTypes() );
        
        // then add all the superiors MAY (recursively)
        List<ObjectClass> superiors = objectClass.getSuperiors();
        
        if ( superiors != null )
        {
            for ( ObjectClass superior : superiors )
            {
                mays.addAll( getAllMays( superior) );
            }
        }
        
        return mays;
    }
    
    
    /**
     * Helper method to print a list of AT's names.
     */
    private String dumpATs( Set<AttributeType> attributeTypes )
    {
        if ( ( attributeTypes == null ) || ( attributeTypes.size() == 0 ) )
        {
            return "";
        }
        
        StringBuilder sb = new StringBuilder();
        boolean isFirst = true;
        sb.append( '{' );
        
        for ( AttributeType attributeType : attributeTypes )
        {
            if ( isFirst )
            {
                isFirst = false;
            }
            else
            {
                sb.append( ", " );
            }
            
            sb.append( attributeType.getName() );
        }

        sb.append( '}' );

        
        return sb.toString();
    }

    /**
     * Read some configuration element from the DIT using its name 
     */
    private List<AdsBaseBean> read( DN baseDn, String name, SearchScope scope, boolean mandatory ) throws ConfigurationException
    {
        LOG.debug( "Reading from '{}', entry {}", baseDn, name );
        
        // Search for the element starting at some point in the DIT
        // Prepare the search request
        AttributeType adsdAt = schemaManager.getAttributeType( SchemaConstants.OBJECT_CLASS_AT );
        EqualityNode<?> filter = new EqualityNode( adsdAt, new StringValue( name ) );
        SearchControls controls = new SearchControls();
        controls.setSearchScope( scope.ordinal() );
        IndexCursor<Long, Entry, Long> cursor = null;
        
        // Create a container for all the read beans
        List<AdsBaseBean> beans = new ArrayList<AdsBaseBean>();

        try
        {
            // Do the search
            cursor = se.cursor( baseDn, AliasDerefMode.NEVER_DEREF_ALIASES, filter, controls );
    
            // First, check if we have some entries to process.
            if ( !cursor.next() )
            {
                if ( mandatory )
                {
                    cursor.close();
                    
                    // the requested element is mandatory so let's throw an exception
                    String message = "No directoryService instance was configured under the DN "
                        + configPartition.getSuffix();
                    LOG.error( message );
                    throw new ConfigurationException( message );
                }
                else
                {
                    return null;
                }
            }

            // Loop on all the found elements
            do
            {
                ForwardIndexEntry<Long, Entry, Long> forwardEntry = ( ForwardIndexEntry<Long, Entry, Long> ) cursor
                .get();

                // Now, get the entry
                Entry entry = configPartition.lookup( forwardEntry.getId() );
                LOG.debug( "Entry read : {}", entry );
                
                // Let's instanciate the bean we need. The upper ObjectClass's name
                // will be used to do that
                EntryAttribute objectClassAttr = entry.get( SchemaConstants.OBJECT_CLASS_AT );
                
                ObjectClass objectClass = findObjectClass( objectClassAttr );
                AdsBaseBean bean = createBean( objectClass );
                
                // Now, read the AttributeTypes and store the values into the bean fields
                // The MAY
                Set<AttributeType> mays = getAllMays( objectClass );
                LOG.debug( "Fetching the following MAY attributes : {}", dumpATs( mays ) );
                readFields( bean, entry, mays, OPTIONNAL );
                
                // The MUST
                Set<AttributeType> musts = getAllMusts( objectClass );
                LOG.debug( "Fetching the following MAY attributes : {}", dumpATs( musts ) );
                readFields( bean, entry, musts, MANDATORY );
                
                // Done, we can add the bean into the list
                beans.add( bean );
            }
            while ( cursor.next() );
        }
        catch ( ConfigurationException ce )
        {
            ce.printStackTrace();
            throw ce;
        }
        catch ( Exception e )
        {
            String message = "Cannot open a cursor to read the configuration on " + baseDn;
            LOG.error( message );
            throw new ConfigurationException( message );
        }
        finally
        {
            if ( cursor != null )
            {
                try
                {
                    cursor.close();
                }
                catch ( Exception e )
                {
                    // So ??? If the cursor can't be close, there is nothing we can do
                    // but rethrow the exception
                    throw new ConfigurationException( e.getMessage(), e.getCause() );
                }
            }
        }
        
        return beans;

        // Get the elements : we might have more than one
        /*
        LOG.debug( "directory service entry {}", dsEntry );

        DirectoryServiceBean dirServicebean = new DirectoryServiceBean();
        
        // MUST attributes
        dirServicebean.setInstanceId( getString( ConfigSchemaConstants.ADS_DIRECTORYSERVICE_ID, dsEntry ) );
        dirServicebean.setReplicaId( getInt( ConfigSchemaConstants.ADS_DS_REPLICA_ID, dsEntry ) );

        Set<InterceptorBean> interceptors = readInterceptors( dsEntry.getDn() );
        dirServicebean.setInterceptors( interceptors );
        
        AuthenticationInterceptor authnInterceptor = ( AuthenticationInterceptor ) dirServicebean.getInterceptor( AuthenticationInterceptor.class.getName() );
        authnInterceptor.setPwdPolicyConfig( createPwdPolicyConfig( dsEntry.getDn() ) );

        Map<String, Partition> partitions = createPartitions( dsEntry.getDn() );

        Partition systemPartition = partitions.remove( "system" );

        if ( systemPartition == null )
        {
            throw new Exception( I18n.err( I18n.ERR_505 ) );
        }

        dirServicebean.setSystemPartition( systemPartition );
        dirServicebean.setPartitions( new HashSet<Partition>( partitions.values() ) );

        // MAY attributes
        EntryAttribute acEnabledAttr = dsEntry.get( ConfigSchemaConstants.ADS_DS_ACCESSCONTROL_ENABLED );

        if ( acEnabledAttr != null )
        {
            dirServicebean.setAccessControlEnabled( Boolean.parseBoolean( acEnabledAttr.getString() ) );
        }

        EntryAttribute anonAccessAttr = dsEntry.get( ConfigSchemaConstants.ADS_DS_ALLOW_ANONYMOUS_ACCESS );

        if ( anonAccessAttr != null )
        {
            dirServicebean.setAllowAnonymousAccess( Boolean.parseBoolean( anonAccessAttr.getString() ) );
        }

        EntryAttribute changeLogAttr = dsEntry.get( ConfigSchemaConstants.ADS_DSCHANGELOG );

        if ( changeLogAttr != null )
        {
            DN clDN = new DN( changeLogAttr.getString(), schemaManager );
            ChangeLog cl = createChangeLog( clDN );
            dirServicebean.setChangeLog( cl );
        }

        EntryAttribute denormAttr = dsEntry.get( ConfigSchemaConstants.ADS_DS_DENORMALIZE_OPATTRS_ENABLED );

        if ( denormAttr != null )
        {
            dirServicebean.setDenormalizeOpAttrsEnabled( Boolean.parseBoolean( denormAttr.getString() ) );
        }

        EntryAttribute journalAttr = dsEntry.get( ConfigSchemaConstants.ADS_DSJOURNAL );

        if ( journalAttr != null )
        {
            DN journalDN = new DN( journalAttr.getString(), schemaManager );
            dirServicebean.setJournal( createJournal( journalDN ) );
        }

        EntryAttribute maxPduAttr = dsEntry.get( ConfigSchemaConstants.ADS_DS_MAXPDU_SIZE );

        if ( maxPduAttr != null )
        {
            dirServicebean.setMaxPDUSize( Integer.parseInt( maxPduAttr.getString() ) );
        }

        EntryAttribute passwordHidAttr = dsEntry.get( ConfigSchemaConstants.ADS_DS_PASSWORD_HIDDEN );

        if ( passwordHidAttr != null )
        {
            dirServicebean.setPasswordHidden( Boolean.parseBoolean( passwordHidAttr.getString() ) );
        }

        EntryAttribute replAttr = dsEntry.get( ConfigSchemaConstants.ADS_DS_REPLICATION );

        if ( replAttr != null )
        {
            // configure replication
        }

        EntryAttribute syncPeriodAttr = dsEntry.get( ConfigSchemaConstants.ADS_DS_SYNCPERIOD_MILLIS );

        if ( syncPeriodAttr != null )
        {
            dirServicebean.setSyncPeriodMillis( Long.parseLong( syncPeriodAttr.getString() ) );
        }

        EntryAttribute testEntryAttr = dsEntry.get( ConfigSchemaConstants.ADS_DS_TEST_ENTRIES );

        if ( testEntryAttr != null )
        {
            String entryFilePath = testEntryAttr.getString();
            dirServicebean.setTestEntries( readTestEntries( entryFilePath ) );
        }

        if ( !isEnabled( dsEntry ) )
        {
            // will only be useful if we ever allow more than one DS to be configured and
            // switch between them
            // decide which one to use based on this flag
        }

        return dirServicebean;
        */
    }
    
    
    /**
     * Read the configuration from the DIT, returning a bean containing all of it.
     * 
     * @param base The base DN in the DIT where the configuration is stored
     * @return The Config bean, containing the whole configuration
     * @throws ConfigurationException If we had some issue reading the configuration
     */
    public ConfigBean readConfig( String baseDn ) throws LdapException
    {
        // The starting point is the DirectoryService element
        return readConfig( new DN( baseDn ), ConfigSchemaConstants.ADS_DIRECTORY_SERVICE_OC.getValue() );
    }
    
    
    /**
     * Read the configuration from the DIT, returning a bean containing all of it.
     * 
     * @param base The base DN in the DIT where the configuration is stored
     * @return The Config bean, containing the whole configuration
     * @throws ConfigurationException If we had some issue reading the configuration
     */
    public ConfigBean readConfig( DN baseDn ) throws ConfigurationException
    {
        // The starting point is the DirectoryService element
        return readConfig( baseDn, ConfigSchemaConstants.ADS_DIRECTORY_SERVICE_OC.getValue() );
    }
    
    
    /**
     * Read the configuration from the DIT, returning a bean containing all of it.
     * 
     * @param baseDn The base DN in the DIT where the configuration is stored
     * @param objectClass The element to read from the DIT
     * @return The bean containing the configuration for the required element
     * @throws ConfigurationException
     */
    public ConfigBean readConfig( String baseDn, String objectClass ) throws LdapException
    {
        return readConfig( new DN( baseDn ), objectClass );
    }
    
    
    /**
     * Read the configuration from the DIT, returning a bean containing all of it.
     * 
     * @param baseDn The base DN in the DIT where the configuration is stored
     * @param objectClass The element to read from the DIT
     * @return The bean containing the configuration for the required element
     * @throws ConfigurationException
     */
    public ConfigBean readConfig( DN baseDn, String objectClass ) throws ConfigurationException
    {
        LOG.debug( "Reading configuration for the {} element, from {} ", objectClass, baseDn );
        ConfigBean configBean = new ConfigBean();
        
        if ( baseDn == null )
        {
            baseDn = configPartition.getSuffix();
        }
        
        List<AdsBaseBean> beans = read( baseDn, objectClass, SearchScope.ONELEVEL, MANDATORY );
        
        if ( LOG.isDebugEnabled() )
        {
            if ( ( beans == null ) || ( beans.size() == 0 ) )
            {
                LOG.debug( "No {} element to read", objectClass );
            }
            else
            {
                LOG.debug( beans.get( 0 ).toString() );
            }
        }
        
        configBean.setDirectoryServiceBeans( beans );
        
        System.out.println( configBean );
        
        return configBean;
    }
    
    
    /**
     * instantiates a DirectoryService based on the configuration present in the partition 
     *
     * @throws Exception
     *
    public DirectoryService createDirectoryService() throws Exception
    {
        DirectoryServiceBean directoryServiceBean = (DirectoryServiceBean)read( configPartition.getSuffix(), ConfigSchemaConstants.ADS_DIRECTORYSERVICE_ID, SearchScope.SUBTREE, MANDATORY );
        
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

        List<Interceptor> interceptors = createInterceptors( dsEntry.getDn() );
        dirService.setInterceptors( interceptors );
        
        AuthenticationInterceptor authnInterceptor = ( AuthenticationInterceptor ) dirService.getInterceptor( AuthenticationInterceptor.class.getName() );
        authnInterceptor.setPwdPolicyConfig( createPwdPolicyConfig( dsEntry.getDn() ) );

        Map<String, Partition> partitions = createPartitions( dsEntry.getDn() );

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
            ChangeLog cl = createChangeLog( clDN );
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
            dirService.setJournal( createJournal( journalDN ) );
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

        //EntryAttribute replAttr = dsEntry.get( ConfigSchemaConstants.ADS_DS_REPLICATION );

        //if ( replAttr != null )
        //{
            // configure replication
        //}

        EntryAttribute syncPeriodAttr = dsEntry.get( ConfigSchemaConstants.ADS_DS_SYNCPERIOD_MILLIS );

        if ( syncPeriodAttr != null )
        {
            dirService.setSyncPeriodMillis( Long.parseLong( syncPeriodAttr.getString() ) );
        }

        EntryAttribute testEntryAttr = dsEntry.get( ConfigSchemaConstants.ADS_DS_TEST_ENTRIES );

        if ( testEntryAttr != null )
        {
            String entryFilePath = testEntryAttr.getString();
            dirService.setTestEntries( readTestEntries( entryFilePath ) );
        }

        if ( !isEnabled( dsEntry ) )
        {
            // will only be useful if we ever allow more than one DS to be configured and
            // switch between them
            // decide which one to use based on this flag
        }

        return dirService;
    }


    private List<SyncreplConfiguration> createReplProviderConfigs() throws Exception
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
            
            EntryAttribute replUseTls = entry.get( ConfigSchemaConstants.ADS_REPL_USE_TLS );
            if( replUseTls != null )
            {
                config.setUseTls( Boolean.parseBoolean( replUseTls.getString() ) );
            }
            
            EntryAttribute replPeerCertificate = entry.get( ConfigSchemaConstants.ADS_REPL_PEER_CERTIFICATE );
            if( replPeerCertificate != null )
            {
                // directly add to the ReplicationTrustManager instead of storing it in the config
                ReplicationTrustManager.addCertificate( String.valueOf( config.getReplicaId() ), replPeerCertificate.getBytes() );
            }
            else
            {
                config.setStrictCertVerification( false );
            }
            
            syncReplConfigLst.add( config );
        }
        while( cursor.next() );
        
        cursor.close();
        
        return syncReplConfigLst;
    }
    
    
    /**
     * Creates the Interceptor instances from the configuration
     *
     * @param dirServiceDN the DN under which interceptors are configured
     * @return a list of instantiated Interceptor objects
     * @throws Exception If the instanciation failed
     */
    private List<Interceptor> createInterceptors( Set<InterceptorBean> interceptorBeans ) throws Exception
    {
        List<Interceptor> interceptors = new ArrayList<Interceptor>( interceptorBeans.size() );
        
        for ( InterceptorBean interceptorBean : interceptorBeans )
        {
            try
            {
                LOG.debug( "loading the interceptor class {} and instantiating", interceptorBean.getInterceptorClassName() );
                Interceptor ic = ( Interceptor ) Class.forName( interceptorBean.getInterceptorClassName() ).newInstance();
                interceptors.add( ic );
            }
            catch ( Exception e )
            {
                throw e;
            }
        }
        
        return interceptors;
    }
    
    
    /**
     * Create the set of Partitions instanciated from the configuration
     * 
     * @param dirServiceDN the DN under which Partitions are configured
     * @return A Map of all the instanciated partitions
     * @throws Exception If we cannot process some Partition
     */
    public Map<String, Partition> createPartitions( Map<String, PartitionBean> partitionBeans ) throws Exception
    {
        Map<String, Partition> partitions = new HashMap<String, Partition>( partitionBeans.size() );
        
        for ( String key : partitionBeans.keySet() )
        {
            PartitionBean partitionBean = partitionBeans.get( key );
            
            JdbmPartition partition = createJdbmPartition( (JdbmPartitionBean)partitionBean );
            partitions.put( key, partition );
        }
        
        return partitions;
    }
    
    
    /**
     * Create a new instance of a JdbmPartition from an instance of JdbmIndexBean
     * 
     * @param partitionEntry The entry containing the JdbmPartition configuration
     * @return An JdbmPartition instance
     * @throws Exception If the instance cannot be created
     */
    public JdbmPartition createJdbmPartition( Entry partitionEntry ) throws Exception
    {
        JdbmPartition partition = new JdbmPartition();
        JdbmPartitionBean jdbmPartitionBean = readJdbmPartition( partitionEntry );
        
        partition.setSchemaManager( schemaManager );
        partition.setCacheSize( jdbmPartitionBean.getPartitionCacheSize() );
        partition.setId( jdbmPartitionBean.getPartitionId() );
        partition.setOptimizerEnabled( jdbmPartitionBean.isJdbmPartitionOptimizerEnabled() );
        partition.setPartitionDir( new File( workDir + File.separator + jdbmPartitionBean.getPartitionId() ) );
        partition.setSuffix( jdbmPartitionBean.getPartitionSuffix() );
        partition.setSyncOnWrite( jdbmPartitionBean.isPartitionSyncOnWrite() );
        partition.setIndexedAttributes( createIndexes( jdbmPartitionBean.getJdbmIndexes() ) );

        return partition;
    }

    
    /**
     * Create a new instance of a JdbmPartition from an instance of JdbmIndexBean
     * 
     * @param partitionEntry The entry containing the JdbmPartition configuration
     * @return An JdbmPartition instance
     * @throws Exception If the instance cannot be created
     */
    private JdbmPartition createJdbmPartition( JdbmPartitionBean jdbmPartitionBean ) throws Exception
    {
        JdbmPartition partition = new JdbmPartition();
        
        partition.setSchemaManager( schemaManager );
        partition.setCacheSize( jdbmPartitionBean.getPartitionCacheSize() );
        partition.setId( jdbmPartitionBean.getPartitionId() );
        partition.setOptimizerEnabled( jdbmPartitionBean.isJdbmPartitionOptimizerEnabled() );
        partition.setPartitionDir( new File( workDir + File.separator + jdbmPartitionBean.getPartitionId() ) );
        partition.setSuffix( jdbmPartitionBean.getPartitionSuffix() );
        partition.setSyncOnWrite( jdbmPartitionBean.isPartitionSyncOnWrite() );
        partition.setIndexedAttributes( createIndexes( jdbmPartitionBean.getJdbmIndexes() ) );

        return partition;
    }
    

    private Set<Index<?, Entry, Long>> createIndexes( DN partitionDN ) throws Exception
    {
        Set<JdbmIndexBean<String, Entry>> indexesBean = readIndexes( partitionDN );
        
        Set<Index<?, Entry, Long>> indexes = new HashSet<Index<?, Entry, Long>>();

        for ( JdbmIndexBean<String, Entry> indexBean : indexesBean )
        {
            indexes.add( createJdbmIndex( indexBean ) );
        }

        return indexes;
    }


    private Set<Index<?, Entry, Long>> createIndexes( Set<JdbmIndexBean<String, Entry>> indexesBean ) throws Exception
    {
        Set<Index<?, Entry, Long>> indexes = new HashSet<Index<?, Entry, Long>>();

        for ( JdbmIndexBean<String, Entry> indexBean : indexesBean )
        {
            indexes.add( createJdbmIndex( indexBean ) );
        }

        return indexes;
    }


    /**
     * Create a new instance of a JdbmIndex from the configuration read from the DIT
     * 
     * @param indexEntry The entry containing the JdbmIndex configuration
     * @return An JdbmIndex instance
     * @throws Exception If the instance cannot be created
     */
    public JdbmIndex<?, Entry> createJdbmIndex( Entry indexEntry ) throws Exception
    {
        JdbmIndex<String, Entry> index = new JdbmIndex<String, Entry>();
        JdbmIndexBean<String, Entry> indexBean = readJdbmIndex( indexEntry );
        
        index.setAttributeId( indexBean.getIndexAttributeId() );
        index.setCacheSize( indexBean.getIndexCacheSize() );
        index.setNumDupLimit( indexBean.getIndexNumDupLimit() );
        
        return index;
    }


    /**
     * Create a new instance of a JdbmIndex from an instance of JdbmIndexBean
     * 
     * @param JdbmIndexBean The JdbmIndexBean to convert
     * @return An JdbmIndex instance
     * @throws Exception If the instance cannot be created
     */
    private JdbmIndex<?, Entry> createJdbmIndex( JdbmIndexBean<String, Entry> indexBean ) throws Exception
    {
        JdbmIndex<String, Entry> index = new JdbmIndex<String, Entry>();
        
        index.setAttributeId( indexBean.getIndexAttributeId() );
        index.setCacheSize( indexBean.getIndexCacheSize() );
        index.setNumDupLimit( indexBean.getIndexNumDupLimit() );
        
        return index;
    }

    
    /**
     * Creates the array of transports read from the DIT 
     */
    private Transport[] createTransports(TransportBean[] transportBeans ) throws Exception
    {
        Transport[] transports = new Transport[ transportBeans.length ];
        int i = 0;
        
        for ( TransportBean transportBean : transportBeans )
        {
            transports[i++] = createTransport( transportBean );
        }
        
        return transports;
    }
    
    
    /**
     * Creates a Transport reading its configuration from the DIT
     * 
     * @param transportBean The created instance of transport
     * @return An instance of transport
     * @throws Exception If the instance cannot be read 
     */
    public Transport createTransport( TransportBean transportBean ) throws Exception
    {
        Transport transport = null;

        if ( transportBean instanceof TcpTransportBean )
        {
            transport = new TcpTransport();
        }
        else if ( transportBean instanceof UdpTransportBean )
        {
            transport = new UdpTransport();
        }

        transport.setPort( transportBean.getSystemPort() );
        transport.setAddress( transportBean.getTransportAddress() );
        transport.setBackLog( transportBean.getTransportBackLog() );
        transport.setEnableSSL( transportBean.isTransportEnableSSL() );
        transport.setNbThreads( transportBean.getTransportNbThreads() );

        return transport;
    }


    /**
     * Read the configuration for the ChangeLog system
     * 
     * @param changelogDN The DN for the ChngeLog configuration
     * @return
     * @throws Exception
     */
    public ChangeLog createChangeLog( ChangeLogBean changeLogBean ) throws Exception
    {
        ChangeLog changeLog = new DefaultChangeLog();
        changeLog.setEnabled( changeLogBean.isEnabled() );
        changeLog.setExposed( changeLogBean.isChangeLogExposed() );

        return changeLog;
    }
    
    
    /**
     * Instanciate the Journal object from the stored configuration
     * 
     * @param journalDN The DN in the DIt for the Journal configuration
     * @return An instance of Journal
     * @throws Exception If the Journal creation failed
     */
    public Journal createJournal( JournalBean journalBean ) throws Exception
    {
        Journal journal = new DefaultJournal();

        journal.setRotation( journalBean.getJournalRotation() );
        journal.setEnabled( journalBean.isEnabled() );

        JournalStore store = new DefaultJournalStore();

        store.setFileName( journalBean.getJournalFileName() );
        store.setWorkingDirectory( journalBean.getJournalWorkingDir() );

        journal.setJournalStore( store );
        
        return journal;
    }


    /**
     * Load the Test entries
     * 
     * @param entryFilePath The place on disk where the test entris are stored
     * @return A list of difEntry elements
     * @throws Exception If we weren't able to read the config
     */
    public List<LdifEntry> readTestEntries( String entryFilePath ) throws Exception
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


    private Set<WebApp> createWebApps( DN webAppsDN ) throws Exception
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

            if ( ! isEnabled( webAppEntry ) )
            {
                continue;
            }
            
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
    public MechanismHandler createSaslMechHandler( Entry saslMechHandlerEntry ) throws Exception
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
    public PasswordPolicyConfiguration createPwdPolicyConfig( DN dirServiceDN ) throws Exception
    {
        PasswordPolicyConfiguration passwordPolicy = readPwdPolicyConfig( dirServiceDN );
        
        return passwordPolicy;
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
}