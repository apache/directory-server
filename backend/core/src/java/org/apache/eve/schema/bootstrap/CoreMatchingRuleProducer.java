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
import org.apache.ldap.common.schema.AbstractMatchingRule;

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
public class CoreMatchingRuleProducer implements BootstrapProducer
{
    public ProducerTypeEnum getType()
    {
        return null;
    }

    public void produce( BootstrapRegistries registries, ProducerCallback cb )
        throws NamingException
    {
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

        mrule = new MutableMatchingRule( "2.5.13.0", registries );
        mrule.setNames( new String[] { "objectIdentifierMatch" } );
        mrule.setSyntaxOid( "1.3.6.1.4.1.1466.115.121.1.38" );
        cb.schemaObjectProduced( this, mrule.getOid(), mrule );

        mrule = new MutableMatchingRule( "2.5.13.1", registries  );
        mrule.setNames( new String[] { "distinguishedNameMatch" } );
        mrule.setSyntaxOid( "1.3.6.1.4.1.1466.115.121.1.12" );
        cb.schemaObjectProduced( this, mrule.getOid(), mrule );

        mrule = new MutableMatchingRule( "2.5.13.2", registries  );
        mrule.setNames( new String[] { "caseIgnoreMatch" } );
        mrule.setSyntaxOid( "1.3.6.1.4.1.1466.115.121.1.15" );
        cb.schemaObjectProduced( this, mrule.getOid(), mrule );

        mrule = new MutableMatchingRule( "2.5.13.3", registries  );
        mrule.setNames( new String[] { "caseIgnoreOrderingMatch" } );
        mrule.setSyntaxOid( "1.3.6.1.4.1.1466.115.121.1.15" );
        cb.schemaObjectProduced( this, mrule.getOid(), mrule );

        mrule = new MutableMatchingRule( "2.5.13.4", registries  );
        mrule.setNames( new String[] { "caseIgnoreSubstringsMatch" } );
        mrule.setSyntaxOid( "1.3.6.1.4.1.1466.115.121.1.58" );
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

        mrule = new MutableMatchingRule( "2.5.13.8", registries  );
        mrule.setNames( new String[] { "numericStringMatch" } );
        mrule.setSyntaxOid( "1.3.6.1.4.1.1466.115.121.1.36" );
        cb.schemaObjectProduced( this, mrule.getOid(), mrule );

        mrule = new MutableMatchingRule( "2.5.13.10", registries  );
        mrule.setNames( new String[] { "numericStringSubstringsMatch" } );
        mrule.setSyntaxOid( "1.3.6.1.4.1.1466.115.121.1.58" );
        cb.schemaObjectProduced( this, mrule.getOid(), mrule );

        mrule = new MutableMatchingRule( "2.5.13.11", registries  );
        mrule.setNames( new String[] { "caseIgnoreListMatch" } );
        mrule.setSyntaxOid( "1.3.6.1.4.1.1466.115.121.1.41" );
        cb.schemaObjectProduced( this, mrule.getOid(), mrule );

        mrule = new MutableMatchingRule( "2.5.13.14", registries  );
        mrule.setNames( new String[] { "integerMatch" } );
        mrule.setSyntaxOid( "1.3.6.1.4.1.1466.115.121.1.27" );
        cb.schemaObjectProduced( this, mrule.getOid(), mrule );

        mrule = new MutableMatchingRule( "2.5.13.16", registries  );
        mrule.setNames( new String[] { "bitStringMatch" } );
        mrule.setSyntaxOid( "1.3.6.1.4.1.1466.115.121.1.6" );
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

        mrule = new MutableMatchingRule( "2.5.13.20", registries  );
        mrule.setNames( new String[] { "telephoneNumberMatch" } );
        mrule.setSyntaxOid( "1.3.6.1.4.1.1466.115.121.1.50" );
        cb.schemaObjectProduced( this, mrule.getOid(), mrule );

        mrule = new MutableMatchingRule( "2.5.13.21", registries  );
        mrule.setNames( new String[] { "telephoneNumberSubstringsMatch" } );
        mrule.setSyntaxOid( "1.3.6.1.4.1.1466.115.121.1.58" );
        cb.schemaObjectProduced( this, mrule.getOid(), mrule );

        mrule = new MutableMatchingRule( "2.5.13.22", registries  );
        mrule.setNames( new String[] { "presentationAddressMatch" } );
        mrule.setSyntaxOid( "1.3.6.1.4.1.1466.115.121.1.43" );
        cb.schemaObjectProduced( this, mrule.getOid(), mrule );

        mrule = new MutableMatchingRule( "2.5.13.23", registries  );
        mrule.setNames( new String[] { "uniqueMemberMatch" } );
        mrule.setSyntaxOid( "1.3.6.1.4.1.1466.115.121.1.34" );
        cb.schemaObjectProduced( this, mrule.getOid(), mrule );

        mrule = new MutableMatchingRule( "2.5.13.24", registries  );
        mrule.setNames( new String[] { "protocolInformationMatch" } );
        mrule.setSyntaxOid( "1.3.6.1.4.1.1466.115.121.1.42" );
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

        mrule = new MutableMatchingRule( "2.5.13.27", registries  );
        mrule.setNames( new String[] { "generalizedTimeMatch" } );
        mrule.setSyntaxOid( "1.3.6.1.4.1.1466.115.121.1.24" );
        cb.schemaObjectProduced( this, mrule.getOid(), mrule );

        mrule = new MutableMatchingRule( "2.5.13.28", registries  );
        mrule.setNames( new String[] { "generalizedTimeOrderingMatch" } );
        mrule.setSyntaxOid( "1.3.6.1.4.1.1466.115.121.1.24" );
        cb.schemaObjectProduced( this, mrule.getOid(), mrule );

        mrule = new MutableMatchingRule( "2.5.13.29", registries  );
        mrule.setNames( new String[] { "integerFirstComponentMatch" } );
        mrule.setSyntaxOid( "1.3.6.1.4.1.1466.115.121.1.27" );
        cb.schemaObjectProduced( this, mrule.getOid(), mrule );

        mrule = new MutableMatchingRule( "2.5.13.30", registries  );
        mrule.setNames( new String[] { "objectIdentifierFirstComponentMatch" } );
        mrule.setSyntaxOid( "1.3.6.1.4.1.1466.115.121.1.38" );
        cb.schemaObjectProduced( this, mrule.getOid(), mrule );

        mrule = new MutableMatchingRule( "1.3.6.1.4.1.1466.109.114.1", registries  );
        mrule.setNames( new String[] { "caseExactIA5Match" } );
        mrule.setSyntaxOid( "1.3.6.1.4.1.1466.115.121.1.26" );
        cb.schemaObjectProduced( this, mrule.getOid(), mrule );

        mrule = new MutableMatchingRule( "1.3.6.1.4.1.1466.109.114.2", registries  );
        mrule.setNames( new String[] { "caseIgnoreIA5Match" } );
        mrule.setSyntaxOid( "1.3.6.1.4.1.1466.115.121.1.26" );
        cb.schemaObjectProduced( this, mrule.getOid(), mrule );
    }


