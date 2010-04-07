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

package org.apache.directory.server.core.partition.impl.btree.jdbm;


import org.apache.directory.shared.ldap.name.RDN;
import org.apache.directory.shared.ldap.schema.SchemaManager;
import org.apache.directory.shared.ldap.schema.comparators.SerializableComparator;


/**
 * A comparator used internally by the JdbmRdnIndex class
 * 
 * Note: this is a special purpose comparator which compares based on the parent IDs of
 *       the RDNs. Generic usage of this comparator is not encouraged
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class InternalRdnComparator extends SerializableComparator<RDN>
{

    private static final long serialVersionUID = 5414960421568991202L;


    public InternalRdnComparator( String matchingRuleOid )
    {
        super( matchingRuleOid );
    }


    @Override
    public int compare( RDN rdn1, RDN rdn2 )
    {
        int val = rdn1.compareTo( rdn2 );
        if ( val == 0 )
        {
            if ( ( rdn1._getParentId() != -1 ) && ( rdn2._getParentId() != -1 ) )
            {
                val = ( int ) ( rdn1._getParentId() - rdn2._getParentId() );
            }
        }

        return val;
    }


    @Override
    public void setSchemaManager( SchemaManager schemaManager )
    {
        // no need to deal with the schema manager
    }

}