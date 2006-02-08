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
 * Stores the different states of a PDU parsing.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class TLVStateEnum
{
    //~ Static fields/initializers -----------------------------------------------------------------

    /** Start means that the deconding hasn't read the first byte */
    public static final int TAG_STATE_START = 0x00;

    /** Pending means that the Type Tag is contained in more that one byte */
    public static final int TAG_STATE_PENDING = 0x01;

    /** End means that the Type is totally read */
    public static final int TAG_STATE_END = 0x02;

    /**
     * Overflow could have two meaning : either there are more than 5 bytes to
     * encode the value (5 bytes = 5bits + 4*7 bits = 33 bits) or the value that
     * is represented by those bytes is over MAX_INTEGER
     */
    public static final int TAG_STATE_OVERFLOW = 0x04;

    /** Start means that the decoding hasn't read the first byte */
    public static final int LENGTH_STATE_START = 0x08;

    /** Pending means that the Type length is contained in more that one byte */
    public static final int LENGTH_STATE_PENDING = 0x10;

    /** End means that the Length is totally read */
    public static final int LENGTH_STATE_END    = 0x20;

    /** Start means that the decoding hasn't read the first byte */
    public static final int VALUE_STATE_START   = 0x40;

    /** Pending means that the Type Value is contained in more that one byte */
    public static final int VALUE_STATE_PENDING = 0x80;

    /** End means that the Value is totally read */
    public static final int VALUE_STATE_END = 0x100;

    /** The decoding of a TLV is done */
    public static final int TLV_STATE_DONE = 0x200;

    /** The decoding of a PDU is done */
    public static final int PDU_DECODED = 0x400;
}
