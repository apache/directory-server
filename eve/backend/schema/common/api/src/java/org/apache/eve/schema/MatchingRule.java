/*

 ============================================================================
                   The Apache Software License, Version 1.1
 ============================================================================

 Copyright (C) 1999-2002 The Apache Software Foundation. All rights reserved.

 Redistribution and use in source and binary forms, with or without modifica-
 tion, are permitted provided that the following conditions are met:

 1. Redistributions of  source code must  retain the above copyright  notice,
    this list of conditions and the following disclaimer.

 2. Redistributions in binary form must reproduce the above copyright notice,
    this list of conditions and the following disclaimer in the documentation
    and/or other materials provided with the distribution.

 3. The end-user documentation included with the redistribution, if any, must
    include  the following  acknowledgment:  "This product includes  software
    developed  by the  Apache Software Foundation  (http://www.apache.org/)."
    Alternately, this  acknowledgment may  appear in the software itself,  if
    and wherever such third-party acknowledgments normally appear.

 4. The names "Eve Directory Server", "Apache Directory Project", "Apache Eve" 
    and "Apache Software Foundation"  must not be used to endorse or promote
    products derived  from this  software without  prior written
    permission. For written permission, please contact apache@apache.org.

 5. Products  derived from this software may not  be called "Apache", nor may
    "Apache" appear  in their name,  without prior written permission  of the
    Apache Software Foundation.

 THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED WARRANTIES,
 INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 FITNESS  FOR A PARTICULAR  PURPOSE ARE  DISCLAIMED.  IN NO  EVENT SHALL  THE
 APACHE SOFTWARE  FOUNDATION  OR ITS CONTRIBUTORS  BE LIABLE FOR  ANY DIRECT,
 INDIRECT, INCIDENTAL, SPECIAL,  EXEMPLARY, OR CONSEQUENTIAL  DAMAGES (INCLU-
 DING, BUT NOT LIMITED TO, PROCUREMENT  OF SUBSTITUTE GOODS OR SERVICES; LOSS
 OF USE, DATA, OR  PROFITS; OR BUSINESS  INTERRUPTION)  HOWEVER CAUSED AND ON
 ANY  THEORY OF LIABILITY,  WHETHER  IN CONTRACT,  STRICT LIABILITY,  OR TORT
 (INCLUDING  NEGLIGENCE OR  OTHERWISE) ARISING IN  ANY WAY OUT OF THE  USE OF
 THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

 This software  consists of voluntary contributions made  by many individuals
 on  behalf of the Apache Software  Foundation. For more  information on the
 Apache Software Foundation, please see <http://www.apache.org/>.

*/
package org.apache.eve.schema ;


import java.util.Comparator ;


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
 *         numericoid                 ; object identifier
 *         [ SP "NAME" SP qdescrs ]   ; short names (descriptors)
 *         [ SP "DESC" SP qdstring ]  ; description
 *         [ SP "OBSOLETE" ]          ; not active
 *         SP "SYNTAX" SP numericoid  ; assertion syntax
 *         extensions WSP RPAREN      ; extensions
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
 * @see <a href=
 *     "http://www.ietf.org/internet-drafts/draft-ietf-ldapbis-models-09.txt">
 *     ldapbis [MODELS]</a>
 * @see DescriptionUtils#getDescription(MatchingRule)
 * @author <a href="mailto:akarasulu@apache.org">Alex Karasulu</a>
 * @author $Author$
 * @version $Rev$
 */
public interface MatchingRule
{
    /**
     * Gets a long description for the MatchingRule.
     * 
     * @return a long description
     */
    String getDescription() ;

    /**
     * Gets a short descriptive name for the MatchingRule. 
     * 
     * @return a short name
     */
    String getName() ;

    /**
     * Gets the oid for this MatchingRule.
     * 
     * @return the object identifier
     */
    String getOid() ;

    /**
     * Gets the SyntaxImpl used by this MatchingRule.
     * 
     * @return the SyntaxImpl of this MatchingRule
     */
    Syntax getSyntax() ;

    /**
     * Gets whether or not this MatchingRule has been obsoleted for another.
     * 
     * @return true if it is obsolete false otherwise
     */
    boolean isObsolete() ;

    /**
     * Gets the Comparator enabling the use of this MatchingRule for ORDERING
     * and sorted indexing. 
     * 
     * @return the ordering Comparator 
     */
    Comparator getComparator() ;

    /**
     * Gets the Normalizer enabling the use of this MatchingRule for EQUALITY
     * matching and indexing. 
     * 
     * @return the ordering Comparator 
     */
    Normalizer getNormalizer() ;
}