    private static class MutableMatchingRule extends AbstractMatchingRule
    {
        final SyntaxRegistry syntaxRegistry;
        final NormalizerRegistry normalizerRegistry;
        final ComparatorRegistry comparatorRegistry;
        String syntaxOid;


        public MutableMatchingRule( String oid, BootstrapRegistries registries )
        {
            super( oid );
            this.syntaxRegistry = registries.getSyntaxRegistry();
            this.normalizerRegistry = registries.getNormalizerRegistry();
            this.comparatorRegistry = registries.getComparatorRegistry();
        }


        protected void setNames( String[] names )
        {
            super.setNames( names );
        }

        protected void setSyntaxOid( String syntaxOid )
        {
            this.syntaxOid = syntaxOid;
        }

        protected void setDescription( String description )
        {
            super.setDescription( description );
        }

        protected void setObsolete( boolean isObsolete )
        {
            super.setObsolete( isObsolete );
        }


        // accessors


        public Syntax getSyntax() throws NamingException
        {
            return syntaxRegistry.lookup( syntaxOid );
        }

        public Comparator getComparator() throws NamingException
        {
            return comparatorRegistry.lookup( getOid() );
        }

        public Normalizer getNormalizer() throws NamingException
        {
            return normalizerRegistry.lookup( getOid() );
        }
    }
}
