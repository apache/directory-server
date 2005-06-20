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


/**
 * Represents an {@link Invocation} on {@link BackingStore#hasEntry(Name)}.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class HasEntry extends EntryInvocation
{
    private static final long serialVersionUID = 3257850974013240370L;

    public HasEntry( Name name )
    {
        super( name );
    }


    protected Object doExecute( BackingStore store ) throws NamingException
    {
        return store.hasEntry( getName() ) ? Boolean.TRUE : Boolean.FALSE;
    }
}
