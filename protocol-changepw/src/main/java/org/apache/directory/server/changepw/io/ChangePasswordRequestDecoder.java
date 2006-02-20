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
package org.apache.directory.server.changepw.io;


import java.io.IOException;
import java.nio.ByteBuffer;

import org.apache.directory.server.changepw.messages.ChangePasswordRequest;
import org.apache.directory.server.changepw.messages.ChangePasswordRequestModifier;
import org.apache.directory.server.kerberos.shared.io.decoder.ApplicationRequestDecoder;
import org.apache.directory.server.kerberos.shared.io.decoder.PrivateMessageDecoder;
import org.apache.directory.server.kerberos.shared.messages.ApplicationRequest;
import org.apache.directory.server.kerberos.shared.messages.application.PrivateMessage;


public class ChangePasswordRequestDecoder
{
    public ChangePasswordRequest decode( ByteBuffer buf ) throws IOException
    {
        ChangePasswordRequestModifier modifier = new ChangePasswordRequestModifier();

        modifier.setMessageLength( buf.getShort() );
        modifier.setProtocolVersionNumber( buf.getShort() );

        short authHeaderLength = buf.getShort();
        modifier.setAuthHeaderLength( authHeaderLength );

        byte[] undecodedAuthHeader = new byte[authHeaderLength];
        buf.get( undecodedAuthHeader, 0, authHeaderLength );

        ApplicationRequestDecoder decoder = new ApplicationRequestDecoder();
        ApplicationRequest authHeader = decoder.decode( undecodedAuthHeader );

        modifier.setAuthHeader( authHeader );

        byte[] encodedPrivate = new byte[buf.remaining()];
        buf.get( encodedPrivate, 0, buf.remaining() );

        PrivateMessageDecoder privateDecoder = new PrivateMessageDecoder();
        PrivateMessage privMessage = privateDecoder.decode( encodedPrivate );

        modifier.setPrivateMessage( privMessage );

        return modifier.getChangePasswordMessage();
    }
}
