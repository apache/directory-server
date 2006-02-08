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

import org.apache.directory.server.core.schema.bootstrap.ProducerTypeEnum;


/**
 * A simple maching rule configuration where objects and java code are used
 * to create matching rules.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class SystemMatchingRuleProducer extends AbstractBootstrapProducer
{
    public SystemMatchingRuleProducer()
    {
        super( ProducerTypeEnum.MATCHING_RULE_PRODUCER );
    }


    public void produce( BootstrapRegistries registries, ProducerCallback cb )
        throws NamingException
    {
        BootstrapMatchingRule mrule = null;

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

        mrule = new BootstrapMatchingRule( "2.5.13.0", registries );
        mrule.setNames( new String[] { "objectIdentifierMatch" } );
        mrule.setSyntaxOid( "1.3.6.1.4.1.1466.115.121.1.38" );
        cb.schemaObjectProduced( this, mrule.getOid(), mrule );

        mrule = new BootstrapMatchingRule( "2.5.13.1", registries  );
        mrule.setNames( new String[] { "distinguishedNameMatch" } );
        mrule.setSyntaxOid( "1.3.6.1.4.1.1466.115.121.1.12" );
        cb.schemaObjectProduced( this, mrule.getOid(), mrule );

        mrule = new BootstrapMatchingRule( "2.5.13.2", registries  );
        mrule.setNames( new String[] { "caseIgnoreMatch" } );
        mrule.setSyntaxOid( "1.3.6.1.4.1.1466.115.121.1.15" );
        cb.schemaObjectProduced( this, mrule.getOid(), mrule );

        mrule = new BootstrapMatchingRule( "2.5.13.3", registries  );
        mrule.setNames( new String[] { "caseIgnoreOrderingMatch" } );
        mrule.setSyntaxOid( "1.3.6.1.4.1.1466.115.121.1.15" );
        cb.schemaObjectProduced( this, mrule.getOid(), mrule );

        mrule = new BootstrapMatchingRule( "2.5.13.4", registries  );
        mrule.setNames( new String[] { "caseIgnoreSubstringsMatch" } );
        mrule.setSyntaxOid( "1.3.6.1.4.1.1466.115.121.1.58" );
        cb.schemaObjectProduced( this, mrule.getOid(), mrule );

        /*
         * Straight out of RFC 3698: Section 2.3
         * http://www.faqs.org/rfcs/rfc3698.html
         * =======================================
         ( 2.5.13.6 NAME 'caseExactOrderingMatch'
           SYNTAX 1.3.6.1.4.1.1466.115.121.1.15 )
         */

        mrule = new BootstrapMatchingRule( "2.5.13.6", registries  );
        mrule.setNames( new String[] { "caseExactOrderingMatch" } );
        mrule.setSyntaxOid( "1.3.6.1.4.1.1466.115.121.1.15" );
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

        ( 2.5.13.17 NAME 'octetStringMatch'
          SYNTAX 1.3.6.1.4.1.1466.115.121.1.40 )
        */

        mrule = new BootstrapMatchingRule( "2.5.13.8", registries  );
        mrule.setNames( new String[] { "numericStringMatch" } );
        mrule.setSyntaxOid( "1.3.6.1.4.1.1466.115.121.1.36" );
        cb.schemaObjectProduced( this, mrule.getOid(), mrule );

        mrule = new BootstrapMatchingRule( "2.5.13.10", registries  );
        mrule.setNames( new String[] { "numericStringSubstringsMatch" } );
        mrule.setSyntaxOid( "1.3.6.1.4.1.1466.115.121.1.58" );
        cb.schemaObjectProduced( this, mrule.getOid(), mrule );

        mrule = new BootstrapMatchingRule( "2.5.13.11", registries  );
        mrule.setNames( new String[] { "caseIgnoreListMatch" } );
        mrule.setSyntaxOid( "1.3.6.1.4.1.1466.115.121.1.41" );
        cb.schemaObjectProduced( this, mrule.getOid(), mrule );

        mrule = new BootstrapMatchingRule( "2.5.13.14", registries  );
        mrule.setNames( new String[] { "integerMatch" } );
        mrule.setSyntaxOid( "1.3.6.1.4.1.1466.115.121.1.27" );
        cb.schemaObjectProduced( this, mrule.getOid(), mrule );

        /*
         * Straight out of RFC 3698: Section 2.7
         * http://www.faqs.org/rfcs/rfc3698.html
         * =======================================
         ( 2.5.13.15 NAME 'integerOrderingMatch'
           SYNTAX 1.3.6.1.4.1.1466.115.121.1.27 )
         */

        mrule = new BootstrapMatchingRule( "2.5.13.15", registries  );
        mrule.setNames( new String[] { "integerOrderingMatch" } );
        mrule.setSyntaxOid( "1.3.6.1.4.1.1466.115.121.1.27" );
        cb.schemaObjectProduced( this, mrule.getOid(), mrule );

        mrule = new BootstrapMatchingRule( "2.5.13.16", registries  );
        mrule.setNames( new String[] { "bitStringMatch" } );
        mrule.setSyntaxOid( "1.3.6.1.4.1.1466.115.121.1.6" );
        cb.schemaObjectProduced( this, mrule.getOid(), mrule );

        mrule = new BootstrapMatchingRule( "2.5.13.17", registries  );
        mrule.setNames( new String[] { "octetStringMatch" } );
        mrule.setSyntaxOid( "1.3.6.1.4.1.1466.115.121.1.40" );
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

        mrule = new BootstrapMatchingRule( "2.5.13.20", registries  );
        mrule.setNames( new String[] { "telephoneNumberMatch" } );
        mrule.setSyntaxOid( "1.3.6.1.4.1.1466.115.121.1.50" );
        cb.schemaObjectProduced( this, mrule.getOid(), mrule );

        mrule = new BootstrapMatchingRule( "2.5.13.21", registries  );
        mrule.setNames( new String[] { "telephoneNumberSubstringsMatch" } );
        mrule.setSyntaxOid( "1.3.6.1.4.1.1466.115.121.1.58" );
        cb.schemaObjectProduced( this, mrule.getOid(), mrule );

        mrule = new BootstrapMatchingRule( "2.5.13.22", registries  );
        mrule.setNames( new String[] { "presentationAddressMatch" } );
        mrule.setSyntaxOid( "1.3.6.1.4.1.1466.115.121.1.43" );
        cb.schemaObjectProduced( this, mrule.getOid(), mrule );

        mrule = new BootstrapMatchingRule( "2.5.13.23", registries  );
        mrule.setNames( new String[] { "uniqueMemberMatch" } );
        mrule.setSyntaxOid( "1.3.6.1.4.1.1466.115.121.1.34" );
        cb.schemaObjectProduced( this, mrule.getOid(), mrule );

        mrule = new BootstrapMatchingRule( "2.5.13.24", registries  );
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

        mrule = new BootstrapMatchingRule( "2.5.13.27", registries  );
        mrule.setNames( new String[] { "generalizedTimeMatch" } );
        mrule.setSyntaxOid( "1.3.6.1.4.1.1466.115.121.1.24" );
        cb.schemaObjectProduced( this, mrule.getOid(), mrule );

        mrule = new BootstrapMatchingRule( "2.5.13.28", registries  );
        mrule.setNames( new String[] { "generalizedTimeOrderingMatch" } );
        mrule.setSyntaxOid( "1.3.6.1.4.1.1466.115.121.1.24" );
        cb.schemaObjectProduced( this, mrule.getOid(), mrule );

        mrule = new BootstrapMatchingRule( "2.5.13.29", registries  );
        mrule.setNames( new String[] { "integerFirstComponentMatch" } );
        mrule.setSyntaxOid( "1.3.6.1.4.1.1466.115.121.1.27" );
        cb.schemaObjectProduced( this, mrule.getOid(), mrule );

        mrule = new BootstrapMatchingRule( "2.5.13.30", registries  );
        mrule.setNames( new String[] { "objectIdentifierFirstComponentMatch" } );
        mrule.setSyntaxOid( "1.3.6.1.4.1.1466.115.121.1.38" );
        cb.schemaObjectProduced( this, mrule.getOid(), mrule );

        /*
         * Straight out of RFC 3698: Section 2.6
         * http://www.faqs.org/rfcs/rfc3698.html
         * =======================================
         * ( 2.5.13.31 NAME 'directoryStringFirstComponentMatch'
         *   SYNTAX 1.3.6.1.4.1.1466.115.121.1.15 )
         */

        mrule = new BootstrapMatchingRule( "2.5.13.31", registries  );
        mrule.setNames( new String[] { "directoryStringFirstComponentMatch" } );
        mrule.setSyntaxOid( "1.3.6.1.4.1.1466.115.121.1.15" );
        cb.schemaObjectProduced( this, mrule.getOid(), mrule );

        mrule = new BootstrapMatchingRule( "1.3.6.1.4.1.1466.109.114.1", registries  );
        mrule.setNames( new String[] { "caseExactIA5Match" } );
        mrule.setSyntaxOid( "1.3.6.1.4.1.1466.115.121.1.26" );
        cb.schemaObjectProduced( this, mrule.getOid(), mrule );

        mrule = new BootstrapMatchingRule( "1.3.6.1.4.1.1466.109.114.2", registries  );
        mrule.setNames( new String[] { "caseIgnoreIA5Match" } );
        mrule.setSyntaxOid( "1.3.6.1.4.1.1466.115.121.1.26" );
        cb.schemaObjectProduced( this, mrule.getOid(), mrule );

        /*
         * MatchingRules from section 2 of http://www.faqs.org/rfcs/rfc3698.html
         * for Additional MatchingRules

         ( 2.5.13.13 NAME 'booleanMatch'
           SYNTAX 1.3.6.1.4.1.1466.115.121.1.7 )

         ( 2.5.13.18 NAME 'octetStringOrderingMatch'
           SYNTAX 1.3.6.1.4.1.1466.115.121.1.40 )

         */

        mrule = new BootstrapMatchingRule( "2.5.13.13", registries  );
        mrule.setNames( new String[] { "booleanMatch" } );
        mrule.setSyntaxOid( "1.3.6.1.4.1.1466.115.121.1.7" );
        cb.schemaObjectProduced( this, mrule.getOid(), mrule );
        
        mrule = new BootstrapMatchingRule( "2.5.13.18", registries  );
        mrule.setNames( new String[] { "octetStringOrderingMatch" } );
        mrule.setSyntaxOid( "1.3.6.1.4.1.1466.115.121.1.40" );
        cb.schemaObjectProduced( this, mrule.getOid(), mrule );
    }
}
