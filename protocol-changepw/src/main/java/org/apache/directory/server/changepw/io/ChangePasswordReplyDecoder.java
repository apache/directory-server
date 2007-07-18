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
package org.apache.directory.server.changepw.io;


import java.io.IOException;
import java.nio.ByteBuffer;

import org.apache.directory.server.changepw.messages.ChangePasswordReply;
import org.apache.directory.server.changepw.messages.ChangePasswordReplyModifier;
import org.apache.directory.server.kerberos.shared.io.decoder.ApplicationReplyDecoder;
import org.apache.directory.server.kerberos.shared.io.decoder.PrivateMessageDecoder;
import org.apache.directory.server.kerberos.shared.messages.application.ApplicationReply;
import org.apache.directory.server.kerberos.shared.messages.application.PrivateMessage;


/**
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class ChangePasswordReplyDecoder
{
    private static final int HEADER_LENGTH = 6;


    /**
     * Decodes a {@link ByteBuffer} into a {@link ChangePasswordReply}.
     *
     * @param buf
     * @return The {@link ChangePasswordReply}.
     * @throws IOException
     */
    public ChangePasswordReply decode( ByteBuffer buf ) throws IOException
    {
        ChangePasswordReplyModifier modifier = new ChangePasswordReplyModifier();

        short messageLength = buf.getShort();
        short protocolVersion = buf.getShort();
        short encodedAppReplyLength = buf.getShort();

        modifier.setProtocolVersionNumber( protocolVersion );

        byte[] encodedAppReply = new byte[encodedAppReplyLength];
        buf.get( encodedAppReply );

        ApplicationReplyDecoder appDecoder = new ApplicationReplyDecoder();
        ApplicationReply applicationReply = appDecoder.decode( encodedAppReply );
        modifier.setApplicationReply( applicationReply );

        int privateBytesLength = messageLength - HEADER_LENGTH - encodedAppReplyLength;
        byte[] encodedPrivateMessage = new byte[privateBytesLength];
        buf.get( encodedPrivateMessage );

        PrivateMessageDecoder privateDecoder = new PrivateMessageDecoder();
        PrivateMessage privateMessage = privateDecoder.decode( encodedPrivateMessage );
        modifier.setPrivateMessage( privateMessage );

        return modifier.getChangePasswordReply();
    }
}
