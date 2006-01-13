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
package org.apache.ldap.common.schema;

import javax.naming.NamingException;


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
 *        numericoid                ; object identifier
 *        [ SP "NAME" SP qdescrs ]  ; short names (descriptors)
 *        [ SP "DESC" SP qdstring ] ; description
 *        [ SP "OBSOLETE" ]         ; not active
 *        SP "OC" SP oid            ; structural object class
 *        SP "MUST" SP oids         ; attribute types
 *        [ SP "MAY" SP oids ]      ; attribute types
 *        extensions WSP RPAREN     ; extensions
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
 * @see <a href="http://www.ietf.org/internet-drafts/draft-ietf-ldapbis-models-11.txt">ldapbis [MODELS]</a>
 * @see DescriptionUtils#getDescription(NameForm)
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public interface NameForm extends SchemaObject
{
    /**
     * Gets the STRUCTURAL ObjectClass this name form specifies naming 
     * attributes for.
     *
     * @return the ObjectClass this NameForm is for
     * @throws NamingException if there is a failure resolving the object
     */
    ObjectClass getObjectClass() throws NamingException;

    /**
     * Gets all the AttributeTypes of the attributes this NameForm specifies as 
     * having to be used in the given objectClass for naming: as part of the 
     * Rdn.
     *
     * @return the AttributeTypes of the must use attributes
     * @throws NamingException if there is a failure resolving the object
     */
    AttributeType[] getMustUse() throws NamingException;
    
    /**
     * Gets all the AttributeTypes of the attribute this NameForm specifies as 
     * being useable without requirement in the given objectClass for naming: 
     * as part of the Rdn.
     *
     * @return the AttributeTypes of the may use attributes
     * @throws NamingException if there is a failure resolving the object
     */
    AttributeType[] getMaytUse() throws NamingException;
}
