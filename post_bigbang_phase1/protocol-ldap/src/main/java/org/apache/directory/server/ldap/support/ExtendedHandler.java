    /*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *  
 *    http://www.apache.org/licenses/LICENSE-2.0
 *  
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License. 
 *  
 */
package org.apache.directory.server.ldap.support;


import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.directory.server.ldap.ExtendedOperationHandler;
import org.apache.directory.shared.ldap.message.ExtendedRequest;
import org.apache.mina.common.IoSession;
import org.apache.mina.handler.demux.MessageHandler;


/**
 * A single reply handler for {@link org.apache.directory.shared.ldap.message.ExtendedRequest}s.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public abstract class ExtendedHandler extends AbstractLdapHandler implements MessageHandler
{
    private final Map<String, ExtendedOperationHandler> handlers = new HashMap<String, ExtendedOperationHandler>();


    public final ExtendedOperationHandler addHandler( ExtendedOperationHandler eoh )
    {
        synchronized ( handlers )
        {
            return handlers.put( eoh.getOid(), eoh );
        }
    }


    public final ExtendedOperationHandler removeHandler( String oid )
    {
        synchronized ( handlers )
        {
            return handlers.remove( oid );
        }
    }


    public final ExtendedOperationHandler getHandler( String oid )
    {
        return handlers.get( oid );
    }


    public final Map<String,ExtendedOperationHandler> getHandlerMap()
    {
        return Collections.unmodifiableMap( handlers );
    }


    public final void messageReceived( IoSession session, Object request ) throws Exception
    {
        extendedMessageReceived( session, ( ExtendedRequest ) request );
    }


    protected abstract void extendedMessageReceived( IoSession session, ExtendedRequest extendedRequest )
            throws Exception;
}
