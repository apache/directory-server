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
 * An objectClass definition.  
 * <p>
 * According to ldapbis [MODELS]:
 * </p>
 * <pre>
 * Object Class definitions are written according to the ABNF:
 * 
 *   ObjectClassDescription = LPAREN WSP
 *       numericoid                 ; object identifier
 *       [ SP "NAME" SP qdescrs ]   ; short names (descriptors)
 *       [ SP "DESC" SP qdstring ]  ; description
 *       [ SP "OBSOLETE" ]          ; not active
 *       [ SP "SUP" SP oids ]       ; superior object classes
 *       [ SP kind ]                ; kind of class
 *       [ SP "MUST" SP oids ]      ; attribute types
 *       [ SP "MAY" SP oids ]       ; attribute types
 *       extensions WSP RPAREN
 *
 *    kind = "ABSTRACT" / "STRUCTURAL" / "AUXILIARY"
 *
 *  where:
 *    [numericoid] is object identifier assigned to this object class;
 *    NAME [qdescrs] are short names (descriptors) identifying this object
 *        class;
 *    DESC [qdstring] is a short descriptive string;
 *    OBSOLETE indicates this object class is not active;
 *    SUP [oids] specifies the direct superclasses of this object class;
 *    the kind of object class is indicated by one of ABSTRACT,
 *        STRUCTURAL, or AUXILIARY, default is STRUCTURAL;
 *    MUST and MAY specify the sets of required and allowed attribute
 *        types, respectively; and
 *   [extensions] describe extensions.
 * </pre>
 * 
 * @see <a href="http://www.faqs.org/rfcs/rfc2252.html">RFC2252 Section 4.4</a>
 * @see <a href=
 *     "http://www.ietf.org/internet-drafts/draft-ietf-ldapbis-models-09.txt">
 *     ldapbis [MODELS]</a>
 * @see DescriptionUtils#getDescription(ObjectClass)
 * @author <a href="mailto:akarasulu@apache.org">Alex Karasulu</a>
 * @author $Author$
 * @version $Rev$
 */
public interface ObjectClass
{
    /**
     * Gets a verbal description of this ObjectClass.
     *
     * @return a verbal description
     */
    String getDescription() ;

    /**
     * Gets the object identifier for this ObjectClass definition.
     *
     * @return the OID for this ObjectClass
     */
    String getOid() ;

    /**
     * Gets the human readable name of this ObjectClass.
     *
     * @return the name of this ObjectClass
     */
    String getName() ;

    /**
     * Gets whether or not this NameForm is obsolete.
     *
     * @return true if obsolete, false if not.
     */
    boolean isObsolete() ;

    /**
     * Gets the superclasses of this ObjectClass.
     *
     * @return the superclasses
     */
    ObjectClass [] getSuperClasses() ;

    /**
     * Gets the type of this ObjectClass as a type safe enum.
     *
     * @return the ObjectClass type as an enum
     */
    ObjectClassTypeEnum getType() ;

    /**
     * Gets the AttributeTypes whose attributes must be present within an entry
     * of this ObjectClass.
     *
     * @return the AttributeTypes of attributes that must be within entries of
     * this ObjectClass
     */
    AttributeType [] getMustList() ;

    /**
     * Gets the AttributeTypes whose attributes may be present within an entry
     * of this ObjectClass.
     *
     * @return the AttributeTypes of attributes that may be within entries of
     * this ObjectClass
     */
    AttributeType [] getMayList() ;
}