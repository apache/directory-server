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
 * A nameForm description.  NameForms define the relationship between a 
 * STRUCTURAL objectClass definition and the attributeTypes allowed to be used
 * for the naming of an Entry of that objectClass: it defines which attributes 
 * can be used for the RDN.  
 * <p> 
 * According to ldapbis [MODELS]:
 * </p>
 * <pre>
 * 4.1.7.2. Name Forms
 * 
 *  A name form "specifies a permissible RDN for entries of a particular
 *  structural object class.  A name form identifies a named object
 *  class and one or more attribute types to be used for naming (i.e.
 *  for the RDN).  Name forms are primitive pieces of specification
 *  used in the definition of DIT structure rules" [X.501].
 *
 *  Each name form indicates the structural object class to be named,
 *  a set of required attribute types, and a set of allowed attributes
 *  types.  A particular attribute type cannot be listed in both sets.
 *
 *  Entries governed by the form must be named using a value from each
 *  required attribute type and zero or more values from the allowed
 *  attribute types.
 *
 *  Each name form is identified by an object identifier (OID) and,
 *  optionally, one or more short names (descriptors).
 *
 *  Name form descriptions are written according to the ABNF:
 *
 *    NameFormDescription = LPAREN WSP
 *        numericoid                 ; object identifier
 *        [ SP "NAME" SP qdescrs ]   ; short names (descriptors)
 *        [ SP "DESC" SP qdstring ]  ; description
 *        [ SP "OBSOLETE" ]          ; not active
 *        SP "OC" SP oid             ; structural object class
 *        SP "MUST" SP oids          ; attribute types
 *        [ SP "MAY" SP oids ]       ; attribute types
 *        extensions WSP RPAREN      ; extensions
 *
 *  where:
 *
 *    [numericoid] is object identifier which identifies this name form;
 *    NAME [qdescrs] are short names (descriptors) identifying this name
 *        form;
 *    DESC [qdstring] is a short descriptive string;
 *    OBSOLETE indicates this name form is not active;
 *    OC identifies the structural object class this rule applies to,
 *    MUST and MAY specify the sets of required and allowed, respectively,
 *        naming attributes for this name form; and
 *    [extensions] describe extensions.
 *
 *  All attribute types in the required ("MUST") and allowed ("MAY") lists
 *  shall be different.
 * </pre>
 *
 * @see <a href="http://www.faqs.org/rfcs/rfc2252.html">RFC2252 Section 6.22</a>
 * @see <a href=
 *     "http://www.ietf.org/internet-drafts/draft-ietf-ldapbis-models-09.txt">
 *     ldapbis [MODELS]</a>
 * @see DescriptionUtils#getDescription(NameForm)
 * @author <a href="mailto:akarasulu@apache.org">Alex Karasulu</a>
 * @author $Author$
 * @version $Rev$
 */
public interface NameForm
{
    /**
     * Gets a description of this NameForm.
     *
     * @return a description
     */
    String getDescription() ;

    /**
     * Gets a short descriptive name for the NameForm.
     *
     * @return a short name.
     */
    String getName() ;

    /**
     * Gets the oid for this NameForm.
     *
     * @return the object identifier
     */
    String getOid() ;

    /**
     * Gets whether or not this NameForm is obsolete.
     *
     * @return true if obsolete, false if not.
     */
    boolean isObsolete() ;

    /**
     * Gets the STRUCTURAL ObjectClass this name form specifies naming 
     * attributes for.
     *
     * @return the ObjectClass this NameForm is for
     */
    ObjectClass getObjectClass() ;

    /**
     * Gets all the AttributeTypes of the attributes this NameForm specifies as 
     * having to be used in the given objectClass for naming: as part of the 
     * Rdn.
     *
     * @return the AttributeTypes of the must use attributes
     */
    AttributeType [] getMustUse() ;
    
    /**
     * Gets all the AttributeTypes of the attribute this NameForm specifies as 
     * being useable without requirement in the given objectClass for naming: 
     * as part of the Rdn.
     *
     * @return the AttributeTypes of the may use attributes
     */
    AttributeType [] getMaytUse() ;
}
