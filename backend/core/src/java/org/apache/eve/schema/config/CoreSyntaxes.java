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
package org.apache.eve.schema.config;


import org.apache.ldap.common.schema.Syntax;
import org.apache.ldap.common.schema.BaseSyntax;
import org.apache.eve.schema.SyntaxCheckerRegistry;


/**
 * A syntax schema object configuration set.
 *
 * @author <a href="mailto:directory-dev@incubator.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class CoreSyntaxes implements SyntaxConfigSet
{
    /** an empty string array */
    private final static String[] EMPTY_ARRAY = new String[0];
    /** the DN of the owner of the objects within this SyntaxConfigSet */
    private static final String OWNER = "uid=admin,ou=system";
    /** the logical schema the objects within this SyntaxConfigSet belong to */  
    private static final String SCHEMA = "core";


    // ------------------------------------------------------------------------
    // Configuration Set Methods
    // ------------------------------------------------------------------------


    public String getOwner()
    {
        return OWNER;
    }


    public String getSchemaName()
    {
        return SCHEMA;
    }


    public String[] getDependentSchemas()
    {
        return EMPTY_ARRAY;
    }


    // ------------------------------------------------------------------------
    // Syntax Configuration Set Methods
    // ------------------------------------------------------------------------


    public Syntax[] load( SyntaxCheckerRegistry registry )
    {
        MutableSyntax[] syntaxes = new MutableSyntax[54];

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
        syntaxes[0] = new MutableSyntax( "1.3.6.1.4.1.1466.115.121.1.1" );
        syntaxes[0].setName( "ACI Item" );
        syntaxes[0].setHumanReadible( false );

        syntaxes[1] = new MutableSyntax( "1.3.6.1.4.1.1466.115.121.1.2" );
        syntaxes[1].setName( "Access Point" );
        syntaxes[1].setHumanReadible( true );

        syntaxes[2] = new MutableSyntax( "1.3.6.1.4.1.1466.115.121.1.3" );
        syntaxes[2].setName( "Attribute Type Description" );
        syntaxes[2].setHumanReadible( true );

        syntaxes[3] = new MutableSyntax( "1.3.6.1.4.1.1466.115.121.1.4" );
        syntaxes[3].setName( "Audio" );
        syntaxes[3].setHumanReadible( false );

        syntaxes[4] = new MutableSyntax( "1.3.6.1.4.1.1466.115.121.1.5" );
        syntaxes[4].setName( "Binary" );
        syntaxes[4].setHumanReadible( false );

        syntaxes[5] = new MutableSyntax( "1.3.6.1.4.1.1466.115.121.1.6" );
        syntaxes[5].setName( "Bit String" );
        syntaxes[5].setHumanReadible( true );

        syntaxes[6] = new MutableSyntax( "1.3.6.1.4.1.1466.115.121.1.7" );
        syntaxes[6].setName( "Boolean" );
        syntaxes[6].setHumanReadible( true );

        syntaxes[7] = new MutableSyntax( "1.3.6.1.4.1.1466.115.121.1.8" );
        syntaxes[7].setName( "Certificate" );
        syntaxes[7].setHumanReadible( false );

        syntaxes[8] = new MutableSyntax( "1.3.6.1.4.1.1466.115.121.1.9" );
        syntaxes[8].setName( "Certificate List" );
        syntaxes[8].setHumanReadible( false );

        syntaxes[9] = new MutableSyntax( "1.3.6.1.4.1.1466.115.121.1.10" );
        syntaxes[9].setName( "Certificate Pair" );
        syntaxes[9].setHumanReadible( false );

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
        syntaxes[10] = new MutableSyntax( "1.3.6.1.4.1.1466.115.121.1.11" );
        syntaxes[10].setName( "Country String" );
        syntaxes[10].setHumanReadible( true );

        syntaxes[11] = new MutableSyntax( "1.3.6.1.4.1.1466.115.121.1.12" );
        syntaxes[11].setName( "DN" );
        syntaxes[11].setHumanReadible( true );

        syntaxes[12] = new MutableSyntax( "1.3.6.1.4.1.1466.115.121.1.13" );
        syntaxes[12].setName( "Data Quality Syntax" );
        syntaxes[12].setHumanReadible( true );

        syntaxes[13] = new MutableSyntax( "1.3.6.1.4.1.1466.115.121.1.14" );
        syntaxes[13].setName( "Delivery Method" );
        syntaxes[13].setHumanReadible( true );

        syntaxes[14] = new MutableSyntax( "1.3.6.1.4.1.1466.115.121.1.15" );
        syntaxes[14].setName( "Directory String" );
        syntaxes[14].setHumanReadible( true );

        syntaxes[15] = new MutableSyntax( "1.3.6.1.4.1.1466.115.121.1.16" );
        syntaxes[15].setName( "DIT Content Rule Description" );
        syntaxes[15].setHumanReadible( true );

        syntaxes[16] = new MutableSyntax( "1.3.6.1.4.1.1466.115.121.1.17" );
        syntaxes[16].setName( "DIT Structure Rule Description" );
        syntaxes[16].setHumanReadible( true );

        syntaxes[17] = new MutableSyntax( "1.3.6.1.4.1.1466.115.121.1.18" );
        syntaxes[17].setName( "DL Submit Permission" );
        syntaxes[17].setHumanReadible( true );

        syntaxes[18] = new MutableSyntax( "1.3.6.1.4.1.1466.115.121.1.19" );
        syntaxes[18].setName( "DSA Quality Syntax" );
        syntaxes[18].setHumanReadible( true );

        syntaxes[19] = new MutableSyntax( "1.3.6.1.4.1.1466.115.121.1.20" );
        syntaxes[19].setName( "DSE Type" );
        syntaxes[19].setHumanReadible( true );

        /*
         * Value being represented        H-R OBJECT IDENTIFIER
         * ===================================================================
         Enhanced Guide                  Y  1.3.6.1.4.1.1466.115.121.1.21
         Facsimile Telephone Number      Y  1.3.6.1.4.1.1466.115.121.1.22
         Fax                             N  1.3.6.1.4.1.1466.115.121.1.23
         Generalized Time                Y  1.3.6.1.4.1.1466.115.121.1.24
         Guide                           Y  1.3.6.1.4.1.1466.115.121.1.25
         IA5 String                      Y  1.3.6.1.4.1.1466.115.121.1.26
         INTEGER                         Y  1.3.6.1.4.1.1466.115.121.1.27
         JPEG                            N  1.3.6.1.4.1.1466.115.121.1.28
         Master And Shadow Access Points Y  1.3.6.1.4.1.1466.115.121.1.29
         Matching Rule Description       Y  1.3.6.1.4.1.1466.115.121.1.30
         */
        syntaxes[20] = new MutableSyntax( "1.3.6.1.4.1.1466.115.121.1.21" );
        syntaxes[20].setName( "Enhanced Guide" );
        syntaxes[20].setHumanReadible( true );

        syntaxes[21] = new MutableSyntax( "1.3.6.1.4.1.1466.115.121.1.22" );
        syntaxes[21].setName( "Facsimile Telephone Number" );
        syntaxes[21].setHumanReadible( true );

        syntaxes[22] = new MutableSyntax( "1.3.6.1.4.1.1466.115.121.1.23" );
        syntaxes[22].setName( "Fax" );
        syntaxes[22].setHumanReadible( false );

        syntaxes[23] = new MutableSyntax( "1.3.6.1.4.1.1466.115.121.1.24" );
        syntaxes[23].setName( "Generalized Time" );
        syntaxes[23].setHumanReadible( true );

        syntaxes[24] = new MutableSyntax( "1.3.6.1.4.1.1466.115.121.1.25" );
        syntaxes[24].setName( "Guide" );
        syntaxes[24].setHumanReadible( true );

        syntaxes[25] = new MutableSyntax( "1.3.6.1.4.1.1466.115.121.1.26" );
        syntaxes[25].setName( "IA5 String" );
        syntaxes[25].setHumanReadible( true );

        syntaxes[26] = new MutableSyntax( "1.3.6.1.4.1.1466.115.121.1.27" );
        syntaxes[26].setName( "INTEGER" );
        syntaxes[26].setHumanReadible( true );

        syntaxes[27] = new MutableSyntax( "1.3.6.1.4.1.1466.115.121.1.28" );
        syntaxes[27].setName( "JPEG" );
        syntaxes[27].setHumanReadible( false );

        syntaxes[28] = new MutableSyntax( "1.3.6.1.4.1.1466.115.121.1.29" );
        syntaxes[28].setName( "Master And Shadow Access Points" );
        syntaxes[28].setHumanReadible( true );

        syntaxes[29] = new MutableSyntax( "1.3.6.1.4.1.1466.115.121.1.30" );
        syntaxes[29].setName( "Matching Rule Description" );
        syntaxes[29].setHumanReadible( true );

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
        syntaxes[30] = new MutableSyntax( "1.3.6.1.4.1.1466.115.121.1.31" );
        syntaxes[30].setName( "Matching Rule Use Description" );
        syntaxes[30].setHumanReadible( true );

        syntaxes[31] = new MutableSyntax( "1.3.6.1.4.1.1466.115.121.1.32" );
        syntaxes[31].setName( "Mail Preference" );
        syntaxes[31].setHumanReadible( true );

        syntaxes[32] = new MutableSyntax( "1.3.6.1.4.1.1466.115.121.1.33" );
        syntaxes[32].setName( "MHS OR Address" );
        syntaxes[32].setHumanReadible( true );

        syntaxes[33] = new MutableSyntax( "1.3.6.1.4.1.1466.115.121.1.34" );
        syntaxes[33].setName( "Name And Optional UID" );
        syntaxes[33].setHumanReadible( true );

        syntaxes[34] = new MutableSyntax( "1.3.6.1.4.1.1466.115.121.1.35" );
        syntaxes[34].setName( "Name Form Description" );
        syntaxes[34].setHumanReadible( true );

        syntaxes[35] = new MutableSyntax( "1.3.6.1.4.1.1466.115.121.1.36" );
        syntaxes[35].setName( "Numeric String" );
        syntaxes[35].setHumanReadible( true );

        syntaxes[36] = new MutableSyntax( "1.3.6.1.4.1.1466.115.121.1.37" );
        syntaxes[36].setName( "Object Class Description" );
        syntaxes[36].setHumanReadible( true );

        syntaxes[37] = new MutableSyntax( "1.3.6.1.4.1.1466.115.121.1.38" );
        syntaxes[37].setName( "OID" );
        syntaxes[37].setHumanReadible( true );

        syntaxes[38] = new MutableSyntax( "1.3.6.1.4.1.1466.115.121.1.39" );
        syntaxes[38].setName( "Other Mailbox" );
        syntaxes[38].setHumanReadible( true );

        syntaxes[39] = new MutableSyntax( "1.3.6.1.4.1.1466.115.121.1.40" );
        syntaxes[39].setName( "Octet String" );
        syntaxes[39].setHumanReadible( true );

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
        syntaxes[40] = new MutableSyntax( "1.3.6.1.4.1.1466.115.121.1.41" );
        syntaxes[40].setName( "Postal Address" );
        syntaxes[40].setHumanReadible( true );

        syntaxes[41] = new MutableSyntax( "1.3.6.1.4.1.1466.115.121.1.42" );
        syntaxes[41].setName( "Protocol Information" );
        syntaxes[41].setHumanReadible( true );

        syntaxes[42] = new MutableSyntax( "1.3.6.1.4.1.1466.115.121.1.43" );
        syntaxes[42].setName( "Presentation Address" );
        syntaxes[42].setHumanReadible( true );

        syntaxes[43] = new MutableSyntax( "1.3.6.1.4.1.1466.115.121.1.44" );
        syntaxes[43].setName( "Printable String" );
        syntaxes[43].setHumanReadible( true );

        syntaxes[44] = new MutableSyntax( "1.3.6.1.4.1.1466.115.121.1.45" );
        syntaxes[44].setName( "Subtree Specification" );
        syntaxes[44].setHumanReadible( true );

        syntaxes[45] = new MutableSyntax( "1.3.6.1.4.1.1466.115.121.1.46" );
        syntaxes[45].setName( "Supplier Information" );
        syntaxes[45].setHumanReadible( true );

        syntaxes[46] = new MutableSyntax( "1.3.6.1.4.1.1466.115.121.1.47" );
        syntaxes[46].setName( "Supplier Or Consumer" );
        syntaxes[46].setHumanReadible( true );

        syntaxes[47] = new MutableSyntax( "1.3.6.1.4.1.1466.115.121.1.48" );
        syntaxes[47].setName( "Supplier And Consumer" );
        syntaxes[47].setHumanReadible( true );

        syntaxes[48] = new MutableSyntax( "1.3.6.1.4.1.1466.115.121.1.49" );
        syntaxes[48].setName( "Supported Algorithm" );
        syntaxes[48].setHumanReadible( false );

        syntaxes[49] = new MutableSyntax( "1.3.6.1.4.1.1466.115.121.1.50" );
        syntaxes[49].setName( "Telephone Number" );
        syntaxes[49].setHumanReadible( true );

        /*
         * Value being represented        H-R OBJECT IDENTIFIER
         * ==================================================================
         *
         * 50 Teletex Terminal Identifier     Y  1.3.6.1.4.1.1466.115.121.1.51
         * 51 Telex Number                    Y  1.3.6.1.4.1.1466.115.121.1.52
         * 52 UTC Time                        Y  1.3.6.1.4.1.1466.115.121.1.53
         * 53 LDAP Syntax Description         Y  1.3.6.1.4.1.1466.115.121.1.54
         * 54 Modify Rights                   Y  1.3.6.1.4.1.1466.115.121.1.55
         * 55 LDAP Schema Definition          Y  1.3.6.1.4.1.1466.115.121.1.56
         * 56 LDAP Schema Description         Y  1.3.6.1.4.1.1466.115.121.1.57
         * 57 Substring Assertion             Y  1.3.6.1.4.1.1466.115.121.1.58
         */
        syntaxes[50] = new MutableSyntax( "1.3.6.1.4.1.1466.115.121.1.51" );
        syntaxes[50].setName( "Teletex Terminal Identifier" );
        syntaxes[50].setHumanReadible( true );

        syntaxes[51] = new MutableSyntax( "1.3.6.1.4.1.1466.115.121.1.52" );
        syntaxes[51].setName( "Telex Number" );
        syntaxes[51].setHumanReadible( true );

        syntaxes[52] = new MutableSyntax( "1.3.6.1.4.1.1466.115.121.1.53" );
        syntaxes[52].setName( "UTC Time" );
        syntaxes[52].setHumanReadible( true );

        syntaxes[53] = new MutableSyntax( "1.3.6.1.4.1.1466.115.121.1.54" );
        syntaxes[53].setName( "LDAP Syntax Description" );
        syntaxes[53].setHumanReadible( true );

        syntaxes[54] = new MutableSyntax( "1.3.6.1.4.1.1466.115.121.1.55" );
        syntaxes[54].setName( "Modify Rights" );
        syntaxes[54].setHumanReadible( true );

        syntaxes[55] = new MutableSyntax( "1.3.6.1.4.1.1466.115.121.1.56" );
        syntaxes[55].setName( "LDAP Schema Definition" );
        syntaxes[55].setHumanReadible( true );

        syntaxes[56] = new MutableSyntax( "1.3.6.1.4.1.1466.115.121.1.57" );
        syntaxes[56].setName( "LDAP Schema Description" );
        syntaxes[56].setHumanReadible( true );

        syntaxes[57] = new MutableSyntax( "1.3.6.1.4.1.1466.115.121.1.58" );
        syntaxes[57].setName( "Substring Assertion" );
        syntaxes[57].setHumanReadible( true );

        return syntaxes;
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
