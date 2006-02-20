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
package org.apache.directory.server.kerberos.shared.service;


import javax.security.auth.kerberos.KerberosPrincipal;

import org.apache.directory.server.kerberos.shared.exceptions.KerberosException;
import org.apache.directory.server.kerberos.shared.messages.ErrorMessage;
import org.apache.directory.server.kerberos.shared.messages.ErrorMessageModifier;
import org.apache.directory.server.kerberos.shared.messages.value.KerberosTime;
import org.apache.directory.server.protocol.shared.chain.Command;
import org.apache.directory.server.protocol.shared.chain.Filter;
import org.apache.directory.server.protocol.shared.chain.impl.CommandBase;


/**
 * A {@link Command} for helping convert a {@link KerberosException} into
 * an {@link ErrorMessage} to be returned to clients.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public abstract class ErrorMessageHandler extends CommandBase implements Filter
{
    public ErrorMessage getErrorMessage( KerberosPrincipal principal, KerberosException exception )
    {
        ErrorMessageModifier modifier = new ErrorMessageModifier();

        KerberosTime now = new KerberosTime();

        modifier.setErrorCode( exception.getErrorCode() );
        modifier.setExplanatoryText( exception.getMessage() );
        modifier.setServerPrincipal( principal );
        modifier.setServerTime( now );
        modifier.setServerMicroSecond( 0 );
        modifier.setExplanatoryData( exception.getExplanatoryData() );

        return modifier.getErrorMessage();
    }
}
