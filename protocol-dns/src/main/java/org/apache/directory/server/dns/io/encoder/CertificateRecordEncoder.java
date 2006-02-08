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

package org.apache.directory.server.dns.io.encoder;

/**
 * 2. The CERT Resource Record
 * 
 *    The CERT resource record (RR) has the structure given below.  Its RR
 *    type code is 37.
 * 
 *                          1 1 1 1 1 1 1 1 1 1 2 2 2 2 2 2 2 2 2 2 3 3
 *      0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
 *     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *     |             type              |             key tag           |
 *     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *     |   algorithm   |                                               /
 *     +---------------+            certificate or CRL                 /
 *     /                                                               /
 *     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-|
 * 
 *    The type field is the certificate type as define in section 2.1
 *    below.
 * 
 *    The algorithm field has the same meaning as the algorithm field in
 *    KEY and SIG RRs [RFC 2535] except that a zero algorithm field
 *    indicates the algorithm is unknown to a secure DNS, which may simply
 *    be the result of the algorithm not having been standardized for
 *    secure DNS.
 * 
 *    The key tag field is the 16 bit value computed for the key embedded
 *    in the certificate as specified in the DNSSEC Standard [RFC 2535].
 *    This field is used as an efficiency measure to pick which CERT RRs
 *    may be applicable to a particular key.  The key tag can be calculated
 *    for the key in question and then only CERT RRs with the same key tag
 *    need be examined. However, the key must always be transformed to the
 *    format it would have as the public key portion of a KEY RR before the
 *    key tag is computed.  This is only possible if the key is applicable
 *    to an algorithm (and limits such as key size limits) defined for DNS
 *    security.  If it is not, the algorithm field MUST BE zero and the tag
 *    field is meaningless and SHOULD BE zero.
 */
public class CertificateRecordEncoder
{
}
