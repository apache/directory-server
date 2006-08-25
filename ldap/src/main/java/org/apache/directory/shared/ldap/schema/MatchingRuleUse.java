/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *  
 *    http://www.apache.org/licenses/LICENSE-2.0
 *  
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License. 
 *  
 */
package org.apache.directory.shared.ldap.schema;


import javax.naming.NamingException;


/**
 * Represents an LDAP MatchingRuleUseDescription defined in RFC 2252.
 * <p>
 * According to ldapbis [MODELS]:
 * </p>
 * 
 * <pre>
 *  Values of the matchingRuleUse list the attributes which are suitable
 *  for use with an extensible matching rule.
 *  
 *    Matching rule use descriptions are written according to the following
 *    ABNF:
 * 
 *      MatchingRuleUseDescription = LPAREN WSP
 *          numericoid                ; object identifier
 *          [ SP &quot;NAME&quot; SP qdescrs ]  ; short names (descriptors)
 *          [ SP &quot;DESC&quot; SP qdstring ] ; description
 *          [ SP &quot;OBSOLETE&quot; ]         ; not active
 *          SP &quot;APPLIES&quot; SP oids      ; attribute types
 *          extensions WSP RPAREN     ; extensions
 *  
 *    where:
 *      [numericoid] is the object identifier of the matching rule
 *          associated with this matching rule use description;
 *      NAME [qdescrs] are short names (descriptors) identifying this
 *          matching rule use;
 *      DESC [qdstring] is a short descriptive string;
 *      OBSOLETE indicates this matching rule use is not active;
 *      APPLIES provides a list of attribute types the matching rule applies
 *          to; and
 *      [extensions] describe extensions.
 * 
 *  The matchingRule within the MatchingRuleUse definition can be used by an
 *  extensible match assertion if the assertion is based on the attributes 
 *  listed within the MatchingRuleUse definition.  If an extensible match 
 *  assertion is based on attributes other than those listed within the 
 *  MatchingRuleUse definition then the assertion is deemed undefined.
 *  
 *  Also according to 3.3.20 of [SYNTAXES] (ldapbis working group):
 *  
 *  A value of the Matching Rule Use Description syntax indicates the
 *  attribute types to which a matching rule may be applied in an
 *  extensibleMatch search filter [PROT].  The LDAP-specific encoding of
 *  a value of this syntax is defined by the &lt;MatchingRuleUseDescription&gt;
 *  rule in [MODELS] above.
 * </pre>
 * 
 * @see <a
 *      href="http://www.ietf.org/internet-drafts/draft-ietf-ldapbis-models-11.txt">ldapbis
 *      [MODELS]</a>
 * @see <a
 *      href="http://www.ietf.org/internet-drafts/draft-ietf-ldapbis-syntaxes-09.txt">ldapbis
 *      [SYNTAXES]</a>
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public interface MatchingRuleUse extends SchemaObject
{
    /**
     * Gets the matchingRule this MatchingRuleUse definition applies to.
     * 
     * @return the matchingRule
     * @throws NamingException
     *             if there is a failure resolving the object
     */
    public MatchingRule getMatchingRule() throws NamingException;


    /**
     * Gets the the attributes which can be used with the matching rule in an
     * extensible match assertion.
     * 
     * @return the applicable attributes
     * @throws NamingException
     *             if there is a failure resolving the object
     */
    public AttributeType[] getApplicableAttributes() throws NamingException;
}
