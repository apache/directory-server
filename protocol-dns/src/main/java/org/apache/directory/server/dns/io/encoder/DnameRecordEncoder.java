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
 * 3. The DNAME Resource Record
 * 
 *    The DNAME RR has mnemonic DNAME and type code 39 (decimal).
 * 
 *    DNAME has the following format:
 * 
 *       <owner> <ttl> <class> DNAME <target>
 * 
 *    The format is not class-sensitive.  All fields are required.  The
 *    RDATA field <target> is a <domain-name> [DNSIS].
 * 
 *    The DNAME RR causes type NS additional section processing.
 * 
 *    The effect of the DNAME record is the substitution of the record's
 *    <target> for its <owner> as a suffix of a domain name.  A "no-
 *    descendants" limitation governs the use of DNAMEs in a zone file:
 * 
 *       If a DNAME RR is present at a node N, there may be other data at N
 *       (except a CNAME or another DNAME), but there MUST be no data at
 *       any descendant of N.  This restriction applies only to records of
 *       the same class as the DNAME record.
 * 
 *    This rule assures predictable results when a DNAME record is cached
 *    by a server which is not authoritative for the record's zone.  It
 *    MUST be enforced when authoritative zone data is loaded.  Together
 *    with the rules for DNS zone authority [DNSCLR] it implies that DNAME
 *    and NS records can only coexist at the top of a zone which has only
 *    one node.
 * 
 *    The compression scheme of [DNSIS] MUST NOT be applied to the RDATA
 *    portion of a DNAME record unless the sending server has some way of
 *    knowing that the receiver understands the DNAME record format.
 *    Signalling such understanding is expected to be the subject of future
 *    DNS Extensions.
 * 
 *    Naming loops can be created with DNAME records or a combination of
 *    DNAME and CNAME records, just as they can with CNAME records alone.
 *    Resolvers, including resolvers embedded in DNS servers, MUST limit
 *    the resources they devote to any query.  Implementors should note,
 *    however, that fairly lengthy chains of DNAME records may be valid.
 */
public class DnameRecordEncoder
{
}
