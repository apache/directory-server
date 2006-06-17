/*
 *   Copyright 2006 The Apache Software Foundation
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

package org.apache.directory.shared.ldap.util;

import java.lang.reflect.Method;

/**
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev:$
 */
public class DirectoryClassUtils
{
    
    /**
     * A replacement for {@link java.lang.Class.getMethod} with extended functionality.
     * 
     * <p>
     * This method returns parameter-list assignment-compatible method as well as
     * exact-signature matching method.
     * 
     * @param clazz
     * @param candidateMethodName
     * @param candidateParameterTypes
     * @return
     * @throws NoSuchMethodException
     */
    public static Method getAssignmentCompatibleMethod( Class clazz,
                                                        String candidateMethodName,
                                                        Class[] candidateParameterTypes
                                                      ) throws NoSuchMethodException
    {
        try
        {
            // Look for exactly the same signature.
            Method exactMethod = clazz.getMethod( candidateMethodName, candidateParameterTypes );
            if ( exactMethod != null )
            {
                return exactMethod;
            }
        }
        catch ( SecurityException e ) { }
        catch ( NoSuchMethodException e ) { }
        
        /**
         * Look for the assignment-compatible signature.
         */
        
        // Get all methods of the clazz.
        Method[] methods = clazz.getMethods();
        
        // For each method of the clazz...
        for ( int mx = 0; mx < methods.length; mx++ )
        {
            // ... Get parameter types list.
            Class[] parameterTypes = methods[ mx ].getParameterTypes();
            
            // If parameter types list length mismatch...
            if ( parameterTypes.length != candidateParameterTypes.length )
            {
                // ... Go on with the next method.
                continue;
            }
            // If parameter types list length is OK...
            // ... For each parameter of the method...
            for ( int px = 0; px < parameterTypes.length; px++ )
            {
                // ... If the parameter is not assignment-compatible with the candidate parameter type...
                if ( ! parameterTypes[ px ].isAssignableFrom( candidateParameterTypes[ px ] ) )
                {
                    // ... Go on with the next method.
                    break;
                }
            }
            
            // Return the only one possible and found method.
            return methods[ mx ];
        }
        
        throw new NoSuchMethodException();
        
    }

}
