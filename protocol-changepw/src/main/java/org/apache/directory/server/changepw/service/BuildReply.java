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
package org.apache.directory.server.changepw.service;

import java.net.InetAddress;

import org.apache.directory.server.changepw.exceptions.ChangePasswordException;
import org.apache.directory.server.changepw.exceptions.ErrorType;
import org.apache.directory.server.changepw.messages.ChangePasswordReplyModifier;
import org.apache.directory.server.protocol.shared.chain.Context;
import org.apache.directory.server.protocol.shared.chain.impl.CommandBase;
import org.apache.kerberos.exceptions.KerberosException;
import org.apache.kerberos.messages.application.ApplicationReply;
import org.apache.kerberos.messages.application.PrivateMessage;
import org.apache.kerberos.messages.components.Authenticator;
import org.apache.kerberos.messages.components.EncApRepPart;
import org.apache.kerberos.messages.components.EncApRepPartModifier;
import org.apache.kerberos.messages.components.EncKrbPrivPart;
import org.apache.kerberos.messages.components.EncKrbPrivPartModifier;
import org.apache.kerberos.messages.components.Ticket;
import org.apache.kerberos.messages.value.EncryptedData;
import org.apache.kerberos.messages.value.EncryptionKey;
import org.apache.kerberos.messages.value.HostAddress;
import org.apache.kerberos.service.LockBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BuildReply extends CommandBase
{
    /** the log for this class */
    private static final Logger log = LoggerFactory.getLogger( BuildReply.class );

    public boolean execute( Context context ) throws Exception
    {
        ChangePasswordContext changepwContext = (ChangePasswordContext) context;

        Authenticator authenticator = changepwContext.getAuthenticator();
        Ticket ticket = changepwContext.getTicket();
        LockBox lockBox = changepwContext.getLockBox();

        // begin building reply

        // create priv message
        // user-data component is short result code
        EncKrbPrivPartModifier modifier = new EncKrbPrivPartModifier();
        byte[] resultCode = { (byte) 0x00, (byte) 0x00 };
        modifier.setUserData( resultCode );

        modifier.setSenderAddress( new HostAddress( InetAddress.getLocalHost() ) );
        EncKrbPrivPart privPart = modifier.getEncKrbPrivPart();

        // get the subsession key from the Authenticator
        EncryptionKey subSessionKey = authenticator.getSubSessionKey();

        EncryptedData encPrivPart;

        try
        {
            encPrivPart = lockBox.seal( subSessionKey, privPart );
        }
        catch ( KerberosException ke )
        {
            log.error( ke.getMessage(), ke );
            throw new ChangePasswordException( ErrorType.KRB5_KPASSWD_SOFTERROR );
        }

        PrivateMessage privateMessage = new PrivateMessage( encPrivPart );

        // Begin AP_REP generation
        EncApRepPartModifier encApModifier = new EncApRepPartModifier();
        encApModifier.setClientTime( authenticator.getClientTime() );
        encApModifier.setClientMicroSecond( authenticator.getClientMicroSecond() );
        encApModifier.setSequenceNumber( new Integer( authenticator.getSequenceNumber() ) );
        encApModifier.setSubSessionKey( authenticator.getSubSessionKey() );

        EncApRepPart repPart = encApModifier.getEncApRepPart();

        EncryptedData encRepPart;

        try
        {
            encRepPart = lockBox.seal( ticket.getSessionKey(), repPart );
        }
        catch ( KerberosException ke )
        {
            log.error( ke.getMessage(), ke );
            throw new ChangePasswordException( ErrorType.KRB5_KPASSWD_SOFTERROR );
        }

        ApplicationReply appReply = new ApplicationReply( encRepPart );

        // return status message value object
        ChangePasswordReplyModifier replyModifier = new ChangePasswordReplyModifier();
        replyModifier.setApplicationReply( appReply );
        replyModifier.setPrivateMessage( privateMessage );

        changepwContext.setReply( replyModifier.getChangePasswordReply() );

        return CONTINUE_CHAIN;
    }
}
