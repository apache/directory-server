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

import org.apache.directory.server.changepw.messages.ChangePasswordError;
import org.apache.kerberos.io.encoder.ErrorMessageEncoder;
import org.apache.kerberos.messages.ErrorMessage;

public class ChangePasswordErrorEncoder
{
    public void encode( ByteBuffer buf, ChangePasswordError message ) throws IOException
    {
        // Build error message bytes
        ErrorMessage errorMessage = message.getErrorMessage();
        ErrorMessageEncoder errorEncoder = new ErrorMessageEncoder();
        byte[] errorBytes = errorEncoder.encode( errorMessage );

        short headerLength = 6;

        short messageLength = (short) ( headerLength + errorBytes.length );
        buf.putShort( messageLength );

        short protocolVersion = 1;
        buf.putShort( protocolVersion );

        short zeroIndicatesError = 0;
        buf.putShort( zeroIndicatesError );

        buf.put( errorBytes );
    }
}
