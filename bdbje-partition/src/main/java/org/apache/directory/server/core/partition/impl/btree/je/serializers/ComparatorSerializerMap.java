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

package org.apache.directory.server.core.partition.impl.btree.je.serializers;


import java.util.HashMap;
import java.util.Map;

import org.apache.directory.api.ldap.model.schema.LdapComparator;
import org.apache.directory.api.ldap.model.schema.comparators.IntegerComparator;
import org.apache.directory.api.ldap.model.schema.comparators.StringComparator;
import org.apache.directory.server.core.partition.impl.btree.LongComparator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A class holding the mappings of comparator to the respective serializer.
 * If no mapping fornd for a comparator then the default serializer will be returned
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public final class ComparatorSerializerMap
{
    private static final Logger LOG = LoggerFactory.getLogger( ComparatorSerializerMap.class );

    private static final Map<Class<? extends LdapComparator<?>>, Serializer> MAP = new HashMap<Class<? extends LdapComparator<?>>, Serializer>();

    static
    {
        MAP.put( LongComparator.class, new LongSerializer() );
        MAP.put( StringComparator.class, new StringSerializer() );
        MAP.put( IntegerComparator.class, new LongSerializer() );
    }

    private ComparatorSerializerMap()
    {
    }

    public static Serializer getSerializer( Class compClass )
    {
        Serializer sz = MAP.get( compClass );

        if ( sz == null )
        {
            LOG.info( "there is no configured serializer for the class " + compClass.getName() );
            LOG.info( "returning the object serializer instead" );

            sz = ObjectSerializer.INSTANCE;
        }

        return sz;
    }
}
