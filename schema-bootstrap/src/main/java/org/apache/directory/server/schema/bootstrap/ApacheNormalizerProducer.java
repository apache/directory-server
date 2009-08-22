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
package org.apache.directory.server.schema.bootstrap;


import javax.naming.NamingException;

import org.apache.directory.server.schema.bootstrap.ProducerTypeEnum;
import org.apache.directory.server.schema.registries.Registries;
import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.schema.Normalizer;
import org.apache.directory.shared.ldap.schema.normalizers.NoOpNormalizer;


/**
 * A producer of Normalizer objects for the eve schema.
 * Probably modified by hand from generated code
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class ApacheNormalizerProducer extends AbstractBootstrapProducer
{
    public ApacheNormalizerProducer()
    {
        super( ProducerTypeEnum.NORMALIZER_PRODUCER );
    }


    // ------------------------------------------------------------------------
    // BootstrapProducer Methods
    // ------------------------------------------------------------------------

    /**
     * @see org.apache.directory.server.schema.bootstrap.BootstrapProducer#produce(org.apache.directory.server.schema.registries.Registries, org.apache.directory.server.schema.bootstrap.ProducerCallback)
     */
    public void produce( Registries registries, ProducerCallback cb ) throws NamingException
    {
        Normalizer normalizer;

        // For exactDnAsStringMatch -> 1.3.6.1.4.1.18060.0.4.1.1.1
        normalizer = new NoOpNormalizer( SchemaConstants.EXACT_DN_AS_STRING_MATCH_MR_OID );
        cb.schemaObjectProduced( this, SchemaConstants.EXACT_DN_AS_STRING_MATCH_MR_OID, normalizer );

        // For bigIntegerMatch -> 1.3.6.1.4.1.18060.0.4.1.1.2
        normalizer = new NoOpNormalizer( SchemaConstants.BIG_INTEGER_MATCH_MR_OID );
        cb.schemaObjectProduced( this, SchemaConstants.BIG_INTEGER_MATCH_MR_OID, normalizer );

        // For jdbmStringMatch -> 1.3.6.1.4.1.18060.0.4.1.1.3
        normalizer = new NoOpNormalizer( SchemaConstants.JDBM_STRING_MATCH_MR_OID );
        cb.schemaObjectProduced( this, SchemaConstants.JDBM_STRING_MATCH_MR_OID, normalizer );

        // For uuidMatch -> 1.3.6.1.1.16.2
        normalizer = new NoOpNormalizer( SchemaConstants.UUID_MATCH_MR_OID );
        cb.schemaObjectProduced( this, SchemaConstants.UUID_MATCH_MR_OID, normalizer );

        // For uuidOrderingMatch -> 1.3.6.1.1.16.3
        normalizer = new NoOpNormalizer( SchemaConstants.UUID_ORDERING_MATCH_MR_OID );
        cb.schemaObjectProduced( this, SchemaConstants.UUID_ORDERING_MATCH_MR_OID, normalizer );

        // For CSNMatch -> 1.3.6.1.4.1.4203.666.11.2.2
        normalizer = new NoOpNormalizer( SchemaConstants.CSN_MATCH_MR_OID );
        cb.schemaObjectProduced( this, SchemaConstants.CSN_MATCH_MR_OID, normalizer );

        // For CSNOrderingMatch -> 1.3.6.1.4.1.4203.666.11.2.3
        normalizer = new NoOpNormalizer( SchemaConstants.CSN_ORDERING_MATCH_MR_OID );
        cb.schemaObjectProduced( this, SchemaConstants.CSN_ORDERING_MATCH_MR_OID, normalizer );

        // For CSNSidMatch -> 1.3.6.1.4.1.4203.666.11.2.5
        normalizer = new NoOpNormalizer( SchemaConstants.CSN_SID_MATCH_MR_OID );
        cb.schemaObjectProduced( this, SchemaConstants.CSN_SID_MATCH_MR_OID, normalizer );
    }
}
