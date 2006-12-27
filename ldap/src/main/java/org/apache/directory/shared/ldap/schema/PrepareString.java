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

package org.apache.directory.shared.ldap.schema;

import java.io.IOException;

import org.apache.directory.shared.ldap.util.unicode.InvalidCharacterException;
// import org.apache.directory.shared.ldap.util.unicode.Normalizer;

/**
 * 
 * This class implements the 6 steps described in RFC 4518
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class PrepareString
{
    /** ALl the possible combining marks */
    private static final char[][] COMBINING_MARKS = new char[][] 
        {
            { 0x0300, 0x034F }, { 0x0360, 0x036F }, { 0x0483, 0x0486 },  { 0x0488, 0x0489 }, 
            { 0x0591, 0x05A1 }, { 0x05A3, 0x05B9 }, { 0x05BB, 0x05BC }, { 0x05BF, 0x05BF }, 
            { 0x05C1, 0x05C2 }, { 0x05C4, 0x05C4 }, { 0x064B, 0x0655 }, { 0x0670, 0x0670 }, 
            { 0x06D6, 0x06DC }, { 0x06DE, 0x06E4 }, { 0x06E7, 0x06E8 }, { 0x06EA, 0x06ED }, 
            { 0x0711, 0x0711 }, { 0x0730, 0x074A }, { 0x07A6, 0x07B0 }, { 0x0901, 0x0903 }, 
            { 0x093C, 0x093C }, { 0x093E, 0x094F }, { 0x0951, 0x0954 }, { 0x0962, 0x0963 },
            { 0x0981, 0x0983 }, { 0x09BC, 0x09BC }, { 0x09BE, 0x09C4 }, { 0x09C7, 0x09C8 }, 
            { 0x09CB, 0x09CD }, { 0x09D7, 0x09D7 }, { 0x09E2, 0x09E3 }, { 0x0A02, 0x0A02 }, 
            { 0x0A3C, 0x0A3C }, { 0x0A3E, 0x0A42 }, { 0x0A47, 0x0A48 }, { 0x0A4B, 0x0A4D },
            { 0x0A70, 0x0A71 }, { 0x0A81, 0x0A83 }, { 0x0ABC, 0x0ABC }, { 0x0ABE, 0x0AC5 }, 
            { 0x0AC7, 0x0AC9 }, { 0x0ACB, 0x0ACD }, { 0x0B01, 0x0B03 }, { 0x0B3C, 0x0B3C },
            { 0x0B3E, 0x0B43 }, { 0x0B47, 0x0B48 }, { 0x0B4B, 0x0B4D }, { 0x0B56, 0x0B57 },
            { 0x0B82, 0x0B82 }, { 0x0BBE, 0x0BC2 }, { 0x0BC6, 0x0BC8 }, { 0x0BCA, 0x0BCD }, 
            { 0x0BD7, 0x0BD7 }, { 0x0C01, 0x0C03 }, { 0x0C3E, 0x0C44 }, { 0x0C46, 0x0C48 }, 
            { 0x0C4A, 0x0C4D }, { 0x0C55, 0x0C56 }, { 0x0C82, 0x0C83 }, { 0x0CBE, 0x0CC4 }, 
            { 0x0CC6, 0x0CC8 }, { 0x0CCA, 0x0CCD }, { 0x0CD5, 0x0CD6 }, { 0x0D02, 0x0D03 },
            { 0x0D3E, 0x0D43 }, { 0x0D46, 0x0D48 }, { 0x0D4A, 0x0D4D }, { 0x0D57, 0x0D57 },
            { 0x0D82, 0x0D83 }, { 0x0DCA, 0x0DCA }, { 0x0DCF, 0x0DD4 }, { 0x0DD6, 0x0DD6 },
            { 0x0DD8, 0x0DDF }, { 0x0DF2, 0x0DF3 }, { 0x0E31, 0x0E31 }, { 0x0E34, 0x0E3A },
            { 0x0E47, 0x0E4E }, { 0x0EB1, 0x0EB1 }, { 0x0EB4, 0x0EB9 }, { 0x0EBB, 0x0EBC }, 
            { 0x0EC8, 0x0ECD }, { 0x0F18, 0x0F19 }, { 0x0F35, 0x0F35 }, { 0x0F37, 0x0F37 },
            { 0x0F39, 0x0F39 }, { 0x0F3E, 0x0F3F }, { 0x0F71, 0x0F84 }, { 0x0F86, 0x0F87 }, 
            { 0x0F90, 0x0F97 }, { 0x0F99, 0x0FBC }, { 0x0FC6, 0x0FC6 }, { 0x102C, 0x1032 }, 
            { 0x1036, 0x1039 }, { 0x1056, 0x1059 }, { 0x1712, 0x1714 }, { 0x1732, 0x1734 }, 
            { 0x1752, 0x1753 }, { 0x1772, 0x1773 }, { 0x17B4, 0x17D3 }, { 0x180B, 0x180D }, 
            { 0x18A9, 0x18A9 }, { 0x20D0, 0x20EA }, { 0x302A, 0x302F }, { 0x3099, 0x309A }, 
            { 0xFB1E, 0xFB1E }, { 0xFE00, 0xFE0F }, { 0xFE20, 0xFE23 }
        };
    
    /**
     * Tells if a char is a combining mark.
     *
     * @param c The char to check
     * @return <code>true> if the char is a combining mark, false otherwise
     */
    private static boolean isCombiningMark( char c )
    {
        for ( char[] interval:COMBINING_MARKS )
        {
            if ( ( c >= interval[0] ) && ( c <= interval[1] ) )
            {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 
     * TODO normalize.
     *
     * @param str
     * @return
     * @throws IOException
     */
    public static StringBuilder normalize( String str ) throws IOException
    {
        return null; //Normalizer.normalize( str, Normalizer.Form.KC );
    }
    
    /**
     * Execute the mapping step of the string preparation :
     * - suppress useless chars
     * - transform to spaces
     * - lowercase
     * 
     * @param str The string to transform
     * @return The transformed string
     */
    public static String map( String str )
    {
        return ( str == null ? null : map( str.toCharArray() ).toString() );
    }
    
    /**
     * Execute the mapping step of the string preparation :
     * - suppress useless chars
     * - transform to spaces
     * - lowercase
     * 
     * @param array The char array to transform
     * @return The transformed StringBuilder
     */
    public static StringBuilder map( char[] array )
    {
        if ( array == null )
        {
            return null;
        }

        StringBuilder sb = new StringBuilder( array.length );
        
        for ( char c:array )
        {
            // First, eliminate surrogates, and replace them by FFFD char
            if ( ( c >= 0xD800 ) && ( c <= 0xDFFF ) )
            {
                sb.append( (char)0xFFFD );
                continue;
            }
            
            switch ( c )
            {
                case 0x0000:
                case 0x0001:
                case 0x0002:
                case 0x0003:
                case 0x0004:
                case 0x0005:
                case 0x0006:
                case 0x0007:
                case 0x0008:
                    break;
                    
                case 0x0009:
                case 0x000A:
                case 0x000B:
                case 0x000C:
                case 0x000D:
                    sb.append( (char)0x20 );
                    break;
                    
                case 0x000E:
                case 0x000F:
                case 0x0010:
                case 0x0011:
                case 0x0012:
                case 0x0013:
                case 0x0014:
                case 0x0015:
                case 0x0016:
                case 0x0017:
                case 0x0018:
                case 0x0019:
                case 0x001A:
                case 0x001B:
                case 0x001C:
                case 0x001D:
                case 0x001E:
                case 0x001F:
                    break;

                case 0x0041 : 
                case 0x0042 : 
                case 0x0043 : 
                case 0x0044 : 
                case 0x0045 : 
                case 0x0046 : 
                case 0x0047 : 
                case 0x0048 : 
                case 0x0049 : 
                case 0x004A : 
                case 0x004B : 
                case 0x004C : 
                case 0x004D : 
                case 0x004E : 
                case 0x004F : 
                case 0x0050 : 
                case 0x0051 : 
                case 0x0052 : 
                case 0x0053 : 
                case 0x0054 : 
                case 0x0055 : 
                case 0x0056 : 
                case 0x0057 : 
                case 0x0058 : 
                case 0x0059 : 
                case 0x005A : 
                    sb.append( (char)( c | 0x0020 ) );
                    break;
        
                case 0x007F:
                case 0x0080:
                case 0x0081:
                case 0x0082:
                case 0x0083:
                case 0x0084:
                    break;
                    
                case 0x0085:
                    sb.append( (char)0x20 );
                    break;

                case 0x0086:
                case 0x0087:
                case 0x0088:
                case 0x0089:
                case 0x008A:
                case 0x008B:
                case 0x008C:
                case 0x008D:
                case 0x008E:
                case 0x008F:
                case 0x0090:
                case 0x0091:
                case 0x0092:
                case 0x0093:
                case 0x0094:
                case 0x0095:
                case 0x0096:
                case 0x0097:
                case 0x0098:
                case 0x0099:
                case 0x009A:
                case 0x009B:
                case 0x009C:
                case 0x009D:
                case 0x009E:
                case 0x009F:
                    break;
                    
                case 0x00A0:
                    sb.append( (char)0x20 );
                    break;

                case 0x00AD:
                    break;

                    
                case 0x00B5 : 
                    sb.append( (char)0x03BC );
                    break;
        
                case 0x00C0 : 
                case 0x00C1 : 
                case 0x00C2 : 
                case 0x00C3 : 
                case 0x00C4 : 
                case 0x00C5 : 
                case 0x00C6 : 
                case 0x00C7 : 
                case 0x00C8 : 
                case 0x00C9 : 
                case 0x00CA : 
                case 0x00CB : 
                case 0x00CC : 
                case 0x00CD : 
                case 0x00CE : 
                case 0x00CF : 
                case 0x00D0 : 
                case 0x00D1 : 
                case 0x00D2 : 
                case 0x00D3 : 
                case 0x00D4 : 
                case 0x00D5 : 
                case 0x00D6 : 
                case 0x00D8 : 
                case 0x00D9 : 
                case 0x00DA : 
                case 0x00DB : 
                case 0x00DC : 
                case 0x00DD : 
                case 0x00DE : 
                    sb.append( (char)( c | 0x0020 ) );
                    break;
        
                case 0x00DF : 
                    sb.append( (char)0x0073 );
                    sb.append( (char)0x0073 );
                    break;
        
                case 0x0100 : 
                    sb.append( (char)0x0101 );
                    break;
        
                case 0x0102 : 
                    sb.append( (char)0x0103 );
                    break;
        
                case 0x0104 : 
                    sb.append( (char)0x0105 );
                    break;
        
                case 0x0106 : 
                    sb.append( (char)0x0107 );
                    break;
        
                case 0x0108 : 
                    sb.append( (char)0x0109 );
                    break;
        
                case 0x010A : 
                    sb.append( (char)0x010B );
                    break;
        
                case 0x010C : 
                    sb.append( (char)0x010D );
                    break;
        
                case 0x010E : 
                    sb.append( (char)0x010F );
                    break;
        
                case 0x0110 : 
                    sb.append( (char)0x0111 );
                    break;
        
                case 0x0112 : 
                    sb.append( (char)0x0113 );
                    break;
        
                case 0x0114 : 
                    sb.append( (char)0x0115 );
                    break;
        
                case 0x0116 : 
                    sb.append( (char)0x0117 );
                    break;
        
                case 0x0118 : 
                    sb.append( (char)0x0119 );
                    break;
        
                case 0x011A : 
                    sb.append( (char)0x011B );
                    break;
        
                case 0x011C : 
                    sb.append( (char)0x011D );
                    break;
        
                case 0x011E : 
                    sb.append( (char)0x011F );
                    break;
        
                case 0x0120 : 
                    sb.append( (char)0x0121 );
                    break;
        
                case 0x0122 : 
                    sb.append( (char)0x0123 );
                    break;
        
                case 0x0124 : 
                    sb.append( (char)0x0125 );
                    break;
        
                case 0x0126 : 
                    sb.append( (char)0x0127 );
                    break;
        
                case 0x0128 : 
                    sb.append( (char)0x0129 );
                    break;
        
                case 0x012A : 
                    sb.append( (char)0x012B );
                    break;
        
                case 0x012C : 
                    sb.append( (char)0x012D );
                    break;
        
                case 0x012E : 
                    sb.append( (char)0x012F );
                    break;
        
                case 0x0130 : 
                    sb.append( (char)0x0069 );
                    sb.append( (char)0x0307 );
                    break;
        
                case 0x0132 : 
                    sb.append( (char)0x0133 );
                    break;
        
                case 0x0134 : 
                    sb.append( (char)0x0135 );
                    break;
        
                case 0x0136 : 
                    sb.append( (char)0x0137 );
                    break;
        
                case 0x0139 : 
                    sb.append( (char)0x013A );
                    break;
        
                case 0x013B : 
                    sb.append( (char)0x013C );
                    break;
        
                case 0x013D : 
                    sb.append( (char)0x013E );
                    break;
        
                case 0x013F : 
                    sb.append( (char)0x0140 );
                    break;
        
                case 0x0141 : 
                    sb.append( (char)0x0142 );
                    break;
        
                case 0x0143 : 
                    sb.append( (char)0x0144 );
                    break;
        
                case 0x0145 : 
                    sb.append( (char)0x0146 );
                    break;
        
                case 0x0147 : 
                    sb.append( (char)0x0148 );
                    break;
        
                case 0x0149 : 
                    sb.append( (char)0x02BC );
                    sb.append( (char)0x006E );
                    break;
        
                case 0x014A : 
                    sb.append( (char)0x014B );
                    break;
        
                case 0x014C : 
                    sb.append( (char)0x014D );
                    break;
        
                case 0x014E : 
                    sb.append( (char)0x014F );
                    break;
        
                case 0x0150 : 
                    sb.append( (char)0x0151 );
                    break;
        
                case 0x0152 : 
                    sb.append( (char)0x0153 );
                    break;
        
                case 0x0154 : 
                    sb.append( (char)0x0155 );
                    break;
        
                case 0x0156 : 
                    sb.append( (char)0x0157 );
                    break;
        
                case 0x0158 : 
                    sb.append( (char)0x0159 );
                    break;
        
                case 0x015A : 
                    sb.append( (char)0x015B );
                    break;
        
                case 0x015C : 
                    sb.append( (char)0x015D );
                    break;
        
                case 0x015E : 
                    sb.append( (char)0x015F );
                    break;
        
                case 0x0160 : 
                    sb.append( (char)0x0161 );
                    break;
        
                case 0x0162 : 
                    sb.append( (char)0x0163 );
                    break;
        
                case 0x0164 : 
                    sb.append( (char)0x0165 );
                    break;
        
                case 0x0166 : 
                    sb.append( (char)0x0167 );
                    break;
        
                case 0x0168 : 
                    sb.append( (char)0x0169 );
                    break;
        
                case 0x016A : 
                    sb.append( (char)0x016B );
                    break;
        
                case 0x016C : 
                    sb.append( (char)0x016D );
                    break;
        
                case 0x016E : 
                    sb.append( (char)0x016F );
                    break;
        
                case 0x0170 : 
                    sb.append( (char)0x0171 );
                    break;
        
                case 0x0172 : 
                    sb.append( (char)0x0173 );
                    break;
        
                case 0x0174 : 
                    sb.append( (char)0x0175 );
                    break;
        
                case 0x0176 : 
                    sb.append( (char)0x0177 );
                    break;
        
                case 0x0178 : 
                    sb.append( (char)0x00FF );
                    break;
        
                case 0x0179 : 
                    sb.append( (char)0x017A );
                    break;
        
                case 0x017B : 
                    sb.append( (char)0x017C );
                    break;
        
                case 0x017D : 
                    sb.append( (char)0x017E );
                    break;
        
                case 0x017F : 
                    sb.append( (char)0x0073 );
                    break;
        
                case 0x0181 : 
                    sb.append( (char)0x0253 );
                    break;
        
                case 0x0182 : 
                    sb.append( (char)0x0183 );
                    break;
        
                case 0x0184 : 
                    sb.append( (char)0x0185 );
                    break;
        
                case 0x0186 : 
                    sb.append( (char)0x0254 );
                    break;
        
                case 0x0187 : 
                    sb.append( (char)0x0188 );
                    break;
        
                case 0x0189 : 
                    sb.append( (char)0x0256 );
                    break;
        
                case 0x018A : 
                    sb.append( (char)0x0257 );
                    break;
        
                case 0x018B : 
                    sb.append( (char)0x018C );
                    break;
        
                case 0x018E : 
                    sb.append( (char)0x01DD );
                    break;
        
                case 0x018F : 
                    sb.append( (char)0x0259 );
                    break;
        
                case 0x0190 : 
                    sb.append( (char)0x025B );
                    break;
        
                case 0x0191 : 
                    sb.append( (char)0x0192 );
                    break;
        
                case 0x0193 : 
                    sb.append( (char)0x0260 );
                    break;
        
                case 0x0194 : 
                    sb.append( (char)0x0263 );
                    break;
        
                case 0x0196 : 
                    sb.append( (char)0x0269 );
                    break;
        
                case 0x0197 : 
                    sb.append( (char)0x0268 );
                    break;
        
                case 0x0198 : 
                    sb.append( (char)0x0199 );
                    break;
        
                case 0x019C : 
                    sb.append( (char)0x026F );
                    break;
        
                case 0x019D : 
                    sb.append( (char)0x0272 );
                    break;
        
                case 0x019F : 
                    sb.append( (char)0x0275 );
                    break;
        
                case 0x01A0 : 
                    sb.append( (char)0x01A1 );
                    break;
        
                case 0x01A2 : 
                    sb.append( (char)0x01A3 );
                    break;
        
                case 0x01A4 : 
                    sb.append( (char)0x01A5 );
                    break;
        
                case 0x01A6 : 
                    sb.append( (char)0x0280 );
                    break;
        
                case 0x01A7 : 
                    sb.append( (char)0x01A8 );
                    break;
        
                case 0x01A9 : 
                    sb.append( (char)0x0283 );
                    break;
        
                case 0x01AC : 
                    sb.append( (char)0x01AD );
                    break;
        
                case 0x01AE : 
                    sb.append( (char)0x0288 );
                    break;
        
                case 0x01AF : 
                    sb.append( (char)0x01B0 );
                    break;
        
                case 0x01B1 : 
                    sb.append( (char)0x028A );
                    break;
        
                case 0x01B2 : 
                    sb.append( (char)0x028B );
                    break;
        
                case 0x01B3 : 
                    sb.append( (char)0x01B4 );
                    break;
        
                case 0x01B5 : 
                    sb.append( (char)0x01B6 );
                    break;
        
                case 0x01B7 : 
                    sb.append( (char)0x0292 );
                    break;
        
                case 0x01B8 : 
                    sb.append( (char)0x01B9 );
                    break;
        
                case 0x01BC : 
                    sb.append( (char)0x01BD );
                    break;
        
                case 0x01C4 : 
                    sb.append( (char)0x01C6 );
                    break;
        
                case 0x01C5 : 
                    sb.append( (char)0x01C6 );
                    break;
        
                case 0x01C7 : 
                    sb.append( (char)0x01C9 );
                    break;
        
                case 0x01C8 : 
                    sb.append( (char)0x01C9 );
                    break;
        
                case 0x01CA : 
                    sb.append( (char)0x01CC );
                    break;
        
                case 0x01CB : 
                    sb.append( (char)0x01CC );
                    break;
        
                case 0x01CD : 
                    sb.append( (char)0x01CE );
                    break;
        
                case 0x01CF : 
                    sb.append( (char)0x01D0 );
                    break;
        
                case 0x01D1 : 
                    sb.append( (char)0x01D2 );
                    break;
        
                case 0x01D3 : 
                    sb.append( (char)0x01D4 );
                    break;
        
                case 0x01D5 : 
                    sb.append( (char)0x01D6 );
                    break;
        
                case 0x01D7 : 
                    sb.append( (char)0x01D8 );
                    break;
        
                case 0x01D9 : 
                    sb.append( (char)0x01DA );
                    break;
        
                case 0x01DB : 
                    sb.append( (char)0x01DC );
                    break;
        
                case 0x01DE : 
                    sb.append( (char)0x01DF );
                    break;
        
                case 0x01E0 : 
                    sb.append( (char)0x01E1 );
                    break;
        
                case 0x01E2 : 
                    sb.append( (char)0x01E3 );
                    break;
        
                case 0x01E4 : 
                    sb.append( (char)0x01E5 );
                    break;
        
                case 0x01E6 : 
                    sb.append( (char)0x01E7 );
                    break;
        
                case 0x01E8 : 
                    sb.append( (char)0x01E9 );
                    break;
        
                case 0x01EA : 
                    sb.append( (char)0x01EB );
                    break;
        
                case 0x01EC : 
                    sb.append( (char)0x01ED );
                    break;
        
                case 0x01EE : 
                    sb.append( (char)0x01EF );
                    break;
        
                case 0x01F0 : 
                    sb.append( (char)0x006A );
                    sb.append( (char)0x030C );
                    break;
        
                case 0x01F1 : 
                    sb.append( (char)0x01F3 );
                    break;
        
                case 0x01F2 : 
                    sb.append( (char)0x01F3 );
                    break;
        
                case 0x01F4 : 
                    sb.append( (char)0x01F5 );
                    break;
        
                case 0x01F6 : 
                    sb.append( (char)0x0195 );
                    break;
        
                case 0x01F7 : 
                    sb.append( (char)0x01BF );
                    break;
        
                case 0x01F8 : 
                    sb.append( (char)0x01F9 );
                    break;
        
                case 0x01FA : 
                    sb.append( (char)0x01FB );
                    break;
        
                case 0x01FC : 
                    sb.append( (char)0x01FD );
                    break;
        
                case 0x01FE : 
                    sb.append( (char)0x01FF );
                    break;
        
                case 0x0200 : 
                    sb.append( (char)0x0201 );
                    break;
        
                case 0x0202 : 
                    sb.append( (char)0x0203 );
                    break;
        
                case 0x0204 : 
                    sb.append( (char)0x0205 );
                    break;
        
                case 0x0206 : 
                    sb.append( (char)0x0207 );
                    break;
        
                case 0x0208 : 
                    sb.append( (char)0x0209 );
                    break;
        
                case 0x020A : 
                    sb.append( (char)0x020B );
                    break;
        
                case 0x020C : 
                    sb.append( (char)0x020D );
                    break;
        
                case 0x020E : 
                    sb.append( (char)0x020F );
                    break;
        
                case 0x0210 : 
                    sb.append( (char)0x0211 );
                    break;
        
                case 0x0212 : 
                    sb.append( (char)0x0213 );
                    break;
        
                case 0x0214 : 
                    sb.append( (char)0x0215 );
                    break;
        
                case 0x0216 : 
                    sb.append( (char)0x0217 );
                    break;
        
                case 0x0218 : 
                    sb.append( (char)0x0219 );
                    break;
        
                case 0x021A : 
                    sb.append( (char)0x021B );
                    break;
        
                case 0x021C : 
                    sb.append( (char)0x021D );
                    break;
        
                case 0x021E : 
                    sb.append( (char)0x021F );
                    break;
        
                case 0x0220 : 
                    sb.append( (char)0x019E );
                    break;
        
                case 0x0222 : 
                    sb.append( (char)0x0223 );
                    break;
        
                case 0x0224 : 
                    sb.append( (char)0x0225 );
                    break;
        
                case 0x0226 : 
                    sb.append( (char)0x0227 );
                    break;
        
                case 0x0228 : 
                    sb.append( (char)0x0229 );
                    break;
        
                case 0x022A : 
                    sb.append( (char)0x022B );
                    break;
        
                case 0x022C : 
                    sb.append( (char)0x022D );
                    break;
        
                case 0x022E : 
                    sb.append( (char)0x022F );
                    break;
        
                case 0x0230 : 
                    sb.append( (char)0x0231 );
                    break;
        
                case 0x0232 : 
                    sb.append( (char)0x0233 );
                    break;
        
                case 0x0345 : 
                    sb.append( (char)0x03B9 );
                    break;
        
                case 0x034F : 
                    break;
        
                case 0x037A : 
                    sb.append( (char)0x0020 );
                    sb.append( (char)0x03B9 );
                    break;
        
                case 0x0386 : 
                    sb.append( (char)0x03AC );
                    break;
        
                case 0x0388 : 
                    sb.append( (char)0x03AD );
                    break;
        
                case 0x0389 : 
                    sb.append( (char)0x03AE );
                    break;
        
                case 0x038A : 
                    sb.append( (char)0x03AF );
                    break;
        
                case 0x038C : 
                    sb.append( (char)0x03CC );
                    break;
        
                case 0x038E : 
                    sb.append( (char)0x03CD );
                    break;
        
                case 0x038F : 
                    sb.append( (char)0x03CE );
                    break;
        
                case 0x0390 : 
                    sb.append( (char)0x03B9 );
                    sb.append( (char)0x0308 );
                    sb.append( (char)0x0301 );
                    break;
        
                case 0x0391 : 
                    sb.append( (char)0x03B1 );
                    break;
        
                case 0x0392 : 
                    sb.append( (char)0x03B2 );
                    break;
        
                case 0x0393 : 
                    sb.append( (char)0x03B3 );
                    break;
        
                case 0x0394 : 
                    sb.append( (char)0x03B4 );
                    break;
        
                case 0x0395 : 
                    sb.append( (char)0x03B5 );
                    break;
        
                case 0x0396 : 
                    sb.append( (char)0x03B6 );
                    break;
        
                case 0x0397 : 
                    sb.append( (char)0x03B7 );
                    break;
        
                case 0x0398 : 
                    sb.append( (char)0x03B8 );
                    break;
        
                case 0x0399 : 
                    sb.append( (char)0x03B9 );
                    break;
        
                case 0x039A : 
                    sb.append( (char)0x03BA );
                    break;
        
                case 0x039B : 
                    sb.append( (char)0x03BB );
                    break;
        
                case 0x039C : 
                    sb.append( (char)0x03BC );
                    break;
        
                case 0x039D : 
                    sb.append( (char)0x03BD );
                    break;
        
                case 0x039E : 
                    sb.append( (char)0x03BE );
                    break;
        
                case 0x039F : 
                    sb.append( (char)0x03BF );
                    break;
        
                case 0x03A0 : 
                    sb.append( (char)0x03C0 );
                    break;
        
                case 0x03A1 : 
                    sb.append( (char)0x03C1 );
                    break;
        
                case 0x03A3 : 
                    sb.append( (char)0x03C3 );
                    break;
        
                case 0x03A4 : 
                    sb.append( (char)0x03C4 );
                    break;
        
                case 0x03A5 : 
                    sb.append( (char)0x03C5 );
                    break;
        
                case 0x03A6 : 
                    sb.append( (char)0x03C6 );
                    break;
        
                case 0x03A7 : 
                    sb.append( (char)0x03C7 );
                    break;
        
                case 0x03A8 : 
                    sb.append( (char)0x03C8 );
                    break;
        
                case 0x03A9 : 
                    sb.append( (char)0x03C9 );
                    break;
        
                case 0x03AA : 
                    sb.append( (char)0x03CA );
                    break;
        
                case 0x03AB : 
                    sb.append( (char)0x03CB );
                    break;
        
                case 0x03B0 : 
                    sb.append( (char)0x03C5 );
                    sb.append( (char)0x0308 );
                    sb.append( (char)0x0301 );
                    break;
        
                case 0x03C2 : 
                    sb.append( (char)0x03C3 );
                    break;
        
                case 0x03D0 : 
                    sb.append( (char)0x03B2 );
                    break;
        
                case 0x03D1 : 
                    sb.append( (char)0x03B8 );
                    break;
        
                case 0x03D2 : 
                    sb.append( (char)0x03C5 );
                    break;
        
                case 0x03D3 : 
                    sb.append( (char)0x03CD );
                    break;
        
                case 0x03D4 : 
                    sb.append( (char)0x03CB );
                    break;
        
                case 0x03D5 : 
                    sb.append( (char)0x03C6 );
                    break;
        
                case 0x03D6 : 
                    sb.append( (char)0x03C0 );
                    break;
        
                case 0x03D8 : 
                    sb.append( (char)0x03D9 );
                    break;
        
                case 0x03DA : 
                    sb.append( (char)0x03DB );
                    break;
        
                case 0x03DC : 
                    sb.append( (char)0x03DD );
                    break;
        
                case 0x03DE : 
                    sb.append( (char)0x03DF );
                    break;
        
                case 0x03E0 : 
                    sb.append( (char)0x03E1 );
                    break;
        
                case 0x03E2 : 
                    sb.append( (char)0x03E3 );
                    break;
        
                case 0x03E4 : 
                    sb.append( (char)0x03E5 );
                    break;
        
                case 0x03E6 : 
                    sb.append( (char)0x03E7 );
                    break;
        
                case 0x03E8 : 
                    sb.append( (char)0x03E9 );
                    break;
        
                case 0x03EA : 
                    sb.append( (char)0x03EB );
                    break;
        
                case 0x03EC : 
                    sb.append( (char)0x03ED );
                    break;
        
                case 0x03EE : 
                    sb.append( (char)0x03EF );
                    break;
        
                case 0x03F0 : 
                    sb.append( (char)0x03BA );
                    break;
        
                case 0x03F1 : 
                    sb.append( (char)0x03C1 );
                    break;
        
                case 0x03F2 : 
                    sb.append( (char)0x03C3 );
                    break;
        
                case 0x03F4 : 
                    sb.append( (char)0x03B8 );
                    break;
        
                case 0x03F5 : 
                    sb.append( (char)0x03B5 );
                    break;
        
                case 0x0400 : 
                    sb.append( (char)0x0450 );
                    break;
        
                case 0x0401 : 
                    sb.append( (char)0x0451 );
                    break;
        
                case 0x0402 : 
                    sb.append( (char)0x0452 );
                    break;
        
                case 0x0403 : 
                    sb.append( (char)0x0453 );
                    break;
        
                case 0x0404 : 
                    sb.append( (char)0x0454 );
                    break;
        
                case 0x0405 : 
                    sb.append( (char)0x0455 );
                    break;
        
                case 0x0406 : 
                    sb.append( (char)0x0456 );
                    break;
        
                case 0x0407 : 
                    sb.append( (char)0x0457 );
                    break;
        
                case 0x0408 : 
                    sb.append( (char)0x0458 );
                    break;
        
                case 0x0409 : 
                    sb.append( (char)0x0459 );
                    break;
        
                case 0x040A : 
                    sb.append( (char)0x045A );
                    break;
        
                case 0x040B : 
                    sb.append( (char)0x045B );
                    break;
        
                case 0x040C : 
                    sb.append( (char)0x045C );
                    break;
        
                case 0x040D : 
                    sb.append( (char)0x045D );
                    break;
        
                case 0x040E : 
                    sb.append( (char)0x045E );
                    break;
        
                case 0x040F : 
                    sb.append( (char)0x045F );
                    break;
        
                case 0x0410 : 
                    sb.append( (char)0x0430 );
                    break;
        
                case 0x0411 : 
                    sb.append( (char)0x0431 );
                    break;
        
                case 0x0412 : 
                    sb.append( (char)0x0432 );
                    break;
        
                case 0x0413 : 
                    sb.append( (char)0x0433 );
                    break;
        
                case 0x0414 : 
                    sb.append( (char)0x0434 );
                    break;
        
                case 0x0415 : 
                    sb.append( (char)0x0435 );
                    break;
        
                case 0x0416 : 
                    sb.append( (char)0x0436 );
                    break;
        
                case 0x0417 : 
                    sb.append( (char)0x0437 );
                    break;
        
                case 0x0418 : 
                    sb.append( (char)0x0438 );
                    break;
        
                case 0x0419 : 
                    sb.append( (char)0x0439 );
                    break;
        
                case 0x041A : 
                    sb.append( (char)0x043A );
                    break;
        
                case 0x041B : 
                    sb.append( (char)0x043B );
                    break;
        
                case 0x041C : 
                    sb.append( (char)0x043C );
                    break;
        
                case 0x041D : 
                    sb.append( (char)0x043D );
                    break;
        
                case 0x041E : 
                    sb.append( (char)0x043E );
                    break;
        
                case 0x041F : 
                    sb.append( (char)0x043F );
                    break;
        
                case 0x0420 : 
                    sb.append( (char)0x0440 );
                    break;
        
                case 0x0421 : 
                    sb.append( (char)0x0441 );
                    break;
        
                case 0x0422 : 
                    sb.append( (char)0x0442 );
                    break;
        
                case 0x0423 : 
                    sb.append( (char)0x0443 );
                    break;
        
                case 0x0424 : 
                    sb.append( (char)0x0444 );
                    break;
        
                case 0x0425 : 
                    sb.append( (char)0x0445 );
                    break;
        
                case 0x0426 : 
                    sb.append( (char)0x0446 );
                    break;
        
                case 0x0427 : 
                    sb.append( (char)0x0447 );
                    break;
        
                case 0x0428 : 
                    sb.append( (char)0x0448 );
                    break;
        
                case 0x0429 : 
                    sb.append( (char)0x0449 );
                    break;
        
                case 0x042A : 
                    sb.append( (char)0x044A );
                    break;
        
                case 0x042B : 
                    sb.append( (char)0x044B );
                    break;
        
                case 0x042C : 
                    sb.append( (char)0x044C );
                    break;
        
                case 0x042D : 
                    sb.append( (char)0x044D );
                    break;
        
                case 0x042E : 
                    sb.append( (char)0x044E );
                    break;
        
                case 0x042F : 
                    sb.append( (char)0x044F );
                    break;
        
                case 0x0460 : 
                    sb.append( (char)0x0461 );
                    break;
        
                case 0x0462 : 
                    sb.append( (char)0x0463 );
                    break;
        
                case 0x0464 : 
                    sb.append( (char)0x0465 );
                    break;
        
                case 0x0466 : 
                    sb.append( (char)0x0467 );
                    break;
        
                case 0x0468 : 
                    sb.append( (char)0x0469 );
                    break;
        
                case 0x046A : 
                    sb.append( (char)0x046B );
                    break;
        
                case 0x046C : 
                    sb.append( (char)0x046D );
                    break;
        
                case 0x046E : 
                    sb.append( (char)0x046F );
                    break;
        
                case 0x0470 : 
                    sb.append( (char)0x0471 );
                    break;
        
                case 0x0472 : 
                    sb.append( (char)0x0473 );
                    break;
        
                case 0x0474 : 
                    sb.append( (char)0x0475 );
                    break;
        
                case 0x0476 : 
                    sb.append( (char)0x0477 );
                    break;
        
                case 0x0478 : 
                    sb.append( (char)0x0479 );
                    break;
        
                case 0x047A : 
                    sb.append( (char)0x047B );
                    break;
        
                case 0x047C : 
                    sb.append( (char)0x047D );
                    break;
        
                case 0x047E : 
                    sb.append( (char)0x047F );
                    break;
        
                case 0x0480 : 
                    sb.append( (char)0x0481 );
                    break;
        
                case 0x048A : 
                    sb.append( (char)0x048B );
                    break;
        
                case 0x048C : 
                    sb.append( (char)0x048D );
                    break;
        
                case 0x048E : 
                    sb.append( (char)0x048F );
                    break;
        
                case 0x0490 : 
                    sb.append( (char)0x0491 );
                    break;
        
                case 0x0492 : 
                    sb.append( (char)0x0493 );
                    break;
        
                case 0x0494 : 
                    sb.append( (char)0x0495 );
                    break;
        
                case 0x0496 : 
                    sb.append( (char)0x0497 );
                    break;
        
                case 0x0498 : 
                    sb.append( (char)0x0499 );
                    break;
        
                case 0x049A : 
                    sb.append( (char)0x049B );
                    break;
        
                case 0x049C : 
                    sb.append( (char)0x049D );
                    break;
        
                case 0x049E : 
                    sb.append( (char)0x049F );
                    break;
        
                case 0x04A0 : 
                    sb.append( (char)0x04A1 );
                    break;
        
                case 0x04A2 : 
                    sb.append( (char)0x04A3 );
                    break;
        
                case 0x04A4 : 
                    sb.append( (char)0x04A5 );
                    break;
        
                case 0x04A6 : 
                    sb.append( (char)0x04A7 );
                    break;
        
                case 0x04A8 : 
                    sb.append( (char)0x04A9 );
                    break;
        
                case 0x04AA : 
                    sb.append( (char)0x04AB );
                    break;
        
                case 0x04AC : 
                    sb.append( (char)0x04AD );
                    break;
        
                case 0x04AE : 
                    sb.append( (char)0x04AF );
                    break;
        
                case 0x04B0 : 
                    sb.append( (char)0x04B1 );
                    break;
        
                case 0x04B2 : 
                    sb.append( (char)0x04B3 );
                    break;
        
                case 0x04B4 : 
                    sb.append( (char)0x04B5 );
                    break;
        
                case 0x04B6 : 
                    sb.append( (char)0x04B7 );
                    break;
        
                case 0x04B8 : 
                    sb.append( (char)0x04B9 );
                    break;
        
                case 0x04BA : 
                    sb.append( (char)0x04BB );
                    break;
        
                case 0x04BC : 
                    sb.append( (char)0x04BD );
                    break;
        
                case 0x04BE : 
                    sb.append( (char)0x04BF );
                    break;
        
                case 0x04C1 : 
                    sb.append( (char)0x04C2 );
                    break;
        
                case 0x04C3 : 
                    sb.append( (char)0x04C4 );
                    break;
        
                case 0x04C5 : 
                    sb.append( (char)0x04C6 );
                    break;
        
                case 0x04C7 : 
                    sb.append( (char)0x04C8 );
                    break;
        
                case 0x04C9 : 
                    sb.append( (char)0x04CA );
                    break;
        
                case 0x04CB : 
                    sb.append( (char)0x04CC );
                    break;
        
                case 0x04CD : 
                    sb.append( (char)0x04CE );
                    break;
        
                case 0x04D0 : 
                    sb.append( (char)0x04D1 );
                    break;
        
                case 0x04D2 : 
                    sb.append( (char)0x04D3 );
                    break;
        
                case 0x04D4 : 
                    sb.append( (char)0x04D5 );
                    break;
        
                case 0x04D6 : 
                    sb.append( (char)0x04D7 );
                    break;
        
                case 0x04D8 : 
                    sb.append( (char)0x04D9 );
                    break;
        
                case 0x04DA : 
                    sb.append( (char)0x04DB );
                    break;
        
                case 0x04DC : 
                    sb.append( (char)0x04DD );
                    break;
        
                case 0x04DE : 
                    sb.append( (char)0x04DF );
                    break;
        
                case 0x04E0 : 
                    sb.append( (char)0x04E1 );
                    break;
        
                case 0x04E2 : 
                    sb.append( (char)0x04E3 );
                    break;
        
                case 0x04E4 : 
                    sb.append( (char)0x04E5 );
                    break;
        
                case 0x04E6 : 
                    sb.append( (char)0x04E7 );
                    break;
        
                case 0x04E8 : 
                    sb.append( (char)0x04E9 );
                    break;
        
                case 0x04EA : 
                    sb.append( (char)0x04EB );
                    break;
        
                case 0x04EC : 
                    sb.append( (char)0x04ED );
                    break;
        
                case 0x04EE : 
                    sb.append( (char)0x04EF );
                    break;
        
                case 0x04F0 : 
                    sb.append( (char)0x04F1 );
                    break;
        
                case 0x04F2 : 
                    sb.append( (char)0x04F3 );
                    break;
        
                case 0x04F4 : 
                    sb.append( (char)0x04F5 );
                    break;
        
                case 0x04F8 : 
                    sb.append( (char)0x04F9 );
                    break;
        
                case 0x0500 : 
                    sb.append( (char)0x0501 );
                    break;
        
                case 0x0502 : 
                    sb.append( (char)0x0503 );
                    break;
        
                case 0x0504 : 
                    sb.append( (char)0x0505 );
                    break;
        
                case 0x0506 : 
                    sb.append( (char)0x0507 );
                    break;
        
                case 0x0508 : 
                    sb.append( (char)0x0509 );
                    break;
        
                case 0x050A : 
                    sb.append( (char)0x050B );
                    break;
        
                case 0x050C : 
                    sb.append( (char)0x050D );
                    break;
        
                case 0x050E : 
                    sb.append( (char)0x050F );
                    break;
        
                case 0x0531 : 
                    sb.append( (char)0x0561 );
                    break;
        
                case 0x0532 : 
                    sb.append( (char)0x0562 );
                    break;
        
                case 0x0533 : 
                    sb.append( (char)0x0563 );
                    break;
        
                case 0x0534 : 
                    sb.append( (char)0x0564 );
                    break;
        
                case 0x0535 : 
                    sb.append( (char)0x0565 );
                    break;
        
                case 0x0536 : 
                    sb.append( (char)0x0566 );
                    break;
        
                case 0x0537 : 
                    sb.append( (char)0x0567 );
                    break;
        
                case 0x0538 : 
                    sb.append( (char)0x0568 );
                    break;
        
                case 0x0539 : 
                    sb.append( (char)0x0569 );
                    break;
        
                case 0x053A : 
                    sb.append( (char)0x056A );
                    break;
        
                case 0x053B : 
                    sb.append( (char)0x056B );
                    break;
        
                case 0x053C : 
                    sb.append( (char)0x056C );
                    break;
        
                case 0x053D : 
                    sb.append( (char)0x056D );
                    break;
        
                case 0x053E : 
                    sb.append( (char)0x056E );
                    break;
        
                case 0x053F : 
                    sb.append( (char)0x056F );
                    break;
        
                case 0x0540 : 
                    sb.append( (char)0x0570 );
                    break;
        
                case 0x0541 : 
                    sb.append( (char)0x0571 );
                    break;
        
                case 0x0542 : 
                    sb.append( (char)0x0572 );
                    break;
        
                case 0x0543 : 
                    sb.append( (char)0x0573 );
                    break;
        
                case 0x0544 : 
                    sb.append( (char)0x0574 );
                    break;
        
                case 0x0545 : 
                    sb.append( (char)0x0575 );
                    break;
        
                case 0x0546 : 
                    sb.append( (char)0x0576 );
                    break;
        
                case 0x0547 : 
                    sb.append( (char)0x0577 );
                    break;
        
                case 0x0548 : 
                    sb.append( (char)0x0578 );
                    break;
        
                case 0x0549 : 
                    sb.append( (char)0x0579 );
                    break;
        
                case 0x054A : 
                    sb.append( (char)0x057A );
                    break;
        
                case 0x054B : 
                    sb.append( (char)0x057B );
                    break;
        
                case 0x054C : 
                    sb.append( (char)0x057C );
                    break;
        
                case 0x054D : 
                    sb.append( (char)0x057D );
                    break;
        
                case 0x054E : 
                    sb.append( (char)0x057E );
                    break;
        
                case 0x054F : 
                    sb.append( (char)0x057F );
                    break;
        
                case 0x0550 : 
                    sb.append( (char)0x0580 );
                    break;
        
                case 0x0551 : 
                    sb.append( (char)0x0581 );
                    break;
        
                case 0x0552 : 
                    sb.append( (char)0x0582 );
                    break;
        
                case 0x0553 : 
                    sb.append( (char)0x0583 );
                    break;
        
                case 0x0554 : 
                    sb.append( (char)0x0584 );
                    break;
        
                case 0x0555 : 
                    sb.append( (char)0x0585 );
                    break;
        
                case 0x0556 : 
                    sb.append( (char)0x0586 );
                    break;
        
                case 0x0587 : 
                    sb.append( (char)0x0565 );
                    sb.append( (char)0x0582 );
                    break;
        
                case 0x06DD : 
                    break;
        
                case 0x070F : 
                    break;
        
                case 0x1680 :
                    sb.append( (char)0x0020 );
                    break;
        
                case 0x1806 : 
                    break;
        
                case 0x180B : 
                case 0x180C : 
                case 0x180D : 
                case 0x180E : 
                    break;
        
                    
                case 0x1E00 : 
                    sb.append( (char)0x1E01 );
                    break;
        
                case 0x1E02 : 
                    sb.append( (char)0x1E03 );
                    break;
        
                case 0x1E04 : 
                    sb.append( (char)0x1E05 );
                    break;
        
                case 0x1E06 : 
                    sb.append( (char)0x1E07 );
                    break;
        
                case 0x1E08 : 
                    sb.append( (char)0x1E09 );
                    break;
        
                case 0x1E0A : 
                    sb.append( (char)0x1E0B );
                    break;
        
                case 0x1E0C : 
                    sb.append( (char)0x1E0D );
                    break;
        
                case 0x1E0E : 
                    sb.append( (char)0x1E0F );
                    break;
        
                case 0x1E10 : 
                    sb.append( (char)0x1E11 );
                    break;
        
                case 0x1E12 : 
                    sb.append( (char)0x1E13 );
                    break;
        
                case 0x1E14 : 
                    sb.append( (char)0x1E15 );
                    break;
        
                case 0x1E16 : 
                    sb.append( (char)0x1E17 );
                    break;
        
                case 0x1E18 : 
                    sb.append( (char)0x1E19 );
                    break;
        
                case 0x1E1A : 
                    sb.append( (char)0x1E1B );
                    break;
        
                case 0x1E1C : 
                    sb.append( (char)0x1E1D );
                    break;
        
                case 0x1E1E : 
                    sb.append( (char)0x1E1F );
                    break;
        
                case 0x1E20 : 
                    sb.append( (char)0x1E21 );
                    break;
        
                case 0x1E22 : 
                    sb.append( (char)0x1E23 );
                    break;
        
                case 0x1E24 : 
                    sb.append( (char)0x1E25 );
                    break;
        
                case 0x1E26 : 
                    sb.append( (char)0x1E27 );
                    break;
        
                case 0x1E28 : 
                    sb.append( (char)0x1E29 );
                    break;
        
                case 0x1E2A : 
                    sb.append( (char)0x1E2B );
                    break;
        
                case 0x1E2C : 
                    sb.append( (char)0x1E2D );
                    break;
        
                case 0x1E2E : 
                    sb.append( (char)0x1E2F );
                    break;
        
                case 0x1E30 : 
                    sb.append( (char)0x1E31 );
                    break;
        
                case 0x1E32 : 
                    sb.append( (char)0x1E33 );
                    break;
        
                case 0x1E34 : 
                    sb.append( (char)0x1E35 );
                    break;
        
                case 0x1E36 : 
                    sb.append( (char)0x1E37 );
                    break;
        
                case 0x1E38 : 
                    sb.append( (char)0x1E39 );
                    break;
        
                case 0x1E3A : 
                    sb.append( (char)0x1E3B );
                    break;
        
                case 0x1E3C : 
                    sb.append( (char)0x1E3D );
                    break;
        
                case 0x1E3E : 
                    sb.append( (char)0x1E3F );
                    break;
        
                case 0x1E40 : 
                    sb.append( (char)0x1E41 );
                    break;
        
                case 0x1E42 : 
                    sb.append( (char)0x1E43 );
                    break;
        
                case 0x1E44 : 
                    sb.append( (char)0x1E45 );
                    break;
        
                case 0x1E46 : 
                    sb.append( (char)0x1E47 );
                    break;
        
                case 0x1E48 : 
                    sb.append( (char)0x1E49 );
                    break;
        
                case 0x1E4A : 
                    sb.append( (char)0x1E4B );
                    break;
        
                case 0x1E4C : 
                    sb.append( (char)0x1E4D );
                    break;
        
                case 0x1E4E : 
                    sb.append( (char)0x1E4F );
                    break;
        
                case 0x1E50 : 
                    sb.append( (char)0x1E51 );
                    break;
        
                case 0x1E52 : 
                    sb.append( (char)0x1E53 );
                    break;
        
                case 0x1E54 : 
                    sb.append( (char)0x1E55 );
                    break;
        
                case 0x1E56 : 
                    sb.append( (char)0x1E57 );
                    break;
        
                case 0x1E58 : 
                    sb.append( (char)0x1E59 );
                    break;
        
                case 0x1E5A : 
                    sb.append( (char)0x1E5B );
                    break;
        
                case 0x1E5C : 
                    sb.append( (char)0x1E5D );
                    break;
        
                case 0x1E5E : 
                    sb.append( (char)0x1E5F );
                    break;
        
                case 0x1E60 : 
                    sb.append( (char)0x1E61 );
                    break;
        
                case 0x1E62 : 
                    sb.append( (char)0x1E63 );
                    break;
        
                case 0x1E64 : 
                    sb.append( (char)0x1E65 );
                    break;
        
                case 0x1E66 : 
                    sb.append( (char)0x1E67 );
                    break;
        
                case 0x1E68 : 
                    sb.append( (char)0x1E69 );
                    break;
        
                case 0x1E6A : 
                    sb.append( (char)0x1E6B );
                    break;
        
                case 0x1E6C : 
                    sb.append( (char)0x1E6D );
                    break;
        
                case 0x1E6E : 
                    sb.append( (char)0x1E6F );
                    break;
        
                case 0x1E70 : 
                    sb.append( (char)0x1E71 );
                    break;
        
                case 0x1E72 : 
                    sb.append( (char)0x1E73 );
                    break;
        
                case 0x1E74 : 
                    sb.append( (char)0x1E75 );
                    break;
        
                case 0x1E76 : 
                    sb.append( (char)0x1E77 );
                    break;
        
                case 0x1E78 : 
                    sb.append( (char)0x1E79 );
                    break;
        
                case 0x1E7A : 
                    sb.append( (char)0x1E7B );
                    break;
        
                case 0x1E7C : 
                    sb.append( (char)0x1E7D );
                    break;
        
                case 0x1E7E : 
                    sb.append( (char)0x1E7F );
                    break;
        
                case 0x1E80 : 
                    sb.append( (char)0x1E81 );
                    break;
        
                case 0x1E82 : 
                    sb.append( (char)0x1E83 );
                    break;
        
                case 0x1E84 : 
                    sb.append( (char)0x1E85 );
                    break;
        
                case 0x1E86 : 
                    sb.append( (char)0x1E87 );
                    break;
        
                case 0x1E88 : 
                    sb.append( (char)0x1E89 );
                    break;
        
                case 0x1E8A : 
                    sb.append( (char)0x1E8B );
                    break;
        
                case 0x1E8C : 
                    sb.append( (char)0x1E8D );
                    break;
        
                case 0x1E8E : 
                    sb.append( (char)0x1E8F );
                    break;
        
                case 0x1E90 : 
                    sb.append( (char)0x1E91 );
                    break;
        
                case 0x1E92 : 
                    sb.append( (char)0x1E93 );
                    break;
        
                case 0x1E94 : 
                    sb.append( (char)0x1E95 );
                    break;
        
                case 0x1E96 : 
                    sb.append( (char)0x0068 );
                    sb.append( (char)0x0331 );
                    break;
        
                case 0x1E97 : 
                    sb.append( (char)0x0074 );
                    sb.append( (char)0x0308 );
                    break;
        
                case 0x1E98 : 
                    sb.append( (char)0x0077 );
                    sb.append( (char)0x030A );
                    break;
        
                case 0x1E99 : 
                    sb.append( (char)0x0079 );
                    sb.append( (char)0x030A );
                    break;
        
                case 0x1E9A : 
                    sb.append( (char)0x0061 );
                    sb.append( (char)0x02BE );
                    break;
        
                case 0x1E9B : 
                    sb.append( (char)0x1E61 );
                    break;
        
                case 0x1EA0 : 
                    sb.append( (char)0x1EA1 );
                    break;
        
                case 0x1EA2 : 
                    sb.append( (char)0x1EA3 );
                    break;
        
                case 0x1EA4 : 
                    sb.append( (char)0x1EA5 );
                    break;
        
                case 0x1EA6 : 
                    sb.append( (char)0x1EA7 );
                    break;
        
                case 0x1EA8 : 
                    sb.append( (char)0x1EA9 );
                    break;
        
                case 0x1EAA : 
                    sb.append( (char)0x1EAB );
                    break;
        
                case 0x1EAC : 
                    sb.append( (char)0x1EAD );
                    break;
        
                case 0x1EAE : 
                    sb.append( (char)0x1EAF );
                    break;
        
                case 0x1EB0 : 
                    sb.append( (char)0x1EB1 );
                    break;
        
                case 0x1EB2 : 
                    sb.append( (char)0x1EB3 );
                    break;
        
                case 0x1EB4 : 
                    sb.append( (char)0x1EB5 );
                    break;
        
                case 0x1EB6 : 
                    sb.append( (char)0x1EB7 );
                    break;
        
                case 0x1EB8 : 
                    sb.append( (char)0x1EB9 );
                    break;
        
                case 0x1EBA : 
                    sb.append( (char)0x1EBB );
                    break;
        
                case 0x1EBC : 
                    sb.append( (char)0x1EBD );
                    break;
        
                case 0x1EBE : 
                    sb.append( (char)0x1EBF );
                    break;
        
                case 0x1EC0 : 
                    sb.append( (char)0x1EC1 );
                    break;
        
                case 0x1EC2 : 
                    sb.append( (char)0x1EC3 );
                    break;
        
                case 0x1EC4 : 
                    sb.append( (char)0x1EC5 );
                    break;
        
                case 0x1EC6 : 
                    sb.append( (char)0x1EC7 );
                    break;
        
                case 0x1EC8 : 
                    sb.append( (char)0x1EC9 );
                    break;
        
                case 0x1ECA : 
                    sb.append( (char)0x1ECB );
                    break;
        
                case 0x1ECC : 
                    sb.append( (char)0x1ECD );
                    break;
        
                case 0x1ECE : 
                    sb.append( (char)0x1ECF );
                    break;
        
                case 0x1ED0 : 
                    sb.append( (char)0x1ED1 );
                    break;
        
                case 0x1ED2 : 
                    sb.append( (char)0x1ED3 );
                    break;
        
                case 0x1ED4 : 
                    sb.append( (char)0x1ED5 );
                    break;
        
                case 0x1ED6 : 
                    sb.append( (char)0x1ED7 );
                    break;
        
                case 0x1ED8 : 
                    sb.append( (char)0x1ED9 );
                    break;
        
                case 0x1EDA : 
                    sb.append( (char)0x1EDB );
                    break;
        
                case 0x1EDC : 
                    sb.append( (char)0x1EDD );
                    break;
        
                case 0x1EDE : 
                    sb.append( (char)0x1EDF );
                    break;
        
                case 0x1EE0 : 
                    sb.append( (char)0x1EE1 );
                    break;
        
                case 0x1EE2 : 
                    sb.append( (char)0x1EE3 );
                    break;
        
                case 0x1EE4 : 
                    sb.append( (char)0x1EE5 );
                    break;
        
                case 0x1EE6 : 
                    sb.append( (char)0x1EE7 );
                    break;
        
                case 0x1EE8 : 
                    sb.append( (char)0x1EE9 );
                    break;
        
                case 0x1EEA : 
                    sb.append( (char)0x1EEB );
                    break;
        
                case 0x1EEC : 
                    sb.append( (char)0x1EED );
                    break;
        
                case 0x1EEE : 
                    sb.append( (char)0x1EEF );
                    break;
        
                case 0x1EF0 : 
                    sb.append( (char)0x1EF1 );
                    break;
        
                case 0x1EF2 : 
                    sb.append( (char)0x1EF3 );
                    break;
        
                case 0x1EF4 : 
                    sb.append( (char)0x1EF5 );
                    break;
        
                case 0x1EF6 : 
                    sb.append( (char)0x1EF7 );
                    break;
        
                case 0x1EF8 : 
                    sb.append( (char)0x1EF9 );
                    break;
        
                case 0x1F08 : 
                    sb.append( (char)0x1F00 );
                    break;
        
                case 0x1F09 : 
                    sb.append( (char)0x1F01 );
                    break;
        
                case 0x1F0A : 
                    sb.append( (char)0x1F02 );
                    break;
        
                case 0x1F0B : 
                    sb.append( (char)0x1F03 );
                    break;
        
                case 0x1F0C : 
                    sb.append( (char)0x1F04 );
                    break;
        
                case 0x1F0D : 
                    sb.append( (char)0x1F05 );
                    break;
        
                case 0x1F0E : 
                    sb.append( (char)0x1F06 );
                    break;
        
                case 0x1F0F : 
                    sb.append( (char)0x1F07 );
                    break;
        
                case 0x1F18 : 
                    sb.append( (char)0x1F10 );
                    break;
        
                case 0x1F19 : 
                    sb.append( (char)0x1F11 );
                    break;
        
                case 0x1F1A : 
                    sb.append( (char)0x1F12 );
                    break;
        
                case 0x1F1B : 
                    sb.append( (char)0x1F13 );
                    break;
        
                case 0x1F1C : 
                    sb.append( (char)0x1F14 );
                    break;
        
                case 0x1F1D : 
                    sb.append( (char)0x1F15 );
                    break;
        
                case 0x1F28 : 
                    sb.append( (char)0x1F20 );
                    break;
        
                case 0x1F29 : 
                    sb.append( (char)0x1F21 );
                    break;
        
                case 0x1F2A : 
                    sb.append( (char)0x1F22 );
                    break;
        
                case 0x1F2B : 
                    sb.append( (char)0x1F23 );
                    break;
        
                case 0x1F2C : 
                    sb.append( (char)0x1F24 );
                    break;
        
                case 0x1F2D : 
                    sb.append( (char)0x1F25 );
                    break;
        
                case 0x1F2E : 
                    sb.append( (char)0x1F26 );
                    break;
        
                case 0x1F2F : 
                    sb.append( (char)0x1F27 );
                    break;
        
                case 0x1F38 : 
                    sb.append( (char)0x1F30 );
                    break;
        
                case 0x1F39 : 
                    sb.append( (char)0x1F31 );
                    break;
        
                case 0x1F3A : 
                    sb.append( (char)0x1F32 );
                    break;
        
                case 0x1F3B : 
                    sb.append( (char)0x1F33 );
                    break;
        
                case 0x1F3C : 
                    sb.append( (char)0x1F34 );
                    break;
        
                case 0x1F3D : 
                    sb.append( (char)0x1F35 );
                    break;
        
                case 0x1F3E : 
                    sb.append( (char)0x1F36 );
                    break;
        
                case 0x1F3F : 
                    sb.append( (char)0x1F37 );
                    break;
        
                case 0x1F48 : 
                    sb.append( (char)0x1F40 );
                    break;
        
                case 0x1F49 : 
                    sb.append( (char)0x1F41 );
                    break;
        
                case 0x1F4A : 
                    sb.append( (char)0x1F42 );
                    break;
        
                case 0x1F4B : 
                    sb.append( (char)0x1F43 );
                    break;
        
                case 0x1F4C : 
                    sb.append( (char)0x1F44 );
                    break;
        
                case 0x1F4D : 
                    sb.append( (char)0x1F45 );
                    break;
        
                case 0x1F50 : 
                    sb.append( (char)0x03C5 );
                    sb.append( (char)0x0313 );
                    break;
        
                case 0x1F52 : 
                    sb.append( (char)0x03C5 );
                    sb.append( (char)0x0313 );
                    sb.append( (char)0x0300 );
                    break;
        
                case 0x1F54 : 
                    sb.append( (char)0x03C5 );
                    sb.append( (char)0x0313 );
                    sb.append( (char)0x0301 );
                    break;
        
                case 0x1F56 : 
                    sb.append( (char)0x03C5 );
                    sb.append( (char)0x0313 );
                    sb.append( (char)0x0342 );
                    break;
        
                case 0x1F59 : 
                    sb.append( (char)0x1F51 );
                    break;
        
                case 0x1F5B : 
                    sb.append( (char)0x1F53 );
                    break;
        
                case 0x1F5D : 
                    sb.append( (char)0x1F55 );
                    break;
        
                case 0x1F5F : 
                    sb.append( (char)0x1F57 );
                    break;
        
                case 0x1F68 : 
                    sb.append( (char)0x1F60 );
                    break;
        
                case 0x1F69 : 
                    sb.append( (char)0x1F61 );
                    break;
        
                case 0x1F6A : 
                    sb.append( (char)0x1F62 );
                    break;
        
                case 0x1F6B : 
                    sb.append( (char)0x1F63 );
                    break;
        
                case 0x1F6C : 
                    sb.append( (char)0x1F64 );
                    break;
        
                case 0x1F6D : 
                    sb.append( (char)0x1F65 );
                    break;
        
                case 0x1F6E : 
                    sb.append( (char)0x1F66 );
                    break;
        
                case 0x1F6F : 
                    sb.append( (char)0x1F67 );
                    break;
        
                case 0x1F80 : 
                    sb.append( (char)0x1F00 );
                    sb.append( (char)0x03B9 );
                    break;
        
                case 0x1F81 : 
                    sb.append( (char)0x1F01 );
                    sb.append( (char)0x03B9 );
                    break;
        
                case 0x1F82 : 
                    sb.append( (char)0x1F02 );
                    sb.append( (char)0x03B9 );
                    break;
        
                case 0x1F83 : 
                    sb.append( (char)0x1F03 );
                    sb.append( (char)0x03B9 );
                    break;
        
                case 0x1F84 : 
                    sb.append( (char)0x1F04 );
                    sb.append( (char)0x03B9 );
                    break;
        
                case 0x1F85 : 
                    sb.append( (char)0x1F05 );
                    sb.append( (char)0x03B9 );
                    break;
        
                case 0x1F86 : 
                    sb.append( (char)0x1F06 );
                    sb.append( (char)0x03B9 );
                    break;
        
                case 0x1F87 : 
                    sb.append( (char)0x1F07 );
                    sb.append( (char)0x03B9 );
                    break;
        
                case 0x1F88 : 
                    sb.append( (char)0x1F00 );
                    sb.append( (char)0x03B9 );
                    break;
        
                case 0x1F89 : 
                    sb.append( (char)0x1F01 );
                    sb.append( (char)0x03B9 );
                    break;
        
                case 0x1F8A : 
                    sb.append( (char)0x1F02 );
                    sb.append( (char)0x03B9 );
                    break;
        
                case 0x1F8B : 
                    sb.append( (char)0x1F03 );
                    sb.append( (char)0x03B9 );
                    break;
        
                case 0x1F8C : 
                    sb.append( (char)0x1F04 );
                    sb.append( (char)0x03B9 );
                    break;
        
                case 0x1F8D : 
                    sb.append( (char)0x1F05 );
                    sb.append( (char)0x03B9 );
                    break;
        
                case 0x1F8E : 
                    sb.append( (char)0x1F06 );
                    sb.append( (char)0x03B9 );
                    break;
        
                case 0x1F8F : 
                    sb.append( (char)0x1F07 );
                    sb.append( (char)0x03B9 );
                    break;
        
                case 0x1F90 : 
                    sb.append( (char)0x1F20 );
                    sb.append( (char)0x03B9 );
                    break;
        
                case 0x1F91 : 
                    sb.append( (char)0x1F21 );
                    sb.append( (char)0x03B9 );
                    break;
        
                case 0x1F92 : 
                    sb.append( (char)0x1F22 );
                    sb.append( (char)0x03B9 );
                    break;
        
                case 0x1F93 : 
                    sb.append( (char)0x1F23 );
                    sb.append( (char)0x03B9 );
                    break;
        
                case 0x1F94 : 
                    sb.append( (char)0x1F24 );
                    sb.append( (char)0x03B9 );
                    break;
        
                case 0x1F95 : 
                    sb.append( (char)0x1F25 );
                    sb.append( (char)0x03B9 );
                    break;
        
                case 0x1F96 : 
                    sb.append( (char)0x1F26 );
                    sb.append( (char)0x03B9 );
                    break;
        
                case 0x1F97 : 
                    sb.append( (char)0x1F27 );
                    sb.append( (char)0x03B9 );
                    break;
        
                case 0x1F98 : 
                    sb.append( (char)0x1F20 );
                    sb.append( (char)0x03B9 );
                    break;
        
                case 0x1F99 : 
                    sb.append( (char)0x1F21 );
                    sb.append( (char)0x03B9 );
                    break;
        
                case 0x1F9A : 
                    sb.append( (char)0x1F22 );
                    sb.append( (char)0x03B9 );
                    break;
        
                case 0x1F9B : 
                    sb.append( (char)0x1F23 );
                    sb.append( (char)0x03B9 );
                    break;
        
                case 0x1F9C : 
                    sb.append( (char)0x1F24 );
                    sb.append( (char)0x03B9 );
                    break;
        
                case 0x1F9D : 
                    sb.append( (char)0x1F25 );
                    sb.append( (char)0x03B9 );
                    break;
        
                case 0x1F9E : 
                    sb.append( (char)0x1F26 );
                    sb.append( (char)0x03B9 );
                    break;
        
                case 0x1F9F : 
                    sb.append( (char)0x1F27 );
                    sb.append( (char)0x03B9 );
                    break;
        
                case 0x1FA0 : 
                    sb.append( (char)0x1F60 );
                    sb.append( (char)0x03B9 );
                    break;
        
                case 0x1FA1 : 
                    sb.append( (char)0x1F61 );
                    sb.append( (char)0x03B9 );
                    break;
        
                case 0x1FA2 : 
                    sb.append( (char)0x1F62 );
                    sb.append( (char)0x03B9 );
                    break;
        
                case 0x1FA3 : 
                    sb.append( (char)0x1F63 );
                    sb.append( (char)0x03B9 );
                    break;
        
                case 0x1FA4 : 
                    sb.append( (char)0x1F64 );
                    sb.append( (char)0x03B9 );
                    break;
        
                case 0x1FA5 : 
                    sb.append( (char)0x1F65 );
                    sb.append( (char)0x03B9 );
                    break;
        
                case 0x1FA6 : 
                    sb.append( (char)0x1F66 );
                    sb.append( (char)0x03B9 );
                    break;
        
                case 0x1FA7 : 
                    sb.append( (char)0x1F67 );
                    sb.append( (char)0x03B9 );
                    break;
        
                case 0x1FA8 : 
                    sb.append( (char)0x1F60 );
                    sb.append( (char)0x03B9 );
                    break;
        
                case 0x1FA9 : 
                    sb.append( (char)0x1F61 );
                    sb.append( (char)0x03B9 );
                    break;
        
                case 0x1FAA : 
                    sb.append( (char)0x1F62 );
                    sb.append( (char)0x03B9 );
                    break;
        
                case 0x1FAB : 
                    sb.append( (char)0x1F63 );
                    sb.append( (char)0x03B9 );
                    break;
        
                case 0x1FAC : 
                    sb.append( (char)0x1F64 );
                    sb.append( (char)0x03B9 );
                    break;
        
                case 0x1FAD : 
                    sb.append( (char)0x1F65 );
                    sb.append( (char)0x03B9 );
                    break;
        
                case 0x1FAE : 
                    sb.append( (char)0x1F66 );
                    sb.append( (char)0x03B9 );
                    break;
        
                case 0x1FAF : 
                    sb.append( (char)0x1F67 );
                    sb.append( (char)0x03B9 );
                    break;
        
                case 0x1FB2 : 
                    sb.append( (char)0x1F70 );
                    sb.append( (char)0x03B9 );
                    break;
        
                case 0x1FB3 : 
                    sb.append( (char)0x03B1 );
                    sb.append( (char)0x03B9 );
                    break;
        
                case 0x1FB4 : 
                    sb.append( (char)0x03AC );
                    sb.append( (char)0x03B9 );
                    break;
        
                case 0x1FB6 : 
                    sb.append( (char)0x03B1 );
                    sb.append( (char)0x0342 );
                    break;
        
                case 0x1FB7 : 
                    sb.append( (char)0x03B1 );
                    sb.append( (char)0x0342 );
                    sb.append( (char)0x03B9 );
                    break;
        
                case 0x1FB8 : 
                    sb.append( (char)0x1FB0 );
                    break;
        
                case 0x1FB9 : 
                    sb.append( (char)0x1FB1 );
                    break;
        
                case 0x1FBA : 
                    sb.append( (char)0x1F70 );
                    break;
        
                case 0x1FBB : 
                    sb.append( (char)0x1F71 );
                    break;
        
                case 0x1FBC : 
                    sb.append( (char)0x03B1 );
                    sb.append( (char)0x03B9 );
                    break;
        
                case 0x1FBE : 
                    sb.append( (char)0x03B9 );
                    break;
        
                case 0x1FC2 : 
                    sb.append( (char)0x1F74 );
                    sb.append( (char)0x03B9 );
                    break;
        
                case 0x1FC3 : 
                    sb.append( (char)0x03B7 );
                    sb.append( (char)0x03B9 );
                    break;
        
                case 0x1FC4 : 
                    sb.append( (char)0x03AE );
                    sb.append( (char)0x03B9 );
                    break;
        
                case 0x1FC6 : 
                    sb.append( (char)0x03B7 );
                    sb.append( (char)0x0342 );
                    break;
        
                case 0x1FC7 : 
                    sb.append( (char)0x03B7 );
                    sb.append( (char)0x0342 );
                    sb.append( (char)0x03B9 );
                    break;
        
                case 0x1FC8 : 
                    sb.append( (char)0x1F72 );
                    break;
        
                case 0x1FC9 : 
                    sb.append( (char)0x1F73 );
                    break;
        
                case 0x1FCA : 
                    sb.append( (char)0x1F74 );
                    break;
        
                case 0x1FCB : 
                    sb.append( (char)0x1F75 );
                    break;
        
                case 0x1FCC : 
                    sb.append( (char)0x03B7 );
                    sb.append( (char)0x03B9 );
                    break;
        
                case 0x1FD2 : 
                    sb.append( (char)0x03B9 );
                    sb.append( (char)0x0308 );
                    sb.append( (char)0x0300 );
                    break;
        
                case 0x1FD3 : 
                    sb.append( (char)0x03B9 );
                    sb.append( (char)0x0308 );
                    sb.append( (char)0x0301 );
                    break;
        
                case 0x1FD6 : 
                    sb.append( (char)0x03B9 );
                    sb.append( (char)0x0342 );
                    break;
        
                case 0x1FD7 : 
                    sb.append( (char)0x03B9 );
                    sb.append( (char)0x0308 );
                    sb.append( (char)0x0342 );
                    break;
        
                case 0x1FD8 : 
                    sb.append( (char)0x1FD0 );
                    break;
        
                case 0x1FD9 : 
                    sb.append( (char)0x1FD1 );
                    break;
        
                case 0x1FDA : 
                    sb.append( (char)0x1F76 );
                    break;
        
                case 0x1FDB : 
                    sb.append( (char)0x1F77 );
                    break;
        
                case 0x1FE2 : 
                    sb.append( (char)0x03C5 );
                    sb.append( (char)0x0308 );
                    sb.append( (char)0x0300 );
                    break;
        
                case 0x1FE3 : 
                    sb.append( (char)0x03C5 );
                    sb.append( (char)0x0308 );
                    sb.append( (char)0x0301 );
                    break;
        
                case 0x1FE4 : 
                    sb.append( (char)0x03C1 );
                    sb.append( (char)0x0313 );
                    break;
        
                case 0x1FE6 : 
                    sb.append( (char)0x03C5 );
                    sb.append( (char)0x0342 );
                    break;
        
                case 0x1FE7 : 
                    sb.append( (char)0x03C5 );
                    sb.append( (char)0x0308 );
                    sb.append( (char)0x0342 );
                    break;
        
                case 0x1FE8 : 
                    sb.append( (char)0x1FE0 );
                    break;
        
                case 0x1FE9 : 
                    sb.append( (char)0x1FE1 );
                    break;
        
                case 0x1FEA : 
                    sb.append( (char)0x1F7A );
                    break;
        
                case 0x1FEB : 
                    sb.append( (char)0x1F7B );
                    break;
        
                case 0x1FEC : 
                    sb.append( (char)0x1FE5 );
                    break;
        
                case 0x1FF2 : 
                    sb.append( (char)0x1F7C );
                    sb.append( (char)0x03B9 );
                    break;
        
                case 0x1FF3 : 
                    sb.append( (char)0x03C9 );
                    sb.append( (char)0x03B9 );
                    break;
        
                case 0x1FF4 : 
                    sb.append( (char)0x03CE );
                    sb.append( (char)0x03B9 );
                    break;
        
                case 0x1FF6 : 
                    sb.append( (char)0x03C9 );
                    sb.append( (char)0x0342 );
                    break;
        
                case 0x1FF7 : 
                    sb.append( (char)0x03C9 );
                    sb.append( (char)0x0342 );
                    sb.append( (char)0x03B9 );
                    break;
        
                case 0x1FF8 : 
                    sb.append( (char)0x1F78 );
                    break;
        
                case 0x1FF9 : 
                    sb.append( (char)0x1F79 );
                    break;
        
                case 0x1FFA : 
                    sb.append( (char)0x1F7C );
                    break;
        
                case 0x1FFB : 
                    sb.append( (char)0x1F7D );
                    break;
        
                case 0x1FFC : 
                    sb.append( (char)0x03C9 );
                    sb.append( (char)0x03B9 );
                    break;
        
                case 0x2000 :
                case 0x2001 :
                case 0x2002 :
                case 0x2003 :
                case 0x2004 :
                case 0x2005 :
                case 0x2006 :
                case 0x2007 :
                case 0x2008 :
                case 0x2009 :
                case 0x200A :
                    sb.append( (char)0x0020 );
                    break;
        
                case 0x200B :
                case 0x200C :
                case 0x200D :
                case 0x200E :
                case 0x200F :
                    break;
        
                case 0x2028 :
                case 0x2029 :
                    sb.append( (char)0x0020 );
                    break;
        
                case 0x202A :
                case 0x202B :
                case 0x202C :
                case 0x202D :
                case 0x202E :
                    break;
        
                case 0x202F :
                    sb.append( (char)0x0020 );
                    break;
        
                case 0x205F :
                    sb.append( (char)0x0020 );
                    break;
        
                case 0x2060 :
                case 0x2061 :
                case 0x2062 :
                case 0x2063 :
                    break;
        
                case 0x206A :
                case 0x206B :
                case 0x206C :
                case 0x206D :
                case 0x206E :
                case 0x206F :
                    break;
        
                case 0x20A8 : 
                    sb.append( (char)0x0072 );
                    sb.append( (char)0x0073 );
                    break;
        
                case 0x2102 : 
                    sb.append( (char)0x0063 );
                    break;
        
                case 0x2103 : 
                    sb.append( (char)0x00B0 );
                    sb.append( (char)0x0063 );
                    break;
        
                case 0x2107 : 
                    sb.append( (char)0x025B );
                    break;
        
                case 0x2109 : 
                    sb.append( (char)0x00B0 );
                    sb.append( (char)0x0066 );
                    break;
        
                case 0x210B : 
                    sb.append( (char)0x0068 );
                    break;
        
                case 0x210C : 
                    sb.append( (char)0x0068 );
                    break;
        
                case 0x210D : 
                    sb.append( (char)0x0068 );
                    break;
        
                case 0x2110 : 
                    sb.append( (char)0x0069 );
                    break;
        
                case 0x2111 : 
                    sb.append( (char)0x0069 );
                    break;
        
                case 0x2112 : 
                    sb.append( (char)0x006C );
                    break;
        
                case 0x2115 : 
                    sb.append( (char)0x006E );
                    break;
        
                case 0x2116 : 
                    sb.append( (char)0x006E );
                    sb.append( (char)0x006F );
                    break;
        
                case 0x2119 : 
                    sb.append( (char)0x0070 );
                    break;
        
                case 0x211A : 
                    sb.append( (char)0x0071 );
                    break;
        
                case 0x211B : 
                    sb.append( (char)0x0072 );
                    break;
        
                case 0x211C : 
                    sb.append( (char)0x0072 );
                    break;
        
                case 0x211D : 
                    sb.append( (char)0x0072 );
                    break;
        
                case 0x2120 : 
                    sb.append( (char)0x0073 );
                    sb.append( (char)0x006D );
                    break;
        
                case 0x2121 : 
                    sb.append( (char)0x0074 );
                    sb.append( (char)0x0065 );
                    sb.append( (char)0x006C );
                    break;
        
                case 0x2122 : 
                    sb.append( (char)0x0074 );
                    sb.append( (char)0x006D );
                    break;
        
                case 0x2124 : 
                    sb.append( (char)0x007A );
                    break;
        
                case 0x2126 : 
                    sb.append( (char)0x03C9 );
                    break;
        
                case 0x2128 : 
                    sb.append( (char)0x007A );
                    break;
        
                case 0x212A : 
                    sb.append( (char)0x006B );
                    break;
        
                case 0x212B : 
                    sb.append( (char)0x00E5 );
                    break;
        
                case 0x212C : 
                    sb.append( (char)0x0062 );
                    break;
        
                case 0x212D : 
                    sb.append( (char)0x0063 );
                    break;
        
                case 0x2130 : 
                    sb.append( (char)0x0065 );
                    break;
        
                case 0x2131 : 
                    sb.append( (char)0x0066 );
                    break;
        
                case 0x2133 : 
                    sb.append( (char)0x006D );
                    break;
        
                case 0x213E : 
                    sb.append( (char)0x03B3 );
                    break;
        
                case 0x213F : 
                    sb.append( (char)0x03C0 );
                    break;
        
                case 0x2145 : 
                    sb.append( (char)0x0064 );
                    break;
        
                case 0x2160 : 
                    sb.append( (char)0x2170 );
                    break;
        
                case 0x2161 : 
                    sb.append( (char)0x2171 );
                    break;
        
                case 0x2162 : 
                    sb.append( (char)0x2172 );
                    break;
        
                case 0x2163 : 
                    sb.append( (char)0x2173 );
                    break;
        
                case 0x2164 : 
                    sb.append( (char)0x2174 );
                    break;
        
                case 0x2165 : 
                    sb.append( (char)0x2175 );
                    break;
        
                case 0x2166 : 
                    sb.append( (char)0x2176 );
                    break;
        
                case 0x2167 : 
                    sb.append( (char)0x2177 );
                    break;
        
                case 0x2168 : 
                    sb.append( (char)0x2178 );
                    break;
        
                case 0x2169 : 
                    sb.append( (char)0x2179 );
                    break;
        
                case 0x216A : 
                    sb.append( (char)0x217A );
                    break;
        
                case 0x216B : 
                    sb.append( (char)0x217B );
                    break;
        
                case 0x216C : 
                    sb.append( (char)0x217C );
                    break;
        
                case 0x216D : 
                    sb.append( (char)0x217D );
                    break;
        
                case 0x216E : 
                    sb.append( (char)0x217E );
                    break;
        
                case 0x216F : 
                    sb.append( (char)0x217F );
                    break;
        
                case 0x24B6 : 
                    sb.append( (char)0x24D0 );
                    break;
        
                case 0x24B7 : 
                    sb.append( (char)0x24D1 );
                    break;
        
                case 0x24B8 : 
                    sb.append( (char)0x24D2 );
                    break;
        
                case 0x24B9 : 
                    sb.append( (char)0x24D3 );
                    break;
        
                case 0x24BA : 
                    sb.append( (char)0x24D4 );
                    break;
        
                case 0x24BB : 
                    sb.append( (char)0x24D5 );
                    break;
        
                case 0x24BC : 
                    sb.append( (char)0x24D6 );
                    break;
        
                case 0x24BD : 
                    sb.append( (char)0x24D7 );
                    break;
        
                case 0x24BE : 
                    sb.append( (char)0x24D8 );
                    break;
        
                case 0x24BF : 
                    sb.append( (char)0x24D9 );
                    break;
        
                case 0x24C0 : 
                    sb.append( (char)0x24DA );
                    break;
        
                case 0x24C1 : 
                    sb.append( (char)0x24DB );
                    break;
        
                case 0x24C2 : 
                    sb.append( (char)0x24DC );
                    break;
        
                case 0x24C3 : 
                    sb.append( (char)0x24DD );
                    break;
        
                case 0x24C4 : 
                    sb.append( (char)0x24DE );
                    break;
        
                case 0x24C5 : 
                    sb.append( (char)0x24DF );
                    break;
        
                case 0x24C6 : 
                    sb.append( (char)0x24E0 );
                    break;
        
                case 0x24C7 : 
                    sb.append( (char)0x24E1 );
                    break;
        
                case 0x24C8 : 
                    sb.append( (char)0x24E2 );
                    break;
        
                case 0x24C9 : 
                    sb.append( (char)0x24E3 );
                    break;
        
                case 0x24CA : 
                    sb.append( (char)0x24E4 );
                    break;
        
                case 0x24CB : 
                    sb.append( (char)0x24E5 );
                    break;
        
                case 0x24CC : 
                    sb.append( (char)0x24E6 );
                    break;
        
                case 0x24CD : 
                    sb.append( (char)0x24E7 );
                    break;
        
                case 0x24CE : 
                    sb.append( (char)0x24E8 );
                    break;
        
                case 0x24CF : 
                    sb.append( (char)0x24E9 );
                    break;
        
                case 0x3000 :
                    sb.append( (char)0x0020 );
                    break;
        
                case 0x3371 : 
                    sb.append( (char)0x0068 );
                    sb.append( (char)0x0070 );
                    sb.append( (char)0x0061 );
                    break;
        
                case 0x3373 : 
                    sb.append( (char)0x0061 );
                    sb.append( (char)0x0075 );
                    break;
        
                case 0x3375 : 
                    sb.append( (char)0x006F );
                    sb.append( (char)0x0076 );
                    break;
        
                case 0x3380 : 
                    sb.append( (char)0x0070 );
                    sb.append( (char)0x0061 );
                    break;
        
                case 0x3381 : 
                    sb.append( (char)0x006E );
                    sb.append( (char)0x0061 );
                    break;
        
                case 0x3382 : 
                    sb.append( (char)0x03BC );
                    sb.append( (char)0x0061 );
                    break;
        
                case 0x3383 : 
                    sb.append( (char)0x006D );
                    sb.append( (char)0x0061 );
                    break;
        
                case 0x3384 : 
                    sb.append( (char)0x006B );
                    sb.append( (char)0x0061 );
                    break;
        
                case 0x3385 : 
                    sb.append( (char)0x006B );
                    sb.append( (char)0x0062 );
                    break;
        
                case 0x3386 : 
                    sb.append( (char)0x006D );
                    sb.append( (char)0x0062 );
                    break;
        
                case 0x3387 : 
                    sb.append( (char)0x0067 );
                    sb.append( (char)0x0062 );
                    break;
        
                case 0x338A : 
                    sb.append( (char)0x0070 );
                    sb.append( (char)0x0066 );
                    break;
        
                case 0x338B : 
                    sb.append( (char)0x006E );
                    sb.append( (char)0x0066 );
                    break;
        
                case 0x338C : 
                    sb.append( (char)0x03BC );
                    sb.append( (char)0x0066 );
                    break;
        
                case 0x3390 : 
                    sb.append( (char)0x0068 );
                    sb.append( (char)0x007A );
                    break;
        
                case 0x3391 : 
                    sb.append( (char)0x006B );
                    sb.append( (char)0x0068 );
                    sb.append( (char)0x007A );
                    break;
        
                case 0x3392 : 
                    sb.append( (char)0x006D );
                    sb.append( (char)0x0068 );
                    sb.append( (char)0x007A );
                    break;
        
                case 0x3393 : 
                    sb.append( (char)0x0067 );
                    sb.append( (char)0x0068 );
                    sb.append( (char)0x007A );
                    break;
        
                case 0x3394 : 
                    sb.append( (char)0x0074 );
                    sb.append( (char)0x0068 );
                    sb.append( (char)0x007A );
                    break;
        
                case 0x33A9 : 
                    sb.append( (char)0x0070 );
                    sb.append( (char)0x0061 );
                    break;
        
                case 0x33AA : 
                    sb.append( (char)0x006B );
                    sb.append( (char)0x0070 );
                    sb.append( (char)0x0061 );
                    break;
        
                case 0x33AB : 
                    sb.append( (char)0x006D );
                    sb.append( (char)0x0070 );
                    sb.append( (char)0x0061 );
                    break;
        
                case 0x33AC : 
                    sb.append( (char)0x0067 );
                    sb.append( (char)0x0070 );
                    sb.append( (char)0x0061 );
                    break;
        
                case 0x33B4 : 
                    sb.append( (char)0x0070 );
                    sb.append( (char)0x0076 );
                    break;
        
                case 0x33B5 : 
                    sb.append( (char)0x006E );
                    sb.append( (char)0x0076 );
                    break;
        
                case 0x33B6 : 
                    sb.append( (char)0x03BC );
                    sb.append( (char)0x0076 );
                    break;
        
                case 0x33B7 : 
                    sb.append( (char)0x006D );
                    sb.append( (char)0x0076 );
                    break;
        
                case 0x33B8 : 
                    sb.append( (char)0x006B );
                    sb.append( (char)0x0076 );
                    break;
        
                case 0x33B9 : 
                    sb.append( (char)0x006D );
                    sb.append( (char)0x0076 );
                    break;
        
                case 0x33BA : 
                    sb.append( (char)0x0070 );
                    sb.append( (char)0x0077 );
                    break;
        
                case 0x33BB : 
                    sb.append( (char)0x006E );
                    sb.append( (char)0x0077 );
                    break;
        
                case 0x33BC : 
                    sb.append( (char)0x03BC );
                    sb.append( (char)0x0077 );
                    break;
        
                case 0x33BD : 
                    sb.append( (char)0x006D );
                    sb.append( (char)0x0077 );
                    break;
        
                case 0x33BE : 
                    sb.append( (char)0x006B );
                    sb.append( (char)0x0077 );
                    break;
        
                case 0x33BF : 
                    sb.append( (char)0x006D );
                    sb.append( (char)0x0077 );
                    break;
        
                case 0x33C0 : 
                    sb.append( (char)0x006B );
                    sb.append( (char)0x03C9 );
                    break;
        
                case 0x33C1 : 
                    sb.append( (char)0x006D );
                    sb.append( (char)0x03C9 );
                    break;
        
                case 0x33C3 : 
                    sb.append( (char)0x0062 );
                    sb.append( (char)0x0071 );
                    break;
        
                case 0x33C6 : 
                    sb.append( (char)0x0063 );
                    sb.append( (char)0x2215 );
                    sb.append( (char)0x006B );
                    sb.append( (char)0x0067 );
                    break;
        
                case 0x33C7 : 
                    sb.append( (char)0x0063 );
                    sb.append( (char)0x006F );
                    sb.append( (char)0x002E );
                    break;
        
                case 0x33C8 : 
                    sb.append( (char)0x0064 );
                    sb.append( (char)0x0062 );
                    break;
        
                case 0x33C9 : 
                    sb.append( (char)0x0067 );
                    sb.append( (char)0x0079 );
                    break;
        
                case 0x33CB : 
                    sb.append( (char)0x0068 );
                    sb.append( (char)0x0070 );
                    break;
        
                case 0x33CD : 
                    sb.append( (char)0x006B );
                    sb.append( (char)0x006B );
                    break;
        
                case 0x33CE : 
                    sb.append( (char)0x006B );
                    sb.append( (char)0x006D );
                    break;
        
                case 0x33D7 : 
                    sb.append( (char)0x0070 );
                    sb.append( (char)0x0068 );
                    break;
        
                case 0x33D9 : 
                    sb.append( (char)0x0070 );
                    sb.append( (char)0x0070 );
                    sb.append( (char)0x006D );
                    break;
        
                case 0x33DA : 
                    sb.append( (char)0x0070 );
                    sb.append( (char)0x0072 );
                    break;
        
                case 0x33DC : 
                    sb.append( (char)0x0073 );
                    sb.append( (char)0x0076 );
                    break;
        
                case 0x33DD : 
                    sb.append( (char)0x0077 );
                    sb.append( (char)0x0062 );
                    break;
        
                case 0xFB00 : 
                    sb.append( (char)0x0066 );
                    sb.append( (char)0x0066 );
                    break;
        
                case 0xFB01 : 
                    sb.append( (char)0x0066 );
                    sb.append( (char)0x0069 );
                    break;
        
                case 0xFB02 : 
                    sb.append( (char)0x0066 );
                    sb.append( (char)0x006C );
                    break;
        
                case 0xFB03 : 
                    sb.append( (char)0x0066 );
                    sb.append( (char)0x0066 );
                    sb.append( (char)0x0069 );
                    break;
        
                case 0xFB04 : 
                    sb.append( (char)0x0066 );
                    sb.append( (char)0x0066 );
                    sb.append( (char)0x006C );
                    break;
        
                case 0xFB05 : 
                    sb.append( (char)0x0073 );
                    sb.append( (char)0x0074 );
                    break;
        
                case 0xFB06 : 
                    sb.append( (char)0x0073 );
                    sb.append( (char)0x0074 );
                    break;
        
                case 0xFB13 : 
                    sb.append( (char)0x0574 );
                    sb.append( (char)0x0576 );
                    break;
        
                case 0xFB14 : 
                    sb.append( (char)0x0574 );
                    sb.append( (char)0x0565 );
                    break;
        
                case 0xFB15 : 
                    sb.append( (char)0x0574 );
                    sb.append( (char)0x056B );
                    break;
        
                case 0xFB16 : 
                    sb.append( (char)0x057E );
                    sb.append( (char)0x0576 );
                    break;
        
                case 0xFB17 : 
                    sb.append( (char)0x0574 );
                    sb.append( (char)0x056D );
                    break;
        
                case 0xFE00 :
                case 0xFE01 :
                case 0xFE02 :
                case 0xFE03 :
                case 0xFE04 :
                case 0xFE05 :
                case 0xFE06 :
                case 0xFE07 :
                case 0xFE08 :
                case 0xFE09 :
                case 0xFE0A :
                case 0xFE0B :
                case 0xFE0C :
                case 0xFE0D :
                case 0xFE0E :
                case 0xFE0F :
                    break;
        
                case 0xFEFF :
                    break;
        
                case 0xFF21 : 
                    sb.append( (char)0xFF41 );
                    break;
        
                case 0xFF22 : 
                    sb.append( (char)0xFF42 );
                    break;
        
                case 0xFF23 : 
                    sb.append( (char)0xFF43 );
                    break;
        
                case 0xFF24 : 
                    sb.append( (char)0xFF44 );
                    break;
        
                case 0xFF25 : 
                    sb.append( (char)0xFF45 );
                    break;
        
                case 0xFF26 : 
                    sb.append( (char)0xFF46 );
                    break;
        
                case 0xFF27 : 
                    sb.append( (char)0xFF47 );
                    break;
        
                case 0xFF28 : 
                    sb.append( (char)0xFF48 );
                    break;
        
                case 0xFF29 : 
                    sb.append( (char)0xFF49 );
                    break;
        
                case 0xFF2A : 
                    sb.append( (char)0xFF4A );
                    break;
        
                case 0xFF2B : 
                    sb.append( (char)0xFF4B );
                    break;
        
                case 0xFF2C : 
                    sb.append( (char)0xFF4C );
                    break;
        
                case 0xFF2D : 
                    sb.append( (char)0xFF4D );
                    break;
        
                case 0xFF2E : 
                    sb.append( (char)0xFF4E );
                    break;
        
                case 0xFF2F : 
                    sb.append( (char)0xFF4F );
                    break;
        
                case 0xFF30 : 
                    sb.append( (char)0xFF50 );
                    break;
        
                case 0xFF31 : 
                    sb.append( (char)0xFF51 );
                    break;
        
                case 0xFF32 : 
                    sb.append( (char)0xFF52 );
                    break;
        
                case 0xFF33 : 
                    sb.append( (char)0xFF53 );
                    break;
        
                case 0xFF34 : 
                    sb.append( (char)0xFF54 );
                    break;
        
                case 0xFF35 : 
                    sb.append( (char)0xFF55 );
                    break;
        
                case 0xFF36 : 
                    sb.append( (char)0xFF56 );
                    break;
        
                case 0xFF37 : 
                    sb.append( (char)0xFF57 );
                    break;
        
                case 0xFF38 : 
                    sb.append( (char)0xFF58 );
                    break;
        
                case 0xFF39 : 
                    sb.append( (char)0xFF59 );
                    break;
        
                case 0xFF3A : 
                    sb.append( (char)0xFF5A );
                    break;
                    
                case 0xFFF9 :
                case 0xFFFA :
                case 0xFFFB :
                case 0xFFFC :
                    break;
        
                default :
                    sb.append( c );
            }
        }
        
        return sb;
    }
    
    /**
     * 
     * Prohibit characters described in RFC 4518 :
     *  - Table A.1 of RFC 3454
     *  - Table C.3 of RFC 3454
     *  - Table C.4 of RFC 3454
     *  - Table C.5 of RFC 3454
     *  - Table C.8 of RFC 3454
     *  - character U-FFFD
     *
     * @param str The String to analyze
     * @throws InvalidCharacterException If any character is prohibited
     */
    public static void prohibit( String str ) throws InvalidCharacterException
    {
        prohibit( str.toCharArray() );
    }
    
    /**
     * 
     * Prohibit characters described in RFC 4518 :
     *  - Table A.1 of RFC 3454
     *  - Table C.3 of RFC 3454
     *  - Table C.4 of RFC 3454
     *  - Table C.5 of RFC 3454
     *  - Table C.8 of RFC 3454
     *  - character U-FFFD
     *
     * @param array The char array to analyze
     * @throws InvalidCharacterException If any character is prohibited
     */
    public static void prohibit( char[] array ) throws InvalidCharacterException
    {
        if ( array == null )
        {
            return;
        }

        for ( char c:array )
        {
            // RFC 3454, Table A.1
            switch ( c )
            {
                case 0x0221 :
                case 0x038B :
                case 0x038D :
                case 0x03A2 :
                case 0x03CF :
                case 0x0487 :
                case 0x04CF :
                case 0x0560 :
                case 0x0588 :
                case 0x05A2 :
                case 0x05BA :
                case 0x0620 :
                case 0x06FF :
                case 0x070E :
                case 0x0904 :
                case 0x0984 :
                case 0x09A9 :
                case 0x09B1 :
                case 0x09BD :
                case 0x09DE :
                case 0x0A29 :
                case 0x0A31 :
                case 0x0A34 :
                case 0x0A37 :
                case 0x0A3D :
                case 0x0A5D :
                case 0x0A84 :
                case 0x0A8C :
                case 0x0A8E :
                case 0x0A92 :
                case 0x0AA9 :
                case 0x0AB1 :
                case 0x0AB4 :
                case 0x0AC6 :
                case 0x0ACA :
                case 0x0B04 :
                case 0x0B29 :
                case 0x0B31 :
                case 0x0B5E :
                case 0x0B84 :
                case 0x0B91 :
                case 0x0B9B :
                case 0x0B9D :
                case 0x0BB6 :
                case 0x0BC9 :
                case 0x0C04 :
                case 0x0C0D :
                case 0x0C11 :
                case 0x0C29 :
                case 0x0C34 :
                case 0x0C45 :
                case 0x0C49 :
                case 0x0C84 :
                case 0x0C8D :
                case 0x0C91 :
                case 0x0CA9 :
                case 0x0CB4 :
                case 0x0CC5 :
                case 0x0CC9 :
                case 0x0CDF :
                case 0x0D04 :
                case 0x0D0D :
                case 0x0D11 :
                case 0x0D29 :
                case 0x0D49 :
                case 0x0D84 :
                case 0x0DB2 :
                case 0x0DBC :
                case 0x0DD5 :
                case 0x0DD7 :
                case 0x0E83 :
                case 0x0E89 :
                case 0x0E98 :
                case 0x0EA0 :
                case 0x0EA4 :
                case 0x0EA6 :
                case 0x0EAC :
                case 0x0EBA :
                case 0x0EC5 :
                case 0x0EC7 :
                case 0x0F48 :
                case 0x0F98 :
                case 0x0FBD :
                case 0x1022 :
                case 0x1028 :
                case 0x102B :
                case 0x1207 :
                case 0x1247 :
                case 0x1249 :
                case 0x1257 :
                case 0x1259 :
                case 0x1287 :
                case 0x1289 :
                case 0x12AF :
                case 0x12B1 :
                case 0x12BF :
                case 0x12C1 :
                case 0x12CF :
                case 0x12D7 :
                case 0x12EF :
                case 0x130F :
                case 0x1311 :
                case 0x131F :
                case 0x1347 :
                case 0x170D :
                case 0x176D :
                case 0x1771 :
                case 0x180F :
                case 0x1F58 :
                case 0x1F5A :
                case 0x1F5C :
                case 0x1F5E :
                case 0x1FB5 :
                case 0x1FC5 :
                case 0x1FDC :
                case 0x1FF5 :
                case 0x1FFF :
                case 0x24FF :
                case 0x2618 :
                case 0x2705 :
                case 0x2728 :
                case 0x274C :
                case 0x274E :
                case 0x2757 :
                case 0x27B0 :
                case 0x2E9A :
                case 0x3040 :
                case 0x318F :
                case 0x32FF :
                case 0x33FF :
                case 0xFB37 :
                case 0xFB3D :
                case 0xFB3F :
                case 0xFB42 :
                case 0xFB45 :
                case 0xFE53 :
                case 0xFE67 :
                case 0xFE75 :
                case 0xFF00 :
                case 0xFFE7 :
                    throw new InvalidCharacterException( c );
            }
            
            // RFC 3454, Table A.1, intervals
            if ( ( c >= 0x0234 ) && ( c <= 0x024F ) ) 
            {
                throw new InvalidCharacterException( c );
            }

            if ( ( c >= 0x02AE ) && ( c <= 0x02AF ) ) 
            {
                throw new InvalidCharacterException( c );
            }

            if ( ( c >= 0x02EF ) && ( c <= 0x02FF ) ) 
            {
                throw new InvalidCharacterException( c );
            }

            if ( ( c >= 0x0350 ) && ( c <= 0x035F ) ) 
            {
                throw new InvalidCharacterException( c );
            }

            if ( ( c >= 0x0370 ) && ( c <= 0x0373 ) ) 
            {
                throw new InvalidCharacterException( c );
            }

            if ( ( c >= 0x0376 ) && ( c <= 0x0379 ) ) 
            {
                throw new InvalidCharacterException( c );
            }

            if ( ( c >= 0x037B ) && ( c <= 0x037D ) ) 
            {
                throw new InvalidCharacterException( c );
            }

            if ( ( c >= 0x037F ) && ( c <= 0x0383 ) ) 
            {
                throw new InvalidCharacterException( c );
            }

            if ( ( c >= 0x03F7 ) && ( c <= 0x03FF ) ) 
            {
                throw new InvalidCharacterException( c );
            }

            if ( ( c >= 0x04F6 ) && ( c <= 0x04F7 ) ) 
            {
                throw new InvalidCharacterException( c );
            }

            if ( ( c >= 0x04FA ) && ( c <= 0x04FF ) ) 
            {
                throw new InvalidCharacterException( c );
            }

            if ( ( c >= 0x0510 ) && ( c <= 0x0530 ) ) 
            {
                throw new InvalidCharacterException( c );
            }

            if ( ( c >= 0x0557 ) && ( c <= 0x0558 ) ) 
            {
                throw new InvalidCharacterException( c );
            }

            if ( ( c >= 0x058B ) && ( c <= 0x0590 ) ) 
            {
                throw new InvalidCharacterException( c );
            }

            if ( ( c >= 0x05C5 ) && ( c <= 0x05CF ) ) 
            {
                throw new InvalidCharacterException( c );
            }

            if ( ( c >= 0x05EB ) && ( c <= 0x05EF ) ) 
            {
                throw new InvalidCharacterException( c );
            }

            if ( ( c >= 0x05F5 ) && ( c <= 0x060B ) ) 
            {
                throw new InvalidCharacterException( c );
            }

            if ( ( c >= 0x060D ) && ( c <= 0x061A ) ) 
            {
                throw new InvalidCharacterException( c );
            }

            if ( ( c >= 0x061C ) && ( c <= 0x061E ) ) 
            {
                throw new InvalidCharacterException( c );
            }

            if ( ( c >= 0x063B ) && ( c <= 0x063F ) ) 
            {
                throw new InvalidCharacterException( c );
            }

            if ( ( c >= 0x0656 ) && ( c <= 0x065F ) ) 
            {
                throw new InvalidCharacterException( c );
            }

            if ( ( c >= 0x06EE ) && ( c <= 0x06EF ) ) 
            {
                throw new InvalidCharacterException( c );
            }

            if ( ( c >= 0x072D ) && ( c <= 0x072F ) ) 
            {
                throw new InvalidCharacterException( c );
            }

            if ( ( c >= 0x074B ) && ( c <= 0x077F ) ) 
            {
                throw new InvalidCharacterException( c );
            }

            if ( ( c >= 0x07B2 ) && ( c <= 0x0900 ) ) 
            {
                throw new InvalidCharacterException( c );
            }

            if ( ( c >= 0x093A ) && ( c <= 0x093B ) ) 
            {
                throw new InvalidCharacterException( c );
            }

            if ( ( c >= 0x094E ) && ( c <= 0x094F ) ) 
            {
                throw new InvalidCharacterException( c );
            }

            if ( ( c >= 0x0955 ) && ( c <= 0x0957 ) ) 
            {
                throw new InvalidCharacterException( c );
            }

            if ( ( c >= 0x0971 ) && ( c <= 0x0980 ) ) 
            {
                throw new InvalidCharacterException( c );
            }

            if ( ( c >= 0x098D ) && ( c <= 0x098E ) ) 
            {
                throw new InvalidCharacterException( c );
            }

            if ( ( c >= 0x0991 ) && ( c <= 0x0992 ) ) 
            {
                throw new InvalidCharacterException( c );
            }

            if ( ( c >= 0x09B3 ) && ( c <= 0x09B5 ) ) 
            {
                throw new InvalidCharacterException( c );
            }

            if ( ( c >= 0x09BA ) && ( c <= 0x09BB ) ) 
            {
                throw new InvalidCharacterException( c );
            }

            if ( ( c >= 0x09C5 ) && ( c <= 0x09C6 ) ) 
            {
                throw new InvalidCharacterException( c );
            }

            if ( ( c >= 0x09C9 ) && ( c <= 0x09CA ) ) 
            {
                throw new InvalidCharacterException( c );
            }

            if ( ( c >= 0x09CE ) && ( c <= 0x09D6 ) ) 
            {
                throw new InvalidCharacterException( c );
            }

            if ( ( c >= 0x09D8 ) && ( c <= 0x09DB ) ) 
            {
                throw new InvalidCharacterException( c );
            }

            if ( ( c >= 0x09E4 ) && ( c <= 0x09E5 ) ) 
            {
                throw new InvalidCharacterException( c );
            }

            if ( ( c >= 0x09FB ) && ( c <= 0x0A01 ) ) 
            {
                throw new InvalidCharacterException( c );
            }

            if ( ( c >= 0x0A03 ) && ( c <= 0x0A04 ) ) 
            {
                throw new InvalidCharacterException( c );
            }

            if ( ( c >= 0x0A0B ) && ( c <= 0x0A0E ) ) 
            {
                throw new InvalidCharacterException( c );
            }

            if ( ( c >= 0x0A11 ) && ( c <= 0x0A12 ) ) 
            {
                throw new InvalidCharacterException( c );
            }

            if ( ( c >= 0x0A3A ) && ( c <= 0x0A3B ) ) 
            {
                throw new InvalidCharacterException( c );
            }

            if ( ( c >= 0x0A43 ) && ( c <= 0x0A46 ) ) 
            {
                throw new InvalidCharacterException( c );
            }

            if ( ( c >= 0x0A49 ) && ( c <= 0x0A4A ) ) 
            {
                throw new InvalidCharacterException( c );
            }

            if ( ( c >= 0x0A4E ) && ( c <= 0x0A58 ) ) 
            {
                throw new InvalidCharacterException( c );
            }

            if ( ( c >= 0x0A5F ) && ( c <= 0x0A65 ) ) 
            {
                throw new InvalidCharacterException( c );
            }

            if ( ( c >= 0x0A75 ) && ( c <= 0x0A80 ) ) 
            {
                throw new InvalidCharacterException( c );
            }

            if ( ( c >= 0x0ABA ) && ( c <= 0x0ABB ) ) 
            {
                throw new InvalidCharacterException( c );
            }

            if ( ( c >= 0x0ACE ) && ( c <= 0x0ACF ) ) 
            {
                throw new InvalidCharacterException( c );
            }

            if ( ( c >= 0x0AD1 ) && ( c <= 0x0ADF ) ) 
            {
                throw new InvalidCharacterException( c );
            }

            if ( ( c >= 0x0AE1 ) && ( c <= 0x0AE5 ) ) 
            {
                throw new InvalidCharacterException( c );
            }

            if ( ( c >= 0x0AF0 ) && ( c <= 0x0B00 ) ) 
            {
                throw new InvalidCharacterException( c );
            }

            if ( ( c >= 0x0B0D ) && ( c <= 0x0B0E ) ) 
            {
                throw new InvalidCharacterException( c );
            }

            if ( ( c >= 0x0B11 ) && ( c <= 0x0B12 ) ) 
            {
                throw new InvalidCharacterException( c );
            }

            if ( ( c >= 0x0B34 ) && ( c <= 0x0B35 ) ) 
            {
                throw new InvalidCharacterException( c );
            }

            if ( ( c >= 0x0B3A ) && ( c <= 0x0B3B ) ) 
            {
                throw new InvalidCharacterException( c );
            }

            if ( ( c >= 0x0B44 ) && ( c <= 0x0B46 ) ) 
            {
                throw new InvalidCharacterException( c );
            }

            if ( ( c >= 0x0B49 ) && ( c <= 0x0B4A ) ) 
            {
                throw new InvalidCharacterException( c );
            }

            if ( ( c >= 0x0B4E ) && ( c <= 0x0B55 ) ) 
            {
                throw new InvalidCharacterException( c );
            }

            if ( ( c >= 0x0B58 ) && ( c <= 0x0B5B ) ) 
            {
                throw new InvalidCharacterException( c );
            }

            if ( ( c >= 0x0B62 ) && ( c <= 0x0B65 ) ) 
            {
                throw new InvalidCharacterException( c );
            }

            if ( ( c >= 0x0B71 ) && ( c <= 0x0B81 ) ) 
            {
                throw new InvalidCharacterException( c );
            }

            if ( ( c >= 0x0B8B ) && ( c <= 0x0B8D ) ) 
            {
                throw new InvalidCharacterException( c );
            }

            if ( ( c >= 0x0B96 ) && ( c <= 0x0B98 ) ) 
            {
                throw new InvalidCharacterException( c );
            }

            if ( ( c >= 0x0BA0 ) && ( c <= 0x0BA2 ) ) 
            {
                throw new InvalidCharacterException( c );
            }

            if ( ( c >= 0x0BA5 ) && ( c <= 0x0BA7 ) ) 
            {
                throw new InvalidCharacterException( c );
            }

            if ( ( c >= 0x0BAB ) && ( c <= 0x0BAD ) ) 
            {
                throw new InvalidCharacterException( c );
            }

            if ( ( c >= 0x0BBA ) && ( c <= 0x0BBD ) ) 
            {
                throw new InvalidCharacterException( c );
            }

            if ( ( c >= 0x0BC3 ) && ( c <= 0x0BC5 ) ) 
            {
                throw new InvalidCharacterException( c );
            }

            if ( ( c >= 0x0BCE ) && ( c <= 0x0BD6 ) ) 
            {
                throw new InvalidCharacterException( c );
            }

            if ( ( c >= 0x0BD8 ) && ( c <= 0x0BE6 ) ) 
            {
                throw new InvalidCharacterException( c );
            }

            if ( ( c >= 0x0BF3 ) && ( c <= 0x0C00 ) ) 
            {
                throw new InvalidCharacterException( c );
            }

            // RFC 3454, Table C.3
            if ( ( c >= 0xE000 ) && ( c <= 0xF8FF ) )
            {
                throw new InvalidCharacterException( c );
            }

            // RFC 3454, Table C.4
            if ( ( c >= 0xFDD0 ) && ( c <= 0xFDEF ) )
            {
                throw new InvalidCharacterException( c );
            }

            if ( ( c == 0xFFFE ) || ( c <= 0xFFFF ) )
            {
                throw new InvalidCharacterException( c );
            }

            // RFC 3454, Table C.5 (Surrogates)
            if ( ( c >= 0xD800 ) && ( c <= 0xDFFF ) )
            {
                throw new InvalidCharacterException( c );
            }

            // RFC 3454, Table C.8 
            switch ( c) 
            {
                case 0x0340 : // COMBINING GRAVE TONE MARK
                case 0x0341 : // COMBINING ACUTE TONE MARK
                case 0x200E : // LEFT-TO-RIGHT MARK
                case 0x200F : // RIGHT-TO-LEFT MARK
                case 0x202A : // LEFT-TO-RIGHT EMBEDDING
                case 0x202B : // RIGHT-TO-LEFT EMBEDDING
                case 0x202C : // POP DIRECTIONAL FORMATTING
                case 0x202D : // LEFT-TO-RIGHT OVERRIDE
                case 0x202E : // RIGHT-TO-LEFT OVERRIDE
                case 0x206A : // INHIBIT SYMMETRIC SWAPPING
                case 0x206B : // ACTIVATE SYMMETRIC SWAPPING
                case 0x206C : // INHIBIT ARABIC FORM SHAPING
                case 0x206D : // ACTIVATE ARABIC FORM SHAPING
                case 0x206E : // NATIONAL DIGIT SHAPES
                case 0x206F : // NOMINAL DIGIT SHAPES
                    throw new InvalidCharacterException( c );
            }
            
            if ( c == 0xFFFD ) 
            {
                throw new InvalidCharacterException( c );
            }
        }
    }
    
    /**
     * 
     * Remove all bidirectionnal chars
     *
     * @param str The string where bidi chars are to be removed
     * @return The cleaned string
     */
    public static String bidi( String str )
    {
        return bidi( str.toCharArray() ).toString();
    }
    
    /**
     * 
     * Remove all bidirectionnal chars
     *
     * @param array The char array where bidi chars are to be removed
     * @return The cleaned StringBuilder
     */
    public static StringBuilder bidi( char[] array )
    {
        if ( array == null )
        {
            return null;
        }

        StringBuilder sb = new StringBuilder( array.length );
        
        for ( char c:array )
        {
            // RFC 3454, Table D1
            switch ( c )
            {
                case 0x05BE :
                case 0x05C0 :
                case 0x05C3 :
                case 0x061B :
                case 0x061F :
                case 0x06DD :
                case 0x0710 :
                case 0x07B1 :
                case 0x200F :
                case 0xFB1D :
                case 0xFB3E :
                    continue;
            }
            
            // RFC 3454, Table D1, intervals
            if ( ( c >= 0x05D0 ) && ( c <= 0x05EA ) )
            {
                continue;
            }
            
            if ( ( c >= 0x05F0 ) && ( c <= 0x05F4 ) )
            {
                continue;
            }

            if ( ( c >= 0x0621 ) && ( c <= 0x063A ) )
            {
                continue;
            }

            if ( ( c >= 0x0640 ) && ( c <= 0x064A ) )
            {
                continue;
            }

            if ( ( c >= 0x066D ) && ( c <= 0x066F ) )
            {
                continue;
            }

            if ( ( c >= 0x0671 ) && ( c <= 0x06D5 ) )
            {
                continue;
            }
            
            if ( ( c >= 0x06E5 ) && ( c <= 0x06E6 ) )
            {
                continue;
            }

            if ( ( c >= 0x06FA ) && ( c <= 0x06FE ) )
            {
                continue;
            }

            if ( ( c >= 0x0700 ) && ( c <= 0x070D ) )
            {
                continue;
            }

            if ( ( c >= 0x0712 ) && ( c <= 0x072C ) )
            {
                continue;
            }

            if ( ( c >= 0x0780 ) && ( c <= 0x07A5 ) )
            {
                continue;
            }

            if ( ( c >= 0xFB1F ) && ( c <= 0xFB28 ) )
            {
                continue;
            }

            if ( ( c >= 0xFB2A ) && ( c <= 0xFB36 ) )
            {
                continue;
            }

            if ( ( c >= 0xFB38 ) && ( c <= 0xFB3C ) )
            {
                continue;
            }

            if ( ( c >= 0xFB40 ) && ( c <= 0xFB41 ) )
            {
                continue;
            }

            if ( ( c >= 0xFB43 ) && ( c <= 0xFB44 ) )
            {
                continue;
            }

            if ( ( c >= 0xFB46 ) && ( c <= 0xFBB1 ) )
            {
                continue;
            }

            if ( ( c >= 0xFBD3 ) && ( c <= 0xFD3D ) )
            {
                continue;
            }
            
            if ( ( c >= 0xFD50 ) && ( c <= 0xFD8F ) )
            {
                continue;
            }

            if ( ( c >= 0xFD92 ) && ( c <= 0xFDC7 ) )
            {
                continue;
            }

            if ( ( c >= 0xFDF0 ) && ( c <= 0xFDFC ) )
            {
                continue;
            }

            if ( ( c >= 0xFE70 ) && ( c <= 0xFE74 ) )
            {
                continue;
            }

            if ( ( c >= 0xFE76 ) && ( c <= 0xFEFC ) )
            {
                continue;
            }
            
            // RFC 3454, Table D.2
            switch ( c ) 
            {
                case 0x00AA :
                case 0x00B5 :
                case 0x00BA :
                case 0x02EE :
                case 0x037A :
                case 0x0386 :
                case 0x038C :
                case 0x0589 :
                case 0x0903 :
                case 0x0950 :
                case 0x09B2 :
                case 0x09D7 :
                case 0x0A5E :
                case 0x0A83 :
                case 0x0A8D :
                case 0x0AC9 :
                case 0x0AD0 :
                case 0x0AE0 :
                case 0x0B40 :
                case 0x0B57 :
                case 0x0B83 :
                case 0x0B9C :
                case 0x0BD7 :
                case 0x0CBE :
                case 0x0CDE :
                case 0x0D57 :
                case 0x0DBD :
                case 0x0E84 :
                case 0x0E8A :
                case 0x0E8D :
                case 0x0EA5 :
                case 0x0EA7 :
                case 0x0EBD :
                case 0x0EC6 :
                case 0x0F36 :
                case 0x0F38 :
                case 0x0F7F :
                case 0x0F85 :
                case 0x0FCF :
                case 0x102C :
                case 0x1031 :
                case 0x1038 :
                case 0x10FB :
                case 0x1248 :
                case 0x1258 :
                case 0x1288 :
                case 0x12B0 :
                case 0x12C0 :
                case 0x1310 :
                case 0x17DC :
                case 0x1F59 :
                case 0x1F5B :
                case 0x1F5D :
                case 0x1FBE :
                case 0x200E :
                case 0x2071 :
                case 0x207F :
                case 0x2102 :
                case 0x2107 :
                case 0x2115 :
                case 0x2124 :
                case 0x2126 :
                case 0x2128 :
                    continue;
            }
            
            // RFC 3454, Table D.2 intervals
            if ( ( c >= 0x0041 ) && ( c <= 0x005A ) ) 
            {
                continue;
            }

            if ( ( c >= 0x0061 ) && ( c <= 0x007A ) ) 
            {
                continue;
            }

            if ( ( c >= 0x00C0 ) && ( c <= 0x00D6 ) ) 
            {
                continue;
            }

            if ( ( c >= 0x00D8 ) && ( c <= 0x00F6 ) ) 
            {
                continue;
            }

            if ( ( c >= 0x00F8 ) && ( c <= 0x0220 ) ) 
            {
                continue;
            }

            if ( ( c >= 0x0222 ) && ( c <= 0x0233 ) ) 
            {
                continue;
            }

            if ( ( c >= 0x0250 ) && ( c <= 0x02AD ) ) 
            {
                continue;
            }

            if ( ( c >= 0x02B0 ) && ( c <= 0x02B8 ) ) 
            {
                continue;
            }

            if ( ( c >= 0x02BB ) && ( c <= 0x02C1 ) ) 
            {
                continue;
            }

            if ( ( c >= 0x02D0 ) && ( c <= 0x02D1 ) ) 
            {
                continue;
            }

            if ( ( c >= 0x02E0 ) && ( c <= 0x02E4 ) ) 
            {
                continue;
            }

            if ( ( c >= 0x0388 ) && ( c <= 0x038A ) ) 
            {
                continue;
            }

            if ( ( c >= 0x038E ) && ( c <= 0x03A1 ) ) 
            {
                continue;
            }

            if ( ( c >= 0x03A3 ) && ( c <= 0x03CE ) ) 
            {
                continue;
            }

            if ( ( c >= 0x03D0 ) && ( c <= 0x03F5 ) ) 
            {
                continue;
            }

            if ( ( c >= 0x0400 ) && ( c <= 0x0482 ) ) 
            {
                continue;
            }

            if ( ( c >= 0x048A ) && ( c <= 0x04CE ) ) 
            {
                continue;
            }

            if ( ( c >= 0x04D0 ) && ( c <= 0x04F5 ) ) 
            {
                continue;
            }

            if ( ( c >= 0x04F8 ) && ( c <= 0x04F9 ) ) 
            {
                continue;
            }

            if ( ( c >= 0x0500 ) && ( c <= 0x050F ) ) 
            {
                continue;
            }

            if ( ( c >= 0x0531 ) && ( c <= 0x0556 ) ) 
            {
                continue;
            }

            if ( ( c >= 0x0559 ) && ( c <= 0x055F ) ) 
            {
                continue;
            }

            if ( ( c >= 0x0561 ) && ( c <= 0x0587 ) ) 
            {
                continue;
            }

            if ( ( c >= 0x0905 ) && ( c <= 0x0939 ) ) 
            {
                continue;
            }

            if ( ( c >= 0x093D ) && ( c <= 0x0940 ) ) 
            {
                continue;
            }

            if ( ( c >= 0x0949 ) && ( c <= 0x094C ) ) 
            {
                continue;
            }

            if ( ( c >= 0x0958 ) && ( c <= 0x0961 ) ) 
            {
                continue;
            }

            if ( ( c >= 0x0964 ) && ( c <= 0x0970 ) ) 
            {
                continue;
            }

            if ( ( c >= 0x0982 ) && ( c <= 0x0983 ) ) 
            {
                continue;
            }

            if ( ( c >= 0x0985 ) && ( c <= 0x098C ) ) 
            {
                continue;
            }

            if ( ( c >= 0x098F ) && ( c <= 0x0990 ) ) 
            {
                continue;
            }

            if ( ( c >= 0x0993 ) && ( c <= 0x09A8 ) ) 
            {
                continue;
            }

            if ( ( c >= 0x09AA ) && ( c <= 0x09B0 ) ) 
            {
                continue;
            }

            if ( ( c >= 0x09B6 ) && ( c <= 0x09B9 ) ) 
            {
                continue;
            }

            if ( ( c >= 0x09BE ) && ( c <= 0x09C0 ) ) 
            {
                continue;
            }

            if ( ( c >= 0x09C7 ) && ( c <= 0x09C8 ) ) 
            {
                continue;
            }

            if ( ( c >= 0x09CB ) && ( c <= 0x09CC ) ) 
            {
                continue;
            }

            if ( ( c >= 0x09DC ) && ( c <= 0x09DD ) ) 
            {
                continue;
            }

            if ( ( c >= 0x09DF ) && ( c <= 0x09E1 ) ) 
            {
                continue;
            }

            if ( ( c >= 0x09E6 ) && ( c <= 0x09F1 ) ) 
            {
                continue;
            }

            if ( ( c >= 0x09F4 ) && ( c <= 0x09FA ) ) 
            {
                continue;
            }

            if ( ( c >= 0x0A05 ) && ( c <= 0x0A0A ) ) 
            {
                continue;
            }

            if ( ( c >= 0x0A0F ) && ( c <= 0x0A10 ) ) 
            {
                continue;
            }

            if ( ( c >= 0x0A13 ) && ( c <= 0x0A28 ) ) 
            {
                continue;
            }

            if ( ( c >= 0x0A2A ) && ( c <= 0x0A30 ) ) 
            {
                continue;
            }

            if ( ( c >= 0x0A32 ) && ( c <= 0x0A33 ) ) 
            {
                continue;
            }

            if ( ( c >= 0x0A35 ) && ( c <= 0x0A36 ) ) 
            {
                continue;
            }

            if ( ( c >= 0x0A38 ) && ( c <= 0x0A39 ) ) 
            {
                continue;
            }

            if ( ( c >= 0x0A3E ) && ( c <= 0x0A40 ) ) 
            {
                continue;
            }

            if ( ( c >= 0x0A59 ) && ( c <= 0x0A5C ) ) 
            {
                continue;
            }

            if ( ( c >= 0x0A66 ) && ( c <= 0x0A6F ) ) 
            {
                continue;
            }

            if ( ( c >= 0x0A72 ) && ( c <= 0x0A74 ) ) 
            {
                continue;
            }

            if ( ( c >= 0x0A85 ) && ( c <= 0x0A8B ) ) 
            {
                continue;
            }

            if ( ( c >= 0x0A8F ) && ( c <= 0x0A91 ) ) 
            {
                continue;
            }

            if ( ( c >= 0x0A93 ) && ( c <= 0x0AA8 ) ) 
            {
                continue;
            }

            if ( ( c >= 0x0AAA ) && ( c <= 0x0AB0 ) ) 
            {
                continue;
            }

            if ( ( c >= 0x0AB2 ) && ( c <= 0x0AB3 ) ) 
            {
                continue;
            }

            if ( ( c >= 0x0AB5 ) && ( c <= 0x0AB9 ) ) 
            {
                continue;
            }

            if ( ( c >= 0x0ABD ) && ( c <= 0x0AC0 ) ) 
            {
                continue;
            }

            if ( ( c >= 0x0ACB ) && ( c <= 0x0ACC ) ) 
            {
                continue;
            }

            if ( ( c >= 0x0AE6 ) && ( c <= 0x0AEF ) ) 
            {
                continue;
            }

            if ( ( c >= 0x0B02 ) && ( c <= 0x0B03 ) ) 
            {
                continue;
            }

            if ( ( c >= 0x0B05 ) && ( c <= 0x0B0C ) ) 
            {
                continue;
            }

            if ( ( c >= 0x0B0F ) && ( c <= 0x0B10 ) ) 
            {
                continue;
            }

            if ( ( c >= 0x0B13 ) && ( c <= 0x0B28 ) ) 
            {
                continue;
            }

            if ( ( c >= 0x0B2A ) && ( c <= 0x0B30 ) ) 
            {
                continue;
            }

            if ( ( c >= 0x0B32 ) && ( c <= 0x0B33 ) ) 
            {
                continue;
            }

            if ( ( c >= 0x0B36 ) && ( c <= 0x0B39 ) ) 
            {
                continue;
            }

            if ( ( c >= 0x0B3D ) && ( c <= 0x0B3E ) ) 
            {
                continue;
            }

            if ( ( c >= 0x0B47 ) && ( c <= 0x0B48 ) ) 
            {
                continue;
            }

            if ( ( c >= 0x0B4B ) && ( c <= 0x0B4C ) ) 
            {
                continue;
            }

            if ( ( c >= 0x0B5C ) && ( c <= 0x0B5D ) ) 
            {
                continue;
            }

            if ( ( c >= 0x0B5F ) && ( c <= 0x0B61 ) ) 
            {
                continue;
            }

            if ( ( c >= 0x0B66 ) && ( c <= 0x0B70 ) ) 
            {
                continue;
            }

            if ( ( c >= 0x0B85 ) && ( c <= 0x0B8A ) ) 
            {
                continue;
            }

            if ( ( c >= 0x0B8E ) && ( c <= 0x0B90 ) ) 
            {
                continue;
            }

            if ( ( c >= 0x0B92 ) && ( c <= 0x0B95 ) ) 
            {
                continue;
            }

            if ( ( c >= 0x0B99 ) && ( c <= 0x0B9A ) ) 
            {
                continue;
            }

            if ( ( c >= 0x0B9E ) && ( c <= 0x0B9F ) ) 
            {
                continue;
            }

            if ( ( c >= 0x0BA3 ) && ( c <= 0x0BA4 ) ) 
            {
                continue;
            }

            if ( ( c >= 0x0BA8 ) && ( c <= 0x0BAA ) ) 
            {
                continue;
            }

            if ( ( c >= 0x0BAE ) && ( c <= 0x0BB5 ) ) 
            {
                continue;
            }

            if ( ( c >= 0x0BB7 ) && ( c <= 0x0BB9 ) ) 
            {
                continue;
            }

            if ( ( c >= 0x0BBE ) && ( c <= 0x0BBF ) ) 
            {
                continue;
            }

            if ( ( c >= 0x0BC1 ) && ( c <= 0x0BC2 ) ) 
            {
                continue;
            }

            if ( ( c >= 0x0BC6 ) && ( c <= 0x0BC8 ) ) 
            {
                continue;
            }

            if ( ( c >= 0x0BCA ) && ( c <= 0x0BCC ) ) 
            {
                continue;
            }

            if ( ( c >= 0x0BE7 ) && ( c <= 0x0BF2 ) ) 
            {
                continue;
            }

            if ( ( c >= 0x0C01 ) && ( c <= 0x0C03 ) ) 
            {
                continue;
            }

            if ( ( c >= 0x0C05 ) && ( c <= 0x0C0C ) ) 
            {
                continue;
            }

            if ( ( c >= 0x0C0E ) && ( c <= 0x0C10 ) ) 
            {
                continue;
            }

            if ( ( c >= 0x0C12 ) && ( c <= 0x0C28 ) ) 
            {
                continue;
            }

            if ( ( c >= 0x0C2A ) && ( c <= 0x0C33 ) ) 
            {
                continue;
            }

            if ( ( c >= 0x0C35 ) && ( c <= 0x0C39 ) ) 
            {
                continue;
            }

            if ( ( c >= 0x0C41 ) && ( c <= 0x0C44 ) ) 
            {
                continue;
            }

            if ( ( c >= 0x0C60 ) && ( c <= 0x0C61 ) ) 
            {
                continue;
            }

            if ( ( c >= 0x0C66 ) && ( c <= 0x0C6F ) ) 
            {
                continue;
            }

            if ( ( c >= 0x0C82 ) && ( c <= 0x0C83 ) ) 
            {
                continue;
            }

            if ( ( c >= 0x0C85 ) && ( c <= 0x0C8C ) ) 
            {
                continue;
            }

            if ( ( c >= 0x0C8E ) && ( c <= 0x0C90 ) ) 
            {
                continue;
            }

            if ( ( c >= 0x0C92 ) && ( c <= 0x0CA8 ) ) 
            {
                continue;
            }

            if ( ( c >= 0x0CAA ) && ( c <= 0x0CB3 ) ) 
            {
                continue;
            }

            if ( ( c >= 0x0CB5 ) && ( c <= 0x0CB9 ) ) 
            {
                continue;
            }

            if ( ( c >= 0x0CC0 ) && ( c <= 0x0CC4 ) ) 
            {
                continue;
            }

            if ( ( c >= 0x0CC7 ) && ( c <= 0x0CC8 ) ) 
            {
                continue;
            }

            if ( ( c >= 0x0CCA ) && ( c <= 0x0CCB ) ) 
            {
                continue;
            }

            if ( ( c >= 0x0CD5 ) && ( c <= 0x0CD6 ) ) 
            {
                continue;
            }

            if ( ( c >= 0x0CE0 ) && ( c <= 0x0CE1 ) ) 
            {
                continue;
            }

            if ( ( c >= 0x0CE6 ) && ( c <= 0x0CEF ) ) 
            {
                continue;
            }

            if ( ( c >= 0x0D02 ) && ( c <= 0x0D03 ) ) 
            {
                continue;
            }

            if ( ( c >= 0x0D05 ) && ( c <= 0x0D0C ) ) 
            {
                continue;
            }

            if ( ( c >= 0x0D0E ) && ( c <= 0x0D10 ) ) 
            {
                continue;
            }

            if ( ( c >= 0x0D12 ) && ( c <= 0x0D28 ) ) 
            {
                continue;
            }

            if ( ( c >= 0x0D2A ) && ( c <= 0x0D39 ) ) 
            {
                continue;
            }

            if ( ( c >= 0x0D3E ) && ( c <= 0x0D40 ) ) 
            {
                continue;
            }

            if ( ( c >= 0x0D46 ) && ( c <= 0x0D48 ) ) 
            {
                continue;
            }

            if ( ( c >= 0x0D4A ) && ( c <= 0x0D4C ) ) 
            {
                continue;
            }

            if ( ( c >= 0x0D60 ) && ( c <= 0x0D61 ) ) 
            {
                continue;
            }

            if ( ( c >= 0x0D66 ) && ( c <= 0x0D6F ) ) 
            {
                continue;
            }

            if ( ( c >= 0x0D82 ) && ( c <= 0x0D83 ) ) 
            {
                continue;
            }

            if ( ( c >= 0x0D85 ) && ( c <= 0x0D96 ) ) 
            {
                continue;
            }

            if ( ( c >= 0x0D9A ) && ( c <= 0x0DB1 ) ) 
            {
                continue;
            }

            if ( ( c >= 0x0DB3 ) && ( c <= 0x0DBB ) ) 
            {
                continue;
            }

            if ( ( c >= 0x0DC0 ) && ( c <= 0x0DC6 ) ) 
            {
                continue;
            }

            if ( ( c >= 0x0DCF ) && ( c <= 0x0DD1 ) ) 
            {
                continue;
            }

            if ( ( c >= 0x0DD8 ) && ( c <= 0x0DDF ) ) 
            {
                continue;
            }

            if ( ( c >= 0x0DF2 ) && ( c <= 0x0DF4 ) ) 
            {
                continue;
            }

            if ( ( c >= 0x0E01 ) && ( c <= 0x0E30 ) ) 
            {
                continue;
            }

            if ( ( c >= 0x0E32 ) && ( c <= 0x0E33 ) ) 
            {
                continue;
            }

            if ( ( c >= 0x0E40 ) && ( c <= 0x0E46 ) ) 
            {
                continue;
            }

            if ( ( c >= 0x0E4F ) && ( c <= 0x0E5B ) ) 
            {
                continue;
            }

            if ( ( c >= 0x0E81 ) && ( c <= 0x0E82 ) ) 
            {
                continue;
            }

            if ( ( c >= 0x0E87 ) && ( c <= 0x0E88 ) ) 
            {
                continue;
            }

            if ( ( c >= 0x0E94 ) && ( c <= 0x0E97 ) ) 
            {
                continue;
            }

            if ( ( c >= 0x0E99 ) && ( c <= 0x0E9F ) ) 
            {
                continue;
            }

            if ( ( c >= 0x0EA1 ) && ( c <= 0x0EA3 ) ) 
            {
                continue;
            }

            if ( ( c >= 0x0EAA ) && ( c <= 0x0EAB ) ) 
            {
                continue;
            }

            if ( ( c >= 0x0EAD ) && ( c <= 0x0EB0 ) ) 
            {
                continue;
            }

            if ( ( c >= 0x0EB2 ) && ( c <= 0x0EB3 ) ) 
            {
                continue;
            }

            if ( ( c >= 0x0EC0 ) && ( c <= 0x0EC4 ) ) 
            {
                continue;
            }

            if ( ( c >= 0x0ED0 ) && ( c <= 0x0ED9 ) ) 
            {
                continue;
            }

            if ( ( c >= 0x0EDC ) && ( c <= 0x0EDD ) ) 
            {
                continue;
            }

            if ( ( c >= 0x0F00 ) && ( c <= 0x0F17 ) ) 
            {
                continue;
            }

            if ( ( c >= 0x0F1A ) && ( c <= 0x0F34 ) ) 
            {
                continue;
            }

            if ( ( c >= 0x0F3E ) && ( c <= 0x0F47 ) ) 
            {
                continue;
            }

            if ( ( c >= 0x0F49 ) && ( c <= 0x0F6A ) ) 
            {
                continue;
            }

            if ( ( c >= 0x0F88 ) && ( c <= 0x0F8B ) ) 
            {
                continue;
            }

            if ( ( c >= 0x0FBE ) && ( c <= 0x0FC5 ) ) 
            {
                continue;
            }

            if ( ( c >= 0x0FC7 ) && ( c <= 0x0FCC ) ) 
            {
                continue;
            }

            if ( ( c >= 0x1000 ) && ( c <= 0x1021 ) ) 
            {
                continue;
            }

            if ( ( c >= 0x1023 ) && ( c <= 0x1027 ) ) 
            {
                continue;
            }

            if ( ( c >= 0x1029 ) && ( c <= 0x102A ) ) 
            {
                continue;
            }

            if ( ( c >= 0x1040 ) && ( c <= 0x1057 ) ) 
            {
                continue;
            }

            if ( ( c >= 0x10A0 ) && ( c <= 0x10C5 ) ) 
            {
                continue;
            }

            if ( ( c >= 0x10D0 ) && ( c <= 0x10F8 ) ) 
            {
                continue;
            }

            if ( ( c >= 0x1100 ) && ( c <= 0x1159 ) ) 
            {
                continue;
            }

            if ( ( c >= 0x115F ) && ( c <= 0x11A2 ) ) 
            {
                continue;
            }

            if ( ( c >= 0x11A8 ) && ( c <= 0x11F9 ) ) 
            {
                continue;
            }

            if ( ( c >= 0x1200 ) && ( c <= 0x1206 ) ) 
            {
                continue;
            }

            if ( ( c >= 0x1208 ) && ( c <= 0x1246 ) ) 
            {
                continue;
            }

            if ( ( c >= 0x124A ) && ( c <= 0x124D ) ) 
            {
                continue;
            }

            if ( ( c >= 0x1250 ) && ( c <= 0x1256 ) ) 
            {
                continue;
            }

            if ( ( c >= 0x125A ) && ( c <= 0x125D ) ) 
            {
                continue;
            }

            if ( ( c >= 0x1260 ) && ( c <= 0x1286 ) ) 
            {
                continue;
            }

            if ( ( c >= 0x128A ) && ( c <= 0x128D ) ) 
            {
                continue;
            }

            if ( ( c >= 0x1290 ) && ( c <= 0x12AE ) ) 
            {
                continue;
            }

            if ( ( c >= 0x12B2 ) && ( c <= 0x12B5 ) ) 
            {
                continue;
            }

            if ( ( c >= 0x12B8 ) && ( c <= 0x12BE ) ) 
            {
                continue;
            }

            if ( ( c >= 0x12C2 ) && ( c <= 0x12C5 ) ) 
            {
                continue;
            }

            if ( ( c >= 0x12C8 ) && ( c <= 0x12CE ) ) 
            {
                continue;
            }

            if ( ( c >= 0x12D0 ) && ( c <= 0x12D6 ) ) 
            {
                continue;
            }

            if ( ( c >= 0x12D8 ) && ( c <= 0x12EE ) ) 
            {
                continue;
            }

            if ( ( c >= 0x12F0 ) && ( c <= 0x130E ) ) 
            {
                continue;
            }

            if ( ( c >= 0x1312 ) && ( c <= 0x1315 ) ) 
            {
                continue;
            }

            if ( ( c >= 0x1318 ) && ( c <= 0x131E ) ) 
            {
                continue;
            }

            if ( ( c >= 0x1320 ) && ( c <= 0x1346 ) ) 
            {
                continue;
            }

            if ( ( c >= 0x1348 ) && ( c <= 0x135A ) ) 
            {
                continue;
            }

            if ( ( c >= 0x1361 ) && ( c <= 0x137C ) ) 
            {
                continue;
            }

            if ( ( c >= 0x13A0 ) && ( c <= 0x13F4 ) ) 
            {
                continue;
            }

            if ( ( c >= 0x1401 ) && ( c <= 0x1676 ) ) 
            {
                continue;
            }

            if ( ( c >= 0x1681 ) && ( c <= 0x169A ) ) 
            {
                continue;
            }

            if ( ( c >= 0x16A0 ) && ( c <= 0x16F0 ) ) 
            {
                continue;
            }

            if ( ( c >= 0x1700 ) && ( c <= 0x170C ) ) 
            {
                continue;
            }

            if ( ( c >= 0x170E ) && ( c <= 0x1711 ) ) 
            {
                continue;
            }

            if ( ( c >= 0x1720 ) && ( c <= 0x1731 ) ) 
            {
                continue;
            }

            if ( ( c >= 0x1735 ) && ( c <= 0x1736 ) ) 
            {
                continue;
            }

            if ( ( c >= 0x1740 ) && ( c <= 0x1751 ) ) 
            {
                continue;
            }

            if ( ( c >= 0x1760 ) && ( c <= 0x176C ) ) 
            {
                continue;
            }

            if ( ( c >= 0x176E ) && ( c <= 0x1770 ) ) 
            {
                continue;
            }

            if ( ( c >= 0x1780 ) && ( c <= 0x17B6 ) ) 
            {
                continue;
            }

            if ( ( c >= 0x17BE ) && ( c <= 0x17C5 ) ) 
            {
                continue;
            }

            if ( ( c >= 0x17C7 ) && ( c <= 0x17C8 ) ) 
            {
                continue;
            }

            if ( ( c >= 0x17D4 ) && ( c <= 0x17DA ) ) 
            {
                continue;
            }

            if ( ( c >= 0x17E0 ) && ( c <= 0x17E9 ) ) 
            {
                continue;
            }

            if ( ( c >= 0x1810 ) && ( c <= 0x1819 ) ) 
            {
                continue;
            }

            if ( ( c >= 0x1820 ) && ( c <= 0x1877 ) ) 
            {
                continue;
            }

            if ( ( c >= 0x1880 ) && ( c <= 0x18A8 ) ) 
            {
                continue;
            }

            if ( ( c >= 0x1E00 ) && ( c <= 0x1E9B ) ) 
            {
                continue;
            }

            if ( ( c >= 0x1EA0 ) && ( c <= 0x1EF9 ) ) 
            {
                continue;
            }

            if ( ( c >= 0x1F00 ) && ( c <= 0x1F15 ) ) 
            {
                continue;
            }

            if ( ( c >= 0x1F18 ) && ( c <= 0x1F1D ) ) 
            {
                continue;
            }

            if ( ( c >= 0x1F20 ) && ( c <= 0x1F45 ) ) 
            {
                continue;
            }

            if ( ( c >= 0x1F48 ) && ( c <= 0x1F4D ) ) 
            {
                continue;
            }

            if ( ( c >= 0x1F50 ) && ( c <= 0x1F57 ) ) 
            {
                continue;
            }

            if ( ( c >= 0x1F5F ) && ( c <= 0x1F7D ) ) 
            {
                continue;
            }

            if ( ( c >= 0x1F80 ) && ( c <= 0x1FB4 ) ) 
            {
                continue;
            }

            if ( ( c >= 0x1FB6 ) && ( c <= 0x1FBC ) ) 
            {
                continue;
            }

            if ( ( c >= 0x1FC2 ) && ( c <= 0x1FC4 ) ) 
            {
                continue;
            }

            if ( ( c >= 0x1FC6 ) && ( c <= 0x1FCC ) ) 
            {
                continue;
            }

            if ( ( c >= 0x1FD0 ) && ( c <= 0x1FD3 ) ) 
            {
                continue;
            }

            if ( ( c >= 0x1FD6 ) && ( c <= 0x1FDB ) ) 
            {
                continue;
            }

            if ( ( c >= 0x1FE0 ) && ( c <= 0x1FEC ) ) 
            {
                continue;
            }

            if ( ( c >= 0x1FF2 ) && ( c <= 0x1FF4 ) ) 
            {
                continue;
            }

            if ( ( c >= 0x1FF6 ) && ( c <= 0x1FFC ) ) 
            {
                continue;
            }

            if ( ( c >= 0x210A ) && ( c <= 0x2113 ) ) 
            {
                continue;
            }

            if ( ( c >= 0x2119 ) && ( c <= 0x211D ) ) 
            {
                continue;
            }

            if ( ( c >= 0x212A ) && ( c <= 0x212D ) ) 
            {
                continue;
            }

            if ( ( c >= 0x212F ) && ( c <= 0x2131 ) ) 
            {
                continue;
            }

            if ( ( c >= 0x2133 ) && ( c <= 0x2139 ) ) 
            {
                continue;
            }

            if ( ( c >= 0x213D ) && ( c <= 0x213F ) ) 
            {
                continue;
            }

            if ( ( c >= 0x2145 ) && ( c <= 0x2149 ) ) 
            {
                continue;
            }

            if ( ( c >= 0x2160 ) && ( c <= 0x2183 ) ) 
            {
                continue;
            }

            if ( ( c >= 0x2336 ) && ( c <= 0x237A ) ) 
            {
                continue;
            }

            if ( ( c >= 0x249C ) && ( c <= 0x24E9 ) ) 
            {
                continue;
            }

            if ( ( c >= 0x3005 ) && ( c <= 0x3007 ) ) 
            {
                continue;
            }

            if ( ( c >= 0x3021 ) && ( c <= 0x3029 ) ) 
            {
                continue;
            }

            if ( ( c >= 0x3031 ) && ( c <= 0x3035 ) ) 
            {
                continue;
            }

            if ( ( c >= 0x3038 ) && ( c <= 0x303C ) ) 
            {
                continue;
            }

            if ( ( c >= 0x3041 ) && ( c <= 0x3096 ) ) 
            {
                continue;
            }

            if ( ( c >= 0x309D ) && ( c <= 0x309F ) ) 
            {
                continue;
            }

            if ( ( c >= 0x30A1 ) && ( c <= 0x30FA ) ) 
            {
                continue;
            }

            if ( ( c >= 0x30FC ) && ( c <= 0x30FF ) ) 
            {
                continue;
            }

            if ( ( c >= 0x3105 ) && ( c <= 0x312C ) ) 
            {
                continue;
            }

            if ( ( c >= 0x3131 ) && ( c <= 0x318E ) ) 
            {
                continue;
            }

            if ( ( c >= 0x3190 ) && ( c <= 0x31B7 ) ) 
            {
                continue;
            }

            if ( ( c >= 0x31F0 ) && ( c <= 0x321C ) ) 
            {
                continue;
            }

            if ( ( c >= 0x3220 ) && ( c <= 0x3243 ) ) 
            {
                continue;
            }

            if ( ( c >= 0x3260 ) && ( c <= 0x327B ) ) 
            {
                continue;
            }

            if ( ( c >= 0x327F ) && ( c <= 0x32B0 ) ) 
            {
                continue;
            }

            if ( ( c >= 0x32C0 ) && ( c <= 0x32CB ) ) 
            {
                continue;
            }

            if ( ( c >= 0x32D0 ) && ( c <= 0x32FE ) ) 
            {
                continue;
            }

            if ( ( c >= 0x3300 ) && ( c <= 0x3376 ) ) 
            {
                continue;
            }

            if ( ( c >= 0x337B ) && ( c <= 0x33DD ) ) 
            {
                continue;
            }

            if ( ( c >= 0x33E0 ) && ( c <= 0x33FE ) ) 
            {
                continue;
            }

            if ( ( c >= 0x3400 ) && ( c <= 0x4DB5 ) ) 
            {
                continue;
            }

            if ( ( c >= 0x4E00 ) && ( c <= 0x9FA5 ) ) 
            {
                continue;
            }

            if ( ( c >= 0xA000 ) && ( c <= 0xA48C ) ) 
            {
                continue;
            }

            if ( ( c >= 0xAC00 ) && ( c <= 0xD7A3 ) ) 
            {
                continue;
            }

            if ( ( c >= 0xD800 ) && ( c <= 0xFA2D ) ) 
            {
                continue;
            }

            if ( ( c >= 0xFA30 ) && ( c <= 0xFA6A ) ) 
            {
                continue;
            }

            if ( ( c >= 0xFB00 ) && ( c <= 0xFB06 ) ) 
            {
                continue;
            }

            if ( ( c >= 0xFB13 ) && ( c <= 0xFB17 ) ) 
            {
                continue;
            }

            if ( ( c >= 0xFF21 ) && ( c <= 0xFF3A ) ) 
            {
                continue;
            }

            if ( ( c >= 0xFF41 ) && ( c <= 0xFF5A ) ) 
            {
                continue;
            }

            if ( ( c >= 0xFF66 ) && ( c <= 0xFFBE ) ) 
            {
                continue;
            }

            if ( ( c >= 0xFFC2 ) && ( c <= 0xFFC7 ) ) 
            {
                continue;
            }

            if ( ( c >= 0xFFCA ) && ( c <= 0xFFCF ) ) 
            {
                continue;
            }

            if ( ( c >= 0xFFD2 ) && ( c <= 0xFFD7 ) ) 
            {
                continue;
            }

            if ( ( c >= 0xFFDA ) && ( c <= 0xFFDC ) ) 
            {
                continue;
            }

            // Now, fo every other chars, add them to the buffer.
            sb.append( c );
        }
        
        return sb;
    }
    
    /**
     * 
     * Remove all insignifiant chars in a Telephone Number :
     * Hyphen and spaces. 
     * 
     * For instance, the following telephone number :
     * "+ (33) 1-123--456  789"
     * will be trasnformed to :
     * "+(33)1123456789"
     *
     * @param str The telephone number
     * @return The modified telephone number
     */
    public static String insignifiantCharTelephoneNumber( String str )
    {
        return insignifiantCharTelephoneNumber( str.toCharArray() ).toString();
    }
    
    /**
     * 
     * Remove all insignifiant chars in a Telephone Number :
     * Hyphen and spaces. 
     * 
     * For instance, the following telephone number :
     * "+ (33) 1-123--456  789"
     * will be trasnformed to :
     * "+(33)1123456789"
     *
     * @param array The telephone number char array
     * @return The modified telephone number StringBuilder
     */
    public static StringBuilder insignifiantCharTelephoneNumber( char[] array )
    {
        if ( array == null )
        {
            return null;
        }

        StringBuilder sb = new StringBuilder( array.length );
        boolean isSpaceOrHyphen = false;
        char soh = '\0';
        
        for ( char c:array )
        {
            switch ( c )
            {
                case 0x0020 : // SPACE
                case 0x002D : // HYPHEN-MINUS
                case 0x058A : // ARMENIAN HYPHEN
                case 0x2010 : // HYPHEN
                case 0x2011 : // NON-BREAKING HYPHEN
                case 0x2212 : // MINUS SIGN
                case 0xFE63 : // SMALL HYPHEN-MINUS
                case 0xFF0D : // FULLWIDTH HYPHEN-MINUS
                    soh = c;
                    break;
                    
                default :
                    if ( isSpaceOrHyphen && isCombiningMark( c ) )
                    {
                        sb.append( soh );
                        isSpaceOrHyphen = false;
                    }
                
                    sb.append( c );
            }
        }
        
        return sb;
    }

    /**
     * 
     * Remove all insignifiant spaces in a numeric string. For
     * instance, the following numeric string :
     * "  123  456  789  "
     * will be transformed to :
     * "123456789"
     *
     * @param str The numeric string
     * @return The modified numeric String
     */
    public static String insignifiantCharNumericString( String str )
    {
        return ( str == null ? null : insignifiantCharNumericString( str.toCharArray() ).toString() );
    }
    
    /**
     * 
     * Remove all insignifiant spaces in a numeric string. For
     * instance, the following numeric string :
     * "  123  456  789  "
     * will be transformed to :
     * "123456789"
     *
     * @param array The numeric char array
     * @return The modified numeric StringBuilder
     */
    public static StringBuilder insignifiantCharNumericString( char[] array )
    {
        if ( array == null )
        {
            return null;
        }

        StringBuilder sb = new StringBuilder( array.length );
        boolean isSpace = false;
        
        for ( char c:array )
        {
            if ( c != 0x20 )
            {
                if ( isSpace && isCombiningMark( c ) )
                {
                    sb.append( ' ' );
                    isSpace = false;
                }
                    
                sb.append( c );
            }
            else
            {
                isSpace = true;
            }
        }
        
        return sb;
    }

    /**
     * 
     * The 6 possible states for the insignifiant state machine
     */
    private enum State 
    {
        START,
        START_SPACE,
        INNER_START_SPACE,
        CHAR,
        COMBINING,
        INNER_SPACE
    }

    /**
     * 
     * Remove all insignifiant spaces in a string.
     * 
     * This method use a finite state machine to parse
     * the text.
     * 
     * @param str The string
     * @return The modified String
     */
    public static String insignifiantSpacesString( String str ) throws InvalidCharacterException
    {
        if ( str == null )
        {
            return "  ";
        }
        else
        {
            return insignifiantSpacesString( str.toCharArray() ).toString();
        }
    }
    
    /**
     * 
     * Remove all insignifiant spaces in a string.
     * 
     * This method use a finite state machine to parse
     * the text.
     * 
     * @param array The char array  representing the string
     * @return The modified StringBuilder
     */
    public static StringBuilder insignifiantSpacesString( char[] array ) throws InvalidCharacterException
    {
        if ( ( array == null ) || ( array.length == 0 ) )
        {
            // Special case : an empty strings is replaced by 2 spaces
            return new StringBuilder( "  " );
        }
        
        StringBuilder sb = new StringBuilder( array.length );
        
        // Initialise the starting state
        State state = State.START;
        
        for ( char c:array )
        {
            switch ( state )
            {
                case START :
                    if ( c == ' ' )
                    {
                        state = State.START_SPACE;
                    }
                    else if ( isCombiningMark( c ) )
                    {
                        // The first char can't be a combining char
                        throw new InvalidCharacterException( c );
                    }
                    else
                    {
                        sb.append( ' ' );
                        sb.append( c );
                        state = State.CHAR;
                    }

                    break;
                    
                case START_SPACE :
                    if ( isCombiningMark( c ) )
                    {
                        state = State.COMBINING;
                        sb.append( ' ' );
                        sb.append( ' ' );
                        sb.append( c );
                    }
                    else if ( c != ' ' )
                    {
                        state = State.CHAR;
                        sb.append( ' ' );
                        sb.append( c );
                    }

                    break;
                     
                case CHAR :
                    if ( c == ' ' )
                    {
                        state = State.INNER_START_SPACE;
                    }
                    else if ( isCombiningMark( c ) )
                    {
                        state = State.COMBINING;
                        sb.append( c );
                    }
                    else
                    {
                        sb.append( c );
                    }
                    
                    break;
                    
                case COMBINING :
                    if ( c == ' ' )
                    {
                        state = State.INNER_START_SPACE;
                    }
                    else if ( !isCombiningMark( c ) )
                    {
                        state = State.CHAR;
                        sb.append( c );
                    }
                    else
                    {
                        sb.append( c );
                    }
                    
                    break;
                    
                case INNER_START_SPACE :
                    if ( isCombiningMark( c ) )
                    {
                        state = State.COMBINING;
                        sb.append( ' ' );
                        sb.append( c );
                    }
                    else if ( c == ' ' )
                    {
                        state = State.INNER_SPACE;
                    }
                    else
                    {
                        state = State.CHAR;
                        sb.append( ' ' );
                        sb.append( c );
                    }
                    
                    break;

                case INNER_SPACE :
                    if ( isCombiningMark( c ) )
                    {
                        state = State.COMBINING;
                        sb.append( ' ' );
                        sb.append( ' ' );
                        sb.append( c );
                    }
                    else if ( c != ' ' )
                    {
                        state = State.CHAR;
                        sb.append( ' ' );
                        sb.append( c );
                    }
                    
                    break;
            }
        }
        
        // Last, add final space if needed
        sb.append( ' ' );
        
        if ( state == State.START_SPACE )
        {
            sb.append( ' ' );
        }
        
        return sb;
    }
}
