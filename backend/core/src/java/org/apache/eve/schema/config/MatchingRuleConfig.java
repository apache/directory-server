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
import org.apache.ldap.common.schema.Normalizer;
import org.apache.ldap.common.schema.MatchingRule;
import org.apache.ldap.common.schema.BaseMatchingRule;

import org.apache.eve.schema.SyntaxRegistry;
import org.apache.eve.schema.NormalizerRegistry;
import org.apache.eve.schema.ComparatorRegistry;

import java.util.Comparator;
import javax.naming.NamingException;


/**
 * A simple maching rule configuration where objects and java code are used
 * to create matching rules.
 *
 * @author <a href="mailto:directory-dev@incubator.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class MatchingRuleConfig
{
    public MatchingRule[] loadMatchingRules( SyntaxRegistry registry,
                                             NormalizerRegistry normRegistry,
                                             ComparatorRegistry compRegistry )
        throws NamingException
    {
        MutableMatchingRule[] mrules = new MutableMatchingRule[21];

        /*
         * Straight out of RFC 2252: Section 8
         * =======================================
        ( 2.5.13.0 NAME 'objectIdentifierMatch'
          SYNTAX 1.3.6.1.4.1.1466.115.121.1.38 )

        ( 2.5.13.1 NAME 'distinguishedNameMatch'
          SYNTAX 1.3.6.1.4.1.1466.115.121.1.12 )

        ( 2.5.13.2 NAME 'caseIgnoreMatch'
          SYNTAX 1.3.6.1.4.1.1466.115.121.1.15 )

        ( 2.5.13.3 NAME 'caseIgnoreOrderingMatch'
          SYNTAX 1.3.6.1.4.1.1466.115.121.1.15 )

        ( 2.5.13.4 NAME 'caseIgnoreSubstringsMatch'
         SYNTAX 1.3.6.1.4.1.1466.115.121.1.58 )
        */

        mrules[0] = new MutableMatchingRule( "2.5.13.0" );
        mrules[0].setName( "objectIdentifierMatch" );
        mrules[0].setSyntax( registry.lookup( "1.3.6.1.4.1.1466.115.121.1.38" ) );

        mrules[1] = new MutableMatchingRule( "2.5.13.1" );
        mrules[1].setName( "distinguishedNameMatch" );
        mrules[1].setSyntax( registry.lookup( "" ) );

        mrules[2] = new MutableMatchingRule( "2.5.13.2" );
        mrules[2].setName( "caseIgnoreMatch" );
        mrules[2].setSyntax( registry.lookup( "1.3.6.1.4.1.1466.115.121.1.15" ) );

        mrules[3] = new MutableMatchingRule( "2.5.13.3" );
        mrules[3].setName( "caseIgnoreOrderingMatch" );
        mrules[3].setSyntax( registry.lookup( "1.3.6.1.4.1.1466.115.121.1.15" ) );

        mrules[4] = new MutableMatchingRule( "2.5.13.4" );
        mrules[4].setName( "caseIgnoreSubstringsMatch" );
        mrules[4].setSyntax( registry.lookup( "1.3.6.1.4.1.1466.115.121.1.58" ) );

        /*
         * Straight out of RFC 2252: Section 8
         * =======================================
        ( 2.5.13.8 NAME 'numericStringMatch'
          SYNTAX 1.3.6.1.4.1.1466.115.121.1.36 )

        ( 2.5.13.10 NAME 'numericStringSubstringsMatch'
          SYNTAX 1.3.6.1.4.1.1466.115.121.1.58 )

        ( 2.5.13.11 NAME 'caseIgnoreListMatch'
          SYNTAX 1.3.6.1.4.1.1466.115.121.1.41 )

        ( 2.5.13.14 NAME 'integerMatch'
          SYNTAX 1.3.6.1.4.1.1466.115.121.1.27 )

        ( 2.5.13.16 NAME 'bitStringMatch'
          SYNTAX 1.3.6.1.4.1.1466.115.121.1.6 )
        */

        mrules[5] = new MutableMatchingRule( "2.5.13.8" );
        mrules[5].setName( "numericStringMatch" );
        mrules[5].setSyntax( registry.lookup( "1.3.6.1.4.1.1466.115.121.1.36" ) );

        mrules[6] = new MutableMatchingRule( "2.5.13.10" );
        mrules[6].setName( "numericStringSubstringsMatch" );
        mrules[6].setSyntax( registry.lookup( "1.3.6.1.4.1.1466.115.121.1.58" ) );

        mrules[7] = new MutableMatchingRule( "2.5.13.11" );
        mrules[7].setName( "caseIgnoreListMatch" );
        mrules[7].setSyntax( registry.lookup( "1.3.6.1.4.1.1466.115.121.1.41" ) );

        mrules[8] = new MutableMatchingRule( "2.5.13.14" );
        mrules[8].setName( "integerMatch" );
        mrules[8].setSyntax( registry.lookup( "1.3.6.1.4.1.1466.115.121.1.27" ) );

        mrules[9] = new MutableMatchingRule( "2.5.13.16" );
        mrules[9].setName( "bitStringMatch" );
        mrules[9].setSyntax( registry.lookup( "1.3.6.1.4.1.1466.115.121.1.6" ) );

        /*
         * Straight out of RFC 2252: Section 8
         * =======================================
        ( 2.5.13.20 NAME 'telephoneNumberMatch'
          SYNTAX 1.3.6.1.4.1.1466.115.121.1.50 )

        ( 2.5.13.21 NAME 'telephoneNumberSubstringsMatch'
          SYNTAX 1.3.6.1.4.1.1466.115.121.1.58 )

        ( 2.5.13.22 NAME 'presentationAddressMatch'
          SYNTAX 1.3.6.1.4.1.1466.115.121.1.43 )

        ( 2.5.13.23 NAME 'uniqueMemberMatch'
          SYNTAX 1.3.6.1.4.1.1466.115.121.1.34 )

        ( 2.5.13.24 NAME 'protocolInformationMatch'
          SYNTAX 1.3.6.1.4.1.1466.115.121.1.42 )
        */

        mrules[10] = new MutableMatchingRule( "2.5.13.20" );
        mrules[10].setName( "telephoneNumberMatch" );
        mrules[10].setSyntax( registry.lookup( "1.3.6.1.4.1.1466.115.121.1.50" ) );

        mrules[11] = new MutableMatchingRule( "2.5.13.21" );
        mrules[11].setName( "telephoneNumberSubstringsMatch" );
        mrules[11].setSyntax( registry.lookup( "1.3.6.1.4.1.1466.115.121.1.58" ) );

        mrules[12] = new MutableMatchingRule( "2.5.13.22" );
        mrules[12].setName( "presentationAddressMatch" );
        mrules[12].setSyntax( registry.lookup( "1.3.6.1.4.1.1466.115.121.1.43" ) );

        mrules[13] = new MutableMatchingRule( "2.5.13.23" );
        mrules[13].setName( "uniqueMemberMatch" );
        mrules[13].setSyntax( registry.lookup( "1.3.6.1.4.1.1466.115.121.1.34" ) );

        mrules[14] = new MutableMatchingRule( "2.5.13.24" );
        mrules[14].setName( "protocolInformationMatch" );
        mrules[14].setSyntax( registry.lookup( "1.3.6.1.4.1.1466.115.121.1.42" ) );

        /*
         * Straight out of RFC 2252: Section 8
         * =======================================
        ( 2.5.13.27 NAME 'generalizedTimeMatch'
          SYNTAX 1.3.6.1.4.1.1466.115.121.1.24 )

        ( 2.5.13.28 NAME 'generalizedTimeOrderingMatch'
          SYNTAX 1.3.6.1.4.1.1466.115.121.1.24 )

        ( 2.5.13.29 NAME 'integerFirstComponentMatch'
          SYNTAX 1.3.6.1.4.1.1466.115.121.1.27 )

        ( 2.5.13.30 NAME 'objectIdentifierFirstComponentMatch'
          SYNTAX 1.3.6.1.4.1.1466.115.121.1.38 )

        ( 1.3.6.1.4.1.1466.109.114.1 NAME 'caseExactIA5Match'
          SYNTAX 1.3.6.1.4.1.1466.115.121.1.26 )

        ( 1.3.6.1.4.1.1466.109.114.2 NAME 'caseIgnoreIA5Match'
          SYNTAX 1.3.6.1.4.1.1466.115.121.1.26 )

        */

        mrules[15] = new MutableMatchingRule( "2.5.13.27" );
        mrules[15].setName( "generalizedTimeMatch" );
        mrules[15].setSyntax( registry.lookup( "1.3.6.1.4.1.1466.115.121.1.24" ) );

        mrules[16] = new MutableMatchingRule( "2.5.13.28" );
        mrules[16].setName( "generalizedTimeOrderingMatch" );
        mrules[16].setSyntax( registry.lookup( "1.3.6.1.4.1.1466.115.121.1.24" ) );

        mrules[17] = new MutableMatchingRule( "2.5.13.29" );
        mrules[17].setName( "integerFirstComponentMatch" );
        mrules[17].setSyntax( registry.lookup( "1.3.6.1.4.1.1466.115.121.1.27" ) );

        mrules[18] = new MutableMatchingRule( "2.5.13.30" );
        mrules[18].setName( "objectIdentifierFirstComponentMatch" );
        mrules[18].setSyntax( registry.lookup( "1.3.6.1.4.1.1466.115.121.1.38" ) );

        mrules[19] = new MutableMatchingRule( "1.3.6.1.4.1.1466.109.114.1" );
        mrules[19].setName( "caseExactIA5Match" );
        mrules[19].setSyntax( registry.lookup( "1.3.6.1.4.1.1466.115.121.1.26" ) );

        mrules[20] = new MutableMatchingRule( "1.3.6.1.4.1.1466.109.114.2" );
        mrules[20].setName( "caseIgnoreIA5Match" );
        mrules[20].setSyntax( registry.lookup( "1.3.6.1.4.1.1466.115.121.1.26" ) );

        return mrules;
    }


    private static class MutableMatchingRule extends BaseMatchingRule
    {
        public MutableMatchingRule( String oid )
        {
            super( oid );
        }

        protected void setName( String name )
        {
            super.setName( name );
        }

        protected void setSyntax( Syntax syntax )
        {
            super.setSyntax( syntax );
        }

        protected void setComparator( Comparator comparator )
        {
            super.setComparator( comparator );
        }

        protected void setNormalizer( Normalizer normalizer )
        {
            super.setNormalizer( normalizer );
        }

        protected void setDescription( String description )
        {
            super.setDescription( description );
        }

        protected void setObsolete( boolean isObsolete )
        {
            super.setObsolete( isObsolete );
        }
    }
}
