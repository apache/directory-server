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
package org.apache.directory.server.core.api.sp;


import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.server.core.api.CoreSession;


/**
 * An abstraction over stored procedure execution depending on the type of the language supported.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public interface StoredProcEngine
{

    /**
     * @return the unique identifier of the supported stored procedure language.
     */
    String getSPLangId();


    /**
     * Registers an entry found to be containing a stored procedure unit which this engine can operate on.
     *
     * This method should be called before an attempt to invoke a stored procedure via this Engine.
     * 
     * @param spUnit The Stored Procedure unit to set
     */
    void setSPUnitEntry( Entry spUnit );


    /**
     * Invokes the stored procedure handled by the engine.
     * 
     * @param session The CoreSession it's acting on
     * @param fullSPName A fully qualified name of the stored procedure including its unit name.
     * @param spArgs A list or arguments to be passed to the stored procedure. It should be an empty array if there aren't any parameters defined.
     * @return The value obtained from invoked procedure. The client should know what will return exactly so that it can downcast to the appropriate type.
     * @throws org.apache.directory.api.ldap.model.exception.LdapException If an error occurs during invocation.
     */
    Object invokeProcedure( CoreSession session, String fullSPName, Object[] spArgs ) throws LdapException;
}
