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


/**
 * A ditContentRule specification.  ditContentRules identify the content of 
 * entries of a particular structural objectClass.  They specify the AUXILIARY
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
 *         numericoid                ; object identifier
 *         [ SP "NAME" SP qdescrs ]  ; short names (descriptors)
 *         [ SP "DESC" SP qdstring ] ; description
 *         [ SP "OBSOLETE" ]         ; not active
 *         [ SP "AUX" SP oids ]      ; auxiliary object classes
 *         [ SP "MUST" SP oids ]     ; attribute types
 *         [ SP "MAY" SP oids ]      ; attribute types
 *         [ SP "NOT" SP oids ]      ; attribute types
 *         extensions WSP RPAREN     ; extensions
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
 * @see <a href="http://www.faqs.org/rfcs/rfc2252.html">RFC 2252 Section 5.4.3</a>
 * @see <a href="http://www.ietf.org/internet-drafts/draft-ietf-ldapbis-models-11.txt">ldapbis [MODELS]</a>
 * @see DescriptionUtils#getDescription(DITContentRule)
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public interface DITContentRule extends SchemaObject
{
    /**
     * Gets the STRUCTURAL ObjectClass this DITContentRule specifies attributes 
     * for.
     *
     * @return the ObjectClass this DITContentRule specifies attributes for
     * @throws NamingException if there is a failure resolving the object
     */
    ObjectClass getObjectClass() throws NamingException;

    /**
     * Gets all the AUXILIARY ObjectClasses this DITContentRule specifies for the
     * given STRUCTURAL objectClass.
     *
     * @return the extra AUXILIARY ObjectClasses
     * @throws NamingException if there is a failure resolving the object
     */
    ObjectClass[] getAuxObjectClasses() throws NamingException;

    /**
     * Gets all the AttributeTypes of the "must" attribute names this 
     * DITContentRule specifies for the given STRUCTURAL objectClass.
     *
     * @return the AttributeTypes of attributes that must be included in entries
     * @throws NamingException if there is a failure resolving the object
     */
    AttributeType[] getMustNames() throws NamingException;

    /**
     * Gets all the AttributeTypes of the "may" attribute names this DITContentRule
     * specifies for the given STRUCTURAL objectClass.
     *
     * @return the AttributeTypes of attributes that may be included in entries
     * @throws NamingException if there is a failure resolving the object
     */
    AttributeType[] getMayNames() throws NamingException;

    /**
     * Gets all the AttributeTypes of the "not" attribute names this DITContentRule
     * specifies for the given STRUCTURAL objectClass.
     *
     * @return the AttributeTypes of attributes that are excluded in entries
     * @throws NamingException if there is a failure resolving the object
     */
    AttributeType[] getNotNames() throws NamingException;
}
