/*
 *   Copyright 2004 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package org.apache.ldap.server.partition.impl.btree;


import java.math.BigInteger;

import javax.naming.directory.Attributes;
import javax.naming.directory.SearchResult;


/**
 * A special search result that includes the unique database primary key or 
 * 'row id' of the entry in the master table for quick lookup.  This speeds 
 * up various operations.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class BTreeSearchResult extends SearchResult
{
    private static final long serialVersionUID = 3976739172700860977L;

    /** the primary key used for the resultant entry */
    private final BigInteger id;
    
    
    // ------------------------------------------------------------------------
    // C O N S T R U C T O R S
    // ------------------------------------------------------------------------
    
    
    /**
     * Creates a database search result.
     * 
     * @param id the database id of the entry
     * @param name the user provided relative or distinguished name
     * @param obj the object if any
     * @param attrs the attributes of the entry
     */
    public BTreeSearchResult( BigInteger id, String name, Object obj,
        Attributes attrs )
    {
        super( name, obj, attrs );
        this.id = id;
    }

    
    /**
     * Creates a database search result.
     * 
     * @param id the database id of the entry
     * @param name the user provided relative or distinguished name
     * @param obj the object if any
     * @param attrs the attributes of the entry
     * @param isRelative whether or not the name is relative to the base
     */
    public BTreeSearchResult( BigInteger id, String name, Object obj,
        Attributes attrs, boolean isRelative )
    {
        super( name, obj, attrs, isRelative );
        this.id = id;
    }

    
    /**
     * Creates a database search result.
     * 
     * @param id the database id of the entry
     * @param name the user provided relative or distinguished name
     * @param className the classname of the entry if any
     * @param obj the object if any
     * @param attrs the attributes of the entry
     */
    public BTreeSearchResult( BigInteger id, String name, String className,
        Object obj, Attributes attrs )
    {
        super( name, className, obj, attrs );
        this.id = id;
    }

    
    /**
     * Creates a database search result.
     * 
     * @param id the database id of the entry
     * @param name the user provided relative or distinguished name
     * @param className the classname of the entry if any
     * @param obj the object if any
     * @param attrs the attributes of the entry
     * @param isRelative whether or not the name is relative to the base
     */
    public BTreeSearchResult( BigInteger id, String name, String className,
        Object obj, Attributes attrs, boolean isRelative )
    {
        super( name, className, obj, attrs, isRelative );
        this.id = id;
    }
    
    
    /**
     * Gets the unique row id of the entry into the master table.
     * 
     * @return Returns the id.
     */
    public BigInteger getId()
    {
        return id;
    }
}
