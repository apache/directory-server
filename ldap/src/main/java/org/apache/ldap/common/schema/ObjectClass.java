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
 * An objectClass definition.  
 * <p>
 * According to ldapbis [MODELS]:
 * </p>
 * <pre>
 * Object Class definitions are written according to the ABNF:
 * 
 *   ObjectClassDescription = LPAREN WSP
 *       numericoid                ; object identifier
 *       [ SP "NAME" SP qdescrs ]  ; short names (descriptors)
 *       [ SP "DESC" SP qdstring ] ; description
 *       [ SP "OBSOLETE" ]         ; not active
 *       [ SP "SUP" SP oids ]      ; superior object classes
 *       [ SP kind ]               ; kind of class
 *       [ SP "MUST" SP oids ]     ; attribute types
 *       [ SP "MAY" SP oids ]      ; attribute types
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
 * @see <a href="http://www.ietf.org/internet-drafts/draft-ietf-ldapbis-models-11.txt">ldapbis [MODELS]</a>
 * @see DescriptionUtils#getDescription(ObjectClass)
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public interface ObjectClass extends SchemaObject
{
    /**
     * Gets the superclasses of this ObjectClass.
     *
     * @return the superclasses
     * @throws NamingException if there is a failure resolving the object
     */
    ObjectClass[] getSuperClasses() throws NamingException;

    /**
     * Gets the type of this ObjectClass as a type safe enum.
     *
     * @return the ObjectClass type as an enum
     */
    ObjectClassTypeEnum getType();

    /**
     * Gets the AttributeTypes whose attributes must be present within an entry
     * of this ObjectClass.
     *
     * @return the AttributeTypes of attributes that must be within entries of
     * this ObjectClass
     * @throws NamingException if there is a failure resolving the object
     */
    AttributeType[] getMustList() throws NamingException;

    /**
     * Gets the AttributeTypes whose attributes may be present within an entry
     * of this ObjectClass.
     *
     * @return the AttributeTypes of attributes that may be within entries of
     * this ObjectClass
     * @throws NamingException if there is a failure resolving the object
     */
    AttributeType[] getMayList() throws NamingException;
}