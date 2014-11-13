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
package org.apache.directory.server.core.partition.impl.btree.mavibot;

import java.util.Comparator;

import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.mavibot.btree.Tuple;

/**
 * TODO LdifTupleComparator.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class LdifTupleComparator implements Comparator<Tuple<Dn, String>>
{

    @Override
    public int compare( Tuple<Dn, String> t1, Tuple<Dn, String> t2 )
    {
        Dn dn1 = t1.getKey();
        Dn dn2 = t2.getKey();
        
        if( dn1.isAncestorOf( dn2 ) )
        {
            return -1;
        }
        else if( dn2.isAncestorOf( dn1 ) )
        {
            return 1;
        }
        else if ( dn1.equals( dn2 ) )
        {
            return 0;
        }
        
        return dn1.getNormName().compareTo( dn2.getNormName() );
    }

}
