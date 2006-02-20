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
 * A producer of MatchingRule objects for the inetorgperson schema.  This code has been
 * automatically generated using schema files in the OpenLDAP format along with
 * the eve schema plugin for maven.  This has been done to facilitate
 * Eve<->OpenLDAP schema interoperability.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class InetorgpersonMatchingRuleProducer extends AbstractBootstrapProducer
{
    public InetorgpersonMatchingRuleProducer()
    {
        super( ProducerTypeEnum.MATCHING_RULE_PRODUCER );
    }


    // ------------------------------------------------------------------------
    // BootstrapProducer Methods
    // ------------------------------------------------------------------------

    /**
     * @see org.apache.directory.server.core.schema.bootstrap.BootstrapProducer#produce(org.apache.directory.server.core.schema.bootstrap.BootstrapRegistries, org.apache.directory.server.core.schema.bootstrap.ProducerCallback)
     */
    public void produce( BootstrapRegistries registries, ProducerCallback cb ) throws NamingException
    {
        BootstrapMatchingRule mrule = null;

        /*
         * Straight out of RFC 2798 for InetOrgPerson: Section 9.3.3
         * =========================================================

         ( 2.5.13.5 NAME 'caseExactMatch'
         SYNTAX 1.3.6.1.4.1.1466.115.121.1.15 )

         ( 2.5.13.7 NAME 'caseExactSubstringsMatch'
         SYNTAX 1.3.6.1.4.1.1466.115.121.1.58 )

         ( 2.5.13.12 NAME 'caseIgnoreListSubstringsMatch'
         SYNTAX 1.3.6.1.4.1.1466.115.121.1.58 )
         */

        mrule = new BootstrapMatchingRule( "2.5.13.5", registries );
        mrule.setNames( new String[]
            { "caseExactMatch" } );
        mrule.setSyntaxOid( "1.3.6.1.4.1.1466.115.121.1.15" );
        cb.schemaObjectProduced( this, mrule.getOid(), mrule );

        mrule = new BootstrapMatchingRule( "2.5.13.7", registries );
        mrule.setNames( new String[]
            { "caseExactSubstringsMatch" } );
        mrule.setSyntaxOid( "1.3.6.1.4.1.1466.115.121.1.58" );
        cb.schemaObjectProduced( this, mrule.getOid(), mrule );

        mrule = new BootstrapMatchingRule( "2.5.13.12", registries );
        mrule.setNames( new String[]
            { "caseIgnoreListSubstringsMatch" } );
        mrule.setSyntaxOid( "1.3.6.1.4.1.1466.115.121.1.58" );
        cb.schemaObjectProduced( this, mrule.getOid(), mrule );

        /*
         * Straight out of RFC 2798 for InetOrgPerson: Section 9.3.4
         * =========================================================

         ( 1.3.6.1.4.1.1466.109.114.3 NAME 'caseIgnoreIA5SubstringsMatch'
         SYNTAX 1.3.6.1.4.1.1466.115.121.1.58 )
         */

        mrule = new BootstrapMatchingRule( "1.3.6.1.4.1.1466.109.114.3", registries );
        mrule.setNames( new String[]
            { "caseIgnoreIA5SubstringsMatch" } );
        mrule.setSyntaxOid( "1.3.6.1.4.1.1466.115.121.1.58" );
        cb.schemaObjectProduced( this, mrule.getOid(), mrule );

    }
}
