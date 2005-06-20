/*
 *   @(#) $Id$
 * 
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

import org.apache.ldap.server.partition.ContextPartition;

/**
 * Represents a method invocation on a single entry in {@link ContextPartition}s.
 * <p/>
 * This class is abstract, and developers should extend this class to
 * represent the actual method invocations.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public abstract class EntryInvocation extends Invocation
{
    private Name name;

    public EntryInvocation( Name name )
    {
        setName( name );
    }
    
    public Name getName()
    {
        return name;
    }
    
    public void setName( Name name )
    {
        if ( name == null )
        {
            throw new NullPointerException( "name" );
        }

        this.name = name;
    }
}
