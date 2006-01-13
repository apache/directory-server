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
package org.apache.kerberos.kdc.authentication;

import org.apache.kerberos.exceptions.KerberosException;
import org.apache.kerberos.kdc.KdcConfiguration;
import org.apache.kerberos.messages.ErrorMessage;
import org.apache.kerberos.service.ErrorMessageHandler;
import org.apache.protocol.common.chain.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuthenticationExceptionHandler extends ErrorMessageHandler
{
    private static final Logger log = LoggerFactory.getLogger( AuthenticationExceptionHandler.class );

    public boolean execute( Context context ) throws Exception
    {
        return CONTINUE_CHAIN;
    }

    public boolean postprocess( Context context, Exception exception )
    {
        if ( exception == null )
        {
            return CONTINUE_CHAIN;
        }

        if ( log.isDebugEnabled() )
        {
            log.debug( exception.getMessage(), exception );
        }
        else
        {
            log.info( exception.getMessage() );
        }

        AuthenticationContext authContext = (AuthenticationContext) context;
        KdcConfiguration config = authContext.getConfig();
        KerberosException ke = (KerberosException) exception;

        ErrorMessage errorMessage = getErrorMessage( config.getKdcPrincipal(), ke );

        authContext.setReply( errorMessage );

        return STOP_CHAIN;
    }
}
