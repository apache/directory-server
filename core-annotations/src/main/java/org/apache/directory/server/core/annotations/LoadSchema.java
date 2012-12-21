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
package org.apache.directory.server.core.annotations;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * An annotation for the schema loading. One can list the schema to be loaded, 
 * and if they must be enabled or disabled.<br/>
 * We can only load new schemas, not unload existing ones.However, we 
 * can disable a schema that has been previously loaded : loading a 
 * schema that is already loaded will just apply the enabled flag on 
 * this schema<br/>
 * Note that a schema may be loaded and disabled.<br/>
 * Some schema are automatically loaded, and there is no way they can be disabled :<br/>
 * <ul>
 *   <li> core</li>
 *   <li>system</li>
 * </ul>
 * 
 * Here is an exemple :
 * <pre>
 * @Schemas( {
 *     @LoadSchema( name = "nis", enabled="TRUE" ),
 *     @LoadSchema( name = "posix", enabled="FALSE" ),
 * })
 * )
 * </pre>
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(
    { ElementType.METHOD, ElementType.TYPE })
public @interface LoadSchema
{
    /** The schema name */
    String name();


    /** The flag indicating if the schema should be enabled or disabled */
    boolean enabled() default true;
}
