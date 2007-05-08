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
package org.apache.directory.shared.ldap.message;


import javax.naming.InvalidNameException;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchResult;

import org.apache.directory.shared.ldap.name.LdapDN;

/**
 * Creates a wrapper around a SearchResult object so that we can use the LdapDN
 * instead of parser it over and over
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class ServerSearchResult extends SearchResult
{
    /** Unique serial UID */
    public static final long serialVersionUID = 1L;

    /** Distinguished name for this result */
    private LdapDN dn;
    
    /**
     * @see javax.naming.directory.SearchResult#SearchResult(String, Object, Attributes)
     */
    public ServerSearchResult( String name, Object obj, Attributes attrs ) throws InvalidNameException
    {
        super( name, obj, attrs );
        dn = new LdapDN( name );
    }


    /**
     * @see javax.naming.directory.SearchResult#SearchResult(String, Object, Attributes)
     */
    public ServerSearchResult( String name, Object obj, Attributes attrs, boolean isRelative ) throws InvalidNameException
    {
        super( name, obj, attrs, isRelative );
        dn = new LdapDN( name );
    }


    /**
     * @see javax.naming.directory.SearchResult#SearchResult(String, Object, Attributes)
     */
    public ServerSearchResult( String name, String className, Object obj, Attributes attrs ) throws InvalidNameException
    {
        super( name, className, obj, attrs );
        dn = new LdapDN( name );
    }


    /**
     * @see javax.naming.directory.SearchResult#SearchResult(String, String, Object, Attributes, boolean)
     */
    public ServerSearchResult( String name, String className, Object obj, Attributes attrs, boolean isRelative ) throws InvalidNameException
    {
        super( name, className, obj, attrs, isRelative );
        dn = new LdapDN( name );
    }


    /**
     * @return The result DN
     */
    public LdapDN getDn()
    {
        return dn;
    }
}
