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


import org.apache.eve.processor.NoReplyHandler ;
import org.apache.eve.processor.HandlerTypeEnum ;

import org.apache.ldap.common.message.Request ;
import org.apache.ldap.common.message.MessageTypeEnum ;

import org.apache.commons.lang.NotImplementedException ;


/**
 * Handles the processing of AbandonRequests.  Not presently implemented.
 * 
 * @author <a href="mailto:directory-dev@incubator.apache.org">
 * Apache Directory Project</a>
 * @version $Rev$
 */
public final class AbandonHandler implements NoReplyHandler
{


    // ------------------------------------------------------------------------
    // Constructors
    // ------------------------------------------------------------------------


    /**
     * Creates a handler for AbandonRequests to work on behalf of a
     * ProtocolModule.
     *
     * @param a_module the ProtocolModule this handler is part of.
     */
	public AbandonHandler()
    {
    }


    // ------------------------------------------------------------------------
    // RequestHandler Interface Method Implementations
    // ------------------------------------------------------------------------


    /**
     * Gets the handler type enumeration constant associated with this handler.
     *
     * @return HandlerTypeEnum.NOREPLY
     */
	public final HandlerTypeEnum getHandlerType()
    {
        return HandlerTypeEnum.NOREPLY ;
    }


    /**
     * Gets the message type enumeration constant associated with this handler.
     *
     * @return HandlerTypeEnum.NOREPLY
     */
	public final MessageTypeEnum getRequestType()
    {
        return MessageTypeEnum.ABANDONREQUEST ;
    }


    /**
     * Handles an AbandonRequest by stopping the outstanding request specified.
     *
     * @param a_request the AbandonRequest to handle.
     * @throws ClassCastException if the a_request argument is not an
     * AbandonRequest.
     */
    public final void handle( Request a_request )
    {
        throw new NotImplementedException( "STUB" ) ;
    }
}
