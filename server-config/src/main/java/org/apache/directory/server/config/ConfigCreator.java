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
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.directory.server.config.beans.AdsBaseBean;
import org.apache.directory.server.config.beans.ChangeLogBean;
import org.apache.directory.server.config.beans.ConfigBean;
import org.apache.directory.server.config.beans.DirectoryServiceBean;
import org.apache.directory.server.config.beans.ExtendedOpHandlerBean;
import org.apache.directory.server.config.beans.IndexBean;
import org.apache.directory.server.config.beans.InterceptorBean;
import org.apache.directory.server.config.beans.JdbmIndexBean;
import org.apache.directory.server.config.beans.JdbmPartitionBean;
import org.apache.directory.server.config.beans.JournalBean;
import org.apache.directory.server.config.beans.LdapServerBean;
import org.apache.directory.server.config.beans.PartitionBean;
import org.apache.directory.server.config.beans.PasswordPolicyBean;
import org.apache.directory.server.config.beans.SaslMechHandlerBean;
import org.apache.directory.server.config.beans.TcpTransportBean;
import org.apache.directory.server.config.beans.TransportBean;
import org.apache.directory.server.config.beans.UdpTransportBean;
import org.apache.directory.server.core.DefaultDirectoryService;
import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.core.authn.AuthenticationInterceptor;
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
import org.apache.directory.server.ldap.ExtendedOperationHandler;
import org.apache.directory.server.ldap.LdapServer;
import org.apache.directory.server.ldap.handlers.bind.MechanismHandler;
import org.apache.directory.server.ldap.handlers.bind.ntlm.NtlmMechanismHandler;
import org.apache.directory.server.ldap.replication.ReplicationProvider;
import org.apache.directory.server.ldap.replication.SyncReplProvider;
import org.apache.directory.server.protocol.shared.transport.TcpTransport;
import org.apache.directory.server.protocol.shared.transport.Transport;
import org.apache.directory.server.protocol.shared.transport.UdpTransport;
import org.apache.directory.server.xdbm.Index;
import org.apache.directory.shared.ldap.entry.Entry;
import org.apache.directory.shared.ldap.exception.LdapException;
import org.apache.directory.shared.ldap.exception.LdapInvalidDnException;
import org.apache.directory.shared.ldap.ldif.LdapLdifException;
import org.apache.directory.shared.ldap.ldif.LdifEntry;
import org.apache.directory.shared.ldap.ldif.LdifReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A class used for reading the configuration present in a Partition
 * and instantiate the necessary objects like DirectoryService, Interceptors etc.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class ConfigCreator
{
    /** The logger for this class */
    private static final Logger LOG = LoggerFactory.getLogger( ConfigCreator.class );

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
     * Creates the Interceptor instances from the configuration
     *
     * @param dirServiceDN the DN under which interceptors are configured
     * @return a list of instantiated Interceptor objects
     * @throws Exception If the instanciation failed
     */
    private List<Interceptor> createInterceptors( List<InterceptorBean> interceptorBeans ) throws LdapException
    {
        List<Interceptor> interceptors = new ArrayList<Interceptor>( interceptorBeans.size() );
        
        // First order the interceptorBeans
        Set<InterceptorBean> orderedInterceptorBeans = new TreeSet<InterceptorBean>();

        for ( InterceptorBean interceptorBean : interceptorBeans )
        {
            orderedInterceptorBeans.add( interceptorBean );
        }

        // Instantiate the interceptors now
        for ( InterceptorBean interceptorBean : orderedInterceptorBeans )
        {
            try
            {
                LOG.debug( "loading the interceptor class {} and instantiating", interceptorBean.getInterceptorClassName() );
                Interceptor interceptor = ( Interceptor ) Class.forName( interceptorBean.getInterceptorClassName() ).newInstance();
                interceptors.add( interceptor );
            }
            catch ( Exception e )
            {
                throw new ConfigurationException( e.getMessage() );
            }
        }
        
        return interceptors;
    }
    
    
    /**
     * creates the PassworddPolicyConfiguration object after reading the config entry containing pwdpolicy OC
     *
     * @param PasswordPolicyBean The Bean containing the PasswordPolicy configuration
     * @return the {@link PasswordPolicyConfiguration} object, null if the pwdpolicy entry is not present or disabled
     */
    public PasswordPolicyConfiguration createPwdPolicyConfig( PasswordPolicyBean passwordPolicyBean )
    {
        if ( passwordPolicyBean == null )
        {
            return null;
        }
        
        PasswordPolicyConfiguration passwordPolicy = new PasswordPolicyConfiguration();
        
        passwordPolicy.setPwdAllowUserChange( passwordPolicyBean.isPwdAllowUserChange() );
        passwordPolicy.setPwdAttribute( passwordPolicyBean.getPwdAttribute() );
        passwordPolicy.setPwdCheckQuality( passwordPolicyBean.getPwdCheckQuality() );
        passwordPolicy.setPwdEnabled( passwordPolicyBean.isEnabled() );
        passwordPolicy.setPwdExpireWarning( passwordPolicyBean.getPwdExpireWarning() );
        passwordPolicy.setPwdFailureCountInterval( passwordPolicyBean.getPwdFailureCountInterval() );
        passwordPolicy.setPwdGraceAuthNLimit( passwordPolicyBean.getPwdGraceAuthNLimit() );
        passwordPolicy.setPwdGraceExpire( passwordPolicyBean.getPwdGraceExpire() );
        passwordPolicy.setPwdInHistory( passwordPolicyBean.getPwdInHistory() );
        passwordPolicy.setPwdLockout( passwordPolicyBean.isPwdLockout() );
        passwordPolicy.setPwdLockoutDuration( passwordPolicyBean.getPwdLockoutDuration() );
        passwordPolicy.setPwdMaxAge( passwordPolicyBean.getPwdMaxAge() );
        passwordPolicy.setPwdMaxDelay( passwordPolicyBean.getPwdMaxDelay() );
        passwordPolicy.setPwdMaxFailure( passwordPolicyBean.getPwdMaxFailure() );
        passwordPolicy.setPwdMaxIdle( passwordPolicyBean.getPwdMaxIdle() );
        passwordPolicy.setPwdMaxLength( passwordPolicyBean.getPwdMaxLength() );
        passwordPolicy.setPwdMinAge( passwordPolicyBean.getPwdMinAge() );
        passwordPolicy.setPwdMinDelay( passwordPolicyBean.getPwdMinDelay() );
        passwordPolicy.setPwdMinLength( passwordPolicyBean.getPwdMinLength() );
        passwordPolicy.setPwdMustChange( passwordPolicyBean.isPwdMustChange() );
        passwordPolicy.setPwdSafeModify( passwordPolicyBean.isPwdSafeModify() );
        
        return passwordPolicy;
    }

    
    /**
     * Read the configuration for the ChangeLog system
     * 
     * @param changelogBean The Bean containing the ChangeLog configuration
     * @return The instantiated ChangeLog element
     */
    public ChangeLog createChangeLog( ChangeLogBean changeLogBean )
    {
        ChangeLog changeLog = new DefaultChangeLog();
        
        changeLog.setEnabled( changeLogBean.isEnabled() );
        changeLog.setExposed( changeLogBean.isChangeLogExposed() );

        return changeLog;
    }
    
    
    /**
     * Instantiate the Journal object from the stored configuration
     * 
     * @param changelogBean The Bean containing the ChangeLog configuration
     * @return An instance of Journal
     */
    public Journal createJournal( JournalBean journalBean )
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
     * @param entryFilePath The place on disk where the test entries are stored
     * @return A list of LdifEntry elements
     * @throws ConfigurationException If we weren't able to read the entries
     */
    public List<LdifEntry> readTestEntries( String entryFilePath ) throws ConfigurationException
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
            
            try
            {
                loadEntries( file, entries );
            }
            catch ( LdapLdifException e )
            {
                String message = "Error while parsing a LdifEntry : " + e.getMessage();
                LOG.error( message );
                throw new ConfigurationException( message );
            }
            catch ( IOException e )
            {
                String message = "cannot read the Ldif entries from the " + entryFilePath + " location";
                LOG.error( message );
                throw new ConfigurationException( message );
            }
        }

        return entries;
    }
    

    /**
     * Load the entries from a Ldif file recursively
     * @throws LdapLdifException 
     * @throws IOException 
     */
    private void loadEntries( File ldifFile, List<LdifEntry> entries ) throws LdapLdifException, IOException
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
            
            try
            {
                entries.addAll( reader.parseLdifFile( ldifFile.getAbsolutePath() ) );
            }
            finally
            {
                reader.close();
            }
        }
    }


    /**
     * Loads and instantiates a MechanismHandler from the configuration entry
     *
     * @param saslMechHandlerEntry the entry of OC type {@link ConfigSchemaConstants#ADS_LDAP_SERVER_SASL_MECH_HANDLER_OC}
     * @return an instance of the MechanismHandler type
     * @throws ConfigurationException if the SASL mechanism handler cannot be created
     */
    public MechanismHandler createSaslMechHandler( SaslMechHandlerBean saslMechHandlerBean ) throws ConfigurationException
    {
        String mechClassName = saslMechHandlerBean.getSaslMechClassName();
        
        Class<?> mechClass = null;
        
        try
        {
            mechClass = Class.forName( mechClassName );
        }
        catch ( ClassNotFoundException e )
        {
            String message = "Cannot find the class " + mechClassName;
            LOG.error( message );
            throw new ConfigurationException( message );
        }
        
        MechanismHandler handler = null;
        
        try
        {
            handler = ( MechanismHandler ) mechClass.newInstance();
        }
        catch ( InstantiationException e )
        {
            String message = "Cannot instantiate the class : " + mechClassName;
            LOG.error( message );
            throw new ConfigurationException( message );
        }
        catch ( IllegalAccessException e )
        {
            String message = "Cnnot invoke the class' constructor for " + mechClassName;
            LOG.error( message );
            throw new ConfigurationException( message );
        }
        
        if ( mechClass == NtlmMechanismHandler.class )
        {
            NtlmMechanismHandler ntlmHandler = ( NtlmMechanismHandler ) handler;
            ntlmHandler.setNtlmProviderFqcn( saslMechHandlerBean.getNtlmMechProvider() );
        }
        
        return handler;
    }
    
    
    /**
     * Creates a Transport from the configuration
     * 
     * @param transportBean The created instance of transport
     * @return An instance of transport
     */
    public Transport createTransport( TransportBean transportBean )
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
     * Creates the array of transports read from the DIT 
     * 
     * @param transportBeans The array of Transport configuration
     * @return An arry of Transport instance
     */
    public Transport[] createTransports( TransportBean[] transportBeans )
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
     * Instantiates a LdapServer based on the configuration present in the partition 
     *
     * @param ldapServer The LdapServerBean containing the LdapServer configuration
     * @return Instance of LdapServer
     * @throws LdapException
     */
    private LdapServer createLdapServer( LdapServerBean ldapServerBean, DirectoryService directoryService ) throws LdapException
    {
        // Fist, do nothing if the LdapServer is disabled
        if ( !ldapServerBean.isEnabled() )
        {
            return null;
        }

        LdapServer ldapServer = new LdapServer();
        
        ldapServer.setDirectoryService( directoryService );
        
        // The ID
        ldapServer.setServiceId( ldapServerBean.getServerId() );

        // SearchBaseDN
        ldapServer.setSearchBaseDn( ldapServerBean.getSearchBaseDn().getName() );

        // KeyStore
        ldapServer.setKeystoreFile( ldapServerBean.getLdapServerKeystoreFile() );
            
        // Certificate password
        ldapServer.setCertificatePassword( ldapServerBean.getLdapServerCertificatePassword() );
        
        // ConfidentialityRequired
        ldapServer.setConfidentialityRequired( ldapServerBean.isLdapServerConfidentialityRequired() );

        // Max size limit
        ldapServer.setMaxSizeLimit( ldapServerBean.getLdapServerMaxSizeLimit() );

        // Max time limit
        ldapServer.setMaxTimeLimit( ldapServerBean.getLdapServerMaxTimeLimit() );
        
        // Sasl Host
        ldapServer.setSaslHost( ldapServerBean.getLdapServerSaslHost() );
        
        // Sasl Principal
        ldapServer.setSaslPrincipal( ldapServerBean.getLdapServerSaslPrincipal() );
        
        // Sasl realm
        ldapServer.setSaslRealms( ldapServerBean.getLdapServerSaslRealms() );
        
        // The transports
        Transport[] transports = createTransports( ldapServerBean.getTransports() );
        ldapServer.setTransports( transports );

        // SaslMechs
        for ( SaslMechHandlerBean saslMechHandlerBean : ldapServerBean.getSaslMechHandlers() )
        {
            if ( saslMechHandlerBean.isEnabled() )
            {
                String mechanism = saslMechHandlerBean.getSaslMechName();
                ldapServer.addSaslMechanismHandler( mechanism, createSaslMechHandler( saslMechHandlerBean ) );
            }
        }
        
        // ExtendedOpHandlers
        for ( ExtendedOpHandlerBean extendedpHandlerBean : ldapServerBean.getExtendedOps() )
        {
            if ( extendedpHandlerBean.isEnabled() )
            {
                try
                {
                    Class<?> extendedOpClass = Class.forName( extendedpHandlerBean.getExtendedOpHandlerClass() );
                    ExtendedOperationHandler extOpHandler = ( ExtendedOperationHandler ) extendedOpClass.newInstance();
                    ldapServer.addExtendedOperationHandler( extOpHandler );
                }
                catch ( Exception e )
                {
                    String message = "Failed to load and instantiate ExtendedOperationHandler implementation " 
                        + extendedpHandlerBean.getExtendedOpId() + ": " + e.getMessage();
                    LOG.error( message );
                    throw new ConfigurationException( message );
                }
            }
        }

        // PasswordPolicy
        // TODO
        
        // ReplProvider
        if ( ldapServerBean.isEnableReplProvider() )
        {
            //EntryAttribute replProvImplAttr = ldapServerEntry.get( ConfigSchemaConstants.ADS_REPL_PROVIDER_IMPL );
            
            String fqcn = ldapServerBean.getReplProvider().getReplAttribute();
            
            if ( fqcn == null )
            {
                // default replication provider
                fqcn = SyncReplProvider.class.getName();
            }
            
            try
            {
                Class<?> replProvImplClz = Class.forName( fqcn );
                ReplicationProvider rp = ( ReplicationProvider ) replProvImplClz.newInstance();
                ldapServer.setReplicationProvider( rp );
            }
            catch( Exception e )
            {
                String message = "Failed to load and instantiate ReplicationProvider implementation : " + e.getMessage();
                LOG.error( message );
                throw new ConfigurationException( message );
            }
            
            // TODO
            // ldapServer.setReplProviderConfigs( createReplProviderConfigs() );
        }
        
        // ReplConsumer
        // TODO 
        

        return ldapServer;
    }
    
    
    /**
     * Create a new instance of a JdbmIndex from an instance of JdbmIndexBean
     * 
     * @param JdbmIndexBean The JdbmIndexBean to convert
     * @return An JdbmIndex instance
     * @throws Exception If the instance cannot be created
     */
    private JdbmIndex<?, Entry> createJdbmIndex( JdbmPartition partition, JdbmIndexBean<String, Entry> jdbmIndexBean )
    {
        JdbmIndex<String, Entry> index = new JdbmIndex<String, Entry>();
        
        index.setAttributeId( jdbmIndexBean.getIndexAttributeId() );
        index.setCacheSize( jdbmIndexBean.getIndexCacheSize() );
        index.setNumDupLimit( jdbmIndexBean.getIndexNumDupLimit() );
                
        if ( jdbmIndexBean.getIndexWorkingDir() != null )
        {
            index.setWkDirPath( new File( jdbmIndexBean.getIndexWorkingDir() + File.pathSeparator + jdbmIndexBean.getIndexFileName()) );
        }
        else
        {
            // Set the Partition working dir as a default
            index.setWkDirPath( new File( partition.getPartitionDir().getAbsolutePath() + File.pathSeparator + jdbmIndexBean.getIndexFileName() ) );
        }
                
        return index;
    }

    
    /**
     * Create the list of Index from the configuration
     */
    private Set<Index<?, Entry, Long>> createJdbmIndexes( JdbmPartition partition, List<IndexBean> indexesBeans ) //throws Exception
    {
        Set<Index<?, Entry, Long>> indexes = new HashSet<Index<?, Entry, Long>>();

        for ( IndexBean indexBean : indexesBeans )
        {
            if ( indexBean instanceof JdbmIndexBean )
            {
                indexes.add( createJdbmIndex( partition, (JdbmIndexBean)indexBean ) );
            }
        }

        return indexes;
    }


    /**
     * Create a new instance of a JdbmPartition
     * 
     * @param jdbmPartitionBean the JdbmPartition bean
     * @return The instantiated JdbmPartition
     * @throws LdapInvalidDnException 
     * @throws Exception If the instance cannot be created
     */
    public JdbmPartition createJdbmPartition( DirectoryService directoryService, JdbmPartitionBean jdbmPartitionBean ) throws ConfigurationException
    {
        JdbmPartition jdbmPartition = new JdbmPartition();
        
        jdbmPartition.setCacheSize( jdbmPartitionBean.getPartitionCacheSize() );
        jdbmPartition.setId( jdbmPartitionBean.getPartitionId() );
        jdbmPartition.setOptimizerEnabled( jdbmPartitionBean.isJdbmPartitionOptimizerEnabled() );
        jdbmPartition.setPartitionDir( new File( directoryService.getWorkingDirectory() + File.separator + jdbmPartitionBean.getPartitionId() ) );
        
        try
        {
            jdbmPartition.setSuffix( jdbmPartitionBean.getPartitionSuffix() );
        }
        catch ( LdapInvalidDnException lide )
        {
            String message = "Cannot set the DN " + jdbmPartitionBean.getPartitionSuffix() + ", " + lide.getMessage();
            LOG.error( message );
            throw new ConfigurationException( message );
        }
        
        jdbmPartition.setSyncOnWrite( jdbmPartitionBean.isPartitionSyncOnWrite() );
        jdbmPartition.setIndexedAttributes( createJdbmIndexes( jdbmPartition, jdbmPartitionBean.getIndexes() ) );
        
        String contextEntry = jdbmPartitionBean.getContextEntry();
        
        if ( contextEntry != null )
        {
            try
            {
                // Replace '\n' to real LF
                contextEntry.replaceAll( "\\n", "\n" );
                
                LdifReader ldifReader = new LdifReader();
                
                List<LdifEntry> entries = ldifReader.parseLdif( contextEntry );
                
                if ( ( entries != null ) && ( entries.size() > 0 ) )
                {
                    LdifEntry entry = entries.get( 0 );
                    jdbmPartition.setContextEntry( entry.getEntry() );
                }
            }
            catch ( LdapLdifException lle )
            {
                String message = "Cannot parse the context entry : " + contextEntry + ", " + lle.getMessage();
                LOG.error( message );
                throw new ConfigurationException( message );
            }
        }
        
        return jdbmPartition;
    }
    
    
    /**
     * Create the a Partition instantiated from the configuration
     * 
     * @param partitionBean the Partition bean
     * @return The instantiated Partition
     * @throws ConfigurationException If we cannot process the Partition
     */
    public Partition createPartition( DirectoryService directoryService, PartitionBean partitionBean ) throws ConfigurationException
    {
        if ( partitionBean instanceof JdbmPartitionBean )
        {
            return createJdbmPartition( directoryService, (JdbmPartitionBean)partitionBean );
        }
        else
        {
            return null;
        }
    }
    
    /**
     * Create the set of Partitions instantiated from the configuration
     * 
     * @param partitionBeans the list of Partition beans
     * @return A Map of all the instantiated partitions
     * @throws ConfigurationException If we cannot process some Partition
     */
    public Map<String, Partition> createPartitions( DirectoryService directoryService, List<PartitionBean> partitionBeans ) throws ConfigurationException
    {
        Map<String, Partition> partitions = new HashMap<String, Partition>( partitionBeans.size() );
        
        for ( PartitionBean partitionBean : partitionBeans )
        {
            Partition partition = createPartition( directoryService, partitionBean );
            
            if ( partition != null )
            {
                partitions.put( partitionBean.getPartitionId(), partition );
            }
        }
        
        return partitions;
    }

    
    /**
     * Instantiates a DirectoryService based on the configuration present in the partition 
     *
     * @param directoryServiceBean The bean containing the configuration
     * @return An instance of DirectoryService
     * @throws Exception 
     */
    private DirectoryService createDirectoryService( DirectoryServiceBean directoryServiceBean ) throws Exception
    {
        DirectoryService directoryService = new DefaultDirectoryService();

        // MUST attributes
        // DirectoryService ID
        directoryService.setInstanceId( directoryServiceBean.getDirectoryServiceId() );
        
        // Replica ID
        directoryService.setReplicaId( directoryServiceBean.getDsReplicaId() );

        // WorkingDirectory
        directoryService.setWorkingDirectory( new File( directoryServiceBean.getDsWorkingDirectory() ) );

        // Interceptors
        List<Interceptor> interceptors = createInterceptors( directoryServiceBean.getInterceptors() );
        directoryService.setInterceptors( interceptors );
        
        // PasswordPolicy
        PasswordPolicyConfiguration passwordPolicy = createPwdPolicyConfig( directoryServiceBean.getPasswordPolicy() );
        
        if ( passwordPolicy != null )
        {
            AuthenticationInterceptor authnInterceptor = ( AuthenticationInterceptor ) directoryService.getInterceptor( AuthenticationInterceptor.class.getName() );
            authnInterceptor.setPwdPolicyConfig( passwordPolicy );
        }

        // Partitions
        Map<String, Partition> partitions = createPartitions( directoryService, directoryServiceBean.getPartitions() );

        Partition systemPartition = partitions.remove( "system" );

        if ( systemPartition == null )
        {
            //throw new Exception( I18n.err( I18n.ERR_505 ) );
        }

        directoryService.setSystemPartition( systemPartition );
        directoryService.setPartitions( new HashSet<Partition>( partitions.values() ) );

        // MAY attributes
        // AccessControlEnabled
        directoryService.setAccessControlEnabled( directoryServiceBean.isDsAccessControlEnabled() );
        
        // AllowAnonymousAccess
        directoryService.setAllowAnonymousAccess( directoryServiceBean.isDsAllowAnonymousAccess() );
        
        // ChangeLog
        directoryService.setChangeLog( createChangeLog( directoryServiceBean.getChangeLog() ) );
        
        // DenormalizedOpAttrsEnabled
        directoryService.setDenormalizeOpAttrsEnabled( directoryServiceBean.isDsDenormalizeOpAttrsEnabled() );
        
        // Journal
        directoryService.setJournal( createJournal( directoryServiceBean.getJournal() ) );
        
        // MaxPDUSize
        directoryService.setMaxPDUSize( directoryServiceBean.getDsMaxPDUSize() );
        
        // PasswordHidden
        directoryService.setPasswordHidden( directoryServiceBean.isDsPasswordHidden() );

        // SyncPeriodMillis
        directoryService.setSyncPeriodMillis( directoryServiceBean.getDsSyncPeriodMillis() );

        // testEntries
        String entryFilePath = directoryServiceBean.getDsTestEntries();
        directoryService.setTestEntries( readTestEntries( entryFilePath ) );
        
        // Enabled
        if ( !directoryServiceBean.isEnabled() )
        {
            // will only be useful if we ever allow more than one DS to be configured and
            // switch between them
            // decide which one to use based on this flag
            // TODO
        }

        return directoryService;
    }

    
    /**
     * Create a new DirectoryService instance using the ConfigBean as a container for 
     * the configuration
     * 
     * @param configBean The Bean containing all the needed configuration to create the DS
     * @param directoryServiceId The DS id we want to instantiate
     * @return An instance of DS
     */
    public DirectoryService createDirectoryService( ConfigBean configBean, String directoryServiceId ) throws LdapException, Exception
    {
        List<AdsBaseBean> baseBeans = configBean.getDirectoryServiceBeans();
        
        for ( AdsBaseBean baseBean : baseBeans )
        {
            if ( !( baseBean instanceof DirectoryServiceBean ) )
            {
                String message = "Cannot instanciate a DS if the bean does not contain DirectoryService beans";
                LOG.error( message );
                throw new ConfigurationException( message );
            }
            
            DirectoryServiceBean directoryServiceBean = (DirectoryServiceBean)baseBean;
            
            if ( directoryServiceBean.getDirectoryServiceId().equalsIgnoreCase( directoryServiceId ) )
            {
                DirectoryService directoryService = createDirectoryService( directoryServiceBean );
                
                return directoryService;
            }
        }
        
        LOG.info( "Cannot instanciate the {} directory service, it was not found in the configuration", directoryServiceId );
        return null;
    }
}