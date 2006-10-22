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
package org.apache.directory.mitosis.configuration;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.collections.set.TypedSet;
import org.apache.directory.mitosis.common.CSNFactory;
import org.apache.directory.mitosis.common.Replica;
import org.apache.directory.mitosis.common.ReplicaId;
import org.apache.directory.mitosis.common.SimpleCSNFactory;
import org.apache.directory.mitosis.common.SimpleUUIDFactory;
import org.apache.directory.mitosis.common.UUIDFactory;
import org.apache.directory.mitosis.store.ReplicationStore;
import org.apache.directory.mitosis.store.derby.DerbyReplicationStore;
import org.apache.directory.shared.ldap.util.StringTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReplicationConfiguration
{
    /** The logger */
    private static Logger log = LoggerFactory.getLogger( ReplicationConfiguration.class );

    private ReplicaId replicaId;
    private int serverPort = 7846;
    private int responseTimeout = 60;
    private final Set peerReplicas = TypedSet.decorate( new HashSet(), Replica.class );
    private UUIDFactory uuidFactory = new SimpleUUIDFactory();
    private CSNFactory csnFactory = new SimpleCSNFactory();
    private ReplicationStore store = new DerbyReplicationStore();
    private int logMaxAge = 7; // a week (days)

    public ReplicationConfiguration()
    {
    }
    
    public int getServerPort()
    {
        return serverPort;
    }

    public void setServerPort( int serverPort )
    {
        this.serverPort = serverPort;
    }

    public int getResponseTimeout()
    {
        return responseTimeout;
    }

    public void setResponseTimeout( int responseTimeout )
    {
        this.responseTimeout = responseTimeout;
    }

    public CSNFactory getCsnFactory()
    {
        return csnFactory;
    }
    
    public void setCsnFactory( CSNFactory csnFactory )
    {
        this.csnFactory = csnFactory;
    }
    
    public void addPeerReplica( Replica peer )
    {
        assert peer != null;
        peerReplicas.add( peer );
    }
    
    public void removePeerReplica( Replica peer )
    {
        assert peer != null;
        peerReplicas.remove( peer );
    }
    
    public void removeAllPeerReplicas()
    {
        peerReplicas.clear();
    }
    
    public Set getPeerReplicas()
    {
        Set result = new HashSet();
        result.addAll( peerReplicas );
        return result;
    }
    
    public void setPeerReplicas( Set replicas )
    {
        assert replicas != null;
        
        Set normalizedReplicas = new HashSet();
        Iterator i = replicas.iterator();
        while( i.hasNext() )
        {
            Object o = i.next();
            if( o instanceof Replica )
            {
                normalizedReplicas.add( o );
            }
            else
            {
                normalizedReplicas.add( new Replica( o.toString() ) );
            }
        }
        this.peerReplicas.clear();
        this.peerReplicas.addAll( normalizedReplicas );
    }
    
    public ReplicaId getReplicaId()
    {
        return replicaId;
    }
    
    public void setReplicaId( ReplicaId replicaId )
    {
        assert replicaId != null;
        this.replicaId = replicaId;
    }
    
    public ReplicationStore getStore()
    {
        return store;
    }
    
    public void setStore( ReplicationStore store )
    {
        this.store = store;
    }
    
    public UUIDFactory getUuidFactory()
    {
        return uuidFactory;
    }
    
    public void setUuidFactory( UUIDFactory uuidFactory )
    {
        this.uuidFactory = uuidFactory;
    }
    
    public int getLogMaxAge()
    {
        return logMaxAge;
    }
    
    public void setLogMaxAge( int logMaxAge )
    {
        if( logMaxAge <= 0 )
        {
            throw new ReplicationConfigurationException( "logMaxAge: " + logMaxAge );
        }
        
        this.logMaxAge = logMaxAge;
    }
    
    /**
     * Validate Mitosis configuration.
     * 
     * We check that the configuration file contains valid
     * parameters :
     *  - a replicaId
     *  - a valid server port (between 0 and 65535)
     *  - a valid response timeout ( > 0 )
     *  - a uuidFactory
     *  - a CSN factory
     *  - a store (derby)
     *  - a list of valid replica, none of them being equal
     *  to the replicaId 
     *
     * @throws ReplicationConfigurationException If the configuration file is invalid
     */
    public void validate() throws ReplicationConfigurationException
    {
        if ( replicaId == null )
        {
            log.error( "The replicaId is missing" );
            throw new ReplicationConfigurationException( "Replica ID is not specified." );
        }
        
        if ( serverPort < 0 || serverPort > 65535 )
        {
            log.error( "The replica port is not between 0 and 65535" );
            throw new ReplicationConfigurationException( "Server port is invalid: " + serverPort );
        }
        
        if ( responseTimeout <= 0 )
        {
            log.error( "The replica responsetimeout is negative" );
            throw new ReplicationConfigurationException( "Invalid response timeout: " + responseTimeout );
        }
        
        if( uuidFactory == null )
        {
            log.error( "The UUID factory has not been declared" );
            throw new ReplicationConfigurationException( "UUID factory is not specified." );
        }
        
        if( csnFactory == null )
        {
            log.error( "The CSN factory has not been declared" );
            throw new ReplicationConfigurationException( "CSN factory is not specified." );
        }
        
        if( store == null )
        {
            log.error( "The store has not been declared" );
            throw new ReplicationConfigurationException( "Replication store is not specified." );
        }
        
        if( peerReplicas.size() == 0 )
        {
            log.error( "The replicas peer list is empty" );
            throw new ReplicationConfigurationException( "No peer replicas" );
        }
        
        // Check the peer replicas.
        // We should check that no replica has the same Id, and that we don't
        // have two replicas on the same server with the same port
        Set ids = new TreeSet();
        Map servers = new HashMap();  
        
        // Initialize the set with this server replicaId
        ids.add( replicaId.getId() );
        
        // And store the local inetadress
        Integer localPort = new Integer( serverPort );
        servers.put( "localhost", localPort );
        servers.put( "127.0.0.1", localPort );
        
        try
        {
            servers.put( StringTools.lowerCase( InetAddress.getByName( "127.0.0.1" ).getHostName() ) , localPort );
        }
        catch ( UnknownHostException uhe )
        {
            // Should never occurs with 127.0.0.1
            throw new ReplicationConfigurationException( "Unknown host name" );
        }
        
        for ( Iterator peer = peerReplicas.iterator(); peer.hasNext(); )
        {
            Replica replica = ( Replica ) peer.next();
            
            if ( ids.contains( replica.getId() ) ) 
            {
                log.error( "Peer replica ID '{}' has already been declared.", replica.getId() );
                throw new ReplicationConfigurationException( "Peer replica ID '" + replica.getId() + "' has already been declared." );
            }
            
            // Now check that we don't already have a replica on a server with the same port 
            String replicaServer = StringTools.lowerCase( replica.getAddress().getHostName() );
            Integer replicaPort = new Integer( replica.getAddress().getPort() );
            
            if ( servers.containsKey( replicaServer ) )
            {
                Integer peerPort = ((Integer)servers.get( replicaServer ) );
                
                if ( replicaPort == peerPort )
                {
                    log.error( "The replica in the peer list has already been declared on the server {} with the port {}", replicaServer, peerPort );
                    throw new ReplicationConfigurationException( "Replication store is not specified." );
                }
            }
            
            servers.put( replicaServer, replicaPort );
        }
    }
}
