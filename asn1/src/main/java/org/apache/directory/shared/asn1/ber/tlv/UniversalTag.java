/*
 *   Copyright 2005 The Apache Software Foundation
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
package org.apache.directory.shared.asn1.ber.tlv;


/**
 * Enum for ASN.1 UNIVERSAL class tags. The tags values are constructed using
 * the SNACC representation for tags without the primitive/constructed bit. This
 * is done because several bit, octet and character string types can be encoded
 * as primitives or as constructed types to chunk the value out.
 * <p>
 * These tags can have one of the following values:
 * </p>
 * <p>
 * </p>
 * <table border="1" cellspacing="1" width="60%">
 * <tr>
 * <th>Id</th>
 * <th>Usage</th>
 * </tr>
 * <tr>
 * <td>[UNIVERSAL 0]</td>
 * <td>reserved for BER</td>
 * </tr>
 * <tr>
 * <td>[UNIVERSAL 1]</td>
 * <td>BOOLEAN</td>
 * </tr>
 * <tr>
 * <td>[UNIVERSAL 2]</td>
 * <td>INTEGER</td>
 * </tr>
 * <tr>
 * <td>[UNIVERSAL 3]</td>
 * <td>BIT STRING</td>
 * </tr>
 * <tr>
 * <td>[UNIVERSAL 4]</td>
 * <td>OCTET STRING</td>
 * </tr>
 * <tr>
 * <td>[UNIVERSAL 5]</td>
 * <td>NULL</td>
 * </tr>
 * <tr>
 * <td>[UNIVERSAL 6]</td>
 * <td>OBJECT IDENTIFIER</td>
 * </tr>
 * <tr>
 * <td>[UNIVERSAL 7]</td>
 * <td>ObjectDescriptor</td>
 * </tr>
 * <tr>
 * <td>[UNIVERSAL 8]</td>
 * <td>EXTERNAL, INSTANCE OF</td>
 * </tr>
 * <tr>
 * <td>[UNIVERSAL 9]</td>
 * <td>REAL</td>
 * </tr>
 * <tr>
 * <td>[UNIVERSAL 10]</td>
 * <td>ENUMERATED</td>
 * </tr>
 * <tr>
 * <td>[UNIVERSAL 11]</td>
 * <td>EMBEDDED PDV</td>
 * </tr>
 * <tr>
 * <td>[UNIVERSAL 12]</td>
 * <td>UTF8String</td>
 * </tr>
 * <tr>
 * <td>[UNIVERSAL 13]</td>
 * <td>RELATIVE-OID</td>
 * </tr>
 * <tr>
 * <td>[UNIVERSAL 14]</td>
 * <td>reserved for future use</td>
 * </tr>
 * <tr>
 * <td>[UNIVERSAL 15]</td>
 * <td>reserved for future use</td>
 * </tr>
 * <tr>
 * <td>[UNIVERSAL 16]</td>
 * <td>SEQUENCE, SEQUENCE OF</td>
 * </tr>
 * <tr>
 * <td>[UNIVERSAL 17]</td>
 * <td>SET, SET OF</td>
 * </tr>
 * <tr>
 * <td>[UNIVERSAL 18]</td>
 * <td>NumericString</td>
 * </tr>
 * <tr>
 * <td>[UNIVERSAL 19]</td>
 * <td>PrintableString</td>
 * </tr>
 * <tr>
 * <td>[UNIVERSAL 20]</td>
 * <td>TeletexString, T61String</td>
 * </tr>
 * <tr>
 * <td>[UNIVERSAL 21]</td>
 * <td>VideotexString</td>
 * </tr>
 * <tr>
 * <td>[UNIVERSAL 22]</td>
 * <td>IA5String</td>
 * </tr>
 * <tr>
 * <td>[UNIVERSAL 23]</td>
 * <td>UTCTime</td>
 * </tr>
 * <tr>
 * <td>[UNIVERSAL 24]</td>
 * <td>GeneralizedTime</td>
 * </tr>
 * <tr>
 * <td>[UNIVERSAL 25]</td>
 * <td>GraphicString</td>
 * </tr>
 * <tr>
 * <td>[UNIVERSAL 26]</td>
 * <td>VisibleString, ISO646String</td>
 * </tr>
 * <tr>
 * <td>[UNIVERSAL 27]</td>
 * <td>GeneralString</td>
 * </tr>
 * <tr>
 * <td>[UNIVERSAL 28]</td>
 * <td>UniversalString</td>
 * </tr>
 * <tr>
 * <td>[UNIVERSAL 29]</td>
 * <td>CHARACTER STRING</td>
 * </tr>
 * <tr>
 * <td>[UNIVERSAL 30]</td>
 * <td>BMPString</td>
 * </tr>
 * <tr>
 * <td>[UNIVERSAL 31]</td>
 * <td>reserved for future use</td>
 * </tr>
 * </table>
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class UniversalTag
{
    // ~ Static fields/initializers
    // -----------------------------------------------------------------

    /** value for the tag */
    public static final int RESERVED_0 = 0;

    /** value for the tag */
    public static final int BOOLEAN = 1;

    /** value for the tag */
    public static final int INTEGER = 2;

    /** value for the tag */
    public static final int BIT_STRING = 3;

    /** value for the tag */
    public static final int OCTET_STRING = 4;

    /** value for the tag */
    public static final int NULL = 5;

    /** value for the tag */
    public static final int OBJECT_IDENTIFIER = 6;

    /** value for the tag */
    public static final int OBJECT_DESCRIPTOR = 7;

    /** value for the tag */
    public static final int EXTERNAL_INSTANCE_OF = 8;

    /** value for the tag */
    public static final int REAL = 9;

    /** value for the tag */
    public static final int ENUMERATED = 10;

    /** value for the tag */
    public static final int EMBEDDED_PDV = 11;

    /** value for the tag */
    public static final int UTF8_STRING = 12;

    /** value for the tag */
    public static final int RELATIVE_OID = 13;

    /** value for the tag */
    public static final int RESERVED_14 = 14;

    /** value for the tag */
    public static final int RESERVED_15 = 15;

    /** value for the tag */
    public static final int SEQUENCE_SEQUENCE_OF = 16;

    /** value for the tag */
    public static final int SET_SET_OF = 17;

    /** value for the tag */
    public static final int NUMERIC_STRING = 18;

    /** value for the tag */
    public static final int PRINTABLE_STRING = 19;

    /** value for the tag */
    public static final int TELETEX_STRING = 20;

    /** value for the tag */
    public static final int VIDEOTEX_STRING = 21;

    /** value for the tag */
    public static final int IA5_STRING = 22;

    /** value for the tag */
    public static final int UTC_TIME = 23;

    /** value for the tag */
    public static final int GENERALIZED_TIME = 24;

    /** value for the tag */
    public static final int GRAPHIC_STRING = 25;

    /** value for the tag */
    public static final int VISIBLE_STRING = 26;

    /** value for the tag */
    public static final int GENERAL_STRING = 27;

    /** value for the tag */
    public static final int UNIVERSAL_STRING = 28;

    /** value for the tag */
    public static final int CHARACTER_STRING = 29;

    /** value for the tag */
    public static final int BMP_STRING = 30;

    /** value for the tag */
    public static final int RESERVED_31 = 31;

    /** String representation of the tags */
    private static final String[] UNIVERSAL_TAG_STRING =
        { "RESERVED_0", "BOOLEAN", "INTEGER", "BIT_STRING", "OCTET_STRING", "NULL", "OBJECT_IDENTIFIER",
            "OBJECT_DESCRIPTOR", "EXTERNAL_INSTANCE_OF", "REAL", "ENUMERATED", "EMBEDDED_PDV", "UTF8_STRING",
            "RELATIVE_OID", "RESERVED_14", "RESERVED_15", "SEQUENCE_SEQUENCE_OF", "SET_SET_OF", "NUMERIC_STRING",
            "PRINTABLE_STRING", "TELETEX_STRING", "VIDEOTEX_STRING", "IA5_STRING", "UTC_TIME", "GENERALIZED_TIME",
            "GRAPHIC_STRING", "VISIBLE_STRING", "GENERAL_STRING", "UNIVERSAL_STRING", "CHARACTER_STRING", "BMP_STRING",
            "RESERVED_31" };

    /** ASN.1 primitive tag values */
    public static final byte BOOLEAN_TAG = 0x01;

    public static final byte INTEGER_TAG = 0x02;

    public static final byte OCTET_STRING_TAG = 0x04;

    public static final byte ENUMERATED_TAG = 0x0A;

    public static final byte SEQUENCE_TAG = 0x30;

    public static final byte SET_TAG = 0x31;


    // ~ Methods
    // ------------------------------------------------------------------------------------

    // -----------------------------------------------------------------------
    // Members
    // -----------------------------------------------------------------------
    /**
     * Gets the ASN.1 UNIVERSAL type tag's enum using a tag value.
     * 
     * @param tag
     *            the first octet of the TLV
     * @return the valued enum for the ASN.1 UNIVERSAL type tag
     */
    public static String toString( int tag )
    {
        return UNIVERSAL_TAG_STRING[tag & 0x1F];
    }
} // end class UniversalTag
