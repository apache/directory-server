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


import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.apache.seda.listener.ClientKey;
import org.apache.seda.protocol.AbstractNoReplyHandler;


/**
 * A no reply protocol handler implementation for LDAP {@link
 * org.apache.ldap.common.message.UnbindRequest}s.
 *
 * @author <a href="mailto:directory-dev@incubator.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class UnbindHandler extends AbstractNoReplyHandler
{
    public void handle( ClientKey key, Object request )
    {
        InitialContext ictx = SessionRegistry.getSingleton( null ).get( key );
        SessionRegistry.getSingleton( null ).remove( key );

        try
        {
            ictx.close();
        }
        catch ( NamingException e )
        {
            // @todo not much we can do here but monitoring would be good
            e.printStackTrace();
        }
    }
}
