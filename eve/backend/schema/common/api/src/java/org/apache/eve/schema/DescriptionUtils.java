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
 * Utility class used to generate schema object specifications.  Some of the 
 * latest work coming out of the LDAPBIS working body adds optional extensions 
 * to these syntaxes.  We have not yet added extension support to these 
 * functions or the schema interfaces in this package.  Descriptions can be 
 * generated for the following objects:
 * <ul>
 * <li><a href="./AttributeType.html">AttributeType</a></li>
 * <li><a href="./DITContentRule.html">DITContentRule</a></li>
 * <li><a href="./MatchingRule.html">MatchingRule</a></li>
 * <li><a href="./MatchingRuleUse.html">MatchingRuleUse</a></li>
 * <li><a href="./NameForm.html">NameForm</a></li>
 * <li><a href="./ObjectClass.html">ObjectClass</a></li>
 * <li><a href="./DITStructureRule.html">DITStructureRule</a></li>
 * <li><a href="./Syntax.html">Syntax</a></li>
 * </ul>
 * 
 * @todo possibly add xml descriptor generation functions
 * @author <a href="mailto:akarasulu@apache.org">Alex Karasulu</a>
 * @author $Author$
 * @version $Rev$
 */
public class DescriptionUtils
{
    /**
     * Generates the description using the AttributeTypeDescription as defined
     * by the syntax: 1.3.6.1.4.1.1466.115.121.1.3.  Only the right hand side 
     * of the description starting at the openning parenthesis is generated: 
     * that is 'AttributeTypeDescription = ' is not generated.
     * <pre>
     * AttributeTypeDescription = "(" whsp
     *    numericoid whsp                 ; AttributeType identifier
     *    [ "NAME" qdescrs ]              ; name used in AttributeType
     *    [ "DESC" qdstring ]             ; description
     *    [ "OBSOLETE" whsp ]
     *    [ "SUP" woid ]                  ; derived from parent AttributeType
     *    [ "EQUALITY" woid               ; Matching Rule name
     *    [ "ORDERING" woid               ; Matching Rule name
     *    [ "SUBSTR" woid ]               ; Matching Rule name
     *    [ "SYNTAX" whsp noidlen whsp ]  ; see section 4.3 RFC 2252
     *    [ "SINGLE-VALUE" whsp ]         ; default multi-valued
     *    [ "COLLECTIVE" whsp ]           ; default not collective
     *    [ "NO-USER-MODIFICATION" whsp ] ; default user modifiable
     *    [ "USAGE" whsp AttributeUsage ] ; default userApplications
     *    whsp ")"
     * </pre>
     * @param an_attributeType the attributeType to generate a description for
     * @return the AttributeTypeDescription Syntax for the attributeType in a
     * pretty formated string
     */
    public static String getDescription( AttributeType an_attributeType ) 
    {
        StringBuffer l_buf = new StringBuffer( "( " ) ;
        l_buf.append( an_attributeType.getOid() ) ;
        l_buf.append( '\n' ) ;
        
        l_buf.append( "NAME " ) ;
        l_buf.append( an_attributeType.getName() ) ;
        l_buf.append( '\n' ) ;
        
        if ( an_attributeType.getDescription() != null )
        {    
            l_buf.append( "DESC " ) ;
            l_buf.append( an_attributeType.getDescription() ) ;
            l_buf.append( '\n' ) ;
        }
        
        if ( an_attributeType.isObsolete() )
        {    
            l_buf.append( "OBSOLETE" ) ;
            l_buf.append( '\n' ) ;
        }
        
        l_buf.append( an_attributeType.getSuperior().getOid() ) ;
        
        if ( an_attributeType.getEquality() != null )
        {
            l_buf.append( "EQUALITY " ) ;
            l_buf.append( an_attributeType.getEquality().getOid() ) ;
            l_buf.append( '\n' ) ;
        }
        
        if ( an_attributeType.getOrdering() != null )
        {
            l_buf.append( "ORDERING " ) ;
            l_buf.append( an_attributeType.getOrdering().getOid() ) ;
            l_buf.append( '\n' ) ;
        }
        
        if ( an_attributeType.getSubstr() != null )
        {
            l_buf.append( "SUBSTR " ) ;
            l_buf.append( an_attributeType.getSubstr().getOid() ) ;
            l_buf.append( '\n' ) ;
        }
        
        l_buf.append( "SYNTAX " ) ;
        l_buf.append( an_attributeType.getSyntax().getOid() ) ;
        l_buf.append( '\n' ) ;
        
        if ( an_attributeType.isSingleValue() )
        {
            l_buf.append( "SINGLE-VALUE" ) ;
            l_buf.append( '\n' ) ;
        }
        
        if ( an_attributeType.isCollective() )
        {
            l_buf.append( "COLLECTIVE" ) ;
            l_buf.append( '\n' ) ;
        }
        
        if ( an_attributeType.isCanUserModify() )
        {
            l_buf.append( "NO-USER-MODIFICATION" ) ;
            l_buf.append( '\n' ) ;
        }
        
        l_buf.append( "USAGE " ) ;
        l_buf.append( an_attributeType.getUsage().getName() ) ;
        l_buf.append( " ) " ) ;
        
        return l_buf.toString() ;
    }
    
    
    /**
     * Generates the DITContentRuleDescription for a DITContentRule as defined 
     * by the syntax: 1.3.6.1.4.1.1466.115.121.1.16.  Only the right hand side 
     * of the description starting at the openning parenthesis is generated: 
     * that is 'DITContentRuleDescription = ' is not generated.
     * <pre>
     *  DITContentRuleDescription = "("
     *      numericoid          ; Structural ObjectClass identifier
     *      [ "NAME" qdescrs ]
     *      [ "DESC" qdstring ]
     *      [ "OBSOLETE" ]
     *      [ "AUX" oids ]      ; Auxiliary ObjectClasses
     *      [ "MUST" oids ]     ; AttributeType identifiers
     *      [ "MAY" oids ]      ; AttributeType identifiers
     *      [ "NOT" oids ]      ; AttributeType identifiers
     *     ")"
     * </pre>
     * @param a_crule the DIT content rule specification 
     * @return the specification according to the DITContentRuleDescription
     * syntax 
     */
    public static String getDescription( DITContentRule a_dITContentRule )
    {
        StringBuffer l_buf = new StringBuffer( "( " ) ;
        l_buf.append( a_dITContentRule.getOid() ) ;
        l_buf.append( '\n' ) ;
        
        l_buf.append( "NAME " ) ;
        l_buf.append( a_dITContentRule.getName() ) ;
        l_buf.append( '\n' ) ;
        
        if ( a_dITContentRule.getDescription() != null )
        {    
            l_buf.append( "DESC " ) ;
            l_buf.append( a_dITContentRule.getDescription() ) ;
            l_buf.append( '\n' ) ;
        }
        
        if ( a_dITContentRule.isObsolete() )
        {    
            l_buf.append( "OBSOLETE" ) ;
            l_buf.append( '\n' ) ;
        }
        
        // print out all the auxillary object class oids
        ObjectClass [] l_aux = a_dITContentRule.getAuxObjectClasses() ;
        if ( l_aux != null && l_aux.length > 0 )
        {
            l_buf.append( "AUX\n" ) ;
            for ( int ii = 0; ii < l_aux.length; ii++ ) 
            {
                l_buf.append( '\t' ) ;
                l_buf.append( l_aux[ii].getOid() ) ;
                l_buf.append( '\n' ) ;
            }
        }
        
        AttributeType [] l_must = a_dITContentRule.getMustNames() ;
        if ( l_must != null && l_must.length > 0 )
        {
            l_buf.append( "MUST\n" ) ;
            for ( int ii = 0; ii < l_must.length; ii++ ) 
            {
                l_buf.append( '\t' ) ;
                l_buf.append( l_must[ii].getOid() ) ;
                l_buf.append( '\n' ) ;
            }
        }
        
        AttributeType [] l_may = a_dITContentRule.getMayNames() ;
        if ( l_may != null && l_may.length > 0 )
        {
            l_buf.append( "MAY\n" ) ;
            for ( int ii = 0; ii < l_may.length; ii++ ) 
            {
                l_buf.append( '\t' ) ;
                l_buf.append( l_may[ii].getOid() ) ;
                l_buf.append( '\n' ) ;
            }
        }
        
        AttributeType [] l_not = a_dITContentRule.getNotNames() ;
        if ( l_not != null && l_not.length > 0 )
        {
            l_buf.append( "NOT\n" ) ;
            for ( int ii = 0; ii < l_not.length; ii++ ) 
            {
                l_buf.append( '\t' ) ;
                l_buf.append( l_not[ii].getOid() ) ;
                l_buf.append( '\n' ) ;
            }
        }
        
        l_buf.append( " )" ) ;
        return l_buf.toString() ;
    }
    
    
    /**
     * Generates the MatchingRuleDescription for a MatchingRule as defined by 
     * the syntax: 1.3.6.1.4.1.1466.115.121.1.30.  Only the right hand side 
     * of the description starting at the openning parenthesis is generated: 
     * that is 'MatchingRuleDescription = ' is not generated.
     * <pre>
     * MatchingRuleDescription = "(" whsp
     *    numericoid whsp       ; MatchingRule object identifier
     *    [ "NAME" qdescrs ]
     *    [ "DESC" qdstring ]
     *    [ "OBSOLETE" whsp ]
     *    "SYNTAX" numericoid
     * whsp ")"
     * </pre>
     * @param a_matchingEule the MatchingRule to generate the description for
     * @return the MatchingRuleDescription string
     */
    public static String getDescription( MatchingRule a_matchingRule )
    {
        StringBuffer l_buf = new StringBuffer( "( " ) ;
        l_buf.append( a_matchingRule.getOid() ) ;
        l_buf.append( '\n' ) ;
        
        l_buf.append( "NAME " ) ;
        l_buf.append( a_matchingRule.getName() ) ;
        l_buf.append( '\n' ) ;
        
        if ( a_matchingRule.getDescription() != null )
        {    
            l_buf.append( "DESC " ) ;
            l_buf.append( a_matchingRule.getDescription() ) ;
            l_buf.append( '\n' ) ;
        }
        
        if ( a_matchingRule.isObsolete() )
        {    
            l_buf.append( "OBSOLETE" ) ;
            l_buf.append( '\n' ) ;
        }
        
        l_buf.append( "SYNTAX " ) ;
        l_buf.append( a_matchingRule.getSyntax().getOid() ) ;
        l_buf.append( " ) " ) ;
        return l_buf.toString() ;
    }
    
    
    /**
     * Generates the MatchingRuleUseDescription for a MatchingRuleUse as defined
     * by the syntax: 1.3.6.1.4.1.1466.115.121.1.31.  Only the right hand side 
     * of the description starting at the openning parenthesis is generated: 
     * that is 'MatchingRuleUseDescription = ' is not generated.
     * <pre>
     *     MatchingRuleUseDescription = LPAREN WSP
     *         numericoid                 ; object identifier
     *         [ SP "NAME" SP qdescrs ]   ; short names (descriptors)
     *         [ SP "DESC" SP qdstring ]  ; description
     *         [ SP "OBSOLETE" ]          ; not active
     *         SP "APPLIES" SP oids       ; attribute types
     *         extensions WSP RPAREN      ; extensions
     * 
     *   where:
     *     [numericoid] is the object identifier of the matching rule
     *         associated with this matching rule use description;
     *     NAME [qdescrs] are short names (descriptors) identifying this
     *         matching rule use;
     *     DESC [qdstring] is a short descriptive string;
     *     OBSOLETE indicates this matching rule use is not active;
     *     APPLIES provides a list of attribute types the matching rule applies
     *         to; and
     *     [extensions] describe extensions.
     * </pre>
     * @param a_matchingRuleUse
     * @return
     */
    public static String getDescription( MatchingRuleUse a_matchingRuleUse )
    {
        StringBuffer l_buf = new StringBuffer( "( " ) ;
        l_buf.append( a_matchingRuleUse.getMatchingRule().getOid() ) ;
        l_buf.append( '\n' ) ;
        
        l_buf.append( "NAME " ) ;
        l_buf.append( a_matchingRuleUse.getName() ) ;
        l_buf.append( '\n' ) ;
        
        if ( a_matchingRuleUse.getDescription() != null )
        {    
            l_buf.append( "DESC " ) ;
            l_buf.append( a_matchingRuleUse.getDescription() ) ;
            l_buf.append( '\n' ) ;
        }
        
        if ( a_matchingRuleUse.isObsolete() )
        {    
            l_buf.append( "OBSOLETE" ) ;
            l_buf.append( '\n' ) ;
        }
        
        l_buf.append( "APPLIES " ) ;
        AttributeType[] l_attributeTypes = 
            a_matchingRuleUse.getApplicableAttributes() ;
        if ( l_attributeTypes.length == 1 )
        {
            l_buf.append( l_attributeTypes[0].getOid() ) ;
        }
        else // for list of oids we need a parenthesis
        {
            l_buf.append( "( " ) ;
            l_buf.append( l_attributeTypes[0] ) ;
            for ( int ii = 1; ii < l_attributeTypes.length; ii++ )
            {
                l_buf.append( " $ " ) ;
                l_buf.append( l_attributeTypes[ii] ) ;
            }
            l_buf.append( " ) " ) ;
        }
        
        l_buf.append( '\n' ) ;
        return l_buf.toString() ;
    }
    
    
    /**
     * Generates the NameFormDescription for a NameForm as defined by the 
     * syntax: 1.3.6.1.4.1.1466.115.121.1.35.  Only the right hand side 
     * of the description starting at the openning parenthesis is generated: 
     * that is 'NameFormDescription = ' is not generated.
     * <pre>
     * NameFormDescription = "(" whsp
     *     numericoid whsp                ; NameForm identifier
     *     [ "NAME" qdescrs ]
     *     [ "DESC" qdstring ]
     *     [ "OBSOLETE" whsp ]
     *     "OC" woid                      ; Structural ObjectClass
     *     "MUST" oids                    ; AttributeTypes
     *     [ "MAY" oids ]                 ; AttributeTypes
     * whsp ")"
     *</pre>
     * @param a_nameForm the NameForm to generate the description for
     * @return the NameFormDescription string
     */
    public static String getDescription( NameForm a_nameForm )
    {
        StringBuffer l_buf = new StringBuffer( "( " ) ;
        l_buf.append( a_nameForm.getOid() ) ;
        l_buf.append( '\n' ) ;
        
        l_buf.append( "NAME " ) ;
        l_buf.append( a_nameForm.getName() ) ;
        l_buf.append( '\n' ) ;
        
        if ( a_nameForm.getDescription() != null )
        {    
            l_buf.append( "DESC " ) ;
            l_buf.append( a_nameForm.getDescription() ) ;
            l_buf.append( '\n' ) ;
        }
        
        if ( a_nameForm.isObsolete() )
        {    
            l_buf.append( "OBSOLETE" ) ;
            l_buf.append( '\n' ) ;
        }
        
        l_buf.append( "OC " ) ;
        l_buf.append( a_nameForm.getObjectClass().getOid() ) ;
        l_buf.append( '\n' ) ;
        
        l_buf.append( "MUST\n" ) ;
        AttributeType[] l_must = a_nameForm.getMustUse() ;
        for ( int ii = 0; ii < l_must.length; ii++ )
        {
            l_buf.append( '\t' ) ;
            l_buf.append( l_must[ii].getOid() ) ;
            l_buf.append( '\n' ) ;
        }
        
        AttributeType[] l_may = a_nameForm.getMaytUse() ;
        if ( l_may != null && l_may.length > 0 )
        {
            l_buf.append( "MAY\n" ) ;
            for ( int ii = 0; ii < l_must.length; ii++ )
            {
                l_buf.append( '\t' ) ;
                l_buf.append( l_may[ii].getOid() ) ;
                l_buf.append( '\n' ) ;
            }
        }
        
        l_buf.append( " )" ) ;
        return l_buf.toString() ;
    }
    
    
    /**
     * Generates the ObjectClassDescription for an ObjectClass as defined by the
     * syntax: 1.3.6.1.4.1.1466.115.121.1.37.  Only the right hand side 
     * of the description starting at the openning parenthesis is generated: 
     * that is 'ObjectClassDescription = ' is not generated.
     * <pre>
     * ObjectClassDescription = "(" whsp
     *     numericoid whsp      ; ObjectClass identifier
     *     [ "NAME" qdescrs ]
     *     [ "DESC" qdstring ]
     *     [ "OBSOLETE" whsp ]
     *     [ "SUP" oids ]       ; Superior ObjectClasses
     *     [ ( "ABSTRACT" / "STRUCTURAL" / "AUXILIARY" ) whsp ]
     *                          ; default structural
     *     [ "MUST" oids ]      ; AttributeTypes
     *     [ "MAY" oids ]       ; AttributeTypes
     * whsp ")"
     * </pre>
     * @param a_objectClass the ObjectClass to generate a description for
     * @return the description in the ObjectClassDescription syntax
     */
    public static String getDescription( ObjectClass a_objectClass )
    {
        StringBuffer l_buf = new StringBuffer( "( " ) ;
        l_buf.append( a_objectClass.getOid() ) ;
        l_buf.append( '\n' ) ;
        
        l_buf.append( "NAME " ) ;
        l_buf.append( a_objectClass.getName() ) ;
        l_buf.append( '\n' ) ;
        
        if ( a_objectClass.getDescription() != null )
        {    
            l_buf.append( "DESC " ) ;
            l_buf.append( a_objectClass.getDescription() ) ;
            l_buf.append( '\n' ) ;
        }
        
        if ( a_objectClass.isObsolete() )
        {    
            l_buf.append( "OBSOLETE" ) ;
            l_buf.append( '\n' ) ;
        }
        
        ObjectClass [] l_sups = a_objectClass.getSuperClasses() ;
        if ( l_sups != null && l_sups.length > 0 )
        {
            l_buf.append( "SUP\n" ) ;
            for ( int ii = 0; ii < l_sups.length; ii++ )
            {
                l_buf.append( '\t' ) ;
                l_buf.append( l_sups[ii].getOid() ) ;
                l_buf.append( '\n' ) ;
            }
        }
        
        if ( a_objectClass.getType() != null )
        {
            l_buf.append( a_objectClass.getType().getName() ) ;
            l_buf.append( '\n' ) ;
        }
        
        AttributeType [] l_must = a_objectClass.getMustList() ;
        if ( l_must != null && l_must.length > 0 )
        {
            l_buf.append( "MUST\n" ) ;
            for ( int ii = 0; ii < l_must.length; ii++ )
            {
                l_buf.append( '\t' ) ;
                l_buf.append( l_must[ii].getOid() ) ;
                l_buf.append( '\n' ) ;
            }
        }
        
        AttributeType [] l_may = a_objectClass.getMayList() ;
        if ( l_may != null && l_may.length > 0 )
        {
            l_buf.append( "MAY\n" ) ;
            for ( int ii = 0; ii < l_may.length; ii++ )
            {
                l_buf.append( '\t' ) ;
                l_buf.append( l_may[ii].getOid() ) ;
                l_buf.append( '\n' ) ;
            }
        }
        
        l_buf.append( " )" ) ;
        return l_buf.toString() ;
    }
    
    
    /**
     * Generates the DITStructureRuleDescription for a DITStructureRule as 
     * defined by the syntax: 1.3.6.1.4.1.1466.115.121.1.17.  Only the right 
     * hand side of the description starting at the openning parenthesis is 
     * generated: that is 'DITStructureRuleDescription = ' is not generated.
     * <pre>
     * DITStructureRuleDescription = "(" whsp
     *     ruleidentifier whsp            ; DITStructureRule identifier
     *     [ "NAME" qdescrs ]
     *     [ "DESC" qdstring ]
     *     [ "OBSOLETE" whsp ]
     *     "FORM" woid whsp               ; NameForm
     *     [ "SUP" ruleidentifiers whsp ] ; superior DITStructureRules
     * ")"
     * </pre>
     * @param a_dITStructureRule the DITStructureRule to generate the
     *      description for
     * @return the description in the DITStructureRuleDescription syntax
     */
    public static String getDescription( DITStructureRule a_dITStructureRule )
    {
        StringBuffer l_buf = new StringBuffer( "( " ) ;
        l_buf.append( a_dITStructureRule.getOid() ) ;
        l_buf.append( '\n' ) ;
        
        l_buf.append( "NAME " ) ;
        l_buf.append( a_dITStructureRule.getName() ) ;
        l_buf.append( '\n' ) ;
        
        if ( a_dITStructureRule.getDescription() != null )
        {    
            l_buf.append( "DESC " ) ;
            l_buf.append( a_dITStructureRule.getDescription() ) ;
            l_buf.append( '\n' ) ;
        }
        
        if ( a_dITStructureRule.isObsolete() )
        {    
            l_buf.append( "OBSOLETE" ) ;
            l_buf.append( '\n' ) ;
        }
        
        l_buf.append( "FORM " ) ;
        l_buf.append( a_dITStructureRule.getNameForm().getOid() ) ;
        l_buf.append( '\n' ) ;
        
        DITStructureRule [] l_sups = a_dITStructureRule.getSuperClasses() ;
        if ( l_sups != null && l_sups.length > 0 )
        {
            l_buf.append( "SUP\n" ) ;
            for ( int ii = 0; ii < l_sups.length; ii++ )
            {
                l_buf.append( '\t' ) ;
                l_buf.append( l_sups[ii].getOid() ) ;
                l_buf.append( '\n' ) ;
            }
        }
        
        l_buf.append( " )" ) ;
        return l_buf.toString() ;
    }


    /**
     * Generates the SyntaxDescription for a Syntax as defined by the syntax:
     * 1.3.6.1.4.1.1466.115.121.1.54.  Only the right hand side 
     * of the description starting at the openning parenthesis is generated: 
     * that is 'SyntaxDescription = ' is not generated.
     * <pre>
     * SyntaxDescription = "(" whsp
     *     numericoid whsp
     *     [ "DESC" qdstring ]
     * whsp ")"
     * </pre>
     * @param a_syntax the Syntax to generate a description for
     * @return the description in the SyntaxDescription syntax
     */
    public static String getDescription( Syntax a_syntax )
    {
        StringBuffer l_buf = new StringBuffer( "( " ) ;
        l_buf.append( a_syntax.getOid() ) ;
        l_buf.append( '\n' ) ;
        
        if ( a_syntax.getDescription() != null )
        {    
            l_buf.append( "DESC " ) ;
            l_buf.append( a_syntax.getDescription() ) ;
            l_buf.append( '\n' ) ;
        }
        
        l_buf.append( " )" ) ;
        return l_buf.toString() ;
    }
}
