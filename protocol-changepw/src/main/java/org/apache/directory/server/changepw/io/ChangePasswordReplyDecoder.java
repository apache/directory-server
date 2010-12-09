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
import org.apache.directory.server.kerberos.protocol.KerberosDecoder;
import org.apache.directory.server.kerberos.shared.messages.application.PrivateMessage;
import org.apache.directory.shared.kerberos.messages.ApRep;


/**
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
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
        short messageLength = buf.getShort();
        short protocolVersion = buf.getShort();
        short encodedAppReplyLength = buf.getShort();

        byte[] encodedAppReply = new byte[encodedAppReplyLength];
        buf.get( encodedAppReply );

        ApRep applicationReply = KerberosDecoder.decodeApRep( encodedAppReply );

        int privateBytesLength = messageLength - HEADER_LENGTH - encodedAppReplyLength;
        byte[] encodedPrivateMessage = new byte[privateBytesLength];
        buf.get( encodedPrivateMessage );

        PrivateMessageDecoder privateDecoder = new PrivateMessageDecoder();
        PrivateMessage privateMessage = privateDecoder.decode( encodedPrivateMessage );
        applicationReply.setPrivateMessage( privateMessage );

        return applicationReply.getChangePasswordReply();
    }
}
