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


import org.apache.ldap.common.schema.Syntax;
import org.apache.ldap.common.schema.Normalizer;
import org.apache.ldap.common.schema.BaseMatchingRule;

import org.apache.eve.schema.SyntaxRegistry;

import java.util.Comparator;
import javax.naming.NamingException;


/**
 * A simple maching rule configuration where objects and java code are used
 * to create matching rules.
 *
 * @author <a href="mailto:directory-dev@incubator.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class CoreMatchingRuleProducer implements BootstrapProducer
{
    public ProducerTypeEnum getType()
    {
        return null;
    }

    public void produce( BootstrapRegistries registries, ProducerCallback cb )
        throws NamingException
    {
        SyntaxRegistry syntaxRegistry = registries.getSyntaxRegistry();
        MutableMatchingRule mrule = null;

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

        mrule = new MutableMatchingRule( "2.5.13.0" );
        mrule.setName( "objectIdentifierMatch" );
        mrule.setSyntax( syntaxRegistry.lookup( "1.3.6.1.4.1.1466.115.121.1.38" ) );
        cb.schemaObjectProduced( this, mrule.getOid(), mrule );

        mrule = new MutableMatchingRule( "2.5.13.1" );
        mrule.setName( "distinguishedNameMatch" );
        mrule.setSyntax( syntaxRegistry.lookup( "" ) );
        cb.schemaObjectProduced( this, mrule.getOid(), mrule );

        mrule = new MutableMatchingRule( "2.5.13.2" );
        mrule.setName( "caseIgnoreMatch" );
        mrule.setSyntax( syntaxRegistry.lookup( "1.3.6.1.4.1.1466.115.121.1.15" ) );
        cb.schemaObjectProduced( this, mrule.getOid(), mrule );

        mrule = new MutableMatchingRule( "2.5.13.3" );
        mrule.setName( "caseIgnoreOrderingMatch" );
        mrule.setSyntax( syntaxRegistry.lookup( "1.3.6.1.4.1.1466.115.121.1.15" ) );
        cb.schemaObjectProduced( this, mrule.getOid(), mrule );

        mrule = new MutableMatchingRule( "2.5.13.4" );
        mrule.setName( "caseIgnoreSubstringsMatch" );
        mrule.setSyntax( syntaxRegistry.lookup( "1.3.6.1.4.1.1466.115.121.1.58" ) );
        cb.schemaObjectProduced( this, mrule.getOid(), mrule );

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

        mrule = new MutableMatchingRule( "2.5.13.8" );
        mrule.setName( "numericStringMatch" );
        mrule.setSyntax( syntaxRegistry.lookup( "1.3.6.1.4.1.1466.115.121.1.36" ) );
        cb.schemaObjectProduced( this, mrule.getOid(), mrule );

        mrule = new MutableMatchingRule( "2.5.13.10" );
        mrule.setName( "numericStringSubstringsMatch" );
        mrule.setSyntax( syntaxRegistry.lookup( "1.3.6.1.4.1.1466.115.121.1.58" ) );
        cb.schemaObjectProduced( this, mrule.getOid(), mrule );

        mrule = new MutableMatchingRule( "2.5.13.11" );
        mrule.setName( "caseIgnoreListMatch" );
        mrule.setSyntax( syntaxRegistry.lookup( "1.3.6.1.4.1.1466.115.121.1.41" ) );
        cb.schemaObjectProduced( this, mrule.getOid(), mrule );

        mrule = new MutableMatchingRule( "2.5.13.14" );
        mrule.setName( "integerMatch" );
        mrule.setSyntax( syntaxRegistry.lookup( "1.3.6.1.4.1.1466.115.121.1.27" ) );
        cb.schemaObjectProduced( this, mrule.getOid(), mrule );

        mrule = new MutableMatchingRule( "2.5.13.16" );
        mrule.setName( "bitStringMatch" );
        mrule.setSyntax( syntaxRegistry.lookup( "1.3.6.1.4.1.1466.115.121.1.6" ) );
        cb.schemaObjectProduced( this, mrule.getOid(), mrule );

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

        mrule = new MutableMatchingRule( "2.5.13.20" );
        mrule.setName( "telephoneNumberMatch" );
        mrule.setSyntax( syntaxRegistry.lookup( "1.3.6.1.4.1.1466.115.121.1.50" ) );
        cb.schemaObjectProduced( this, mrule.getOid(), mrule );

        mrule = new MutableMatchingRule( "2.5.13.21" );
        mrule.setName( "telephoneNumberSubstringsMatch" );
        mrule.setSyntax( syntaxRegistry.lookup( "1.3.6.1.4.1.1466.115.121.1.58" ) );
        cb.schemaObjectProduced( this, mrule.getOid(), mrule );

        mrule = new MutableMatchingRule( "2.5.13.22" );
        mrule.setName( "presentationAddressMatch" );
        mrule.setSyntax( syntaxRegistry.lookup( "1.3.6.1.4.1.1466.115.121.1.43" ) );
        cb.schemaObjectProduced( this, mrule.getOid(), mrule );

        mrule = new MutableMatchingRule( "2.5.13.23" );
        mrule.setName( "uniqueMemberMatch" );
        mrule.setSyntax( syntaxRegistry.lookup( "1.3.6.1.4.1.1466.115.121.1.34" ) );
        cb.schemaObjectProduced( this, mrule.getOid(), mrule );

        mrule = new MutableMatchingRule( "2.5.13.24" );
        mrule.setName( "protocolInformationMatch" );
        mrule.setSyntax( syntaxRegistry.lookup( "1.3.6.1.4.1.1466.115.121.1.42" ) );
        cb.schemaObjectProduced( this, mrule.getOid(), mrule );

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

        mrule = new MutableMatchingRule( "2.5.13.27" );
        mrule.setName( "generalizedTimeMatch" );
        mrule.setSyntax( syntaxRegistry.lookup( "1.3.6.1.4.1.1466.115.121.1.24" ) );
        cb.schemaObjectProduced( this, mrule.getOid(), mrule );

        mrule = new MutableMatchingRule( "2.5.13.28" );
        mrule.setName( "generalizedTimeOrderingMatch" );
        mrule.setSyntax( syntaxRegistry.lookup( "1.3.6.1.4.1.1466.115.121.1.24" ) );
        cb.schemaObjectProduced( this, mrule.getOid(), mrule );

        mrule = new MutableMatchingRule( "2.5.13.29" );
        mrule.setName( "integerFirstComponentMatch" );
        mrule.setSyntax( syntaxRegistry.lookup( "1.3.6.1.4.1.1466.115.121.1.27" ) );
        cb.schemaObjectProduced( this, mrule.getOid(), mrule );

        mrule = new MutableMatchingRule( "2.5.13.30" );
        mrule.setName( "objectIdentifierFirstComponentMatch" );
        mrule.setSyntax( syntaxRegistry.lookup( "1.3.6.1.4.1.1466.115.121.1.38" ) );
        cb.schemaObjectProduced( this, mrule.getOid(), mrule );

        mrule = new MutableMatchingRule( "1.3.6.1.4.1.1466.109.114.1" );
        mrule.setName( "caseExactIA5Match" );
        mrule.setSyntax( syntaxRegistry.lookup( "1.3.6.1.4.1.1466.115.121.1.26" ) );
        cb.schemaObjectProduced( this, mrule.getOid(), mrule );

        mrule = new MutableMatchingRule( "1.3.6.1.4.1.1466.109.114.2" );
        mrule.setName( "caseIgnoreIA5Match" );
        mrule.setSyntax( syntaxRegistry.lookup( "1.3.6.1.4.1.1466.115.121.1.26" ) );
        cb.schemaObjectProduced( this, mrule.getOid(), mrule );
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
