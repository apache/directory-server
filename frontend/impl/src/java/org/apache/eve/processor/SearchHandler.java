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


import java.util.Iterator;

import org.apache.commons.lang.NotImplementedException;

import org.apache.eve.processor.HandlerTypeEnum ;
import org.apache.eve.processor.ManyReplyHandler ;

import org.apache.ldap.common.message.ResultResponse ;
import org.apache.ldap.common.message.MessageTypeEnum ;
import org.apache.ldap.common.message.ManyReplyRequest ;
import org.apache.ldap.common.message.SearchResponseDoneImpl ;


/**
 * SearchRequest handler.
 * 
 * @author <a href="mailto:directory-dev@incubator.apache.org">
 * Apache Directory Project</a>
 * @version $Rev$
 */
public class SearchHandler implements ManyReplyHandler
{
    // ------------------------------------------------------------------------
    // Constructor
    // ------------------------------------------------------------------------


    /**
     * Creates a SearchRequest handler instance for use by a ProtocolModule.
     */
    public SearchHandler()
    {
    }


    // ------------------------------------------------------------------------
    // SearchHandler's Primary Handling Method
    // ------------------------------------------------------------------------


    /**
     * Specifically designed handler method for processing SearchRequests
     *
     * @param a_request the SearchRequest to handle
     */
    public Iterator handle( ManyReplyRequest a_request )
    {
        throw new NotImplementedException( "STUB" ) ;
    }
    
    
    public ResultResponse getDoneResponse( int id )
    {
        return new SearchResponseDoneImpl( id ) ;
    }


    // ------------------------------------------------------------------------
    // RequestHandler Interface Method Implementations
    // ------------------------------------------------------------------------


    /**
     * Gets the handler type for this RequestHandler.
     *
     * @return HandlerTypeEnum.SEARCH always
     */
    public HandlerTypeEnum getHandlerType()
    {
        return HandlerTypeEnum.SEARCH ;
    }


    /**
     * Gets the message type this handler is designed to respond to.
     *
     * @return MessageTypeEnum.SEARCHREQUEST always.
     */
    public MessageTypeEnum getRequestType()
    {
        return MessageTypeEnum.SEARCHREQUEST ;
    }
}
