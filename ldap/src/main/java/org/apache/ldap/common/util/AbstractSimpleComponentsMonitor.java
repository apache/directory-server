/*
 *   Copyright 2005 The Apache Software Foundation
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


package org.apache.ldap.common.util;


import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;


public abstract class AbstractSimpleComponentsMonitor implements ComponentsMonitor
{
    private List components;

    public AbstractSimpleComponentsMonitor( String [] components )
    {
        // register components
        this.components = new LinkedList( Arrays.asList( components ) );
    }
    
    public ComponentsMonitor useComponent( String component ) throws IllegalArgumentException
    {
        if ( !components.remove( component ) )
        {
            throw new IllegalArgumentException( "Unregistered or previously used component: " + component );
        }
        
        return this;
    }

    public boolean allComponentsUsed()
    {
        return components.isEmpty();
    }

    public List getRemainingComponents()
    {
        return Collections.unmodifiableList( components );
    }

    public abstract boolean finalStateValid();
}
