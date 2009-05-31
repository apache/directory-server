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
package org.apache.directory.server.core.partition.impl.btree.jdbm;


import org.apache.directory.server.schema.registries.ComparatorRegistry;
import org.apache.directory.shared.ldap.schema.parsers.ComparatorDescription;

import javax.naming.NamingException;
import java.util.Comparator;
import java.util.Iterator;


/**
 * TODO doc me!
*
* @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
* @version $$Rev$$
*/
class MockComparatorRegistry implements ComparatorRegistry
{
    private Comparator<Integer> comparator = new Comparator<Integer>()
    {
        public int compare( Integer i1, Integer i2 )
        {
            return i1.compareTo( i2 );
        }
    };

    public String getSchemaName( String oid ) throws NamingException
    {
        return null;
    }


    public void register( ComparatorDescription description, Comparator comparator ) throws NamingException
    {
    }


    public Comparator lookup( String oid ) throws NamingException
    {
        return comparator;
    }


    public boolean hasComparator( String oid )
    {
        return true;
    }


    public Iterator<String> iterator()
    {
        return null;
    }


    public Iterator<ComparatorDescription> comparatorDescriptionIterator()
    {
        return null;
    }


    public void unregister( String oid ) throws NamingException
    {
    }


    public void unregisterSchemaElements( String schemaName )
    {
    }


    public void renameSchema( String originalSchemaName, String newSchemaName )
    {
    }
}
