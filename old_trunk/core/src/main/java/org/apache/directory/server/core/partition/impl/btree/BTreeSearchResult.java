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
package org.apache.directory.server.core.partition.impl.btree;


import javax.naming.InvalidNameException;

import org.apache.directory.server.core.entry.ServerEntry;
import org.apache.directory.server.core.entry.ServerSearchResult;
import org.apache.directory.shared.ldap.name.LdapDN;


/**
 * A special search result that includes the unique database primary key or 
 * 'row id' of the entry in the master table for quick lookup.  This speeds 
 * up various operations.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class BTreeSearchResult extends ServerSearchResult
{
    private static final long serialVersionUID = 3976739172700860977L;

    /** the primary key used for the resultant entry */
    private final Long id;


    // ------------------------------------------------------------------------
    // C O N S T R U C T O R S
    // ------------------------------------------------------------------------

    /**
     * Creates a database search result.
     * 
     * @param id the database id of the entry
     * @param dn the user provided relative or distinguished name
     * @param obj the object if any
     * @param attrs the entry
     */
    public BTreeSearchResult( Long id, LdapDN dn, Object obj, ServerEntry attrs ) throws InvalidNameException
    {
        super( dn, obj, attrs );
        this.id = id;
    }


    /**
     * Creates a database search result.
     * 
     * @param id the database id of the entry
     * @param dn the user provided relative or distinguished name
     * @param obj the object if any
     * @param attrs the entry
     * @param isRelative whether or not the name is relative to the base
     */
    public BTreeSearchResult( Long id, LdapDN dn, Object obj, ServerEntry attrs, boolean isRelative ) throws InvalidNameException
    {
        super( dn, obj, attrs, isRelative );
        this.id = id;
    }


    /**
     * Creates a database search result.
     * 
     * @param id the database id of the entry
     * @param dn the user provided relative or distinguished name
     * @param className the classname of the entry if any
     * @param obj the object if any
     * @param attrs the entry
     */
    public BTreeSearchResult( Long id, LdapDN dn, String className, Object obj, ServerEntry attrs ) throws InvalidNameException
    {
        super( dn, className, obj, attrs );
        this.id = id;
    }


    /**
     * Creates a database search result.
     * 
     * @param id the database id of the entry
     * @param dn the user provided relative or distinguished name
     * @param className the classname of the entry if any
     * @param obj the object if any
     * @param attrs the entry
     * @param isRelative whether or not the name is relative to the base
     */
    public BTreeSearchResult( Long id, LdapDN dn, String className, Object obj, ServerEntry attrs,
        boolean isRelative ) throws InvalidNameException
    {
        super( dn, className, obj, attrs, isRelative );
        this.id = id;
    }


    /**
     * Gets the unique row id of the entry into the master table.
     * 
     * @return Returns the id.
     */
    public Long getId()
    {
        return id;
    }
}
