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
package org.apache.directory.server.core.entry;



import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.name.Dn;

/**
 * Creates a wrapper around a SearchResult object so that we can use the Dn
 * instead of parser it over and over
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class ServerSearchResult
{
    /** Distinguished name for this result */
    private Dn dn;
    
    /** The associated entry */
    private Entry serverEntry;
    
    /** Tells if the name is relative to the target context */
    private boolean isRelative;
    
    /** The bound object */
    private Object object;
    

    /**
     * 
     * Creates a new instance of ServerSearchResult.
     *
     * @param dn Distinguished name for this result
     * @param serverEntry The associated entry 
     * @param isRelative Tells if the name is relative to the target context
     */
    public ServerSearchResult( Dn dn, Entry serverEntry, boolean isRelative )
    {
        this.dn = dn;
        this.serverEntry = serverEntry;
        this.serverEntry.setDn( dn );
        this.isRelative = isRelative;
    }


    /**
     * 
     * Creates a new instance of ServerSearchResult.
     *
     * @param dn Distinguished name for this result
     * @param serverEntry The associated entry
     */
    public ServerSearchResult( Dn dn, Entry serverEntry )
    {
        this.dn = dn;
        this.serverEntry = serverEntry;
        this.serverEntry.setDn( dn );
    }


    /**
     * @return The result Dn
     */
    public Dn getDn()
    {
        return dn;
    }
    
    
    /**
     * @return The entry
     */
    public Entry getServerEntry()
    {
        return serverEntry;
    }


    public boolean isRelative() 
    {
        return isRelative;
    }


    public void setRelative( boolean isRelative ) 
    {
        this.isRelative = isRelative;
    }


    public void setServerEntry( Entry serverEntry ) 
    {
        this.serverEntry = serverEntry;
    }


    public Object getObject() 
    {
        return object;
    }


    public void setObject( Object object ) 
    {
        this.object = object;
    }
    
    
    /**
     * @see Object#toString()
     */
    public String toString()
    {
        String name = (dn == null ? "null" : ( dn == Dn.EMPTY_DN ? "\"\"" : dn.getName() ) );
        return "ServerSearchResult : " + name + "\n" + serverEntry;
    }
}
