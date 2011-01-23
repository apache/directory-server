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
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.shared.ldap.model.entry.BinaryValue;
import org.apache.directory.shared.ldap.model.entry.StringValue;
import org.apache.directory.shared.ldap.model.entry.Value;
import org.apache.directory.shared.ldap.model.exception.LdapException;
import org.apache.directory.shared.ldap.schema.AttributeType;
import org.apache.directory.shared.ldap.schema.LdapComparator;
import org.apache.directory.shared.ldap.schema.LdapSyntax;
import org.apache.directory.shared.ldap.schema.MatchingRule;
import org.apache.directory.shared.ldap.schema.Normalizer;
import org.apache.directory.shared.ldap.schema.SyntaxChecker;
import org.apache.directory.shared.ldap.schema.comparators.ByteArrayComparator;
import org.apache.directory.shared.ldap.schema.normalizers.DeepTrimToLowerNormalizer;
import org.apache.directory.shared.util.Strings;

/**
 * Some common declaration used by the serverEntry tests.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class TestServerEntryUtils
{
    /**
     * A local Syntax class for tests
     */
    static class AT extends AttributeType
    {
        private static final long serialVersionUID = 0L;

        protected AT( String oid )
        {
            super( oid );
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
    static class MR extends MatchingRule
    {
        private static final long serialVersionUID = 0L;

        protected MR( String oid )
        {
            super( oid );
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

        public S( String oid, boolean humanReadible )
        {
            super( oid, "", humanReadible );
        }
    }

    /* no protection*/ 
    //This will suppress PMD.AvoidUsingHardCodedIP warnings in this class
    @SuppressWarnings("PMD.AvoidUsingHardCodedIP")
    static AttributeType getCaseIgnoringAttributeNoNumbersType()
    {
        AttributeType attributeType = new AttributeType( "1.1.3.1" );
        LdapSyntax syntax = new LdapSyntax( "1.1.1.1", "", true );

        syntax.setSyntaxChecker( new SyntaxChecker( "1.1.2.1" )
        {
            private static final long serialVersionUID = 0L;

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
        
        MatchingRule matchingRule = new MatchingRule( "1.1.2.1" );
        matchingRule.setSyntax( syntax );


        matchingRule.setLdapComparator( new LdapComparator<String>( matchingRule.getOid() )
        {
            private static final long serialVersionUID = 0L;

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
                
                throw new IllegalArgumentException( I18n.err( I18n.ERR_472 ) );
            }
        } );
        
        Normalizer normalizer = new Normalizer( "1.1.1" )
        {
            private static final long serialVersionUID = 0L;

            public Value<?> normalize( Value<?> value ) throws LdapException
            {
                if ( !value.isBinary() )
                {
                    return new StringValue( value.getString().toLowerCase() );
                }

                throw new IllegalStateException( I18n.err( I18n.ERR_473 ) );
            }
            
            
            public String normalize( String value ) throws LdapException
            {
                return value.toLowerCase();
            }
        };
        
        matchingRule.setNormalizer( normalizer );
        
        attributeType.setEquality( matchingRule );
        attributeType.setSyntax( syntax );
        
        return attributeType;
    }


    /* no protection*/ static AttributeType getIA5StringAttributeType()
    {
        AttributeType attributeType = new AttributeType( "1.1" );
        attributeType.addName( "1.1" );
        LdapSyntax syntax = new LdapSyntax( "1.1.1", "", true );

        syntax.setSyntaxChecker( new SyntaxChecker( "1.1.2" )
        {
            private static final long serialVersionUID = 0L;

            public boolean isValidSyntax( Object value )
            {
                return ((String)value == null) || (((String)value).length() < 7) ;
            }
        } );
        
        MatchingRule matchingRule = new MatchingRule( "1.1.2" );
        matchingRule.setSyntax( syntax );


        matchingRule.setLdapComparator( new LdapComparator<String>( matchingRule.getOid() )
        {
            private static final long serialVersionUID = 0L;

            public int compare( String o1, String o2 )
            {
                return ( ( o1 == null ) ? 
                    ( o2 == null ? 0 : -1 ) :
                    ( o2 == null ? 1 : o1.compareTo( o2 ) ) );
            }
        } );
        
        matchingRule.setNormalizer( new DeepTrimToLowerNormalizer( matchingRule.getOid() ) );
        
        attributeType.setEquality( matchingRule );
        attributeType.setSyntax( syntax );
        
        return attributeType;
    }


    /* No protection */ static AttributeType getBytesAttributeType()
    {
        AttributeType attributeType = new AttributeType( "1.2" );
        LdapSyntax syntax = new LdapSyntax( "1.2.1", "", true );

        syntax.setSyntaxChecker( new SyntaxChecker( "1.2.1" )
        {
            private static final long serialVersionUID = 0L;

            public boolean isValidSyntax( Object value )
            {
                return ( value == null ) || ( ((byte[])value).length < 5 );
            }
        } );

        MatchingRule matchingRule = new MatchingRule( "1.2.2" );
        matchingRule.setSyntax( syntax );

        matchingRule.setLdapComparator( new ByteArrayComparator( "1.2.2" ) );
        
        matchingRule.setNormalizer( new Normalizer( "1.1.1" )
        {
            // The serial UID
            private static final long serialVersionUID = 1L;
            
            public Value<?> normalize( Value<?> value ) throws LdapException
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
                    
                    return new BinaryValue( Strings.trim(newVal) );
                }

                throw new IllegalStateException( I18n.err( I18n.ERR_474 ) );
            }

            public String normalize( String value ) throws LdapException
            {
                throw new IllegalStateException( I18n.err( I18n.ERR_474 ) );
            }
        } );
        
        attributeType.setEquality( matchingRule );
        attributeType.setSyntax( syntax );

        return attributeType;
    }
}
