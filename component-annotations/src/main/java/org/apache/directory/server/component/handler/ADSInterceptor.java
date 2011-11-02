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
package org.apache.directory.server.component.handler;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * This annotation declares an ApacheDS Interceptor.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@Target(ElementType.TYPE)
public @interface ADSInterceptor
{
    /*
     * ordering will be used to sort this interceptor
     * among other isntalled ones.
     * 
     * Default value, "relax", will cause it be sorted with
     * the other relexed ones at the end of the interceptor list
     * by alphabetical order.
     */
    String ordering() default "relax";
}
