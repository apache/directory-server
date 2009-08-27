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
import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.schema.registries.Registries;
import org.apache.directory.shared.ldap.schema.syntaxChecker.ACIItemSyntaxChecker;
import org.apache.directory.shared.ldap.schema.syntaxChecker.AcceptAllSyntaxChecker;
import org.apache.directory.shared.ldap.schema.syntaxChecker.AccessPointSyntaxChecker;
import org.apache.directory.shared.ldap.schema.syntaxChecker.AttributeTypeDescriptionSyntaxChecker;
import org.apache.directory.shared.ldap.schema.syntaxChecker.AudioSyntaxChecker;
import org.apache.directory.shared.ldap.schema.syntaxChecker.BinarySyntaxChecker;
import org.apache.directory.shared.ldap.schema.syntaxChecker.BitStringSyntaxChecker;
import org.apache.directory.shared.ldap.schema.syntaxChecker.BooleanSyntaxChecker;
import org.apache.directory.shared.ldap.schema.syntaxChecker.CertificateListSyntaxChecker;
import org.apache.directory.shared.ldap.schema.syntaxChecker.CertificatePairSyntaxChecker;
import org.apache.directory.shared.ldap.schema.syntaxChecker.CertificateSyntaxChecker;
import org.apache.directory.shared.ldap.schema.syntaxChecker.CountrySyntaxChecker;
import org.apache.directory.shared.ldap.schema.syntaxChecker.DITContentRuleDescriptionSyntaxChecker;
import org.apache.directory.shared.ldap.schema.syntaxChecker.DITStructureRuleDescriptionSyntaxChecker;
import org.apache.directory.shared.ldap.schema.syntaxChecker.DLSubmitPermissionSyntaxChecker;
import org.apache.directory.shared.ldap.schema.syntaxChecker.DNSyntaxChecker;
import org.apache.directory.shared.ldap.schema.syntaxChecker.DSAQualitySyntaxSyntaxChecker;
import org.apache.directory.shared.ldap.schema.syntaxChecker.DSETypeSyntaxChecker;
import org.apache.directory.shared.ldap.schema.syntaxChecker.DataQualitySyntaxSyntaxChecker;
import org.apache.directory.shared.ldap.schema.syntaxChecker.DeliveryMethodSyntaxChecker;
import org.apache.directory.shared.ldap.schema.syntaxChecker.DirectoryStringSyntaxChecker;
import org.apache.directory.shared.ldap.schema.syntaxChecker.EnhancedGuideSyntaxChecker;
import org.apache.directory.shared.ldap.schema.syntaxChecker.FacsimileTelephoneNumberSyntaxChecker;
import org.apache.directory.shared.ldap.schema.syntaxChecker.FaxSyntaxChecker;
import org.apache.directory.shared.ldap.schema.syntaxChecker.GeneralizedTimeSyntaxChecker;
import org.apache.directory.shared.ldap.schema.syntaxChecker.GuideSyntaxChecker;
import org.apache.directory.shared.ldap.schema.syntaxChecker.Ia5StringSyntaxChecker;
import org.apache.directory.shared.ldap.schema.syntaxChecker.IntegerSyntaxChecker;
import org.apache.directory.shared.ldap.schema.syntaxChecker.JpegSyntaxChecker;
import org.apache.directory.shared.ldap.schema.syntaxChecker.LdapSyntaxDescriptionSyntaxChecker;
import org.apache.directory.shared.ldap.schema.syntaxChecker.MHSORAddressSyntaxChecker;
import org.apache.directory.shared.ldap.schema.syntaxChecker.MailPreferenceSyntaxChecker;
import org.apache.directory.shared.ldap.schema.syntaxChecker.MasterAndShadowAccessPointSyntaxChecker;
import org.apache.directory.shared.ldap.schema.syntaxChecker.MatchingRuleDescriptionSyntaxChecker;
import org.apache.directory.shared.ldap.schema.syntaxChecker.MatchingRuleUseDescriptionSyntaxChecker;
import org.apache.directory.shared.ldap.schema.syntaxChecker.NameAndOptionalUIDSyntaxChecker;
import org.apache.directory.shared.ldap.schema.syntaxChecker.NameFormDescriptionSyntaxChecker;
import org.apache.directory.shared.ldap.schema.syntaxChecker.NumericStringSyntaxChecker;
import org.apache.directory.shared.ldap.schema.syntaxChecker.ObjectClassDescriptionSyntaxChecker;
import org.apache.directory.shared.ldap.schema.syntaxChecker.OctetStringSyntaxChecker;
import org.apache.directory.shared.ldap.schema.syntaxChecker.OidSyntaxChecker;
import org.apache.directory.shared.ldap.schema.syntaxChecker.OtherMailboxSyntaxChecker;
import org.apache.directory.shared.ldap.schema.syntaxChecker.PostalAddressSyntaxChecker;
import org.apache.directory.shared.ldap.schema.syntaxChecker.PresentationAddressSyntaxChecker;
import org.apache.directory.shared.ldap.schema.syntaxChecker.PrintableStringSyntaxChecker;
import org.apache.directory.shared.ldap.schema.syntaxChecker.ProtocolInformationSyntaxChecker;
import org.apache.directory.shared.ldap.schema.syntaxChecker.SubstringAssertionSyntaxChecker;
import org.apache.directory.shared.ldap.schema.syntaxChecker.SubtreeSpecificationSyntaxChecker;
import org.apache.directory.shared.ldap.schema.syntaxChecker.SupplierAndConsumerSyntaxChecker;
import org.apache.directory.shared.ldap.schema.syntaxChecker.SupplierInformationSyntaxChecker;
import org.apache.directory.shared.ldap.schema.syntaxChecker.SupplierOrConsumerSyntaxChecker;
import org.apache.directory.shared.ldap.schema.syntaxChecker.SupportedAlgorithmSyntaxChecker;
import org.apache.directory.shared.ldap.schema.syntaxChecker.TelephoneNumberSyntaxChecker;
import org.apache.directory.shared.ldap.schema.syntaxChecker.TeletexTerminalIdentifierSyntaxChecker;
import org.apache.directory.shared.ldap.schema.syntaxChecker.TelexNumberSyntaxChecker;
import org.apache.directory.shared.ldap.schema.syntaxChecker.UtcTimeSyntaxChecker;


