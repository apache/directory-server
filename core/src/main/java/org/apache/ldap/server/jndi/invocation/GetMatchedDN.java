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

import javax.naming.Name;
import javax.naming.NamingException;

import org.apache.ldap.server.BackingStore;
import org.apache.ldap.server.PartitionNexus;

/**
 * Represents an {@link Invocation} on {@link PartitionNexus#getMatchedDn(Name, boolean)}.
 * 
 * @author Apache Directory Project (dev@directory.apache.org)
 * @author Trustin Lee (trustin@apache.org)
 * @version $Rev$, $Date$
 */
public class GetMatchedDN extends Invocation {

    private final Name name;
    private final boolean normalized;
    
    public GetMatchedDN( Name name, boolean normalized )
    {
        if( name == null )
        {
            throw new NullPointerException( "name" );
        }
        
        this.name = name;
        this.normalized = normalized;
    }

    public boolean isNormalized() {
        return normalized;
    }

    public Name getName() {
        return name;
    }

    protected Object doExecute(BackingStore store) throws NamingException {
        return ( ( PartitionNexus ) store ).getMatchedDn( name, normalized );
    }
}
