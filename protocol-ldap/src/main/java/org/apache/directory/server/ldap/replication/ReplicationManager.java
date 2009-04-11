
package org.apache.directory.server.ldap.replication;

import org.apache.mina.core.service.IoConnector;
import org.apache.mina.transport.socket.nio.NioSocketConnector;

public class ReplicationManager
{
    /** The connector use to connect to the replicas */
    IoConnector connector;
    
    /**
     * 
     * Creates a new instance of ReplicationManager. It initialize the 
     * connections to the replicas.
     *
     */
    public ReplicationManager()
    {
        // Create the connector
        connector = new NioSocketConnector();
        
        
    }
}
