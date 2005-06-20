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

import org.apache.ldap.server.partition.ContextPartition;
import org.apache.ldap.server.partition.ContextPartitionNexus;


/**
 * Represents an {@link Invocation} on {@link ContextPartitionNexus#getMatchedDn(Name, boolean)}.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class GetMatchedDN extends EntryInvocation
{
    private static final long serialVersionUID = 3834032467559723826L;

    private final boolean normalized;


    public GetMatchedDN( Name name, boolean normalized )
    {
        super( name );
        this.normalized = normalized;
    }


    public boolean isNormalized()
    {
        return normalized;
    }


    protected Object doExecute( ContextPartition store ) throws NamingException
    {
        return ( ( ContextPartitionNexus ) store ).getMatchedDn( getName(), isNormalized() );
    }
}
