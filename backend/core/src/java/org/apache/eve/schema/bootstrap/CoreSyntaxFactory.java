/*
 *   Copyright 2004 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package org.apache.eve.schema.bootstrap;


import org.apache.ldap.common.schema.BaseSyntax;
import org.apache.eve.schema.SyntaxCheckerRegistry;

import javax.naming.NamingException;
import java.util.Map;
import java.util.HashMap;


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
 * 39 Octet String                    Y  1.3.6.1.4.1.1466.115.121.1.40
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
 * </pre>
 *
 *
 * @author <a href="mailto:directory-dev@incubator.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class CoreSyntaxFactory implements BootstrapProducer
{

    // ------------------------------------------------------------------------
    // BootstrapProducer Methods
    // ------------------------------------------------------------------------


    public ProducerTypeEnum getType()
    {
        return ProducerTypeEnum.SYNTAX_PRODUCER;
    }


    public void produce( BootstrapRegistries registries, ProducerCallback cb )
        throws NamingException
    {
        MutableSyntax syntax;

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
        syntax = new MutableSyntax( "1.3.6.1.4.1.1466.115.121.1.1" );
        syntax.setName( "ACI Item" );
        syntax.setHumanReadible( false );
        cb.schemaObjectProduced( this, syntax.getOid(), syntax );

        syntax = new MutableSyntax( "1.3.6.1.4.1.1466.115.121.1.2" );
        syntax.setName( "Access Point" );
        syntax.setHumanReadible( true );
        cb.schemaObjectProduced( this, syntax.getOid(), syntax );

        syntax = new MutableSyntax( "1.3.6.1.4.1.1466.115.121.1.3" );
        syntax.setName( "Attribute Type Description" );
        syntax.setHumanReadible( true );
        cb.schemaObjectProduced( this, syntax.getOid(), syntax );

        syntax = new MutableSyntax( "1.3.6.1.4.1.1466.115.121.1.4" );
        syntax.setName( "Audio" );
        syntax.setHumanReadible( false );
        cb.schemaObjectProduced( this, syntax.getOid(), syntax );

        syntax = new MutableSyntax( "1.3.6.1.4.1.1466.115.121.1.5" );
        syntax.setName( "Binary" );
        syntax.setHumanReadible( false );
        cb.schemaObjectProduced( this, syntax.getOid(), syntax );

        syntax = new MutableSyntax( "1.3.6.1.4.1.1466.115.121.1.6" );
        syntax.setName( "Bit String" );
        syntax.setHumanReadible( true );
        cb.schemaObjectProduced( this, syntax.getOid(), syntax );

        syntax = new MutableSyntax( "1.3.6.1.4.1.1466.115.121.1.7" );
        syntax.setName( "Boolean" );
        syntax.setHumanReadible( true );
        cb.schemaObjectProduced( this, syntax.getOid(), syntax );

        syntax = new MutableSyntax( "1.3.6.1.4.1.1466.115.121.1.8" );
        syntax.setName( "Certificate" );
        syntax.setHumanReadible( false );
        cb.schemaObjectProduced( this, syntax.getOid(), syntax );

        syntax = new MutableSyntax( "1.3.6.1.4.1.1466.115.121.1.9" );
        syntax.setName( "Certificate List" );
        syntax.setHumanReadible( false );
        cb.schemaObjectProduced( this, syntax.getOid(), syntax );

        syntax = new MutableSyntax( "1.3.6.1.4.1.1466.115.121.1.10" );
        syntax.setName( "Certificate Pair" );
        syntax.setHumanReadible( false );
        cb.schemaObjectProduced( this, syntax.getOid(), syntax );

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
        syntax = new MutableSyntax( "1.3.6.1.4.1.1466.115.121.1.11" );
        syntax.setName( "Country String" );
        syntax.setHumanReadible( true );
        cb.schemaObjectProduced( this, syntax.getOid(), syntax );

        syntax = new MutableSyntax( "1.3.6.1.4.1.1466.115.121.1.12" );
        syntax.setName( "DN" );
        syntax.setHumanReadible( true );
        cb.schemaObjectProduced( this, syntax.getOid(), syntax );

        syntax = new MutableSyntax( "1.3.6.1.4.1.1466.115.121.1.13" );
        syntax.setName( "Data Quality Syntax" );
        syntax.setHumanReadible( true );
        cb.schemaObjectProduced( this, syntax.getOid(), syntax );

        syntax = new MutableSyntax( "1.3.6.1.4.1.1466.115.121.1.14" );
        syntax.setName( "Delivery Method" );
        syntax.setHumanReadible( true );
        cb.schemaObjectProduced( this, syntax.getOid(), syntax );

        syntax = new MutableSyntax( "1.3.6.1.4.1.1466.115.121.1.15" );
        syntax.setName( "Directory String" );
        syntax.setHumanReadible( true );
        cb.schemaObjectProduced( this, syntax.getOid(), syntax );

        syntax = new MutableSyntax( "1.3.6.1.4.1.1466.115.121.1.16" );
        syntax.setName( "DIT Content Rule Description" );
        syntax.setHumanReadible( true );
        cb.schemaObjectProduced( this, syntax.getOid(), syntax );

        syntax = new MutableSyntax( "1.3.6.1.4.1.1466.115.121.1.17" );
        syntax.setName( "DIT Structure Rule Description" );
        syntax.setHumanReadible( true );
        cb.schemaObjectProduced( this, syntax.getOid(), syntax );

        syntax = new MutableSyntax( "1.3.6.1.4.1.1466.115.121.1.18" );
        syntax.setName( "DL Submit Permission" );
        syntax.setHumanReadible( true );
        cb.schemaObjectProduced( this, syntax.getOid(), syntax );

        syntax = new MutableSyntax( "1.3.6.1.4.1.1466.115.121.1.19" );
        syntax.setName( "DSA Quality Syntax" );
        syntax.setHumanReadible( true );
        cb.schemaObjectProduced( this, syntax.getOid(), syntax );

        syntax = new MutableSyntax( "1.3.6.1.4.1.1466.115.121.1.20" );
        syntax.setName( "DSE Type" );
        syntax.setHumanReadible( true );
        cb.schemaObjectProduced( this, syntax.getOid(), syntax );

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
        syntax = new MutableSyntax( "1.3.6.1.4.1.1466.115.121.1.21" );
        syntax.setName( "Enhanced Guide" );
        syntax.setHumanReadible( true );
        cb.schemaObjectProduced( this, syntax.getOid(), syntax );

        syntax = new MutableSyntax( "1.3.6.1.4.1.1466.115.121.1.22" );
        syntax.setName( "Facsimile Telephone Number" );
        syntax.setHumanReadible( true );
        cb.schemaObjectProduced( this, syntax.getOid(), syntax );

        syntax = new MutableSyntax( "1.3.6.1.4.1.1466.115.121.1.23" );
        syntax.setName( "Fax" );
        syntax.setHumanReadible( false );
        cb.schemaObjectProduced( this, syntax.getOid(), syntax );

        syntax = new MutableSyntax( "1.3.6.1.4.1.1466.115.121.1.24" );
        syntax.setName( "Generalized Time" );
        syntax.setHumanReadible( true );
        cb.schemaObjectProduced( this, syntax.getOid(), syntax );

        syntax = new MutableSyntax( "1.3.6.1.4.1.1466.115.121.1.25" );
        syntax.setName( "Guide" );
        syntax.setHumanReadible( true );
        cb.schemaObjectProduced( this, syntax.getOid(), syntax );

        syntax = new MutableSyntax( "1.3.6.1.4.1.1466.115.121.1.26" );
        syntax.setName( "IA5 String" );
        syntax.setHumanReadible( true );
        cb.schemaObjectProduced( this, syntax.getOid(), syntax );

        syntax = new MutableSyntax( "1.3.6.1.4.1.1466.115.121.1.27" );
        syntax.setName( "INTEGER" );
        syntax.setHumanReadible( true );
        cb.schemaObjectProduced( this, syntax.getOid(), syntax );

        syntax = new MutableSyntax( "1.3.6.1.4.1.1466.115.121.1.28" );
        syntax.setName( "JPEG" );
        syntax.setHumanReadible( false );
        cb.schemaObjectProduced( this, syntax.getOid(), syntax );

        syntax = new MutableSyntax( "1.3.6.1.4.1.1466.115.121.1.29" );
        syntax.setName( "Master And Shadow Access Points" );
        syntax.setHumanReadible( true );
        cb.schemaObjectProduced( this, syntax.getOid(), syntax );

        syntax = new MutableSyntax( "1.3.6.1.4.1.1466.115.121.1.30" );
        syntax.setName( "Matching Rule Description" );
        syntax.setHumanReadible( true );
        cb.schemaObjectProduced( this, syntax.getOid(), syntax );

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
        syntax = new MutableSyntax( "1.3.6.1.4.1.1466.115.121.1.31" );
        syntax.setName( "Matching Rule Use Description" );
        syntax.setHumanReadible( true );
        cb.schemaObjectProduced( this, syntax.getOid(), syntax );

        syntax = new MutableSyntax( "1.3.6.1.4.1.1466.115.121.1.32" );
        syntax.setName( "Mail Preference" );
        syntax.setHumanReadible( true );
        cb.schemaObjectProduced( this, syntax.getOid(), syntax );

        syntax = new MutableSyntax( "1.3.6.1.4.1.1466.115.121.1.33" );
        syntax.setName( "MHS OR Address" );
        syntax.setHumanReadible( true );
        cb.schemaObjectProduced( this, syntax.getOid(), syntax );

        syntax = new MutableSyntax( "1.3.6.1.4.1.1466.115.121.1.34" );
        syntax.setName( "Name And Optional UID" );
        syntax.setHumanReadible( true );
        cb.schemaObjectProduced( this, syntax.getOid(), syntax );

        syntax = new MutableSyntax( "1.3.6.1.4.1.1466.115.121.1.35" );
        syntax.setName( "Name Form Description" );
        syntax.setHumanReadible( true );
        cb.schemaObjectProduced( this, syntax.getOid(), syntax );

        syntax = new MutableSyntax( "1.3.6.1.4.1.1466.115.121.1.36" );
        syntax.setName( "Numeric String" );
        syntax.setHumanReadible( true );
        cb.schemaObjectProduced( this, syntax.getOid(), syntax );

        syntax = new MutableSyntax( "1.3.6.1.4.1.1466.115.121.1.37" );
        syntax.setName( "Object Class Description" );
        syntax.setHumanReadible( true );
        cb.schemaObjectProduced( this, syntax.getOid(), syntax );

        syntax = new MutableSyntax( "1.3.6.1.4.1.1466.115.121.1.38" );
        syntax.setName( "OID" );
        syntax.setHumanReadible( true );
        cb.schemaObjectProduced( this, syntax.getOid(), syntax );

        syntax = new MutableSyntax( "1.3.6.1.4.1.1466.115.121.1.39" );
        syntax.setName( "Other Mailbox" );
        syntax.setHumanReadible( true );
        cb.schemaObjectProduced( this, syntax.getOid(), syntax );

        syntax = new MutableSyntax( "1.3.6.1.4.1.1466.115.121.1.40" );
        syntax.setName( "Octet String" );
        syntax.setHumanReadible( true );
        cb.schemaObjectProduced( this, syntax.getOid(), syntax );

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
        syntax = new MutableSyntax( "1.3.6.1.4.1.1466.115.121.1.41" );
        syntax.setName( "Postal Address" );
        syntax.setHumanReadible( true );
        cb.schemaObjectProduced( this, syntax.getOid(), syntax );

        syntax = new MutableSyntax( "1.3.6.1.4.1.1466.115.121.1.42" );
        syntax.setName( "Protocol Information" );
        syntax.setHumanReadible( true );
        cb.schemaObjectProduced( this, syntax.getOid(), syntax );

        syntax = new MutableSyntax( "1.3.6.1.4.1.1466.115.121.1.43" );
        syntax.setName( "Presentation Address" );
        syntax.setHumanReadible( true );
        cb.schemaObjectProduced( this, syntax.getOid(), syntax );

        syntax = new MutableSyntax( "1.3.6.1.4.1.1466.115.121.1.44" );
        syntax.setName( "Printable String" );
        syntax.setHumanReadible( true );
        cb.schemaObjectProduced( this, syntax.getOid(), syntax );

        syntax = new MutableSyntax( "1.3.6.1.4.1.1466.115.121.1.45" );
        syntax.setName( "Subtree Specification" );
        syntax.setHumanReadible( true );
        cb.schemaObjectProduced( this, syntax.getOid(), syntax );

        syntax = new MutableSyntax( "1.3.6.1.4.1.1466.115.121.1.46" );
        syntax.setName( "Supplier Information" );
        syntax.setHumanReadible( true );
        cb.schemaObjectProduced( this, syntax.getOid(), syntax );

        syntax = new MutableSyntax( "1.3.6.1.4.1.1466.115.121.1.47" );
        syntax.setName( "Supplier Or Consumer" );
        syntax.setHumanReadible( true );
        cb.schemaObjectProduced( this, syntax.getOid(), syntax );

        syntax = new MutableSyntax( "1.3.6.1.4.1.1466.115.121.1.48" );
        syntax.setName( "Supplier And Consumer" );
        syntax.setHumanReadible( true );
        cb.schemaObjectProduced( this, syntax.getOid(), syntax );

        syntax = new MutableSyntax( "1.3.6.1.4.1.1466.115.121.1.49" );
        syntax.setName( "Supported Algorithm" );
        syntax.setHumanReadible( false );
        cb.schemaObjectProduced( this, syntax.getOid(), syntax );

        syntax = new MutableSyntax( "1.3.6.1.4.1.1466.115.121.1.50" );
        syntax.setName( "Telephone Number" );
        syntax.setHumanReadible( true );
        cb.schemaObjectProduced( this, syntax.getOid(), syntax );

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
        syntax = new MutableSyntax( "1.3.6.1.4.1.1466.115.121.1.51" );
        syntax.setName( "Teletex Terminal Identifier" );
        syntax.setHumanReadible( true );
        cb.schemaObjectProduced( this, syntax.getOid(), syntax );

        syntax = new MutableSyntax( "1.3.6.1.4.1.1466.115.121.1.52" );
        syntax.setName( "Telex Number" );
        syntax.setHumanReadible( true );
        cb.schemaObjectProduced( this, syntax.getOid(), syntax );

        syntax = new MutableSyntax( "1.3.6.1.4.1.1466.115.121.1.53" );
        syntax.setName( "UTC Time" );
        syntax.setHumanReadible( true );
        cb.schemaObjectProduced( this, syntax.getOid(), syntax );

        syntax = new MutableSyntax( "1.3.6.1.4.1.1466.115.121.1.54" );
        syntax.setName( "LDAP Syntax Description" );
        syntax.setHumanReadible( true );
        cb.schemaObjectProduced( this, syntax.getOid(), syntax );

        syntax = new MutableSyntax( "1.3.6.1.4.1.1466.115.121.1.55" );
        syntax.setName( "Modify Rights" );
        syntax.setHumanReadible( true );
        cb.schemaObjectProduced( this, syntax.getOid(), syntax );

        syntax = new MutableSyntax( "1.3.6.1.4.1.1466.115.121.1.56" );
        syntax.setName( "LDAP BootstrapSchema Definition" );
        syntax.setHumanReadible( true );
        cb.schemaObjectProduced( this, syntax.getOid(), syntax );

        syntax = new MutableSyntax( "1.3.6.1.4.1.1466.115.121.1.57" );
        syntax.setName( "LDAP BootstrapSchema Description" );
        syntax.setHumanReadible( true );
        cb.schemaObjectProduced( this, syntax.getOid(), syntax );

        syntax = new MutableSyntax( "1.3.6.1.4.1.1466.115.121.1.58" );
        syntax.setName( "Substring Assertion" );
        syntax.setHumanReadible( true );
        cb.schemaObjectProduced( this, syntax.getOid(), syntax );
    }


    /**
     * Used to access protected mutators of BaseSyntax from within this class.
     */
    private static class MutableSyntax extends BaseSyntax
    {
        protected MutableSyntax( String oid )
        {
            super( oid );
        }


        protected void setHumanReadible( boolean isHumanReadible )
        {
            super.setHumanReadible( isHumanReadible );
        }


        protected void setName( String name )
        {
            super.setName( name );
        }
    }
}
