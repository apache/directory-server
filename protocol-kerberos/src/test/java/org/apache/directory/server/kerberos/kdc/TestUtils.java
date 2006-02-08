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
package org.apache.directory.server.kerberos.kdc;

import java.io.BufferedInputStream;
import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

public class TestUtils
{
    public static char[] getControlDocument( String resource ) throws IOException
    {
        InputStream is = ClassLoader.getSystemClassLoader().getResourceAsStream( resource );

        Reader reader = new InputStreamReader( new BufferedInputStream( is ) );

        CharArrayWriter writer = new CharArrayWriter();

        try
        {
            char[] buf = new char[ 2048 ];
            int len = 0;
            while ( len >= 0 )
            {
                len = reader.read( buf );
                if ( len > 0 )
                {
                    writer.write( buf, 0, len );
                }
            }
        }
        finally
        {
            try
            {
                reader.close();
            }
            catch ( IOException ioe )
            {
            }
        }

        char[] isca = writer.toCharArray();
        return isca;
    }

    public static byte[] getBytesFromResource( String resource ) throws IOException
    {
        InputStream is = ClassLoader.getSystemClassLoader().getResourceAsStream( resource );

        BufferedInputStream stream = new BufferedInputStream( is );
        int len = stream.available();
        byte[] bytes = new byte[ len ];
        stream.read( bytes, 0, len );

        return bytes;
    }

    public static void hexdump( byte[] data )
    {
        hexdump( data, true );
    }

    public static void hexdump( byte[] data, boolean delimit )
    {
        String delimiter = new String( "-------------------------------------------------" );

        if ( delimit )
        {
            System.out.println( delimiter );
        }

        int lineLength = 0;
        for ( int ii = 0; ii < data.length; ii++ )
        {
            System.out.print( byte2hexString( data[ ii ] ) + " " );
            lineLength++;
            
            if ( lineLength == 8 )
            {
                System.out.print( "  " );
            }
            
            if ( lineLength == 16 )
            {
                System.out.println();
                lineLength = 0;
            }
        }

        if ( delimit )
        {
            System.out.println();
            System.out.println( delimiter );
        }
    }

    public static final String[] hex_digit = { "0", "1", "2", "3", "4", "5", "6", "7", "8", "9",
            "a", "b", "c", "d", "e", "f" };

    public static String byte2hexString( byte x )
    {
        String s = "";
        for ( int ii = 0; ii < 2; ii++ )
        {
            s = hex_digit[ ( ( ( x ) & 0xff ) & ( 15 << ( ii * 4 ) ) ) >>> ( ii * 4 ) ] + s;
        }

        return s;
    }

    public static String int2hexString( int x )
    {
        String s = "";
        for ( int ii = 0; ii < 8; ii++ )
        {
            s = hex_digit[ ( x & ( 15 << ( ii * 4 ) ) ) >>> ( ii * 4 ) ] + s;
        }

        return s;
    }

    public static String int2binString( int x )
    {
        String s = "";
        for ( int ii = 0; ii < 32; ii++ )
        {
            if ( ( ii > 0 ) && ( ii % 4 == 0 ) ) 
            {
                s = " " + s;
            }

            s = hex_digit[ ( x & ( 1 << ii ) ) >>> ii ] + s;
        }

        return s;
    }

    public static String long2hexString( long x )
    {
        String s = "";
        for ( int ii = 0; ii < 16; ii++ )
        {
            s = hex_digit[ (int) ( ( x & ( 15L << ( ii * 4 ) ) ) >>> ( ii * 4 ) ) ] + s;
        }

        return s;
    }

    public static String long2binString( long x )
    {
        String s = "";
        for ( int ii = 0; ii < 64; ii++ )
        {
            if ( ( ii > 0 ) && ( ii % 4 == 0 ) )
            {
                s = " " + s;
            }

            s = hex_digit[ (int) ( ( x & ( 1L << ii ) ) >>> ii ) ] + s;
        }

        return s;
    }

    public static String byte2hexString( byte[] input )
    {
        return byte2hexString( input, 0, input.length );
    }

    public static String byte2hexString( byte[] input, int offset )
    {
        return byte2hexString( input, offset, input.length );
    }

    public static String byte2hexString( byte[] input, int offset, int length )
    {
        String result = "";
        for ( int ii = 0; ii < length; ii++ )
        {
            if ( ii + offset < input.length )
            {
                result += byte2hexString( input[ ii + offset ] );
            }
        }

        return result;
    }
}
