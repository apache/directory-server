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
package org.apache.directory.server.schema.bootstrap;


import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import javax.naming.NamingException;

import org.apache.directory.server.schema.DnNormalizer;
import org.apache.directory.server.schema.NameAndOptionalUIDNormalizer;
import org.apache.directory.server.schema.registries.Registries;
import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.schema.Normalizer;
import org.apache.directory.shared.ldap.schema.normalizers.BooleanNormalizer;
import org.apache.directory.shared.ldap.schema.normalizers.CachingNormalizer;
import org.apache.directory.shared.ldap.schema.normalizers.DeepTrimNormalizer;
import org.apache.directory.shared.ldap.schema.normalizers.DeepTrimToLowerNormalizer;
import org.apache.directory.shared.ldap.schema.normalizers.NoOpNormalizer;
import org.apache.directory.shared.ldap.schema.normalizers.ObjectIdentifierNormalizer;


/**
 * A bootstrap producer which creates and announces newly created Normalizers
 * for various matchingRules in the core schema.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class SystemNormalizerProducer extends AbstractBootstrapProducer
{
    public SystemNormalizerProducer()
    {
        super( ProducerTypeEnum.NORMALIZER_PRODUCER );
    }

    
    public static class CachingDeepTrimToLowerNormalizer extends CachingNormalizer
    {
        private static final long serialVersionUID = 1L;

        public CachingDeepTrimToLowerNormalizer( String oid )
        {
            super( oid, new DeepTrimToLowerNormalizer( oid) );
        }
    }
    
    
    public static class CachingDeepTrimNormalizer extends CachingNormalizer
    {
        private static final long serialVersionUID = 1L;

        public CachingDeepTrimNormalizer( String oid )
        {
            super( oid, new DeepTrimNormalizer( oid ) );
        }
    }
    
    
    public static class CachingDnNormalizer extends CachingNormalizer
    {
        private static final long serialVersionUID = 1L;

        /** Used for looking up the setRegistries(Registries) method */
        private final static Class<?>[] parameterTypes = new Class<?>[] { Registries.class };

        
        public CachingDnNormalizer( String oid )
        {
            super( oid, DnNormalizer.INSTANCE );
        }

        
        public void setRegistries( Registries registries ) throws NamingException
        {
            injectRegistries( super.normalizer, registries );
        }
        
        
        private void injectRegistries( Object obj, Registries registries ) throws NamingException
        {
            String className = obj.getClass().getName();
            
            try
            {
                Method method = obj.getClass().getMethod( "setRegistries", parameterTypes );
                
                if ( method == null )
                {
                    return;
                }
                
                Object[] args = new Object[] { registries };
                method.invoke( obj, args );
            }
            catch ( SecurityException e )
            {
                NamingException ne = new NamingException( "SyntaxChecker class "+ className 
                    + " could not have the Registries dependency injected." );
                ne.setRootCause( e );
                throw ne;
            }
            catch ( NoSuchMethodException e )
            {
                // this is ok since not every object may have setRegistries()
            }
            catch ( IllegalArgumentException e )
            {
                NamingException ne = new NamingException( "SyntaxChecker class "+ className 
                    + " could not have the Registries dependency injected." );
                ne.setRootCause( e );
                throw ne;
            }
            catch ( IllegalAccessException e )
            {
                NamingException ne = new NamingException( "SyntaxChecker class "+ className 
                    + " could not have the Registries dependency injected." );
                ne.setRootCause( e );
                throw ne;
            }
            catch ( InvocationTargetException e )
            {
                NamingException ne = new NamingException( "SyntaxChecker class "+ className 
                    + " could not have the Registries dependency injected." );
                ne.setRootCause( e );
                throw ne;
            }
        }
    }
    

    public void produce( Registries registries, ProducerCallback cb ) throws NamingException
    {
        Normalizer normalizer;

        /*
         * Straight out of RFC 4517
         * =======================================
         */
        
        /*
         * ( 2.5.13.0 NAME 'objectIdentifierMatch'
         * SYNTAX 1.3.6.1.4.1.1466.115.121.1.38 )
         */
        normalizer = new ObjectIdentifierNormalizer();
        cb.schemaObjectProduced( this, SchemaConstants.OBJECT_IDENTIFIER_MATCH_MR_OID, normalizer );

        /*
         * ( 2.5.13.1 NAME 'distinguishedNameMatch'
         * SYNTAX 1.3.6.1.4.1.1466.115.121.1.12 )
         */
        normalizer = new CachingDnNormalizer();
        ( ( CachingDnNormalizer ) normalizer ).setRegistries( registries );
        cb.schemaObjectProduced( this, SchemaConstants.DISTINGUISHED_NAME_MATCH_MR_OID, normalizer );

        /*
         * ( 2.5.13.2 NAME 'caseIgnoreMatch'
         * SYNTAX 1.3.6.1.4.1.1466.115.121.1.15 )
         */
        normalizer = new CachingDeepTrimToLowerNormalizer();
        cb.schemaObjectProduced( this, SchemaConstants.CASE_IGNORE_MATCH_MR_OID, normalizer );

        /*
         * ( 2.5.13.3 NAME 'caseIgnoreOrderingMatch'
         * SYNTAX 1.3.6.1.4.1.1466.115.121.1.15 )
         */
        normalizer = new CachingDeepTrimToLowerNormalizer();
        cb.schemaObjectProduced( this, SchemaConstants.CASE_IGNORE_ORDERING_MATCH_MR_OID, normalizer );

        /*
         * ( 2.5.13.4 NAME 'caseIgnoreSubstringsMatch'
         * SYNTAX 1.3.6.1.4.1.1466.115.121.1.58 )
         */
        normalizer = new CachingDeepTrimToLowerNormalizer();
        cb.schemaObjectProduced( this, SchemaConstants.CASE_IGNORE_SUBSTRING_MATCH_MR_OID, normalizer );

        /*
         * ( 2.5.13.5 NAME 'caseExactMatch'
         * SYNTAX 1.3.6.1.4.1.1466.115.121.1.15 )
         */
        normalizer = new CachingDeepTrimNormalizer();
        cb.schemaObjectProduced( this, SchemaConstants.CASE_EXACT_MATCH_MR_OID, normalizer );
        
        /*
         * ( 2.5.13.6 NAME 'caseExactOrderingMatch'
         * SYNTAX 1.3.6.1.4.1.1466.115.121.1.15 )
         */
        normalizer = new NoOpNormalizer();
        cb.schemaObjectProduced( this, SchemaConstants.CASE_EXACT_ORDERING_MATCH_MR_OID, normalizer );

        /*
         * ( 2.5.13.7 NAME 'caseExactSubstringsMatch'
         * SYNTAX 1.3.6.1.4.1.1466.115.121.1.58 )
         */
        normalizer = new CachingDeepTrimNormalizer();
        cb.schemaObjectProduced( this, SchemaConstants.CASE_EXACT_SUBSTRING_MATCH_MR_OID, normalizer );

        /*
         * ( 2.5.13.8 NAME 'numericStringMatch'
         * SYNTAX 1.3.6.1.4.1.1466.115.121.1.36 )
         */
        normalizer = new NoOpNormalizer();
        cb.schemaObjectProduced( this, SchemaConstants.NUMERIC_STRING_MATCH_MR_OID, normalizer );

        /*
         * ( 2.5.13.9 NAME 'numericStringOrderingMatch'
         * SYNTAX 1.3.6.1.4.1.1466.115.121.1.36 )
         */
        normalizer = new NoOpNormalizer();
        cb.schemaObjectProduced( this, SchemaConstants.NUMERIC_STRING_ORDERING_MATCH_MR_OID, normalizer );

        /*
         * ( 2.5.13.10 NAME 'numericStringSubstringsMatch'
         * SYNTAX 1.3.6.1.4.1.1466.115.121.1.58 )
         */
        normalizer = new NoOpNormalizer();
        cb.schemaObjectProduced( this, SchemaConstants.NUMERIC_STRING_SUBSTRINGS_MATCH_MR_OID, normalizer );

        /*
         * ( 2.5.13.11 NAME 'caseIgnoreListMatch'
         * SYNTAX 1.3.6.1.4.1.1466.115.121.1.41 )
         */
        normalizer = new CachingDeepTrimToLowerNormalizer();
        cb.schemaObjectProduced( this, SchemaConstants.CASE_IGNORE_LIST_MATCH_MR_OID, normalizer );

        /*
         * ( 2.5.13.12 NAME 'caseIgnoreListSubstringsMatch'
         * SYNTAX 1.3.6.1.4.1.1466.115.121.1.58 )
         */
        normalizer = new CachingDeepTrimToLowerNormalizer();
        cb.schemaObjectProduced( this, SchemaConstants.CASE_IGNORE_LIST_SUBSTRINGS_MATCH_MR_OID, normalizer );

        /*
         * ( 2.5.13.13 NAME 'booleanMatch'
         * SYNTAX 1.3.6.1.4.1.1466.115.121.1.7 )
         */
        normalizer = new BooleanNormalizer();
        cb.schemaObjectProduced( this, SchemaConstants.BOOLEAN_MATCH_MR_OID, normalizer );

        /*
         * ( 2.5.13.14 NAME 'integerMatch'
         * SYNTAX 1.3.6.1.4.1.1466.115.121.1.27 )
         */
        normalizer = new NoOpNormalizer();
        cb.schemaObjectProduced( this, SchemaConstants.INTEGER_MATCH_MR_OID, normalizer );

        /*
         * ( 2.5.13.15 NAME 'integerOrderingMatch'
         * SYNTAX 1.3.6.1.4.1.1466.115.121.1.27 )
         */
        normalizer = new NoOpNormalizer();
        cb.schemaObjectProduced( this, SchemaConstants.INTEGER_ORDERING_MATCH_MR_OID, normalizer );

        /*
         * ( 2.5.13.16 NAME 'bitStringMatch'
         * SYNTAX 1.3.6.1.4.1.1466.115.121.1.6 )
         */
        normalizer = new NoOpNormalizer();
        cb.schemaObjectProduced( this, SchemaConstants.BIT_STRING_MATCH_MR_OID, normalizer );

        /*
         * ( 2.5.13.17 NAME 'octetStringMatch'
         * SYNTAX 1.3.6.1.4.1.1466.115.121.1.40 )
         */
        normalizer = new NoOpNormalizer();
        cb.schemaObjectProduced( this, SchemaConstants.OCTET_STRING_MATCH_MR_OID, normalizer );

        /*
         * ( 2.5.13.18 NAME 'octetStringOrderingMatch'
         * SYNTAX 1.3.6.1.4.1.1466.115.121.1.40 )
         */
        normalizer = new NoOpNormalizer();
        cb.schemaObjectProduced( this, SchemaConstants.OCTET_STRING_ORDERING_MATCH_MR_OID, normalizer );

        /*
         * ( 2.5.13.19 NAME 'octetStringSubstringsMatch'
         * SYNTAX 1.3.6.1.4.1.1466.115.121.1.40 )
         */
        normalizer = new NoOpNormalizer();
        cb.schemaObjectProduced( this, SchemaConstants.OCTET_STRING_SUBSTRINGS_MATCH_MR_OID, normalizer );
        
        /*
         * ( 2.5.13.20 NAME 'telephoneNumberMatch'
         * SYNTAX 1.3.6.1.4.1.1466.115.121.1.50 )
         */
        normalizer = new NoOpNormalizer();
        cb.schemaObjectProduced( this, SchemaConstants.TELEPHONE_NUMBER_MATCH_MR_OID, normalizer );

        /*
         * ( 2.5.13.21 NAME 'telephoneNumberSubstringsMatch'
         * SYNTAX 1.3.6.1.4.1.1466.115.121.1.58 )
         */
        normalizer = new NoOpNormalizer();
        cb.schemaObjectProduced( this, SchemaConstants.TELEPHONE_NUMBER_SUBSTRINGS_MATCH_MR_OID, normalizer );

        /*
         * ( 2.5.13.22 NAME 'presentationAddressMatch'
         * SYNTAX 1.3.6.1.4.1.1466.115.121.1.43 )
         */
        normalizer = new NoOpNormalizer();
        cb.schemaObjectProduced( this, SchemaConstants.PRESENTATION_ADDRESS_MATCH_MATCH_MR_OID, normalizer );

        /*
         * ( 2.5.13.23 NAME 'uniqueMemberMatch'
         * SYNTAX 1.3.6.1.4.1.1466.115.121.1.34 )
         */
        normalizer = NameAndOptionalUIDNormalizer.INSTANCE;
        cb.schemaObjectProduced( this, SchemaConstants.UNIQUE_MEMBER_MATCH_MATCH_MR_OID, normalizer );

        /*
         * ( 2.5.13.24 NAME 'protocolInformationMatch'
         * SYNTAX 1.3.6.1.4.1.1466.115.121.1.42 )
         * 
         * This MatchingRule has been removed from RFC 4517
         */
        normalizer = new CachingDeepTrimNormalizer();
        cb.schemaObjectProduced( this, SchemaConstants.PROTOCOL_INFORMATION_MATCH_MATCH_MR_OID, normalizer );

        // 2.5.13.25 is not defined ...
        
        // 2.5.13.26 is not defined ...
        
        /*
         * ( 2.5.13.27 NAME 'generalizedTimeMatch'
         * SYNTAX 1.3.6.1.4.1.1466.115.121.1.24 )
         */
        normalizer = new CachingDeepTrimNormalizer();
        cb.schemaObjectProduced( this, SchemaConstants.GENERALIZED_TIME_MATCH_MR_OID, normalizer );

        /*
         * ( 2.5.13.28 NAME 'generalizedTimeOrderingMatch'
         * SYNTAX 1.3.6.1.4.1.1466.115.121.1.24 )
         */
        normalizer = new CachingDeepTrimNormalizer();
        cb.schemaObjectProduced( this, SchemaConstants.GENERALIZED_TIME_ORDERING_MATCH_MR_OID, normalizer );

        /*
         * ( 2.5.13.29 NAME 'integerFirstComponentMatch'
         * SYNTAX 1.3.6.1.4.1.1466.115.121.1.27 )
         */
        normalizer = new NoOpNormalizer();
        cb.schemaObjectProduced( this, SchemaConstants.INTEGER_FIRST_COMPONENT_MATCH_MR_OID, normalizer );

        /*
         * ( 2.5.13.30 NAME 'objectIdentifierFirstComponentMatch'
         * SYNTAX 1.3.6.1.4.1.1466.115.121.1.38 )
         */
        normalizer = new NoOpNormalizer();
        cb.schemaObjectProduced( this, SchemaConstants.OBJECT_IDENTIFIER_FIRST_COMPONENT_MATCH_MR_OID, normalizer );

        /*
         * ( 2.5.13.31 NAME 'directoryStringFirstComponentMatch'
         *   SYNTAX 1.3.6.1.4.1.1466.115.121.1.15 )
         */
        normalizer = new NoOpNormalizer();
        cb.schemaObjectProduced( this, SchemaConstants.DIRECTORY_STRING_FIRST_COMPONENT_MATCH_MR_OID, normalizer );

        /*
         * ( 2.5.13.32 NAME 'wordMatch'
         *   SYNTAX 1.3.6.1.4.1.1466.115.121.1.15 )
         */
        normalizer = new NoOpNormalizer();
        cb.schemaObjectProduced( this, SchemaConstants.WORD_MATCH_MR_OID, normalizer );

        /*
         * ( 2.5.13.33 NAME 'keywordMatch'
         *   SYNTAX 1.3.6.1.4.1.1466.115.121.1.15 )
         */
        normalizer = new NoOpNormalizer();
        cb.schemaObjectProduced( this, SchemaConstants.KEYWORD_MATCH_MR_OID, normalizer );

        /*
         * ( 1.3.6.1.4.1.1466.109.114.1 NAME 'caseExactIA5Match'
         * SYNTAX 1.3.6.1.4.1.1466.115.121.1.26 )
         */
        normalizer = new CachingDeepTrimNormalizer();
        cb.schemaObjectProduced( this, SchemaConstants.CASE_EXACT_IA5_MATCH_MR_OID, normalizer );

        /*
         * ( 1.3.6.1.4.1.1466.109.114.2 NAME 'caseIgnoreIA5Match'
         * SYNTAX 1.3.6.1.4.1.1466.115.121.1.26 )
         */
        normalizer = new CachingDeepTrimToLowerNormalizer();
        cb.schemaObjectProduced( this, SchemaConstants.CASE_IGNORE_IA5_MATCH_MR_OID, normalizer );

        /*
         * ( 1.3.6.1.4.1.1466.109.114.3 NAME 'caseIgnoreIA5SubstringsMatch'
         * SYNTAX 1.3.6.1.4.1.1466.115.121.1.58 )
         */

        normalizer = new CachingDeepTrimToLowerNormalizer();
        cb.schemaObjectProduced( this, SchemaConstants.CASE_IGNORE_IA5_SUBSTRINGS_MATCH_MR_OID, normalizer );
    }
}
