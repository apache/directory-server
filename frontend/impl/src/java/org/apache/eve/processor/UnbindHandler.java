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
package org.apache.eve.processor ;


import org.apache.commons.lang.NotImplementedException;
import org.apache.eve.processor.NoReplyHandler ;
import org.apache.eve.processor.HandlerTypeEnum ;

import org.apache.ldap.common.message.Request ;
import org.apache.ldap.common.message.MessageTypeEnum ;


/**
 * Handles the processing of UnbindRequests.  Not presently implemented.
 * 
 * @author <a href="mailto:directory-dev@incubator.apache.org">
 * Apache Directory Project</a>
 * @version $Rev$
 */
public class UnbindHandler implements NoReplyHandler
{
    // ------------------------------------------------------------------------
    // Constructors
    // ------------------------------------------------------------------------


    /**
     * Creates a handler for UnbindRequests to work on behalf of a
     * ProtocolModule.
     */
	public UnbindHandler()
    {
    }


    // ------------------------------------------------------------------------
    // RequestHandler Interface Method Implementations
    // ------------------------------------------------------------------------


    /**
     * Gets the handler type for this handler which will always be the
     * HandlerTypeEnum.NOREPLY enumeration constant.
     *
     * @return the HandlerTypeEnum.NOREPLY enumeration constant
     */
	public final HandlerTypeEnum getHandlerType()
    {
        return HandlerTypeEnum.NOREPLY ;
    }


    /**
     * Gets the message type handled by this handler which will always be the
     * MessageTypeEnum.UNBINDREQUEST enumeration constant.
     *
     * @return the MessageTypeEnum.UNBINDREQUEST enumeration constant
     */
	public MessageTypeEnum getRequestType()
    {
        return MessageTypeEnum.UNBINDREQUEST ;
    }


    /**
     * Handles an unbind request by disconnecting a client and terminating their
     * session on the server.
     *
     * @param a_request the UnbindRequest.
     * @throws ClassCastException if the a_request argument is not an
     * UnbindRequest
     */
    public void handle( Request a_request )
    {
        throw new NotImplementedException( "STUB" ) ;
    }
}
