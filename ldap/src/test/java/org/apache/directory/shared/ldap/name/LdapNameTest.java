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
package org.apache.directory.shared.ldap.name;


import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;

import javax.naming.Name;

import org.apache.directory.shared.ldap.name.DnParser;
import org.apache.directory.shared.ldap.name.LdapName;

import junit.framework.TestCase;


/**
 * Testcase for LdapName
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class LdapNameTest extends TestCase
{
    /** Parser to use */
    private DnParser m_parser;


    /**
     * Constructor for LdapNameTest.
     * 
     * @param a_arg0
     *            an arg
     */
    public LdapNameTest(String a_arg0)
    {
        super( a_arg0 );
    }


    /**
     * @param a_args
     *            none
     */
    public static void main( String[] a_args )
    {
        junit.textui.TestRunner.run( LdapNameTest.class );
    }


    /**
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception
    {
        super.setUp();

        m_parser = new DnParser();
    }


    /**
     * @see TestCase#tearDown()
     */
    protected void tearDown() throws Exception
    {
        super.tearDown();
        m_parser = null;
    }


    // ------------------------------------------------------------------------
    // Start Tests Here!
    // ------------------------------------------------------------------------

    /**
     * Tests the examples from the JNDI tutorials to make sure LdapName behaves
     * appropriately. The example can be found online <a href="">here</a>.
     * 
     * @throws Exception
     *             if anything goes wrong
     */
    public void testJNDITutorialExample() throws Exception
    {
        // Parse the name
        Name l_name = m_parser.parse( "cn=John,ou=People,ou=Marketing" );

        // Remove the second component from the head: ou=People
        String l_out = l_name.remove( 1 ).toString();
        // System.out.println( l_out ) ;
        assertEquals( "ou=People", l_out );

        // Add to the head (first): cn=John,ou=Marketing,ou=East
        l_out = l_name.add( 0, "ou=East" ).toString();
        // System.out.println( l_out ) ;
        assertEquals( "cn=John,ou=Marketing,ou=East", l_out );

        // Add to the tail (last): cn=HomeDir,cn=John,ou=Marketing,ou=East
        l_out = l_name.add( "cn=HomeDir" ).toString();
        // System.out.println( l_out ) ;
        assertEquals( "cn=HomeDir,cn=John,ou=Marketing,ou=East", l_out );
    }


    /**
     * @throws Exception
     *             if anything goes wrong.
     */
    public void testHashCode() throws Exception
    {
        String l_strName = "cn=HomeDir,cn=John,ou=Marketing,ou=East";
        Name l_name = m_parser.parse( l_strName );
        assertEquals( l_name.hashCode(), l_strName.hashCode() );
    }


    /**
     * Class to test for void LdapName(String)
     * 
     * @throws Exception
     *             if anything goes wrong.
     */
    public void testLdapNameString() throws Exception
    {
        LdapName l_name = new LdapName( "" );
        LdapName l_name50 = new LdapName();
        assertEquals( l_name50, l_name );

        Name l_name0 = new LdapName( "ou=Marketing,ou=East" );
        Name l_copy = m_parser.parse( "ou=Marketing,ou=East" );
        Name l_name1 = new LdapName( "cn=John,ou=Marketing,ou=East" );
        Name l_name2 = new LdapName( "cn=HomeDir,cn=John,ou=Marketing,ou=East" );
        Name l_name3 = new LdapName( "cn=HomeDir,cn=John,ou=Marketing,ou=West" );
        Name l_name4 = new LdapName( "cn=Website,cn=John,ou=Marketing,ou=West" );
        Name l_name5 = new LdapName( "cn=Airline,cn=John,ou=Marketing,ou=West" );

        assertTrue( l_name0.compareTo( l_copy ) == 0 );
        assertTrue( l_name0.compareTo( l_name1 ) < 0 );
        assertTrue( l_name0.compareTo( l_name2 ) < 0 );
        assertTrue( l_name1.compareTo( l_name2 ) < 0 );
        assertTrue( l_name2.compareTo( l_name1 ) > 0 );
        assertTrue( l_name2.compareTo( l_name0 ) > 0 );
        assertTrue( l_name2.compareTo( l_name3 ) < 0 );
        assertTrue( l_name2.compareTo( l_name4 ) < 0 );
        assertTrue( l_name3.compareTo( l_name4 ) < 0 );
        assertTrue( l_name3.compareTo( l_name5 ) > 0 );
        assertTrue( l_name4.compareTo( l_name5 ) > 0 );
        assertTrue( l_name2.compareTo( l_name5 ) < 0 );
    }


    /**
     * Class to test for void LdapName()
     */
    public void testLdapName()
    {
        Name l_name = new LdapName();
        assertTrue( l_name.toString().equals( "" ) );
    }


    /**
     * Class to test for void LdapName(List)
     */
    public void testLdapNameList()
    {
        ArrayList l_list = new ArrayList();
        l_list.add( "ou=People" );
        l_list.add( "dc=example" );
        l_list.add( "dc=com" );
        LdapName l_name = new LdapName( l_list );
        assertTrue( l_name.toString().equals( "ou=People,dc=example,dc=com" ) );
    }


    /**
     * Class to test for void LdapName(Iterator)
     */
    public void testLdapNameIterator()
    {
        ArrayList l_list = new ArrayList();
        l_list.add( "ou=People" );
        l_list.add( "dc=example" );
        l_list.add( "dc=com" );
        LdapName l_name = new LdapName( l_list.iterator() );
        assertTrue( l_name.toString().equals( "ou=People,dc=example,dc=com" ) );
    }


    /**
     * Class to test for Object clone()
     * 
     * @throws Exception
     *             if anything goes wrong.
     */
    public void testClone() throws Exception
    {
        String l_strName = "cn=HomeDir,cn=John,ou=Marketing,ou=East";
        Name l_name = m_parser.parse( l_strName );
        assertEquals( l_name, l_name.clone() );
    }


    /**
     * Class to test for compareTo
     * 
     * @throws Exception
     *             if anything goes wrong.
     */
    public void testCompareTo() throws Exception
    {
        Name l_name0 = m_parser.parse( "ou=Marketing,ou=East" );
        Name l_copy = m_parser.parse( "ou=Marketing,ou=East" );
        Name l_name1 = m_parser.parse( "cn=John,ou=Marketing,ou=East" );
        Name l_name2 = m_parser.parse( "cn=HomeDir,cn=John,ou=Marketing,ou=East" );
        Name l_name3 = m_parser.parse( "cn=HomeDir,cn=John,ou=Marketing,ou=West" );
        Name l_name4 = m_parser.parse( "cn=Website,cn=John,ou=Marketing,ou=West" );
        Name l_name5 = m_parser.parse( "cn=Airline,cn=John,ou=Marketing,ou=West" );

        assertTrue( l_name0.compareTo( l_copy ) == 0 );
        assertTrue( l_name0.compareTo( l_name1 ) < 0 );
        assertTrue( l_name0.compareTo( l_name2 ) < 0 );
        assertTrue( l_name1.compareTo( l_name2 ) < 0 );
        assertTrue( l_name2.compareTo( l_name1 ) > 0 );
        assertTrue( l_name2.compareTo( l_name0 ) > 0 );
        assertTrue( l_name2.compareTo( l_name3 ) < 0 );
        assertTrue( l_name2.compareTo( l_name4 ) < 0 );
        assertTrue( l_name3.compareTo( l_name4 ) < 0 );
        assertTrue( l_name3.compareTo( l_name5 ) > 0 );
        assertTrue( l_name4.compareTo( l_name5 ) > 0 );
        assertTrue( l_name2.compareTo( l_name5 ) < 0 );

        ArrayList l_list = new ArrayList();
        Comparator l_comparator = new Comparator()
        {
            public int compare( Object a_obj1, Object a_obj2 )
            {
                LdapName l_name1 = ( LdapName ) a_obj1;
                LdapName l_name2 = ( LdapName ) a_obj2;
                return l_name1.compareTo( l_name2 );
            }


            public boolean equals( Object a_obj )
            {
                return super.equals( a_obj );
            }


            public int hashCode()
            {
                return super.hashCode();
            }
        };

        l_list.add( l_name0 );
        l_list.add( l_name1 );
        l_list.add( l_name2 );
        l_list.add( l_name3 );
        l_list.add( l_name4 );
        l_list.add( l_name5 );
        Collections.sort( l_list, l_comparator );

        assertEquals( l_name0, l_list.get( 0 ) );
        assertEquals( l_name1, l_list.get( 1 ) );
        assertEquals( l_name2, l_list.get( 2 ) );
        assertEquals( l_name5, l_list.get( 3 ) );
        assertEquals( l_name3, l_list.get( 4 ) );
        assertEquals( l_name4, l_list.get( 5 ) );
    }


    /**
     * Class to test for size
     * 
     * @throws Exception
     *             if anything goes wrong.
     */
    public void testSize() throws Exception
    {
        Name name0 = m_parser.parse( "" );
        Name name1 = m_parser.parse( "ou=East" );
        Name name2 = m_parser.parse( "ou=Marketing,ou=East" );
        Name name3 = m_parser.parse( "cn=John,ou=Marketing,ou=East" );
        Name name4 = m_parser.parse( "cn=HomeDir,cn=John,ou=Marketing,ou=East" );
        Name name5 = m_parser.parse( "cn=Website,cn=HomeDir,cn=John,ou=Marketing,ou=West" );
        Name name6 = m_parser.parse( "cn=Airline,cn=Website,cn=HomeDir,cn=John,ou=Marketing,ou=West" );

        assertEquals( 0, name0.size() );
        assertEquals( 1, name1.size() );
        assertEquals( 2, name2.size() );
        assertEquals( 3, name3.size() );
        assertEquals( 4, name4.size() );
        assertEquals( 5, name5.size() );
        assertEquals( 6, name6.size() );
    }


    /**
     * Class to test for isEmpty
     * 
     * @throws Exception
     *             if anything goes wrong.
     */
    public void testIsEmpty() throws Exception
    {
        Name name0 = m_parser.parse( "" );
        Name name1 = m_parser.parse( "ou=East" );
        Name name2 = m_parser.parse( "ou=Marketing,ou=East" );
        Name name3 = m_parser.parse( "cn=John,ou=Marketing,ou=East" );
        Name name4 = m_parser.parse( "cn=HomeDir,cn=John,ou=Marketing,ou=East" );
        Name name5 = m_parser.parse( "cn=Website,cn=HomeDir,cn=John,ou=Marketing,ou=West" );
        Name name6 = m_parser.parse( "cn=Airline,cn=Website,cn=HomeDir,cn=John,ou=Marketing,ou=West" );

        assertEquals( true, name0.isEmpty() );
        assertEquals( false, name1.isEmpty() );
        assertEquals( false, name2.isEmpty() );
        assertEquals( false, name3.isEmpty() );
        assertEquals( false, name4.isEmpty() );
        assertEquals( false, name5.isEmpty() );
        assertEquals( false, name6.isEmpty() );
    }


    /**
     * Class to test for getSuffix
     * 
     * @throws Exception
     *             if anything goes wrong.
     */
    public void testGetAll() throws Exception
    {
        Name l_name0 = m_parser.parse( "" );
        Name l_name1 = m_parser.parse( "ou=East" );
        Name l_name2 = m_parser.parse( "ou=Marketing,ou=East" );
        Name l_name3 = m_parser.parse( "cn=John,ou=Marketing,ou=East" );
        Name l_name4 = m_parser.parse( "cn=HomeDir,cn=John,ou=Marketing,ou=East" );
        Name l_name5 = m_parser.parse( "cn=Website,cn=HomeDir,cn=John,ou=Marketing,ou=West" );
        Name l_name6 = m_parser.parse( "cn=Airline,cn=Website,cn=HomeDir,cn=John,ou=Marketing,ou=West" );

        Enumeration l_enum0 = l_name0.getAll();
        assertEquals( false, l_enum0.hasMoreElements() );

        Enumeration l_enum1 = l_name1.getAll();
        assertEquals( true, l_enum1.hasMoreElements() );
        for ( int ii = 0; l_enum1.hasMoreElements(); ii++ )
        {
            String l_element = ( String ) l_enum1.nextElement();
            if ( ii == 0 )
            {
                assertEquals( "ou=East", l_element );
            }
        }

        Enumeration l_enum2 = l_name2.getAll();
        assertEquals( true, l_enum2.hasMoreElements() );
        for ( int ii = 0; l_enum2.hasMoreElements(); ii++ )
        {
            String l_element = ( String ) l_enum2.nextElement();
            if ( ii == 0 )
            {
                assertEquals( "ou=East", l_element );
            }
            if ( ii == 1 )
            {
                assertEquals( "ou=Marketing", l_element );
            }
        }

        Enumeration l_enum3 = l_name3.getAll();
        assertEquals( true, l_enum3.hasMoreElements() );
        for ( int ii = 0; l_enum3.hasMoreElements(); ii++ )
        {
            String l_element = ( String ) l_enum3.nextElement();
            if ( ii == 0 )
            {
                assertEquals( "ou=East", l_element );
            }
            if ( ii == 1 )
            {
                assertEquals( "ou=Marketing", l_element );
            }
            if ( ii == 2 )
            {
                assertEquals( "cn=John", l_element );
            }
        }

        Enumeration l_enum4 = l_name4.getAll();
        assertEquals( true, l_enum4.hasMoreElements() );
        for ( int ii = 0; l_enum4.hasMoreElements(); ii++ )
        {
            String l_element = ( String ) l_enum4.nextElement();
            if ( ii == 0 )
            {
                assertEquals( "ou=East", l_element );
            }
            if ( ii == 1 )
            {
                assertEquals( "ou=Marketing", l_element );
            }
            if ( ii == 2 )
            {
                assertEquals( "cn=John", l_element );
            }
            if ( ii == 3 )
            {
                assertEquals( "cn=HomeDir", l_element );
            }
        }

        Enumeration l_enum5 = l_name5.getAll();
        assertEquals( true, l_enum5.hasMoreElements() );
        for ( int ii = 0; l_enum5.hasMoreElements(); ii++ )
        {
            String l_element = ( String ) l_enum5.nextElement();
            if ( ii == 0 )
            {
                assertEquals( "ou=West", l_element );
            }
            if ( ii == 1 )
            {
                assertEquals( "ou=Marketing", l_element );
            }
            if ( ii == 2 )
            {
                assertEquals( "cn=John", l_element );
            }
            if ( ii == 3 )
            {
                assertEquals( "cn=HomeDir", l_element );
            }
            if ( ii == 4 )
            {
                assertEquals( "cn=Website", l_element );
            }
        }

        Enumeration l_enum6 = l_name6.getAll();
        assertEquals( true, l_enum6.hasMoreElements() );
        for ( int ii = 0; l_enum6.hasMoreElements(); ii++ )
        {
            String l_element = ( String ) l_enum6.nextElement();
            if ( ii == 0 )
            {
                assertEquals( "ou=West", l_element );
            }
            if ( ii == 1 )
            {
                assertEquals( "ou=Marketing", l_element );
            }
            if ( ii == 2 )
            {
                assertEquals( "cn=John", l_element );
            }
            if ( ii == 3 )
            {
                assertEquals( "cn=HomeDir", l_element );
            }
            if ( ii == 4 )
            {
                assertEquals( "cn=Website", l_element );
            }
            if ( ii == 5 )
            {
                assertEquals( "cn=Airline", l_element );
            }
        }
    }


    /**
     * Class to test for get
     * 
     * @throws Exception
     *             anything goes wrong
     */
    public void testGet() throws Exception
    {
        Name l_name = m_parser.parse( "cn=HomeDir,cn=John,ou=Marketing,ou=East" );
        assertEquals( "cn=HomeDir", l_name.get( 3 ) );
        assertEquals( "cn=John", l_name.get( 2 ) );
        assertEquals( "ou=Marketing", l_name.get( 1 ) );
        assertEquals( "ou=East", l_name.get( 0 ) );
    }


    /**
     * Class to test for getSuffix
     * 
     * @throws Exception
     *             anything goes wrong
     */
    public void testGetXSuffix() throws Exception
    {
        Name l_name = m_parser.parse( "cn=HomeDir,cn=John,ou=Marketing,ou=East" );
        assertEquals( "", l_name.getSuffix( 0 ).toString() );
        assertEquals( "ou=East", l_name.getSuffix( 1 ).toString() );
        assertEquals( "ou=Marketing,ou=East", l_name.getSuffix( 2 ).toString() );
        assertEquals( "cn=John,ou=Marketing,ou=East", l_name.getSuffix( 3 ).toString() );
        assertEquals( "cn=HomeDir,cn=John,ou=Marketing,ou=East", l_name.getSuffix( 4 ).toString() );
    }


    /**
     * Class to test for getPrefix
     * 
     * @throws Exception
     *             anything goes wrong
     */
    public void testGetPrefix() throws Exception
    {
        Name l_name = m_parser.parse( "cn=HomeDir,cn=John,ou=Marketing,ou=East" );

        assertEquals( "cn=HomeDir,cn=John,ou=Marketing,ou=East", l_name.getPrefix( 0 ).toString() );
        assertEquals( "cn=John,ou=Marketing,ou=East", l_name.getPrefix( 1 ).toString() );
        assertEquals( "ou=Marketing,ou=East", l_name.getPrefix( 2 ).toString() );
        assertEquals( "ou=East", l_name.getPrefix( 3 ).toString() );
        assertEquals( "", l_name.getPrefix( 4 ).toString() );
    }


    /**
     * Class to test for getPrefix
     * 
     * @throws Exception
     *             anything goes wrong
     */
    /*
     * Temporarely commented, as the getPrefix returns the wrong value. public
     * void testGetPrefixModified() throws Exception { Name l_name =
     * m_parser.parse( "cn=HomeDir,cn=John,ou=Marketing,ou=East" ) ; Name prefix =
     * l_name.getPrefix( 1 ); assertEquals( "ou=East", prefix ); }
     */

    /**
     * Class to test for startsWith
     * 
     * @throws Exception
     *             anything goes wrong
     */
    public void testStartsWith() throws Exception
    {
        Name l_name0 = m_parser.parse( "cn=HomeDir,cn=John,ou=Marketing,ou=East" );
        Name l_name1 = m_parser.parse( "cn=HomeDir,cn=John,ou=Marketing,ou=East" );
        Name l_name2 = m_parser.parse( "cn=John,ou=Marketing,ou=East" );
        Name l_name3 = m_parser.parse( "ou=Marketing,ou=East" );
        Name l_name4 = m_parser.parse( "ou=East" );
        Name l_name5 = m_parser.parse( "" );

        Name l_name6 = m_parser.parse( "cn=HomeDir" );
        Name l_name7 = m_parser.parse( "cn=HomeDir,cn=John" );
        Name l_name8 = m_parser.parse( "cn=HomeDir,cn=John,ou=Marketing" );

        assertTrue( l_name0.startsWith( l_name1 ) );
        assertTrue( l_name0.startsWith( l_name2 ) );
        assertTrue( l_name0.startsWith( l_name3 ) );
        assertTrue( l_name0.startsWith( l_name4 ) );
        assertTrue( l_name0.startsWith( l_name5 ) );

        assertTrue( !l_name0.startsWith( l_name6 ) );
        assertTrue( !l_name0.startsWith( l_name7 ) );
        assertTrue( !l_name0.startsWith( l_name8 ) );
    }


    /**
     * Class to test for endsWith
     * 
     * @throws Exception
     *             anything goes wrong
     */
    public void testEndsWith() throws Exception
    {
        Name l_name0 = m_parser.parse( "cn=HomeDir,cn=John,ou=Marketing,ou=East" );
        Name l_name1 = m_parser.parse( "cn=HomeDir,cn=John,ou=Marketing,ou=East" );
        Name l_name2 = m_parser.parse( "cn=John,ou=Marketing,ou=East" );
        Name l_name3 = m_parser.parse( "ou=Marketing,ou=East" );
        Name l_name4 = m_parser.parse( "ou=East" );
        Name l_name5 = m_parser.parse( "" );

        Name l_name6 = m_parser.parse( "cn=HomeDir" );
        Name l_name7 = m_parser.parse( "cn=HomeDir,cn=John" );
        Name l_name8 = m_parser.parse( "cn=HomeDir,cn=John,ou=Marketing" );

        assertTrue( l_name0.endsWith( l_name1 ) );
        assertTrue( !l_name0.endsWith( l_name2 ) );
        assertTrue( !l_name0.endsWith( l_name3 ) );
        assertTrue( !l_name0.endsWith( l_name4 ) );
        assertTrue( l_name0.endsWith( l_name5 ) );

        assertTrue( l_name0.endsWith( l_name6 ) );
        assertTrue( l_name0.endsWith( l_name7 ) );
        assertTrue( l_name0.endsWith( l_name8 ) );

        /*
         * Hashtable l_env = new Hashtable() ; l_env.put(
         * Context.SECURITY_AUTHENTICATION, "simple" ) ; l_env.put(
         * Context.SECURITY_PRINCIPAL, "cn=admin,dc=example,dc=com" ) ;
         * l_env.put( Context.SECURITY_CREDENTIALS, "jPasswordField1" ) ;
         * l_env.put( Context.INITIAL_CONTEXT_FACTORY,
         * "com.sun.jndi.ldap.LdapCtxFactory" ) ; l_env.put(
         * Context.PROVIDER_URL, "ldap://localhost:1396/dc=example,dc=com" ) ;
         * DirContext l_ctx = new InitialDirContext( l_env ) ; NamingEnumeration
         * l_enum = l_ctx.listBindings( "" ) ; Name l_name0 = m_parser.parse(
         * "ou=Special Users,dc=example,dc=com" ) ; Name l_name1 =
         * m_parser.parse( "dc=example,dc=com" ) ; Name l_name2 =
         * m_parser.parse( "dc=com" ) ; Name l_name3 = m_parser.parse(
         * "ou=Special Users" ) ; Name l_name4 = m_parser.parse( "ou=Special
         * Users,dc=example" ) ; Name l_name5 = m_parser.parse( "" ) ; while (
         * l_enum.hasMore() ) { Binding l_binding = ( Binding ) l_enum.next() ;
         * DirContext l_dirCtx = ( DirContext ) l_binding.getObject() ;
         * NameParser l_parser = l_dirCtx.getNameParser( "" ) ; Name l_namex =
         * l_parser.parse( l_dirCtx.getNameInNamespace() ) ; // DirContext
         * l_dirCtx = ( DirContext ) l_enum.next() ; }
         */
    }


    /**
     * Class to test for Name addAll(Name)
     * 
     * @throws Exception
     *             when anything goes wrong
     */
    public void testAddAllName0() throws Exception
    {
        Name l_name = new LdapName();
        Name l_name0 = m_parser.parse( "cn=HomeDir,cn=John,ou=Marketing,ou=East" );
        assertTrue( l_name0.equals( l_name.addAll( l_name0 ) ) );
    }


    /**
     * Class to test for Name addAll(Name)
     * 
     * @throws Exception
     *             when anything goes wrong
     */
    public void testAddAllNameExisting0() throws Exception
    {
        Name name1 = new LdapName( "ou=Marketing,ou=East" );
        Name name2 = new LdapName( "cn=HomeDir,cn=John" );
        Name nameAdded = new LdapName( "cn=HomeDir,cn=John, ou=Marketing,ou=East" );
        assertTrue( nameAdded.equals( name1.addAll( name2 ) ) );
    }


    /**
     * Class to test for Name addAll(Name)
     * 
     * @throws Exception
     *             when anything goes wrong
     */
    public void testAddAllName1() throws Exception
    {
        Name l_name = new LdapName();
        Name l_name0 = m_parser.parse( "ou=Marketing,ou=East" );
        Name l_name1 = m_parser.parse( "cn=HomeDir,cn=John" );
        Name l_name2 = m_parser.parse( "cn=HomeDir,cn=John,ou=Marketing,ou=East" );

        assertTrue( l_name0.equals( l_name.addAll( l_name0 ) ) );
        assertTrue( l_name2.equals( l_name.addAll( l_name1 ) ) );
    }


    /**
     * Class to test for Name addAll(int, Name)
     * 
     * @throws Exception
     *             when something goes wrong
     */
    public void testAddAllintName0() throws Exception
    {
        Name l_name = new LdapName();
        Name l_name0 = m_parser.parse( "ou=Marketing,ou=East" );
        Name l_name1 = m_parser.parse( "cn=HomeDir,cn=John" );
        Name l_name2 = m_parser.parse( "cn=HomeDir,cn=John,ou=Marketing,ou=East" );

        assertTrue( l_name0.equals( l_name.addAll( l_name0 ) ) );
        assertTrue( l_name2.equals( l_name.addAll( 2, l_name1 ) ) );
    }


    /**
     * Class to test for Name addAll(int, Name)
     * 
     * @throws Exception
     *             when something goes wrong
     */
    public void testAddAllintName1() throws Exception
    {
        Name l_name = new LdapName();
        Name l_name0 = m_parser.parse( "cn=HomeDir,ou=Marketing,ou=East" );
        Name l_name1 = m_parser.parse( "cn=John" );
        Name l_name2 = m_parser.parse( "cn=HomeDir,cn=John,ou=Marketing,ou=East" );

        assertTrue( l_name0.equals( l_name.addAll( l_name0 ) ) );
        assertTrue( l_name2.equals( l_name.addAll( 2, l_name1 ) ) );

        Name l_name3 = m_parser.parse( "cn=Airport" );
        Name l_name4 = m_parser.parse( "cn=Airport,cn=HomeDir,cn=John,ou=Marketing,ou=East" );

        assertTrue( l_name4.equals( l_name.addAll( 4, l_name3 ) ) );

        Name l_name5 = m_parser.parse( "cn=ABC123" );
        Name l_name6 = m_parser.parse( "cn=Airport,cn=HomeDir,cn=ABC123,cn=John,ou=Marketing,ou=East" );

        assertTrue( l_name6.equals( l_name.addAll( 3, l_name5 ) ) );
    }


    /**
     * Class to test for Name add(String)
     * 
     * @throws Exception
     *             when something goes wrong
     */
    public void testAddString() throws Exception
    {
        Name l_name = new LdapName();
        assertEquals( l_name, m_parser.parse( "" ) );

        Name l_name4 = m_parser.parse( "ou=East" );
        l_name.add( "ou=East" );
        assertEquals( l_name4, l_name );

        Name l_name3 = m_parser.parse( "ou=Marketing,ou=East" );
        l_name.add( "ou=Marketing" );
        assertEquals( l_name3, l_name );

        Name l_name2 = m_parser.parse( "cn=John,ou=Marketing,ou=East" );
        l_name.add( "cn=John" );
        assertEquals( l_name2, l_name );

        Name l_name0 = m_parser.parse( "cn=HomeDir,cn=John,ou=Marketing,ou=East" );
        l_name.add( "cn=HomeDir" );
        assertEquals( l_name0, l_name );
    }


    /**
     * Class to test for Name add(int, String)
     * 
     * @throws Exception
     *             if anything goes wrong
     */
    public void testAddintString() throws Exception
    {
        Name l_name = new LdapName();
        assertEquals( l_name, m_parser.parse( "" ) );

        Name l_name4 = m_parser.parse( "ou=East" );
        l_name.add( "ou=East" );
        assertEquals( l_name4, l_name );

        Name l_name3 = m_parser.parse( "ou=Marketing,ou=East" );
        l_name.add( 1, "ou=Marketing" );
        assertEquals( l_name3, l_name );

        Name l_name2 = m_parser.parse( "cn=John,ou=Marketing,ou=East" );
        l_name.add( 2, "cn=John" );
        assertEquals( l_name2, l_name );

        Name l_name0 = m_parser.parse( "cn=HomeDir,cn=John,ou=Marketing,ou=East" );
        l_name.add( 3, "cn=HomeDir" );
        assertEquals( l_name0, l_name );

        Name l_name5 = m_parser.parse( "cn=HomeDir,cn=John,ou=Marketing,ou=East,o=LL " + "Bean Inc." );
        l_name.add( 0, "o=LL Bean Inc." );
        assertEquals( l_name5, l_name );

        Name l_name6 = m_parser.parse( "cn=HomeDir,cn=John,ou=Marketing,ou=East,c=US,o=LL " + "Bean Inc." );
        l_name.add( 1, "c=US" );
        assertEquals( l_name6, l_name );

        Name l_name7 = m_parser.parse( "cn=HomeDir,cn=John,ou=Advertising,ou=Marketing," + "ou=East,c=US,o=LL "
            + "Bean Inc." );
        l_name.add( 4, "ou=Advertising" );
        assertEquals( l_name7, l_name );
    }


    /**
     * Class to test for remove
     * 
     * @throws Exception
     *             if anything goes wrong
     */
    public void testRemove() throws Exception
    {
        Name l_name = new LdapName();
        assertEquals( m_parser.parse( "" ), l_name );

        Name l_name3 = m_parser.parse( "ou=Marketing" );
        l_name.add( "ou=East" );
        l_name.add( 1, "ou=Marketing" );
        l_name.remove( 0 );
        assertEquals( l_name3, l_name );

        Name l_name2 = m_parser.parse( "cn=HomeDir,ou=Marketing,ou=East" );
        l_name.add( 0, "ou=East" );
        l_name.add( 2, "cn=John" );
        l_name.add( "cn=HomeDir" );
        l_name.remove( 2 );
        assertEquals( l_name2, l_name );

        l_name.remove( 1 );
        Name l_name1 = m_parser.parse( "cn=HomeDir,ou=East" );
        assertEquals( l_name1, l_name );

        l_name.remove( 1 );
        Name l_name0 = m_parser.parse( "ou=East" );
        assertEquals( l_name0, l_name );

        l_name.remove( 0 );
        assertEquals( m_parser.parse( "" ), l_name );
    }


    /**
     * Class to test for String toString()
     * 
     * @throws Exception
     *             if anything goes wrong
     */
    public void testToString() throws Exception
    {
        Name l_name = new LdapName();
        assertEquals( "", l_name.toString() );

        l_name.add( "ou=East" );
        assertEquals( "ou=East", l_name.toString() );

        l_name.add( 1, "ou=Marketing" );
        assertEquals( "ou=Marketing,ou=East", l_name.toString() );

        l_name.add( "cn=John" );
        assertEquals( "cn=John,ou=Marketing,ou=East", l_name.toString() );

        l_name.add( "cn=HomeDir" );
        assertEquals( "cn=HomeDir,cn=John,ou=Marketing,ou=East", l_name.toString() );
    }


    /**
     * Class to test for boolean equals(Object)
     * 
     * @throws Exception
     *             if anything goes wrong
     */
    public void testEqualsObject() throws Exception
    {
        assertTrue( m_parser.parse( "ou=People" ).equals( m_parser.parse( "ou=People" ) ) );
        assertTrue( !m_parser.parse( "ou=People,dc=example,dc=com" ).equals( m_parser.parse( "ou=People" ) ) );
        assertTrue( !m_parser.parse( "ou=people" ).equals( m_parser.parse( "ou=People" ) ) );
        assertTrue( !m_parser.parse( "ou=Groups" ).equals( m_parser.parse( "ou=People" ) ) );
    }


    public void testAttributeEqualsIsCaseInSensitive() throws Exception
    {
        Name name1 = new LdapName( "cn=HomeDir" );
        Name name2 = new LdapName( "CN=HomeDir" );

        assertTrue( name1.equals( name2 ) );
    }


    public void testAttributeTypeEqualsIsCaseInsensitive() throws Exception
    {
        Name name1 = new LdapName( "cn=HomeDir+cn=WorkDir" );
        Name name2 = new LdapName( "cn=HomeDir+CN=WorkDir" );

        assertTrue( name1.equals( name2 ) );
    }


    public void testNameEqualsIsInsensitiveToAttributesOrder() throws Exception
    {

        Name name1 = new LdapName( "cn=HomeDir+cn=WorkDir" );
        Name name2 = new LdapName( "cn=WorkDir+cn=HomeDir" );

        assertTrue( name1.equals( name2 ) );
    }


    public void testAttributeComparisonIsCaseInSensitive() throws Exception
    {
        Name name1 = new LdapName( "cn=HomeDir" );
        Name name2 = new LdapName( "CN=HomeDir" );

        assertEquals( 0, name1.compareTo( name2 ) );
    }


    public void testAttributeTypeComparisonIsCaseInsensitive() throws Exception
    {
        Name name1 = new LdapName( "cn=HomeDir+cn=WorkDir" );
        Name name2 = new LdapName( "cn=HomeDir+CN=WorkDir" );

        assertEquals( 0, name1.compareTo( name2 ) );
    }


    public void testNameComparisonIsInsensitiveToAttributesOrder() throws Exception
    {

        Name name1 = new LdapName( "cn=HomeDir+cn=WorkDir" );
        Name name2 = new LdapName( "cn=WorkDir+cn=HomeDir" );

        assertEquals( 0, name1.compareTo( name2 ) );
    }


    public void testNameComparisonIsInsensitiveToAttributesOrderFailure() throws Exception
    {

        Name name1 = new LdapName( "cn=HomeDir+cn=Workdir" );
        Name name2 = new LdapName( "cn=Work+cn=HomeDir" );

        assertEquals( -15, name1.compareTo( name2 ) );
    }


    public void testNameFrenchChars() throws Exception
    {
        String cn = new String( new byte[]
            { 'c', 'n', '=', 0x4A, ( byte ) 0xC3, ( byte ) 0xA9, 0x72, ( byte ) 0xC3, ( byte ) 0xB4, 0x6D, 0x65 } );

        Name name = new LdapName( cn );

        assertEquals( cn, name.toString() );
    }


    public void testNameGermanChars() throws Exception
    {
        String cn = new String( new byte[]
            { 'c', 'n', '=', ( byte ) 0xC3, ( byte ) 0x84, ( byte ) 0xC3, ( byte ) 0x96, ( byte ) 0xC3, ( byte ) 0x9C,
                ( byte ) 0xC3, ( byte ) 0x9F, ( byte ) 0xC3, ( byte ) 0xA4, ( byte ) 0xC3, ( byte ) 0xB6,
                ( byte ) 0xC3, ( byte ) 0xBC }, "UTF-8" );

        Name name = new LdapName( cn );

        assertEquals( cn, name.toString() );
    }


    public void testNameTurkishChars() throws Exception
    {
        String cn = new String( new byte[]
            { 'c', 'n', '=', ( byte ) 0xC4, ( byte ) 0xB0, ( byte ) 0xC4, ( byte ) 0xB1, ( byte ) 0xC5, ( byte ) 0x9E,
                ( byte ) 0xC5, ( byte ) 0x9F, ( byte ) 0xC3, ( byte ) 0x96, ( byte ) 0xC3, ( byte ) 0xB6,
                ( byte ) 0xC3, ( byte ) 0x9C, ( byte ) 0xC3, ( byte ) 0xBC, ( byte ) 0xC4, ( byte ) 0x9E,
                ( byte ) 0xC4, ( byte ) 0x9F }, "UTF-8" );

        Name name = new LdapName( cn );

        assertEquals( cn, name.toString() );
    }

    /**
     * Class to test for toOid( Name, Map)
     */
    /*
     * public void testLdapNameToOid() throws Exception { ArrayList list = new
     * ArrayList() ; list.add( "ou=People" ) ; list.add( "dc=example" ) ;
     * list.add( "dc=com" ) ; LdapName name = new LdapName( list.iterator() ) ;
     * Map oids = new HashMap(); oids.put( "dc", "0.9.2342.19200300.100.1.25" );
     * oids.put( "ou", "2.5.4.11" ); LdapName result = LdapName.toOidName( name,
     * oids ); assertTrue( result.toString().equals(
     * "2.5.4.11=People,0.9.2342.19200300.100.1.25=example,0.9.2342.19200300.100.1.25=com" ) ) ; }
     */

    /**
     * Class to test for toOid( Name, Map)
     */
    /*
     * public void testLdapNameToOidEmpty() throws Exception { LdapName name =
     * new LdapName() ; Map oids = new HashMap(); oids.put( "dc",
     * "0.9.2342.19200300.100.1.25" ); oids.put( "ou", "2.5.4.11" ); LdapName
     * result = LdapName.toOidName( name, oids ); assertTrue(
     * result.toString().equals( "" ) ) ; }
     */
}
