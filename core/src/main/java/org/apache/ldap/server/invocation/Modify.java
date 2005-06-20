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
import javax.naming.directory.Attributes;

import org.apache.ldap.server.partition.ContextPartition;


/**
 * Represents an {@link Invocation} on {@link ContextPartition#modify(Name, int, Attributes)}.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class Modify extends EntryInvocation
{
    private static final long serialVersionUID = 3258134673732416053L;

    private final int modOp;

    private final Attributes attributes;


    public Modify( Name name, int modOp, Attributes attributes )
    {
        super( name );
        this.modOp = modOp;
        this.attributes = attributes;
    }


    public int getModOp()
    {
        return modOp;
    }


    public Attributes getAttributes()
    {
        return attributes;
    }


    protected Object doExecute( ContextPartition store ) throws NamingException
    {
        store.modify( getName(), modOp, attributes );
        return null;
    }
}
