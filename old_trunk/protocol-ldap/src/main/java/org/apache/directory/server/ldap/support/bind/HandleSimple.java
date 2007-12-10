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
package org.apache.directory.server.ldap.support.bind;


import javax.naming.Context;

import org.apache.directory.shared.ldap.message.BindRequest;
import org.apache.mina.common.IoSession;
import org.apache.mina.handler.chain.IoHandlerCommand;


/**
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class HandleSimple implements IoHandlerCommand
{
    public void execute( NextCommand next, IoSession session, Object message ) throws Exception
    {
        String sessionMechanism = ( String ) session.getAttribute( "sessionMechanism" );

        if ( sessionMechanism.equals( "SIMPLE" ) )
        {
            BindRequest request = ( BindRequest ) message;

            /*
             * This is the principal name that will be used to bind to the DIT.
             */
            session.setAttribute( Context.SECURITY_PRINCIPAL, request.getName() );

            /*
             * These are the credentials that will be used to bind to the DIT.
             * For the simple mechanism, this will be a password, possibly one-way hashed.
             */
            session.setAttribute( Context.SECURITY_CREDENTIALS, request.getCredentials() );
        }

        next.execute( session, message );
    }
}
