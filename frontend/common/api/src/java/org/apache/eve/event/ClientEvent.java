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


import java.util.EventObject ;

import org.apache.eve.listener.ClientKey ;


/**
 * An event associated with a specific client. 
 * 
 * @author <a href="mailto:directory-dev@incubator.apache.org">
 * Apache Directory Project</a>
 * @version $Rev$
 */
public abstract class ClientEvent extends EventObject
{
    /** the unique client identifier */
    private final ClientKey m_clientKey ;
    
    
    /**
     * Creates a client based event using a unique client key.
     * 
     * @param source the source that generated this event
     * @param clientKey the client's read client key
     */
    public ClientEvent( Object source, ClientKey clientKey )
    {
        super( source ) ;
        m_clientKey = clientKey ;
    }


    /**
     * Gets the unique identifier for the client associated with this event.
     * 
     * @return the client's unique key
     */
    public final ClientKey getClientKey()
    {
        return m_clientKey ;
    }
}
