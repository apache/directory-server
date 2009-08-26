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

import javax.naming.NamingException;
import javax.naming.directory.InvalidAttributeValueException;

import org.apache.directory.shared.ldap.entry.Value;
import org.apache.directory.shared.ldap.entry.client.ClientBinaryValue;
import org.apache.directory.shared.ldap.entry.client.ClientStringValue;
import org.apache.directory.shared.ldap.schema.AbstractAttributeType;
import org.apache.directory.shared.ldap.schema.AbstractMatchingRule;
import org.apache.directory.shared.ldap.schema.AttributeType;
import org.apache.directory.shared.ldap.schema.LdapComparator;
import org.apache.directory.shared.ldap.schema.MatchingRule;
import org.apache.directory.shared.ldap.schema.Normalizer;
import org.apache.directory.shared.ldap.schema.LdapSyntax;
import org.apache.directory.shared.ldap.schema.SyntaxChecker;
import org.apache.directory.shared.ldap.schema.comparators.ByteArrayComparator;
import org.apache.directory.shared.ldap.schema.normalizers.DeepTrimToLowerNormalizer;
import org.apache.directory.shared.ldap.util.StringTools;

/**
 * Some common declaration used by the serverEntry tests.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class TestServerEntryUtils
{
    /**
     * A local Syntax class for tests
     */
    static class AT extends AbstractAttributeType
    {
        private static final long serialVersionUID = 0L;
        AttributeType superior;
        LdapSyntax syntax;
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


        public LdapSyntax getSyntax() throws NamingException
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


        public void setSyntax( LdapSyntax syntax )
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

    public static MatchingRule matchingRuleFactory( String oid )
    {
        MatchingRule matchingRule = new MatchingRule( oid );
        
        return matchingRule;
    }
    /**
     * A local MatchingRule class for tests
     */
    static class MR extends AbstractMatchingRule
    {
        private static final long serialVersionUID = 0L;
        LdapSyntax syntax;
        LdapComparator<? super Object> ldapComparator;
        Normalizer normalizer;

        protected MR( String oid )
        {
            super( oid );
        }

        public LdapSyntax getSyntax() throws NamingException
        {
            return syntax;
        }

        public LdapComparator<? super Object> getLdapComparator() throws NamingException
        {
            return ldapComparator;
        }


        public Normalizer getNormalizer() throws NamingException
        {
            return normalizer;
        }


        public void setSyntax( LdapSyntax syntax )
        {
            this.syntax = syntax;
        }


        public void setComparator( Comparator<? super Object> comparator )
        {
            this.ldapComparator = comparator;
        }


        public void setNormalizer( Normalizer normalizer )
        {
            this.normalizer = normalizer;
        }
    }


    /**
     * A local Syntax class used for the tests
     */
    public static LdapSyntax syntaxFactory( String oid, boolean humanReadable )
    {
        LdapSyntax ldapSyntax = new LdapSyntax( oid );
        
        ldapSyntax.setHumanReadable( humanReadable );
        
        return ldapSyntax;
    }
    static class S extends LdapSyntax
    {
        private static final long serialVersionUID = 0L;
        SyntaxChecker checker;

        public S( String oid, boolean humanReadible )
        {
            super( oid, "", humanReadible );
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

    /* no protection*/ static AttributeType getCaseIgnoringAttributeNoNumbersType()
    {
        S s = new S( "1.1.1.1", true );

        s.setSyntaxChecker( new SyntaxChecker( "1.1.2.1" )
        {
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
        } );

        final MR mr = new MR( "1.1.2.1" );
        mr.syntax = s;
        mr.ldapComparator = new LdapComparator<String>()
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
        
        mr.normalizer = new Normalizer( "1.1.1" )
        {
            // The serial UID
            private static final long serialVersionUID = 1L;

            public Value<?> normalize( Value<?> value ) throws NamingException
            {
                if ( !value.isBinary() )
                {
                    return new ClientStringValue( value.getString().toLowerCase() );
                }

                throw new IllegalStateException( "expected string to normalize" );
            }
            
            
            public String normalize( String value ) throws NamingException
            {
                return value.toLowerCase();
            }
        };
        
        AttributeType at = new AttributeType( "1.1.3.1" );
        at.setEquality( mr );
        at.setSyntax( s );
        return at;
    }


    /* no protection*/ static AttributeType getIA5StringAttributeType()
    {
        AT at = new AT( "1.1" );

        S s = new S( "1.1.1", true );

        s.setSyntaxChecker( new SyntaxChecker( "1.1.2" )
        {
            public boolean isValidSyntax( Object value )
            {
                return ((String)value == null) || (((String)value).length() < 7) ;
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
        
        mr.normalizer = new DeepTrimToLowerNormalizer( mr.getOid());
        
        at.setEquality( mr );
        at.setSyntax( s );
        return at;
    }


    /* No protection */ static AttributeType getBytesAttributeType()
    {
        AT at = new AT( "1.2" );

        S s = new S( "1.2.1", true );

        s.setSyntaxChecker( new SyntaxChecker( "1.2.1" )
        {
            public boolean isValidSyntax( Object value )
            {
                return ( value == null ) || ( ((byte[])value).length < 5 );
            }
        } );

        final MR mr = new MR( "1.2.2" );
        mr.syntax = s;
        mr.setComparator( new ByteArrayComparator() );
        
        mr.normalizer = new Normalizer( "1.1.1" )
        {
            // The serial UID
            private static final long serialVersionUID = 1L;
            
            public Value<?> normalize( Value<?> value ) throws NamingException
            {
                if ( value.isBinary() )
                {
                    byte[] val = value.getBytes();
                    
                    // each byte will be changed to be > 0, and spaces will be trimmed
                    byte[] newVal = new byte[ val.length ];
                    
                    int i = 0;
                    
                    for ( byte b:val )
                    {
                        newVal[i++] = (byte)(b & 0x007F); 
                    }
                    
                    return new ClientBinaryValue( StringTools.trim( newVal ) );
                }

                throw new IllegalStateException( "expected byte[] to normalize" );
            }

            public String normalize( String value ) throws NamingException
            {
                throw new IllegalStateException( "expected byte[] to normalize" );
            }
        };
        
        at.setEquality( mr );
        at.setSyntax( s );
        return at;
    }
}
