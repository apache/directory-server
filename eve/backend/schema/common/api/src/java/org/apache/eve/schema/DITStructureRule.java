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
 * A dITStructureRule definition.  A dITStructureRules is a rule governing the 
 * structure of the DIT by specifying a permitted superior to subordinate entry 
 * relationship.  A structure rule relates a nameForm, and therefore a 
 * STRUCTURAL objectClass, to superior dITStructureRules.  This permits entries 
 * of the STRUCTURAL objectClass identified by the nameForm to exist in the DIT 
 * as subordinates to entries governed by the indicated superior 
 * dITStructureRules.  Hence dITStructureRules only apply to structural object
 * classes.
 * <p>
 * According to ldapbis [MODELS]:
 * </p>
 * <pre>
 * DIT structure rule descriptions are written according to the ABNF:
 * 
 *   DITStructureRuleDescription = LPAREN WSP
 *       ruleid                     ; rule identifier
 *       [ SP "NAME" SP qdescrs ]   ; short names (descriptors)
 *       [ SP "DESC" SP qdstring ]  ; description
 *       [ SP "OBSOLETE" ]          ; not active
 *       SP "FORM" SP oid           ; NameForm
 *       [ SP "SUP" ruleids ]       ; superior rules
 *       extensions WSP RPAREN      ; extensions
 *
 *   ruleids = ruleid / ( LPAREN WSP ruleidlist WSP RPAREN )
 *
 *   ruleidlist = ruleid *( SP ruleid )
 *
 *   ruleid = number
 *
 * where:
 *   [ruleid] is the rule identifier of this DIT structure rule;
 *   NAME [qdescrs] are short names (descriptors) identifying this DIT
 *       structure rule;
 *   DESC [qdstring] is a short descriptive string;
 *   OBSOLETE indicates this DIT structure rule use is not active;
 *   FORM is specifies the name form associated with this DIT structure
 *       rule;
 *   SUP identifies superior rules (by rule id); and
 *   [extensions] describe extensions.
 * 
 * If no superior rules are identified, the DIT structure rule applies
 * to an autonomous administrative point (e.g. the root vertex of the
 * subtree controlled by the subschema) [X.501].
 * </pre>
 * 
 * @see <a href="http://www.faqs.org/rfcs/rfc2252.html">RFC2252 Section 6.33</a>
 * @see <a href=
 *     "http://www.ietf.org/internet-drafts/draft-ietf-ldapbis-models-09.txt">
 *     ldapbis [MODELS]</a>
 * @see DescriptionUtils#getDescription(DITStructureRule)
 * @author <a href="mailto:akarasulu@apache.org">Alex Karasulu</a>
 * @author $Author$
 * @version $Rev$
 */
public interface DITStructureRule
{
    /**
     * Gets a description of this DITStructureRule.
     *
     * @return a description
     */
    String getDescription() ;

    /**
     * Gets a short descriptive name for the DITStructureRule.
     *
     * @return a short name.
     */
    String getName() ;

    /**
     * Gets the oid for this DITStructureRule.
     *
     * @return the object identifier
     */
    String getOid() ;

    /**
     * Gets whether or not this DITStructureRule is obsolete.
     *
     * @return true if obsolete, false if not.
     */
    boolean isObsolete() ;

    /**
     * The nameForm associating this ditStructureRule with a structural 
     * objectClass.
     *
     * @return the nameForm for the structural objectClass
     */
    NameForm getNameForm() ;

    /**
     * Gets a collection of all the superior StructureRules. The difference 
     * with getSuperClass is this method will resolve the entire superior 
     * class chain.
     *
     * @return the chain of StructureRules 
     */
    DITStructureRule [] getSuperClasses() ;
}
