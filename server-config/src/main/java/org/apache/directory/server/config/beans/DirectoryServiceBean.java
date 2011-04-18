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
package org.apache.directory.server.config.beans;


import java.util.ArrayList;
import java.util.List;

import org.apache.directory.server.config.ConfigurationElement;


/**
 * A class used to store the DirectoryService configuration.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class DirectoryServiceBean extends AdsBaseBean
{
    /** The DS instance Id */
    @ConfigurationElement(attributeType = "ads-directoryServiceId", isRdn = true)
    private String directoryServiceId;

    /** The directory instance replication ID */
    @ConfigurationElement(attributeType = "ads-dsReplicaId")
    private int dsReplicaId;

    /** The flag that tells if the AccessControl system is activated */
    @ConfigurationElement(attributeType = "ads-dsAccessControlEnabled")
    private boolean dsAccessControlEnabled = true;

    /** The flag that tells if Anonymous connections are allowed */
    @ConfigurationElement(attributeType = "ads-dsAllowAnonymousAccess")
    private boolean dsAllowAnonymousAccess = false;

    /** The flag that tells if Dn must be denormalized */
    @ConfigurationElement(attributeType = "ads-dsDenormalizeOpAttrsEnabled")
    private boolean dsDenormalizeOpAttrsEnabled = true;

    /** The maximum size of an incoming PDU */
    @ConfigurationElement(attributeType = "ads-dsMaxPDUSize")
    private int dsMaxPDUSize = 2048;

    /** The flag that tells if the password should be returned as a normal attribute or not */
    @ConfigurationElement(attributeType = "ads-dsPasswordHidden", isOptional = true, defaultValue = "false")
    private boolean dsPasswordHidden = false;

    /** The delay between two flushes on disk */
    @ConfigurationElement(attributeType = "ads-dsSyncPeriodMillis")
    private long dsSyncPeriodMillis = 15000L;

    /** The ldif entries to inject into the server at startup */
    @ConfigurationElement(attributeType = "dsTestEntries")
    private String dsTestEntries;

    /** The ChangeLog component */
    @ConfigurationElement
    private ChangeLogBean changeLog;

    /** The journal component */
    @ConfigurationElement
    private JournalBean journal;

    /** The servers */
    @ConfigurationElement(attributeType = "ads-servers", container = "servers")
    private List<ServerBean> servers = new ArrayList<ServerBean>();

    /** The list of declared interceptors */
    @ConfigurationElement(attributeType = "ads-interceptors", container = "interceptors")
    private List<InterceptorBean> interceptors = new ArrayList<InterceptorBean>();

    /** The set of associated partitions */
    @ConfigurationElement(attributeType = "ads-partitions", container = "partitions")
    private List<PartitionBean> partitions = new ArrayList<PartitionBean>();

    /** The reference to the Password Policy component */
    @ConfigurationElement(attributeType = "ads-passwordPolicies", container = "passwordPolicies")
    private List<PasswordPolicyBean> passwordPolicies = new ArrayList<PasswordPolicyBean>();


    /**
     * Create a new DnsServerBean instance
     */
    public DirectoryServiceBean()
    {
    }


    /**
     * Sets the ID for this DirectoryService
     * @param directoryServiceId The DirectoryService ID
     */
    public void setDirectoryServiceId( String directoryServiceId )
    {
        this.directoryServiceId = directoryServiceId;
    }


    /**
     * @return The DirectoryService Id
     */
    public String getDirectoryServiceId()
    {
        return directoryServiceId;
    }


    /**
     * @return the replicaId
     */
    public int getDsReplicaId()
    {
        return dsReplicaId;
    }


    /**
     * @param dsReplicaId the replicaId to set
     */
    public void setDsReplicaId( int dsReplicaId )
    {
        if ( ( dsReplicaId < 0 ) || ( dsReplicaId > 999 ) )
        {
            this.dsReplicaId = 0;
        }
        else
        {
            this.dsReplicaId = dsReplicaId;
        }
    }


    /**
     * Returns interceptors in the server.
     *
     * @return the interceptors in the server.
     */
    public List<InterceptorBean> getInterceptors()
    {
        return interceptors;
    }


    /**
     * Sets the interceptors in the server.
     *
     * @param interceptors the interceptors to be used in the server.
     */
    public void setInterceptors( List<InterceptorBean> interceptors )
    {
        this.interceptors = interceptors;
    }


    /**
     * Adds the interceptors in the server.
     *
     * @param interceptors the interceptors to be added in the server.
     */
    public void addInterceptors( InterceptorBean... interceptors )
    {
        for ( InterceptorBean interceptor : interceptors )
        {
            this.interceptors.add( interceptor );
        }
    }


    /**
     * @return the dsAccessControlEnabled
     */
    public boolean isDsAccessControlEnabled()
    {
        return dsAccessControlEnabled;
    }


    /**
     * @param dsAccessControlEnabled the dsAccessControlEnabled to set
     */
    public void setDsAccessControlEnabled( boolean dsAccessControlEnabled )
    {
        this.dsAccessControlEnabled = dsAccessControlEnabled;
    }


    /**
     * @return the dsAllowAnonymousAccess
     */
    public boolean isDsAllowAnonymousAccess()
    {
        return dsAllowAnonymousAccess;
    }


    /**
     * @param dsAllowAnonymousAccess the dsAllowAnonymousAccess to set
     */
    public void setDsAllowAnonymousAccess( boolean dsAllowAnonymousAccess )
    {
        this.dsAllowAnonymousAccess = dsAllowAnonymousAccess;
    }


    /**
     * @return the dsDenormalizeOpAttrsEnabled
     */
    public boolean isDsDenormalizeOpAttrsEnabled()
    {
        return dsDenormalizeOpAttrsEnabled;
    }


    /**
     * @param dsDenormalizeOpAttrsEnabled the dsDenormalizeOpAttrsEnabled to set
     */
    public void setDsDenormalizeOpAttrsEnabled( boolean dsDenormalizeOpAttrsEnabled )
    {
        this.dsDenormalizeOpAttrsEnabled = dsDenormalizeOpAttrsEnabled;
    }


    /**
     * @return the dsMaxPDUSize
     */
    public int getDsMaxPDUSize()
    {
        return dsMaxPDUSize;
    }


    /**
     * @param dsMaxPDUSize the dsMaxPDUSize to set
     */
    public void setDsMaxPDUSize( int dsMaxPDUSize )
    {
        this.dsMaxPDUSize = dsMaxPDUSize;
    }


    /**
     * @return the dsPasswordHidden
     */
    public boolean isDsPasswordHidden()
    {
        return dsPasswordHidden;
    }


    /**
     * @param dsPasswordHidden the dsPasswordHidden to set
     */
    public void setDsPasswordHidden( boolean dsPasswordHidden )
    {
        this.dsPasswordHidden = dsPasswordHidden;
    }


    /**
     * @return the dsSyncPeriodMillis
     */
    public long getDsSyncPeriodMillis()
    {
        return dsSyncPeriodMillis;
    }


    /**
     * @param dsSyncPeriodMillis the dsSyncPeriodMillis to set
     */
    public void setDsSyncPeriodMillis( long dsSyncPeriodMillis )
    {
        this.dsSyncPeriodMillis = dsSyncPeriodMillis;
    }


    /**
     * @return the dsTestEntries
     */
    public String getDsTestEntries()
    {
        return dsTestEntries;
    }


    /**
     * @param dsTestEntries the dsTestEntries to set
     */
    public void setDsTestEntries( String dsTestEntries )
    {
        this.dsTestEntries = dsTestEntries;
    }


    /**
     * @return the ChangeLog
     */
    public ChangeLogBean getChangeLog()
    {
        return changeLog;
    }


    /**
     * @param cChangeLog the ChangeLog to set
     */
    public void setChangeLog( ChangeLogBean changeLog )
    {
        this.changeLog = changeLog;
    }


    /**
     * @return the journal
     */
    public JournalBean getJournal()
    {
        return journal;
    }


    /**
     * @param journal the journal to set
     */
    public void setJournal( JournalBean journal )
    {
        this.journal = journal;
    }


    /**
     * @return the partitions
     */
    public List<PartitionBean> getPartitions()
    {
        return partitions;
    }


    /**
     * @param partitions the partitions to set
     */
    public void setPartitions( List<PartitionBean> partitions )
    {
        this.partitions = partitions;
    }


    /**
     * @param partitions the partitions to add
     */
    public void addPartitions( PartitionBean... partitions )
    {
        for ( PartitionBean partition : partitions )
        {
            this.partitions.add( partition );
        }
    }


    /**
     * @return the servers
     */
    public List<ServerBean> getServers()
    {
        return servers;
    }


    /**
     * @return The LdapServerBean configuration
     */
    public LdapServerBean getLdapServerBean()
    {
        for ( ServerBean server : servers )
        {
            if ( server instanceof LdapServerBean )
            {
                return ( LdapServerBean ) server;
            }
        }

        return null;
    }


    /**
     * @return The NtpServerBean configuration
     */
    public NtpServerBean getNtpServerBean()
    {
        for ( ServerBean server : servers )
        {
            if ( server instanceof NtpServerBean )
            {
                return ( NtpServerBean ) server;
            }
        }

        return null;
    }


    /**
     * @return The DnsServerBean configuration
     */
    public DnsServerBean getDnsServerBean()
    {
        for ( ServerBean server : servers )
        {
            if ( server instanceof DnsServerBean )
            {
                return ( DnsServerBean ) server;
            }
        }

        return null;
    }


    /**
     * @return The DhcpServerBean configuration
     */
    public DhcpServerBean getDhcpServerBean()
    {
        for ( ServerBean server : servers )
        {
            if ( server instanceof DhcpServerBean )
            {
                return ( DhcpServerBean ) server;
            }
        }

        return null;
    }


    /**
     * @return The HttpServerBean configuration
     */
    public HttpServerBean getHttpServerBean()
    {
        for ( ServerBean server : servers )
        {
            if ( server instanceof HttpServerBean )
            {
                return ( HttpServerBean ) server;
            }
        }

        return null;
    }


    /**
     * @return The KdcServerBean configuration
     */
    public KdcServerBean getKdcServerBean()
    {
        for ( ServerBean server : servers )
        {
            if ( server instanceof KdcServerBean )
            {
                return ( KdcServerBean ) server;
            }
        }

        return null;
    }


    /**
     * @return The ChangePasswordServerBean configuration
     */
    public ChangePasswordServerBean getChangePasswordServerBean()
    {
        for ( ServerBean server : servers )
        {
            if ( server instanceof ChangePasswordServerBean )
            {
                return ( ChangePasswordServerBean ) server;
            }
        }

        return null;
    }


    /**
     * @param servers the servers to set
     */
    public void setServers( List<ServerBean> servers )
    {
        this.servers = servers;
    }


    /**
     * @param servers the servers to add
     */
    public void addServers( ServerBean... servers )
    {
        for ( ServerBean server : servers )
        {
            this.servers.add( server );
        }
    }


    /**
     * @return the passwordPolicies
     */
    public List<PasswordPolicyBean> getPasswordPolicies()
    {
        return passwordPolicies;
    }


    /**
     * @param passwordPolicies the pwdPolicies to set
     */
    public void setPasswordPolicies( List<PasswordPolicyBean> passwordPolicies )
    {
        this.passwordPolicies = passwordPolicies;
    }


    /**
     * @param ppolicies the password policies to add
     */
    public void addPasswordPolicies( PasswordPolicyBean... ppolicies )
    {
        for ( PasswordPolicyBean ppolicy : ppolicies )
        {
            this.passwordPolicies.add( ppolicy );
        }
    }

    
    /**
     * {@inheritDoc}
     */
    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        sb.append( "DirectoryServiceBean : \n" );
        sb.append( super.toString( "  " ) );

        // Dump the must attributes
        sb.append( "  directoryService ID : " ).append( directoryServiceId ).append( '\n' );
        sb.append( "  replica ID : " ).append( dsReplicaId ).append( '\n' );
        sb.append( toString( "  ", "accessControl enabled", dsAccessControlEnabled ) );
        sb.append( toString( "  ", "allow anonymous access", dsAllowAnonymousAccess ) );
        sb.append( toString( "  ", "denormalized attributes enabled", dsDenormalizeOpAttrsEnabled ) );
        sb.append( toString( "  ", "password hidden", dsPasswordHidden ) );
        sb.append( "  max PDU size : " ).append( dsMaxPDUSize ).append( '\n' );
        sb.append( "  sync period millisecond : " ).append( dsSyncPeriodMillis ).append( '\n' );
        sb.append( toString( "  ", "test entries", dsTestEntries ) );

        sb.append( "  interceptors : \n" );

        if ( ( interceptors != null ) && ( interceptors.size() > 0 ) )
        {
            for ( InterceptorBean interceptor : interceptors )
            {
                sb.append( interceptor.toString( "    " ) );
            }
        }

        sb.append( "  partitions : \n" );

        if ( ( partitions != null ) && ( partitions.size() > 0 ) )
        {
            for ( PartitionBean partition : partitions )
            {
                sb.append( partition.toString( "    " ) );
            }
        }

        if ( journal != null )
        {
            sb.append( journal.toString( "  " ) );
        }

        if ( changeLog != null )
        {
            sb.append( changeLog.toString( "  " ) );
        }

        if ( ( passwordPolicies != null ) && ( passwordPolicies.size() > 0 ) )
        {
            for ( PasswordPolicyBean ppolicy : passwordPolicies )
            {
                sb.append( ppolicy.toString( "    " ) );
            }
        }

        sb.append( "  servers : \n" );

        if ( ( servers != null ) && ( servers.size() > 0 ) )
        {
            for ( ServerBean server : servers )
            {
                sb.append( server.toString( "    " ) );
            }
        }

        sb.append( '\n' );

        return sb.toString();
    }
}
