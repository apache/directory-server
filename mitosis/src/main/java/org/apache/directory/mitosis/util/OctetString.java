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
package org.apache.directory.mitosis.util;

/**
 * A utuility class that generates octet strings from numbers.
 * 
 * @author Trustin Lee (trustin@apache.org)
 * @version $Rev: 118 $, $Date: 2006-09-18 13:48:47Z $
 */
public class OctetString
{
    private static final char[] highDigits;
    private static final char[] lowDigits;
    
    static
    {
        final char[] digits =
        { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };

        int i;
        char[] high = new char[256];
        char[] low = new char[256];
    
        for( i = 0; i < 256; i++ )
        {
            high[i] = digits[ i >>> 4 ];
            low[i] = digits[ i & 0x0F ];
        }
    
        highDigits = high;
        lowDigits = low;
    }

    public static void append( StringBuffer dst, long value )
    {
        int v;
        v = (int) ( value >>> 56 );
        dst.append( highDigits[ v ] );
        dst.append( lowDigits[ v ] );
        
        v = (int) ( value >>> 48 ) & 0xff;
        dst.append( highDigits[ v ] );
        dst.append( lowDigits[ v ] );
        
        v = (int) ( value >>> 40 ) & 0xff;
        dst.append( highDigits[ v ] );
        dst.append( lowDigits[ v ] );
        
        v = (int) ( value >>> 32 ) & 0xff;
        dst.append( highDigits[ v ] );
        dst.append( lowDigits[ v ] );
        
        v = (int) ( value >>> 24 ) & 0xff;
        dst.append( highDigits[ v ] );
        dst.append( lowDigits[ v ] );
        
        v = (int) ( value >>> 16 ) & 0xff;
        dst.append( highDigits[ v ] );
        dst.append( lowDigits[ v ] );
        
        v = (int) ( value >>> 8 ) & 0xff;
        dst.append( highDigits[ v ] );
        dst.append( lowDigits[ v ] );
        
        v = (int) value & 0xff;
        dst.append( highDigits[ v ] );
        dst.append( lowDigits[ v ] );
    }
    
    public static void append( StringBuffer dst, int value )
    {
        int v;
        v = ( value >>> 24 ) & 0xff;
        dst.append( highDigits[ v ] );
        dst.append( lowDigits[ v ] );
        
        v = ( value >>> 16 ) & 0xff;
        dst.append( highDigits[ v ] );
        dst.append( lowDigits[ v ] );
        
        v = ( value >>> 8 ) & 0xff;
        dst.append( highDigits[ v ] );
        dst.append( lowDigits[ v ] );
        
        v = value & 0xff;
        dst.append( highDigits[ v ] );
        dst.append( lowDigits[ v ] );
    }
    
    public static String toString( byte[] src )
    {
        final int end = src.length;
        StringBuffer dst = new StringBuffer( src.length << 1 );
        for( int i = 0; i < end; i++ )
        {
            dst.append( highDigits[ src[ i ] & 0xff ] );
            dst.append( lowDigits[ src[ i ] & 0xff ] );
        }
        
        return dst.toString();
    }
    
    public static String toString( long value )
    {
        StringBuffer dst = new StringBuffer( 16 );
        append( dst, value );
        return dst.toString();
    }
    
    public static String toString( int value )
    {
        StringBuffer dst = new StringBuffer( 8 );
        append( dst, value );
        return dst.toString();
    }
    
    public static int parseInt( String value )
    {
        return Integer.parseInt( value, 16 );
    }
    
    public static long parseLong( String value )
    {
        return Long.parseLong( value, 16 );
    }

    private OctetString()
    {
    }
}
