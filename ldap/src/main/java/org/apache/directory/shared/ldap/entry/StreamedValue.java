/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.directory.shared.ldap.entry;


import org.apache.directory.shared.ldap.NotImplementedException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;


/**
 * Document me!
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class StreamedValue implements Value<URI>
{
    private URI wrapped;


    /**
     * Creates a streamed value without a wrapped URI.
     */
    public StreamedValue()
    {
    }


    /**
     * Creates a streamed value with an initial wrapped URI.
     *
     * @param wrapped the URI to wrap
     */
    public StreamedValue( URI wrapped )
    {
        this.wrapped = wrapped;
    }


    /**
     * Gets an input stream to read the streamed data.
     *
     * @return the streamed data
     * @throws IOException if there are errors estabilishing the data channel
     */
    public InputStream getInputStream() throws IOException
    {
        /*
         * Need some URI handlers to get the stream.
         */
        throw new NotImplementedException();
    }


    /**
     * Gets an output stream to write streamed data for this value.
     *
     * @return the streamed data
     * @throws IOException if there are errors establishing the data channel
     */
    public OutputStream getOutputStream() throws IOException
    {
        /*
         * Need some URI handlers to get the stream.
         */
        throw new NotImplementedException();
    }


    public URI get()
    {
        return wrapped;
    }
    
    
    public boolean isNull()
    {
        return wrapped == null;
    }


    public void set( URI wrapped )
    {
        this.wrapped = wrapped;
    }


    public int compareTo( Value<URI> value )
    {
        if ( value == null && get() == null )
        {
            return 0;
        }

        if ( value == null )
        {
            return 1;
        }

        if ( value.get() == null )
        {
            return -1;
        }

        return wrapped.compareTo( value.get() );
    }
}
