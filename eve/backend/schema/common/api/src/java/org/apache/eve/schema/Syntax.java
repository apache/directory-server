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
 * A syntax definition.  Each attribute stored in a directory has a defined 
 * syntax (i.e. data type) which constrains the structure and format of its 
 * values.  The description of each syntax specifies how attribute or assertion 
 * values conforming to the syntax are normally represented when transferred 
 * in LDAP operations.  This representation is referred to as the LDAP-specific 
 * encoding to distinguish it from other methods of encoding attribute values.
 * <p>
 * According to ldapbis [MODELS]:
 * </p>
 * <pre>
 * 4.1.5. LDAP Syntaxes
 * 
 *   LDAP Syntaxes of (attribute and assertion) values are described in
 *   terms of ASN.1 [X.680] and, optionally, have an octet string encoding
 *   known as the LDAP-specific encoding.  Commonly, the LDAP-specific
 *   encoding is constrained to string of Universal Character Set (UCS)
 *   [ISO10646] characters in UTF-8 [UTF-8] form.
 *
 *   Each LDAP syntax is identified by an object identifier (OID).
 *
 *   LDAP syntax definitions are written according to the ABNF:
 *
 *     SyntaxDescription = LPAREN WSP
 *         numericoid                 ; object identifier
 *         [ SP "DESC" SP qdstring ]  ; description
 *         extensions WSP RPAREN      ; extensions
 *
 *   where:
 *     [numericoid] is object identifier assigned to this LDAP syntax;
 *     DESC [qdstring] is a short descriptive string; and
 *     [extensions] describe extensions.
 * </pre>
 * 
 * @see <a href="http://www.faqs.org/rfcs/rfc2252.html">
 *      RFC2252 Section 4.3.3</a>
 * @see <a href=
 *      "http://www.ietf.org/internet-drafts/draft-ietf-ldapbis-models-09.txt">
 *      ldapbis [MODELS]</a>
 * @see DescriptionUtils#getDescription(Syntax)
 * @author <a href="mailto:akarasulu@apache.org">Alex Karasulu</a>
 * @author $Author$
 * @version $Rev$
 */
public interface Syntax
{
    /**
     * Gets whether or not the Syntax is human readable.
     * 
     * @return true if the syntax can be interpretted by humans, false otherwise
     */
    boolean isHumanReadable() ;
    
    /**
     * Gets a description of this Syntax.
     * 
     * @return a description
     */
    String getDescription() ;
    
    /**
     * Gets a short descriptive name for the Syntax. 
     * 
     * @return a short name
     */
    String getName() ;
    
    /**
     * Gets the oid for this Syntax.
     * 
     * @return the object identifier
     */
    String getOid() ;
    
    /**
     * Gets the SyntaxChecker used to validate values in accordance with this
     * Syntax.
     * 
     * @return the SyntaxChecker
     */
    SyntaxChecker getSyntaxChecker() ;
}
