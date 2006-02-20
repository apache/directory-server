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
 * A producer of MatchingRule objects for the eve schema. 
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class ApacheMatchingRuleProducer extends AbstractBootstrapProducer
{
    public ApacheMatchingRuleProducer()
    {
        super( ProducerTypeEnum.MATCHING_RULE_PRODUCER );
    }


    // ------------------------------------------------------------------------
    // BootstrapProducer Methods
    // ------------------------------------------------------------------------

    /**
     * @see BootstrapProducer#produce(BootstrapRegistries, org.apache.directory.server.core.schema.bootstrap.ProducerCallback)
     */
    public void produce( BootstrapRegistries registries, ProducerCallback cb ) throws NamingException
    {
        BootstrapMatchingRule mrule = null;

        mrule = new BootstrapMatchingRule( "1.2.6.1.4.1.18060.1.1.1.2.1", registries );
        mrule.setNames( new String[]
            { "exactDnAsStringMatch" } );
        mrule.setSyntaxOid( "1.3.6.1.4.1.1466.115.121.1.12" );
        cb.schemaObjectProduced( this, mrule.getOid(), mrule );

        mrule = new BootstrapMatchingRule( "1.2.6.1.4.1.18060.1.1.1.2.2", registries );
        mrule.setNames( new String[]
            { "bigIntegerMatch" } );
        mrule.setSyntaxOid( "1.3.6.1.4.1.1466.115.121.1.27" );
        cb.schemaObjectProduced( this, mrule.getOid(), mrule );

        mrule = new BootstrapMatchingRule( "1.2.6.1.4.1.18060.1.1.1.2.3", registries );
        mrule.setNames( new String[]
            { "jdbmStringMatch" } );
        mrule.setSyntaxOid( "1.3.6.1.4.1.1466.115.121.1.15" );
        cb.schemaObjectProduced( this, mrule.getOid(), mrule );
    }
}
