/*
 *   Copyright 2005 The Apache Software Foundation
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
package org.apache.directory.server.kerberos.kdc.authentication;


import java.util.Date;

import org.apache.directory.server.kerberos.shared.exceptions.ErrorType;
import org.apache.directory.server.kerberos.shared.exceptions.KerberosException;
import org.apache.directory.server.kerberos.shared.store.PrincipalStoreEntry;
import org.apache.mina.common.IoSession;
import org.apache.mina.handler.chain.IoHandlerCommand;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;

public class VerifyPolicy implements IoHandlerCommand
{
    /** the log for this class */
//    private static final Logger log = LoggerFactory.getLogger( VerifyPolicy.class );
    private String contextKey = "context";
    

    public void execute( NextCommand next, IoSession session, Object message ) throws Exception
    {
        AuthenticationContext authContext = ( AuthenticationContext ) session.getAttribute( getContextKey() );
        PrincipalStoreEntry entry = authContext.getClientEntry();

        if ( entry.isDisabled() )
        {
            throw new KerberosException( ErrorType.KDC_ERR_CLIENT_REVOKED );
        }

        if ( entry.isLockedOut() )
        {
            throw new KerberosException( ErrorType.KDC_ERR_CLIENT_REVOKED );
        }

        if ( entry.getExpiration().getTime() < new Date().getTime() )
        {
            throw new KerberosException( ErrorType.KDC_ERR_CLIENT_REVOKED );
        }
        next.execute( session, message ); 
    }


    public String getContextKey()
    {
        return ( this.contextKey );
    }
}