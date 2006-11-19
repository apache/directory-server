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
 * Various utility methods for schema functions and objects.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class SchemaUtils
{
    // ------------------------------------------------------------------------
    // qdescrs rendering operations
    // ------------------------------------------------------------------------

    /**
     * Renders qdescrs into an existing buffer.
     * 
     * @param buf
     *            the string buffer to render the quoted description strs into
     * @param qdescrs
     *            the quoted description strings to render
     * @return the same string buffer that was given for call chaining
     */
    public static StringBuffer render( StringBuffer buf, String[] qdescrs )
    {
        if ( qdescrs == null || qdescrs.length == 0 )
        {
            return buf;
        }
        else if ( qdescrs.length == 1 )
        {
            buf.append( "'" ).append( qdescrs[0] ).append( "'" );
        }
        else
        {
            buf.append( "( " );
            for ( int ii = 0; ii < qdescrs.length; ii++ )
            {
                buf.append( "'" ).append( qdescrs[ii] ).append( "' " );
            }
            buf.append( ")" );
        }

        return buf;
    }


    /**
     * Renders qdescrs into a new buffer.
     * 
     * @param qdescrs
     *            the quoted description strings to render
     * @return the string buffer the qdescrs are rendered into
     */
    public static StringBuffer render( String[] qdescrs )
    {
        StringBuffer buf = new StringBuffer();
        return render( buf, qdescrs );
    }


    // ------------------------------------------------------------------------
    // objectClass list rendering operations
    // ------------------------------------------------------------------------

    /**
     * Renders a list of object classes for things like a list of superior
     * objectClasses using the ( oid $ oid ) format.
     * 
     * @param ocs
     *            the objectClasses to list
     * @return a buffer which contains the rendered list
     */
    public static StringBuffer render( ObjectClass[] ocs )
    {
        StringBuffer buf = new StringBuffer();
        return render( buf, ocs );
    }


    /**
     * Renders a list of object classes for things like a list of superior
     * objectClasses using the ( oid $ oid ) format into an existing buffer.
     * 
     * @param buf
     *            the string buffer to render the list of objectClasses into
     * @param ocs
     *            the objectClasses to list
     * @return a buffer which contains the rendered list
     */
    public static StringBuffer render( StringBuffer buf, ObjectClass[] ocs )
    {
        if ( ocs == null || ocs.length == 0 )
        {
            return buf;
        }
        else if ( ocs.length == 1 )
        {
            buf.append( ocs[0].getName() );
        }
        else
        {
            buf.append( "( " );
            for ( int ii = 0; ii < ocs.length; ii++ )
            {
                if ( ii + 1 < ocs.length )
                {
                    buf.append( ocs[ii].getName() ).append( " $ " );
                }
                else
                {
                    buf.append( ocs[ii].getName() );
                }
            }
            buf.append( " )" );
        }

        return buf;
    }


    // ------------------------------------------------------------------------
    // attributeType list rendering operations
    // ------------------------------------------------------------------------

    /**
     * Renders a list of attributeTypes for things like the must or may list of
     * objectClasses using the ( oid $ oid ) format.
     * 
     * @param ats
     *            the attributeTypes to list
     * @return a buffer which contains the rendered list
     */
    public static StringBuffer render( AttributeType[] ats )
    {
        StringBuffer buf = new StringBuffer();
        return render( buf, ats );
    }


    /**
     * Renders a list of attributeTypes for things like the must or may list of
     * objectClasses using the ( oid $ oid ) format into an existing buffer.
     * 
     * @param buf
     *            the string buffer to render the list of attributeTypes into
     * @param ats
     *            the attributeTypes to list
     * @return a buffer which contains the rendered list
     */
    public static StringBuffer render( StringBuffer buf, AttributeType[] ats )
    {
        if ( ats == null || ats.length == 0 )
        {
            return buf;
        }
        else if ( ats.length == 1 )
        {
            buf.append( ats[0].getName() );
        }
        else
        {
            buf.append( "( " );
            for ( int ii = 0; ii < ats.length; ii++ )
            {
                if ( ii + 1 < ats.length )
                {
                    buf.append( ats[ii].getName() ).append( " $ " );
                }
                else
                {
                    buf.append( ats[ii].getName() );
                }
            }
            buf.append( " )" );
        }

        return buf;
    }


    // ------------------------------------------------------------------------
    // schema object rendering operations
    // ------------------------------------------------------------------------

    /**
     * Renders an objectClass into a new StringBuffer according to the Object
     * Class Description Syntax 1.3.6.1.4.1.1466.115.121.1.37. The syntax is
     * described in detail within section 4.1.1. of LDAPBIS [<a
     * href="http://ietf.org/internet-drafts/draft-ietf-ldapbis-models-12.txt">MODELS</a>]
     * which is replicated here for convenience:
     * 
     * <pre>
     *  4.1.1. Object Class Definitions
     * 
     *   Object Class definitions are written according to the ABNF:
     * 
     *     ObjectClassDescription = LPAREN WSP
     *         numericoid                 ; object identifier
     *         [ SP &quot;NAME&quot; SP qdescrs ]   ; short names (descriptors)
     *         [ SP &quot;DESC&quot; SP qdstring ]  ; description
     *         [ SP &quot;OBSOLETE&quot; ]          ; not active
     *         [ SP &quot;SUP&quot; SP oids ]       ; superior object classes
     *         [ SP kind ]                ; kind of class
     *         [ SP &quot;MUST&quot; SP oids ]      ; attribute types
     *         [ SP &quot;MAY&quot; SP oids ]       ; attribute types
     *         extensions WSP RPAREN
     * 
     *     kind = &quot;ABSTRACT&quot; / &quot;STRUCTURAL&quot; / &quot;AUXILIARY&quot;
     * 
     *   where:
     *     &lt;numericoid&gt; is object identifier assigned to this object class;
     *     NAME &lt;qdescrs&gt; are short names (descriptors) identifying this object
     *         class;
     *     DESC &lt;qdstring&gt; is a short descriptive string;
     *     OBSOLETE indicates this object class is not active;
     *     SUP &lt;oids&gt; specifies the direct superclasses of this object class;
     *     the kind of object class is indicated by one of ABSTRACT,
     *         STRUCTURAL, or AUXILIARY, default is STRUCTURAL;
     *     MUST and MAY specify the sets of required and allowed attribute
     *         types, respectively; and
     *     &lt;extensions&gt; describe extensions.
     * </pre>
     * @param oc the objectClass to render the description of
     * @return the buffer containing the objectClass description
     * @throws NamingException if there are any problems accessing objectClass
     * information
     */
    public static StringBuffer render( ObjectClass oc ) throws NamingException
    {
        StringBuffer buf = new StringBuffer();
        buf.append( "( " ).append( oc.getOid() ).append( " NAME " );
        render( buf, oc.getNames() ).append( " " );

        if ( oc.getDescription() != null )
        {
            buf.append( "DESC " ).append( "'" ).append( oc.getDescription() ).append( "' " );
        }

        if ( oc.isObsolete() )
        {
            buf.append( " OBSOLETE " );
        }

        if ( oc.getSuperClasses() != null && oc.getSuperClasses().length > 0 )
        {
            buf.append( "SUP " );
            render( buf, oc.getSuperClasses() );
        }

        if ( oc.getType() != null )
        {
            buf.append( " " ).append( oc.getType().getName() );
        }

        if ( oc.getMustList() != null && oc.getMustList().length > 0 )
        {
            buf.append( " MUST " );
            render( buf, oc.getMustList() );
        }

        if ( oc.getMayList() != null && oc.getMayList().length > 0 )
        {
            buf.append( " MAY " );
            render( buf, oc.getMayList() );
        }

        // @todo extensions are not presently supported and skipped
        // the extensions would go here before closing off the description

        buf.append( " )" );

        return buf;
    }


    /**
     * Renders an attributeType into a new StringBuffer according to the
     * Attribute Type Description Syntax 1.3.6.1.4.1.1466.115.121.1.3. The
     * syntax is described in detail within section 4.1.2. of LDAPBIS [<a
     * href="http://ietf.org/internet-drafts/draft-ietf-ldapbis-models-12.txt">MODELS</a>]
     * which is replicated here for convenience:
     * 
     * <pre>
     *  4.1.2. Attribute Types
     * 
     *   Attribute Type definitions are written according to the ABNF:
     * 
     *   AttributeTypeDescription = LPAREN WSP
     *         numericoid                    ; object identifier
     *         [ SP &quot;NAME&quot; SP qdescrs ]      ; short names (descriptors)
     *         [ SP &quot;DESC&quot; SP qdstring ]     ; description
     *         [ SP &quot;OBSOLETE&quot; ]             ; not active
     *         [ SP &quot;SUP&quot; SP oid ]           ; supertype
     *         [ SP &quot;EQUALITY&quot; SP oid ]      ; equality matching rule
     *         [ SP &quot;ORDERING&quot; SP oid ]      ; ordering matching rule
     *         [ SP &quot;SUBSTR&quot; SP oid ]        ; substrings matching rule
     *         [ SP &quot;SYNTAX&quot; SP noidlen ]    ; value syntax
     *         [ SP &quot;SINGLE-VALUE&quot; ]         ; single-value
     *         [ SP &quot;COLLECTIVE&quot; ]           ; collective
     *         [ SP &quot;NO-USER-MODIFICATION&quot; ] ; not user modifiable
     *         [ SP &quot;USAGE&quot; SP usage ]       ; usage
     *         extensions WSP RPAREN         ; extensions
     * 
     *     usage = &quot;userApplications&quot;     /  ; user
     *             &quot;directoryOperation&quot;   /  ; directory operational
     *             &quot;distributedOperation&quot; /  ; DSA-shared operational
     *             &quot;dSAOperation&quot;            ; DSA-specific operational
     * 
     *   where:
     *     &lt;numericoid&gt; is object identifier assigned to this attribute type;
     *     NAME &lt;qdescrs&gt; are short names (descriptors) identifying this
     *         attribute type;
     *     DESC &lt;qdstring&gt; is a short descriptive string;
     *     OBSOLETE indicates this attribute type is not active;
     *     SUP oid specifies the direct supertype of this type;
     *     EQUALITY, ORDERING, SUBSTR provide the oid of the equality,
     *         ordering, and substrings matching rules, respectively;
     *     SYNTAX identifies value syntax by object identifier and may suggest
     *         a minimum upper bound;
     *     SINGLE-VALUE indicates attributes of this type are restricted to a
     *         single value;
     *     COLLECTIVE indicates this attribute type is collective
     *         [X.501][RFC3671];
     *     NO-USER-MODIFICATION indicates this attribute type is not user
     *         modifiable;
     *     USAGE indicates the application of this attribute type; and
     *     &lt;extensions&gt; describe extensions.
     * </pre>
     * @param at the AttributeType to render the description for
     * @return the StringBuffer containing the rendered attributeType description
     * @throws NamingException if there are problems accessing the objects
     * associated with the attribute type.
     */
    public static StringBuffer render( AttributeType at ) throws NamingException
    {
        StringBuffer buf = new StringBuffer();
        buf.append( "( " ).append( at.getOid() ).append( " NAME " );
        render( buf, at.getNames() ).append( " " );

        if ( at.getDescription() != null )
        {
            buf.append( "DESC " ).append( "'" ).append( at.getDescription() ).append( "' " );
        }

        if ( at.isObsolete() )
        {
            buf.append( " OBSOLETE" );
        }

        if ( at.getSuperior() != null )
        {
            buf.append( " SUP " ).append( at.getSuperior().getName() );
        }

        if ( at.getEquality() != null )
        {
            buf.append( " EQUALITY " ).append( at.getEquality().getName() );
        }

        if ( at.getOrdering() != null )
        {
            buf.append( " ORDERING " ).append( at.getOrdering().getName() );
        }

        if ( at.getSubstr() != null )
        {
            buf.append( " SUBSTR " ).append( at.getSubstr().getName() );
        }

        if ( at.getSyntax() != null )
        {
            buf.append( " SYNTAX " ).append( at.getSyntax().getOid() );

            if ( at.getLength() > 0 )
            {
                buf.append( "{" ).append( at.getLength() ).append( "}" );
            }
        }

        if ( at.isSingleValue() )
        {
            buf.append( " SINGLE-VALUE" );
        }

        if ( at.isCollective() )
        {
            buf.append( " COLLECTIVE" );
        }

        if ( !at.isCanUserModify() )
        {
            buf.append( " NO-USER-MODIFICATION" );
        }

        if ( at.getUsage() != null )
        {
            buf.append( " USAGE " ).append( at.getUsage() );
        }

        // @todo extensions are not presently supported and skipped
        // the extensions would go here before closing off the description

        buf.append( " )" );

        return buf;
    }


    /**
     * Renders an matchingRule into a new StringBuffer according to the
     * MatchingRule Description Syntax 1.3.6.1.4.1.1466.115.121.1.30. The syntax
     * is described in detail within section 4.1.3. of LDAPBIS [<a
     * href="http://ietf.org/internet-drafts/draft-ietf-ldapbis-models-12.txt">MODELS</a>]
     * which is replicated here for convenience:
     * 
     * <pre>
     *  4.1.3. Matching Rules
     * 
     *   Matching rules are used in performance of attribute value assertions,
     *   such as in performance of a Compare operation.  They are also used in
     *   evaluation of a Search filters, in determining which individual values
     *   are be added or deleted during performance of a Modify operation, and
     *   used in comparison of distinguished names.
     * 
     *   Each matching rule is identified by an object identifier (OID) and,
     *   optionally, one or more short names (descriptors).
     * 
     *   Matching rule definitions are written according to the ABNF:
     * 
     *   MatchingRuleDescription = LPAREN WSP
     *        numericoid                 ; object identifier
     *         [ SP &quot;NAME&quot; SP qdescrs ]   ; short names (descriptors)
     *         [ SP &quot;DESC&quot; SP qdstring ]  ; description
     *         [ SP &quot;OBSOLETE&quot; ]          ; not active
     *         SP &quot;SYNTAX&quot; SP numericoid  ; assertion syntax
     *         extensions WSP RPAREN      ; extensions
     * 
     *   where:
     *     &lt;numericoid&gt; is object identifier assigned to this matching rule;
     *     NAME &lt;qdescrs&gt; are short names (descriptors) identifying this
     *         matching rule;
     *     DESC &lt;qdstring&gt; is a short descriptive string;
     *     OBSOLETE indicates this matching rule is not active;
     *     SYNTAX identifies the assertion syntax (the syntax of the assertion
     *         value) by object identifier; and
     *     &lt;extensions&gt; describe extensions.
     * </pre>
     * @param mr the MatchingRule to render the description for
     * @return the StringBuffer containing the rendered matchingRule description
     * @throws NamingException if there are problems accessing the objects
     * associated with the MatchingRule.
     */
    public static StringBuffer render( MatchingRule mr ) throws NamingException
    {
        StringBuffer buf = new StringBuffer();
        buf.append( "( " ).append( mr.getOid() ).append( " NAME " );
        render( buf, mr.getNames() ).append( " " );

        if ( mr.getDescription() != null )
        {
            buf.append( "DESC " ).append( "'" ).append( mr.getDescription() ).append( "' " );
        }

        if ( mr.isObsolete() )
        {
            buf.append( " OBSOLETE" );
        }

        if ( mr.getSyntax() != null )
        {
            buf.append( " SYNTAX " ).append( mr.getSyntax().getOid() );
        }

        // @todo extensions are not presently supported and skipped
        // the extensions would go here before closing off the description

        buf.append( " )" );

        return buf;
    }


    /**
     * Renders a Syntax into a new StringBuffer according to the LDAP Syntax
     * Description Syntax 1.3.6.1.4.1.1466.115.121.1.54. The syntax is described
     * in detail within section 4.1.5. of LDAPBIS [<a
     * href="http://ietf.org/internet-drafts/draft-ietf-ldapbis-models-12.txt">MODELS</a>]
     * which is replicated here for convenience:
     * 
     * <pre>
     *  LDAP syntax definitions are written according to the ABNF:
     * 
     *   SyntaxDescription = LPAREN WSP
     *       numericoid                 ; object identifier
     *       [ SP &quot;DESC&quot; SP qdstring ]  ; description
     *       extensions WSP RPAREN      ; extensions
     * 
     *  where:
     *   &lt;numericoid&gt; is the object identifier assigned to this LDAP syntax;
     *   DESC &lt;qdstring&gt; is a short descriptive string; and
     *   &lt;extensions&gt; describe extensions.
     * </pre>
     * @param syntax the Syntax to render the description for
     * @return the StringBuffer containing the rendered syntax description
     */
    public static StringBuffer render( Syntax syntax )
    {
        StringBuffer buf = new StringBuffer();
        buf.append( "( " ).append( syntax.getOid() ).append( " NAME " );
        render( buf, syntax.getNames() ).append( " " );

        if ( syntax.getDescription() != null )
        {
            buf.append( "DESC " ).append( "'" ).append( syntax.getDescription() ).append( "' " );
        }

        // @todo extensions are not presently supported and skipped
        // the extensions would go here before closing off the description

        buf.append( " )" );

        return buf;
    }


    /**
     * NOT FULLY IMPLEMENTED!
     */
    public static StringBuffer render( MatchingRuleUse mru )
    {
        StringBuffer buf = new StringBuffer();
        buf.append( "( " ).append( mru.getOid() ).append( " NAME " );
        render( buf, mru.getNames() ).append( " " );

        if ( mru.getDescription() != null )
        {
            buf.append( "DESC " ).append( "'" ).append( mru.getDescription() ).append( "' " );
        }

        // @todo extensions are not presently supported and skipped
        // the extensions would go here before closing off the description

        buf.append( " )" );

        return buf;
    }


    /**
     * NOT FULLY IMPLEMENTED!
     */
    public static StringBuffer render( DITContentRule dcr )
    {
        StringBuffer buf = new StringBuffer();
        buf.append( "( " ).append( dcr.getOid() ).append( " NAME " );
        render( buf, dcr.getNames() ).append( " " );

        if ( dcr.getDescription() != null )
        {
            buf.append( "DESC " ).append( "'" ).append( dcr.getDescription() ).append( "' " );
        }

        // @todo extensions are not presently supported and skipped
        // the extensions would go here before closing off the description

        buf.append( " )" );

        return buf;
    }


    /**
     * NOT FULLY IMPLEMENTED!
     */
    public static StringBuffer render( DITStructureRule dsr )
    {
        StringBuffer buf = new StringBuffer();
        buf.append( "( " ).append( dsr.getOid() ).append( " NAME " );
        render( buf, dsr.getNames() ).append( " " );

        if ( dsr.getDescription() != null )
        {
            buf.append( "DESC " ).append( "'" ).append( dsr.getDescription() ).append( "' " );
        }

        // @todo extensions are not presently supported and skipped
        // the extensions would go here before closing off the description

        buf.append( " )" );

        return buf;
    }


    /**
     * NOT FULLY IMPLEMENTED!
     */
    public static StringBuffer render( NameForm nf )
    {
        StringBuffer buf = new StringBuffer();
        buf.append( "( " ).append( nf.getOid() ).append( " NAME " );
        render( buf, nf.getNames() ).append( " " );

        if ( nf.getDescription() != null )
        {
            buf.append( "DESC " ).append( "'" ).append( nf.getDescription() ).append( "' " );
        }

        // @todo extensions are not presently supported and skipped
        // the extensions would go here before closing off the description

        buf.append( " )" );

        return buf;
    }
}