/**
 * A producer of SyntaxCheckers for the core schema.
 *
 * @todo now we use do nothing checkers for place holder and will add as we go 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class SystemSyntaxCheckerProducer extends AbstractBootstrapProducer
{
    /**
     * Creates a producer which produces all 58 of the core schema syntax's
     * SyntaxCheckers.
     */
    public SystemSyntaxCheckerProducer()
    {
        super( ProducerTypeEnum.SYNTAX_CHECKER_PRODUCER );
    }


    public void produce( Registries registries, ProducerCallback cb ) throws NamingException
    {
        /*
         * We are going to need a syntax checker for each and every one of
         * these syntaxes.  However right now we're probably not going to be
         * turning on syntax checking or are not as interested in it.  So we
         * can put in place simple do nothing syntax checkers - which is really
         * the binary syntax checker.
         */

        /*
         * From RFC 2252 Section 4.3.2. on Syntax Object Identifiers
         */

        /*
         * Value being represented        H-R OBJECT IDENTIFIER
         * ==================================================================
         * 0 ACI Item                         N  1.3.6.1.4.1.1466.115.121.1.1
         * 1 Access Point                     Y  1.3.6.1.4.1.1466.115.121.1.2
         * 2 Attribute Type Description       Y  1.3.6.1.4.1.1466.115.121.1.3
         * 3 Audio                            N  1.3.6.1.4.1.1466.115.121.1.4
         * 4 Binary                           N  1.3.6.1.4.1.1466.115.121.1.5
         * 5 Bit String                       Y  1.3.6.1.4.1.1466.115.121.1.6
         * 6 Boolean                          Y  1.3.6.1.4.1.1466.115.121.1.7
         * 7 Certificate                      N  1.3.6.1.4.1.1466.115.121.1.8
         * 8 Certificate List                 N  1.3.6.1.4.1.1466.115.121.1.9
         * 9 Certificate Pair                 N  1.3.6.1.4.1.1466.115.121.1.10
         */
        cb.schemaObjectProduced( this, SchemaConstants.ACI_ITEM_SYNTAX, new ACIItemSyntaxChecker() );
        cb.schemaObjectProduced( this, SchemaConstants.ACCESS_POINT_SYNTAX, new AccessPointSyntaxChecker() );
        cb.schemaObjectProduced( this, SchemaConstants.ATTRIBUTE_TYPE_DESCRIPTION_SYNTAX, new AttributeTypeDescriptionSyntaxChecker() );
        cb.schemaObjectProduced( this, SchemaConstants.AUDIO_SYNTAX, new AudioSyntaxChecker() );
        cb.schemaObjectProduced( this, SchemaConstants.BINARY_SYNTAX, new BinarySyntaxChecker() );
        cb.schemaObjectProduced( this, SchemaConstants.BIT_STRING_SYNTAX, new BitStringSyntaxChecker() );
        cb.schemaObjectProduced( this, SchemaConstants.BOOLEAN_SYNTAX, new BooleanSyntaxChecker() );
        cb.schemaObjectProduced( this, SchemaConstants.CERTIFICATE_SYNTAX, new CertificateSyntaxChecker() );
        cb.schemaObjectProduced( this, SchemaConstants.CERTIFICATE_LIST_SYNTAX, new CertificateListSyntaxChecker() );
        cb.schemaObjectProduced( this, SchemaConstants.CERTIFICATE_PAIR_SYNTAX, new CertificatePairSyntaxChecker() );

        /*
         * 10 Country String                  Y  1.3.6.1.4.1.1466.115.121.1.11
         * 11 DN                              Y  1.3.6.1.4.1.1466.115.121.1.12
         * 12 Data Quality Syntax             Y  1.3.6.1.4.1.1466.115.121.1.13
         * 13 Delivery Method                 Y  1.3.6.1.4.1.1466.115.121.1.14
         * 14 Directory String                Y  1.3.6.1.4.1.1466.115.121.1.15
         * 15 DIT Content Rule Description    Y  1.3.6.1.4.1.1466.115.121.1.16
         * 16 DIT Structure Rule Description  Y  1.3.6.1.4.1.1466.115.121.1.17
         * 17 DL Submit Permission            Y  1.3.6.1.4.1.1466.115.121.1.18
         * 18 DSA Quality Syntax              Y  1.3.6.1.4.1.1466.115.121.1.19
         * 19 DSE Type                        Y  1.3.6.1.4.1.1466.115.121.1.20
         */
        cb.schemaObjectProduced( this, SchemaConstants.COUNTRY_STRING_SYNTAX, new CountrySyntaxChecker() );
        cb.schemaObjectProduced( this, SchemaConstants.DN_SYNTAX, new DNSyntaxChecker() );
        cb.schemaObjectProduced( this, SchemaConstants.DATA_QUALITY_SYNTAX, new DataQualitySyntaxSyntaxChecker() );
        cb.schemaObjectProduced( this, SchemaConstants.DELIVERY_METHOD_SYNTAX, new DeliveryMethodSyntaxChecker() );
        cb.schemaObjectProduced( this, SchemaConstants.DIRECTORY_STRING_SYNTAX, new DirectoryStringSyntaxChecker() );
        cb.schemaObjectProduced( this, SchemaConstants.DIT_CONTENT_RULE_SYNTAX, new DITContentRuleDescriptionSyntaxChecker() );
        cb.schemaObjectProduced( this, SchemaConstants.DIT_STRUCTURE_RULE_SYNTAX, new DITStructureRuleDescriptionSyntaxChecker() );
        cb.schemaObjectProduced( this, SchemaConstants.DL_SUBMIT_PERMISSION_SYNTAX, new DLSubmitPermissionSyntaxChecker() );
        cb.schemaObjectProduced( this, SchemaConstants.DSA_QUALITY_SYNTAX, new DSAQualitySyntaxSyntaxChecker() );
        cb.schemaObjectProduced( this, SchemaConstants.DSE_TYPE_SYNTAX, new DSETypeSyntaxChecker() );

        /* 20 Enhanced Guide                  Y  1.3.6.1.4.1.1466.115.121.1.21
         * 21 Facsimile Telephone Number      Y  1.3.6.1.4.1.1466.115.121.1.22
         * 22 Fax                             N  1.3.6.1.4.1.1466.115.121.1.23
         * 23 Generalized Time                Y  1.3.6.1.4.1.1466.115.121.1.24
         * 24 Guide                           Y  1.3.6.1.4.1.1466.115.121.1.25
         * 25 IA5 String                      Y  1.3.6.1.4.1.1466.115.121.1.26
         * 26 INTEGER                         Y  1.3.6.1.4.1.1466.115.121.1.27
         * 27 JPEG                            N  1.3.6.1.4.1.1466.115.121.1.28
         * 28 Master And Shadow Access Points Y  1.3.6.1.4.1.1466.115.121.1.29
         * 29 Matching Rule Description       Y  1.3.6.1.4.1.1466.115.121.1.30
         */
        cb.schemaObjectProduced( this, SchemaConstants.ENHANCED_GUIDE_SYNTAX, new EnhancedGuideSyntaxChecker() );
        cb.schemaObjectProduced( this, SchemaConstants.FACSIMILE_TELEPHONE_NUMBER_SYNTAX, new FacsimileTelephoneNumberSyntaxChecker() );
        cb.schemaObjectProduced( this, SchemaConstants.FAX_SYNTAX, new FaxSyntaxChecker() );
        cb.schemaObjectProduced( this, SchemaConstants.GENERALIZED_TIME_SYNTAX, new GeneralizedTimeSyntaxChecker() );
        cb.schemaObjectProduced( this, SchemaConstants.GUIDE_SYNTAX, new GuideSyntaxChecker() );
        cb.schemaObjectProduced( this, SchemaConstants.IA5_STRING_SYNTAX, new Ia5StringSyntaxChecker() );
        cb.schemaObjectProduced( this, SchemaConstants.INTEGER_SYNTAX, new IntegerSyntaxChecker() );
        cb.schemaObjectProduced( this, SchemaConstants.JPEG_SYNTAX, new JpegSyntaxChecker() );
        cb.schemaObjectProduced( this, SchemaConstants.MASTER_AND_SHADOW_ACCESS_POINTS_SYNTAX, new MasterAndShadowAccessPointSyntaxChecker() );
        cb.schemaObjectProduced( this, SchemaConstants.MATCHING_RULE_DESCRIPTION_SYNTAX, new MatchingRuleDescriptionSyntaxChecker() );

        /* 30 Matching Rule Use Description   Y  1.3.6.1.4.1.1466.115.121.1.31
         * 31 Mail Preference                 Y  1.3.6.1.4.1.1466.115.121.1.32
         * 32 MHS OR Address                  Y  1.3.6.1.4.1.1466.115.121.1.33
         * 33 Name And Optional UID           Y  1.3.6.1.4.1.1466.115.121.1.34
         * 34 Name Form Description           Y  1.3.6.1.4.1.1466.115.121.1.35
         * 35 Numeric String                  Y  1.3.6.1.4.1.1466.115.121.1.36
         * 36 Object Class Description        Y  1.3.6.1.4.1.1466.115.121.1.37
         * 37 OID                             Y  1.3.6.1.4.1.1466.115.121.1.38
         * 38 Other Mailbox                   Y  1.3.6.1.4.1.1466.115.121.1.39
         * 39 Octet String                    Y  1.3.6.1.4.1.1466.115.121.1.40
         */
        cb.schemaObjectProduced( this, SchemaConstants.MATCHING_RULE_USE_DESCRIPTION_SYNTAX, new MatchingRuleUseDescriptionSyntaxChecker() );
        cb.schemaObjectProduced( this, SchemaConstants.MAIL_PREFERENCE_SYNTAX, new MailPreferenceSyntaxChecker() );
        cb.schemaObjectProduced( this, SchemaConstants.MHS_OR_ADDRESS_SYNTAX, new MHSORAddressSyntaxChecker() );
        cb.schemaObjectProduced( this, SchemaConstants.NAME_AND_OPTIONAL_UID_SYNTAX, new NameAndOptionalUIDSyntaxChecker() );
        cb.schemaObjectProduced( this, SchemaConstants.NAME_FORM_DESCRIPTION_SYNTAX, new NameFormDescriptionSyntaxChecker() );
        cb.schemaObjectProduced( this, SchemaConstants.NUMERIC_STRING_SYNTAX, new NumericStringSyntaxChecker() );
        cb.schemaObjectProduced( this, SchemaConstants.OBJECT_CLASS_DESCRIPTION_SYNTAX, new ObjectClassDescriptionSyntaxChecker() );
        cb.schemaObjectProduced( this, SchemaConstants.OID_SYNTAX, new OidSyntaxChecker() );
        cb.schemaObjectProduced( this, SchemaConstants.OTHER_MAILBOX_SYNTAX, new OtherMailboxSyntaxChecker() );
        cb.schemaObjectProduced( this, SchemaConstants.OCTET_STRING_SYNTAX, new OctetStringSyntaxChecker() );

        /*
         * 40 Postal Address                  Y  1.3.6.1.4.1.1466.115.121.1.41
         * 41 Protocol Information            Y  1.3.6.1.4.1.1466.115.121.1.42
         * 42 Presentation Address            Y  1.3.6.1.4.1.1466.115.121.1.43
         * 43 Printable String                Y  1.3.6.1.4.1.1466.115.121.1.44
         * 44 Subtree Specification           Y  1.3.6.1.4.1.1466.115.121.1.45
         * 45 Supplier Information            Y  1.3.6.1.4.1.1466.115.121.1.46
         * 46 Supplier Or Consumer            Y  1.3.6.1.4.1.1466.115.121.1.47
         * 47 Supplier And Consumer           Y  1.3.6.1.4.1.1466.115.121.1.48
         * 48 Supported Algorithm             N  1.3.6.1.4.1.1466.115.121.1.49
         * 49 Telephone Number                Y  1.3.6.1.4.1.1466.115.121.1.50
         */
        cb.schemaObjectProduced( this, SchemaConstants.POSTAL_ADDRESS_SYNTAX, new PostalAddressSyntaxChecker() );
        cb.schemaObjectProduced( this, SchemaConstants.PROTOCOL_INFORMATION_SYNTAX, new ProtocolInformationSyntaxChecker() );
        cb.schemaObjectProduced( this, SchemaConstants.PRESENTATION_ADDRESS_SYNTAX, new PresentationAddressSyntaxChecker() );
        cb.schemaObjectProduced( this, SchemaConstants.PRINTABLE_STRING_SYNTAX, new PrintableStringSyntaxChecker() );
        cb.schemaObjectProduced( this, SchemaConstants.SUBTREE_SPECIFICATION_SYNTAX, new SubtreeSpecificationSyntaxChecker() );
        cb.schemaObjectProduced( this, SchemaConstants.SUPPLIER_INFORMATION_SYNTAX, new SupplierInformationSyntaxChecker() );
        cb.schemaObjectProduced( this, SchemaConstants.SUPPLIER_OR_CONSUMER_SYNTAX, new SupplierOrConsumerSyntaxChecker() );
        cb.schemaObjectProduced( this, SchemaConstants.SUPPLIER_AND_CONSUMER_SYNTAX, new SupplierAndConsumerSyntaxChecker() );
        cb.schemaObjectProduced( this, SchemaConstants.SUPPORTED_ALGORITHM_SYNTAX, new SupportedAlgorithmSyntaxChecker() );
        cb.schemaObjectProduced( this, SchemaConstants.TELEPHONE_NUMBER_SYNTAX, new TelephoneNumberSyntaxChecker() );

        /*
         * 50 Teletex Terminal Identifier     Y  1.3.6.1.4.1.1466.115.121.1.51
         * 51 Telex Number                    Y  1.3.6.1.4.1.1466.115.121.1.52
         * 52 UTC Time                        Y  1.3.6.1.4.1.1466.115.121.1.53
         * 53 LDAP Syntax Description         Y  1.3.6.1.4.1.1466.115.121.1.54
         * 54 Modify Rights                   Y  1.3.6.1.4.1.1466.115.121.1.55  (No defined SC yet)
         * 55 LDAP BootstrapSchema Definition Y  1.3.6.1.4.1.1466.115.121.1.56  (No defined SC yet) 
         * 56 LDAP BootstrapSchema DescriptionY  1.3.6.1.4.1.1466.115.121.1.57  (No defined SC yet)
         * 57 Substring Assertion             Y  1.3.6.1.4.1.1466.115.121.1.58
         */
        cb.schemaObjectProduced( this, SchemaConstants.TELETEX_TERMINAL_IDENTIFIER_SYNTAX, new TeletexTerminalIdentifierSyntaxChecker() );
        cb.schemaObjectProduced( this, SchemaConstants.TELEX_NUMBER_SYNTAX, new TelexNumberSyntaxChecker() );
        cb.schemaObjectProduced( this, SchemaConstants.UTC_TIME_SYNTAX, new UtcTimeSyntaxChecker() );
        cb.schemaObjectProduced( this, SchemaConstants.LDAP_SYNTAX_DESCRIPTION_SYNTAX, new LdapSyntaxDescriptionSyntaxChecker() );

        cb.schemaObjectProduced( this, SchemaConstants.MODIFY_RIGHTS_SYNTAX, 
            new AcceptAllSyntaxChecker( SchemaConstants.MODIFY_RIGHTS_SYNTAX ) );
        
        cb.schemaObjectProduced( this, SchemaConstants.LDAP_SCHEMA_DEFINITION_SYNTAX, 
            new AcceptAllSyntaxChecker( SchemaConstants.LDAP_SCHEMA_DEFINITION_SYNTAX ) );
        
        cb.schemaObjectProduced( this, SchemaConstants.LDAP_SCHEMA_DESCRIPTION_SYNTAX, 
            new AcceptAllSyntaxChecker( SchemaConstants.LDAP_SCHEMA_DESCRIPTION_SYNTAX ) );
        
        cb.schemaObjectProduced( this, SchemaConstants.SUBSTRING_ASSERTION_SYNTAX, new SubstringAssertionSyntaxChecker() );

        cb.schemaObjectProduced( this, SchemaConstants.ATTRIBUTE_CERTIFICATE_ASSERTION_SYNTAX, 
            new AcceptAllSyntaxChecker( SchemaConstants.ATTRIBUTE_CERTIFICATE_ASSERTION_SYNTAX ) );
    }
}
