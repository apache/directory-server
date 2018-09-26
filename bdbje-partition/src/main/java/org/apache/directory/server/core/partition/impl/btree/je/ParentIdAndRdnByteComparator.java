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


import java.io.IOException;
import java.io.Serializable;
import java.util.Comparator;

import org.apache.directory.server.core.partition.impl.btree.je.serializers.ParentIdAndRdnSerializer;
import org.apache.directory.server.xdbm.ParentIdAndRdn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * TODO ParentIdAndRdnByteComparator.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class ParentIdAndRdnByteComparator implements Comparator<byte[]>, Serializable
{

    private static final Logger LOG = LoggerFactory.getLogger( ParentIdAndRdnByteComparator.class );

    private transient ParentIdAndRdnSerializer serializer;


    public ParentIdAndRdnByteComparator()
    {
    }


    public void setSerializer( ParentIdAndRdnSerializer serializer )
    {
        this.serializer = serializer;
    }


    public int compare( byte[] rdnBytes1, byte[] rdnBytes2 )
    {
        int val = 0;
        try
        {
            ParentIdAndRdn rdn1 = serializer.deserialize( rdnBytes1 );
            ParentIdAndRdn rdn2 = serializer.deserialize( rdnBytes2 );

            val = rdn1.compareTo( rdn2 );
        }
        catch ( IOException e )
        {
            // shound't happen
            LOG.error( "exception while comparing ParentIdAndRdn objects "
                + "after deserializing from byte arrays, something must be wrong with RDN index", e );
        }

        return val;
    }

}
