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
package org.apache.directory.server.kerberos.protocol.codec;


import java.io.IOException;
import java.nio.ByteBuffer;

import org.apache.directory.api.asn1.Asn1Object;
import org.apache.directory.api.asn1.EncoderException;


/**
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class KerberosEncoder
{
    public static ByteBuffer encode( Asn1Object asn1Obj, boolean isTcp ) throws IOException
    {
        ByteBuffer kerberosMessage = null;

        int responseLength = asn1Obj.computeLength();

        int bufferLen = responseLength;

        if ( isTcp )
        {
            bufferLen += 4;
        }

        kerberosMessage = ByteBuffer.allocate( bufferLen );

        if ( isTcp )
        {
            kerberosMessage.putInt( responseLength );
        }

        try
        {
            asn1Obj.encode( kerberosMessage );

            kerberosMessage.flip();

            return kerberosMessage;
        }
        catch ( EncoderException e )
        {
            throw new IOException( e.getMessage() );
        }
    }
}
