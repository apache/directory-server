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
import org.apache.ldap.common.message.Response ;


/**
 * An event used to denote the response to a client request.  The response event
 * only connotates that a response was made not delivered.  The delivery is an
 * output event.
 *
 * @author <a href="mailto:directory-dev@incubator.apache.org">
 * Apache Directory Project</a>
 * @version $Rev$
 */
public class ResponseEvent extends ClientEvent
{
    /** the LDAP response message */
    private final Response response ;

    
    /**
     * Creates an event used to denote the response to a request before it is
     * delivered.
     *  
     * @param source the source that created this event
     * @param clientKey the key of the client associated with this event
     * @param response the LDAP response message
     */
    public ResponseEvent( Object source, ClientKey clientKey, 
                          Response response )
    {
        super( source, clientKey ) ;
        this.response = response ;
    }
    
    
    /**
     * Gets the LDAP response message associated with this event.
     * 
     * @return the LDAP response message associated with this event
     */
    public Response getResponse()
    {
        return response ;
    }
}
