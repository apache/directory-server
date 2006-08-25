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

package org.apache.directory.shared.ldap.trigger;

import javax.naming.Name;


/**
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev:$, $Date:$
 */
public class StoredProcedureSearchContextOption implements StoredProcedureOption
{
    
    private final Name baseObject;
    private SearchScope searchScope;

    
    public StoredProcedureSearchContextOption( Name baseObject )
    {
        // the default search scope is "base"
        this( baseObject, SearchScope.BASE );
    }
    
    public StoredProcedureSearchContextOption( Name baseObject, SearchScope searchScope )
    {
        this.baseObject = baseObject;
        this.searchScope = searchScope;
    }

    public Name getBaseObject()
    {
        return baseObject;
    }
    
    public SearchScope getSearchScope()
    {
        return searchScope;
    }

    public String toString()
    {
        return "searchContext { scope " + searchScope + " } \"" + baseObject + "\""; 
    }

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    public int hashCode()
    {
        final int PRIME = 31;
        int result = 1;
        result = PRIME * result + ( ( baseObject == null ) ? 0 : baseObject.hashCode() );
        result = PRIME * result + ( ( searchScope == null ) ? 0 : searchScope.hashCode() );
        return result;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals( Object obj )
    {
        if ( this == obj )
            return true;
        if ( obj == null )
            return false;
        if ( getClass() != obj.getClass() )
            return false;
        final StoredProcedureSearchContextOption other = ( StoredProcedureSearchContextOption ) obj;
        if ( baseObject == null )
        {
            if ( other.baseObject != null )
                return false;
        }
        else if ( !baseObject.equals( other.baseObject ) )
            return false;
        if ( searchScope == null )
        {
            if ( other.searchScope != null )
                return false;
        }
        else if ( !searchScope.equals( other.searchScope ) )
            return false;
        return true;
    }
    
}
