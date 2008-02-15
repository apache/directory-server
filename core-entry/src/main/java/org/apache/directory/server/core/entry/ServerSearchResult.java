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


import javax.naming.InvalidNameException;

import org.apache.directory.shared.ldap.name.LdapDN;

/**
 * Creates a wrapper around a SearchResult object so that we can use the LdapDN
 * instead of parser it over and over
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class ServerSearchResult // extends SearchResult
{
    /** Unique serial UID */
    public static final long serialVersionUID = 1L;

    /** Distinguished name for this result */
    private LdapDN dn;
    
    /** The associated entry */
    private ServerEntry serverEntry;
    
    /** Tells if the name is relative to the target context */
    private boolean isRelative;
    
    /** The bound object */
    private Object object;
    
    /**
     * @see javax.naming.directory.SearchResult#SearchResult(String, Object, ServerEntry)
     */
    public ServerSearchResult( String name, Object obj, ServerEntry serverEntry ) throws InvalidNameException
    {
        dn = new LdapDN( name );
        this.serverEntry = serverEntry;
        this.serverEntry.setDn( dn );
    }


    /**
     * @see javax.naming.directory.SearchResult#SearchResult(String, Object, ServerEntry)
     */
    public ServerSearchResult( String name, Object obj, ServerEntry serverEntry, boolean isRelative ) throws InvalidNameException
    {
        dn = new LdapDN( name );
        this.serverEntry = serverEntry;
        this.serverEntry.setDn( dn );
        this.isRelative = isRelative;
    }


    /**
     * @see javax.naming.directory.SearchResult#SearchResult(String, Object, ServerEntry)
     */
    public ServerSearchResult( String name, String className, Object obj, ServerEntry serverEntry ) throws InvalidNameException
    {
        dn = new LdapDN( name );
        this.serverEntry = serverEntry;
        this.serverEntry.setDn( dn );
    }


    /**
     * @see javax.naming.directory.SearchResult#SearchResult(String, String, Object, ServerEntry, boolean)
     */
    public ServerSearchResult( String name, String className, Object obj, ServerEntry serverEntry, boolean isRelative ) throws InvalidNameException
    {
        dn = new LdapDN( name );
        this.serverEntry = serverEntry;
        this.serverEntry.setDn( dn );
    }


    /**
     * @return The result DN
     */
    public LdapDN getDn()
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
		String name = (dn == null ? "null" : ( dn == LdapDN.EMPTY_LDAPDN ? "\"\"" : dn.getUpName() ) );
		return "ServerSearchResult : " + name + "\n" + serverEntry;
	}
}
