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
package org.apache.ldap.server.jndi.invocation;


import org.apache.ldap.server.BackingStore;

import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;


/**
 * Represents an {@link Invocation} on {@link BackingStore#modify(Name, int, Attributes)}.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class Modify extends Invocation
{

    private final Name name;

    private final int modOp;

    private final Attributes attributes;


    public Modify( Name name, int modOp, Attributes attributes )
    {
        if ( name == null )
        {
            throw new NullPointerException( "name" );
        }

        this.name = name;
        this.modOp = modOp;
        this.attributes = attributes;
    }


    public Name getName()
    {
        return name;
    }


    public int getModOp()
    {
        return modOp;
    }


    public Attributes getAttributes()
    {
        return attributes;
    }


    protected Object doExecute( BackingStore store ) throws NamingException
    {
        store.modify( name, modOp, attributes );
        return null;
    }
}
