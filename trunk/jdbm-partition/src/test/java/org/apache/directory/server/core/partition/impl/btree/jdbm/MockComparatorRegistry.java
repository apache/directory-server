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


import java.util.Iterator;

import org.apache.directory.shared.ldap.model.exception.LdapException;
import org.apache.directory.shared.ldap.model.schema.LdapComparator;
import org.apache.directory.shared.ldap.model.schema.parsers.LdapComparatorDescription;
import org.apache.directory.shared.ldap.model.schema.registries.DefaultComparatorRegistry;


/**
 * TODO doc me!
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
class MockComparatorRegistry extends DefaultComparatorRegistry
{
    public MockComparatorRegistry()
    {
        super();
    }

    private LdapComparator<Integer> comparator = new LdapComparator<Integer>( "1.1.1" )
    {
        public int compare( Integer i1, Integer i2 )
        {
            return i1.compareTo( i2 );
        }
    };


    public String getSchemaName( String oid ) throws LdapException
    {
        return null;
    }


    public void register( LdapComparatorDescription description, LdapComparator<?> comparator ) throws LdapException
    {
    }


    public LdapComparator<?> lookup( String oid ) throws LdapException
    {
        return comparator;
    }


    public void register( LdapComparator<?> comparator ) throws LdapException
    {
    }


    public boolean contains( String oid )
    {
        return true;
    }


    public Iterator<LdapComparator<?>> iterator()
    {
        return null;
    }


    public Iterator<String> oidsIterator()
    {
        return null;
    }


    public Iterator<LdapComparatorDescription> ldapComparatorDescriptionIterator()
    {
        return null;
    }


    public LdapComparator<Integer> unregister( String oid ) throws LdapException
    {
        return this.comparator;
    }


    public void unregisterSchemaElements( String schemaName )
    {
    }


    public void renameSchema( String originalSchemaName, String newSchemaName )
    {
    }
}
