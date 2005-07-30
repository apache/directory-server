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

import org.apache.ldap.server.partition.ContextPartitionNexus;


/**
 * Represents a call from JNDI {@link Context} to {@link ContextPartitionNexus}.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class Invocation
{
    private final Context caller;
    private final String name;
    private final List parameters;
    
    /**
     * Creates a new instance that represents an invocation without parameters.
     * 
     * @parem caller the JNDI {@link Context} that made this invocation
     * @param name the name of the called method
     */
    public Invocation( Context caller, String name )
    {
        this( caller, name, null );
    }

    /**
     * Creates a new instance.
     * 
     * @parem caller the JNDI {@link Context} that made this invocation
     * @param name the name of the called method
     * @param parameters the array of parameters passed to the called method
     */
    public Invocation( Context caller, String name, Object[] parameters )
    {
        if( caller == null )
        {
            throw new NullPointerException( "caller" );
        }
        if( name == null )
        {
            throw new NullPointerException( "name" );
        }
        
        if( parameters == null )
        {
            parameters = new Object[ 0 ];
        }
        
        this.caller = caller;
        this.name = name;
        
        List paramList = new ArrayList();
        for( int i = 0; i < parameters.length; i++ )
        {
            paramList.add( parameters[ i ] );
        }
        
        this.parameters = Collections.unmodifiableList( paramList );
    }
    
    /**
     * Returns the JNDI {@link Context} which made this invocation.
     */
    public Context getCaller()
    {
        return caller;
    }
    
    /**
     * Returns the name of the called method.
     */
    public String getName()
    {
        return name;
    }
    
    /**
     * Returns the list of parameters parameters passed to the called method.
     */
    public List getParameters()
    {
        return parameters;
    }
}
