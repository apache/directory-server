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
import java.util.ArrayList;
import java.util.List;

import org.apache.directory.server.changepw.ChangePasswordConfiguration;
import org.apache.directory.server.core.configuration.StartupConfiguration;
import org.apache.directory.server.kerberos.kdc.KdcConfiguration;
import org.apache.directory.server.ldap.LdapConfiguration;
import org.apache.directory.server.ntp.NtpConfiguration;
import org.apache.directory.server.protocol.shared.store.LdifLoadFilter;


/**
 * A {@link StartupConfiguration} that starts up ApacheDS with network layer support.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class ServerStartupConfiguration extends StartupConfiguration
{
    private static final long serialVersionUID = -7138616822614155454L;

    private static final long DEFAULT_SYNC_PERIOD_MILLIS = 20000;

    private long synchPeriodMillis = DEFAULT_SYNC_PERIOD_MILLIS;

    private boolean enableNetworking = true;

    private File ldifDirectory = null;
    private final List ldifFilters = new ArrayList();

    private KdcConfiguration kdcConfiguration = new KdcConfiguration();
    private LdapConfiguration ldapConfiguration = new LdapConfiguration();
    private LdapConfiguration ldapsConfiguration = new LdapConfiguration();
    private ChangePasswordConfiguration changePasswordConfiguration = new ChangePasswordConfiguration();
    private NtpConfiguration ntpConfiguration = new NtpConfiguration();


    protected ServerStartupConfiguration()
    {
    }


    protected ServerStartupConfiguration( String instanceId )
    {
        super( instanceId );
    }


    /**
     * Returns <tt>true</tt> if networking (LDAP, LDAPS, and Kerberos) is enabled.
     */
    public boolean isEnableNetworking()
    {
        return enableNetworking;
    }


    /**
     * Sets whether to enable networking (LDAP, LDAPS, and Kerberos) or not.
     */
    public void setEnableNetworking( boolean enableNetworking )
    {
        this.enableNetworking = enableNetworking;
    }


    public File getLdifDirectory()
    {
        return this.ldifDirectory;
    }


    protected void setLdifDirectory( File ldifDirectory )
    {
        this.ldifDirectory = ldifDirectory;
    }


    public List getLdifFilters()
    {
        return new ArrayList( ldifFilters );
    }


    protected void setLdifFilters( List filters )
    {
        for ( int ii = 0; ii < filters.size(); ii++ )
        {
            if ( !( filters.get( ii ) instanceof LdifLoadFilter ) )
            {
                throw new IllegalArgumentException(
                    "The specified filter collection contains an element which is not an LdifLoadFilter." );
            }
        }

        this.ldifFilters.clear();
        this.ldifFilters.addAll( filters );
    }


    protected void setSynchPeriodMillis( long synchPeriodMillis )
    {
        this.synchPeriodMillis = synchPeriodMillis;
    }


    public long getSynchPeriodMillis()
    {
        return synchPeriodMillis;
    }


    protected void setKdcConfiguration( KdcConfiguration kdcConfiguration )
    {
        this.kdcConfiguration = kdcConfiguration;
    }


    public KdcConfiguration getKdcConfiguration()
    {
        return kdcConfiguration;
    }


    protected void setLdapConfiguration( LdapConfiguration ldapConfiguration )
    {
        this.ldapConfiguration = ldapConfiguration;
    }


    public LdapConfiguration getLdapConfiguration()
    {
        return ldapConfiguration;
    }


    protected void setLdapsConfiguration( LdapConfiguration ldapsConfiguration )
    {
        this.ldapsConfiguration = ldapsConfiguration;
    }


    public LdapConfiguration getLdapsConfiguration()
    {
        return ldapsConfiguration;
    }


    protected void setNtpConfiguration( NtpConfiguration ntpConfiguration )
    {
        this.ntpConfiguration = ntpConfiguration;
    }


    public NtpConfiguration getNtpConfiguration()
    {
        return ntpConfiguration;
    }


    protected void setChangePasswordConfiguration( ChangePasswordConfiguration changePasswordConfiguration )
    {
        this.changePasswordConfiguration = changePasswordConfiguration;
    }


    public ChangePasswordConfiguration getChangePasswordConfiguration()
    {
        return changePasswordConfiguration;
    }
}
