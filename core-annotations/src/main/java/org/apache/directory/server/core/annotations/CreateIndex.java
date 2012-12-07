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

import org.apache.directory.server.xdbm.Index;


/**
 * An annotation for the Index creation. It's used when we need to inject an
 * indexed attribute into a given partition. Here is an exemple :
 * <pre>
 * @CreatePartition(
 *     name = "example",
 *     @Indexes( {
 *         @CreateIndex( attribute = "cn" ),
 *         @CreateIndex( attribute = "sn' )
 *     })
 * )
 * </pre>
 * There is one more parameter, the 'factory', which can be used to declare
 * a specific kind of Index. It defaults to JdbmIndex.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(
    { ElementType.METHOD, ElementType.TYPE })
public @interface CreateIndex
{
    /** The index implementation class */
    Class<? extends Index> type() default Index.class;


    /** The cache size */
    int cacheSize() default 1000;


    /** The indexed attribute */
    String attribute();
}
