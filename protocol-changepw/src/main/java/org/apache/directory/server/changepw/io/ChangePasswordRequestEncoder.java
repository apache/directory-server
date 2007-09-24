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

import org.apache.directory.server.changepw.messages.ChangePasswordRequest;
import org.apache.directory.server.kerberos.shared.io.encoder.ApplicationRequestEncoder;
import org.apache.directory.server.kerberos.shared.io.encoder.PrivateMessageEncoder;
import org.apache.directory.server.kerberos.shared.messages.application.ApplicationRequest;
import org.apache.directory.server.kerberos.shared.messages.application.PrivateMessage;


/**
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class ChangePasswordRequestEncoder
{
    private static final int HEADER_LENGTH = 6;


    /**
     * Encodes a {@link ChangePasswordRequest} into a {@link ByteBuffer}.
     *
     * @param buf
     * @param message
     * @throws IOException
     */
    public void encode( ByteBuffer buf, ChangePasswordRequest message ) throws IOException
    {
        // Build application request bytes
        ApplicationRequest appRequest = message.getAuthHeader();
        ApplicationRequestEncoder appEncoder = new ApplicationRequestEncoder();
        byte[] encodedAppRequest = appEncoder.encode( appRequest );

        // Build private message bytes
        PrivateMessage privateMessage = message.getPrivateMessage();
        PrivateMessageEncoder privateEncoder = new PrivateMessageEncoder();
        byte[] privateBytes = privateEncoder.encode( privateMessage );

        short messageLength = ( short ) ( HEADER_LENGTH + encodedAppRequest.length + privateBytes.length );

        short protocolVersion = 1;

        buf.putShort( messageLength );
        buf.putShort( protocolVersion );
        buf.putShort( ( short ) encodedAppRequest.length );

        buf.put( encodedAppRequest );
        buf.put( privateBytes );
    }
}
