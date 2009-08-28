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


import java.util.Comparator;

import javax.naming.NamingException;

import org.apache.directory.server.schema.DnComparator;
import org.apache.directory.server.schema.NameAndOptionalUIDComparator;
import org.apache.directory.server.schema.bootstrap.ProducerTypeEnum;
import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.schema.Normalizer;
import org.apache.directory.shared.ldap.schema.comparators.ByteArrayComparator;
import org.apache.directory.shared.ldap.schema.comparators.ComparableComparator;
import org.apache.directory.shared.ldap.schema.comparators.IntegerOrderingComparator;
import org.apache.directory.shared.ldap.schema.comparators.NormalizingComparator;
import org.apache.directory.shared.ldap.schema.comparators.ObjectIdentifierComparator;
import org.apache.directory.shared.ldap.schema.comparators.TelephoneNumberComparator;
import org.apache.directory.shared.ldap.schema.normalizers.CachingNormalizer;
import org.apache.directory.shared.ldap.schema.normalizers.DeepTrimNormalizer;
import org.apache.directory.shared.ldap.schema.normalizers.DeepTrimToLowerNormalizer;
import org.apache.directory.shared.ldap.schema.normalizers.NameAndOptionalUIDNormalizer;
import org.apache.directory.shared.ldap.schema.registries.Registries;


