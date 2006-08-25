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
 * A syntax definition. Each attribute stored in a directory has a defined
 * syntax (i.e. data type) which constrains the structure and format of its
 * values. The description of each syntax specifies how attribute or assertion
 * values conforming to the syntax are normally represented when transferred in
 * LDAP operations. This representation is referred to as the LDAP-specific
 * encoding to distinguish it from other methods of encoding attribute values.
 * <p>
 * According to ldapbis [MODELS]:
 * </p>
 * 
 * <pre>
 *  4.1.5. LDAP Syntaxes
 *  
 *    LDAP Syntaxes of (attribute and assertion) values are described in
 *    terms of ASN.1 [X.680] and, optionally, have an octet string encoding
 *    known as the LDAP-specific encoding.  Commonly, the LDAP-specific
 *    encoding is constrained to string of Universal Character Set (UCS)
 *    [ISO10646] characters in UTF-8 [UTF-8] form.
 * 
 *    Each LDAP syntax is identified by an object identifier (OID).
 * 
 *    LDAP syntax definitions are written according to the ABNF:
 * 
 *      SyntaxDescription = LPAREN WSP
 *          numericoid                ; object identifier
 *          [ SP &quot;DESC&quot; SP qdstring ] ; description
 *          extensions WSP RPAREN     ; extensions
 * 
 *    where:
 *      [numericoid] is object identifier assigned to this LDAP syntax;
 *      DESC [qdstring] is a short descriptive string; and
 *      [extensions] describe extensions.
 * </pre>
 * 
 * @see <a href="http://www.faqs.org/rfcs/rfc2252.html"> RFC2252 Section 4.3.3</a>
 * @see <a href=
 *      "http://www.ietf.org/internet-drafts/draft-ietf-ldapbis-models-09.txt">
 *      ldapbis [MODELS]</a>
 * @see DescriptionUtils#getDescription(Syntax)
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public interface Syntax extends SchemaObject
{
    /**
     * Gets whether or not the Syntax is human readible.
     * 
     * @return true if the syntax can be interpretted by humans, false otherwise
     */
    boolean isHumanReadible();


    /**
     * Gets the SyntaxChecker used to validate values in accordance with this
     * Syntax.
     * 
     * @return the SyntaxChecker
     */
    SyntaxChecker getSyntaxChecker() throws NamingException;
}
