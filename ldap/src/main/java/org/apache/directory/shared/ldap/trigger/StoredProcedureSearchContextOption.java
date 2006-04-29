/*
 *   Copyright 2006 The Apache Software Foundation
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
        return baseObject.toString();
    }

    public boolean equals( Object obj )
    {
        if ( obj == this )
        {
            return true;
        }
        
        if ( obj instanceof StoredProcedureSearchContextOption )
        {
            StoredProcedureSearchContextOption stsco = ( StoredProcedureSearchContextOption ) obj;
            if ( stsco.getBaseObject().equals( this.getBaseObject() ) && stsco.getSearchScope().equals( this.getSearchScope() ) )
            {
                return true;
            }
            else
            {
                return false;
            }
        }
        else
        {
            return false;
        }
    }
    
}
