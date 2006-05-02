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
package org.apache.directory.server.core.schema.bootstrap;


import javax.naming.NamingException;

import org.apache.directory.server.core.schema.SyntaxCheckerRegistry;
import org.apache.directory.server.core.schema.bootstrap.ProducerTypeEnum;


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

    public void produce( BootstrapRegistries registries, ProducerCallback cb ) throws NamingException
    {
        BootstrapSyntax syntax;
        SyntaxCheckerRegistry syntaxCheckerRegistry = registries.getSyntaxCheckerRegistry();

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
        syntax = new BootstrapSyntax( "1.3.6.1.4.1.1466.115.121.1.1", syntaxCheckerRegistry );
        syntax.setNames( new String[]
            { "ACI Item" } );
        // This is in direct conflict with RFC 2252 but for us ACI Item is human readable
        syntax.setHumanReadible( true );
        cb.schemaObjectProduced( this, syntax.getOid(), syntax );

        syntax = new BootstrapSyntax( "1.3.6.1.4.1.1466.115.121.1.2", syntaxCheckerRegistry );
        syntax.setNames( new String[]
            { "Access Point" } );
        syntax.setHumanReadible( true );
        cb.schemaObjectProduced( this, syntax.getOid(), syntax );

        syntax = new BootstrapSyntax( "1.3.6.1.4.1.1466.115.121.1.3", syntaxCheckerRegistry );
        syntax.setNames( new String[]
            { "Attribute Type Description" } );
        syntax.setHumanReadible( true );
        cb.schemaObjectProduced( this, syntax.getOid(), syntax );

        syntax = new BootstrapSyntax( "1.3.6.1.4.1.1466.115.121.1.4", syntaxCheckerRegistry );
        syntax.setNames( new String[]
            { "Audio" } );
        syntax.setHumanReadible( false );
        cb.schemaObjectProduced( this, syntax.getOid(), syntax );

        syntax = new BootstrapSyntax( "1.3.6.1.4.1.1466.115.121.1.5", syntaxCheckerRegistry );
        syntax.setNames( new String[]
            { "Binary" } );
        syntax.setHumanReadible( false );
        cb.schemaObjectProduced( this, syntax.getOid(), syntax );

        syntax = new BootstrapSyntax( "1.3.6.1.4.1.1466.115.121.1.6", syntaxCheckerRegistry );
        syntax.setNames( new String[]
            { "Bit String" } );
        syntax.setHumanReadible( true );
        cb.schemaObjectProduced( this, syntax.getOid(), syntax );

        syntax = new BootstrapSyntax( "1.3.6.1.4.1.1466.115.121.1.7", syntaxCheckerRegistry );
        syntax.setNames( new String[]
            { "Boolean" } );
        syntax.setHumanReadible( true );
        cb.schemaObjectProduced( this, syntax.getOid(), syntax );

        syntax = new BootstrapSyntax( "1.3.6.1.4.1.1466.115.121.1.8", syntaxCheckerRegistry );
        syntax.setNames( new String[]
            { "Certificate" } );
        syntax.setHumanReadible( false );
        cb.schemaObjectProduced( this, syntax.getOid(), syntax );

        syntax = new BootstrapSyntax( "1.3.6.1.4.1.1466.115.121.1.9", syntaxCheckerRegistry );
        syntax.setNames( new String[]
            { "Certificate List" } );
        syntax.setHumanReadible( false );
        cb.schemaObjectProduced( this, syntax.getOid(), syntax );

        syntax = new BootstrapSyntax( "1.3.6.1.4.1.1466.115.121.1.10", syntaxCheckerRegistry );
        syntax.setNames( new String[]
            { "Certificate Pair" } );
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
        syntax = new BootstrapSyntax( "1.3.6.1.4.1.1466.115.121.1.11", syntaxCheckerRegistry );
        syntax.setNames( new String[]
            { "Country String" } );
        syntax.setHumanReadible( true );
        cb.schemaObjectProduced( this, syntax.getOid(), syntax );

        syntax = new BootstrapSyntax( "1.3.6.1.4.1.1466.115.121.1.12", syntaxCheckerRegistry );
        syntax.setNames( new String[]
            { "DN" } );
        syntax.setHumanReadible( true );
        cb.schemaObjectProduced( this, syntax.getOid(), syntax );

        syntax = new BootstrapSyntax( "1.3.6.1.4.1.1466.115.121.1.13", syntaxCheckerRegistry );
        syntax.setNames( new String[]
            { "Data Quality Syntax" } );
        syntax.setHumanReadible( true );
        cb.schemaObjectProduced( this, syntax.getOid(), syntax );

        syntax = new BootstrapSyntax( "1.3.6.1.4.1.1466.115.121.1.14", syntaxCheckerRegistry );
        syntax.setNames( new String[]
            { "Delivery Method" } );
        syntax.setHumanReadible( true );
        cb.schemaObjectProduced( this, syntax.getOid(), syntax );

        syntax = new BootstrapSyntax( "1.3.6.1.4.1.1466.115.121.1.15", syntaxCheckerRegistry );
        syntax.setNames( new String[]
            { "Directory String" } );
        syntax.setHumanReadible( true );
        cb.schemaObjectProduced( this, syntax.getOid(), syntax );

        syntax = new BootstrapSyntax( "1.3.6.1.4.1.1466.115.121.1.16", syntaxCheckerRegistry );
        syntax.setNames( new String[]
            { "DIT Content Rule Description" } );
        syntax.setHumanReadible( true );
        cb.schemaObjectProduced( this, syntax.getOid(), syntax );

        syntax = new BootstrapSyntax( "1.3.6.1.4.1.1466.115.121.1.17", syntaxCheckerRegistry );
        syntax.setNames( new String[]
            { "DIT Structure Rule Description" } );
        syntax.setHumanReadible( true );
        cb.schemaObjectProduced( this, syntax.getOid(), syntax );

        syntax = new BootstrapSyntax( "1.3.6.1.4.1.1466.115.121.1.18", syntaxCheckerRegistry );
        syntax.setNames( new String[]
            { "DL Submit Permission" } );
        syntax.setHumanReadible( true );
        cb.schemaObjectProduced( this, syntax.getOid(), syntax );

        syntax = new BootstrapSyntax( "1.3.6.1.4.1.1466.115.121.1.19", syntaxCheckerRegistry );
        syntax.setNames( new String[]
            { "DSA Quality Syntax" } );
        syntax.setHumanReadible( true );
        cb.schemaObjectProduced( this, syntax.getOid(), syntax );

        syntax = new BootstrapSyntax( "1.3.6.1.4.1.1466.115.121.1.20", syntaxCheckerRegistry );
        syntax.setNames( new String[]
            { "DSE Type" } );
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
        syntax = new BootstrapSyntax( "1.3.6.1.4.1.1466.115.121.1.21", syntaxCheckerRegistry );
        syntax.setNames( new String[]
            { "Enhanced Guide" } );
        syntax.setHumanReadible( true );
        cb.schemaObjectProduced( this, syntax.getOid(), syntax );

        syntax = new BootstrapSyntax( "1.3.6.1.4.1.1466.115.121.1.22", syntaxCheckerRegistry );
        syntax.setNames( new String[]
            { "Facsimile Telephone Number" } );
        syntax.setHumanReadible( true );
        cb.schemaObjectProduced( this, syntax.getOid(), syntax );

        syntax = new BootstrapSyntax( "1.3.6.1.4.1.1466.115.121.1.23", syntaxCheckerRegistry );
        syntax.setNames( new String[]
            { "Fax" } );
        syntax.setHumanReadible( false );
        cb.schemaObjectProduced( this, syntax.getOid(), syntax );

        syntax = new BootstrapSyntax( "1.3.6.1.4.1.1466.115.121.1.24", syntaxCheckerRegistry );
        syntax.setNames( new String[]
            { "Generalized Time" } );
        syntax.setHumanReadible( true );
        cb.schemaObjectProduced( this, syntax.getOid(), syntax );

        syntax = new BootstrapSyntax( "1.3.6.1.4.1.1466.115.121.1.25", syntaxCheckerRegistry );
        syntax.setNames( new String[]
            { "Guide" } );
        syntax.setHumanReadible( true );
        cb.schemaObjectProduced( this, syntax.getOid(), syntax );

        syntax = new BootstrapSyntax( "1.3.6.1.4.1.1466.115.121.1.26", syntaxCheckerRegistry );
        syntax.setNames( new String[]
            { "IA5 String" } );
        syntax.setHumanReadible( true );
        cb.schemaObjectProduced( this, syntax.getOid(), syntax );

        syntax = new BootstrapSyntax( "1.3.6.1.4.1.1466.115.121.1.27", syntaxCheckerRegistry );
        syntax.setNames( new String[]
            { "INTEGER" } );
        syntax.setHumanReadible( true );
        cb.schemaObjectProduced( this, syntax.getOid(), syntax );

        syntax = new BootstrapSyntax( "1.3.6.1.4.1.1466.115.121.1.28", syntaxCheckerRegistry );
        syntax.setNames( new String[]
            { "JPEG" } );
        syntax.setHumanReadible( false );
        cb.schemaObjectProduced( this, syntax.getOid(), syntax );

        syntax = new BootstrapSyntax( "1.3.6.1.4.1.1466.115.121.1.29", syntaxCheckerRegistry );
        syntax.setNames( new String[]
            { "Master And Shadow Access Points" } );
        syntax.setHumanReadible( true );
        cb.schemaObjectProduced( this, syntax.getOid(), syntax );

        syntax = new BootstrapSyntax( "1.3.6.1.4.1.1466.115.121.1.30", syntaxCheckerRegistry );
        syntax.setNames( new String[]
            { "Matching Rule Description" } );
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
        syntax = new BootstrapSyntax( "1.3.6.1.4.1.1466.115.121.1.31", syntaxCheckerRegistry );
        syntax.setNames( new String[]
            { "Matching Rule Use Description" } );
        syntax.setHumanReadible( true );
        cb.schemaObjectProduced( this, syntax.getOid(), syntax );

        syntax = new BootstrapSyntax( "1.3.6.1.4.1.1466.115.121.1.32", syntaxCheckerRegistry );
        syntax.setNames( new String[]
            { "Mail Preference" } );
        syntax.setHumanReadible( true );
        cb.schemaObjectProduced( this, syntax.getOid(), syntax );

        syntax = new BootstrapSyntax( "1.3.6.1.4.1.1466.115.121.1.33", syntaxCheckerRegistry );
        syntax.setNames( new String[]
            { "MHS OR Address" } );
        syntax.setHumanReadible( true );
        cb.schemaObjectProduced( this, syntax.getOid(), syntax );

        syntax = new BootstrapSyntax( "1.3.6.1.4.1.1466.115.121.1.34", syntaxCheckerRegistry );
        syntax.setNames( new String[]
            { "Name And Optional UID" } );
        syntax.setHumanReadible( true );
        cb.schemaObjectProduced( this, syntax.getOid(), syntax );

        syntax = new BootstrapSyntax( "1.3.6.1.4.1.1466.115.121.1.35", syntaxCheckerRegistry );
        syntax.setNames( new String[]
            { "Name Form Description" } );
        syntax.setHumanReadible( true );
        cb.schemaObjectProduced( this, syntax.getOid(), syntax );

        syntax = new BootstrapSyntax( "1.3.6.1.4.1.1466.115.121.1.36", syntaxCheckerRegistry );
        syntax.setNames( new String[]
            { "Numeric String" } );
        syntax.setHumanReadible( true );
        cb.schemaObjectProduced( this, syntax.getOid(), syntax );

        syntax = new BootstrapSyntax( "1.3.6.1.4.1.1466.115.121.1.37", syntaxCheckerRegistry );
        syntax.setNames( new String[]
            { "Object Class Description" } );
        syntax.setHumanReadible( true );
        cb.schemaObjectProduced( this, syntax.getOid(), syntax );

        syntax = new BootstrapSyntax( "1.3.6.1.4.1.1466.115.121.1.38", syntaxCheckerRegistry );
        syntax.setNames( new String[]
            { "OID" } );
        syntax.setHumanReadible( true );
        cb.schemaObjectProduced( this, syntax.getOid(), syntax );

        syntax = new BootstrapSyntax( "1.3.6.1.4.1.1466.115.121.1.39", syntaxCheckerRegistry );
        syntax.setNames( new String[]
            { "Other Mailbox" } );
        syntax.setHumanReadible( true );
        cb.schemaObjectProduced( this, syntax.getOid(), syntax );

        /*
         * This is where we deviate.  An octet string may or may not be human readable.  Essentially
         * we are using this property of a syntax to determine if a value should be treated as binary
         * data or not.  It must be human readable always in order to get this property set to true.
         *
         * If we set this to true then userPasswords which implement this syntax are not treated as
         * binary attributes.  If that happens we can have data corruption due to UTF-8 handling.
         */
        syntax = new BootstrapSyntax( "1.3.6.1.4.1.1466.115.121.1.40", syntaxCheckerRegistry );
        syntax.setNames( new String[]
            { "Octet String" } );
        syntax.setHumanReadible( false );
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
        syntax = new BootstrapSyntax( "1.3.6.1.4.1.1466.115.121.1.41", syntaxCheckerRegistry );
        syntax.setNames( new String[]
            { "Postal Address" } );
        syntax.setHumanReadible( true );
        cb.schemaObjectProduced( this, syntax.getOid(), syntax );

        syntax = new BootstrapSyntax( "1.3.6.1.4.1.1466.115.121.1.42", syntaxCheckerRegistry );
        syntax.setNames( new String[]
            { "Protocol Information" } );
        syntax.setHumanReadible( true );
        cb.schemaObjectProduced( this, syntax.getOid(), syntax );

        syntax = new BootstrapSyntax( "1.3.6.1.4.1.1466.115.121.1.43", syntaxCheckerRegistry );
        syntax.setNames( new String[]
            { "Presentation Address" } );
        syntax.setHumanReadible( true );
        cb.schemaObjectProduced( this, syntax.getOid(), syntax );

        syntax = new BootstrapSyntax( "1.3.6.1.4.1.1466.115.121.1.44", syntaxCheckerRegistry );
        syntax.setNames( new String[]
            { "Printable String" } );
        syntax.setHumanReadible( true );
        cb.schemaObjectProduced( this, syntax.getOid(), syntax );

        syntax = new BootstrapSyntax( "1.3.6.1.4.1.1466.115.121.1.45", syntaxCheckerRegistry );
        syntax.setNames( new String[]
            { "Subtree Specification" } );
        syntax.setHumanReadible( true );
        cb.schemaObjectProduced( this, syntax.getOid(), syntax );

        syntax = new BootstrapSyntax( "1.3.6.1.4.1.1466.115.121.1.46", syntaxCheckerRegistry );
        syntax.setNames( new String[]
            { "Supplier Information" } );
        syntax.setHumanReadible( true );
        cb.schemaObjectProduced( this, syntax.getOid(), syntax );

        syntax = new BootstrapSyntax( "1.3.6.1.4.1.1466.115.121.1.47", syntaxCheckerRegistry );
        syntax.setNames( new String[]
            { "Supplier Or Consumer" } );
        syntax.setHumanReadible( true );
        cb.schemaObjectProduced( this, syntax.getOid(), syntax );

        syntax = new BootstrapSyntax( "1.3.6.1.4.1.1466.115.121.1.48", syntaxCheckerRegistry );
        syntax.setNames( new String[]
            { "Supplier And Consumer" } );
        syntax.setHumanReadible( true );
        cb.schemaObjectProduced( this, syntax.getOid(), syntax );

        syntax = new BootstrapSyntax( "1.3.6.1.4.1.1466.115.121.1.49", syntaxCheckerRegistry );
        syntax.setNames( new String[]
            { "Supported Algorithm" } );
        syntax.setHumanReadible( false );
        cb.schemaObjectProduced( this, syntax.getOid(), syntax );

        syntax = new BootstrapSyntax( "1.3.6.1.4.1.1466.115.121.1.50", syntaxCheckerRegistry );
        syntax.setNames( new String[]
            { "Telephone Number" } );
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
        syntax = new BootstrapSyntax( "1.3.6.1.4.1.1466.115.121.1.51", syntaxCheckerRegistry );
        syntax.setNames( new String[]
            { "Teletex Terminal Identifier" } );
        syntax.setHumanReadible( true );
        cb.schemaObjectProduced( this, syntax.getOid(), syntax );

        syntax = new BootstrapSyntax( "1.3.6.1.4.1.1466.115.121.1.52", syntaxCheckerRegistry );
        syntax.setNames( new String[]
            { "Telex Number" } );
        syntax.setHumanReadible( true );
        cb.schemaObjectProduced( this, syntax.getOid(), syntax );

        syntax = new BootstrapSyntax( "1.3.6.1.4.1.1466.115.121.1.53", syntaxCheckerRegistry );
        syntax.setNames( new String[]
            { "UTC Time" } );
        syntax.setHumanReadible( true );
        cb.schemaObjectProduced( this, syntax.getOid(), syntax );

        syntax = new BootstrapSyntax( "1.3.6.1.4.1.1466.115.121.1.54", syntaxCheckerRegistry );
        syntax.setNames( new String[]
            { "LDAP Syntax Description" } );
        syntax.setHumanReadible( true );
        cb.schemaObjectProduced( this, syntax.getOid(), syntax );

        syntax = new BootstrapSyntax( "1.3.6.1.4.1.1466.115.121.1.55", syntaxCheckerRegistry );
        syntax.setNames( new String[]
            { "Modify Rights" } );
        syntax.setHumanReadible( true );
        cb.schemaObjectProduced( this, syntax.getOid(), syntax );

        syntax = new BootstrapSyntax( "1.3.6.1.4.1.1466.115.121.1.56", syntaxCheckerRegistry );
        syntax.setNames( new String[]
            { "LDAP BootstrapSchema Definition" } );
        syntax.setHumanReadible( true );
        cb.schemaObjectProduced( this, syntax.getOid(), syntax );

        syntax = new BootstrapSyntax( "1.3.6.1.4.1.1466.115.121.1.57", syntaxCheckerRegistry );
        syntax.setNames( new String[]
            { "LDAP BootstrapSchema Description" } );
        syntax.setHumanReadible( true );
        cb.schemaObjectProduced( this, syntax.getOid(), syntax );

        syntax = new BootstrapSyntax( "1.3.6.1.4.1.1466.115.121.1.58", syntaxCheckerRegistry );
        syntax.setNames( new String[]
            { "Substring Assertion" } );
        syntax.setHumanReadible( true );
        cb.schemaObjectProduced( this, syntax.getOid(), syntax );
        
        syntax = new BootstrapSyntax( "1.3.6.1.4.1.1466.115.121.1.59", syntaxCheckerRegistry );
        syntax.setNames( new String[]
            { "Trigger Specification" } );
        syntax.setHumanReadible( true );
        cb.schemaObjectProduced( this, syntax.getOid(), syntax );
    }
}
