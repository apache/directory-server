package org.apache.directory.server.core.entry;


import junit.framework.TestCase;
import org.apache.directory.shared.ldap.schema.*;
import org.apache.directory.shared.ldap.schema.syntax.AcceptAllSyntaxChecker;
import org.apache.directory.shared.ldap.schema.syntax.SyntaxChecker;

import javax.naming.NamingException;
import javax.naming.directory.InvalidAttributeValueException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;


/**
 * Tests that the ServerStringValue class works properly as expected.
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
public class ServerStringValueTest extends TestCase
{
    private AttributeType getCaseIgnoringAttributeNoNumbersType()
    {
        S s = new S( "1.1.1.1", true );

        s.setSyntaxChecker( new SyntaxChecker(){
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
                for ( int ii = 0; ii < strval.length(); ii++ )
                {
                    if ( Character.isDigit( strval.charAt( ii ) ) )
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
                if ( o1 == null && o2 == null )
                {
                    return 0;
                }

                //noinspection ConstantConditions
                if ( o1 == null && o2 != null )
                {
                    return -1;
                }

                //noinspection ConstantConditions
                if ( o1 != null && o2 == null )
                {
                    return 1;
                }

                return o1.compareTo( o2 );
            }

            int getValue( String val )
            {
                if ( val.equals( "LOW" ) ) return 0;
                if ( val.equals( "MEDIUM" ) ) return 1;
                if ( val.equals( "HIGH" ) ) return 2;
                throw new IllegalArgumentException( "Not a valid value" );
            }
        };
        mr.normalizer = new Normalizer(){

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


    /**
     * Tests to make sure the hashCode method is working properly.
     * @throws Exception on errors
     */
    public void testHashCodeValidEquals() throws Exception
    {
        AttributeType at = getCaseIgnoringAttributeNoNumbersType();
        ServerStringValue v0 = new ServerStringValue( at, "Alex" );
        ServerStringValue v1 = new ServerStringValue( at, "ALEX" );
        ServerStringValue v2 = new ServerStringValue( at, "alex" );
        assertEquals( v0.hashCode(), "alex".hashCode() );
        assertEquals( v1.hashCode(), "alex".hashCode() );
        assertEquals( v2.hashCode(), "alex".hashCode() );
        assertEquals( v0, v1 );
        assertEquals( v0, v2 );
        assertEquals( v1, v2 );
        assertTrue( v0.isValid() );
        assertTrue( v1.isValid() );
        assertTrue( v2.isValid() );

        ServerStringValue v3 = new ServerStringValue( at, "Timber" );
        assertFalse( v3.equals( v0 ) );
        assertFalse( v3.equals( v1 ) );
        assertFalse( v3.equals( v2 ) );
        assertTrue( v3.isValid() );

        ServerStringValue v4 = new ServerStringValue( at, "Timber123" );
        assertFalse( v4.isValid() );
    }


    /**
     * Presumes an attribute which constrains it's values to some constant
     * strings: LOW, MEDUIM, HIGH.  Normalization does nothing. MatchingRules
     * are exact case matching.
     *
     * @throws Exception on errors
     */
    public void testConstrainedString() throws Exception
    {
        S s = new S( "1.1.1.1", true );
            
        s.setSyntaxChecker( new SyntaxChecker() {
            public String getSyntaxOid() { return "1.1.1.1"; }
            public boolean isValidSyntax( Object value )
            {
                if ( value instanceof String )
                {
                    String strval = ( String ) value;
                    return strval.equals( "HIGH" ) || strval.equals( "LOW" ) || strval.equals( "MEDIUM" );
                }
                return false;
            }
            public void assertSyntax( Object value ) throws NamingException
            { if ( ! isValidSyntax( value ) ) throw new InvalidAttributeValueException(); }
        });

        final MR mr = new MR( "1.1.2.1" );
        mr.syntax = s;
        mr.comparator = new Comparator<String>()
        {
            public int compare( String o1, String o2 )
            {
                if ( o1 == null && o2 == null )
                {
                    return 0;
                }

                //noinspection ConstantConditions
                if ( o1 == null && o2 != null )
                {
                    return -1;
                }

                //noinspection ConstantConditions
                if ( o1 != null && o2 == null )
                {
                    return 1;
                }

                int i1 = getValue( o1 );
                int i2 = getValue( o2 );

                if ( i1 == i2 ) return 0;
                if ( i1 > i2 ) return 1;
                if ( i1 < i2 ) return -1;

                throw new IllegalStateException( "should not get here at all" );
            }

            int getValue( String val )
            {
                if ( val.equals( "LOW" ) ) return 0;
                if ( val.equals( "MEDIUM" ) ) return 1;
                if ( val.equals( "HIGH" ) ) return 2;
                throw new IllegalArgumentException( "Not a valid value" );
            }
        };
        mr.normalizer = new NoOpNormalizer();
        AT at = new AT( "1.1.3.1" );
        at.setEquality( mr );
        at.setSyntax( s );

        // check that normalization and syntax checks work as expected
        ServerStringValue value = new ServerStringValue( at, "HIGH" );
        assertEquals( value.get(), value.getNormalizedValue() );
        assertTrue( value.isValid() );
        value = new ServerStringValue( at, "high" );
        assertFalse( value.isValid() );

        // create a bunch to best tested for equals and in containers
        ServerStringValue v0 = new ServerStringValue( at, "LOW" );
        assertTrue( v0.isValid() );
        ServerStringValue v1 = new ServerStringValue( at, "LOW" );
        assertTrue( v1.isValid() );
        ServerStringValue v2 = new ServerStringValue( at, "MEDIUM" );
        assertTrue( v2.isValid() );
        ServerStringValue v3 = new ServerStringValue( at, "HIGH" );
        assertTrue( v3.isValid() );
        ServerStringValue v4 = new ServerStringValue( at );
        assertFalse( v4.isValid() );
        ServerStringValue v5 = new ServerStringValue( at );
        assertFalse( v5.isValid() );

        // check equals
        assertTrue( v0.equals( v1 ) );
        assertTrue( v1.equals( v0 ) );
        assertEquals( 0, v0.compareTo( v1 ) );

        assertTrue( v4.equals( v5 ) );
        assertTrue( v5.equals( v4 ) );
        assertEquals( 0, v4.compareTo( v5 ) );

        assertFalse( v2.equals( v3 ) );
        assertFalse( v3.equals( v2 ) );
        assertTrue( v2.compareTo( v3 ) < 0 );
        assertTrue( v3.compareTo( v2 ) > 0 );

        // add all except v1 and v5 to a set
        HashSet<ServerStringValue> set = new HashSet<ServerStringValue>();
        set.add( v0 );
        set.add( v2 );
        set.add( v3 );
        set.add( v4 );

        // check contains method
        assertTrue( "since v1.equals( v0 ) and v0 was added then this should be true", set.contains( v1 ) );
        assertTrue( "since v4.equals( v5 ) and v4 was added then this should be true", set.contains( v5 ) );

        // check ordering based on the comparator
        ArrayList<ServerValue<String>> list = new ArrayList<ServerValue<String>>();
        list.add( v1 );
        list.add( v3 );
        list.add( v5 );
        list.add( v0 );
        list.add( v2 );
        list.add( v4 );

        //noinspection unchecked
        Collections.sort( list );

        // null ones are at first 2 indices
        assertTrue( "since v4 equals v5 and has no value either could be at index 0 & 1", list.get( 0 ).equals( v4 ) );
        assertTrue( "since v4 equals v5 and has no value either could be at index 0 & 1", list.get( 0 ).equals( v5 ) );
        assertTrue( "since v4 equals v5 and has no value either could be at index 0 & 1", list.get( 1 ).equals( v4 ) );
        assertTrue( "since v4 equals v5 and has no value either could be at index 0 & 1", list.get( 1 ).equals( v5 ) );

        // low ones are at the 3rd and 4th indices
        assertTrue( "since v0 equals v1 either could be at index 2 & 3", list.get( 2 ).equals( v0 ) );
        assertTrue( "since v0 equals v1 either could be at index 2 & 3", list.get( 2 ).equals( v1 ) );
        assertTrue( "since v0 equals v1 either could be at index 2 & 3", list.get( 3 ).equals( v0 ) );
        assertTrue( "since v0 equals v1 either could be at index 2 & 3", list.get( 3 ).equals( v1 ) );

        // medium then high next
        assertTrue( "since v2 \"MEDIUM\" should be at index 4", list.get( 4 ).equals( v2 ) );
        assertTrue( "since v3 \"HIGH\" should be at index 5", list.get( 5 ).equals( v3 ) );

        assertEquals( 6, list.size() );
    }


    /**
     * Creates a string value with an attribute type that is of a syntax
     * which accepts anything.  Also there is no normalization since the
     * value is the same as the normalized value.  This makes the at technically
     * a binary value however it can be dealt with as a string so this test
     * is still OK.
     * @throws Exception on errors
     */
    public void testAcceptAllNoNormalization() throws Exception
    {
        S s = new S( "1.1.1.1", false );
        s.setSyntaxChecker( new AcceptAllSyntaxChecker( "1.1.1.1" ) );
        final MR mr = new MR( "1.1.2.1" );
        mr.syntax = s;
        mr.comparator = new ByteArrayComparator();
        mr.normalizer = new NoOpNormalizer();
        AT at = new AT( "1.1.3.1" );
        at.setEquality( mr );
        at.setOrdering( mr );
        at.setSubstr( mr );
        at.setSyntax( s );

        // check that normalization and syntax checks work as expected
        ServerStringValue value = new ServerStringValue( at, "hello" );
        assertEquals( value.get(), value.getNormalizedValue() );
        assertTrue( value.isValid() );

        // create a bunch to best tested for equals and in containers
        ServerStringValue v0 = new ServerStringValue( at, "hello" );
        ServerStringValue v1 = new ServerStringValue( at, "hello" );
        ServerStringValue v2 = new ServerStringValue( at, "next0" );
        ServerStringValue v3 = new ServerStringValue( at, "next1" );
        ServerStringValue v4 = new ServerStringValue( at );
        ServerStringValue v5 = new ServerStringValue( at );

        // check equals
        assertTrue( v0.equals( v1 ) );
        assertTrue( v1.equals( v0 ) );
        assertTrue( v4.equals( v5 ) );
        assertTrue( v5.equals( v4 ) );
        assertFalse( v2.equals( v3 ) );
        assertFalse( v3.equals( v2 ) );

        // add all except v1 and v5 to a set
        HashSet<ServerStringValue> set = new HashSet<ServerStringValue>();
        set.add( v0 );
        set.add( v2 );
        set.add( v3 );
        set.add( v4 );

        // check contains method
        assertTrue( "since v1.equals( v0 ) and v0 was added then this should be true", set.contains( v1 ) );
        assertTrue( "since v4.equals( v5 ) and v4 was added then this should be true", set.contains( v5 ) );

        // check ordering based on the comparator
        ArrayList<ServerStringValue> list = new ArrayList<ServerStringValue>();
        list.add( v1 );
        list.add( v3 );
        list.add( v5 );
        list.add( v0 );
        list.add( v2 );
        list.add( v4 );

        Comparator c = new Comparator<ServerStringValue>()
        {
            public int compare( ServerStringValue o1, ServerStringValue o2 )
            {
                byte[] b1 = new byte[0];
                byte[] b2 = new byte[0];

                try
                {
                    if ( o1 != null )
                    {
                        String n1 = o1.getNormalizedValue();
                        if ( n1 != null )
                        {
                            b1 = n1.getBytes( "UTF-8" );
                        }
                    }

                    if ( o2 != null )
                    {
                        String n2 = o2.getNormalizedValue();
                        if ( n2 != null )
                        {
                            b2 = o2.getNormalizedValue().getBytes( "UTF-8" );
                        }
                    }
                }
                catch ( Exception e )
                {
                    e.printStackTrace();
                }

                try
                {
                    //noinspection unchecked
                    return mr.getComparator().compare( b1, b2 );
                }
                catch ( Exception e )
                {
                    throw new IllegalStateException( "Normalization and comparison should succeed!", e );
                }
            }
        };
        //noinspection unchecked
        Collections.sort( list, c );

        assertTrue( "since v4 equals v5 and has no value either could be at index 0 & 1", list.get( 0 ).equals( v4 ) );
        assertTrue( "since v4 equals v5 and has no value either could be at index 0 & 1", list.get( 0 ).equals( v5 ) );
        assertTrue( "since v4 equals v5 and has no value either could be at index 0 & 1", list.get( 1 ).equals( v4 ) );
        assertTrue( "since v4 equals v5 and has no value either could be at index 0 & 1", list.get( 1 ).equals( v5 ) );

        assertTrue( "since v0 equals v1 either could be at index 2 & 3", list.get( 2 ).equals( v0 ) );
        assertTrue( "since v0 equals v1 either could be at index 2 & 3", list.get( 2 ).equals( v1 ) );
        assertTrue( "since v0 equals v1 either could be at index 2 & 3", list.get( 3 ).equals( v0 ) );
        assertTrue( "since v0 equals v1 either could be at index 2 & 3", list.get( 3 ).equals( v1 ) );

        assertTrue( "since v2 \"next0\" should be at index 4", list.get( 4 ).equals( v2 ) );
        assertTrue( "since v3 \"next1\" should be at index 5", list.get( 5 ).equals( v3 ) );

        assertEquals( 6, list.size() );
    }

    
    static class AT extends AbstractAttributeType
    {
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

    static class MR extends AbstractMatchingRule
    {
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


        public void setComparator( Comparator comparator )
        {
            this.comparator = comparator;
        }


        public void setNormalizer( Normalizer normalizer )
        {
            this.normalizer = normalizer;
        }
    }


    static class S extends AbstractSyntax
    {
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
}