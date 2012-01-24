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

/**
 * <pre>
 * <p>
 * Contains interfaces for schema object registry services and simple POJO
 * implementations of these services.  Other helper interfaces and classes are
 * included for handling monitoring of these services.
 * </p>
 * <p>
 * These services and their POJO implementations are purposefully kept really
 * simple here for a reason.  When one looks at these interfaces they stop and
 * think why even bother having them when you can just use a map of objects
 * somewhere.  These simple services can and will get more complex as other
 * facilities come into play namely the object builders that populate these
 * registries.  There might also be caching going on as well as disk based
 * store access.  Finally dependencies become an issue and sometime bootstrap
 * instances of these components are required by the system.  So these simple
 * watered down interfaces and their POJO's have been pruned from previously
 * complex environment specific versions of them.
 * </p>
 * <p>
 * Some key points to apply to services and their POJO impls in this package:
 * <ul>
 * <li>registries only register and allow for lookups: its that simple!</li>
 * <li>don't worry if they change over time</li>
 * <li>don't worry about how they get populated</li>
 * <li>don't worry who or what does the populating</li>
 * <li>don't worry about where the information comes from</li>
 * </ul>
 * </p>
 * </pre>
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */

package org.apache.directory.server.core.schema;


