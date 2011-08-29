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
package org.apache.directory.server.core.authz.support;


import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.directory.shared.ldap.model.exception.LdapException;
import org.apache.directory.shared.ldap.model.schema.registries.OidRegistry;
import org.bouncycastle.util.Strings;


/**
 * A mock {@link OidRegistry} to test {@link ACITupleFilter} implementations.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 *
 */
class DummyOidRegistry extends OidRegistry
{
    public String getOid( String name ) throws LdapException
    {
        return Strings.toLowerCase( name );
    }


    public boolean hasOid( String id )
    {
        return true;
    }


    public String getPrimaryName( String oid ) throws LdapException
    {
        return oid;
    }


    public List<String> getNameSet( String oid ) throws LdapException
    {
        List<String> list = new ArrayList<String>();
        list.add( oid );
        return list;
    }


    public Iterator list()
    {
        // Not used
        return new ArrayList().iterator();
    }


    public void register( String name, String oid )
    {
        // Not used
    }


    /**
     * Get the map of all the oids by their name
     * @return The Map that contains all the oids
     */
    public Map getOidByName()
    {
        return null;
    }


    /**
     * Get the map of all the oids by their name
     * @return The Map that contains all the oids
     */
    public Map getNameByOid()
    {
        return null;
    }


    public void unregister( String numericOid ) throws LdapException
    {
    }
}