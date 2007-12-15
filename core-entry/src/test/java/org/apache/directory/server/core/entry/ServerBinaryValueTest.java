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


import org.apache.directory.shared.ldap.schema.AbstractAttributeType;
import org.apache.directory.shared.ldap.schema.AbstractMatchingRule;
import org.apache.directory.shared.ldap.schema.AbstractSyntax;
import org.apache.directory.shared.ldap.schema.AttributeType;
import org.apache.directory.shared.ldap.schema.ByteArrayComparator;
import org.apache.directory.shared.ldap.schema.MatchingRule;
import org.apache.directory.shared.ldap.schema.Normalizer;
import org.apache.directory.shared.ldap.schema.Syntax;
import org.apache.directory.shared.ldap.schema.syntax.SyntaxChecker;
import org.junit.Test;

import javax.naming.NamingException;
import javax.naming.directory.InvalidAttributeValueException;
import java.util.Arrays;
import java.util.Comparator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;


/**
 * Tests that the ServerBinaryValue class works properly as expected.
 *
 * Some notes while conducting tests:
 *
 * <ul>
 *   <li>comparing values with different types - how does this behave</li>
 *   <li>exposing access to at from value or to a comparator?</li>
 * </ul>
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class ServerBinaryValueTest
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



    private AttributeType getBytesAttributeType()
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
                return ((byte[])value).length < 5 ;
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
        mr.comparator = new Comparator<byte[]>()
        {
            public int compare( byte[] o1, byte[] o2 )
            {
                return ( ( o1 == null ) ? 
                    ( o2 == null ? 0 : -1 ) :
                    ( o2 == null ? 1 : ByteArrayComparator.INSTANCE.compare( o1, o2 ) ) );
            }
        };
        
        mr.normalizer = new Normalizer()
        {
            public static final long serialVersionUID = 1L;
            
            public Object normalize( Object value ) throws NamingException
            {
                if ( value instanceof byte[] )
                {
                    byte[] val = (byte[])value;
                    // each byte will be changed to be > 0
                    byte[] newVal = new byte[ val.length ];
                    int i = 0;
                    
                    for ( byte b:val )
                    {
                        newVal[i++] = (byte)(b & 0x007F); 
                    }
                    
                    return newVal;
                }

                throw new IllegalStateException( "expected byte[] to normalize" );
            }
        };
        
        at.setEquality( mr );
        at.setSyntax( s );
        return at;
    }

    
    /**
     * Test the constructor with bad AttributeType
     */
    @Test public void testBadConstructor()
    {
        try
        {
            new ServerBinaryValue( null );
            fail();
        }
        catch ( AssertionError ae )
        {
            // Expected...
        }
        
        // create a AT without any syntax
        AttributeType at = new AT( "1.1.3.1" );
        
        try
        {
            new ServerBinaryValue( at );
            fail();
        }
        catch ( AssertionError ae )
        {
            // Expected...
        }
    }


    /**
     * Test the constructor with a null value
     */
    @Test public void testNullValue()
    {
        AttributeType at = getBytesAttributeType();
        
        ServerBinaryValue value = new ServerBinaryValue( at, null );
        
        assertNull( value.getReference() );
        assertTrue( value.isNull() );
    }
    
    
    /**
     * Test the equals method
     */
    @Test public void testEquals()
    {
        AttributeType at = getBytesAttributeType();
        
        ServerBinaryValue value1 = new ServerBinaryValue( at, new byte[]{0x01, (byte)0x02} );
        ServerBinaryValue value2 = new ServerBinaryValue( at, new byte[]{0x01, (byte)0x02} );
        ServerBinaryValue value3 = new ServerBinaryValue( at, new byte[]{0x01, (byte)0x82} );
        ServerBinaryValue value4 = new ServerBinaryValue( at, new byte[]{0x01} );
        ServerBinaryValue value5 = new ServerBinaryValue( at, null );
        
        assertTrue( value1.equals( value1 ) );
        assertTrue( value1.equals( value2 ) );
        assertTrue( value1.equals( value3 ) );
        assertFalse( value1.equals( value4 ) );
        assertFalse( value1.equals( value5 ) );
        assertFalse( value1.equals( "test" ) );
        assertFalse( value1.equals( null ) );
    }

    
    /**
     * Test the getNormalized method
     * TODO testNormalized.
     *
     */
    @Test public void testGetNormalized() throws NamingException
    {
        AttributeType at = getBytesAttributeType();
        
        ServerBinaryValue value = new ServerBinaryValue( at, new byte[]{0x01, (byte)0x82} );
        
        assertTrue( Arrays.equals( new byte[]{0x01, (byte)0x02}, value.getNormalizedReference() ) );
        assertTrue( Arrays.equals( new byte[]{0x01, (byte)0x02}, value.getNormalizedCopy() ) );

        value = new ServerBinaryValue( at, null );
        
        assertNull( value.getNormalizedReference() );
    }
    
    
    /**
     * Test the isValid method
     * 
     * The SyntaxChecker does not accept values longer than 5 chars.
     */
    @Test public void testIsValid() throws NamingException
    {
        AttributeType at = getBytesAttributeType();
        
        ServerBinaryValue value = new ServerBinaryValue( at, new byte[]{0x01, 0x02} );
        
        assertTrue( value.isValid() );

        value = new ServerBinaryValue( at, new byte[]{0x01, 0x02, 0x03, 0x04, 0x05, 0x06} );
        
        assertFalse( value.isValid() );
    }
    
    
    /**
     * Tests to make sure the hashCode method is working properly.
     * @throws Exception on errors
     */
    @Test public void testHashCodeValidEquals() throws Exception
    {
        AttributeType at = getBytesAttributeType();
        ServerBinaryValue v0 = new ServerBinaryValue( at, new byte[]{0x01, 0x02} );
        ServerBinaryValue v1 = new ServerBinaryValue( at, new byte[]{(byte)0x81, (byte)0x82} );
        ServerBinaryValue v2 = new ServerBinaryValue( at, new byte[]{0x01, 0x02} );
        assertEquals( v0.hashCode(), v1.hashCode() );
        assertEquals( v1.hashCode(), v2.hashCode() );
        assertEquals( v0.hashCode(), v2.hashCode() );
        assertEquals( v0, v1 );
        assertEquals( v0, v2 );
        assertEquals( v1, v2 );
        assertTrue( v0.isValid() );
        assertTrue( v1.isValid() );
        assertTrue( v2.isValid() );

        ServerBinaryValue v3 = new ServerBinaryValue( at, new byte[]{0x01, 0x03} );
        assertFalse( v3.equals( v0 ) );
        assertFalse( v3.equals( v1 ) );
        assertFalse( v3.equals( v2 ) );
        assertTrue( v3.isValid() );
    }
}