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
 * Represents an {@link Invocation} on {@link BackingStore#add(String, Name, Attributes)}.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class Add extends Invocation
{

    private final String userProvidedName;

    private final Name normalizedName;

    private final Attributes attributes;


    public Add( String userProvidedName, Name normalizedName, Attributes attributes )
    {
        if ( userProvidedName == null )
        {
            throw new NullPointerException( "userProvidedName" );
        }

        if ( normalizedName == null )
        {
            throw new NullPointerException( "normalizedName" );
        }

        if ( attributes == null )
        {
            throw new NullPointerException( "attributes" );
        }

        this.userProvidedName = userProvidedName;
        this.normalizedName = normalizedName;
        this.attributes = attributes;
    }


    public Attributes getAttributes()
    {
        return attributes;
    }


    public Name getNormalizedName()
    {
        return normalizedName;
    }


    public String getUserProvidedName()
    {
        return userProvidedName;
    }


    protected Object doExecute( BackingStore store ) throws NamingException
    {
        store.add( userProvidedName, normalizedName, attributes );
        return null;
    }
}
