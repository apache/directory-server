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


import java.util.Comparator;
import javax.naming.NamingException;

import org.apache.directory.shared.ldap.NotImplementedException;
import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.junit.Test;
import static org.junit.Assert.assertEquals;


/**
 * The unit tests for methods on SchemaUtils.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class SchemaUtilsTest
{
    public static Syntax[] getSyntaxes()
    {
        SyntaxImpl[] syntaxes = new SyntaxImpl[3];
        syntaxes[0] = new SyntaxImpl( "1.3.6.1.4.1.1466.115.121.1.12", "DN syntax", true );
        syntaxes[1] = new SyntaxImpl( SchemaConstants.DIRECTORY_STRING_SYNTAX, "Directory String syntax", true );
        syntaxes[2] = new SyntaxImpl( "1.3.6.1.4.1.1466.115.121.1.58", "Substring assertion syntax", true );
        return syntaxes;
    }


    public static MatchingRule[] getMatchingRules()
    {
        MatchingRuleImpl[] mrs = new MatchingRuleImpl[3];
        mrs[0] = new MatchingRuleImpl( "2.5.13.2", getSyntaxes()[1] );
        mrs[0].setNames( new String[]
            { "caseIgnoreMatch" } );
        mrs[0].setDescription( "Ignores case in strings" );

        mrs[1] = new MatchingRuleImpl( "2.5.13.4", getSyntaxes()[2] );
        mrs[1].setNames( new String[]
            { "caseIgnoreSubstringsMatch" } );
        mrs[1].setDescription( "Ignores case in substrings" );

        mrs[2] = new MatchingRuleImpl( "2.5.13.1", getSyntaxes()[0] );
        mrs[2].setNames( new String[]
            { "distinguishedNameMatch" } );
        mrs[2].setDescription( "distinguishedNameMatch" );

        return mrs;
    }


    public AttributeType[] getAttributeTypes()
    {
        AttributeTypeImpl[] ats = new AttributeTypeImpl[5];

        ats[0] = new AttributeTypeImpl( "2.5.4.41" );
        ats[0].setNames( new String[]
            { "name" } );
        ats[0].syntax = getSyntaxes()[1];
        ats[0].setLength( 32768 );
        ats[0].equality = getMatchingRules()[0];
        ats[0].substr = getMatchingRules()[1];

        // ( 2.5.4.3 NAME 'cn' SUP name )
        ats[1] = new AttributeTypeImpl( "2.5.4.3" );
        ats[1].setNames( new String[]
            { "cn", "commonName" } );

        ats[2] = new AttributeTypeImpl( "2.5.4.41" );
        ats[2].setNames( new String[]
            { "name" } );
        ats[2].syntax = getSyntaxes()[1];
        ats[2].setLength( 32768 );
        ats[2].equality = getMatchingRules()[0];
        ats[2].substr = getMatchingRules()[1];

        ats[3] = new AttributeTypeImpl( "2.5.4.41" );
        ats[3].setNames( new String[]
            { "name" } );
        ats[3].syntax = getSyntaxes()[1];
        ats[3].setLength( 32768 );
        ats[3].equality = getMatchingRules()[0];
        ats[3].substr = getMatchingRules()[1];

        ats[4] = new AttributeTypeImpl( "2.5.4.41" );
        ats[4].setNames( new String[]
            { "name" } );
        ats[4].syntax = getSyntaxes()[1];
        ats[4].setLength( 32768 );
        ats[4].equality = getMatchingRules()[0];
        ats[4].substr = getMatchingRules()[1];

        return ats;
    }


    public ObjectClass[] getObjectClasses()
    {
        /*
         * objectclass ( 2.5.6.2 NAME 'country' DESC 'RFC2256: a country' SUP
         * top STRUCTURAL MUST c MAY ( searchGuide $ description ) )
         */

        DefaultObjectClass[] ocs = new DefaultObjectClass[2];
        ocs[0] = new DefaultObjectClass( "1.1" );
        ocs[0].setNames( new String[]
            { "oc1" } );
        ocs[0].setDescription( "object class #1" );
        ocs[0].setObsolete( false );
        ocs[0].setType( ObjectClassTypeEnum.ABSTRACT );

        /*
         * objectclass ( 2.5.6.6 NAME 'person' DESC 'RFC2256: a person' SUP top
         * STRUCTURAL MUST ( sn $ cn ) MAY ( userPassword $ telephoneNumber $
         * seeAlso $ description ) )
         */

        return ocs;
    }


    /**
     * Tests rendering operations on qdescrs render method. Both overloaded
     * operations {@link SchemaUtils#render(StringBuffer, String[])} and
     * {@link SchemaUtils#render(String[])} are tested here.
     */
    @Test
    public void testRenderQdescrs()
    {
        assertEquals( "", SchemaUtils.render( ( String[] ) null ).toString() );
        assertEquals( "", SchemaUtils.render( new String[]
            {} ).toString() );
        assertEquals( "'name1'", SchemaUtils.render( new String[]
            { "name1" } ).toString() );
        assertEquals( "( 'name1' 'name2' )", SchemaUtils.render( new String[]
            { "name1", "name2" } ).toString() );
        assertEquals( "( 'name1' 'name2' 'name3' )", SchemaUtils.render( new String[]
            { "name1", "name2", "name3" } ).toString() );

        StringBuffer buf = new StringBuffer();
        assertEquals( "", SchemaUtils.render( buf, ( String[] ) null ).toString() );

        assertEquals( "", SchemaUtils.render( new String[]
            {} ).toString() );

        assertEquals( "'name1'", SchemaUtils.render( new String[]
            { "name1" } ).toString() );

        assertEquals( "( 'name1' 'name2' )", SchemaUtils.render( new String[]
            { "name1", "name2" } ).toString() );

        assertEquals( "( 'name1' 'name2' 'name3' )", SchemaUtils.render( new String[]
            { "name1", "name2", "name3" } ).toString() );
    }


    static class SyntaxImpl extends AbstractSyntax
    {
        private static final long serialVersionUID = 1L;
        
        protected SyntaxImpl(String oid)
        {
            super( oid );
        }


        protected SyntaxImpl(String oid, boolean isHumanReadable)
        {
            super( oid, isHumanReadable );
        }


        protected SyntaxImpl(String oid, String description)
        {
            super( oid, description );
        }


        protected SyntaxImpl(String oid, String description, boolean isHumanReadable)
        {
            super( oid, description, isHumanReadable );
        }


        public SyntaxChecker getSyntaxChecker() throws NamingException
        {
            return null;
        }
    }

    static class AttributeTypeImpl extends AbstractAttributeType
    {
        private static final long serialVersionUID = 1L;

        Syntax syntax;

        AttributeType sup;

        MatchingRule equality;

        MatchingRule ordering;

        MatchingRule substr;


        public AttributeTypeImpl(String oid)
        {
            super( oid );
        }


        public AttributeType getSuperior() throws NamingException
        {
            return sup;
        }


        public Syntax getSyntax() throws NamingException
        {
            return syntax;
        }


        public MatchingRule getEquality() throws NamingException
        {
            return equality;
        }


        public MatchingRule getOrdering() throws NamingException
        {
            return ordering;
        }


        public MatchingRule getSubstr() throws NamingException
        {
            return substr;
        }


        public boolean isAncestorOf( AttributeType attributeType ) throws NamingException
        {
            return false;
        }


        public boolean isDescendantOf( AttributeType attributeType ) throws NamingException
        {
            return false;
        }
    }

    static class MatchingRuleImpl extends AbstractMatchingRule
    {
        private static final long serialVersionUID = 1L;

        Syntax syntax;


        protected MatchingRuleImpl(String oid, Syntax syntax)
        {
            super( oid );
            this.syntax = syntax;
        }


        public Syntax getSyntax() throws NamingException
        {
            return syntax;
        }


        public Comparator<String> getComparator() throws NamingException
        {
            throw new NotImplementedException(
                "getComparator in org.apache.ldap.common.schema.SchemaUtilsTest.MatchingRuleImpl not implemented!" );
        }


        public Normalizer getNormalizer() throws NamingException
        {
            throw new NotImplementedException(
                "getNormalizer in org.apache.ldap.common.schema.SchemaUtilsTest.MatchingRuleImpl not implemented!" );
        }
    }
}
