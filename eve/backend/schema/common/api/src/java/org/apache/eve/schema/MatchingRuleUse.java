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


/**
 * Represents an LDAP MatchingRuleUseDescription defined in RFC 2252.
 * <p>
 * According to ldapbis [MODELS]:
 * </p>
 * <pre>
 * Values of the matchingRuleUse list the attributes which are suitable
 * for use with an extensible matching rule.
 * 
 *   Matching rule use descriptions are written according to the following
 *   ABNF:
 *
 *     MatchingRuleUseDescription = LPAREN WSP
 *         numericoid                 ; object identifier
 *         [ SP "NAME" SP qdescrs ]   ; short names (descriptors)
 *         [ SP "DESC" SP qdstring ]  ; description
 *         [ SP "OBSOLETE" ]          ; not active
 *         SP "APPLIES" SP oids       ; attribute types
 *         extensions WSP RPAREN      ; extensions
 * 
 *   where:
 *     [numericoid] is the object identifier of the matching rule
 *         associated with this matching rule use description;
 *     NAME [qdescrs] are short names (descriptors) identifying this
 *         matching rule use;
 *     DESC [qdstring] is a short descriptive string;
 *     OBSOLETE indicates this matching rule use is not active;
 *     APPLIES provides a list of attribute types the matching rule applies
 *         to; and
 *     [extensions] describe extensions.
 *
 * The matchingRule within the MatchingRuleUse definition can be used by an
 * extensible match assertion if the assertion is based on the attributes 
 * listed within the MatchingRuleUse definition.  If an extensible match 
 * assertion is based on attributes other than those listed within the 
 * MatchingRuleUse definition then the assertion is deemed undefined.
 * 
 * Also according to 3.3.20 of [SYNTAXES] (ldapbis working group):
 * 
 * A value of the Matching Rule Use Description syntax indicates the
 * attribute types to which a matching rule may be applied in an
 * extensibleMatch search filter [PROT].  The LDAP-specific encoding of
 * a value of this syntax is defined by the <MatchingRuleUseDescription>
 * rule in [MODELS] above.
 * </pre>
 * 
 * @see <a href=
 *     "http://www.ietf.org/internet-drafts/draft-ietf-ldapbis-models-09.txt">
 *     ldapbis [MODELS]</a>
 * @see <a href=
 *     "http://www.ietf.org/internet-drafts/draft-ietf-ldapbis-syntaxes-07.txt">
 *     ldapbis [SYNTAXES]</a>
 * @author <a href="mailto:akarasulu@apache.org">Alex Karasulu</a>
 * @author $Author$
 * @version $Rev$
 */
public interface MatchingRuleUse
{
    /**
     * Gets a long description for the MatchingRuleUse.
     * 
     * @return a long description
     */
    String getDescription() ;

    /**
     * Gets a short descriptive name for the MatchingRuleUse. 
     * 
     * @return a short name
     */
    String getName() ;

    /**
     * Gets whether or not this MatchingRuleUse has been obsoleted for another.
     * 
     * @return true if it is obsolete false otherwise
     */
    boolean isObsolete() ;

    /**
     * Gets the matchingRule this MatchingRuleUse definition applies to. 
     * 
     * @return the matchingRule
     */
    public MatchingRule getMatchingRule() ;
    
    /**
     * Gets the the attributes which can be used with the matching rule in an 
     * extensible match assertion.
     * 
     * @return the applicable attributes
     */
    public AttributeType [] getApplicableAttributes() ;
}
