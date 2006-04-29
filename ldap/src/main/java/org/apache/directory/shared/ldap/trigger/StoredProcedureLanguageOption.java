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


/**
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev:$, $Date:$
 */
public class StoredProcedureLanguageOption implements StoredProcedureOption
{
    
    private String language;
    
    public StoredProcedureLanguageOption( String language )
    {
        this.language = language;
    }
    
    public String getLanguage()
    {
        return language;
    }

    public boolean equals( Object obj )
    {
        if ( obj == this )
        {
            return true;
        }
        
        if ( obj instanceof StoredProcedureLanguageOption )
        {
            StoredProcedureLanguageOption splo = ( StoredProcedureLanguageOption ) obj; 
            if ( splo.getLanguage().equals( this.getLanguage() ) )
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
