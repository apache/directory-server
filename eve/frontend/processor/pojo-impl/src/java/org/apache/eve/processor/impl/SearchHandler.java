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
package org.apache.eve.processor.impl ;


import org.apache.commons.lang.NotImplementedException;
import org.apache.eve.processor.RequestHandler ;
import org.apache.eve.processor.HandlerTypeEnum ;
import org.apache.ldap.common.message.SearchRequest ;
import org.apache.ldap.common.message.MessageTypeEnum ;


/**
 * SearchRequest handler.
 * 
 * @author <a href="mailto:directory-dev@incubator.apache.org">
 * Apache Directory Project</a>
 * @version $Rev$
 */
public class SearchHandler implements RequestHandler
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
    public void handle( SearchRequest a_request )
    {
        throw new NotImplementedException( "STUB" ) ;
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
