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
 * A ditContentRule specification.  ditContentRules identify the content of 
 * entries of a particular structural objectClass.  They specify the AUXILLARY 
 * objectClasses and additional attribute types permitted to appear, or excluded
 * from appearing in entries of the indicated STRUCTURAL objectClass. 
 * <p>
 * According to ldapbis [MODELS]:
 * </p>
 * <pre>
 * 4.1.6. DIT Content Rules
 * 
 *   A DIT content rule is a "rule governing the content of entries of a
 *   particular structural object class" [X.501].
 * 
 *   For DIT entries of a particular structural object class, a DIT content
 *   rule specifies which auxiliary object classes the entries are allowed
 *   to belong to and which additional attributes (by type) are required,
 *   allowed or not allowed to appear in the entries.
 * 
 *   The list of precluded attributes cannot include any attribute listed
 *   as mandatory in rule, the structural object class, or any of the
 *   allowed auxiliary object classes.
 * 
 *   Each content rule is identified by the object identifier, as well as
 *   any short names (descriptors), of the structural object class it
 *   applies to.
 * 
 *   An entry may only belong to auxiliary object classes listed in the
 *   governing content rule.
 * 
 *   An entry must contain all attributes required by the object classes
 *   the entry belongs to as well as all attributed required by the
 *   governing content rule.
 * 
 *   An entry may contain any non-precluded attributes allowed by the
 *   object classes the entry belongs to as well as all attributes allowed
 *   by the governing content rule.
 * 
 *   An entry cannot include any attribute precluded by the governing
 *   content rule.
 * 
 *   An entry is governed by (if present and active in the subschema) the
 *   DIT content rule which applies to the structural object class of the
 *   entry (see Section 2.4.2).  If no active rule is present for the
 *   entry's structural object class, the entry's content is governed by
 *   the structural object class (and possibly other aspects of user and
 *   system schema).
 *
 *   DIT content rule descriptions are written according to the ABNF:
 *
 *     DITContentRuleDescription = LPAREN WSP
 *         numericoid                 ; object identifier
 *         [ SP "NAME" SP qdescrs ]   ; short names (descriptors)
 *         [ SP "DESC" SP qdstring ]  ; description
 *         [ SP "OBSOLETE" ]          ; not active
 *         [ SP "AUX" SP oids ]       ; auxiliary object classes
 *         [ SP "MUST" SP oids ]      ; attribute types
 *         [ SP "MAY" SP oids ]       ; attribute types
 *         [ SP "NOT" SP oids ]       ; attribute types
 *         extensions WSP RPAREN      ; extensions
 *
 *   where:
 *
 *     [numericoid] is the object identifier of the structural object class
 *         associated with this DIT content rule;
 *     NAME [qdescrs] are short names (descriptors) identifying this DIT
 *         content rule;
 *     DESC [qdstring] is a short descriptive string;
 *     OBSOLETE indicates this DIT content rule use is not active;
 *     AUX specifies a list of auxiliary object classes which entries
 *         subject to this DIT content rule may belong to;
 *     MUST, MAY, and NOT specify lists of attribute types which are
 *         required, allowed, or precluded, respectively, from appearing in
 *         entries subject to this DIT content rule; and
 *     [extensions] describe extensions.
 * </pre>
 * 
 * @see <a href="http://www.faqs.org/rfcs/rfc2252.html">RFC 2252 Section 5.4.3
 * </a>
 * @see <a href=
 *     "http://www.ietf.org/internet-drafts/draft-ietf-ldapbis-models-09.txt">
 *     ldapbis [MODELS]</a>
 * @see DescriptionUtils#getDescription(DITContentRule)
 * @author <a href="mailto:akarasulu@apache.org">Alex Karasulu</a>
 * @author $Author$
 * @version $Rev$
 */
public interface DITContentRule
{
    /**
     * Gets a description of this DITContentRule.
     *
     * @return a description
     */
    String getDescription() ;

    /**
     * Gets a short descriptive name for the DITContentRule.
     *
     * @return a short name.
     */
    String getName() ;

    /**
     * Gets the oid for this DITContentRule.
     *
     * @return the object identifier
     */
    String getOid() ;

    /**
     * Gets whether or not this DITContentRule is obsolete.
     *
     * @return true if obsolete, false if not.
     */
    boolean isObsolete() ;

    /**
     * Gets the STRUCTURAL ObjectClass this DITContentRule specifies attributes 
     * for.
     *
     * @return the ObjectClass this DITContentRule specifies attributes for
     */
    ObjectClass getObjectClass() ;

    /**
     * Gets all the AUXILLARY ObjectClasses this DITContentRule specifies for the 
     * given STRUCTURAL objectClass.
     *
     * @return the extra AUXILLARY ObjectClasses
     */
    ObjectClass[] getAuxObjectClasses() ;

    /**
     * Gets all the AttributeTypes of the "must" attribute names this 
     * DITContentRule specifies for the given STRUCTURAL objectClass.
     *
     * @return the AttributeTypes of attributes that must be included in entries
     */
    AttributeType[] getMustNames() ;

    /**
     * Gets all the AttributeTypes of the "may" attribute names this DITContentRule
     * specifies for the given STRUCTURAL objectClass.
     *
     * @return the AttributeTypes of attributes that may be included in entries
     */
    AttributeType[] getMayNames() ;

    /**
     * Gets all the AttributeTypes of the "not" attribute names this DITContentRule
     * specifies for the given STRUCTURAL objectClass.
     *
     * @return the AttributeTypes of attributes that are excluded in entries
     */
    AttributeType[] getNotNames() ;
}
