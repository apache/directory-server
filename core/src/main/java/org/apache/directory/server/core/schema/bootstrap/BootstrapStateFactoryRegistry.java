/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *  
 *    http://www.apache.org/licenses/LICENSE-2.0
 *  
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License. 
 *  
 */
package org.apache.directory.server.core.schema.bootstrap;


import java.util.HashMap;

import javax.naming.NamingException;

import org.apache.directory.server.core.jndi.ServerDirStateFactory;
import org.apache.directory.server.core.schema.StateFactoryRegistry;


/**
 * A bootstrap service implementation for a state factory registry.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class BootstrapStateFactoryRegistry implements StateFactoryRegistry
{
    /** Used to lookup a state factory by class */
    private final HashMap byClass = new HashMap();


    public ServerDirStateFactory getStateFactories( Object obj ) throws NamingException
    {
        Class c = obj.getClass();

        // if the class is mapped to a factory then this is most specific

        if ( byClass.containsKey( c ) )
        {
            return ( ServerDirStateFactory ) byClass.get( c );
        }

        while ( ( c = c.getSuperclass() ) != null )
        {
            if ( byClass.containsKey( c ) )
            {
                return ( ServerDirStateFactory ) byClass.get( c );
            }
        }

        // if we get this far start searching interfaces for a factory

        Class[] interfaces = c.getInterfaces();

        for ( int ii = 0; ii < interfaces.length; ii++ )
        {
            if ( byClass.containsKey( interfaces[ii] ) )
            {
                return ( ServerDirStateFactory ) byClass.get( interfaces[ii] );
            }
        }

        return null;
    }


    public void register( ServerDirStateFactory factory )
    {
        byClass.put( factory.getAssociatedClass(), factory );
    }
}
