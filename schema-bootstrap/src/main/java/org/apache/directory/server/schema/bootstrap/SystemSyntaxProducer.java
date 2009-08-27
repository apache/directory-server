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
import org.apache.directory.shared.ldap.schema.LdapSyntax;
import org.apache.directory.shared.ldap.schema.registries.Registries;


/**
 * A simple Syntax factory for the core LDAP schema in Section 4.3.2 of
 * <a href="http://www.faqs.org/rfcs/rfc2252.html">RFC2252</a>.
 * The following table reproduced from RFC2252 shows the syntaxes included
 * within this SyntaxFactory:
 * <pre>
 * Index   Value being represented   H-R     OBJECT IDENTIFIER
 * =====================================================================
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
 * 20 Enhanced Guide                  Y  1.3.6.1.4.1.1466.115.121.1.21
 * 21 Facsimile Telephone Number      Y  1.3.6.1.4.1.1466.115.121.1.22
 * 22 Fax                             N  1.3.6.1.4.1.1466.115.121.1.23
 * 23 Generalized Time                Y  1.3.6.1.4.1.1466.115.121.1.24
 * 24 Guide                           Y  1.3.6.1.4.1.1466.115.121.1.25
 * 25 IA5 String                      Y  1.3.6.1.4.1.1466.115.121.1.26
 * 26 INTEGER                         Y  1.3.6.1.4.1.1466.115.121.1.27
 * 27 JPEG                            N  1.3.6.1.4.1.1466.115.121.1.28
 * 28 Master And Shadow Access Points Y  1.3.6.1.4.1.1466.115.121.1.29
 * 29 Matching Rule Description       Y  1.3.6.1.4.1.1466.115.121.1.30
 * 30 Matching Rule Use Description   Y  1.3.6.1.4.1.1466.115.121.1.31
 * 31 Mail Preference                 Y  1.3.6.1.4.1.1466.115.121.1.32
 * 32 MHS OR Address                  Y  1.3.6.1.4.1.1466.115.121.1.33
 * 33 Name And Optional UID           Y  1.3.6.1.4.1.1466.115.121.1.34
 * 34 Name Form Description           Y  1.3.6.1.4.1.1466.115.121.1.35
 * 35 Numeric String                  Y  1.3.6.1.4.1.1466.115.121.1.36
 * 36 Object Class Description        Y  1.3.6.1.4.1.1466.115.121.1.37
 * 37 OID                             Y  1.3.6.1.4.1.1466.115.121.1.38
 * 38 Other Mailbox                   Y  1.3.6.1.4.1.1466.115.121.1.39
 *
 * 39 Octet String                    Y  1.3.6.1.4.1.1466.115.121.1.40
 *
 * This is not going to be followed for OctetString which needs to be treated
 * as binary data.
 *
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
 * 50 Teletex Terminal Identifier     Y  1.3.6.1.4.1.1466.115.121.1.51
 * 51 Telex Number                    Y  1.3.6.1.4.1.1466.115.121.1.52
 * 52 UTC Time                        Y  1.3.6.1.4.1.1466.115.121.1.53
 * 53 LDAP Syntax Description         Y  1.3.6.1.4.1.1466.115.121.1.54
 * 54 Modify Rights                   Y  1.3.6.1.4.1.1466.115.121.1.55
 * 55 LDAP Schema Definition          Y  1.3.6.1.4.1.1466.115.121.1.56
 * 56 LDAP Schema Description         Y  1.3.6.1.4.1.1466.115.121.1.57
 * 57 Substring Assertion             Y  1.3.6.1.4.1.1466.115.121.1.58
 * 58 Attribute Certificate Assertion N  1.3.6.1.4.1.1466.115.121.1.59
 * </pre>
 *
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class SystemSyntaxProducer extends AbstractBootstrapProducer
{
    public SystemSyntaxProducer()
    {
        super( ProducerTypeEnum.SYNTAX_PRODUCER );
    }


    // ------------------------------------------------------------------------
    // BootstrapProducer Methods
    // ------------------------------------------------------------------------
    public void produce( Registries registries, ProducerCallback cb ) throws NamingException
    {
        LdapSyntax ldapSyntax;
        //SyntaxCheckerRegistry syntaxCheckerRegistry = registries.getSyntaxCheckerRegistry();

        /*
         * From RFC 2252 Section 4.3.2. on Syntax Object Identifiers
         */

        /*
         * Value being represented        H-R OBJECT IDENTIFIER
         * ==================================================================
         * 0 ACI Item                        N  1.3.6.1.4.1.1466.115.121.1.1
         * 1 Access Point                    Y  1.3.6.1.4.1.1466.115.121.1.2
         * 2 Attribute Type Description      Y  1.3.6.1.4.1.1466.115.121.1.3
         * 3 Audio                           N  1.3.6.1.4.1.1466.115.121.1.4
         * 4 Binary                          N  1.3.6.1.4.1.1466.115.121.1.5
         * 5 Bit String                      Y  1.3.6.1.4.1.1466.115.121.1.6
         * 6 Boolean                         Y  1.3.6.1.4.1.1466.115.121.1.7
         * 7 Certificate                     N  1.3.6.1.4.1.1466.115.121.1.8
         * 8 Certificate List                N  1.3.6.1.4.1.1466.115.121.1.9
         * 9 Certificate Pair                N  1.3.6.1.4.1.1466.115.121.1.10
         */
        ldapSyntax = new LdapSyntax( SchemaConstants.ACI_ITEM_SYNTAX );
        ldapSyntax.addName( "ACI Item" );
        //ldapSyntax.setSyntaxChecker( syntaxChecker )
        
        // This is in direct conflict with RFC 2252 but for us ACI Item is human readable
        ldapSyntax.setHumanReadable( true );
        cb.schemaObjectProduced( this, ldapSyntax.getOid(), ldapSyntax );

        ldapSyntax = new LdapSyntax( SchemaConstants.ACCESS_POINT_SYNTAX );
        // ); //,syntaxCheckerRegistry );
        ldapSyntax.addName( "Access Point" );
        ldapSyntax.setHumanReadable( true );
        cb.schemaObjectProduced( this, ldapSyntax.getOid(), ldapSyntax );

        ldapSyntax = new LdapSyntax( SchemaConstants.ATTRIBUTE_TYPE_DESCRIPTION_SYNTAX );
        // ); //,syntaxCheckerRegistry );
        ldapSyntax.addName( "Attribute Type Description" );
        ldapSyntax.setHumanReadable( true );
        cb.schemaObjectProduced( this, ldapSyntax.getOid(), ldapSyntax );

        ldapSyntax = new LdapSyntax( SchemaConstants.AUDIO_SYNTAX );
        // ); //,syntaxCheckerRegistry );
        ldapSyntax.addName( "Audio" );
        ldapSyntax.setHumanReadable( false );
        cb.schemaObjectProduced( this, ldapSyntax.getOid(), ldapSyntax );

        ldapSyntax = new LdapSyntax( SchemaConstants.BINARY_SYNTAX );
        //, syntaxCheckerRegistry );
        ldapSyntax.addName( "Binary" );
        ldapSyntax.setHumanReadable( false );
        cb.schemaObjectProduced( this, ldapSyntax.getOid(), ldapSyntax );

        ldapSyntax = new LdapSyntax( SchemaConstants.BIT_STRING_SYNTAX );
        //, syntaxCheckerRegistry );
        ldapSyntax.addName( "Bit String" );
        ldapSyntax.setHumanReadable( true );
        cb.schemaObjectProduced( this, ldapSyntax.getOid(), ldapSyntax );

        ldapSyntax = new LdapSyntax( SchemaConstants.BOOLEAN_SYNTAX );
        //, syntaxCheckerRegistry );
        ldapSyntax.addName( "Boolean" );
        ldapSyntax.setHumanReadable( true );
        cb.schemaObjectProduced( this, ldapSyntax.getOid(), ldapSyntax );

        ldapSyntax = new LdapSyntax( SchemaConstants.CERTIFICATE_SYNTAX );
        //, syntaxCheckerRegistry );
        ldapSyntax.addName( "Certificate" );
        ldapSyntax.setHumanReadable( false );
        cb.schemaObjectProduced( this, ldapSyntax.getOid(), ldapSyntax );

        ldapSyntax = new LdapSyntax( SchemaConstants.CERTIFICATE_LIST_SYNTAX );
        //, syntaxCheckerRegistry );
        ldapSyntax.addName( "Certificate List" );
        ldapSyntax.setHumanReadable( false );
        cb.schemaObjectProduced( this, ldapSyntax.getOid(), ldapSyntax );

        ldapSyntax = new LdapSyntax( SchemaConstants.CERTIFICATE_PAIR_SYNTAX );
        //, syntaxCheckerRegistry );
        ldapSyntax.addName( "Certificate Pair" );
        ldapSyntax.setHumanReadable( false );
        cb.schemaObjectProduced( this, ldapSyntax.getOid(), ldapSyntax );

        /*
         * Value being represented        H-R OBJECT IDENTIFIER
         * ===================================================================
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
        ldapSyntax = new LdapSyntax( SchemaConstants.COUNTRY_STRING_SYNTAX ); //,syntaxCheckerRegistry );
        ldapSyntax.addName( "Country String" );
        ldapSyntax.setHumanReadable( true );
        cb.schemaObjectProduced( this, ldapSyntax.getOid(), ldapSyntax );

        ldapSyntax = new LdapSyntax( SchemaConstants.DN_SYNTAX ); //,syntaxCheckerRegistry );
        ldapSyntax.addName( "DN" );
        ldapSyntax.setHumanReadable( true );
        cb.schemaObjectProduced( this, ldapSyntax.getOid(), ldapSyntax );

        ldapSyntax = new LdapSyntax( SchemaConstants.DATA_QUALITY_SYNTAX ); //,syntaxCheckerRegistry );
        ldapSyntax.addName( "Data Quality Syntax" );
        ldapSyntax.setHumanReadable( true );
        cb.schemaObjectProduced( this, ldapSyntax.getOid(), ldapSyntax );

        ldapSyntax = new LdapSyntax( SchemaConstants.DELIVERY_METHOD_SYNTAX ); //,syntaxCheckerRegistry );
        ldapSyntax.addName( "Delivery Method" );
        ldapSyntax.setHumanReadable( true );
        cb.schemaObjectProduced( this, ldapSyntax.getOid(), ldapSyntax );

        ldapSyntax = new LdapSyntax( SchemaConstants.DIRECTORY_STRING_SYNTAX ); //,syntaxCheckerRegistry );
        ldapSyntax.addName( "Directory String" );
        ldapSyntax.setHumanReadable( true );
        cb.schemaObjectProduced( this, ldapSyntax.getOid(), ldapSyntax );

        ldapSyntax = new LdapSyntax( SchemaConstants.DIT_CONTENT_RULE_SYNTAX ); //,syntaxCheckerRegistry );
        ldapSyntax.addName( "DIT Content Rule Description" );
        ldapSyntax.setHumanReadable( true );
        cb.schemaObjectProduced( this, ldapSyntax.getOid(), ldapSyntax );

        ldapSyntax = new LdapSyntax( SchemaConstants.DIT_STRUCTURE_RULE_SYNTAX ); //,syntaxCheckerRegistry );
        ldapSyntax.addName( "DIT Structure Rule Description" );
        ldapSyntax.setHumanReadable( true );
        cb.schemaObjectProduced( this, ldapSyntax.getOid(), ldapSyntax );

        ldapSyntax = new LdapSyntax( SchemaConstants.DL_SUBMIT_PERMISSION_SYNTAX ); //,syntaxCheckerRegistry );
        ldapSyntax.addName( "DL Submit Permission" );
        ldapSyntax.setHumanReadable( true );
        cb.schemaObjectProduced( this, ldapSyntax.getOid(), ldapSyntax );

        ldapSyntax = new LdapSyntax( SchemaConstants.DSA_QUALITY_SYNTAX ); //,syntaxCheckerRegistry );
        ldapSyntax.addName( "DSA Quality Syntax" );
        ldapSyntax.setHumanReadable( true );
        cb.schemaObjectProduced( this, ldapSyntax.getOid(), ldapSyntax );

        ldapSyntax = new LdapSyntax( SchemaConstants.DSE_TYPE_SYNTAX ); //,syntaxCheckerRegistry );
        ldapSyntax.addName( "DSE Type" );
        ldapSyntax.setHumanReadable( true );
        cb.schemaObjectProduced( this, ldapSyntax.getOid(), ldapSyntax );

        /*
         * Value being represented        H-R OBJECT IDENTIFIER
         * ===================================================================
         * 20 Enhanced Guide                  Y  1.3.6.1.4.1.1466.115.121.1.21
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
        ldapSyntax = new LdapSyntax( SchemaConstants.ENHANCED_GUIDE_SYNTAX ); //,syntaxCheckerRegistry );
        ldapSyntax.addName( "Enhanced Guide" );
        ldapSyntax.setHumanReadable( true );
        cb.schemaObjectProduced( this, ldapSyntax.getOid(), ldapSyntax );

        ldapSyntax = new LdapSyntax( SchemaConstants.FACSIMILE_TELEPHONE_NUMBER_SYNTAX ); //,syntaxCheckerRegistry );
        ldapSyntax.addName( "Facsimile Telephone Number" );
        ldapSyntax.setHumanReadable( true );
        cb.schemaObjectProduced( this, ldapSyntax.getOid(), ldapSyntax );

        ldapSyntax = new LdapSyntax( SchemaConstants.FAX_SYNTAX ); //,syntaxCheckerRegistry );
        ldapSyntax.addName( "Fax" );
        ldapSyntax.setHumanReadable( false );
        cb.schemaObjectProduced( this, ldapSyntax.getOid(), ldapSyntax );

        ldapSyntax = new LdapSyntax( SchemaConstants.GENERALIZED_TIME_SYNTAX ); //,syntaxCheckerRegistry );
        ldapSyntax.addName( "Generalized Time" );
        ldapSyntax.setHumanReadable( true );
        cb.schemaObjectProduced( this, ldapSyntax.getOid(), ldapSyntax );

        ldapSyntax = new LdapSyntax( SchemaConstants.GUIDE_SYNTAX ); //,syntaxCheckerRegistry );
        ldapSyntax.addName( "Guide" );
        ldapSyntax.setHumanReadable( true );
        cb.schemaObjectProduced( this, ldapSyntax.getOid(), ldapSyntax );

        ldapSyntax = new LdapSyntax( SchemaConstants.IA5_STRING_SYNTAX ); //,syntaxCheckerRegistry );
        ldapSyntax.addName( "IA5 String" );
        ldapSyntax.setHumanReadable( true );
        cb.schemaObjectProduced( this, ldapSyntax.getOid(), ldapSyntax );

        ldapSyntax = new LdapSyntax( SchemaConstants.INTEGER_SYNTAX ); //,syntaxCheckerRegistry );
        ldapSyntax.addName( "INTEGER" );
        ldapSyntax.setHumanReadable( true );
        cb.schemaObjectProduced( this, ldapSyntax.getOid(), ldapSyntax );

        ldapSyntax = new LdapSyntax( SchemaConstants.JPEG_SYNTAX ); //,syntaxCheckerRegistry );
        ldapSyntax.addName( "JPEG" );
        ldapSyntax.setHumanReadable( false );
        cb.schemaObjectProduced( this, ldapSyntax.getOid(), ldapSyntax );

        ldapSyntax = new LdapSyntax( SchemaConstants.MASTER_AND_SHADOW_ACCESS_POINTS_SYNTAX ); //,syntaxCheckerRegistry );
        ldapSyntax.addName( "Master And Shadow Access Points" );
        ldapSyntax.setHumanReadable( true );
        cb.schemaObjectProduced( this, ldapSyntax.getOid(), ldapSyntax );

        ldapSyntax = new LdapSyntax( SchemaConstants.MATCHING_RULE_DESCRIPTION_SYNTAX ); //,syntaxCheckerRegistry );
        ldapSyntax.addName( "Matching Rule Description" );
        ldapSyntax.setHumanReadable( true );
        cb.schemaObjectProduced( this, ldapSyntax.getOid(), ldapSyntax );

        /*
         * Value being represented        H-R OBJECT IDENTIFIER
         * ==================================================================
         * 30 Matching Rule Use Description   Y  1.3.6.1.4.1.1466.115.121.1.31
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
        ldapSyntax = new LdapSyntax( SchemaConstants.MATCHING_RULE_USE_DESCRIPTION_SYNTAX ); //,syntaxCheckerRegistry );
        ldapSyntax.addName( "Matching Rule Use Description" );
        ldapSyntax.setHumanReadable( true );
        cb.schemaObjectProduced( this, ldapSyntax.getOid(), ldapSyntax );

        ldapSyntax = new LdapSyntax( SchemaConstants.MAIL_PREFERENCE_SYNTAX ); 
        //,syntaxCheckerRegistry );
        ldapSyntax.addName( "Mail Preference" );
        ldapSyntax.setHumanReadable( true );
        cb.schemaObjectProduced( this, ldapSyntax.getOid(), ldapSyntax );

        ldapSyntax = new LdapSyntax( SchemaConstants.MHS_OR_ADDRESS_SYNTAX ); 
        //,syntaxCheckerRegistry );
        ldapSyntax.addName( "MHS OR Address" );
        ldapSyntax.setHumanReadable( true );
        cb.schemaObjectProduced( this, ldapSyntax.getOid(), ldapSyntax );

        ldapSyntax = new LdapSyntax( SchemaConstants.NAME_AND_OPTIONAL_UID_SYNTAX ); 
        //,syntaxCheckerRegistry );
        ldapSyntax.addName( "Name And Optional UID" );
        ldapSyntax.setHumanReadable( true );
        cb.schemaObjectProduced( this, ldapSyntax.getOid(), ldapSyntax );

        ldapSyntax = new LdapSyntax( SchemaConstants.NAME_FORM_DESCRIPTION_SYNTAX ); 
        //,syntaxCheckerRegistry );
        ldapSyntax.addName( "Name Form Description" );
        ldapSyntax.setHumanReadable( true );
        cb.schemaObjectProduced( this, ldapSyntax.getOid(), ldapSyntax );

        ldapSyntax = new LdapSyntax( SchemaConstants.NUMERIC_STRING_SYNTAX ); 
        //,syntaxCheckerRegistry );
        ldapSyntax.addName( "Numeric String" );
        ldapSyntax.setHumanReadable( true );
        cb.schemaObjectProduced( this, ldapSyntax.getOid(), ldapSyntax );

        ldapSyntax = new LdapSyntax( SchemaConstants.OBJECT_CLASS_DESCRIPTION_SYNTAX ); 
        //,syntaxCheckerRegistry );
        ldapSyntax.addName( "Object Class Description" );
        ldapSyntax.setHumanReadable( true );
        cb.schemaObjectProduced( this, ldapSyntax.getOid(), ldapSyntax );

        ldapSyntax = new LdapSyntax( SchemaConstants.OID_SYNTAX ); 
        //,syntaxCheckerRegistry );
        ldapSyntax.addName( "OID" );
        ldapSyntax.setHumanReadable( true );
        cb.schemaObjectProduced( this, ldapSyntax.getOid(), ldapSyntax );

        ldapSyntax = new LdapSyntax( SchemaConstants.OTHER_MAILBOX_SYNTAX ); 
        //,syntaxCheckerRegistry );
        ldapSyntax.addName( "Other Mailbox" );
        ldapSyntax.setHumanReadable( true );
        cb.schemaObjectProduced( this, ldapSyntax.getOid(), ldapSyntax );

        /*
         * This is where we deviate.  An octet string may or may not be human readable.  Essentially
         * we are using this property of a syntax to determine if a value should be treated as binary
         * data or not.  It must be human readable always in order to get this property set to true.
         *
         * If we set this to true then userPasswords which implement this syntax are not treated as
         * binary attributes.  If that happens we can have data corruption due to UTF-8 handling.
         */
        ldapSyntax = new LdapSyntax( SchemaConstants.OCTET_STRING_SYNTAX ); 
        //,syntaxCheckerRegistry );
        ldapSyntax.addName( "Octet String" );
        ldapSyntax.setHumanReadable( false );
        cb.schemaObjectProduced( this, ldapSyntax.getOid(), ldapSyntax );

        /*
         * Value being represented        H-R OBJECT IDENTIFIER
         * ===================================================================
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
        ldapSyntax = new LdapSyntax( SchemaConstants.POSTAL_ADDRESS_SYNTAX ); 
        //,syntaxCheckerRegistry );
        ldapSyntax.addName( "Postal Address" );
        ldapSyntax.setHumanReadable( true );
        cb.schemaObjectProduced( this, ldapSyntax.getOid(), ldapSyntax );

        ldapSyntax = new LdapSyntax( SchemaConstants.PROTOCOL_INFORMATION_SYNTAX ); 
        //,syntaxCheckerRegistry );
        ldapSyntax.addName( "Protocol Information" );
        ldapSyntax.setHumanReadable( true );
        cb.schemaObjectProduced( this, ldapSyntax.getOid(), ldapSyntax );

        ldapSyntax = new LdapSyntax( SchemaConstants.PRESENTATION_ADDRESS_SYNTAX ); 
        //,syntaxCheckerRegistry );
        ldapSyntax.addName( "Presentation Address" );
        ldapSyntax.setHumanReadable( true );
        cb.schemaObjectProduced( this, ldapSyntax.getOid(), ldapSyntax );

        ldapSyntax = new LdapSyntax( SchemaConstants.PRINTABLE_STRING_SYNTAX ); 
        //,syntaxCheckerRegistry );
        ldapSyntax.addName( "Printable String" );
        ldapSyntax.setHumanReadable( true );
        cb.schemaObjectProduced( this, ldapSyntax.getOid(), ldapSyntax );

        ldapSyntax = new LdapSyntax( SchemaConstants.SUBTREE_SPECIFICATION_SYNTAX ); 
        //,syntaxCheckerRegistry );
        ldapSyntax.addName( "Subtree Specification" );
        ldapSyntax.setHumanReadable( true );
        cb.schemaObjectProduced( this, ldapSyntax.getOid(), ldapSyntax );

        ldapSyntax = new LdapSyntax( SchemaConstants.SUPPLIER_INFORMATION_SYNTAX ); 
        //,syntaxCheckerRegistry );
        ldapSyntax.addName( "Supplier Information" );
        ldapSyntax.setHumanReadable( true );
        cb.schemaObjectProduced( this, ldapSyntax.getOid(), ldapSyntax );

        ldapSyntax = new LdapSyntax( SchemaConstants.SUPPLIER_OR_CONSUMER_SYNTAX ); 
        //,syntaxCheckerRegistry );
        ldapSyntax.addName( "Supplier Or Consumer" );
        ldapSyntax.setHumanReadable( true );
        cb.schemaObjectProduced( this, ldapSyntax.getOid(), ldapSyntax );

        ldapSyntax = new LdapSyntax( SchemaConstants.SUPPLIER_AND_CONSUMER_SYNTAX ); 
        //,syntaxCheckerRegistry );
        ldapSyntax.addName( "Supplier And Consumer" );
        ldapSyntax.setHumanReadable( true );
        cb.schemaObjectProduced( this, ldapSyntax.getOid(), ldapSyntax );

        ldapSyntax = new LdapSyntax( SchemaConstants.SUPPORTED_ALGORITHM_SYNTAX ); 
        //,syntaxCheckerRegistry );
        ldapSyntax.addName( "Supported Algorithm" );
        ldapSyntax.setHumanReadable( false );
        cb.schemaObjectProduced( this, ldapSyntax.getOid(), ldapSyntax );

        ldapSyntax = new LdapSyntax( SchemaConstants.TELEPHONE_NUMBER_SYNTAX ); 
        //,syntaxCheckerRegistry );
        ldapSyntax.addName( "Telephone Number" );
        ldapSyntax.setHumanReadable( true );
        cb.schemaObjectProduced( this, ldapSyntax.getOid(), ldapSyntax );

        /*
         * Value being represented        H-R OBJECT IDENTIFIER
         * ==================================================================
         * 50 Teletex Terminal Identifier     Y  1.3.6.1.4.1.1466.115.121.1.51
         * 51 Telex Number                    Y  1.3.6.1.4.1.1466.115.121.1.52
         * 52 UTC Time                        Y  1.3.6.1.4.1.1466.115.121.1.53
         * 53 LDAP Syntax Description         Y  1.3.6.1.4.1.1466.115.121.1.54
         * 54 Modify Rights                   Y  1.3.6.1.4.1.1466.115.121.1.55
         * 55 LDAP BootstrapSchema Definition          Y  1.3.6.1.4.1.1466.115.121.1.56
         * 56 LDAP BootstrapSchema Description         Y  1.3.6.1.4.1.1466.115.121.1.57
         * 57 Substring Assertion             Y  1.3.6.1.4.1.1466.115.121.1.58
         */
        ldapSyntax = new LdapSyntax( SchemaConstants.TELETEX_TERMINAL_IDENTIFIER_SYNTAX ); 
        //,syntaxCheckerRegistry );
        ldapSyntax.addName( "Teletex Terminal Identifier" );
        ldapSyntax.setHumanReadable( true );
        cb.schemaObjectProduced( this, ldapSyntax.getOid(), ldapSyntax );

        ldapSyntax = new LdapSyntax( SchemaConstants.TELEX_NUMBER_SYNTAX ); 
        //,syntaxCheckerRegistry );
        ldapSyntax.addName( "Telex Number" );
        ldapSyntax.setHumanReadable( true );
        cb.schemaObjectProduced( this, ldapSyntax.getOid(), ldapSyntax );

        ldapSyntax = new LdapSyntax( SchemaConstants.UTC_TIME_SYNTAX ); 
        //,syntaxCheckerRegistry );
        ldapSyntax.addName( "UTC Time" );
        ldapSyntax.setHumanReadable( true );
        cb.schemaObjectProduced( this, ldapSyntax.getOid(), ldapSyntax );

        ldapSyntax = new LdapSyntax( SchemaConstants.LDAP_SYNTAX_DESCRIPTION_SYNTAX ); 
        //,syntaxCheckerRegistry );
        ldapSyntax.addName( "LDAP Syntax Description" );
        ldapSyntax.setHumanReadable( true );
        cb.schemaObjectProduced( this, ldapSyntax.getOid(), ldapSyntax );

        ldapSyntax = new LdapSyntax( SchemaConstants.MODIFY_RIGHTS_SYNTAX ); 
        //,syntaxCheckerRegistry );
        ldapSyntax.addName( "Modify Rights" );
        ldapSyntax.setHumanReadable( true );
        cb.schemaObjectProduced( this, ldapSyntax.getOid(), ldapSyntax );

        ldapSyntax = new LdapSyntax( SchemaConstants.LDAP_SCHEMA_DEFINITION_SYNTAX ); 
        //,syntaxCheckerRegistry );
        ldapSyntax.addName( "LDAP BootstrapSchema Definition" );
        ldapSyntax.setHumanReadable( true );
        cb.schemaObjectProduced( this, ldapSyntax.getOid(), ldapSyntax );

        ldapSyntax = new LdapSyntax( SchemaConstants.LDAP_SCHEMA_DESCRIPTION_SYNTAX );
        //, syntaxCheckerRegistry );
        ldapSyntax.addName( "LDAP BootstrapSchema Description" );
        ldapSyntax.setHumanReadable( true );
        cb.schemaObjectProduced( this, ldapSyntax.getOid(), ldapSyntax );

        ldapSyntax = new LdapSyntax( SchemaConstants.SUBSTRING_ASSERTION_SYNTAX );
        //, syntaxCheckerRegistry );
        ldapSyntax.addName( "Substring Assertion" );
        ldapSyntax.setHumanReadable( true );
        cb.schemaObjectProduced( this, ldapSyntax.getOid(), ldapSyntax );
        
        ldapSyntax = new LdapSyntax( SchemaConstants.ATTRIBUTE_CERTIFICATE_ASSERTION_SYNTAX );
        // ); //,syntaxCheckerRegistry );
        ldapSyntax.addName( "Trigger Specification" );
        ldapSyntax.setHumanReadable( true );
        cb.schemaObjectProduced( this, ldapSyntax.getOid(), ldapSyntax );
    }
}
