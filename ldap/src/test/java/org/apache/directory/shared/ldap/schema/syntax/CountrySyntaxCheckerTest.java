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
package org.apache.directory.shared.ldap.schema.syntax;


import org.apache.directory.shared.ldap.schema.syntaxes.CountrySyntaxChecker;

import org.junit.Test;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Test cases for BitStringSyntaxChecker.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class CountrySyntaxCheckerTest
{
    CountrySyntaxChecker checker = new CountrySyntaxChecker();


    @Test
    public void testNullString()
    {
        assertFalse( checker.isValidSyntax( null ) );
    }


    @Test
    public void testEmptyString()
    {
        assertFalse( checker.isValidSyntax( "" ) );
    }


    @Test
    public void testOneCharString()
    {
        assertFalse( checker.isValidSyntax( "0" ) );
        assertFalse( checker.isValidSyntax( "'" ) );
        assertFalse( checker.isValidSyntax( "1" ) );
        assertFalse( checker.isValidSyntax( "B" ) );
    }
    
    
    @Test
    public void testCorrectCase()
    {
        assertTrue( checker.isValidSyntax( "AF" ) );
        assertTrue( checker.isValidSyntax( "AX" ) );
        assertTrue( checker.isValidSyntax( "AL" ) );
        assertTrue( checker.isValidSyntax( "DZ" ) );
        assertTrue( checker.isValidSyntax( "AS" ) );
        assertTrue( checker.isValidSyntax( "AD" ) );
        assertTrue( checker.isValidSyntax( "AO" ) );
        assertTrue( checker.isValidSyntax( "AI" ) );
        assertTrue( checker.isValidSyntax( "AQ" ) );
        assertTrue( checker.isValidSyntax( "AG" ) );
        assertTrue( checker.isValidSyntax( "AR" ) );
        assertTrue( checker.isValidSyntax( "AM" ) );
        assertTrue( checker.isValidSyntax( "AW" ) );
        assertTrue( checker.isValidSyntax( "AU" ) );
        assertTrue( checker.isValidSyntax( "AT" ) );
        assertTrue( checker.isValidSyntax( "AZ" ) );
        assertTrue( checker.isValidSyntax( "BS" ) );
        assertTrue( checker.isValidSyntax( "BH" ) );
        assertTrue( checker.isValidSyntax( "BD" ) );
        assertTrue( checker.isValidSyntax( "BB" ) );
        assertTrue( checker.isValidSyntax( "BY" ) );
        assertTrue( checker.isValidSyntax( "BE" ) );
        assertTrue( checker.isValidSyntax( "BZ" ) );
        assertTrue( checker.isValidSyntax( "BJ" ) );
        assertTrue( checker.isValidSyntax( "BM" ) );
        assertTrue( checker.isValidSyntax( "BT" ) );
        assertTrue( checker.isValidSyntax( "BO" ) );
        assertTrue( checker.isValidSyntax( "BA" ) );
        assertTrue( checker.isValidSyntax( "BW" ) );
        assertTrue( checker.isValidSyntax( "BV" ) );
        assertTrue( checker.isValidSyntax( "BR" ) );
        assertTrue( checker.isValidSyntax( "IO" ) );
        assertTrue( checker.isValidSyntax( "BN" ) );
        assertTrue( checker.isValidSyntax( "BG" ) );
        assertTrue( checker.isValidSyntax( "BF" ) );
        assertTrue( checker.isValidSyntax( "BI" ) );
        assertTrue( checker.isValidSyntax( "KH" ) );
        assertTrue( checker.isValidSyntax( "CM" ) );
        assertTrue( checker.isValidSyntax( "CA" ) );
        assertTrue( checker.isValidSyntax( "CV" ) );
        assertTrue( checker.isValidSyntax( "KY" ) );
        assertTrue( checker.isValidSyntax( "CF" ) );
        assertTrue( checker.isValidSyntax( "TD" ) );
        assertTrue( checker.isValidSyntax( "CL" ) );
        assertTrue( checker.isValidSyntax( "CN" ) );
        assertTrue( checker.isValidSyntax( "CX" ) );
        assertTrue( checker.isValidSyntax( "CC" ) );
        assertTrue( checker.isValidSyntax( "CO" ) );
        assertTrue( checker.isValidSyntax( "KM" ) );
        assertTrue( checker.isValidSyntax( "CG" ) );
        assertTrue( checker.isValidSyntax( "CD" ) );
        assertTrue( checker.isValidSyntax( "CK" ) );
        assertTrue( checker.isValidSyntax( "CR" ) );
        assertTrue( checker.isValidSyntax( "CI" ) );
        assertTrue( checker.isValidSyntax( "HR" ) );
        assertTrue( checker.isValidSyntax( "CU" ) );
        assertTrue( checker.isValidSyntax( "CY" ) );
        assertTrue( checker.isValidSyntax( "CZ" ) );
        assertTrue( checker.isValidSyntax( "DK" ) );
        assertTrue( checker.isValidSyntax( "DJ" ) );
        assertTrue( checker.isValidSyntax( "DM" ) );
        assertTrue( checker.isValidSyntax( "DO" ) );
        assertTrue( checker.isValidSyntax( "EC" ) );
        assertTrue( checker.isValidSyntax( "EG" ) );
        assertTrue( checker.isValidSyntax( "SV" ) );
        assertTrue( checker.isValidSyntax( "GQ" ) );
        assertTrue( checker.isValidSyntax( "ER" ) );
        assertTrue( checker.isValidSyntax( "EE" ) );
        assertTrue( checker.isValidSyntax( "ET" ) );
        assertTrue( checker.isValidSyntax( "FK" ) );
        assertTrue( checker.isValidSyntax( "FO" ) );
        assertTrue( checker.isValidSyntax( "FJ" ) );
        assertTrue( checker.isValidSyntax( "FI" ) );
        assertTrue( checker.isValidSyntax( "FR" ) );
        assertTrue( checker.isValidSyntax( "GF" ) );
        assertTrue( checker.isValidSyntax( "PF" ) );
        assertTrue( checker.isValidSyntax( "TF" ) );
        assertTrue( checker.isValidSyntax( "GA" ) );
        assertTrue( checker.isValidSyntax( "GM" ) );
        assertTrue( checker.isValidSyntax( "GE" ) );
        assertTrue( checker.isValidSyntax( "DE" ) );
        assertTrue( checker.isValidSyntax( "GH" ) );
        assertTrue( checker.isValidSyntax( "GI" ) );
        assertTrue( checker.isValidSyntax( "GR" ) );
        assertTrue( checker.isValidSyntax( "GL" ) );
        assertTrue( checker.isValidSyntax( "GD" ) );
        assertTrue( checker.isValidSyntax( "GP" ) );
        assertTrue( checker.isValidSyntax( "GU" ) );
        assertTrue( checker.isValidSyntax( "GT" ) );
        assertTrue( checker.isValidSyntax( "GG" ) );
        assertTrue( checker.isValidSyntax( "GN" ) );
        assertTrue( checker.isValidSyntax( "GW" ) );
        assertTrue( checker.isValidSyntax( "GY" ) );
        assertTrue( checker.isValidSyntax( "HT" ) );
        assertTrue( checker.isValidSyntax( "HM" ) );
        assertTrue( checker.isValidSyntax( "VA" ) );
        assertTrue( checker.isValidSyntax( "HN" ) );
        assertTrue( checker.isValidSyntax( "HK" ) );
        assertTrue( checker.isValidSyntax( "HU" ) );
        assertTrue( checker.isValidSyntax( "IS" ) );
        assertTrue( checker.isValidSyntax( "IN" ) );
        assertTrue( checker.isValidSyntax( "ID" ) );
        assertTrue( checker.isValidSyntax( "IR" ) );
        assertTrue( checker.isValidSyntax( "IQ" ) );
        assertTrue( checker.isValidSyntax( "IE" ) );
        assertTrue( checker.isValidSyntax( "IM" ) );
        assertTrue( checker.isValidSyntax( "IL" ) );
        assertTrue( checker.isValidSyntax( "IT" ) );
        assertTrue( checker.isValidSyntax( "JM" ) );
        assertTrue( checker.isValidSyntax( "JP" ) );
        assertTrue( checker.isValidSyntax( "JE" ) );
        assertTrue( checker.isValidSyntax( "JO" ) );
        assertTrue( checker.isValidSyntax( "KZ" ) );
        assertTrue( checker.isValidSyntax( "KE" ) );
        assertTrue( checker.isValidSyntax( "KI" ) );
        assertTrue( checker.isValidSyntax( "KP" ) );
        assertTrue( checker.isValidSyntax( "KR" ) );
        assertTrue( checker.isValidSyntax( "KW" ) );
        assertTrue( checker.isValidSyntax( "KG" ) );
        assertTrue( checker.isValidSyntax( "LA" ) );
        assertTrue( checker.isValidSyntax( "LV" ) );
        assertTrue( checker.isValidSyntax( "LB" ) );
        assertTrue( checker.isValidSyntax( "LS" ) );
        assertTrue( checker.isValidSyntax( "LR" ) );
        assertTrue( checker.isValidSyntax( "LY" ) );
        assertTrue( checker.isValidSyntax( "LI" ) );
        assertTrue( checker.isValidSyntax( "LT" ) );
        assertTrue( checker.isValidSyntax( "LU" ) );
        assertTrue( checker.isValidSyntax( "MO" ) );
        assertTrue( checker.isValidSyntax( "MK" ) );
        assertTrue( checker.isValidSyntax( "MG" ) );
        assertTrue( checker.isValidSyntax( "MW" ) );
        assertTrue( checker.isValidSyntax( "MY" ) );
        assertTrue( checker.isValidSyntax( "MV" ) );
        assertTrue( checker.isValidSyntax( "ML" ) );
        assertTrue( checker.isValidSyntax( "MT" ) );
        assertTrue( checker.isValidSyntax( "MH" ) );
        assertTrue( checker.isValidSyntax( "MQ" ) );
        assertTrue( checker.isValidSyntax( "MR" ) );
        assertTrue( checker.isValidSyntax( "MU" ) );
        assertTrue( checker.isValidSyntax( "YT" ) );
        assertTrue( checker.isValidSyntax( "MX" ) );
        assertTrue( checker.isValidSyntax( "FM" ) );
        assertTrue( checker.isValidSyntax( "MD" ) );
        assertTrue( checker.isValidSyntax( "MC" ) );
        assertTrue( checker.isValidSyntax( "MN" ) );
        assertTrue( checker.isValidSyntax( "ME" ) );
        assertTrue( checker.isValidSyntax( "MS" ) );
        assertTrue( checker.isValidSyntax( "MA" ) );
        assertTrue( checker.isValidSyntax( "MZ" ) );
        assertTrue( checker.isValidSyntax( "MM" ) );
        assertTrue( checker.isValidSyntax( "NA" ) );
        assertTrue( checker.isValidSyntax( "NR" ) );
        assertTrue( checker.isValidSyntax( "NP" ) );
        assertTrue( checker.isValidSyntax( "NL" ) );
        assertTrue( checker.isValidSyntax( "AN" ) );
        assertTrue( checker.isValidSyntax( "NC" ) );
        assertTrue( checker.isValidSyntax( "NZ" ) );
        assertTrue( checker.isValidSyntax( "NI" ) );
        assertTrue( checker.isValidSyntax( "NE" ) );
        assertTrue( checker.isValidSyntax( "NG" ) );
        assertTrue( checker.isValidSyntax( "NU" ) );
        assertTrue( checker.isValidSyntax( "NF" ) );
        assertTrue( checker.isValidSyntax( "MP" ) );
        assertTrue( checker.isValidSyntax( "NO" ) );
        assertTrue( checker.isValidSyntax( "OM" ) );
        assertTrue( checker.isValidSyntax( "PK" ) );
        assertTrue( checker.isValidSyntax( "PW" ) );
        assertTrue( checker.isValidSyntax( "PS" ) );
        assertTrue( checker.isValidSyntax( "PA" ) );
        assertTrue( checker.isValidSyntax( "PG" ) );
        assertTrue( checker.isValidSyntax( "PY" ) );
        assertTrue( checker.isValidSyntax( "PE" ) );
        assertTrue( checker.isValidSyntax( "PH" ) );
        assertTrue( checker.isValidSyntax( "PN" ) );
        assertTrue( checker.isValidSyntax( "PL" ) );
        assertTrue( checker.isValidSyntax( "PT" ) );
        assertTrue( checker.isValidSyntax( "PR" ) );
        assertTrue( checker.isValidSyntax( "QA" ) );
        assertTrue( checker.isValidSyntax( "RE" ) );
        assertTrue( checker.isValidSyntax( "RO" ) );
        assertTrue( checker.isValidSyntax( "RU" ) );
        assertTrue( checker.isValidSyntax( "RW" ) );
        assertTrue( checker.isValidSyntax( "SH" ) );
        assertTrue( checker.isValidSyntax( "KN" ) );
        assertTrue( checker.isValidSyntax( "LC" ) );
        assertTrue( checker.isValidSyntax( "PM" ) );
        assertTrue( checker.isValidSyntax( "VC" ) );
        assertTrue( checker.isValidSyntax( "WS" ) );
        assertTrue( checker.isValidSyntax( "SM" ) );
        assertTrue( checker.isValidSyntax( "ST" ) );
        assertTrue( checker.isValidSyntax( "SA" ) );
        assertTrue( checker.isValidSyntax( "SN" ) );
        assertTrue( checker.isValidSyntax( "RS" ) );
        assertTrue( checker.isValidSyntax( "SC" ) );
        assertTrue( checker.isValidSyntax( "SL" ) );
        assertTrue( checker.isValidSyntax( "SG" ) );
        assertTrue( checker.isValidSyntax( "SK" ) );
        assertTrue( checker.isValidSyntax( "SI" ) );
        assertTrue( checker.isValidSyntax( "SB" ) );
        assertTrue( checker.isValidSyntax( "SO" ) );
        assertTrue( checker.isValidSyntax( "ZA" ) );
        assertTrue( checker.isValidSyntax( "GS" ) );
        assertTrue( checker.isValidSyntax( "ES" ) );
        assertTrue( checker.isValidSyntax( "LK" ) );
        assertTrue( checker.isValidSyntax( "SD" ) );
        assertTrue( checker.isValidSyntax( "SR" ) );
        assertTrue( checker.isValidSyntax( "SJ" ) );
        assertTrue( checker.isValidSyntax( "SZ" ) );
        assertTrue( checker.isValidSyntax( "SE" ) );
        assertTrue( checker.isValidSyntax( "CH" ) );
        assertTrue( checker.isValidSyntax( "SY" ) );
        assertTrue( checker.isValidSyntax( "TW" ) );
        assertTrue( checker.isValidSyntax( "TJ" ) );
        assertTrue( checker.isValidSyntax( "TZ" ) );
        assertTrue( checker.isValidSyntax( "TH" ) );
        assertTrue( checker.isValidSyntax( "TL" ) );
        assertTrue( checker.isValidSyntax( "TG" ) );
        assertTrue( checker.isValidSyntax( "TK" ) );
        assertTrue( checker.isValidSyntax( "TO" ) );
        assertTrue( checker.isValidSyntax( "TT" ) );
        assertTrue( checker.isValidSyntax( "TN" ) );
        assertTrue( checker.isValidSyntax( "TR" ) );
        assertTrue( checker.isValidSyntax( "TM" ) );
        assertTrue( checker.isValidSyntax( "TC" ) );
        assertTrue( checker.isValidSyntax( "TV" ) );
        assertTrue( checker.isValidSyntax( "UG" ) );
        assertTrue( checker.isValidSyntax( "UA" ) );
        assertTrue( checker.isValidSyntax( "AE" ) );
        assertTrue( checker.isValidSyntax( "GB" ) );
        assertTrue( checker.isValidSyntax( "US" ) );
        assertTrue( checker.isValidSyntax( "UM" ) );
        assertTrue( checker.isValidSyntax( "UY" ) );
        assertTrue( checker.isValidSyntax( "UZ" ) );
        assertTrue( checker.isValidSyntax( "VU" ) );
        assertTrue( checker.isValidSyntax( "VE" ) );
        assertTrue( checker.isValidSyntax( "VN" ) );
        assertTrue( checker.isValidSyntax( "VG" ) );
        assertTrue( checker.isValidSyntax( "VI" ) );
        assertTrue( checker.isValidSyntax( "WF" ) );
        assertTrue( checker.isValidSyntax( "EH" ) );
        assertTrue( checker.isValidSyntax( "YE" ) );
        assertTrue( checker.isValidSyntax( "ZM" ) );
        assertTrue( checker.isValidSyntax( "ZW" ) );
    }
}
