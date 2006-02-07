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
 * 4. NAPTR RR Format
 * 
 * 4.1 Packet Format
 * 
 *    The packet format of the NAPTR RR is given below.  The DNS type code
 *    for NAPTR is 35.
 * 
 *       The packet format for the NAPTR record is as follows
 *                                        1  1  1  1  1  1
 *          0  1  2  3  4  5  6  7  8  9  0  1  2  3  4  5
 *        +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
 *        |                     ORDER                     |
 *        +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
 *        |                   PREFERENCE                  |
 *        +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
 *        /                     FLAGS                     /
 *        +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
 *        /                   SERVICES                    /
 *        +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
 *        /                    REGEXP                     /
 *        +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
 *        /                  REPLACEMENT                  /
 *        /                                               /
 *        +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
 * 
 *    <character-string> and <domain-name> as used here are defined in RFC
 *    1035 [7].
 * 
 *    ORDER
 *       A 16-bit unsigned integer specifying the order in which the NAPTR
 *       records MUST be processed in order to accurately represent the
 *       ordered list of Rules.  The ordering is from lowest to highest.
 *       If two records have the same order value then they are considered
 *       to be the same rule and should be selected based on the
 *       combination of the Preference values and Services offered.
 * 
 *    PREFERENCE
 *       Although it is called "preference" in deference to DNS
 *       terminology, this field is equivalent to the Priority value in the
 *       DDDS Algorithm.  It is a 16-bit unsigned integer that specifies
 *       the order in which NAPTR records with equal Order values SHOULD be
 *       processed, low numbers being processed before high numbers.  This
 *       is similar to the preference field in an MX record, and is used so
 *       domain administrators can direct clients towards more capable
 *       hosts or lighter weight protocols.  A client MAY look at records
 *       with higher preference values if it has a good reason to do so
 *       such as not supporting some protocol or service very well.
 * 
 *       The important difference between Order and Preference is that once
 *       a match is found the client MUST NOT consider records with a
 *       different Order but they MAY process records with the same Order
 *       but different Preferences.  The only exception to this is noted in
 *       the second important Note in the DDDS algorithm specification
 *       concerning allowing clients to use more complex Service
 *       determination between steps 3 and 4 in the algorithm.  Preference
 *       is used to give communicate a higher quality of service to rules
 *       that are considered the same from an authority standpoint but not
 *       from a simple load balancing standpoint.
 * 
 *       It is important to note that DNS contains several load balancing
 *       mechanisms and if load balancing among otherwise equal services
 *       should be needed then methods such as SRV records or multiple A
 *       records should be utilized to accomplish load balancing.
 * 
 *    FLAGS
 *       A <character-string> containing flags to control aspects of the
 *       rewriting and interpretation of the fields in the record.  Flags
 *       are single characters from the set A-Z and 0-9.  The case of the
 *       alphabetic characters is not significant.  The field can be empty.
 * 
 *       It is up to the Application specifying how it is using this
 *       Database to define the Flags in this field.  It must define which
 *       ones are terminal and which ones are not.
 * 
 *    SERVICES
 *       A <character-string> that specifies the Service Parameters
 *       applicable to this this delegation path.  It is up to the
 *       Application Specification to specify the values found in this
 *       field.
 * 
 *    REGEXP
 *       A <character-string> containing a substitution expression that is
 *       applied to the original string held by the client in order to
 *       construct the next domain name to lookup.  See the DDDS Algorithm
 *       specification for the syntax of this field.
 * 
 *       As stated in the DDDS algorithm, The regular expressions MUST NOT
 *       be used in a cumulative fashion, that is, they should only be
 *       applied to the original string held by the client, never to the
 *       domain name produced by a previous NAPTR rewrite.  The latter is
 *       tempting in some applications but experience has shown such use to
 *       be extremely fault sensitive, very error prone, and extremely
 *       difficult to debug.
 * 
 *    REPLACEMENT
 *       A <domain-name> which is the next domain-name to query for
 *       depending on the potential values found in the flags field.  This
 *       field is used when the regular expression is a simple replacement
 *       operation.  Any value in this field MUST be a fully qualified
 *       domain-name.  Name compression is not to be used for this field.
 * 
 *       This field and the REGEXP field together make up the Substitution
 *       Expression in the DDDS Algorithm.  It is simply a historical
 *       optimization specifically for DNS compression that this field
 *       exists.  The fields are also mutually exclusive.  If a record is
 *       returned that has values for both fields then it is considered to
 *       be in error and SHOULD be either ignored or an error returned.
 */
public class NameAuthorityPointerEncoder
{
}
