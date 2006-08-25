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
 * A dITStructureRule definition. A dITStructureRules is a rule governing the
 * structure of the DIT by specifying a permitted superior to subordinate entry
 * relationship. A structure rule relates a nameForm, and therefore a STRUCTURAL
 * objectClass, to superior dITStructureRules. This permits entries of the
 * STRUCTURAL objectClass identified by the nameForm to exist in the DIT as
 * subordinates to entries governed by the indicated superior dITStructureRules.
 * Hence dITStructureRules only apply to structural object classes.
 * <p>
 * According to ldapbis [MODELS]:
 * </p>
 * 
 * <pre>
 *  DIT structure rule descriptions are written according to the ABNF:
 *  
 *    DITStructureRuleDescription = LPAREN WSP
 *        ruleid                    ; rule identifier
 *        [ SP &quot;NAME&quot; SP qdescrs ]  ; short names (descriptors)
 *        [ SP &quot;DESC&quot; SP qdstring ] ; description
 *        [ SP &quot;OBSOLETE&quot; ]         ; not active
 *        SP &quot;FORM&quot; SP oid          ; NameForm
 *        [ SP &quot;SUP&quot; ruleids ]      ; superior rules
 *        extensions WSP RPAREN     ; extensions
 * 
 *    ruleids = ruleid / ( LPAREN WSP ruleidlist WSP RPAREN )
 * 
 *    ruleidlist = ruleid *( SP ruleid )
 * 
 *    ruleid = number
 * 
 *  where:
 *    [ruleid] is the rule identifier of this DIT structure rule;
 *    NAME [qdescrs] are short names (descriptors) identifying this DIT
 *        structure rule;
 *    DESC [qdstring] is a short descriptive string;
 *    OBSOLETE indicates this DIT structure rule use is not active;
 *    FORM is specifies the name form associated with this DIT structure
 *        rule;
 *    SUP identifies superior rules (by rule id); and
 *    [extensions] describe extensions.
 *  
 *  If no superior rules are identified, the DIT structure rule applies
 *  to an autonomous administrative point (e.g. the root vertex of the
 *  subtree controlled by the subschema) [X.501].
 * </pre>
 * 
 * @see <a href="http://www.faqs.org/rfcs/rfc2252.html">RFC2252 Section 6.33</a>
 * @see <a
 *      href="http://www.ietf.org/internet-drafts/draft-ietf-ldapbis-models-11.txt">ldapbis
 *      [MODELS]</a>
 * @see DescriptionUtils#getDescription(DITStructureRule)
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public interface DITStructureRule extends SchemaObject
{
    /**
     * The nameForm associating this ditStructureRule with a structural
     * objectClass.
     * 
     * @return the nameForm for the structural objectClass
     * @throws NamingException
     *             if there is a failure resolving the object
     */
    NameForm getNameForm() throws NamingException;


    /**
     * Gets a collection of all the superior StructureRules. The difference with
     * getSuperClass is this method will resolve the entire superior class
     * chain.
     * 
     * @return the chain of StructureRules
     * @throws NamingException
     *             if there is a failure resolving the object
     */
    DITStructureRule[] getSuperClasses() throws NamingException;
}
