/*
 *   Copyright 2004 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package org.apache.eve.event ;


import org.apache.eve.listener.ClientKey ;


/**
 * Represents the acceptance by the server of a new client socket connection.
 * 
 * @author <a href="mailto:directory-dev@incubator.apache.org">
 * Apache Directory Project</a>
 * @version $Rev$
 */
public class DisconnectEvent extends ClientEvent
{
    /**
     * Creates a new disconnect event using the client key associated with the
     * client socket connection that was lost or dropped. 
     * 
     * @param source the object that created this event which in a server
     * would be a component reponsible for dropping or detecting client 
     * disconnections
     * @param clientKey the client socket connection
     */
    public DisconnectEvent( Object source, ClientKey clientKey )
    {
        super( source, clientKey ) ;
    }
}
