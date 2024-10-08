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
package org.apache.directory.server.core.avltree;


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.apache.directory.server.i18n.I18n;


/**
 * A Marshaller which uses default Java Serialization.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class DefaultMarshaller implements Marshaller<Object>
{
    public static final DefaultMarshaller INSTANCE = new DefaultMarshaller();


    public byte[] serialize( Object object ) throws IOException
    {
        try ( ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
            ObjectOutputStream out = new ObjectOutputStream( byteStream ) )
        {
            out.writeObject( object );
            out.flush();
            byte[] data = byteStream.toByteArray();

            return data;
        }
    }


    public Object deserialize( byte[] bytes ) throws IOException
    {
        try ( ByteArrayInputStream byteStream = new ByteArrayInputStream( bytes );
            ObjectInputStream in = new ObjectInputStream( byteStream ) )
        {
            return in.readObject();
        }
        catch ( ClassNotFoundException e )
        {
            IOException ioe = new IOException( I18n.err( I18n.ERR_03009_COULD_NOT_FIND_CLASS ) );
            ioe.initCause( e );
            throw ioe;
        }
    }
}
