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


import org.apache.commons.lang.NotImplementedException ;

import org.apache.eve.processor.HandlerTypeEnum ;
import org.apache.eve.processor.SingleReplyHandler ;

import org.apache.ldap.common.message.ResultResponse ;
import org.apache.ldap.common.message.MessageTypeEnum ;
import org.apache.ldap.common.message.SingleReplyRequest ;


/**
 * AddRequest handler for add protocol requests.
 * 
 * @author <a href="mailto:directory-dev@incubator.apache.org">
 * Apache Directory Project</a>
 * @version $Rev$
 */
public class AddHandler implements SingleReplyHandler
{
    // ------------------------------------------------------------------------
    // Constructors
    // ------------------------------------------------------------------------


    /**
     * Creates a AddRequest protocol data unit handler.
     */
    public AddHandler()
    {
    }


    // ------------------------------------------------------------------------
    // RequestHandler Interface Method Implementations
    // ------------------------------------------------------------------------


    /**
     * Gets the handler type.
     *
     * @return a HandlerTypeEnum constant.
     */
    public HandlerTypeEnum getHandlerType()
    {
        return HandlerTypeEnum.SINGLEREPLY ;
    }


    /**
     * Gets the request message type handled by this handler.
     *
     * @return a MessageTypeEnum constant associated with the request message.
     */
    public MessageTypeEnum getRequestType()
    {
		return MessageTypeEnum.ADDREQUEST ;
    }


    // ------------------------------------------------------------------------
    // RequestHandler Interface Method Implementations
    // ------------------------------------------------------------------------


    /**
     * Gets the response message type for this SingleReplyHandler.
     *
     * @return the MessageTypeEnum constant associated with this handler.
     */
    public MessageTypeEnum getResponseType()
    {
        return MessageTypeEnum.ADDRESPONSE ;
    }


    /**
     * Handles a request that generates a sole response by returning the
     * response object back to the caller.
     *
     * @param request the request to handle.
     * @return the response to the request argument.
     * @throws ClassCastException if request is not a AddRequest
     */
    public ResultResponse handle( SingleReplyRequest request )
    {
        throw new NotImplementedException( "STUB" ) ;
    }
}
