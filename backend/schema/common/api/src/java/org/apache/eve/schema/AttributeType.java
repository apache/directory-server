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
 * An attributeType specification.  attributeType specifications describe the
 * nature of attributes within the directory.  The attributeType specification's
 * properties are accessible through this interface.
 * <p>
 * According to ldapbis [MODELS]:
 * </p>
 * <pre>
 * 4.1.2. Attribute Types
 * 
 *   Attribute Type definitions are written according to the ABNF:
 * 
 *     AttributeTypeDescription = LPAREN WSP
 *         numericoid                    ; object identifier
 *         [ SP "NAME" SP qdescrs ]      ; short names (descriptors)
 *         [ SP "DESC" SP qdstring ]     ; description
 *         [ SP "OBSOLETE" ]             ; not active
 *         [ SP "SUP" SP oid ]           ; supertype
 *         [ SP "EQUALITY" SP oid ]      ; equality matching rule
 *         [ SP "ORDERING" SP oid ]      ; ordering matching rule
 *         [ SP "SUBSTR" SP oid ]        ; substrings matching rule
 *         [ SP "SYNTAX" SP noidlen ]    ; value syntax
 *         [ SP "SINGLE-VALUE" ]         ; single-value
 *         [ SP "COLLECTIVE" ]           ; collective
 *         [ SP "NO-USER-MODIFICATION" ] ; not user modifiable
 *         [ SP "USAGE" SP usage ]       ; usage
 *         extensions WSP RPAREN         ; extensions
 * 
 *     usage = "userApplications"     /  ; user
 *             "directoryOperation"   /  ; directory operational
 *             "distributedOperation" /  ; DSA-shared operational
 *             "dSAOperation"            ; DSA-specific operational
 * 
 *   where:
 *     [numericoid] is object identifier assigned to this attribute type;
 *     NAME [qdescrs] are short names (descriptors) identifying this
 *         attribute type;
 *     DESC [qdstring] is a short descriptive string;
 *     OBSOLETE indicates this attribute type is not active;
 *     SUP oid specifies the direct supertype of this type;
 *     EQUALITY, ORDERING, SUBSTRING provide the oid of the equality,
 *         ordering, and substrings matching rules, respectively;
 *     SYNTAX identifies value syntax by object identifier and may suggest
 *         a minimum upper bound;
 *     COLLECTIVE indicates this attribute type is collective [X.501];
 *     NO-USER-MODIFICATION indicates this attribute type is not user
 *         modifiable;
 *     USAGE indicates the application of this attribute type; and
 *     [extensions] describe extensions.
 * 
 *   Each attribute type description must contain at least one of the SUP
 *   or SYNTAX fields.
 * 
 *   Usage of userApplications, the default, indicates that attributes of
 *   this type represent user information.  That is, they are user
 *   attributes.
 * 
 *   COLLECTIVE requires usage userApplications.  Use of collective
 *   attribute types in LDAP is not discussed in this technical
 *   specification.
 * 
 *   A usage of directoryOperation, distributedOperation, or dSAOperation
 *   indicates that attributes of this type represent operational and/or
 *   administrative information.  That is, they are operational attributes.
 * 
 *   directoryOperation usage indicates that the attribute of this type is
 *   a directory operational attribute.  distributedOperation usage
 *   indicates that the attribute of this DSA-shared usage operational
 *   attribute.  dSAOperation usage indicates that the attribute of this
 *   type is a DSA-specific operational attribute.
 * 
 *   NO-USER-MODIFICATION requires an operational usage.
 * 
 *   Note that the [AttributeTypeDescription] does not list the matching
 *   rules which can be used with that attribute type in an extensibleMatch
 *   search filter.  This is done using the 'matchingRuleUse' attribute
 *   described in Section 4.1.4.
 * 
 *   This document refines the schema description of X.501 by requiring
 *   that the SYNTAX field in an [AttributeTypeDescription] be a string
 *   representation of an object identifier for the LDAP string syntax
 *   definition with an optional indication of the suggested minimum bound
 *   of a value of this attribute.
 * 
 *   A suggested minimum upper bound on the number of characters in a value
 *   with a string-based syntax, or the number of bytes in a value for all
 *   other syntaxes, may be indicated by appending this bound count inside
 *   of curly braces following the syntax's OBJECT IDENTIFIER in an
 * 
 *   Attribute Type Description.  This bound is not part of the syntax name
 *   itself.  For instance, "1.3.6.4.1.1466.0{64}" suggests that server
 *   implementations should allow a string to be 64 characters long,
 *   although they may allow longer strings.  Note that a single character
 *   of the Directory String syntax may be encoded in more than one octet
 *   since UTF-8 is a variable-length encoding.
 * </pre>
 * 
 * @see <a href="http://www.faqs.org/rfcs/rfc2252.html">RFC 2252 Section 4.2</a>
 * @see <a href=
 *     "http://www.ietf.org/internet-drafts/draft-ietf-ldapbis-models-09.txt">
 *     ldapbis [MODELS]</a>
 * @see DescriptionUtils#getDescription(AttributeType)
 * @author <a href="mailto:akarasulu@apache.org">Alex Karasulu</a>
 * @author $Author$
 * @version $Rev$
 */
public interface AttributeType
{
    /**
     * Gets the object identifier for this AttributeType.
     * 
     * @return the object identifier for this AttributeType
     */
    String getOid() ;
    
    /**
     * Gets the first name in the list of names for this AttributeTypeImpl.
     *
     * @return the first name in the list of names
     */
    String getName() ;
    
    /**
     * Gets all the names for this AttributeType.
     *
     * @return String names for this AttributeType
     */
    String[] getAllNames() ;

    /**
     * Gets a description for this AttributeType.
     *
     * @return the verbal description
     */
    String getDescription() ;

    /**
     * Gets whether or not this AttributeType is single-valued.
     *
     * @return true if only one value can exist for this AttributeType, false
     *      otherwise
     */
    boolean isSingleValue() ;

    /**
     * Gets whether or not this AttributeType can be modified by a user.
     *
     * @return true if users can modify it, false if only the directory can.
     */
    boolean isCanUserModify() ;

    /**
     * Gets whether or not this AttributeType is a collective attribute.
     * 
     * @return true if the attribute is collective, false otherwise
     */
    boolean isCollective() ;
    
    /**
     * Gets whether or not this AttributeType is obsolete.
     *
     * @return true if obsolete, false if not.
     */
    boolean isObsolete() ;

    /**
     * Determines the usage for this AttributeType.
     *
     * @return a type safe UsageEnum
     */
    UsageEnum getUsage() ;

    /**
     * Gets the name of the superior class for this AttributeType.
     *
     * @return the super class for this AttributeType 
     */
    AttributeType getSuperior() ;

    /**
     * The Syntax for this AttributeType's values.
     *
     * @return the value syntax
     */
    Syntax getSyntax() ;

    /**
     * Gets a length limit for this AttributeType.
     * 
     * @return the length of the attribute
     */
    int getLength() ;

    /**
     * Gets the MatchingRule for this AttributeType used for equality matching.
     *
     * @return the equality matching rule
     */
    MatchingRule getEquality() ;

    /**
     * Gets the MatchingRule for this AttributeType used for ordering.
     *
     * @return the ordering matching rule
     */
    MatchingRule getOrdering() ;

    /**
     * Gets the MatchingRule for this AttributeType used for substring matching.
     *
     * @return the substring matching rule
     */
    MatchingRule getSubstr() ;
}
