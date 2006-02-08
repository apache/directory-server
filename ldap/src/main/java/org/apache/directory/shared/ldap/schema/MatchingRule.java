/*
 *   Copyright 2004 The Apache Software Foundation
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
package org.apache.directory.shared.ldap.schema;


import javax.naming.NamingException;
import java.util.Comparator;


/**
 * A matchingRule definition.  MatchingRules associate a comparator and a 
 * normalizer, forming the basic tools necessary to assert actions against 
 * attribute values.  MatchingRules are associated with a specific Syntax 
 * for the purpose of resolving a normalized form and for comparisons. 
 * <p>
 * According to ldapbis [MODELS]:
 * </p>
 * <pre>
 * 4.1.3. Matching Rules
 * 
 *   Matching rules are used by servers to compare attribute values against
 *   assertion values when performing Search and Compare operations.  They
 *   are also used to identify the value to be added or deleted when
 *   modifying entries, and are used when comparing a purported
 *   distinguished name with the name of an entry.
 * 
 *   A matching rule specifies the syntax of the assertion value.
 *
 *   Each matching rule is identified by an object identifier (OID) and,
 *   optionally, one or more short names (descriptors).
 *
 *   Matching rule definitions are written according to the ABNF:
 *
 *     MatchingRuleDescription = LPAREN WSP
 *         numericoid                ; object identifier
 *         [ SP "NAME" SP qdescrs ]  ; short names (descriptors)
 *         [ SP "DESC" SP qdstring ] ; description
 *         [ SP "OBSOLETE" ]         ; not active
 *         SP "SYNTAX" SP numericoid ; assertion syntax
 *         extensions WSP RPAREN     ; extensions
 *
 *   where:
 *     [numericoid] is object identifier assigned to this matching rule;
 *     NAME [qdescrs] are short names (descriptors) identifying this
 *         matching rule;
 *     DESC [qdstring] is a short descriptive string;
 *     OBSOLETE indicates this matching rule is not active;
 *     SYNTAX identifies the assertion syntax by object identifier; and
 *     [extensions] describe extensions.
 * </pre>
 *
 * @see <a href="http://www.faqs.org/rfcs/rfc2252.html">RFC 2252 Section 4.5</a>
 * @see <a href="http://www.ietf.org/internet-drafts/draft-ietf-ldapbis-models-11.txt">ldapbis [MODELS]</a>
 * @see DescriptionUtils#getDescription(MatchingRule)
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public interface MatchingRule extends SchemaObject
{
    /**
     * Gets the SyntaxImpl used by this MatchingRule.
     * 
     * @return the SyntaxImpl of this MatchingRule
     * @throws NamingException if there is a failure resolving the object
     */
    Syntax getSyntax() throws NamingException;

    /**
     * Gets the Comparator enabling the use of this MatchingRule for ORDERING
     * and sorted indexing. 
     * 
     * @return the ordering Comparator 
     * @throws NamingException if there is a failure resolving the object
     */
    Comparator getComparator() throws NamingException;

    /**
     * Gets the Normalizer enabling the use of this MatchingRule for EQUALITY
     * matching and indexing. 
     * 
     * @return the ordering Comparator 
     * @throws NamingException if there is a failure resolving the object
     */
    Normalizer getNormalizer() throws NamingException;
}
