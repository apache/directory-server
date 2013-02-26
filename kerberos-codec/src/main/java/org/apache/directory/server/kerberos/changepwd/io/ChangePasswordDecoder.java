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
import java.nio.InvalidMarkException;

import org.apache.directory.server.kerberos.changepwd.exceptions.ChangePasswordException;
import org.apache.directory.server.kerberos.changepwd.messages.AbstractPasswordMessage;
import org.apache.directory.server.kerberos.changepwd.messages.ChangePasswordError;
import org.apache.directory.server.kerberos.changepwd.messages.ChangePasswordReply;
import org.apache.directory.server.kerberos.changepwd.messages.ChangePasswordRequest;


/**
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class ChangePasswordDecoder
{
    public static AbstractPasswordMessage decode( ByteBuffer buf, boolean isTcp ) throws ChangePasswordException
    {
        if ( isTcp )
        {
            // For TCP transport, there is a 4 octet header in network byte order
            // that precedes the message and specifies the length of the message.
            buf.getInt();
            buf.mark();
        }

        // cause we don't have a special message type value, try decoding as request, reply and error and send whichever succeeds

        try
        {
            return ChangePasswordRequest.decode( buf );
        }
        catch ( Exception e )
        {
            resetOrRewind( buf );
        }

        try
        {
            return ChangePasswordReply.decode( buf );
        }
        catch ( Exception e )
        {
            resetOrRewind( buf );
        }

        return ChangePasswordError.decode( buf );
    }


    private static void resetOrRewind( ByteBuffer buf )
    {
        try
        {
            buf.reset();
        }
        catch ( InvalidMarkException e )
        {
            buf.rewind();
        }
    }
}
