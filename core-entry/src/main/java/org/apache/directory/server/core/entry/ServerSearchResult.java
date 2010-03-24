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



import org.apache.directory.shared.ldap.entry.ServerEntry;
import org.apache.directory.shared.ldap.name.DN;

/**
 * Creates a wrapper around a SearchResult object so that we can use the DN
 * instead of parser it over and over
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class ServerSearchResult
{
    /** Distinguished name for this result */
    private DN dn;
    
    /** The associated entry */
    private ServerEntry serverEntry;
    
    /** Tells if the name is relative to the target context */
    private boolean isRelative;
    
    /** The bound object */
    private Object object;
    

    public ServerSearchResult( DN dn, Object obj, ServerEntry serverEntry )
    {
        this.dn = dn;
        this.serverEntry = serverEntry;
        this.serverEntry.setDn( dn );
    }


    public ServerSearchResult( DN dn, Object obj, ServerEntry serverEntry, boolean isRelative )
    {
        this.dn = dn;
        this.serverEntry = serverEntry;
        this.serverEntry.setDn( dn );
        this.isRelative = isRelative;
    }


    public ServerSearchResult( DN dn, String className, Object obj, ServerEntry serverEntry )
    {
        this.dn = dn;
        this.serverEntry = serverEntry;
        this.serverEntry.setDn( dn );
    }


    public ServerSearchResult( DN dn, String className, Object obj, ServerEntry serverEntry, boolean isRelative ) 
    {
        this.dn = dn;
        this.serverEntry = serverEntry;
        this.serverEntry.setDn( dn );
    }


    /**
     * @return The result DN
     */
    public DN getDn()
    {
        return dn;
    }
    
    
    /**
     * @return The entry
     */
    public ServerEntry getServerEntry()
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


    public void setServerEntry( ServerEntry serverEntry ) 
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
        String name = (dn == null ? "null" : ( dn == DN.EMPTY_DN ? "\"\"" : dn.getName() ) );
        return "ServerSearchResult : " + name + "\n" + serverEntry;
    }
}
