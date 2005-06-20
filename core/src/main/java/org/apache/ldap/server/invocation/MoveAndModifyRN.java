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


import javax.naming.Name;
import javax.naming.NamingException;

import org.apache.ldap.server.partition.BackingStore;


/**
 * Represents an {@link org.apache.ldap.server.invocation.Invocation} on {@link BackingStore#move(Name, Name, String, boolean)}.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class MoveAndModifyRN extends EntryInvocation
{
    private static final long serialVersionUID = 3258135751786248245L;

    private Name newParentName;

    private final String newRelativeName;

    private final boolean deleteOldName;


    public MoveAndModifyRN( Name name, Name newParentName, String newRelativeName,
                            boolean deleteOldName )
    {
        super( name );
        setNewParentName( newParentName );

        if ( newRelativeName == null )
        {
            throw new NullPointerException( "newRelativeName" );
        }

        this.newRelativeName = newRelativeName;
        this.deleteOldName = deleteOldName;
    }


    public Name getNewParentName()
    {
        return newParentName;
    }


    public void setNewParentName( Name newParentName )
    {
        if ( newParentName == null )
        {
            throw new NullPointerException( "newParentName" );
        }
        this.newParentName = newParentName;
    }


    public String getNewRelativeName()
    {
        return newRelativeName;
    }


    public boolean isDeleteOldName()
    {
        return deleteOldName;
    }


    protected Object doExecute( BackingStore store ) throws NamingException
    {
        store.move( getName(), newParentName, newRelativeName, deleteOldName );
        return null;
    }
}
