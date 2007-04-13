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
import org.apache.directory.shared.ldap.schema.syntax.ACIItemSyntaxChecker;
import org.apache.directory.shared.ldap.schema.syntax.AcceptAllSyntaxChecker;
import org.apache.directory.shared.ldap.schema.syntax.AccessPointSyntaxChecker;
import org.apache.directory.shared.ldap.schema.syntax.AttributeTypeDescriptionSyntaxChecker;
import org.apache.directory.shared.ldap.schema.syntax.AudioSyntaxChecker;
import org.apache.directory.shared.ldap.schema.syntax.BinarySyntaxChecker;
import org.apache.directory.shared.ldap.schema.syntax.BitStringSyntaxChecker;
import org.apache.directory.shared.ldap.schema.syntax.BooleanSyntaxChecker;
import org.apache.directory.shared.ldap.schema.syntax.CertificateListSyntaxChecker;
import org.apache.directory.shared.ldap.schema.syntax.CertificatePairSyntaxChecker;
import org.apache.directory.shared.ldap.schema.syntax.CertificateSyntaxChecker;
import org.apache.directory.shared.ldap.schema.syntax.CountrySyntaxChecker;
import org.apache.directory.shared.ldap.schema.syntax.DITContentRuleDescriptionSyntaxChecker;
import org.apache.directory.shared.ldap.schema.syntax.DITStructureRuleDescriptionSyntaxChecker;
import org.apache.directory.shared.ldap.schema.syntax.DLSubmitPermissionSyntaxChecker;
import org.apache.directory.shared.ldap.schema.syntax.DNSyntaxChecker;
import org.apache.directory.shared.ldap.schema.syntax.DSAQualitySyntaxSyntaxChecker;
import org.apache.directory.shared.ldap.schema.syntax.DSETypeSyntaxChecker;
import org.apache.directory.shared.ldap.schema.syntax.DataQualitySyntaxSyntaxChecker;
import org.apache.directory.shared.ldap.schema.syntax.DeliveryMethodSyntaxChecker;
import org.apache.directory.shared.ldap.schema.syntax.DirectoryStringSyntaxChecker;
import org.apache.directory.shared.ldap.schema.syntax.EnhancedGuideSyntaxChecker;
import org.apache.directory.shared.ldap.schema.syntax.FacsimileTelephoneNumberSyntaxChecker;
import org.apache.directory.shared.ldap.schema.syntax.FaxSyntaxChecker;
import org.apache.directory.shared.ldap.schema.syntax.GeneralizedTimeSyntaxChecker;
import org.apache.directory.shared.ldap.schema.syntax.GuideSyntaxChecker;
import org.apache.directory.shared.ldap.schema.syntax.Ia5StringSyntaxChecker;
import org.apache.directory.shared.ldap.schema.syntax.IntegerSyntaxChecker;
import org.apache.directory.shared.ldap.schema.syntax.JpegSyntaxChecker;
import org.apache.directory.shared.ldap.schema.syntax.LdapSyntaxDescriptionSyntaxChecker;
import org.apache.directory.shared.ldap.schema.syntax.MHSORAddressSyntaxChecker;
import org.apache.directory.shared.ldap.schema.syntax.MailPreferenceSyntaxChecker;
import org.apache.directory.shared.ldap.schema.syntax.MasterAndShadowAccessPointSyntaxChecker;
import org.apache.directory.shared.ldap.schema.syntax.MatchingRuleDescriptionSyntaxChecker;
import org.apache.directory.shared.ldap.schema.syntax.MatchingRuleUseDescriptionSyntaxChecker;
import org.apache.directory.shared.ldap.schema.syntax.NameAndOptionalUIDSyntaxChecker;
import org.apache.directory.shared.ldap.schema.syntax.NameFormDescriptionSyntaxChecker;
import org.apache.directory.shared.ldap.schema.syntax.NumericStringSyntaxChecker;
import org.apache.directory.shared.ldap.schema.syntax.ObjectClassDescriptionSyntaxChecker;
import org.apache.directory.shared.ldap.schema.syntax.OctetStringSyntaxChecker;
import org.apache.directory.shared.ldap.schema.syntax.OidSyntaxChecker;
import org.apache.directory.shared.ldap.schema.syntax.OtherMailboxSyntaxChecker;
import org.apache.directory.shared.ldap.schema.syntax.PostalAddressSyntaxChecker;
import org.apache.directory.shared.ldap.schema.syntax.PresentationAddressSyntaxChecker;
import org.apache.directory.shared.ldap.schema.syntax.PrintableStringSyntaxChecker;
import org.apache.directory.shared.ldap.schema.syntax.ProtocolInformationSyntaxChecker;
import org.apache.directory.shared.ldap.schema.syntax.SubstringAssertionSyntaxChecker;
import org.apache.directory.shared.ldap.schema.syntax.SubtreeSpecificationSyntaxChecker;
import org.apache.directory.shared.ldap.schema.syntax.SupplierAndConsumerSyntaxChecker;
import org.apache.directory.shared.ldap.schema.syntax.SupplierInformationSyntaxChecker;
import org.apache.directory.shared.ldap.schema.syntax.SupplierOrConsumerSyntaxChecker;
import org.apache.directory.shared.ldap.schema.syntax.SupportedAlgorithmSyntaxChecker;
import org.apache.directory.shared.ldap.schema.syntax.SyntaxChecker;
import org.apache.directory.shared.ldap.schema.syntax.TelephoneNumberSyntaxChecker;
import org.apache.directory.shared.ldap.schema.syntax.TeletexTerminalIdentifierSyntaxChecker;
import org.apache.directory.shared.ldap.schema.syntax.TelexNumberSyntaxChecker;
import org.apache.directory.shared.ldap.schema.syntax.UtcTimeSyntaxChecker;


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
        cb.schemaObjectProduced( this, "1.3.6.1.4.1.1466.115.121.1.1", new ACIItemSyntaxChecker() );
        cb.schemaObjectProduced( this, "1.3.6.1.4.1.1466.115.121.1.2", new AccessPointSyntaxChecker() );
        cb.schemaObjectProduced( this, "1.3.6.1.4.1.1466.115.121.1.3", new AttributeTypeDescriptionSyntaxChecker() );
        cb.schemaObjectProduced( this, "1.3.6.1.4.1.1466.115.121.1.4", new AudioSyntaxChecker() );
        cb.schemaObjectProduced( this, "1.3.6.1.4.1.1466.115.121.1.5", new BinarySyntaxChecker() );
        cb.schemaObjectProduced( this, "1.3.6.1.4.1.1466.115.121.1.6", new BitStringSyntaxChecker() );
        cb.schemaObjectProduced( this, "1.3.6.1.4.1.1466.115.121.1.7", new BooleanSyntaxChecker() );
        cb.schemaObjectProduced( this, "1.3.6.1.4.1.1466.115.121.1.8", new CertificateSyntaxChecker() );
        cb.schemaObjectProduced( this, "1.3.6.1.4.1.1466.115.121.1.9", new CertificateListSyntaxChecker() );
        cb.schemaObjectProduced( this, "1.3.6.1.4.1.1466.115.121.1.10", new CertificatePairSyntaxChecker() );

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
        cb.schemaObjectProduced( this, "1.3.6.1.4.1.1466.115.121.1.11", new CountrySyntaxChecker() );
        cb.schemaObjectProduced( this, "1.3.6.1.4.1.1466.115.121.1.12", new DNSyntaxChecker() );
        cb.schemaObjectProduced( this, "1.3.6.1.4.1.1466.115.121.1.13", new DataQualitySyntaxSyntaxChecker() );
        cb.schemaObjectProduced( this, "1.3.6.1.4.1.1466.115.121.1.14", new DeliveryMethodSyntaxChecker() );
        cb.schemaObjectProduced( this, "1.3.6.1.4.1.1466.115.121.1.15", new DirectoryStringSyntaxChecker() );
        cb.schemaObjectProduced( this, "1.3.6.1.4.1.1466.115.121.1.16", new DITContentRuleDescriptionSyntaxChecker() );
        cb.schemaObjectProduced( this, "1.3.6.1.4.1.1466.115.121.1.17", new DITStructureRuleDescriptionSyntaxChecker() );
        cb.schemaObjectProduced( this, "1.3.6.1.4.1.1466.115.121.1.18", new DLSubmitPermissionSyntaxChecker() );
        cb.schemaObjectProduced( this, "1.3.6.1.4.1.1466.115.121.1.19", new DSAQualitySyntaxSyntaxChecker() );
        cb.schemaObjectProduced( this, "1.3.6.1.4.1.1466.115.121.1.20", new DSETypeSyntaxChecker() );

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
        cb.schemaObjectProduced( this, "1.3.6.1.4.1.1466.115.121.1.21", new EnhancedGuideSyntaxChecker() );
        cb.schemaObjectProduced( this, "1.3.6.1.4.1.1466.115.121.1.22", new FacsimileTelephoneNumberSyntaxChecker() );
        cb.schemaObjectProduced( this, "1.3.6.1.4.1.1466.115.121.1.23", new FaxSyntaxChecker() );
        cb.schemaObjectProduced( this, "1.3.6.1.4.1.1466.115.121.1.24", new GeneralizedTimeSyntaxChecker() );
        cb.schemaObjectProduced( this, "1.3.6.1.4.1.1466.115.121.1.25", new GuideSyntaxChecker() );
        cb.schemaObjectProduced( this, "1.3.6.1.4.1.1466.115.121.1.26", new Ia5StringSyntaxChecker() );
        cb.schemaObjectProduced( this, "1.3.6.1.4.1.1466.115.121.1.27", new IntegerSyntaxChecker() );
        cb.schemaObjectProduced( this, "1.3.6.1.4.1.1466.115.121.1.28", new JpegSyntaxChecker() );
        cb.schemaObjectProduced( this, "1.3.6.1.4.1.1466.115.121.1.29", new MasterAndShadowAccessPointSyntaxChecker() );
        cb.schemaObjectProduced( this, "1.3.6.1.4.1.1466.115.121.1.30", new MatchingRuleDescriptionSyntaxChecker() );

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
        cb.schemaObjectProduced( this, "1.3.6.1.4.1.1466.115.121.1.31", new MatchingRuleUseDescriptionSyntaxChecker() );
        cb.schemaObjectProduced( this, "1.3.6.1.4.1.1466.115.121.1.32", new MailPreferenceSyntaxChecker() );
        cb.schemaObjectProduced( this, "1.3.6.1.4.1.1466.115.121.1.33", new MHSORAddressSyntaxChecker() );
        cb.schemaObjectProduced( this, "1.3.6.1.4.1.1466.115.121.1.34", new NameAndOptionalUIDSyntaxChecker() );
        cb.schemaObjectProduced( this, "1.3.6.1.4.1.1466.115.121.1.35", new NameFormDescriptionSyntaxChecker() );
        cb.schemaObjectProduced( this, "1.3.6.1.4.1.1466.115.121.1.36", new NumericStringSyntaxChecker() );
        cb.schemaObjectProduced( this, "1.3.6.1.4.1.1466.115.121.1.37", new ObjectClassDescriptionSyntaxChecker() );
        cb.schemaObjectProduced( this, "1.3.6.1.4.1.1466.115.121.1.38", new OidSyntaxChecker() );
        cb.schemaObjectProduced( this, "1.3.6.1.4.1.1466.115.121.1.39", new OtherMailboxSyntaxChecker() );
        cb.schemaObjectProduced( this, "1.3.6.1.4.1.1466.115.121.1.40", new OctetStringSyntaxChecker() );

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
        cb.schemaObjectProduced( this, "1.3.6.1.4.1.1466.115.121.1.41", new PostalAddressSyntaxChecker() );
        cb.schemaObjectProduced( this, "1.3.6.1.4.1.1466.115.121.1.42", new ProtocolInformationSyntaxChecker() );
        cb.schemaObjectProduced( this, "1.3.6.1.4.1.1466.115.121.1.43", new PresentationAddressSyntaxChecker() );
        cb.schemaObjectProduced( this, "1.3.6.1.4.1.1466.115.121.1.44", new PrintableStringSyntaxChecker() );
        cb.schemaObjectProduced( this, "1.3.6.1.4.1.1466.115.121.1.45", new SubtreeSpecificationSyntaxChecker() );
        cb.schemaObjectProduced( this, "1.3.6.1.4.1.1466.115.121.1.46", new SupplierInformationSyntaxChecker() );
        cb.schemaObjectProduced( this, "1.3.6.1.4.1.1466.115.121.1.47", new SupplierOrConsumerSyntaxChecker() );
        cb.schemaObjectProduced( this, "1.3.6.1.4.1.1466.115.121.1.48", new SupplierAndConsumerSyntaxChecker() );
        cb.schemaObjectProduced( this, "1.3.6.1.4.1.1466.115.121.1.49", new SupportedAlgorithmSyntaxChecker() );
        cb.schemaObjectProduced( this, "1.3.6.1.4.1.1466.115.121.1.50", new TelephoneNumberSyntaxChecker() );

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
        cb.schemaObjectProduced( this, "1.3.6.1.4.1.1466.115.121.1.51", new TeletexTerminalIdentifierSyntaxChecker() );
        cb.schemaObjectProduced( this, "1.3.6.1.4.1.1466.115.121.1.52", new TelexNumberSyntaxChecker() );
        cb.schemaObjectProduced( this, "1.3.6.1.4.1.1466.115.121.1.53", new UtcTimeSyntaxChecker() );
        cb.schemaObjectProduced( this, "1.3.6.1.4.1.1466.115.121.1.54", new LdapSyntaxDescriptionSyntaxChecker() );

        cb.schemaObjectProduced( this, "1.3.6.1.4.1.1466.115.121.1.55", 
            new AcceptAllSyntaxChecker( "1.3.6.1.4.1.1466.115.121.1.55" ) );
        
        cb.schemaObjectProduced( this, "1.3.6.1.4.1.1466.115.121.1.56", 
            new AcceptAllSyntaxChecker( "1.3.6.1.4.1.1466.115.121.1.56" ) );
        
        cb.schemaObjectProduced( this, "1.3.6.1.4.1.1466.115.121.1.57", 
            new AcceptAllSyntaxChecker( "1.3.6.1.4.1.1466.115.121.1.57" ) );
        
        cb.schemaObjectProduced( this, "1.3.6.1.4.1.1466.115.121.1.58", new SubstringAssertionSyntaxChecker() );

        cb.schemaObjectProduced( this, "1.3.6.1.4.1.1466.115.121.1.59", 
            new AcceptAllSyntaxChecker( "1.3.6.1.4.1.1466.115.121.1.59" ) );
    }
}
