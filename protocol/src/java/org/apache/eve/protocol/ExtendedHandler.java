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
package org.apache.eve.protocol;


import org.apache.seda.protocol.HandlerTypeEnum;
import org.apache.seda.protocol.SingleReplyHandler;
import org.apache.seda.listener.ClientKey;

import org.apache.ldap.common.NotImplementedException;


/**
 * A single reply handler for {@link org.apache.ldap.common.message.ExtendedRequest}s.
 *
 * @author <a href="mailto:directory-dev@incubator.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class ExtendedHandler implements SingleReplyHandler
{
    /**
     * @see org.apache.seda.protocol.SingleReplyHandler#handle(ClientKey,Object)
     */
    public Object handle( ClientKey key, Object request )
    {
        throw new NotImplementedException( "handle in org.apache.eve.protocol.ExtendedHandler not implemented!" );
    }


    /**
     * Gets HandlerTypeEnum.SINGLEREPLY always.
     *
     * @return HandlerTypeEnum.SINGLEREPLY always
     */
    public HandlerTypeEnum getHandlerType()
    {
        return HandlerTypeEnum.SINGLEREPLY;
    }
}
