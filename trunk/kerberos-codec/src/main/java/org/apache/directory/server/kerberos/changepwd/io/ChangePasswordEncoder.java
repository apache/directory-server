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
package org.apache.directory.server.kerberos.changepwd.io;


import java.nio.ByteBuffer;

import org.apache.directory.api.asn1.EncoderException;
import org.apache.directory.server.kerberos.changepwd.messages.AbstractPasswordMessage;


public class ChangePasswordEncoder
{
    public static ByteBuffer encode( AbstractPasswordMessage chngPwdMsg, boolean isTcp ) throws EncoderException
    {
        int len = chngPwdMsg.computeLength();

        ByteBuffer buf;
        if ( isTcp )
        {
            buf = ByteBuffer.allocate( len + 4 );
            buf.putInt( len );
        }
        else
        {
            buf = ByteBuffer.allocate( len );
        }

        buf = chngPwdMsg.encode( buf );
        buf.flip();

        return buf;
    }
}
