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


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.naming.Context;

import org.apache.ldap.server.partition.ContextPartition;


/**
 * Represents a method invocation on {@link ContextPartition}s.
 * <p/>
 * This class is abstract, and developers should extend this class to
 * represent the actual method invocations.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class Invocation
{
    private final Context target;
    private final String name;
    private final List parameters;
    
    /**
     * Creates a new instance.
     */
    public Invocation( Context target, String name )
    {
        this( target, name, null );
    }

    /**
     * Creates a new instance.
     */
    public Invocation( Context target, String name, Object[] parameters )
    {
        if( target == null )
        {
            throw new NullPointerException( "target" );
        }
        if( name == null )
        {
            throw new NullPointerException( "name" );
        }
        
        if( parameters == null )
        {
            parameters = new Object[ 0 ];
        }
        
        this.target = target;
        this.name = name;
        
        List paramList = new ArrayList();
        for( int i = 0; i < parameters.length; i++ )
        {
            paramList.add( parameters[ i ] );
        }
        
        this.parameters = Collections.unmodifiableList( paramList );
    }
    
    /**
     * Returns the target context of this invocation.
     */
    public Context getTarget()
    {
        return target;
    }
    
    /**
     * Returns the name of this invocation.
     */
    public String getName()
    {
        return name;
    }
    
    /**
     * Returns the list of parameters
     */
    public List getParameters()
    {
        return parameters;
    }
}
