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

package org.apache.directory.server.config.builder;


import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.exception.LdapInvalidDnException;
import org.apache.directory.api.ldap.model.ldif.LdapLdifException;
import org.apache.directory.api.ldap.model.ldif.LdifEntry;
import org.apache.directory.api.ldap.model.ldif.LdifReader;
import org.apache.directory.api.ldap.model.message.AliasDerefMode;
import org.apache.directory.api.ldap.model.message.ExtendedRequest;
import org.apache.directory.api.ldap.model.message.ExtendedResponse;
import org.apache.directory.api.ldap.model.message.SearchScope;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.apache.directory.api.util.Strings;
import org.apache.directory.server.config.ConfigurationException;
import org.apache.directory.server.config.beans.AuthenticationInterceptorBean;
import org.apache.directory.server.config.beans.AuthenticatorBean;
import org.apache.directory.server.config.beans.AuthenticatorImplBean;
import org.apache.directory.server.config.beans.ChangeLogBean;
import org.apache.directory.server.config.beans.DelegatingAuthenticatorBean;
import org.apache.directory.server.config.beans.DirectoryServiceBean;
import org.apache.directory.server.config.beans.ExtendedOpHandlerBean;
import org.apache.directory.server.config.beans.HttpServerBean;
import org.apache.directory.server.config.beans.HttpWebAppBean;
import org.apache.directory.server.config.beans.IndexBean;
import org.apache.directory.server.config.beans.InterceptorBean;
import org.apache.directory.server.config.beans.JdbmIndexBean;
import org.apache.directory.server.config.beans.JdbmPartitionBean;
import org.apache.directory.server.config.beans.JournalBean;
import org.apache.directory.server.config.beans.LdapServerBean;
import org.apache.directory.server.config.beans.MavibotIndexBean;
import org.apache.directory.server.config.beans.MavibotPartitionBean;
import org.apache.directory.server.config.beans.NtpServerBean;
import org.apache.directory.server.config.beans.PartitionBean;
import org.apache.directory.server.config.beans.PasswordPolicyBean;
import org.apache.directory.server.config.beans.ReplConsumerBean;
import org.apache.directory.server.config.beans.SaslMechHandlerBean;
import org.apache.directory.server.config.beans.TcpTransportBean;
import org.apache.directory.server.config.beans.TransportBean;
import org.apache.directory.server.constants.ApacheSchemaConstants;
import org.apache.directory.server.core.DefaultDirectoryService;
import org.apache.directory.server.core.api.DirectoryService;
import org.apache.directory.server.core.api.InstanceLayout;
import org.apache.directory.server.core.api.authn.ppolicy.CheckQualityEnum;
import org.apache.directory.server.core.api.authn.ppolicy.DefaultPasswordValidator;
import org.apache.directory.server.core.api.authn.ppolicy.PasswordPolicyConfiguration;
import org.apache.directory.server.core.api.authn.ppolicy.PasswordValidator;
import org.apache.directory.server.core.api.changelog.ChangeLog;
import org.apache.directory.server.core.api.interceptor.Interceptor;
import org.apache.directory.server.core.api.journal.Journal;
import org.apache.directory.server.core.api.journal.JournalStore;
import org.apache.directory.server.core.api.partition.AbstractPartition;
import org.apache.directory.server.core.api.partition.Partition;
import org.apache.directory.server.core.authn.AuthenticationInterceptor;
import org.apache.directory.server.core.authn.Authenticator;
import org.apache.directory.server.core.authn.DelegatingAuthenticator;
import org.apache.directory.server.core.authn.ppolicy.PpolicyConfigContainer;
import org.apache.directory.server.core.changelog.DefaultChangeLog;
import org.apache.directory.server.core.journal.DefaultJournal;
import org.apache.directory.server.core.journal.DefaultJournalStore;
import org.apache.directory.server.core.partition.impl.btree.jdbm.JdbmDnIndex;
import org.apache.directory.server.core.partition.impl.btree.jdbm.JdbmIndex;
import org.apache.directory.server.core.partition.impl.btree.jdbm.JdbmPartition;
import org.apache.directory.server.core.partition.impl.btree.jdbm.JdbmRdnIndex;
import org.apache.directory.server.core.partition.impl.btree.mavibot.MavibotDnIndex;
import org.apache.directory.server.core.partition.impl.btree.mavibot.MavibotIndex;
import org.apache.directory.server.core.partition.impl.btree.mavibot.MavibotPartition;
import org.apache.directory.server.core.partition.impl.btree.mavibot.MavibotRdnIndex;
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.server.integration.http.HttpServer;
import org.apache.directory.server.integration.http.WebApp;
import org.apache.directory.server.ldap.ExtendedOperationHandler;
import org.apache.directory.server.ldap.LdapServer;
import org.apache.directory.server.ldap.handlers.sasl.MechanismHandler;
import org.apache.directory.server.ldap.handlers.sasl.ntlm.NtlmMechanismHandler;
import org.apache.directory.server.ldap.replication.SyncReplConfiguration;
import org.apache.directory.server.ldap.replication.consumer.ReplicationConsumer;
import org.apache.directory.server.ldap.replication.consumer.ReplicationConsumerImpl;
import org.apache.directory.server.ldap.replication.provider.ReplicationRequestHandler;
import org.apache.directory.server.ldap.replication.provider.SyncReplRequestHandler;
import org.apache.directory.server.ntp.NtpServer;
import org.apache.directory.server.protocol.shared.transport.TcpTransport;
import org.apache.directory.server.protocol.shared.transport.Transport;
import org.apache.directory.server.protocol.shared.transport.UdpTransport;
import org.apache.directory.server.xdbm.Index;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A class used for reading the configuration present in a Partition
 * and instantiate the necessary objects like DirectoryService, Interceptors etc.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public final class ServiceBuilder
{
    /** The logger for this class */
    private static final Logger LOG = LoggerFactory.getLogger( ServiceBuilder.class );

    /** LDIF file filter */
    private static FilenameFilter ldifFilter = new FilenameFilter()
    {
        public boolean accept( File file, String name )
        {
            if ( file.isDirectory() )
            {
                return true;
            }

            return Strings.toLowerCaseAscii( file.getName() ).endsWith( ".ldif" );
        }
    };


    private ServiceBuilder()
    {
    }


    /**
     * Creates the Interceptor instances from the configuration
     *
     * @param interceptorBeans The Interceptors configuration used to create Interceptors
     * @return a list of instantiated Interceptor objects
     * @throws LdapException If the instanciation failed
     */
    public static List<Interceptor> createInterceptors( List<InterceptorBean> interceptorBeans ) throws LdapException
    {
        List<Interceptor> interceptors = new ArrayList<>( interceptorBeans.size() );

        // First order the interceptorBeans
        Set<InterceptorBean> orderedInterceptorBeans = new TreeSet<>();

        for ( InterceptorBean interceptorBean : interceptorBeans )
        {
            if ( interceptorBean.isEnabled() )
            {
                orderedInterceptorBeans.add( interceptorBean );
            }
        }

        // Instantiate the interceptors now
        for ( InterceptorBean interceptorBean : orderedInterceptorBeans )
        {
            try
            {
                LOG.debug( "loading the interceptor class {} and instantiating",
                    interceptorBean.getInterceptorClassName() );
                Class<?> clazz = Class.forName( interceptorBean.getInterceptorClassName() );
                Interceptor interceptor = null;
                try
                {
                    Constructor<?> constructor = clazz.getDeclaredConstructor( interceptorBean.getClass() );
                    interceptor = ( Interceptor ) constructor.newInstance( interceptorBean );
                }
                catch ( NoSuchMethodException e )
                {
                    interceptor = ( Interceptor ) Class.forName( interceptorBean.getInterceptorClassName() )
                        .newInstance();
                }

                if ( interceptorBean instanceof AuthenticationInterceptorBean )
                {
                    // Transports
                    Authenticator[] authenticators = createAuthenticators( ( ( AuthenticationInterceptorBean ) interceptorBean )
                        .getAuthenticators() );
                    ( ( AuthenticationInterceptor ) interceptor ).setAuthenticators( authenticators );

                    // password policies
                    List<PasswordPolicyBean> ppolicyBeans = ( ( AuthenticationInterceptorBean ) interceptorBean )
                        .getPasswordPolicies();
                    PpolicyConfigContainer ppolicyContainer = new PpolicyConfigContainer();

                    for ( PasswordPolicyBean ppolicyBean : ppolicyBeans )
                    {
                        PasswordPolicyConfiguration ppolicyConfig = createPwdPolicyConfig( ppolicyBean );

                        if ( ppolicyConfig != null )
                        {
                            ppolicyContainer.addPolicy( ppolicyBean.getDn(), ppolicyConfig );

                            // the name should be strictly 'default', the default policy can't be enforced by defining a new AT
                            if ( ppolicyBean.getPwdId().equalsIgnoreCase( "default" ) )
                            {
                                ppolicyContainer.setDefaultPolicyDn( ppolicyBean.getDn() );
                            }
                        }
                    }

                    ( ( AuthenticationInterceptor ) interceptor ).setPwdPolicies( ppolicyContainer );
                }

                interceptors.add( interceptor );
            }
            catch ( Exception e )
            {
                String message = "Cannot initialize the " + interceptorBean.getInterceptorClassName() + ", error : "
                    + e;
                LOG.error( message );
                throw new ConfigurationException( message );
            }
        }

        return interceptors;
    }


    /**
     * creates the PassworddPolicyConfiguration object after reading the config entry containing pwdPolicy OC
     *
     * @param passwordPolicyBean The Bean containing the PasswordPolicy configuration
     * @return the {@link PasswordPolicyConfiguration} object, null if the pwdPolicy entry is not present or disabled
     */
    public static PasswordPolicyConfiguration createPwdPolicyConfig( PasswordPolicyBean passwordPolicyBean )
    {
        if ( ( passwordPolicyBean == null ) || passwordPolicyBean.isDisabled() )
        {
            return null;
        }

        PasswordPolicyConfiguration passwordPolicy = new PasswordPolicyConfiguration();

        passwordPolicy.setPwdAllowUserChange( passwordPolicyBean.isPwdAllowUserChange() );
        passwordPolicy.setPwdAttribute( passwordPolicyBean.getPwdAttribute() );
        passwordPolicy.setPwdCheckQuality( CheckQualityEnum.getCheckQuality( passwordPolicyBean.getPwdCheckQuality() ) );
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

        PasswordValidator validator = null;

        try
        {
            String className = passwordPolicyBean.getPwdValidator();

            if ( className != null )
            {
                Class<?> cls = Class.forName( className );
                validator = ( PasswordValidator ) cls.newInstance();
            }
        }
        catch ( Exception e )
        {
            LOG.warn(
                "Failed to load and instantiate the custom password validator for password policy config {}, using the default validator",
                passwordPolicyBean.getDn(), e );
        }

        if ( validator == null )
        {
            validator = new DefaultPasswordValidator();
        }

        passwordPolicy.setPwdValidator( validator );

        return passwordPolicy;
    }


    /**
     * Read the configuration for the ChangeLog system
     * 
     * @param changeLogBean The Bean containing the ChangeLog configuration
     * @return The instantiated ChangeLog element
     */
    public static ChangeLog createChangeLog( ChangeLogBean changeLogBean )
    {
        if ( ( changeLogBean == null ) || changeLogBean.isDisabled() )
        {
            return null;
        }

        ChangeLog changeLog = new DefaultChangeLog();

        changeLog.setEnabled( changeLogBean.isEnabled() );
        changeLog.setExposed( changeLogBean.isChangeLogExposed() );

        return changeLog;
    }


    /**
     * Instantiate the Journal object from the stored configuration
     * 
     * @param journalBean The Bean containing the Journal configuration
     * @return An instance of Journal
     */
    public static Journal createJournal( JournalBean journalBean )
    {
        if ( ( journalBean == null ) || journalBean.isDisabled() )
        {
            return null;
        }

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
    public static List<LdifEntry> readTestEntries( String entryFilePath ) throws ConfigurationException
    {
        List<LdifEntry> entries = new ArrayList<>();

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
    private static void loadEntries( File ldifFile, List<LdifEntry> entries ) throws LdapLdifException, IOException
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
     * @param saslMechHandlerBean The SaslMechHandler configuration used to create MechanismHandler instance
     * @return an instance of the MechanismHandler type
     * @throws ConfigurationException if the SASL mechanism handler cannot be created
     */
    public static MechanismHandler createSaslMechHandler( SaslMechHandlerBean saslMechHandlerBean )
        throws ConfigurationException
    {
        if ( ( saslMechHandlerBean == null ) || saslMechHandlerBean.isDisabled() )
        {
            return null;
        }

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
     * Creates a Authenticator from the configuration
     * 
     * @param authenticatorBean The created instance of authenticator
     * @return An instance of authenticator if the given authenticatorBean is not disabled
     * @throws ConfigurationException If the Authenticator cannot be created
     */
    public static Authenticator createAuthenticator( AuthenticatorBean authenticatorBean )
        throws ConfigurationException
    {
        if ( authenticatorBean.isDisabled() )
        {
            return null;
        }

        Authenticator authenticator = null;

        if ( authenticatorBean instanceof DelegatingAuthenticatorBean )
        {
            try
            {
                authenticator = new DelegatingAuthenticator(
                    new Dn(
                        ( ( DelegatingAuthenticatorBean ) authenticatorBean ).getBaseDn() ) );
            }
            catch ( LdapInvalidDnException e )
            {
                String errorMsg = "Failed to instantiate the configured authenticator "
                    + authenticatorBean.getAuthenticatorId();
                LOG.warn( errorMsg );
                throw new ConfigurationException( errorMsg, e );
            }

            ( ( DelegatingAuthenticator ) authenticator )
                .setDelegateHost( ( ( DelegatingAuthenticatorBean ) authenticatorBean ).getDelegateHost() );
            ( ( DelegatingAuthenticator ) authenticator )
                .setDelegatePort( ( ( DelegatingAuthenticatorBean ) authenticatorBean ).getDelegatePort() );
        }
        else if ( authenticatorBean instanceof AuthenticatorImplBean )
        {
            String fqcn = ( ( AuthenticatorImplBean ) authenticatorBean ).getAuthenticatorClass();

            try
            {
                Class<?> authnImplClass = Class.forName( fqcn );
                authenticator = ( Authenticator ) authnImplClass.newInstance();
            }
            catch ( Exception e )
            {
                String errorMsg = "Failed to instantiate the configured authenticator "
                    + authenticatorBean.getAuthenticatorId();
                LOG.warn( errorMsg );
                throw new ConfigurationException( errorMsg, e );
            }
        }

        return authenticator;
    }


    /**
     * Creates a Transport from the configuration
     * 
     * @param transportBean The created instance of transport
     * @return An instance of transport
     */
    public static Transport createTransport( TransportBean transportBean )
    {
        if ( ( transportBean == null ) || transportBean.isDisabled() )
        {
            return null;
        }

        Transport transport = null;

        if ( transportBean instanceof TcpTransportBean )
        {
            transport = new TcpTransport();
        }
        else
        {
            transport = new UdpTransport();
        }

        transport.setPort( transportBean.getSystemPort() );
        transport.setAddress( transportBean.getTransportAddress() );
        transport.setBackLog( transportBean.getTransportBackLog() );
        transport.setNbThreads( transportBean.getTransportNbThreads() );

        if ( transport instanceof TcpTransport )
        {
            ( ( TcpTransport ) transport ).setEnableSSL( transportBean.isTransportEnableSSL() );

            if ( ( ( TcpTransport ) transport ).isSSLEnabled() )
            {
                ( ( TcpTransport ) transport ).setNeedClientAuth( transportBean.getNeedClientAuth() );
                ( ( TcpTransport ) transport ).setWantClientAuth( transportBean.getWantClientAuth() );
                List<String> enabledProtocols = transportBean.getEnabledProtocols();

                if ( ( enabledProtocols != null ) && !enabledProtocols.isEmpty() )
                {
                    ( ( TcpTransport ) transport ).setEnabledProtocols( enabledProtocols );
                }

                List<String> enabledCiphers = transportBean.getEnabledCiphers();

                if ( ( enabledCiphers != null ) && !enabledCiphers.isEmpty() )
                {
                    ( ( TcpTransport ) transport ).setEnabledCiphers( enabledCiphers );
                }
            }
        }

        return transport;
    }


    /**
     * Creates the array of authenticators
     * 
     * @param list The array of AuthenticatorBean configuration
     * @return An array of Authenticator instance
     * @throws ConfigurationException If one of theAuthenticator cannot be created 
     */
    public static Authenticator[] createAuthenticators( List<AuthenticatorBean> list ) throws ConfigurationException
    {
        Set<Authenticator> authenticators = new HashSet<>( list.size() );

        for ( AuthenticatorBean authenticatorBean : list )
        {
            if ( authenticatorBean.isEnabled() )
            {
                authenticators.add( createAuthenticator( authenticatorBean ) );
            }
        }

        return authenticators.toArray( new Authenticator[]
            {} );
    }


    /**
     * Creates the array of transports read from the DIT
     * 
     * @param transportBeans The array of Transport configuration
     * @return An arry of Transport instance
     */
    public static Transport[] createTransports( TransportBean[] transportBeans )
    {
        List<Transport> transports = new ArrayList<>();

        for ( TransportBean transportBean : transportBeans )
        {
            if ( transportBean.isEnabled() )
            {
                transports.add( createTransport( transportBean ) );
            }
        }

        return transports.toArray( new Transport[transports.size()] );
    }

    /**
     * Instantiates a NtpServer based on the configuration present in the partition
     *
     * @param ntpServerBean The NtpServerBean containing the NtpServer configuration
     * @param directoryService The DirectoryService instance
     * @return Instance of NtpServer
     * @throws LdapException If the NtpServer instance cannot be created
     */
    public static NtpServer createNtpServer( NtpServerBean ntpServerBean, DirectoryService directoryService )
    {
        // Fist, do nothing if the NtpServer is disabled
        if ( ( ntpServerBean == null ) || ntpServerBean.isDisabled() )
        {
            return null;
        }

        NtpServer ntpServer = new NtpServer();

        // The service ID
        ntpServer.setServiceId( ntpServerBean.getServerId() );

        // The transports
        Transport[] transports = createTransports( ntpServerBean.getTransports() );
        ntpServer.setTransports( transports );

        return ntpServer;
    }


    /*
     * Instantiates a DhcpServer based on the configuration present in the partition
     *
     * @param dhcpServerBean The DhcpServerBean containing the DhcpServer configuration
     * @return Instance of DhcpServer
     * @throws LdapException
     *
    public static DhcpServer createDhcpServer( DhcpServerBean dhcpServerBean, DirectoryService directoryService ) throws LdapException
    {
        // Fist, do nothing if the DhcpServer is disabled
        if ( !dhcpServerBean.isEnabled() )
        {
            return null;
        }

        DhcpServer dhcpServer = new DhcpServer();
        
        // The service ID
        dhcpServer.setServiceId( dhcpServerBean.getServerId() );
        
        // The transports
        Transport[] transports = createTransports( dhcpServerBean.getTransports() );
        dhcpServer.setTransports( transports );
        
        return dhcpServer;
    }

    /**
     * Instantiates the HttpWebApps based on the configuration present in the partition
     *
     * @param httpWebAppBeans The list of HttpWebAppBeans containing the HttpWebAppBeans configuration
     * @param directoryService The DirectoryService instance
     * @return Instances of HttpWebAppBean
     * @throws LdapException If the HttpWebApps instance cannot be created
     */
    public static Set<WebApp> createHttpWebApps( List<HttpWebAppBean> httpWebAppBeans, DirectoryService directoryService )
    {
        Set<WebApp> webApps = new HashSet<>();

        if ( httpWebAppBeans == null )
        {
            return webApps;
        }

        for ( HttpWebAppBean httpWebAppBean : httpWebAppBeans )
        {
            if ( httpWebAppBean.isDisabled() )
            {
                continue;
            }

            WebApp webApp = new WebApp();

            // HttpAppCtxPath
            webApp.setContextPath( httpWebAppBean.getHttpAppCtxPath() );

            // HttpWarFile
            webApp.setWarFile( httpWebAppBean.getHttpWarFile() );

            webApps.add( webApp );
        }

        return webApps;
    }


    /**
     * Instantiates a HttpServer based on the configuration present in the partition
     *
     * @param httpServerBean The HttpServerBean containing the HttpServer configuration
     * @param directoryService The DirectoryService instance
     * @return Instance of LdapServer
     * @throws LdapException If the HttpServer cannot be created
     */
    public static HttpServer createHttpServer( HttpServerBean httpServerBean, DirectoryService directoryService )
    {
        // Fist, do nothing if the HttpServer is disabled
        if ( ( httpServerBean == null ) || httpServerBean.isDisabled() )
        {
            return null;
        }

        HttpServer httpServer = new HttpServer();

        // HttpConfFile
        httpServer.setConfFile( httpServerBean.getHttpConfFile() );

        // The transports
        TransportBean[] transports = httpServerBean.getTransports();

        for ( TransportBean transportBean : transports )
        {
            if ( transportBean.isDisabled() )
            {
                continue;
            }

            if ( transportBean instanceof TcpTransportBean )
            {
                TcpTransport transport = new TcpTransport( transportBean.getSystemPort() );
                transport.setAddress( transportBean.getTransportAddress() );

                if ( transportBean.getTransportId().equalsIgnoreCase( HttpServer.HTTP_TRANSPORT_ID ) )
                {
                    httpServer.setHttpTransport( transport );
                }
                else if ( transportBean.getTransportId().equalsIgnoreCase( HttpServer.HTTPS_TRANSPORT_ID ) )
                {
                    httpServer.setHttpsTransport( transport );
                }
                else
                {
                    LOG.warn( "Transport ids of HttpServer should be either 'http' or 'https'" );
                }
            }
        }

        // The webApps
        httpServer.setWebApps( createHttpWebApps( httpServerBean.getHttpWebApps(), directoryService ) );

        return httpServer;
    }


    /**
     * Instantiates a ChangePasswordServer based on the configuration present in the partition
     *
     * @param ldapServerBean The ChangePasswordServerBean containing the ChangePasswordServer configuration
     * @return Instance of ChangePasswordServer
     * @throws LdapException
     *
    public static ChangePasswordServer createChangePasswordServer( ChangePasswordServerBean changePasswordServerBean, DirectoryService directoryService ) throws LdapException
    {
        // Fist, do nothing if the LdapServer is disabled
        if ( ( changePasswordServerBean == null ) || changePasswordServerBean.isDisabled() )
        {
            return null;
        }

        ChangePasswordServer changePasswordServer = new ChangePasswordServer();
        changePasswordServer.setEnabled( true );
        changePasswordServer.setDirectoryService( directoryService );

        // AllowableClockSkew
        changePasswordServer.setAllowableClockSkew( changePasswordServerBean.getKrbAllowableClockSkew() );

        // TODO CatalogBased
        //changePasswordServer.setCatalogBased( changePasswordServerBean.isCatalogBase() );

        // EmptyAddressesAllowed
        changePasswordServer.setEmptyAddressesAllowed( changePasswordServerBean.isKrbEmptyAddressesAllowed() );

        // PolicyCategoryCount
        changePasswordServer.setPolicyCategoryCount( changePasswordServerBean.getChgPwdPolicyCategoryCount() );

        // PolicyPasswordLength
        changePasswordServer.setPolicyPasswordLength( changePasswordServerBean.getChgPwdPolicyPasswordLength() );

        // policyTokenSize
        changePasswordServer.setPolicyTokenSize( changePasswordServerBean.getChgPwdPolicyTokenSize() );

        // PrimaryRealm
        changePasswordServer.setPrimaryRealm( changePasswordServerBean.getKrbPrimaryRealm() );

        // SearchBaseDn
        changePasswordServer.setSearchBaseDn( changePasswordServerBean.getSearchBaseDn().getName() );

        // Id/Name
        changePasswordServer.setServiceName( changePasswordServerBean.getServerId() );
        changePasswordServer.setServiceId( changePasswordServerBean.getServerId() );

        // ServicePrincipal
        changePasswordServer.setServicePrincipal( changePasswordServerBean.getChgPwdServicePrincipal() );

        // Transports
        Transport[] transports = createTransports( changePasswordServerBean.getTransports() );
        changePasswordServer.setTransports( transports );
        
        return changePasswordServer;
    }
    */

    /**
     * Instantiates a LdapServer based on the configuration present in the partition
     *
     * @param ldapServerBean The LdapServerBean containing the LdapServer configuration
     * @param directoryService The DirectoryService instance
     * @return Instance of LdapServer
     * @throws LdapException If the LdapServer cannot be created
     */
    public static LdapServer createLdapServer( LdapServerBean ldapServerBean, DirectoryService directoryService )
        throws LdapException
    {
        // Fist, do nothing if the LdapServer is disabled
        if ( ( ldapServerBean == null ) || ldapServerBean.isDisabled() )
        {
            return null;
        }

        LdapServer ldapServer = new LdapServer();

        ldapServer.setDirectoryService( directoryService );
        ldapServer.setEnabled( true );

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

        // MaxPDUSize
        ldapServer.setMaxPDUSize( ldapServerBean.getMaxPDUSize() );

        // Sasl Host
        ldapServer.setSaslHost( ldapServerBean.getLdapServerSaslHost() );

        // Sasl Principal
        ldapServer.setSaslPrincipal( ldapServerBean.getLdapServerSaslPrincipal() );

        // Sasl realm
        ldapServer.setSaslRealms( ldapServerBean.getLdapServerSaslRealms() );

        // Relplication pinger thread sleep time
        ldapServer.setReplPingerSleepTime( ldapServerBean.getReplPingerSleep() );

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
                    ExtendedOperationHandler<ExtendedRequest, ExtendedResponse> extOpHandler =
                        ( ExtendedOperationHandler<ExtendedRequest, ExtendedResponse> ) extendedOpClass.newInstance();
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

        // ReplReqHandler
        boolean replicationEnabled = ldapServerBean.isReplEnabled();

        if ( replicationEnabled )
        {
            String fqcn = ldapServerBean.getReplReqHandler();

            if ( fqcn != null )
            {
                try
                {
                    Class<?> replProvImplClz = Class.forName( fqcn );
                    ReplicationRequestHandler rp = ( ReplicationRequestHandler ) replProvImplClz.newInstance();
                    ldapServer.setReplicationReqHandler( rp );
                }
                catch ( Exception e )
                {
                    String message = "Failed to load and instantiate ReplicationRequestHandler implementation : "
                        + fqcn;
                    LOG.error( message );
                    throw new ConfigurationException( message );
                }
            }
            else
            {
                // Try with the default handler
                ReplicationRequestHandler rp = new SyncReplRequestHandler();
                ldapServer.setReplicationReqHandler( rp );
            }
        }

        ldapServer.setReplConsumers( createReplConsumers( ldapServerBean.getReplConsumers() ) );

        return ldapServer;
    }


    /**
     * instantiate the ReplicationConsumers based on the configuration present in ReplConsumerBeans
     * 
     * @param replConsumerBeans the list of consumers configured
     * @return a list of ReplicationConsumer instances
     * @throws ConfigurationException If the replication consumer instance cannot be created
     */
    public static List<ReplicationConsumer> createReplConsumers( List<ReplConsumerBean> replConsumerBeans )
        throws ConfigurationException
    {
        List<ReplicationConsumer> lst = new ArrayList<>();

        if ( replConsumerBeans == null )
        {
            return lst;
        }

        for ( ReplConsumerBean replBean : replConsumerBeans )
        {
            if ( replBean.isDisabled() )
            {
                continue;
            }

            String className = replBean.getReplConsumerImpl();

            ReplicationConsumer consumer = null;
            Class<?> consumerClass = null;
            SyncReplConfiguration config = null;

            try
            {
                if ( className == null )
                {
                    consumer = new ReplicationConsumerImpl();
                }
                else
                {
                    consumerClass = Class.forName( className );
                    consumer = ( ReplicationConsumer ) consumerClass.newInstance();
                }

                // we don't support any other configuration impls atm, but this configuration should suffice for many needs
                config = new SyncReplConfiguration();

                config.setBaseDn( replBean.getSearchBaseDn() );
                config.setRemoteHost( replBean.getReplProvHostName() );
                config.setRemotePort( replBean.getReplProvPort() );

                try
                {
                    config.setAliasDerefMode( AliasDerefMode.getDerefMode( replBean.getReplAliasDerefMode() ) );
                }
                catch ( IllegalArgumentException iae )
                {
                    LOG.error( "{}, defaulted to 'never'", iae.getMessage() );
                }

                config.setAttributes( replBean.getReplAttributes().toArray( new String[0] ) );
                config.setRefreshInterval( replBean.getReplRefreshInterval() );
                config.setRefreshNPersist( replBean.isReplRefreshNPersist() );

                int scope = SearchScope.getSearchScope( replBean.getReplSearchScope() );
                config.setSearchScope( SearchScope.getSearchScope( scope ) );

                config.setFilter( replBean.getReplSearchFilter() );
                config.setSearchTimeout( replBean.getReplSearchTimeout() );
                config.setReplUserDn( replBean.getReplUserDn() );
                config.setReplUserPassword( replBean.getReplUserPassword() );
                config.setSearchSizeLimit( replBean.getReplSearchSizeLimit() );

                config.setUseTls( replBean.isReplUseTls() );
                config.setStrictCertVerification( replBean.isReplStrictCertValidation() );

                config.setConfigEntryDn( replBean.getDn() );

                consumer.setConfig( config );

                lst.add( consumer );
            }
            catch ( Exception e )
            {
                throw new ConfigurationException( "cannot configure the replication consumer with FQCN " + className, e );
            }
        }

        return lst;
    }


    /**
     * Create a new instance of a JdbmIndex from an instance of JdbmIndexBean
     * 
     * @param partition The JdbmPartition instance
     * @param jdbmIndexBean The JdbmIndexBean to convert
     * @param directoryService The DirectoryService instance
     * @return An JdbmIndex instance
     */
    public static JdbmIndex<?> createJdbmIndex( JdbmPartition partition,
        JdbmIndexBean jdbmIndexBean, DirectoryService directoryService )
    {
        if ( ( jdbmIndexBean == null ) || jdbmIndexBean.isDisabled() )
        {
            return null;
        }

        JdbmIndex<?> index = null;

        boolean hasReverse = jdbmIndexBean.getIndexHasReverse();

        if ( jdbmIndexBean.getIndexAttributeId().equalsIgnoreCase( ApacheSchemaConstants.APACHE_RDN_AT )
            || jdbmIndexBean.getIndexAttributeId().equalsIgnoreCase( ApacheSchemaConstants.APACHE_RDN_AT_OID ) )
        {
            index = new JdbmRdnIndex();
        }
        else if ( jdbmIndexBean.getIndexAttributeId().equalsIgnoreCase( ApacheSchemaConstants.APACHE_ALIAS_AT )
            || jdbmIndexBean.getIndexAttributeId().equalsIgnoreCase( ApacheSchemaConstants.APACHE_ALIAS_AT_OID ) )
        {
            index = new JdbmDnIndex( ApacheSchemaConstants.APACHE_ALIAS_AT_OID );
        }
        else
        {
            index = new JdbmIndex<>( jdbmIndexBean.getIndexAttributeId(), hasReverse );
        }

        index.setCacheSize( jdbmIndexBean.getIndexCacheSize() );
        index.setNumDupLimit( jdbmIndexBean.getIndexNumDupLimit() );

        // Find the OID for this index
        if ( jdbmIndexBean.getIndexWorkingDir() != null )
        {
            index.setWkDirPath( new File( jdbmIndexBean.getIndexWorkingDir() ).toURI() );
        }
        else
        {
            // Set the Partition working dir as a default
            index.setWkDirPath( partition.getPartitionPath() );
        }

        return index;
    }


    /**
     * Create the list of Index from the configuration
     */
    private static Set<Index<?, String>> createJdbmIndexes( JdbmPartition partition,
        List<IndexBean> indexesBeans,
        DirectoryService directoryService ) //throws Exception
    {
        Set<Index<?, String>> indexes = new HashSet<>();

        for ( IndexBean indexBean : indexesBeans )
        {
            if ( indexBean.isEnabled() && ( indexBean instanceof JdbmIndexBean ) )
            {
                indexes.add( createJdbmIndex( partition, ( JdbmIndexBean ) indexBean, directoryService ) );
            }
        }

        return indexes;
    }


    /**
     * Create a new instance of a JdbmPartition
     * 
     * @param directoryService The DirectoryService instance
     * @param jdbmPartitionBean the JdbmPartition bean
     * @return The instantiated JdbmPartition
     * @throws ConfigurationException If the instance cannot be created
     */
    public static JdbmPartition createJdbmPartition( DirectoryService directoryService,
        JdbmPartitionBean jdbmPartitionBean ) throws ConfigurationException
    {
        if ( ( jdbmPartitionBean == null ) || jdbmPartitionBean.isDisabled() )
        {
            return null;
        }

        JdbmPartition jdbmPartition = new JdbmPartition( directoryService.getSchemaManager(),
            directoryService.getDnFactory() );

        jdbmPartition.setCacheSize( jdbmPartitionBean.getPartitionCacheSize() );
        jdbmPartition.setId( jdbmPartitionBean.getPartitionId() );
        jdbmPartition.setOptimizerEnabled( jdbmPartitionBean.isJdbmPartitionOptimizerEnabled() );
        File partitionPath = new File( directoryService.getInstanceLayout().getPartitionsDirectory(),
            jdbmPartitionBean.getPartitionId() );
        jdbmPartition.setPartitionPath( partitionPath.toURI() );

        try
        {
            jdbmPartition.setSuffixDn( jdbmPartitionBean.getPartitionSuffix() );
        }
        catch ( LdapInvalidDnException lide )
        {
            String message = "Cannot set the Dn " + jdbmPartitionBean.getPartitionSuffix() + ", " + lide.getMessage();
            LOG.error( message );
            throw new ConfigurationException( message );
        }

        jdbmPartition.setSyncOnWrite( jdbmPartitionBean.isPartitionSyncOnWrite() );
        jdbmPartition.setIndexedAttributes( createJdbmIndexes( jdbmPartition, jdbmPartitionBean.getIndexes(),
            directoryService ) );

        setContextEntry( jdbmPartitionBean, jdbmPartition );

        return jdbmPartition;
    }


    /**
     * Create the a Partition instantiated from the configuration
     * 
     * @param directoryService The DirectoryService instance
     * @param partitionBean the Partition bean
     * @return The instantiated Partition
     * @throws ConfigurationException If we cannot process the Partition
     */
    public static Partition createPartition( DirectoryService directoryService, PartitionBean partitionBean )
        throws ConfigurationException
    {
        if ( ( partitionBean == null ) || partitionBean.isDisabled() )
        {
            return null;
        }

        if ( partitionBean instanceof JdbmPartitionBean )
        {
            return createJdbmPartition( directoryService, ( JdbmPartitionBean ) partitionBean );
        }
        else if ( partitionBean instanceof MavibotPartitionBean )
        {
            return createMavibotPartition( directoryService, ( MavibotPartitionBean ) partitionBean );
        }
        else
        {
            return null;
        }
    }


    /**
     * Create the set of Partitions instantiated from the configuration
     * 
     * @param directoryService The DirectoryService instance
     * @param partitionBeans the list of Partition beans
     * @return A Map of all the instantiated partitions
     * @throws ConfigurationException If we cannot process some Partition
     */
    public static Map<String, Partition> createPartitions( DirectoryService directoryService,
        List<PartitionBean> partitionBeans ) throws ConfigurationException
    {
        Map<String, Partition> partitions = new HashMap<>( partitionBeans.size() );

        for ( PartitionBean partitionBean : partitionBeans )
        {
            if ( partitionBean.isDisabled() )
            {
                continue;
            }

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
     * @param instanceLayout The InstanceLayout instance
     * @param schemaManager The SchemaManager instance
     * @return An instance of DirectoryService
     * @throws Exception If the DirectoryService cannot be created
     */
    public static DirectoryService createDirectoryService( DirectoryServiceBean directoryServiceBean,
        InstanceLayout instanceLayout, SchemaManager schemaManager ) throws Exception
    {
        DirectoryService directoryService = new DefaultDirectoryService();

        // The schemaManager
        directoryService.setSchemaManager( schemaManager );

        // MUST attributes
        // DirectoryService ID
        directoryService.setInstanceId( directoryServiceBean.getDirectoryServiceId() );

        // Replica ID
        directoryService.setReplicaId( directoryServiceBean.getDsReplicaId() );

        // WorkingDirectory
        directoryService.setInstanceLayout( instanceLayout );

        // Interceptors
        List<Interceptor> interceptors = createInterceptors( directoryServiceBean.getInterceptors() );
        directoryService.setInterceptors( interceptors );

        // Partitions
        Map<String, Partition> partitions = createPartitions( directoryService, directoryServiceBean.getPartitions() );

        Partition systemPartition = partitions.remove( "system" );

        if ( systemPartition == null )
        {
            throw new Exception( I18n.err( I18n.ERR_505 ) );
        }

        directoryService.setSystemPartition( systemPartition );
        directoryService.setPartitions( new HashSet<Partition>( partitions.values() ) );

        // MAY attributes
        // AccessControlEnabled
        directoryService.setAccessControlEnabled( directoryServiceBean.isDsAccessControlEnabled() );

        // AllowAnonymousAccess
        directoryService.setAllowAnonymousAccess( directoryServiceBean.isDsAllowAnonymousAccess() );

        // ChangeLog
        ChangeLog cl = createChangeLog( directoryServiceBean.getChangeLog() );

        if ( cl != null )
        {
            directoryService.setChangeLog( cl );
        }

        // DenormalizedOpAttrsEnabled
        directoryService.setDenormalizeOpAttrsEnabled( directoryServiceBean.isDsDenormalizeOpAttrsEnabled() );

        // Journal
        Journal journal = createJournal( directoryServiceBean.getJournal() );

        if ( journal != null )
        {
            directoryService.setJournal( journal );
        }

        // PasswordHidden
        directoryService.setPasswordHidden( directoryServiceBean.isDsPasswordHidden() );

        // SyncPeriodMillis
        directoryService.setSyncPeriodMillis( directoryServiceBean.getDsSyncPeriodMillis() );

        // testEntries
        String entryFilePath = directoryServiceBean.getDsTestEntries();

        if ( entryFilePath != null )
        {
            directoryService.setTestEntries( readTestEntries( entryFilePath ) );
        }

        // Enabled
        // if ( !directoryServiceBean.isEnabled() )
        // TODO will only be useful if we ever allow more than one DS to be configured and
        // switch between them decide which one to use based on this flag

        return directoryService;
    }


    public static MavibotPartition createMavibotPartition( DirectoryService directoryService,
        MavibotPartitionBean mvbtPartitionBean ) throws ConfigurationException
    {
        if ( ( mvbtPartitionBean == null ) || mvbtPartitionBean.isDisabled() )
        {
            return null;
        }

        MavibotPartition mvbtPartition = new MavibotPartition( directoryService.getSchemaManager(),
            directoryService.getDnFactory() );

        mvbtPartition.setId( mvbtPartitionBean.getPartitionId() );
        File partitionPath = new File( directoryService.getInstanceLayout().getPartitionsDirectory(),
            mvbtPartitionBean.getPartitionId() );
        mvbtPartition.setPartitionPath( partitionPath.toURI() );

        try
        {
            mvbtPartition.setSuffixDn( mvbtPartitionBean.getPartitionSuffix() );
        }
        catch ( LdapInvalidDnException lide )
        {
            String message = "Cannot set the Dn " + mvbtPartitionBean.getPartitionSuffix() + ", " + lide.getMessage();
            LOG.error( message );
            throw new ConfigurationException( message );
        }

        mvbtPartition.setSyncOnWrite( mvbtPartitionBean.isPartitionSyncOnWrite() );
        mvbtPartition.setIndexedAttributes( createMavibotIndexes( mvbtPartition, mvbtPartitionBean.getIndexes(),
            directoryService ) );

        setContextEntry( mvbtPartitionBean, mvbtPartition );

        return mvbtPartition;
    }


    /**
     * Create the list of MavibotIndex from the configuration
     */
    private static Set<Index<?, String>> createMavibotIndexes( MavibotPartition partition,
        List<IndexBean> indexesBeans,
        DirectoryService directoryService ) //throws Exception
    {
        Set<Index<?, String>> indexes = new HashSet<>();

        for ( IndexBean indexBean : indexesBeans )
        {
            if ( indexBean.isEnabled() && ( indexBean instanceof MavibotIndexBean ) )
            {
                indexes.add( createMavibotIndex( partition, ( MavibotIndexBean ) indexBean, directoryService ) );
            }
        }

        return indexes;
    }


    /**
     * Create a new instance of a MavibotIndex from an instance of MavibotIndexBean
     * 
     * @param partition The Mavibot partition instance
     * @param mavibotIndexBean The MavibotIndexBean to convert
     * @param directoryService The DirectoryService instance
     * @return An MavibotIndex instance
     */
    public static MavibotIndex<?> createMavibotIndex( MavibotPartition partition,
        MavibotIndexBean mavibotIndexBean, DirectoryService directoryService )
    {
        if ( ( mavibotIndexBean == null ) || mavibotIndexBean.isDisabled() )
        {
            return null;
        }

        MavibotIndex<?> index = null;

        boolean hasReverse = mavibotIndexBean.getIndexHasReverse();

        if ( mavibotIndexBean.getIndexAttributeId().equalsIgnoreCase( ApacheSchemaConstants.APACHE_RDN_AT )
            || mavibotIndexBean.getIndexAttributeId().equalsIgnoreCase( ApacheSchemaConstants.APACHE_RDN_AT_OID ) )
        {
            index = new MavibotRdnIndex();
        }
        else if ( mavibotIndexBean.getIndexAttributeId().equalsIgnoreCase( ApacheSchemaConstants.APACHE_ALIAS_AT )
            || mavibotIndexBean.getIndexAttributeId().equalsIgnoreCase( ApacheSchemaConstants.APACHE_ALIAS_AT_OID ) )
        {
            index = new MavibotDnIndex( ApacheSchemaConstants.APACHE_ALIAS_AT_OID );
        }
        else
        {
            index = new MavibotIndex<>( mavibotIndexBean.getIndexAttributeId(), hasReverse );
        }

        index.setWkDirPath( partition.getPartitionPath() );

        return index;
    }


    /**
     * Sets the configured context entry if present in the given partition bean 
     *
     * @param bean the partition configuration bean
     * @param partition the partition instance
     * @throws ConfigurationException
     */
    private static void setContextEntry( PartitionBean bean, AbstractPartition partition )
        throws ConfigurationException
    {
        String contextEntry = bean.getContextEntry();

        if ( contextEntry != null )
        {
            try
            {
                // Replace '\n' to real LF
                String entryStr = contextEntry.replaceAll( "\\\\n", "\n" );

                try ( LdifReader ldifReader = new LdifReader( partition.getSchemaManager() ) )
                {
                    List<LdifEntry> entries = ldifReader.parseLdif( entryStr );
    
                    if ( ( entries != null ) && !entries.isEmpty() )
                    {
                        LdifEntry entry = entries.get( 0 );
                        partition.setContextEntry( entry.getEntry() );
                    }
                }
                catch ( IOException ioe )
                {
                    LOG.error( "Cannot close the ldif reader" );
                }
            }
            catch ( LdapLdifException lle )
            {
                String message = "Cannot parse the context entry : " + contextEntry + ", " + lle.getMessage();
                LOG.error( message );
                throw new ConfigurationException( message );
            }
        }
    }
}
