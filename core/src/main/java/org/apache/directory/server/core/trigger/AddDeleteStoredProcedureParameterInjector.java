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

package org.apache.directory.server.core.trigger;

import java.util.Map;

import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;

import org.apache.directory.shared.ldap.trigger.StoredProcedureParameter.AddStoredProcedureParameter;

public class AddDeleteStoredProcedureParameterInjector extends AbstractStoredProcedureParameterInjector
{
    private Name addedEntryName;
    private Attributes addedEntry;
    
    private Map injectors;
    
    public AddDeleteStoredProcedureParameterInjector()
    {        
        injectors = super.getInjectors();
        injectors.put( AddStoredProcedureParameter.ENTRY, $entryInjector );
        injectors.put( AddStoredProcedureParameter.ATTRIBUTES, $attributesInjector );
    }
    
    public void setAddedEntryName( Name addedEntryName )
    {
        this.addedEntryName = addedEntryName;
    }
    
    public Name getAddedEntryName()
    {
        return addedEntryName;
    }
    
    public Attributes getAddedEntry()
    {
        return addedEntry;
    }

    public void setAddedEntry( Attributes addedEntry )
    {
        this.addedEntry = addedEntry;
    }
    
    MicroInjector $entryInjector = new MicroInjector()
    {
        public Object inject() throws NamingException
        {
            return addedEntryName;
        };
    };
    
    MicroInjector $attributesInjector = new MicroInjector()
    {
        public Object inject() throws NamingException
        {
            return addedEntry;
        };
    };

}
