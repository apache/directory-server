/*
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 *
 */

package org.apache.directory.server.component.handler;


import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

import org.apache.directory.server.hub.api.component.util.InterceptionPoint;
import org.apache.directory.server.hub.api.component.util.InterceptorOperation;


/**
 * Used to declare ApacheDS Interceptor IPojo Component.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@Target(ElementType.TYPE)
public @interface DirectoryInterceptor
{
	boolean factory() default true;


    boolean exclusive() default false;


    boolean threadsafe() default true;
    
    /*
     * Used to specify interception point for all component instances
     * instantiated from factory.
     */
    InterceptionPoint interceptionPoint() default InterceptionPoint.END;

    /*
     * Used to specify in which operations this factory's instances
     * will be called.
     */
    String[] operations();
}
