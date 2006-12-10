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
package org.apache.directory.mitosis.operation;


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamConstants;


/**
 * Encodes {@link Operation}s to <tt>byte[]</tt> and vice versa so an
 * {@link Operation} can be transferred via TCP/IP communication.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class OperationCodec
{
    public OperationCodec()
    {
    }


    /**
     * Transforms the specified {@link Operation} into a byte array.
     */
    public byte[] encode( Operation op )
    {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        try
        {
            ObjectOutputStream out = new ObjectOutputStream( bout );
            out.useProtocolVersion( ObjectStreamConstants.PROTOCOL_VERSION_2 );
            out.writeObject( op );
            out.flush();
            out.close();
        }
        catch ( IOException e )
        {
            throw ( InternalError ) new InternalError().initCause( e );
        }
        return bout.toByteArray();
    }


    /**
     * Transforms the specified byte array into an {@link Operation}.
     */
    public Operation decode( byte[] data )
    {
        ObjectInputStream in;
        try
        {
            in = new ObjectInputStream( new ByteArrayInputStream( data ) );
            return ( Operation ) in.readObject();
        }
        catch ( IOException e )
        {
            throw ( InternalError ) new InternalError().initCause( e );
        }
        catch ( ClassNotFoundException e )
        {
            throw ( InternalError ) new InternalError().initCause( e );
        }
    }
}
