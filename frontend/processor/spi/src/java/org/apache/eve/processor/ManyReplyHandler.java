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


import java.util.Iterator ;

import org.apache.ldap.common.message.ManyReplyRequest ;
import org.apache.ldap.common.message.ResultResponse;


/**
 * A handler for requests that can generate zero, one or more responses.
 *
 * @author <a href="mailto:directory-dev@incubator.apache.org">
 * Apache Directory Project</a>
 * @version $Rev$
 */
public interface ManyReplyHandler extends RequestHandler
{
    /**
     * A handler for a request that can create multiple responses of 
     * heterogenous messages.
     * 
     * @param request the request that may generate many replies
     * @return an enumeration over the responses
     */
    Iterator handle( ManyReplyRequest request ) ;
    
    /**
     * Gets the terminating response.
     * 
     * @return the finishing response that carries the result
     */
    ResultResponse getDoneResponse( int id ) ;
}
