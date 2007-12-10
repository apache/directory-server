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
package org.apache.directory.server.protocol.shared.store;


import java.io.Serializable;

import javax.naming.Name;
import javax.naming.directory.DirContext;


/**
 * Interface to support the command pattern with JNDI contexts.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public interface ContextOperation extends Serializable
{
    /**
     * The command pattern execute method.
     * 
     * @param ctx The context to execute the command with
     * @param baseDn The base DN for working with the context
     * @return Object The result returned by the command
     * @throws Exception The exception thrown by the command
     */
    public Object execute( DirContext ctx, Name baseDn ) throws Exception;
}
