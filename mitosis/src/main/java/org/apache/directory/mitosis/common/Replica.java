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
package org.apache.directory.mitosis.common;

import java.net.InetSocketAddress;

import org.apache.directory.shared.ldap.util.StringTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The class stores a Replica, which is composed of an Id, a server and a port.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class Replica
{
    /** The logger */
    private static Logger log = LoggerFactory.getLogger( Replica.class );

    /** A speedup for logger */
    private static final boolean IS_DEBUG = log.isDebugEnabled();

    /** The replicaId */
    private final ReplicaId id;
    
    /** The server address */
    private final InetSocketAddress address;
    
    /**
     * Creates a new instance of Replica, from a String.
     * 
     * The String format is the following :
     * 
     * <replicaId>@<server>:<port>
     * 
     * @param The replica to create
     */
    public Replica( String replica )
    {
        if ( StringTools.isEmpty( replica ) )
        {
            log.error( "Null or empty replica are not allowed" );
            throw new IllegalArgumentException( "Null or empty Replica " );
        }
        
        replica = replica.trim();
        
        int atPos = replica.indexOf( '@' );
        
        if ( atPos <= 0 )
        {
            log.error( "The ReplicaId '@' element is missing in {}", replica );
            throw new IllegalArgumentException( "Replica ID not found: " + replica );
        }
        
        int colonPos = replica.indexOf( ':', atPos );
        
        if ( colonPos < 0 )
        {
            log.error( "Replica port not found in {}", replica );
            throw new IllegalArgumentException( "Port number not found in replica : " + replica );
        }
        
        id = new ReplicaId( replica.substring( 0, atPos ) );
        String server = replica.substring( atPos + 1, colonPos );
        int port = -1;
        
        try
        {
            port = Integer.parseInt( replica.substring( colonPos + 1 ) ) ;
        }
        catch ( NumberFormatException nfe )
        {
            log.error( "The port value should be a value between 1 and 65535, port  : {}", new Integer( port ) );
            throw new IllegalArgumentException( "Bad port number : " + port );
        }
        
        try
        {
            address = new InetSocketAddress( server, port );
        }
        catch ( IllegalArgumentException iae )
        {
            log.error(  "The server address/name is invalid ({}) in replica {}", server, replica );
            throw new IllegalArgumentException( "The server address/name is invalid in replica " + replica 
                + ", error : " + iae.getMessage() );
        }
        
        if ( IS_DEBUG )
        {
            log.debug( "Created a replica {} on server {}", id, server + ':' + port );
        }
    }

    /**
     * Creates a new instance of Replica, from a valid Id and a valid address.
     *
     * @param id The Replica Id
     * @param address The server address.
     */
    public Replica( ReplicaId id, InetSocketAddress address )
    {
        assert id != null;
        assert address != null;

        this.id = id;
        this.address = address;
    }

    /**
     * @return the replica address
     */
    public InetSocketAddress getAddress()
    {
        return address;
    }

    /**
     * @return the replica Id
     */
    public ReplicaId getId()
    {
        return id;
    }
    
    public int hashCode()
    {
        return id.hashCode();
    }
    
    public boolean equals( Object o )
    {
        if( o == null )
        {
            return false;
        }
        
        if( o == this )
        {
            return true;
        }
        
        if( o instanceof Replica )
        {
            return this.id.equals( ( ( Replica ) o ).id );
        }
        else
        {
            return false;
        }
    }
    
    /**
     * @return The replica. The format is &lt;replica id> '@' &lt;server> ':' &lt;port>
     */
    public String toString()
    {
        return getId().toString() +
                '@' +
                getAddress().getAddress().getHostAddress() +
                ':' +
                getAddress().getPort();
    }
}
