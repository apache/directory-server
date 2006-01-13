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
package org.apache.changepw.service;

import java.io.UnsupportedEncodingException;

import org.apache.changepw.exceptions.ChangePasswordException;
import org.apache.changepw.exceptions.ErrorType;
import org.apache.changepw.io.ChangePasswordDataDecoder;
import org.apache.changepw.messages.ChangePasswordRequest;
import org.apache.changepw.value.ChangePasswordData;
import org.apache.changepw.value.ChangePasswordDataModifier;
import org.apache.kerberos.exceptions.KerberosException;
import org.apache.kerberos.messages.components.Authenticator;
import org.apache.kerberos.messages.components.EncKrbPrivPart;
import org.apache.kerberos.messages.value.EncryptedData;
import org.apache.kerberos.messages.value.EncryptionKey;
import org.apache.kerberos.service.LockBox;
import org.apache.protocol.common.chain.Context;
import org.apache.protocol.common.chain.impl.CommandBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExtractPassword extends CommandBase
{
    /** the log for this class */
    private static final Logger log = LoggerFactory.getLogger( ExtractPassword.class );

    public boolean execute( Context context ) throws Exception
    {
        ChangePasswordContext changepwContext = (ChangePasswordContext) context;

        ChangePasswordRequest request = (ChangePasswordRequest) changepwContext.getRequest();
        Authenticator authenticator = changepwContext.getAuthenticator();
        LockBox lockBox = changepwContext.getLockBox();

        // TODO - check ticket is for service authorized to change passwords
        // ticket.getServerPrincipal().getName().equals(config.getChangepwPrincipal().getName()));

        // TODO - check client principal in ticket is authorized to change password

        // get the subsession key from the Authenticator
        EncryptionKey subSessionKey = authenticator.getSubSessionKey();

        // decrypt the request's private message with the subsession key
        EncryptedData encReqPrivPart = request.getPrivateMessage().getEncryptedPart();

        EncKrbPrivPart privatePart;

        try
        {
            privatePart = (EncKrbPrivPart) lockBox.unseal( EncKrbPrivPart.class, subSessionKey, encReqPrivPart );
        }
        catch ( KerberosException ke )
        {
            log.error( ke.getMessage(), ke );
            throw new ChangePasswordException( ErrorType.KRB5_KPASSWD_SOFTERROR );
        }

        ChangePasswordData passwordData = null;

        if ( request.getVersionNumber() == (short) 1 )
        {
            // Use protocol version 0x0001, the legacy Kerberos change password protocol
            ChangePasswordDataModifier modifier = new ChangePasswordDataModifier();
            modifier.setNewPassword( privatePart.getUserData() );
            passwordData = modifier.getChangePasswdData();
        }
        else
        {
            // Use protocol version 0xFF80, the backwards-compatible MS protocol
            ChangePasswordDataDecoder passwordDecoder = new ChangePasswordDataDecoder();
            passwordData = passwordDecoder.decodeChangePasswordData( privatePart.getUserData() );
        }

        try
        {
            changepwContext.setPassword( new String( passwordData.getPassword(), "UTF-8" ) );
        }
        catch ( UnsupportedEncodingException uee )
        {
            log.error( uee.getMessage(), uee );
            throw new ChangePasswordException( ErrorType.KRB5_KPASSWD_SOFTERROR );
        }

        return CONTINUE_CHAIN;
    }
}
