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
package org.apache.directory.server.configuration;


import java.io.File;
import java.util.List;
import java.util.Set;

import org.apache.directory.server.changepw.ChangePasswordConfiguration;
import org.apache.directory.server.core.configuration.PartitionConfiguration;
import org.apache.directory.server.core.configuration.AuthenticatorConfiguration;
import org.apache.directory.server.core.interceptor.Interceptor;
import org.apache.directory.server.dns.DnsConfiguration;
import org.apache.directory.server.kerberos.kdc.KdcConfiguration;
import org.apache.directory.server.ldap.LdapConfiguration;
import org.apache.directory.server.ntp.NtpConfiguration;
import org.apache.directory.server.protocol.shared.store.LdifLoadFilter;
import org.apache.directory.shared.ldap.ldif.Entry;


/**
 * A mutable version of {@link ServerStartupConfiguration}.
 *
 * @org.apache.xbean.XBean
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class MutableServerStartupConfiguration extends ServerStartupConfiguration
{
    private static final long serialVersionUID = 515104910980600099L;


    /**
     * Creates a new instance of MutableServerStartupConfiguration.
     */
    public MutableServerStartupConfiguration()
    {
        super();
    }


    /**
     * Creates a new instance of MutableServerStartupConfiguration.
     *
     * @param instanceId
     */
    public MutableServerStartupConfiguration( String instanceId )
    {
        super( instanceId );
    }


    public void setSystemPartitionConfiguration( PartitionConfiguration systemPartitionConfiguration )
    {
        super.setSystemPartitionConfiguration( systemPartitionConfiguration );
    }


    public void setMaxThreads( int maxThreads )
    {
        super.setMaxThreads( maxThreads );
    }


    public void setMaxTimeLimit( int maxTimeLimit )
    {
        super.setMaxTimeLimit( maxTimeLimit );
    }


    public void setMaxSizeLimit( int maxSizeLimit )
    {
        super.setMaxSizeLimit( maxSizeLimit );
    }


    public void setSynchPeriodMillis( long synchPeriodMillis )
    {
        super.setSynchPeriodMillis( synchPeriodMillis );
    }


    public void setAccessControlEnabled( boolean accessControlEnabled )
    {
        super.setAccessControlEnabled( accessControlEnabled );
    }


    public void setAllowAnonymousAccess( boolean arg0 )
    {
        super.setAllowAnonymousAccess( arg0 );
        getLdapConfiguration().setAllowAnonymousAccess( arg0 );
    }


    public void setDenormalizeOpAttrsEnabled( boolean denormalizeOpAttrsEnabled )
    {
        super.setDenormalizeOpAttrsEnabled( denormalizeOpAttrsEnabled );
    }

    /**
     * @org.apache.xbean.Property nestedType="org.apache.directory.server.core.configuration.AuthenticatorConfiguration"
     *
     * @param authenticatorConfigurations
     */
    public void setAuthenticatorConfigurations( Set<AuthenticatorConfiguration> authenticatorConfigurations )
    {
        super.setAuthenticatorConfigurations( authenticatorConfigurations );
    }

    /**
     * @org.apache.xbean.Property nestedType="org.apache.directory.server.core.configuration.PartitionConfiguration"
     *
     * @param partitionConfigurations
     */
    public void setPartitionConfigurations( Set<? extends PartitionConfiguration> partitionConfigurations )
    {
        super.setPartitionConfigurations( partitionConfigurations );
    }

    /**
     * @org.apache.xbean.Property nestedType="org.apache.directory.server.core.configuration.InterceptorConfiguration"
     *
     * @param interceptors
     */
    public void setInterceptors( List<Interceptor> interceptors )
    {
        super.setInterceptors( interceptors );
    }

    /**
     * @org.apache.xbean.Property nestedType="org.apache.directory.shared.ldap.ldif.Entry"
     *
     * @param entries
     */
    public void setTestEntries( List<? extends Entry> entries )
    {
        super.setTestEntries( entries );
    }


    public void setWorkingDirectory( File arg0 )
    {
        super.setWorkingDirectory( arg0 );
    }


    public void setLdifDirectory( File ldifDirectory )
    {
        super.setLdifDirectory( ldifDirectory );
    }

    /**
     * @org.apache.xbean.Property nestedType="org.apache.directory.server.protocol.shared.store.LdifLoadFilter"
     *
     * @param ldifFilters
     */
    public void setLdifFilters( List<LdifLoadFilter> ldifFilters )
    {
        super.setLdifFilters( ldifFilters );
    }


    public void setShutdownHookEnabled( boolean shutdownHookEnabled )
    {
        super.setShutdownHookEnabled( shutdownHookEnabled );
    }


    public void setExitVmOnShutdown( boolean exitVmOnShutdown )
    {
        super.setExitVmOnShutdown( exitVmOnShutdown );
    }


    public void setKdcConfiguration( KdcConfiguration kdcConfiguration )
    {
        super.setKdcConfiguration( kdcConfiguration );
    }


    public void setLdapConfiguration( LdapConfiguration ldapConfiguration )
    {
        super.setLdapConfiguration( ldapConfiguration );
    }


    public void setLdapsConfiguration( LdapConfiguration ldapsConfiguration )
    {
        super.setLdapsConfiguration( ldapsConfiguration );
    }


    public void setNtpConfiguration( NtpConfiguration ntpConfiguration )
    {
        super.setNtpConfiguration( ntpConfiguration );
    }


    public void setChangePasswordConfiguration( ChangePasswordConfiguration changePasswordConfiguration )
    {
        super.setChangePasswordConfiguration( changePasswordConfiguration );
    }


    public void setDnsConfiguration( DnsConfiguration dnsConfiguration )
    {
        super.setDnsConfiguration( dnsConfiguration );
    }
}
