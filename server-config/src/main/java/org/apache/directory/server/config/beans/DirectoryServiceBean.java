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


/**
 * A class used to store the DirectoryService configuration.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class DirectoryServiceBean extends AdsBaseBean
{
    /** The DS instance Id */
    private String directoryserviceid;

    /** The directory instance replication ID */
    private int dsreplicaid;

    /** The flag that tells if the AccessControl system is activated */
    private boolean dsaccesscontrolenabled = true;

    /** The flag that tells if Anonymous connections are allowed */
    private boolean dsallowanonymousaccess = false;

    /** The flag that tells if DN must be denormalized */
    private boolean dsdenormalizeopattrsenabled = true;

    /** The maximum size of an incoming PDU */
    private int dsmaxpdusize = 2048;

    /** The flag that tells if the password should be returned as a normal attribute or not */
    private boolean dspasswordhidden = true;

    /** The delay between two flushes on disk */
    private long dssyncperiodmillis = 15000L;

    /** The ldif entries to inject into the server at startup */
    private String dstestentries;

    /** The ChangeLog component */
    private ChangeLogBean changelog;

    /** The journal component */
    private JournalBean journal;

    /** The servers */
    private List<ServerBean> servers = new ArrayList<ServerBean>();

    /** The list of declared interceptors */
    private List<InterceptorBean> interceptors = new ArrayList<InterceptorBean>();

    /** The set of associated partitions */
    private List<PartitionBean> partitions = new ArrayList<PartitionBean>();

    /** The reference to the Password Policy component */
    private PasswordPolicyBean passwordpolicy;

    /** The working directory */
    private String dsworkingdirectory;


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
        this.directoryserviceid = directoryServiceId;
    }


    /**
     * @return The DirectoryService Id
     */
    public String getDirectoryServiceId()
    {
        return directoryserviceid;
    }


    /**
     * @return the replicaId
     */
    public int getDsReplicaId()
    {
        return dsreplicaid;
    }


    /**
     * @param dsReplicaId the replicaId to set
     */
    public void setDsReplicaId( int dsReplicaId )
    {
        if ( ( dsReplicaId < 0 ) || ( dsReplicaId > 999 ) )
        {
            this.dsreplicaid = 0;
        }
        else
        {
            this.dsreplicaid = dsReplicaId;
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
    public void addinterceptors( InterceptorBean... interceptors )
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
        return dsaccesscontrolenabled;
    }


    /**
     * @param dsAccessControlEnabled the dsAccessControlEnabled to set
     */
    public void setDsAccessControlEnabled( boolean dsAccessControlEnabled )
    {
        this.dsaccesscontrolenabled = dsAccessControlEnabled;
    }


    /**
     * @return the dsAllowAnonymousAccess
     */
    public boolean isDsAllowAnonymousAccess()
    {
        return dsallowanonymousaccess;
    }


    /**
     * @param dsAllowAnonymousAccess the dsAllowAnonymousAccess to set
     */
    public void setDsAllowAnonymousAccess( boolean dsAllowAnonymousAccess )
    {
        this.dsallowanonymousaccess = dsAllowAnonymousAccess;
    }


    /**
     * @return the dsDenormalizeOpAttrsEnabled
     */
    public boolean isDsDenormalizeOpAttrsEnabled()
    {
        return dsdenormalizeopattrsenabled;
    }


    /**
     * @param dsDenormalizeOpAttrsEnabled the dsDenormalizeOpAttrsEnabled to set
     */
    public void setDsDenormalizeOpAttrsEnabled( boolean dsDenormalizeOpAttrsEnabled )
    {
        this.dsdenormalizeopattrsenabled = dsDenormalizeOpAttrsEnabled;
    }


    /**
     * @return the dsMaxPDUSize
     */
    public int getDsMaxPDUSize()
    {
        return dsmaxpdusize;
    }


    /**
     * @param dsMaxPDUSize the dsMaxPDUSize to set
     */
    public void setDsMaxPDUSize( int dsMaxPDUSize )
    {
        this.dsmaxpdusize = dsMaxPDUSize;
    }


    /**
     * @return the dsPasswordHidden
     */
    public boolean isDsPasswordHidden()
    {
        return dspasswordhidden;
    }


    /**
     * @param dsPasswordHidden the dsPasswordHidden to set
     */
    public void setDsPasswordHidden( boolean dsPasswordHidden )
    {
        this.dspasswordhidden = dsPasswordHidden;
    }


    /**
     * @return the dsSyncPeriodMillis
     */
    public long getDsSyncPeriodMillis()
    {
        return dssyncperiodmillis;
    }


    /**
     * @param dsSyncPeriodMillis the dsSyncPeriodMillis to set
     */
    public void setDsSyncPeriodMillis( long dsSyncPeriodMillis )
    {
        this.dssyncperiodmillis = dsSyncPeriodMillis;
    }


    /**
     * @return the dsTestEntries
     */
    public String getDsTestEntries()
    {
        return dstestentries;
    }


    /**
     * @param dsTestEntries the dsTestEntries to set
     */
    public void setDsTestEntries( String dsTestEntries )
    {
        this.dstestentries = dsTestEntries;
    }


    /**
     * @return the ChangeLog
     */
    public ChangeLogBean getChangeLog()
    {
        return changelog;
    }


    /**
     * @param cChangeLog the ChangeLog to set
     */
    public void setChangeLog( ChangeLogBean changeLog )
    {
        this.changelog = changeLog;
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
    public void addpartitions( PartitionBean... partitions )
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
     * @param servers the servers to set
     */
    public void setServers( List<ServerBean> servers )
    {
        this.servers = servers;
    }


    /**
     * @param servers the servers to add
     */
    public void addservers( ServerBean... servers )
    {
        for ( ServerBean server : servers )
        {
            this.servers.add( server );
        }
    }

    
    /**
     * @return the passwordPolicy
     */
    public PasswordPolicyBean getPasswordPolicy()
    {
        return passwordpolicy;
    }


    /**
     * @param passwordPolicy the passwordPolicy to set
     */
    public void setPasswordPolicy( PasswordPolicyBean passwordPolicy )
    {
        this.passwordpolicy = passwordPolicy;
    }


    /**
     * @return the dsWorkingDirectory
     */
    public String getDsWorkingDirectory()
    {
        return dsworkingdirectory;
    }


    /**
     * @param dsWorkingDirectory the dsWorkingDirectory to set
     */
    public void setDsWorkingDirectory( String dsWorkingDirectory )
    {
        this.dsworkingdirectory = dsWorkingDirectory;
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
        sb.append( "  directoryService ID : " ).append( directoryserviceid ).append( '\n' );
        sb.append( "  replica ID : " ).append( dsreplicaid ).append( '\n' );
        sb.append( "  working directory : " ).append( dsworkingdirectory ).append( '\n' );
        sb.append( toString( "  ", "accessControl enabled", dsaccesscontrolenabled ) );
        sb.append( toString( "  ", "allow anonymous access", dsallowanonymousaccess ) );
        sb.append( toString( "  ", "denormalized attributes enabled", dsdenormalizeopattrsenabled ) );
        sb.append( toString( "  ", "password hidden", dspasswordhidden ) );
        sb.append( "  max PDU size : " ).append( dsmaxpdusize ).append( '\n' );
        sb.append( "  sync period millisecond : " ).append( dssyncperiodmillis ).append( '\n' );
        sb.append( toString( "  ", "test entries", dstestentries ) );
        
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
            sb.append( "  journal : \n" );
            sb.append( journal.toString( "    " ) );
        }
        
        if ( changelog != null )
        {
            sb.append( changelog.toString( "    " ) );
        }
        
        if ( passwordpolicy != null )
        {
            sb.append( passwordpolicy.toString( "  " ) );
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
