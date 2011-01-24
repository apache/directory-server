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

import org.apache.directory.server.core.partition.Partition;
import org.apache.directory.server.core.partition.ldif.LdifPartition;
import org.apache.directory.server.xdbm.Index;
import org.apache.directory.server.xdbm.impl.avl.AvlIndex;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.name.Dn;


/**
 * A factory used to generate {@link LdifPartition}s.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class LdifPartitionFactory implements PartitionFactory
{

    /**
     * {@inheritDoc}
     */
    public LdifPartition createPartition( String id, String suffix, int cacheSize, File workingDirectory )
        throws Exception
    {
        LdifPartition partition = new LdifPartition();
        partition.setId( id );
        partition.setSuffix( new Dn( suffix ) );
        partition.setCacheSize( 500 );
        partition.setPartitionPath( workingDirectory.toURI() );
        return partition;
    }


    /**
     * {@inheritDoc}
     */
    public void addIndex( Partition partition, String attributeId, int cacheSize ) throws Exception
    {
        if ( !( partition instanceof LdifPartition ) )
        {
            throw new IllegalArgumentException( "Partition must be a LdifPartition" );
        }

        LdifPartition ldifPartition = ( LdifPartition ) partition;
        Set<Index<? extends Object, Entry, Long>> indexedAttributes = ldifPartition.getIndexedAttributes();

        AvlIndex<Object, Entry> index = new AvlIndex<Object, Entry>( attributeId );
        //index.setCacheSize( cacheSize );

        indexedAttributes.add( index );
        ldifPartition.setIndexedAttributes( indexedAttributes );
    }

}
