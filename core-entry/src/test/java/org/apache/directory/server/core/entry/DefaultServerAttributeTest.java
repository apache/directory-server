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
package org.apache.directory.server.core.entry;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.naming.NamingException;
import javax.naming.directory.InvalidAttributeValueException;

import org.apache.directory.shared.ldap.schema.AbstractAttributeType;
import org.apache.directory.shared.ldap.schema.AbstractMatchingRule;
import org.apache.directory.shared.ldap.schema.AbstractSyntax;
import org.apache.directory.shared.ldap.schema.AttributeType;
import org.apache.directory.shared.ldap.schema.MatchingRule;
import org.apache.directory.shared.ldap.schema.Normalizer;
import org.apache.directory.shared.ldap.schema.Syntax;
import org.apache.directory.shared.ldap.schema.syntax.SyntaxChecker;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

/**
 * Tests for the DefaultServerAttribute class. 
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class DefaultServerAttributeTest
{
    /**
     * A local Syntax class for tests
     */
    static class AT extends AbstractAttributeType
    {
        public static final long serialVersionUID = 0L;
        AttributeType superior;
        Syntax syntax;
        MatchingRule equality;
        MatchingRule ordering;
        MatchingRule substr;

        protected AT( String oid )
        {
            super( oid );
        }

        public AttributeType getSuperior() throws NamingException
        {
            return superior;
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


        public void setSuperior( AttributeType superior )
        {
            this.superior = superior;
        }


        public void setSyntax( Syntax syntax )
        {
            this.syntax = syntax;
        }


        public void setEquality( MatchingRule equality )
        {
            this.equality = equality;
        }


        public void setOrdering( MatchingRule ordering )
        {
            this.ordering = ordering;
        }


        public void setSubstr( MatchingRule substr )
        {
            this.substr = substr;
        }
    }

    /**
     * A local MatchingRule class for tests
     */
    static class MR extends AbstractMatchingRule
    {
        public static final long serialVersionUID = 0L;
        private Syntax syntax;
        private Comparator comparator;
        private Normalizer normalizer;

        protected MR( String oid )
        {
            super( oid );
        }

        public Syntax getSyntax() throws NamingException
        {
            return syntax;
        }

        public Comparator getComparator() throws NamingException
        {
            return comparator;
        }


        public Normalizer getNormalizer() throws NamingException
        {
            return normalizer;
        }


        public void setSyntax( Syntax syntax )
        {
            this.syntax = syntax;
        }


        public void setComparator( Comparator<?> comparator )
        {
            this.comparator = comparator;
        }


        public void setNormalizer( Normalizer normalizer )
        {
            this.normalizer = normalizer;
        }
    }


    /**
     * A local Syntax class used for the tests
     */
    static class S extends AbstractSyntax
    {
        public static final long serialVersionUID = 0L;
        SyntaxChecker checker;

        public S( String oid, boolean humanReadible )
        {
            super( oid, humanReadible );
        }

        public void setSyntaxChecker( SyntaxChecker checker )
        {
            this.checker = checker;
        }

        public SyntaxChecker getSyntaxChecker() throws NamingException
        {
            return checker;
        }
    }

    private AttributeType getCaseIgnoringAttributeNoNumbersType()
    {
        S s = new S( "1.1.1.1", true );

        s.setSyntaxChecker( new SyntaxChecker()
        {
            public String getSyntaxOid()
            {
                return "1.1.1.1";
            }
            public boolean isValidSyntax( Object value )
            {
                if ( !( value instanceof String ) )
                {
                    return false;
                }

                String strval = ( String ) value;
                
                for ( char c:strval.toCharArray() )
                {
                    if ( Character.isDigit( c ) )
                    {
                        return false;
                    }
                }
                return true;
            }

            public void assertSyntax( Object value ) throws NamingException
            {
                if ( ! isValidSyntax( value ) )
                {
                    throw new InvalidAttributeValueException();
                }
            }
        } );

        final MR mr = new MR( "1.1.2.1" );
        mr.syntax = s;
        mr.comparator = new Comparator<String>()
        {
            public int compare( String o1, String o2 )
            {
                return ( o1 == null ? 
                    ( o2 == null ? 0 : -1 ) :
                    ( o2 == null ? 1 : o1.compareTo( o2 ) ) );
            }

            int getValue( String val )
            {
                if ( val.equals( "LOW" ) ) 
                {
                    return 0;
                }
                else if ( val.equals( "MEDIUM" ) ) 
                {
                    return 1;
                }
                else if ( val.equals( "HIGH" ) ) 
                {
                    return 2;
                }
                
                throw new IllegalArgumentException( "Not a valid value" );
            }
        };
        
        mr.normalizer = new Normalizer()
        {
            public static final long serialVersionUID = 1L;

            public Object normalize( Object value ) throws NamingException
            {
                if ( value instanceof String )
                {
                    return ( ( String ) value ).toLowerCase();
                }

                throw new IllegalStateException( "expected string to normalize" );
            }
        };
        
        AT at = new AT( "1.1.3.1" );
        at.setEquality( mr );
        at.setSyntax( s );
        return at;
    }


    private AttributeType getIA5StringAttributeType()
    {
        AT at = new AT( "1.1" );

        S s = new S( "1.1.1", true );

        s.setSyntaxChecker( new SyntaxChecker()
        {
            public String getSyntaxOid()
            {
                return "1.1.1";
            }
            public boolean isValidSyntax( Object value )
            {
                return ((String)value).length() < 5 ;
            }

            public void assertSyntax( Object value ) throws NamingException
            {
                if ( ! isValidSyntax( value ) )
                {
                    throw new InvalidAttributeValueException();
                }
            }
        } );

        final MR mr = new MR( "1.1.2" );
        mr.syntax = s;
        mr.comparator = new Comparator<String>()
        {
            public int compare( String o1, String o2 )
            {
                return ( ( o1 == null ) ? 
                    ( o2 == null ? 0 : -1 ) :
                    ( o2 == null ? 1 : o1.compareTo( o2 ) ) );
            }
        };
        
        mr.normalizer = new Normalizer()
        {
            public static final long serialVersionUID = 1L;
            
            public Object normalize( Object value ) throws NamingException
            {
                if ( value instanceof String )
                {
                    return ( ( String ) value ).toLowerCase();
                }

                throw new IllegalStateException( "expected string to normalize" );
            }
        };
        
        at.setEquality( mr );
        at.setSyntax( s );
        return at;
    }

    @Test public void testAddOneValue() throws NamingException
    {
        AttributeType at = getIA5StringAttributeType();
        
        DefaultServerAttribute attr = new DefaultServerAttribute( at );
        
        // Add a String value
        attr.add( "test" );
        
        assertEquals( 1, attr.size() );
        
        assertTrue( attr.getType().getSyntax().isHumanReadable() );
        
        ServerValue<?> value = attr.get();
        
        assertTrue( value instanceof ServerStringValue );
        assertEquals( "test", ((ServerStringValue)value).get() );
        
        // Add a binary value
        try
        {
            attr.add( new byte[]{0x01} );
            fail();
        }
        catch ( InvalidAttributeValueException iave )
        {
            assertTrue( true );
        }
        
        // Add a ServerValue
        ServerValue<?> ssv = new ServerStringValue( at, "test2" );
        
        attr.add( ssv );
        
        assertEquals( 2, attr.size() );
        
        Set<String> expected = new HashSet<String>();
        expected.add( "test" );
        expected.add( "test2" );
        
        for ( Iterator<ServerValue<?>> iter = attr.getAll(); iter.hasNext(); )
        {
            ServerStringValue val = (ServerStringValue)iter.next();
            
            if ( expected.contains( val.get() ) )
            {
                expected.remove( val.get() );
            }
            else
            {
                fail();
            }
        }
        
        assertEquals( 0, expected.size() );
    }


    @Test public void testAddTwoValue() throws NamingException
    {
        AttributeType at = getIA5StringAttributeType();
        
        DefaultServerAttribute attr = new DefaultServerAttribute( at );
        
        // Add String values
        attr.add( "test" );
        attr.add( "test2" );
        
        assertEquals( 2, attr.size() );
        
        assertTrue( attr.getType().getSyntax().isHumanReadable() );
        
        Set<String> expected = new HashSet<String>();
        expected.add( "test" );
        expected.add( "test2" );
        
        for ( Iterator<ServerValue<?>> iter = attr.getAll(); iter.hasNext(); )
        {
            ServerStringValue val = (ServerStringValue)iter.next();
            
            if ( expected.contains( val.get() ) )
            {
                expected.remove( val.get() );
            }
            else
            {
                fail();
            }
        }
        
        assertEquals( 0, expected.size() );
    }


    @Test public void testAddNullValue() throws NamingException
    {
        AttributeType at = getIA5StringAttributeType();
        
        DefaultServerAttribute attr = new DefaultServerAttribute( at );
        
        // Add a null value
        attr.add( new ServerStringValue( at, null ) );
        
        assertEquals( 1, attr.size() );
        
        assertTrue( attr.getType().getSyntax().isHumanReadable() );
        
        ServerValue<?> value = attr.get();
        
        assertTrue( value instanceof ServerStringValue );
        assertNull( ((ServerStringValue)value).get() );
    }
}
