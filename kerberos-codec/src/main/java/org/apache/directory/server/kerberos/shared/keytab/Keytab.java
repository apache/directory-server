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
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.directory.server.i18n.I18n;


/**
 * Keytab file. The format is the following :
 * <pre>
 * { 
 *   version : 2 bytes (0x05 0x02)
 *   keytabEntry*
 * }
 *
 * keytab_entry 
 * {
 *     size : int
 *     numComponents :  short
 *     realm : countedOctetString
 *     components[numComponents] : countedOctetString
 *     nameType : int
 *     timestamp : int
 *     vno8 : byte
 *     key : keyBlock
 *     vno : int // only present if >= 4 bytes left in entry
 * };
 *
 * keyblock 
 * {
 *     type : int
 *     data : countedOctetString
 * }
 *
 * countedOctetString 
 * {
 *     length : short
 *     data[length] : bytes
 * }
 *
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class Keytab
{
    /**
     * Byte array constant for keytab file format 5.1.
     */
    public static final byte[] VERSION_0X501_BYTES = new byte[]
        { ( byte ) 0x05, ( byte ) 0x01 };

    // Format 0x0501
    public static final short VERSION_0X501 = 0x0501;

    /**
     * Byte array constant for keytab file format 5.2.
     */
    public static final byte[] VERSION_0X502_BYTES = new byte[]
        { ( byte ) 0x05, ( byte ) 0x02 };

    // Format 0x0502
    public static final short VERSION_0X502 = 0x0502;

    private byte[] keytabVersion = VERSION_0X502_BYTES;
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
        ByteBuffer buffer = ByteBuffer.wrap( getBytesFromFile( file ) );
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
        ByteBuffer buffer = writer.write( keytabVersion, entries );
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
    static Keytab read( byte[] bytes ) throws IOException
    {
        ByteBuffer buffer = ByteBuffer.wrap( bytes );
        return readKeytab( buffer );
    }


    /**
     * Write the keytab to a {@link ByteBuffer}.
     * @return The buffer.
     */
    ByteBuffer write()
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
    private static Keytab readKeytab( ByteBuffer buffer ) throws IOException
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
        try ( InputStream is = Files.newInputStream( file.toPath() ) )
        {

            long length = file.length();

            // Check to ensure that file is not larger than Integer.MAX_VALUE.
            if ( length > Integer.MAX_VALUE )
            {
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
                throw new IOException( I18n.err( I18n.ERR_619, file.getName() ) );
            }

            return bytes;
        }
    }


    /**
     * Write the contents of the {@link ByteBuffer} to a {@link File}.
     *
     * @param buffer
     * @param file
     * @throws IOException
     */
    protected void writeFile( ByteBuffer buffer, File file ) throws IOException
    {
        // Set append false to replace existing.
        OutputStream out = Files.newOutputStream( file.toPath() );

        try ( WritableByteChannel channel = Channels.newChannel( out ) )
        {
            // Write the bytes between the position and limit.
            channel.write( buffer );
        }
        finally
        {
            out.close();
        }
    }
}
