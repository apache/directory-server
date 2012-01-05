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
package org.apache.directory.server.core.api.partition.index;


import java.util.UUID;

import org.apache.directory.shared.ldap.model.constants.SchemaConstants;
import org.apache.directory.shared.ldap.model.schema.comparators.SerializableComparator;


public class UUIDComparator extends SerializableComparator<UUID>
{
    /** Cached instance */
    public static final UUIDComparator INSTANCE = new UUIDComparator();

    /** The serial version UID */
    private static final long serialVersionUID = 2L;


    public UUIDComparator()
    {
        super( SchemaConstants.UUID_ORDERING_MATCH_MR_OID );
    }


    public int compare( UUID uuid1, UUID uuid2 )
    {
        if ( uuid1 == null )
        {
            return ( uuid2 == null ) ? 0 : -1;
        }

        if ( uuid2 == null )
        {
            return 1;
        }

        return uuid1.compareTo( uuid2 );
    }
}
