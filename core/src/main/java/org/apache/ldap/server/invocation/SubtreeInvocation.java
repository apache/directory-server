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

import org.apache.ldap.server.BackingStore;

/**
 * Represents a method invocation on a DIT in {@link BackingStore}s.
 * <p/>
 * This class is abstract, and developers should extend this class to
 * represent the actual method invocations.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public abstract class SubtreeInvocation extends Invocation
{
    private Name baseName;

    public SubtreeInvocation( Name baseName )
    {
        setBaseName( baseName );
    }

    public Name getBaseName()
    {
        return baseName;
    }

    public void setBaseName( Name baseName )
    {
        if ( baseName == null )
        {
            throw new NullPointerException( "baseName" );
        }
        this.baseName = baseName;
    }
}
