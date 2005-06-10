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
package org.apache.ldap.server.configuration;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * A utility class that provides common functionality while validating configuration.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
class ConfigurationUtil
{
    /**
     * Checks all elements of the specified set is of the specified type,
     * and returns cloned set.
     * 
     * @throws ConfigurationException if the specified set has an element of wrong type
     */
    static Set getTypeSafeSet( Set set, Class type )
    {
        Set newSet = new HashSet();
        Iterator i = set.iterator();
        while( i.hasNext() )
        {
            Object e = i.next();
            if( !type.isAssignableFrom( e.getClass() ) )
            {
                throw new ConfigurationException(
                        "Invalid element type: " + e.getClass() +
                        " (expected " + type );
            }
            newSet.add( e );
        }
        return newSet;
    }
    
    /**
     * Returns the clone of the specified set.
     */
    static Set getClonedSet( Set set )
    {
        Set newSet = new HashSet();
        newSet.addAll( set );
        return newSet;
    }
    
    static void validatePortNumber( int port )
    {
        if( port < 0 || port > 65535 )
        {
            throw new ConfigurationException( "Invalid port number: " + port );
        }
    }
    
    private ConfigurationUtil()
    {
    }
}
