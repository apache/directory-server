/*
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 *
 */

package org.apache.directory.server.core.partition.impl.btree.je;


import org.apache.directory.api.ldap.model.schema.MatchingRule;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.apache.directory.api.ldap.model.schema.comparators.UuidComparator;
import org.apache.directory.server.constants.ApacheSchemaConstants;
import org.apache.directory.server.xdbm.ParentIdAndRdn;
import org.apache.directory.server.xdbm.ParentIdAndRdnComparator;
import org.apache.directory.server.core.partition.impl.btree.je.serializers.ParentIdAndRdnSerializer;
import org.apache.directory.server.core.partition.impl.btree.je.serializers.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sleepycat.je.Database;


/**
 * RDN index implementation based on BdbJeIndex.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class BdbJeRdnIndex<E> extends BdbJeIndex<ParentIdAndRdn>
{

    /** A logger for this class */
    private static final Logger LOG = LoggerFactory.getLogger( BdbJeRdnIndex.class );


    public BdbJeRdnIndex()
    {
        super( ApacheSchemaConstants.APACHE_RDN_AT_OID, true );
        initialized = false;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void init( SchemaManager schemaManager, BdbJePartitionEnviroment environment )
    {
        LOG.debug( "Initializing RDN Index for attribute '{}'", attributeId );

        attributeType = schemaManager.getAttributeType( attributeId );
        attribute = attributeType;
        MatchingRule mr = attributeType.getEquality();

        ParentIdAndRdnComparator<String> comp = new ParentIdAndRdnComparator<>( mr.getOid() );

        UuidComparator.INSTANCE.setSchemaManager( schemaManager );

        ParentIdAndRdnSerializer parentIdAndSerializer = new ParentIdAndRdnSerializer( schemaManager );

        // it is mandatory to set the BTree key comparator for RDN index due to the way ParentIdAndRdn is serialized
        // and compared. This incurs a significant penalty here because we are unable to use JE's byte-by-byte comparison
        // that we use for all other indices.
        Database rdnForwardDb = environment.createDb( ApacheSchemaConstants.APACHE_RDN_AT_OID + FORWARD_KEY,
            new ParentIdAndRdnByteComparator() );
        // it is import to set the serializer because it is a volatile field
        ( ( ParentIdAndRdnByteComparator ) rdnForwardDb.getConfig().getBtreeComparator() )
            .setSerializer( parentIdAndSerializer );

        forward = new BdbJeTable<ParentIdAndRdn, String>( rdnForwardDb, schemaManager, comp, parentIdAndSerializer,
            new StringSerializer() );

        Database rdnReverseDb = environment.createDb( ApacheSchemaConstants.APACHE_RDN_AT_OID + REVERSE_KEY );
        reverse = new BdbJeTable<String, ParentIdAndRdn>( rdnReverseDb, schemaManager, UuidComparator.INSTANCE,
            new StringSerializer(), parentIdAndSerializer );
        initialized = true;
    }

}
