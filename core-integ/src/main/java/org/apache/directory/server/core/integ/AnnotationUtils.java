/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.directory.server.core.integ;


import org.apache.directory.server.core.integ.annotations.Factory;
import org.apache.directory.server.core.integ.annotations.Mode;


/**
 * Various utility methods for dealing with annotations all over.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class AnnotationUtils
{
    public static SetupMode getMode( Mode modeAnnotation, SetupMode defaultMode )
    {
        if ( modeAnnotation != null && modeAnnotation.value() != null )
        {
            return modeAnnotation.value();
        }
        else
        {
            return defaultMode;
        }
    }


    public static DirectoryServiceFactory newFactory( Factory factoryAnnotation,
                                                      DirectoryServiceFactory defaultFactory )
    {
        DirectoryServiceFactory factory = defaultFactory;

        if ( factoryAnnotation != null )
        {
            try
            {
                factory = ( DirectoryServiceFactory ) factoryAnnotation.getClass().newInstance();
            }
            catch ( ClassCastException e )
            {
                throw new RuntimeException( "The specified factory '" +
                        factoryAnnotation.getClass() + "' does not implement DirectoryServiceFactory", e );
            }
            catch ( InstantiationException e )
            {
                throw new RuntimeException( "The specified factory '" +
                        factoryAnnotation.getClass() + "' does not contain a default constructor", e );
            }
            catch ( IllegalAccessException e )
            {
                throw new RuntimeException( "The specified factory '" +
                        factoryAnnotation.getClass() + "' does not contain a public default constructor", e );
            }
        }

        return factory;
    }
}
