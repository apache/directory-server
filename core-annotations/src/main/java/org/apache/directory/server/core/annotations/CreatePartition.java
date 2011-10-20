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

import org.apache.directory.server.core.api.partition.Partition;

/**
 * An annotation for the Partition creation. A partition is defined by
 * a name and a suffix, plus some other characteristics. Here is an example :
 * <pre>
 * @CreatePartition(
 *     name = "example",
 *     suffix = "dc=example, dc=com",
 *     @ContextEntry( 
 *         {
 *             "dn: dc=example, dc=com",
 *             "objectclass: top",
 *             "objectclass: domain",
 *             "dc: example", 
 *         }),
 *     @Indexes( {
 *         @CreateIndex( attribute = "cn" ),
 *         @CreateIndex( attribute = "sn' )
 *     })
 * )
 * </pre>
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target( {ElementType.METHOD, ElementType.TYPE } )
public @interface CreatePartition
{
    /** The partition implementation class */
    Class<? extends Partition> type() default Partition.class;
    
    /** The partition name */
    String name();
    
    /** The partition suffix */
    String suffix();
    
    /** The context entry */
    ContextEntry contextEntry() default @ContextEntry( entryLdif = "" );
    
    /** The associated indexes */
    CreateIndex[] indexes() default {};
    
    /** The cache size */
    int cacheSize() default 1000;
}
