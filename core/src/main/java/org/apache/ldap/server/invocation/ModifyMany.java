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
package org.apache.ldap.server.invocation;


import org.apache.ldap.server.partition.BackingStore;

import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.directory.ModificationItem;


/**
 * Represents an {@link Invocation} on {@link BackingStore#modify(Name, ModificationItem[])}.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class ModifyMany extends EntryInvocation
{
    private static final long serialVersionUID = 3258695386024915001L;

    private final ModificationItem[] modificationItems;


    public ModifyMany( Name name, ModificationItem[] modificationItems )
    {
        super( name );
        if ( modificationItems == null )
        {
            throw new NullPointerException( "modificationItems" );
        }
        this.modificationItems = modificationItems;
    }


    public ModificationItem[] getModificationItems()
    {
        return modificationItems;
    }


    protected Object doExecute( BackingStore store ) throws NamingException
    {
        store.modify( getName(), modificationItems );
        return null;
    }
}
