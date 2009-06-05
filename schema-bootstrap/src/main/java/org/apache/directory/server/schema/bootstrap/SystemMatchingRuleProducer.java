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


import javax.naming.NamingException;

import org.apache.directory.server.schema.bootstrap.ProducerTypeEnum;
import org.apache.directory.server.schema.registries.Registries;
import org.apache.directory.shared.ldap.constants.SchemaConstants;


/**
 * A simple matching rule configuration where objects and java code are used
 * to create matching rules.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class SystemMatchingRuleProducer extends AbstractBootstrapProducer
{
    public SystemMatchingRuleProducer()
    {
        super( ProducerTypeEnum.MATCHING_RULE_PRODUCER );
    }


    public void produce( Registries registries, ProducerCallback cb ) throws NamingException
    {
        BootstrapMatchingRule mrule = null;

        /*
         * Straight out of RFC 4517
         * Straight out of RFC 3698: Section 2.3
         * http://www.faqs.org/rfcs/rfc3698.html
         * =======================================
         */
        /*
         * ( 2.5.13.0 NAME 'objectIdentifierMatch'
         * SYNTAX 1.3.6.1.4.1.1466.115.121.1.38 )
         */
        mrule = new BootstrapMatchingRule( SchemaConstants.OBJECT_IDENTIFIER_MATCH_MR_OID, registries );
        mrule.setNames( new String[]
            { SchemaConstants.OBJECT_IDENTIFIER_MATCH_MR } );
        mrule.setSyntaxOid( SchemaConstants.OID_SYNTAX );
        cb.schemaObjectProduced( this, mrule.getOid(), mrule );

        /*
         * ( 2.5.13.1 NAME 'distinguishedNameMatch'
         * SYNTAX 1.3.6.1.4.1.1466.115.121.1.12 )
         */
        mrule = new BootstrapMatchingRule( SchemaConstants.DISTINGUISHED_NAME_MATCH_MR_OID, registries );
        mrule.setNames( new String[]
            { SchemaConstants.DISTINGUISHED_NAME_MATCH_MR } );
        mrule.setSyntaxOid( SchemaConstants.DN_SYNTAX );
        cb.schemaObjectProduced( this, mrule.getOid(), mrule );

        /*
         * ( 2.5.13.2 NAME 'caseIgnoreMatch'
         * SYNTAX 1.3.6.1.4.1.1466.115.121.1.15 )
         */
        mrule = new BootstrapMatchingRule( SchemaConstants.CASE_IGNORE_MATCH_MR_OID, registries );
        mrule.setNames( new String[]
            { SchemaConstants.CASE_IGNORE_MATCH_MR } );
        mrule.setSyntaxOid( SchemaConstants.DIRECTORY_STRING_SYNTAX );
        cb.schemaObjectProduced( this, mrule.getOid(), mrule );

        /*
         * ( 2.5.13.3 NAME 'caseIgnoreOrderingMatch'
         * SYNTAX 1.3.6.1.4.1.1466.115.121.1.15 )
         */
        mrule = new BootstrapMatchingRule( SchemaConstants.CASE_IGNORE_ORDERING_MATCH_MR_OID, registries );
        mrule.setNames( new String[]
            { SchemaConstants.CASE_IGNORE_ORDERING_MATCH_MR } );
        mrule.setSyntaxOid( SchemaConstants.DIRECTORY_STRING_SYNTAX );
        cb.schemaObjectProduced( this, mrule.getOid(), mrule );

        /*
         * ( 2.5.13.4 NAME 'caseIgnoreSubstringsMatch'
         * SYNTAX 1.3.6.1.4.1.1466.115.121.1.58 )
         */
        mrule = new BootstrapMatchingRule( SchemaConstants.CASE_IGNORE_SUBSTRING_MATCH_MR_OID, registries );
        mrule.setNames( new String[]
            { SchemaConstants.CASE_IGNORE_SUBSTRING_MATCH_MR } );
        mrule.setSyntaxOid( SchemaConstants.SUBSTRING_ASSERTION_SYNTAX );
        cb.schemaObjectProduced( this, mrule.getOid(), mrule );

        /*
         * ( 2.5.13.5 NAME 'caseExactMatch'
         * SYNTAX 1.3.6.1.4.1.1466.115.121.1.15 )
         */
        mrule = new BootstrapMatchingRule( SchemaConstants.CASE_EXACT_MATCH_MR_OID, registries );
        mrule.setNames( new String[]
            { SchemaConstants.CASE_EXACT_MATCH_MR } );
        mrule.setSyntaxOid( SchemaConstants.DIRECTORY_STRING_SYNTAX );
        cb.schemaObjectProduced( this, mrule.getOid(), mrule );

        /*
         * ( 2.5.13.6 NAME 'caseExactOrderingMatch'
         * SYNTAX 1.3.6.1.4.1.1466.115.121.1.15 )
         */
        mrule = new BootstrapMatchingRule( SchemaConstants.CASE_EXACT_ORDERING_MATCH_MR_OID, registries );
        mrule.setNames( new String[]
            { SchemaConstants.CASE_EXACT_ORDERING_MATCH_MR } );
        mrule.setSyntaxOid( SchemaConstants.DIRECTORY_STRING_SYNTAX );
        cb.schemaObjectProduced( this, mrule.getOid(), mrule );
        
        /*
         * ( 2.5.13.7 NAME 'caseExactSubstringsMatch'
         * SYNTAX 1.3.6.1.4.1.1466.115.121.1.58 )
         */
        mrule = new BootstrapMatchingRule( SchemaConstants.CASE_EXACT_SUBSTRING_MATCH_MR_OID, registries );
        mrule.setNames( new String[]
            { SchemaConstants.CASE_EXACT_SUBSTRING_MATCH_MR } );
        mrule.setSyntaxOid( SchemaConstants.SUBSTRING_ASSERTION_SYNTAX );
        cb.schemaObjectProduced( this, mrule.getOid(), mrule );
        
        /*
         * ( 2.5.13.8 NAME 'numericStringMatch'
         * SYNTAX 1.3.6.1.4.1.1466.115.121.1.36 )
         */
        mrule = new BootstrapMatchingRule( SchemaConstants.NUMERIC_STRING_MATCH_MR_OID, registries );
        mrule.setNames( new String[]
            { SchemaConstants.NUMERIC_STRING_MATCH_MR } );
        mrule.setSyntaxOid( SchemaConstants.NUMERIC_STRING_SYNTAX );
        cb.schemaObjectProduced( this, mrule.getOid(), mrule );

        /*
         * ( 2.5.13.9 NAME 'numericStringOrderingMatch'
         * SYNTAX 1.3.6.1.4.1.1466.115.121.1.36 )
         */
        mrule = new BootstrapMatchingRule( SchemaConstants.NUMERIC_STRING_ORDERING_MATCH_MR_OID, registries );
        mrule.setNames( new String[]
            { SchemaConstants.NUMERIC_STRING_ORDERING_MATCH_MR } );
        mrule.setSyntaxOid( SchemaConstants.NUMERIC_STRING_SYNTAX );
        cb.schemaObjectProduced( this, mrule.getOid(), mrule );
        
        /*
         * ( 2.5.13.10 NAME 'numericStringSubstringsMatch'
         * SYNTAX 1.3.6.1.4.1.1466.115.121.1.58 )
         */
        mrule = new BootstrapMatchingRule( SchemaConstants.NUMERIC_STRING_SUBSTRINGS_MATCH_MR_OID, registries );
        mrule.setNames( new String[]
            { SchemaConstants.NUMERIC_STRING_SUBSTRINGS_MATCH_MR } );
        mrule.setSyntaxOid( SchemaConstants.SUBSTRING_ASSERTION_SYNTAX );
        cb.schemaObjectProduced( this, mrule.getOid(), mrule );

        /*
         * ( 2.5.13.11 NAME 'caseIgnoreListMatch'
         * SYNTAX 1.3.6.1.4.1.1466.115.121.1.41 )
         */
        mrule = new BootstrapMatchingRule( SchemaConstants.CASE_IGNORE_LIST_MATCH_MR_OID, registries );
        mrule.setNames( new String[]
            { SchemaConstants.CASE_IGNORE_LIST_MATCH_MR } );
        mrule.setSyntaxOid( SchemaConstants.POSTAL_ADDRESS_SYNTAX );
        cb.schemaObjectProduced( this, mrule.getOid(), mrule );

        /*
         * ( 2.5.13.12 NAME 'caseIgnoreListSubstringsMatch'
         * SYNTAX 1.3.6.1.4.1.1466.115.121.1.58 )
         */
        mrule = new BootstrapMatchingRule( SchemaConstants.CASE_IGNORE_LIST_SUBSTRINGS_MATCH_MR_OID, registries );
        mrule.setNames( new String[]
            { SchemaConstants.CASE_IGNORE_LIST_SUBSTRINGS_MATCH_MR } );
        mrule.setSyntaxOid( SchemaConstants.SUBSTRING_ASSERTION_SYNTAX );
        cb.schemaObjectProduced( this, mrule.getOid(), mrule );

        /*
         * ( 2.5.13.13 NAME 'booleanMatch'
         * SYNTAX 1.3.6.1.4.1.1466.115.121.1.7 )
         */
        mrule = new BootstrapMatchingRule( SchemaConstants.BOOLEAN_MATCH_MR_OID, registries );
        mrule.setNames( new String[]
            { SchemaConstants.BOOLEAN_MATCH_MR } );
        mrule.setSyntaxOid( SchemaConstants.BOOLEAN_SYNTAX );
        cb.schemaObjectProduced( this, mrule.getOid(), mrule );

        /*
         * ( 2.5.13.14 NAME 'integerMatch'
         * SYNTAX 1.3.6.1.4.1.1466.115.121.1.27 )
         */
        mrule = new BootstrapMatchingRule( SchemaConstants.INTEGER_MATCH_MR_OID, registries );
        mrule.setNames( new String[]
            { SchemaConstants.INTEGER_MATCH_MR } );
        mrule.setSyntaxOid( SchemaConstants.INTEGER_SYNTAX );
        cb.schemaObjectProduced( this, mrule.getOid(), mrule );

        /*
         * ( 2.5.13.15 NAME 'integerOrderingMatch'
         * SYNTAX 1.3.6.1.4.1.1466.115.121.1.27 )
         */
        mrule = new BootstrapMatchingRule( SchemaConstants.INTEGER_ORDERING_MATCH_MR_OID, registries );
        mrule.setNames( new String[]
            { SchemaConstants.INTEGER_ORDERING_MATCH_MR } );
        mrule.setSyntaxOid( SchemaConstants.INTEGER_SYNTAX );
        cb.schemaObjectProduced( this, mrule.getOid(), mrule );

        /*
         * ( 2.5.13.16 NAME 'bitStringMatch' 
         * SYNTAX 1.3.6.1.4.1.1466.115.121.1.6 )
         */
        mrule = new BootstrapMatchingRule( SchemaConstants.BIT_STRING_MATCH_MR_OID, registries );
        mrule.setNames( new String[]
            { SchemaConstants.BIT_STRING_MATCH_MR } );
        mrule.setSyntaxOid( SchemaConstants.BIT_STRING_SYNTAX );
        cb.schemaObjectProduced( this, mrule.getOid(), mrule );

        /*
         * ( 2.5.13.17 NAME 'octetStringMatch' 
         * SYNTAX 1.3.6.1.4.1.1466.115.121.1.40 )
         */
        mrule = new BootstrapMatchingRule( SchemaConstants.OCTET_STRING_MATCH_MR_OID, registries );
        mrule.setNames( new String[]
            { SchemaConstants.OCTET_STRING_MATCH_MR } );
        mrule.setSyntaxOid( SchemaConstants.OCTET_STRING_SYNTAX );
        cb.schemaObjectProduced( this, mrule.getOid(), mrule );

        /*
         * ( 2.5.13.18 NAME 'octetStringOrderingMatch'
         * SYNTAX 1.3.6.1.4.1.1466.115.121.1.40 )
         */
        mrule = new BootstrapMatchingRule( SchemaConstants.OCTET_STRING_ORDERING_MATCH_MR_OID, registries );
        mrule.setNames( new String[]
            { SchemaConstants.OCTET_STRING_ORDERING_MATCH_MR } );
        mrule.setSyntaxOid( SchemaConstants.OCTET_STRING_SYNTAX );
        cb.schemaObjectProduced( this, mrule.getOid(), mrule );

        /*
         * ( 2.5.13.19 NAME 'octetStringSubstringsMatch'
         * SYNTAX 1.3.6.1.4.1.1466.115.121.1.40 )
         */
        mrule = new BootstrapMatchingRule( SchemaConstants.OCTET_STRING_SUBSTRINGS_MATCH_MR_OID, registries );
        mrule.setNames( new String[]
            { SchemaConstants.OCTET_STRING_SUBSTRINGS_MATCH_MR } );
        mrule.setSyntaxOid( SchemaConstants.OCTET_STRING_SYNTAX );
        cb.schemaObjectProduced( this, mrule.getOid(), mrule );
        
        /*
         * ( 2.5.13.20 NAME 'telephoneNumberMatch'
         * SYNTAX 1.3.6.1.4.1.1466.115.121.1.50 )
         */
        mrule = new BootstrapMatchingRule( SchemaConstants.TELEPHONE_NUMBER_MATCH_MR_OID, registries );
        mrule.setNames( new String[]
            { SchemaConstants.TELEPHONE_NUMBER_MATCH_MR } );
        mrule.setSyntaxOid( SchemaConstants.TELEPHONE_NUMBER_SYNTAX );
        cb.schemaObjectProduced( this, mrule.getOid(), mrule );

        /*
         * ( 2.5.13.21 NAME 'telephoneNumberSubstringsMatch'
         * SYNTAX 1.3.6.1.4.1.1466.115.121.1.58 )
         */
        mrule = new BootstrapMatchingRule( SchemaConstants.TELEPHONE_NUMBER_SUBSTRINGS_MATCH_MR_OID, registries );
        mrule.setNames( new String[]
            { SchemaConstants.TELEPHONE_NUMBER_SUBSTRINGS_MATCH_MR } );
        mrule.setSyntaxOid( SchemaConstants.SUBSTRING_ASSERTION_SYNTAX );
        cb.schemaObjectProduced( this, mrule.getOid(), mrule );

        /*
         * This MatchingRule is deprecated
         * 
         * ( 2.5.13.22 NAME 'presentationAddressMatch'
         * SYNTAX 1.3.6.1.4.1.1466.115.121.1.43 )
         */
        mrule = new BootstrapMatchingRule( SchemaConstants.PRESENTATION_ADDRESS_MATCH_MATCH_MR_OID, registries );
        mrule.setNames( new String[]
            { SchemaConstants.PRESENTATION_ADDRESS_MATCH_MATCH_MR } );
        mrule.setSyntaxOid( SchemaConstants.PRESENTATION_ADDRESS_SYNTAX );
        cb.schemaObjectProduced( this, mrule.getOid(), mrule );

        /*
         * ( 2.5.13.23 NAME 'uniqueMemberMatch'
         * SYNTAX 1.3.6.1.4.1.1466.115.121.1.34 )
         */
        mrule = new BootstrapMatchingRule( SchemaConstants.UNIQUE_MEMBER_MATCH_MATCH_MR_OID, registries );
        mrule.setNames( new String[]
            { SchemaConstants.UNIQUE_MEMBER_MATCH_MATCH_MR } );
        mrule.setSyntaxOid( SchemaConstants.NAME_AND_OPTIONAL_UID_SYNTAX );
        cb.schemaObjectProduced( this, mrule.getOid(), mrule );

        /*
         * This MatchingRule is deprecated
         * 
         * ( 2.5.13.24 NAME 'protocolInformationMatch'
         * SYNTAX 1.3.6.1.4.1.1466.115.121.1.42 )
         */
        mrule = new BootstrapMatchingRule( SchemaConstants.PROTOCOL_INFORMATION_MATCH_MATCH_MR_OID, registries );
        mrule.setNames( new String[]
            { SchemaConstants.PROTOCOL_INFORMATION_MATCH_MATCH_MR } );
        mrule.setSyntaxOid( SchemaConstants.PROTOCOL_INFORMATION_SYNTAX );
        cb.schemaObjectProduced( this, mrule.getOid(), mrule );

        // "2.5.13.25" is not used ...
        
        // "2.5.13.26" is not used ...

        /*
         * ( 2.5.13.27 NAME 'generalizedTimeMatch'
         * SYNTAX 1.3.6.1.4.1.1466.115.121.1.24 )
         */
        mrule = new BootstrapMatchingRule( SchemaConstants.GENERALIZED_TIME_MATCH_MR_OID, registries );
        mrule.setNames( new String[]
            { SchemaConstants.GENERALIZED_TIME_MATCH_MR } );
        mrule.setSyntaxOid( SchemaConstants.GENERALIZED_TIME_SYNTAX );
        cb.schemaObjectProduced( this, mrule.getOid(), mrule );

        /*
         * ( 2.5.13.28 NAME 'generalizedTimeOrderingMatch'
         * SYNTAX 1.3.6.1.4.1.1466.115.121.1.24 )
         */
        mrule = new BootstrapMatchingRule( SchemaConstants.GENERALIZED_TIME_ORDERING_MATCH_MR_OID, registries );
        mrule.setNames( new String[]
            { SchemaConstants.GENERALIZED_TIME_ORDERING_MATCH_MR } );
        mrule.setSyntaxOid( SchemaConstants.GENERALIZED_TIME_SYNTAX );
        cb.schemaObjectProduced( this, mrule.getOid(), mrule );

        /*
         * ( 2.5.13.29 NAME 'integerFirstComponentMatch'
         * SYNTAX 1.3.6.1.4.1.1466.115.121.1.27 )
         */
        mrule = new BootstrapMatchingRule( SchemaConstants.INTEGER_FIRST_COMPONENT_MATCH_MR_OID, registries );
        mrule.setNames( new String[]
            { SchemaConstants.INTEGER_FIRST_COMPONENT_MATCH_MR } );
        mrule.setSyntaxOid( SchemaConstants.INTEGER_SYNTAX );
        cb.schemaObjectProduced( this, mrule.getOid(), mrule );

        /*
         * ( 2.5.13.30 NAME 'objectIdentifierFirstComponentMatch'
         * SYNTAX 1.3.6.1.4.1.1466.115.121.1.38 )
         */
        mrule = new BootstrapMatchingRule( SchemaConstants.OBJECT_IDENTIFIER_FIRST_COMPONENT_MATCH_MR_OID, registries );
        mrule.setNames( new String[]
            { SchemaConstants.OBJECT_IDENTIFIER_FIRST_COMPONENT_MATCH_MR } );
        mrule.setSyntaxOid( SchemaConstants.OID_SYNTAX );
        cb.schemaObjectProduced( this, mrule.getOid(), mrule );

        /*
         * ( 2.5.13.31 NAME 'directoryStringFirstComponentMatch'
         *   SYNTAX 1.3.6.1.4.1.1466.115.121.1.15 )
         */
        mrule = new BootstrapMatchingRule( SchemaConstants.DIRECTORY_STRING_FIRST_COMPONENT_MATCH_MR_OID, registries );
        mrule.setNames( new String[]
            { SchemaConstants.DIRECTORY_STRING_FIRST_COMPONENT_MATCH_MR } );
        mrule.setSyntaxOid( SchemaConstants.DIRECTORY_STRING_SYNTAX );
        cb.schemaObjectProduced( this, mrule.getOid(), mrule );

        /*
         * ( 2.5.13.32 NAME 'wordMatch'
         * SYNTAX 1.3.6.1.4.1.1466.115.121.1.40 )
         */
        mrule = new BootstrapMatchingRule( SchemaConstants.WORD_MATCH_MR_OID, registries );
        mrule.setNames( new String[]
            { SchemaConstants.WORD_MATCH_MR } );
        mrule.setSyntaxOid( SchemaConstants.DIRECTORY_STRING_SYNTAX );
        cb.schemaObjectProduced( this, mrule.getOid(), mrule );

        /*
         * ( 2.5.13.33 NAME 'keywordMatch'
         * SYNTAX 1.3.6.1.4.1.1466.115.121.1.40 )
         */
        mrule = new BootstrapMatchingRule( SchemaConstants.KEYWORD_MATCH_MR_OID, registries );
        mrule.setNames( new String[]
            { SchemaConstants.KEYWORD_MATCH_MR } );
        mrule.setSyntaxOid( SchemaConstants.DIRECTORY_STRING_SYNTAX );
        cb.schemaObjectProduced( this, mrule.getOid(), mrule );

        /*
         * ( 1.3.6.1.4.1.1466.109.114.1 NAME 'caseExactIA5Match'
         * SYNTAX 1.3.6.1.4.1.1466.115.121.1.26 )
         */
        mrule = new BootstrapMatchingRule( SchemaConstants.CASE_EXACT_IA5_MATCH_MR_OID, registries );
        mrule.setNames( new String[]
            { SchemaConstants.CASE_EXACT_IA5_MATCH_MR } );
        mrule.setSyntaxOid( SchemaConstants.IA5_STRING_SYNTAX );
        cb.schemaObjectProduced( this, mrule.getOid(), mrule );

        /*
         * ( 1.3.6.1.4.1.1466.109.114.2 NAME 'caseIgnoreIA5Match'
         * SYNTAX 1.3.6.1.4.1.1466.115.121.1.26 )
         */
        mrule = new BootstrapMatchingRule( SchemaConstants.CASE_IGNORE_IA5_MATCH_MR_OID, registries );
        mrule.setNames( new String[]
            { SchemaConstants.CASE_IGNORE_IA5_MATCH_MR } );
        mrule.setSyntaxOid( SchemaConstants.IA5_STRING_SYNTAX );
        cb.schemaObjectProduced( this, mrule.getOid(), mrule );

        /*
         * ( 1.3.6.1.4.1.1466.109.114.3 NAME 'caseIgnoreIA5SubstringsMatch'
         * SYNTAX 1.3.6.1.4.1.1466.115.121.1.58 )
         */
        mrule = new BootstrapMatchingRule( SchemaConstants.CASE_IGNORE_IA5_SUBSTRINGS_MATCH_MR_OID, registries );
        mrule.setNames( new String[]
            { SchemaConstants.CASE_IGNORE_IA5_SUBSTRINGS_MATCH_MR } );
        mrule.setSyntaxOid( SchemaConstants.SUBSTRING_ASSERTION_SYNTAX );
        cb.schemaObjectProduced( this, mrule.getOid(), mrule );
    }
}
