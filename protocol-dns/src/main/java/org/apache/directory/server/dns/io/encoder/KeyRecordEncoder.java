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
 * 3. The KEY Resource Record
 * 
 *    The KEY resource record (RR) is used to store a public key that is
 *    associated with a Domain Name System (DNS) name.  This can be the
 *    public key of a zone, a user, or a host or other end entity. Security
 *    aware DNS implementations MUST be designed to handle at least two
 *    simultaneously valid keys of the same type associated with the same
 *    name.
 * 
 *    The type number for the KEY RR is 25.
 * 
 *    A KEY RR is, like any other RR, authenticated by a SIG RR.  KEY RRs
 *    must be signed by a zone level key.
 * 
 * 3.1 KEY RDATA format
 * 
 *    The RDATA for a KEY RR consists of flags, a protocol octet, the
 *    algorithm number octet, and the public key itself.  The format is as
 *    follows:
 * 
 *                         1 1 1 1 1 1 1 1 1 1 2 2 2 2 2 2 2 2 2 2 3 3
 *     0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
 *    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *    |             flags             |    protocol   |   algorithm   |
 *    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *    |                                                               /
 *    /                          public key                           /
 *    /                                                               /
 *    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-|
 * 
 *    The KEY RR is not intended for storage of certificates and a separate
 *    certificate RR has been developed for that purpose, defined in [RFC
 *    2538].
 */
public class KeyRecordEncoder
{
}
