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

package org.apache.dns.io.encoder;

/**
 * 3.1.1.  Format
 * 
 *    The RDATA portion of the A6 record contains two or three fields.
 * 
 *            +-----------+------------------+-------------------+
 *            |Prefix len.|  Address suffix  |    Prefix name    |
 *            | (1 octet) |  (0..16 octets)  |  (0..255 octets)  |
 *            +-----------+------------------+-------------------+
 * 
 *    o  A prefix length, encoded as an eight-bit unsigned integer with
 *       value between 0 and 128 inclusive.
 * 
 *    o  An IPv6 address suffix, encoded in network order (high-order octet
 *       first).  There MUST be exactly enough octets in this field to
 *       contain a number of bits equal to 128 minus prefix length, with 0
 *       to 7 leading pad bits to make this field an integral number of
 *       octets.  Pad bits, if present, MUST be set to zero when loading a
 *       zone file and ignored (other than for SIG [DNSSEC] verification)
 *       on reception.
 * 
 *    o  The name of the prefix, encoded as a domain name.  By the rules of
 *       [DNSIS], this name MUST NOT be compressed.
 * 
 *    The domain name component SHALL NOT be present if the prefix length
 *    is zero.  The address suffix component SHALL NOT be present if the
 *    prefix length is 128.
 * 
 *    It is SUGGESTED that an A6 record intended for use as a prefix for
 *    other A6 records have all the insignificant trailing bits in its
 *    address suffix field set to zero.
 */
public class A6RecordEncoder
{
}
