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

package org.apache.directory.server.core.partition.ldif;


import java.net.URI;

import org.apache.directory.server.core.api.partition.Partition;
import org.apache.directory.server.core.partition.impl.avl.AvlPartition;
import org.apache.directory.shared.ldap.model.csn.CsnFactory;
import org.apache.directory.shared.ldap.model.schema.SchemaManager;


/**
 * A common base class for LDIF file based Partition implementations.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public abstract class AbstractLdifPartition extends AvlPartition
{
    /** The extension used for LDIF entry files */
    protected static final String CONF_FILE_EXTN = ".ldif";

    /** A default CSN factory */
    protected static CsnFactory defaultCSNFactory;


    public AbstractLdifPartition( SchemaManager schemaManager )
    {
        super( schemaManager );

        // Create the CsnFactory with a invalid ReplicaId
        // @TODO : inject a correct ReplicaId
        defaultCSNFactory = new CsnFactory( 0 );
    }


    /**
     * {@inheritDoc}
     */
    public String getDefaultId()
    {
        return Partition.DEFAULT_ID;
    }


    /**
     * {@inheritDoc}
     */
    public URI getPartitionPath()
    {
        return partitionPath;
    }
}
