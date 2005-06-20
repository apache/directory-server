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
import org.apache.ldap.server.partition.ContextPartition;


/**
 * Represents an {@link Invocation} on {@link ContextPartition#getSuffix(boolean)}.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class GetSuffix extends EntryInvocation
{
    private static final long serialVersionUID = 3256443599162980407L;

    private final boolean normalized;


    public GetSuffix( Name name, boolean normalized )
    {
        super( name );
        this.normalized = normalized;
    }


    public boolean isNormalized()
    {
        return normalized;
    }


    protected Object doExecute( BackingStore store ) throws NamingException
    {
        return ( ( ContextPartition ) store ).getSuffix( normalized );
    }
}
