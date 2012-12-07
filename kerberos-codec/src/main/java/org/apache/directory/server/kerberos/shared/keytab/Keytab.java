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
package org.apache.directory.server.kerberos.shared.keytab;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.directory.server.i18n.I18n;
import org.apache.mina.core.buffer.IoBuffer;


/**
 * Keytab file.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class Keytab
{
    /**
     * Byte array constant for keytab file format 5.1.
     */
    public static final byte[] VERSION_51 = new byte[]
        { ( byte ) 0x05, ( byte ) 0x01 };

    /**
     * Byte array constant for keytab file format 5.2.
     */
    public static final byte[] VERSION_52 = new byte[]
        { ( byte ) 0x05, ( byte ) 0x02 };

    private byte[] keytabVersion = VERSION_52;
    private List<KeytabEntry> entries = new ArrayList<KeytabEntry>();


    /**
     * Read a keytab file.
     *
     * @param file
     * @return The keytab.
     * @throws IOException
     */
    public static Keytab read( File file ) throws IOException
    {
        IoBuffer buffer = IoBuffer.wrap( getBytesFromFile( file ) );
        return readKeytab( buffer );
    }


    /**
     * Returns a new instance of a keytab with the version
     * defaulted to 5.2.
     *
     * @return The keytab.
     */
    public static Keytab getInstance()
    {
        return new Keytab();
    }


    /**
     * Write the keytab to a {@link File}.
     *
     * @param file
     * @throws IOException
     */
    public void write( File file ) throws IOException
    {
        KeytabEncoder writer = new KeytabEncoder();
        IoBuffer buffer = writer.write( keytabVersion, entries );
        writeFile( buffer, file );
    }


    /**
     * @param entries The entries to set.
     */
    public void setEntries( List<KeytabEntry> entries )
    {
        this.entries = entries;
    }


    /**
     * @param keytabVersion The keytabVersion to set.
     */
    public void setKeytabVersion( byte[] keytabVersion )
    {
        this.keytabVersion = keytabVersion;
    }


    /**
     * @return The entries.
     */
    public List<KeytabEntry> getEntries()
    {
        return Collections.unmodifiableList( entries );
    }


    /**
     * @return The keytabVersion.
     */
    public byte[] getKeytabVersion()
    {
        return keytabVersion;
    }


    /**
     * Read bytes into a keytab.
     *
     * @param bytes
     * @return The keytab.
     */
    static Keytab read( byte[] bytes )
    {
        IoBuffer buffer = IoBuffer.wrap( bytes );
        return readKeytab( buffer );
    }


    /**
     * Write the keytab to a {@link ByteBuffer}.
     * @return The buffer.
     */
    IoBuffer write()
    {
        KeytabEncoder writer = new KeytabEncoder();
        return writer.write( keytabVersion, entries );
    }


    /**
     * Read the contents of the buffer into a keytab.
     *
     * @param buffer
     * @return The keytab.
     */
    private static Keytab readKeytab( IoBuffer buffer )
    {
        KeytabDecoder reader = new KeytabDecoder();
        byte[] keytabVersion = reader.getKeytabVersion( buffer );
        List<KeytabEntry> entries = reader.getKeytabEntries( buffer );

        Keytab keytab = new Keytab();

        keytab.setKeytabVersion( keytabVersion );
        keytab.setEntries( entries );

        return keytab;
    }


    /**
     * Returns the contents of the {@link File} in a byte array.
     *
     * @param file
     * @return The byte array of the file contents.
     * @throws IOException
     */
    protected static byte[] getBytesFromFile( File file ) throws IOException
    {
        InputStream is = new FileInputStream( file );

        long length = file.length();

        // Check to ensure that file is not larger than Integer.MAX_VALUE.
        if ( length > Integer.MAX_VALUE )
        {
            is.close();
            throw new IOException( I18n.err( I18n.ERR_618, file.getName() ) );
        }

        // Create the byte array to hold the data.
        byte[] bytes = new byte[( int ) length];

        // Read in the bytes
        int offset = 0;
        int numRead = 0;
        while ( offset < bytes.length && ( numRead = is.read( bytes, offset, bytes.length - offset ) ) >= 0 )
        {
            offset += numRead;
        }

        // Ensure all the bytes have been read in.
        if ( offset < bytes.length )
        {
            is.close();
            throw new IOException( I18n.err( I18n.ERR_619, file.getName() ) );
        }

        // Close the input stream and return bytes.
        is.close();
        
        return bytes;
    }


    /**
     * Write the contents of the {@link IoBuffer} to a {@link File}.
     *
     * @param buffer
     * @param file
     * @throws IOException
     */
    protected void writeFile( IoBuffer buffer, File file ) throws IOException
    {
        // Set append false to replace existing.
        FileOutputStream fos = new FileOutputStream( file, false );
        FileChannel wChannel = fos.getChannel();

        // Write the bytes between the position and limit.
        wChannel.write( buffer.buf() );

        wChannel.close();
        fos.close();
    }
}
