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

import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.schema.Normalizer;
import org.apache.directory.shared.ldap.schema.normalizers.DeepTrimToLowerNormalizer;
import org.apache.directory.shared.ldap.schema.normalizers.NameOrNumericIdNormalizer;
import org.apache.directory.shared.ldap.schema.normalizers.NoOpNormalizer;
import org.apache.directory.shared.ldap.schema.registries.Registries;



/**
 * A producer of Normalizer objects for the apachemeta schema.
 * Modified by hand from generated code
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class ApachemetaNormalizerProducer extends AbstractBootstrapProducer
{
    public ApachemetaNormalizerProducer()
    {
        super( ProducerTypeEnum.NORMALIZER_PRODUCER );
    }


    // ------------------------------------------------------------------------
    // BootstrapProducer Methods
    // ------------------------------------------------------------------------


    /**
     * @see BootstrapProducer#produce(Registries, ProducerCallback)
     */
    public void produce( Registries registries, ProducerCallback cb )
        throws NamingException
    {
        Normalizer normalizer = null;
        
        normalizer = new NameOrNumericIdNormalizer( registries );
        cb.schemaObjectProduced( this, SchemaConstants.NAME_OR_NUMERIC_ID_MATCH_OID, normalizer );

        normalizer = new NoOpNormalizer( SchemaConstants.OBJECT_CLASS_TYPE_MATCH_OID );
        cb.schemaObjectProduced( this, SchemaConstants.OBJECT_CLASS_TYPE_MATCH_OID, normalizer );
        
        normalizer = new NoOpNormalizer( SchemaConstants.NUMERIC_OID_MATCH_OID );
        cb.schemaObjectProduced( this, SchemaConstants.NUMERIC_OID_MATCH_OID, normalizer );
        
        normalizer = new DeepTrimToLowerNormalizer( SchemaConstants.SUP_DIT_STRUCTURE_RULE_MATCH_OID );
        cb.schemaObjectProduced( this, SchemaConstants.SUP_DIT_STRUCTURE_RULE_MATCH_OID, normalizer );
        
        normalizer = new DeepTrimToLowerNormalizer( SchemaConstants.RULE_ID_MATCH_OID );
        cb.schemaObjectProduced( this, SchemaConstants.RULE_ID_MATCH_OID, normalizer );
        
        // For entryUuid
        normalizer = new NoOpNormalizer( SchemaConstants.ENTRY_UUID_AT_OID );
        cb.schemaObjectProduced( this, SchemaConstants.ENTRY_UUID_AT_OID, normalizer );
        
        // For entryCSN
        normalizer = new NoOpNormalizer( SchemaConstants.ENTRY_CSN_AT_OID );
        cb.schemaObjectProduced( this, SchemaConstants.ENTRY_CSN_AT_OID, normalizer );
    }
}
