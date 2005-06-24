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
package org.apache.ldap.server.schema.bootstrap;


import javax.naming.NamingException;

import org.apache.ldap.common.schema.AcceptAllSyntaxChecker;
import org.apache.ldap.common.schema.BinarySyntaxChecker;
import org.apache.ldap.common.schema.SyntaxChecker;


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


    public void produce( BootstrapRegistries registries, ProducerCallback cb )
            throws NamingException
    {
        SyntaxChecker syntaxChecker;

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
        syntaxChecker = new AcceptAllSyntaxChecker( "1.3.6.1.4.1.1466.115.121.1.1" );
        cb.schemaObjectProduced( this, syntaxChecker.getSyntaxOid(), syntaxChecker );

        syntaxChecker = new AcceptAllSyntaxChecker( "1.3.6.1.4.1.1466.115.121.1.2" );
        cb.schemaObjectProduced( this, syntaxChecker.getSyntaxOid(), syntaxChecker );

        syntaxChecker = new AcceptAllSyntaxChecker( "1.3.6.1.4.1.1466.115.121.1.3" );
        cb.schemaObjectProduced( this, syntaxChecker.getSyntaxOid(), syntaxChecker );

        syntaxChecker = new AcceptAllSyntaxChecker( "1.3.6.1.4.1.1466.115.121.1.4" );
        cb.schemaObjectProduced( this, syntaxChecker.getSyntaxOid(), syntaxChecker );

        syntaxChecker = BinarySyntaxChecker.INSTANCE;
        cb.schemaObjectProduced( this, syntaxChecker.getSyntaxOid(), syntaxChecker );

        syntaxChecker = new AcceptAllSyntaxChecker( "1.3.6.1.4.1.1466.115.121.1.6" );
        cb.schemaObjectProduced( this, syntaxChecker.getSyntaxOid(), syntaxChecker );

        syntaxChecker = new AcceptAllSyntaxChecker( "1.3.6.1.4.1.1466.115.121.1.7" );
        cb.schemaObjectProduced( this, syntaxChecker.getSyntaxOid(), syntaxChecker );

        syntaxChecker = new AcceptAllSyntaxChecker( "1.3.6.1.4.1.1466.115.121.1.8" );
        cb.schemaObjectProduced( this, syntaxChecker.getSyntaxOid(), syntaxChecker );

        syntaxChecker = new AcceptAllSyntaxChecker( "1.3.6.1.4.1.1466.115.121.1.9" );
        cb.schemaObjectProduced( this, syntaxChecker.getSyntaxOid(), syntaxChecker );

        syntaxChecker = new AcceptAllSyntaxChecker( "1.3.6.1.4.1.1466.115.121.1.10" );
        cb.schemaObjectProduced( this, syntaxChecker.getSyntaxOid(), syntaxChecker );

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
        syntaxChecker = new AcceptAllSyntaxChecker( "1.3.6.1.4.1.1466.115.121.1.11" );
        cb.schemaObjectProduced( this, syntaxChecker.getSyntaxOid(), syntaxChecker );

        syntaxChecker = new AcceptAllSyntaxChecker( "1.3.6.1.4.1.1466.115.121.1.12" );
        cb.schemaObjectProduced( this, syntaxChecker.getSyntaxOid(), syntaxChecker );

        syntaxChecker = new AcceptAllSyntaxChecker( "1.3.6.1.4.1.1466.115.121.1.13" );
        cb.schemaObjectProduced( this, syntaxChecker.getSyntaxOid(), syntaxChecker );

        syntaxChecker = new AcceptAllSyntaxChecker( "1.3.6.1.4.1.1466.115.121.1.14" );
        cb.schemaObjectProduced( this, syntaxChecker.getSyntaxOid(), syntaxChecker );

        syntaxChecker = new AcceptAllSyntaxChecker( "1.3.6.1.4.1.1466.115.121.1.15" );
        cb.schemaObjectProduced( this, syntaxChecker.getSyntaxOid(), syntaxChecker );

        syntaxChecker = new AcceptAllSyntaxChecker( "1.3.6.1.4.1.1466.115.121.1.16" );
        cb.schemaObjectProduced( this, syntaxChecker.getSyntaxOid(), syntaxChecker );

        syntaxChecker = new AcceptAllSyntaxChecker( "1.3.6.1.4.1.1466.115.121.1.17" );
        cb.schemaObjectProduced( this, syntaxChecker.getSyntaxOid(), syntaxChecker );

        syntaxChecker = new AcceptAllSyntaxChecker( "1.3.6.1.4.1.1466.115.121.1.18" );
        cb.schemaObjectProduced( this, syntaxChecker.getSyntaxOid(), syntaxChecker );

        syntaxChecker = new AcceptAllSyntaxChecker( "1.3.6.1.4.1.1466.115.121.1.19" );
        cb.schemaObjectProduced( this, syntaxChecker.getSyntaxOid(), syntaxChecker );

        syntaxChecker = new AcceptAllSyntaxChecker( "1.3.6.1.4.1.1466.115.121.1.20" );
        cb.schemaObjectProduced( this, syntaxChecker.getSyntaxOid(), syntaxChecker );


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
        syntaxChecker = new AcceptAllSyntaxChecker( "1.3.6.1.4.1.1466.115.121.1.21" );
        cb.schemaObjectProduced( this, syntaxChecker.getSyntaxOid(), syntaxChecker );

        syntaxChecker = new AcceptAllSyntaxChecker( "1.3.6.1.4.1.1466.115.121.1.22" );
        cb.schemaObjectProduced( this, syntaxChecker.getSyntaxOid(), syntaxChecker );

        syntaxChecker = new AcceptAllSyntaxChecker( "1.3.6.1.4.1.1466.115.121.1.23" );
        cb.schemaObjectProduced( this, syntaxChecker.getSyntaxOid(), syntaxChecker );

        syntaxChecker = new AcceptAllSyntaxChecker( "1.3.6.1.4.1.1466.115.121.1.24" );
        cb.schemaObjectProduced( this, syntaxChecker.getSyntaxOid(), syntaxChecker );

        syntaxChecker = new AcceptAllSyntaxChecker( "1.3.6.1.4.1.1466.115.121.1.25" );
        cb.schemaObjectProduced( this, syntaxChecker.getSyntaxOid(), syntaxChecker );

        syntaxChecker = new AcceptAllSyntaxChecker( "1.3.6.1.4.1.1466.115.121.1.26" );
        cb.schemaObjectProduced( this, syntaxChecker.getSyntaxOid(), syntaxChecker );

        syntaxChecker = new AcceptAllSyntaxChecker( "1.3.6.1.4.1.1466.115.121.1.27" );
        cb.schemaObjectProduced( this, syntaxChecker.getSyntaxOid(), syntaxChecker );

        syntaxChecker = new AcceptAllSyntaxChecker( "1.3.6.1.4.1.1466.115.121.1.28" );
        cb.schemaObjectProduced( this, syntaxChecker.getSyntaxOid(), syntaxChecker );

        syntaxChecker = new AcceptAllSyntaxChecker( "1.3.6.1.4.1.1466.115.121.1.29" );
        cb.schemaObjectProduced( this, syntaxChecker.getSyntaxOid(), syntaxChecker );

        syntaxChecker = new AcceptAllSyntaxChecker( "1.3.6.1.4.1.1466.115.121.1.30" );
        cb.schemaObjectProduced( this, syntaxChecker.getSyntaxOid(), syntaxChecker );


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
        syntaxChecker = new AcceptAllSyntaxChecker( "1.3.6.1.4.1.1466.115.121.1.31" );
        cb.schemaObjectProduced( this, syntaxChecker.getSyntaxOid(), syntaxChecker );

        syntaxChecker = new AcceptAllSyntaxChecker( "1.3.6.1.4.1.1466.115.121.1.32" );
        cb.schemaObjectProduced( this, syntaxChecker.getSyntaxOid(), syntaxChecker );

        syntaxChecker = new AcceptAllSyntaxChecker( "1.3.6.1.4.1.1466.115.121.1.33" );
        cb.schemaObjectProduced( this, syntaxChecker.getSyntaxOid(), syntaxChecker );

        syntaxChecker = new AcceptAllSyntaxChecker( "1.3.6.1.4.1.1466.115.121.1.34" );
        cb.schemaObjectProduced( this, syntaxChecker.getSyntaxOid(), syntaxChecker );

        syntaxChecker = new AcceptAllSyntaxChecker( "1.3.6.1.4.1.1466.115.121.1.35" );
        cb.schemaObjectProduced( this, syntaxChecker.getSyntaxOid(), syntaxChecker );

        syntaxChecker = new AcceptAllSyntaxChecker( "1.3.6.1.4.1.1466.115.121.1.36" );
        cb.schemaObjectProduced( this, syntaxChecker.getSyntaxOid(), syntaxChecker );

        syntaxChecker = new AcceptAllSyntaxChecker( "1.3.6.1.4.1.1466.115.121.1.37" );
        cb.schemaObjectProduced( this, syntaxChecker.getSyntaxOid(), syntaxChecker );

        syntaxChecker = new AcceptAllSyntaxChecker( "1.3.6.1.4.1.1466.115.121.1.38" );
        cb.schemaObjectProduced( this, syntaxChecker.getSyntaxOid(), syntaxChecker );

        syntaxChecker = new AcceptAllSyntaxChecker( "1.3.6.1.4.1.1466.115.121.1.39" );
        cb.schemaObjectProduced( this, syntaxChecker.getSyntaxOid(), syntaxChecker );

        syntaxChecker = new AcceptAllSyntaxChecker( "1.3.6.1.4.1.1466.115.121.1.40" );
        cb.schemaObjectProduced( this, syntaxChecker.getSyntaxOid(), syntaxChecker );


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
        syntaxChecker = new AcceptAllSyntaxChecker( "1.3.6.1.4.1.1466.115.121.1.41" );
        cb.schemaObjectProduced( this, syntaxChecker.getSyntaxOid(), syntaxChecker );

        syntaxChecker = new AcceptAllSyntaxChecker( "1.3.6.1.4.1.1466.115.121.1.42" );
        cb.schemaObjectProduced( this, syntaxChecker.getSyntaxOid(), syntaxChecker );

        syntaxChecker = new AcceptAllSyntaxChecker( "1.3.6.1.4.1.1466.115.121.1.43" );
        cb.schemaObjectProduced( this, syntaxChecker.getSyntaxOid(), syntaxChecker );

        syntaxChecker = new AcceptAllSyntaxChecker( "1.3.6.1.4.1.1466.115.121.1.44" );
        cb.schemaObjectProduced( this, syntaxChecker.getSyntaxOid(), syntaxChecker );

        syntaxChecker = new AcceptAllSyntaxChecker( "1.3.6.1.4.1.1466.115.121.1.45" );
        cb.schemaObjectProduced( this, syntaxChecker.getSyntaxOid(), syntaxChecker );

        syntaxChecker = new AcceptAllSyntaxChecker( "1.3.6.1.4.1.1466.115.121.1.46" );
        cb.schemaObjectProduced( this, syntaxChecker.getSyntaxOid(), syntaxChecker );

        syntaxChecker = new AcceptAllSyntaxChecker( "1.3.6.1.4.1.1466.115.121.1.47" );
        cb.schemaObjectProduced( this, syntaxChecker.getSyntaxOid(), syntaxChecker );

        syntaxChecker = new AcceptAllSyntaxChecker( "1.3.6.1.4.1.1466.115.121.1.48" );
        cb.schemaObjectProduced( this, syntaxChecker.getSyntaxOid(), syntaxChecker );

        syntaxChecker = new AcceptAllSyntaxChecker( "1.3.6.1.4.1.1466.115.121.1.49" );
        cb.schemaObjectProduced( this, syntaxChecker.getSyntaxOid(), syntaxChecker );

        syntaxChecker = new AcceptAllSyntaxChecker( "1.3.6.1.4.1.1466.115.121.1.50" );
        cb.schemaObjectProduced( this, syntaxChecker.getSyntaxOid(), syntaxChecker );


        /*
         * 50 Teletex Terminal Identifier     Y  1.3.6.1.4.1.1466.115.121.1.51
         * 51 Telex Number                    Y  1.3.6.1.4.1.1466.115.121.1.52
         * 52 UTC Time                        Y  1.3.6.1.4.1.1466.115.121.1.53
         * 53 LDAP Syntax Description         Y  1.3.6.1.4.1.1466.115.121.1.54
         * 54 Modify Rights                   Y  1.3.6.1.4.1.1466.115.121.1.55
         * 55 LDAP BootstrapSchema Definition Y  1.3.6.1.4.1.1466.115.121.1.56
         * 56 LDAP BootstrapSchema Description Y  1.3.6.1.4.1.1466.115.121.1.57
         * 57 Substring Assertion             Y  1.3.6.1.4.1.1466.115.121.1.58
         */
        syntaxChecker = new AcceptAllSyntaxChecker( "1.3.6.1.4.1.1466.115.121.1.51" );
        cb.schemaObjectProduced( this, syntaxChecker.getSyntaxOid(), syntaxChecker );

        syntaxChecker = new AcceptAllSyntaxChecker( "1.3.6.1.4.1.1466.115.121.1.52" );
        cb.schemaObjectProduced( this, syntaxChecker.getSyntaxOid(), syntaxChecker );

        syntaxChecker = new AcceptAllSyntaxChecker( "1.3.6.1.4.1.1466.115.121.1.53" );
        cb.schemaObjectProduced( this, syntaxChecker.getSyntaxOid(), syntaxChecker );

        syntaxChecker = new AcceptAllSyntaxChecker( "1.3.6.1.4.1.1466.115.121.1.54" );
        cb.schemaObjectProduced( this, syntaxChecker.getSyntaxOid(), syntaxChecker );

        syntaxChecker = new AcceptAllSyntaxChecker( "1.3.6.1.4.1.1466.115.121.1.55" );
        cb.schemaObjectProduced( this, syntaxChecker.getSyntaxOid(), syntaxChecker );

        syntaxChecker = new AcceptAllSyntaxChecker( "1.3.6.1.4.1.1466.115.121.1.56" );
        cb.schemaObjectProduced( this, syntaxChecker.getSyntaxOid(), syntaxChecker );

        syntaxChecker = new AcceptAllSyntaxChecker( "1.3.6.1.4.1.1466.115.121.1.57" );
        cb.schemaObjectProduced( this, syntaxChecker.getSyntaxOid(), syntaxChecker );

        syntaxChecker = new AcceptAllSyntaxChecker( "1.3.6.1.4.1.1466.115.121.1.58" );
        cb.schemaObjectProduced( this, syntaxChecker.getSyntaxOid(), syntaxChecker );
    }
}
