/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.directory.server.core.factory;


import java.io.File;
import java.util.Set;

import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.apache.directory.server.core.api.DnFactory;
import org.apache.directory.server.core.api.partition.Partition;
import org.apache.directory.server.core.partition.impl.btree.je.BdbJeIndex;
import org.apache.directory.server.core.partition.impl.btree.je.BdbJePartition;
import org.apache.directory.server.xdbm.Index;


/**
 * A factory used to generate {@link BdbJePartition}s.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class BdbJePartitionFactory implements PartitionFactory
{

    /**
     * {@inheritDoc}
     */
    public BdbJePartition createPartition( SchemaManager schemaManager, DnFactory dnFactory, String id, String suffix,
        int cacheSize,
        File workingDirectory )
        throws Exception
    {
        BdbJePartition partition = new BdbJePartition( schemaManager, dnFactory );
        partition.setId( id );
        partition.setSuffixDn( new Dn( suffix ) );
        partition.setPartitionPath( workingDirectory.toURI() );

        return partition;
    }


    /**
     * {@inheritDoc}
     */
    public void addIndex( Partition partition, String attributeId, int cacheSize ) throws Exception
    {
        if ( !( partition instanceof BdbJePartition ) )
        {
            throw new IllegalArgumentException( "Partition must be a BdbJePartition" );
        }

        BdbJePartition bdbJePartition = ( BdbJePartition ) partition;

        Set<Index<?, String>> indexedAttributes = bdbJePartition.getIndexedAttributes();

        BdbJeIndex<?> idx = new BdbJeIndex<>( attributeId, false );
        indexedAttributes.add( idx );
        bdbJePartition.setIndexedAttributes( indexedAttributes );
    }

}
