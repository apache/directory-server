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


import org.apache.ldap.common.message.Request ;


/**
 * Represents handlers that do not return a response to the client.
 *
 * @author <a href="mailto:aok123@bellsouth.net">Alex Karasulu</a>
 * @author $Author: akarasulu $
 * @version $Revision: 1.2 $
 */
public interface NoReplyHandler
    extends RequestHandler
{
    /**
     * Handles requests that do not reply to the requesting client with a
     * response.
     *
     * @param request the request without a response.
     */
    void handle( Request request ) ;
}
