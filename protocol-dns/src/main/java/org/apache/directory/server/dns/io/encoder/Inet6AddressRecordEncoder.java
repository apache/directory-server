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
 * 2. New resource record definition and domain
 * 
 *    A record type is defined to store a host's IPv6 address.  A host that
 *    has more than one IPv6 address must have more than one such record.
 * 
 * 2.1 AAAA record type
 * 
 *    The AAAA resource record type is a record specific to the Internet
 *    class that stores a single IPv6 address.
 * 
 *    The IANA assigned value of the type is 28 (decimal).
 * 
 * 2.2 AAAA data format
 * 
 *    A 128 bit IPv6 address is encoded in the data portion of an AAAA
 *    resource record in network byte order (high-order byte first).
 * 
 * 2.3 AAAA query
 * 
 *    An AAAA query for a specified domain name in the Internet class
 *    returns all associated AAAA resource records in the answer section of
 *    a response.
 * 
 *    A type AAAA query does not trigger additional section processing.
 * 
 * 2.4 Textual format of AAAA records
 * 
 *    The textual representation of the data portion of the AAAA resource
 *    record used in a master database file is the textual representation
 *    of an IPv6 address as defined in [3].
 * 
 * 2.5 IP6.ARPA Domain
 * 
 *    A special domain is defined to look up a record given an IPv6
 *    address.  The intent of this domain is to provide a way of mapping an
 *    IPv6 address to a host name, although it may be used for other
 *    purposes as well.  The domain is rooted at IP6.ARPA.
 * 
 *    An IPv6 address is represented as a name in the IP6.ARPA domain by a
 *    sequence of nibbles separated by dots with the suffix ".IP6.ARPA".
 *    The sequence of nibbles is encoded in reverse order, i.e., the
 *    low-order nibble is encoded first, followed by the next low-order
 *    nibble and so on.  Each nibble is represented by a hexadecimal digit.
 *    For example, the reverse lookup domain name corresponding to the
 *    address
 * 
 *        4321:0:1:2:3:4:567:89ab
 * 
 *    would be
 * 
 *    b.a.9.8.7.6.5.0.4.0.0.0.3.0.0.0.2.0.0.0.1.0.0.0.0.0.0.0.1.2.3.4.IP6.
 *                                                                   ARPA.
 */
public class Inet6AddressRecordEncoder
{
}
