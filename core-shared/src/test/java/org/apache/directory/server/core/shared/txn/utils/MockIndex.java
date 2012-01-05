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
package org.apache.directory.server.core.shared.txn.utils;


import java.util.UUID;

import org.apache.directory.server.core.api.partition.index.ForwardIndexComparator;
import org.apache.directory.server.core.api.partition.index.GenericIndex;
import org.apache.directory.server.core.api.partition.index.ReverseIndexComparator;
import org.apache.directory.shared.ldap.model.schema.comparators.LongComparator;


/**
 * A Mock index
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class MockIndex extends GenericIndex<Long>
{
    public MockIndex( String attributeOid )
    {
        super( attributeOid );
    }


    public ForwardIndexComparator<Long> getForwardIndexEntryComparator()
    {
        return new ForwardIndexComparator<Long>( new LongComparator( attributeId ) );
    }


    public ReverseIndexComparator<Long> getReverseIndexEntryComparator()
    {
        return new ReverseIndexComparator<Long>( new LongComparator( attributeId ) );
    }


    public void add( Long attrVal, UUID id ) throws Exception
    {
        // Do nothing
    }


    public void drop( Long attrVal, UUID id ) throws Exception
    {
        // Do nothing
    }
}
