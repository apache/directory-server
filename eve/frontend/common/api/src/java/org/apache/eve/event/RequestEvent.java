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
import org.apache.ldap.common.message.Message ;


/**
 * An event used to denote the arrival of a client request.
 *
 * @author <a href="mailto:directory-dev@incubator.apache.org">
 * Apache Directory Project</a>
 * @version $Rev$
 */
public class RequestEvent extends ClientEvent
{
    /** the LDAP request message */
    private final Message request ;

    
    /**
     * Creates an event used to denote the arrival of a client request.
     *  
     * @param source the source that created this event
     * @param clientKey the key of the client associated with this event
     * @param request the LDAP request message
     */
    public RequestEvent( Object source, ClientKey clientKey, Message request )
    {
        super( source, clientKey ) ;
        this.request = request ; 
    }
    
    
    /**
     * Gets the LDAP request message associated with this event.
     * 
     * @return the LDAP request message associated with this event
     */
    public Message getRequest()
    {
        return request ;
    }
}
