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
package org.apache.directory.server.core.annotations;


import java.lang.annotation.Annotation;
import java.lang.reflect.Method;


/**
 * An helper class used to find annotations in methods and classes
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public final class AnnotationUtils
{
    private AnnotationUtils()
    {
    }


    /**
     * Get an instance of a class extracted from the annotation found in the method
     * or the class. We iterate on the stack trace until we find the desired annotation.
     * 
     * @param clazz The Annotation we want to get an instance for
     * @return The instance or null if no annotation is found
     * @throws ClassNotFoundException If we can't find a class
     */
    public static Object getInstance( Class<? extends Annotation> clazz ) throws ClassNotFoundException
    {
        Object instance = null;

        // Get the caller by inspecting the stackTrace
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();

        // Iterate on the stack trace.
        for ( int i = stackTrace.length - 1; i >= 0; i-- )
        {
            Class<?> classCaller = null;

            // Get the current class
            try
            {
                classCaller = Class.forName( stackTrace[i].getClassName() );
            }
            catch ( ClassNotFoundException cnfe )
            {
                // Corner case : we just have to go higher in the stack in this case.
                continue;
            }

            // Get the current method
            String methodCaller = stackTrace[i].getMethodName();

            // Check if we have any annotation associated with the method
            Method[] methods = classCaller.getMethods();

            for ( Method method : methods )
            {
                if ( methodCaller.equals( method.getName() ) )
                {
                    instance = method.getAnnotation( clazz );

                    if ( instance != null )
                    {
                        break;
                    }
                }
            }

            if ( instance == null )
            {
                instance = classCaller.getAnnotation( clazz );
            }

            if ( instance != null )
            {
                break;
            }
        }

        return instance;
    }
}