/**
 * Document this class.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class SystemComparatorProducer extends AbstractBootstrapProducer
{
    public SystemComparatorProducer()
    {
        super( ProducerTypeEnum.COMPARATOR_PRODUCER );
    }

    
    public static class DeepTrimToLowerCachingNormalizingComparator extends NormalizingComparator
    {
        public DeepTrimToLowerCachingNormalizingComparator( String oid )        
        {
            super( new CachingNormalizer( oid, new DeepTrimToLowerNormalizer( oid ) ), new ComparableComparator() );
        }
    }

    
    public static class DeepTrimCachingNormalizingComparator extends NormalizingComparator
    {
        public DeepTrimCachingNormalizingComparator()        
        {
            super( new CachingNormalizer( new DeepTrimNormalizer() ), new ComparableComparator() );
        }
    }

    /**
     * This caching NormalizingComparator would be a good thing to have,
     * sadly we can't use it as the registries are not available here ...
     * 
     *  TODO Inject the AttributeType registry into the caching normalizer.
     */
    public static class NameAndOptionalUIDCachingNormalizingComparator extends NormalizingComparator
    {
        public NameAndOptionalUIDCachingNormalizingComparator()        
        {
            super( new CachingNormalizer( NameAndOptionalUIDNormalizer.INSTANCE ), new NameAndOptionalUIDComparator() );
        }
    }
    
    
    // ------------------------------------------------------------------------
    // BootstrapProducer Methods
    // We need comparators for 
    // o objectIdentifierMatch                2.5.13.0  (ObjectIdentifierComparator)
    // o distinguishedNameMatch               2.5.13.1  (DnComparator)
    // o caseIgnoreMatch                      2.5.13.2  (DeepTrimToLowerCachingNormalizingComparator)
    // o caseIgnoreOrderingMatch              2.5.13.3  (DeepTrimToLowerCachingNormalizingComparator)
    // o caseIgnoreSubstringsMatch            2.5.13.4  (DeepTrimToLowerCachingNormalizingComparator)
    // o caseExactMatch                       2.5.13.5  (DeepTrimCachingNormalizingComparator)
    // o caseExactOrderingMatch               2.5.13.6  (ComparableComparator)
    // o caseExactSubstringsMatch             2.5.13.7  (DeepTrimCachingNormalizingComparator)
    // o numericStringMatch                   2.5.13.8  (ComparableComparator)
    // o numericStringOrderingMatch           2.5.13.9  (ComparableComparator)
    // o numericStringSubstringsMatch         2.5.13.10  (ComparableComparator)
    // o caseIgnoreListMatch                  2.5.13.11  (DeepTrimToLowerCachingNormalizingComparator)
    // o caseIgnoreListSubstringsMatch        2.5.13.12  (DeepTrimToLowerCachingNormalizingComparator)
    // o booleanMatch                         2.5.13.13  (ComparableComparator)
    // o integerMatch                         2.5.13.14  (ComparableComparator)
    // o integerOrderingMatch                 2.5.13.15  (IntegerOrderingComparator)
    // o bitStringMatch                       2.5.13.16  (ComparableComparator)
    // o octetStringMatch                     2.5.13.17  (ByteArrayComparator)
    // o octetStringOrderingMatch             2.5.13.18  (ByteArrayComparator)
    // o octetStringSubstringsMatch           2.5.13.19  (ByteArrayComparator)
    // o telephoneNumberMatch                 2.5.13.20  (TelephoneNumberComparator)
    // o telephoneNumberSubstringsMatch       2.5.13.21  (ComparableComparator)
    // o presentationAddressMatch             2.5.13.22  (ComparableComparator)
    // o uniqueMemberMatch                    2.5.13.23  (NameAndOptionalUIDComparator)
    // o protocolInformationMatch             2.5.13.24  (ComparableComparator)
    // o generalizedTimeMatch                 2.5.13.27  (ComparableComparator)
    // o generalizedTimeOrderingMatch         2.5.13.28  (ComparableComparator)
    // o integerFirstComponentMatch           2.5.13.29  (ComparableComparator)
    // o objectIdentifierFirstComponentMatch  2.5.13.30  (ComparableComparator)
    // o directoryStringFirstComponentMatch   2.5.13.31  (ComparableComparator)
    // o wordMatch                            2.5.13.32  (ComparableComparator)
    // o keywordMatch                         2.5.13.33  (ComparableComparator)
    // o caseExactIA5Match                    1.3.6.1.4.1.1466.109.114.1  (DeepTrimCachingNormalizingComparator)
    // o caseIgnoreIA5Match                   1.3.6.1.4.1.1466.109.114.2  (DeepTrimToLowerCachingNormalizingComparator)
    // o caseIgnoreIA5SubstringsMatch         1.3.6.1.4.1.1466.109.114.3  (DeepTrimToLowerCachingNormalizingComparator)
    // ------------------------------------------------------------------------
    
    
    /**
     * {@inheritDoc}
     */
    public void produce( Registries registries, ProducerCallback cb ) throws NamingException
    {
        Comparator comparator;

        /*
         * Straight out of RFC 4517
         * =======================================
         * ( 2.5.13.0 NAME 'objectIdentifierMatch'
         * SYNTAX 1.3.6.1.4.1.1466.115.121.1.38 )
         */
        comparator = new ObjectIdentifierComparator();
        cb.schemaObjectProduced( this, SchemaConstants.OBJECT_IDENTIFIER_MATCH_MR_OID, comparator );

        /*
         * ( 2.5.13.1 NAME 'distinguishedNameMatch'
         * SYNTAX 1.3.6.1.4.1.1466.115.121.1.12 )
         */
        comparator = new DnComparator( registries.getAttributeTypeRegistry() );
        cb.schemaObjectProduced( this, SchemaConstants.DISTINGUISHED_NAME_MATCH_MR_OID, comparator );

        /*
         * ( 2.5.13.2 NAME 'caseIgnoreMatch'
         * SYNTAX 1.3.6.1.4.1.1466.115.121.1.15 )
         */
        comparator = new DeepTrimToLowerCachingNormalizingComparator();
        cb.schemaObjectProduced( this, SchemaConstants.CASE_IGNORE_MATCH_MR_OID, comparator );

        /*
         * ( 2.5.13.3 NAME 'caseIgnoreOrderingMatch'
         * SYNTAX 1.3.6.1.4.1.1466.115.121.1.15 )
         */
        comparator = new DeepTrimToLowerCachingNormalizingComparator();
        cb.schemaObjectProduced( this, SchemaConstants.CASE_IGNORE_ORDERING_MATCH_MR_OID, comparator );

        /*
         * ( 2.5.13.4 NAME 'caseIgnoreSubstringsMatch'
         * SYNTAX 1.3.6.1.4.1.1466.115.121.1.58 )
         */
        comparator = new DeepTrimToLowerCachingNormalizingComparator();
        cb.schemaObjectProduced( this, SchemaConstants.CASE_IGNORE_SUBSTRING_MATCH_MR_OID, comparator );

        /*
         * ( 2.5.13.5 NAME 'caseExactMatch'
         * SYNTAX 1.3.6.1.4.1.1466.115.121.1.15 )
         */
        comparator = new DeepTrimCachingNormalizingComparator();
        cb.schemaObjectProduced( this, SchemaConstants.CASE_EXACT_MATCH_MR_OID, comparator );
        
        /*
         * ( 2.5.13.6 NAME 'caseExactOrderingMatch'
         * SYNTAX 1.3.6.1.4.1.1466.115.121.1.15 )
         */
        comparator = new ComparableComparator();
        cb.schemaObjectProduced( this, SchemaConstants.CASE_EXACT_ORDERING_MATCH_MR_OID, comparator );

        /*
         * ( 2.5.13.7 NAME 'caseExactSubstringsMatch'
         * SYNTAX 1.3.6.1.4.1.1466.115.121.1.58 )
         */
        comparator = new DeepTrimCachingNormalizingComparator();
        cb.schemaObjectProduced( this, SchemaConstants.CASE_EXACT_SUBSTRING_MATCH_MR_OID, comparator );

        /*
         * ( 2.5.13.8 NAME 'numericStringMatch'
         * SYNTAX 1.3.6.1.4.1.1466.115.121.1.36 )
         */
        comparator = new ComparableComparator();
        cb.schemaObjectProduced( this, SchemaConstants.NUMERIC_STRING_MATCH_MR_OID, comparator );

        /*
         * ( 2.5.13.9 NAME 'numericStringOrderingMatch'
         * SYNTAX 1.3.6.1.4.1.1466.115.121.1.36 )
         */
        comparator = new ComparableComparator();
        cb.schemaObjectProduced( this, SchemaConstants.NUMERIC_STRING_ORDERING_MATCH_MR_OID, comparator );

        /*
         * ( 2.5.13.10 NAME 'numericStringSubstringsMatch'
         * SYNTAX 1.3.6.1.4.1.1466.115.121.1.58 )
         */
        comparator = new ComparableComparator();
        cb.schemaObjectProduced( this, SchemaConstants.NUMERIC_STRING_SUBSTRINGS_MATCH_MR_OID, comparator );

        /*
         * ( 2.5.13.11 NAME 'caseIgnoreListMatch'
         * SYNTAX 1.3.6.1.4.1.1466.115.121.1.41 )
         */
        comparator = new DeepTrimToLowerCachingNormalizingComparator();
        cb.schemaObjectProduced( this, SchemaConstants.CASE_IGNORE_LIST_MATCH_MR_OID, comparator );

        /*
         * ( 2.5.13.12 NAME 'caseIgnoreListSubstringsMatch'
         * SYNTAX 1.3.6.1.4.1.1466.115.121.1.58 )
         */
        comparator = new DeepTrimToLowerCachingNormalizingComparator();
        cb.schemaObjectProduced( this, SchemaConstants.CASE_IGNORE_LIST_SUBSTRINGS_MATCH_MR_OID, comparator );

        /*
         * ( 2.5.13.13 NAME 'booleanMatch'
         * SYNTAX 1.3.6.1.4.1.1466.115.121.1.7 )
         */
        comparator = new ComparableComparator();
        cb.schemaObjectProduced( this, SchemaConstants.BOOLEAN_MATCH_MR_OID, comparator );
        
        /*
         * ( 2.5.13.14 NAME 'integerMatch'
         * SYNTAX 1.3.6.1.4.1.1466.115.121.1.27 )
         */
        comparator = new ComparableComparator();
        cb.schemaObjectProduced( this, SchemaConstants.INTEGER_MATCH_MR_OID, comparator );

        /*
         * ( 2.5.13.15 NAME 'integerOrderingMatch'
         * SYNTAX 1.3.6.1.4.1.1466.115.121.1.27 )
         */
        comparator = new IntegerOrderingComparator();
        cb.schemaObjectProduced( this, SchemaConstants.INTEGER_ORDERING_MATCH_MR_OID, comparator );

        /*
         * ( 2.5.13.16 NAME 'bitStringMatch'
         * SYNTAX 1.3.6.1.4.1.1466.115.121.1.6 )
         */
        comparator = new ComparableComparator();
        cb.schemaObjectProduced( this, SchemaConstants.BIT_STRING_MATCH_MR_OID, comparator );

        /*
         * ( 2.5.13.17 NAME 'octetStringMatch'
         * SYNTAX 1.3.6.1.4.1.1466.115.121.1.40 )
         */
        comparator = new ByteArrayComparator();
        cb.schemaObjectProduced( this, SchemaConstants.OCTET_STRING_MATCH_MR_OID, comparator );

        /*
         * ( 2.5.13.18 NAME 'octetStringOrderingMatch'
         * SYNTAX 1.3.6.1.4.1.1466.115.121.1.40 )
         */
        comparator = new ByteArrayComparator();
        cb.schemaObjectProduced( this, SchemaConstants.OCTET_STRING_ORDERING_MATCH_MR_OID, comparator );

        /*
         * ( 2.5.13.19 NAME 'octetStringSubstringsMatch'
         * SYNTAX 1.3.6.1.4.1.1466.115.121.1.40 )
         */
        comparator = new ByteArrayComparator();
        cb.schemaObjectProduced( this, SchemaConstants.OCTET_STRING_SUBSTRINGS_MATCH_MR_OID, comparator );
       
        /*
         * ( 2.5.13.20 NAME 'telephoneNumberMatch'
         * SYNTAX 1.3.6.1.4.1.1466.115.121.1.50 )
         */
        comparator = new TelephoneNumberComparator();
        cb.schemaObjectProduced( this, SchemaConstants.TELEPHONE_NUMBER_MATCH_MR_OID, comparator );

        /*
         * ( 2.5.13.21 NAME 'telephoneNumberSubstringsMatch'
         * SYNTAX 1.3.6.1.4.1.1466.115.121.1.58 )
         */
        comparator = new ComparableComparator();
        cb.schemaObjectProduced( this, SchemaConstants.TELEPHONE_NUMBER_SUBSTRINGS_MATCH_MR_OID, comparator );

        /*
         * ( 2.5.13.22 NAME 'presentationAddressMatch'
         * SYNTAX 1.3.6.1.4.1.1466.115.121.1.43 )
         */
        comparator = new ComparableComparator();
        cb.schemaObjectProduced( this, SchemaConstants.PRESENTATION_ADDRESS_MATCH_MATCH_MR_OID, comparator );

        /*
         * ( 2.5.13.23 NAME 'uniqueMemberMatch'
         * SYNTAX 1.3.6.1.4.1.1466.115.121.1.34 )
         */
        comparator = new NameAndOptionalUIDComparator();
        cb.schemaObjectProduced( this,SchemaConstants.UNIQUE_MEMBER_MATCH_MATCH_MR_OID, comparator );

        /*
         * ( 2.5.13.24 NAME 'protocolInformationMatch'
         * SYNTAX 1.3.6.1.4.1.1466.115.121.1.42 )
         */
        comparator = new ComparableComparator();
        cb.schemaObjectProduced( this, SchemaConstants.PROTOCOL_INFORMATION_MATCH_MATCH_MR_OID, comparator );

        // 2.5.13.25 is not defined...
        
        // 2.5.13.26 is not defined...

        /*
         * ( 2.5.13.27 NAME 'generalizedTimeMatch'
         * SYNTAX 1.3.6.1.4.1.1466.115.121.1.24 )
         */
        comparator = new ComparableComparator();
        cb.schemaObjectProduced( this, SchemaConstants.GENERALIZED_TIME_MATCH_MR_OID, comparator );

        /*
         * ( 2.5.13.28 NAME 'generalizedTimeOrderingMatch'
         * SYNTAX 1.3.6.1.4.1.1466.115.121.1.24 )
         */
        comparator = new ComparableComparator();
        cb.schemaObjectProduced( this, SchemaConstants.GENERALIZED_TIME_ORDERING_MATCH_MR_OID, comparator );

        /*
         * ( 2.5.13.29 NAME 'integerFirstComponentMatch'
         * SYNTAX 1.3.6.1.4.1.1466.115.121.1.27 )
         */
        comparator = new ComparableComparator();
        cb.schemaObjectProduced( this, SchemaConstants.INTEGER_FIRST_COMPONENT_MATCH_MR_OID, comparator );

        /*
         * ( 2.5.13.30 NAME 'objectIdentifierFirstComponentMatch'
         * SYNTAX 1.3.6.1.4.1.1466.115.121.1.38 )
         */
        comparator = new ComparableComparator();
        cb.schemaObjectProduced( this, SchemaConstants.OBJECT_IDENTIFIER_FIRST_COMPONENT_MATCH_MR_OID, comparator );

        /*
         * ( 2.5.13.31 NAME 'directoryStringFirstComponentMatch'
         *   SYNTAX 1.3.6.1.4.1.1466.115.121.1.15 )
         */
        comparator = new ComparableComparator();
        cb.schemaObjectProduced( this, SchemaConstants.DIRECTORY_STRING_FIRST_COMPONENT_MATCH_MR_OID, comparator );

        /*
         * ( 2.5.13.32 NAME 'wordMatch' 
         * SYNTAX 1.3.6.1.4.1.1466.115.121.1.15 )
         */
        comparator = new ComparableComparator();
        cb.schemaObjectProduced( this, SchemaConstants.WORD_MATCH_MR_OID, comparator );

        /*
         * ( 2.5.13.33 NAME 'keywordMatch' 
         * SYNTAX 1.3.6.1.4.1.1466.115.121.1.15 )
         */
        comparator = new ComparableComparator();
        cb.schemaObjectProduced( this, SchemaConstants.KEYWORD_MATCH_MR_OID, comparator );

        /*
         * ( 1.3.6.1.4.1.1466.109.114.1 NAME 'caseExactIA5Match'
         * SYNTAX 1.3.6.1.4.1.1466.115.121.1.26 )
         */
        comparator = new DeepTrimCachingNormalizingComparator();
        cb.schemaObjectProduced( this, SchemaConstants.CASE_EXACT_IA5_MATCH_MR_OID, comparator );

        /*
         * ( 1.3.6.1.4.1.1466.109.114.2 NAME 'caseIgnoreIA5Match'
         * SYNTAX 1.3.6.1.4.1.1466.115.121.1.26 )
         */
        comparator = new DeepTrimToLowerCachingNormalizingComparator();
        cb.schemaObjectProduced( this, SchemaConstants.CASE_IGNORE_IA5_MATCH_MR_OID, comparator );

        /*
         * ( 1.3.6.1.4.1.1466.109.114.3 NAME 'caseIgnoreIA5SubstringsMatch'
         * SYNTAX 1.3.6.1.4.1.1466.115.121.1.58 )
         */
        comparator = new DeepTrimToLowerCachingNormalizingComparator();
        cb.schemaObjectProduced( this, SchemaConstants.CASE_IGNORE_IA5_SUBSTRINGS_MATCH_MR_OID, comparator );
    }
}
