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


import org.apache.eve.session.ClientSession ;


/**
 * Denotes the destruction of a client session which does not necessarily 
 * coincide with the loss of a socket connection.  Attempts to rebind to
 * the directory may destroy an existing session and create another one without
 * droping the socket connection.
 * 
 * @author <a href="mailto:directory-dev@incubator.apache.org">
 * Apache Directory Project</a>
 * @version $Rev$
 */
public class SessionDestructionEvent extends ClientEvent 
{
    /** the newly created client session */
    private final ClientSession m_session ;
    
    
    /**
     * Creates a new event using a source, a client and the destroyed client
     * session object.
     * 
     * @param source the source that created this event
     * @param session the newly created client session
     */
    public SessionDestructionEvent( Object source, ClientSession session )
    {
        super( source, session.getClientKey() ) ;
        m_session = session ;
    }


    /**
     * Gets the destroyed client session object.
     * 
     * @return the destroyed session object
     */
    public ClientSession getClientSession()
    {
        return m_session ;
    }
}

