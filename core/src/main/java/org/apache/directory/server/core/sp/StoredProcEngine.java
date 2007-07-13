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


package org.apache.directory.server.core.sp;

import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.ldap.LdapContext;


/**
 * An abstraction over stored procedure execution depending on the type of the language supported.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$ $Date$
 */
public interface StoredProcEngine
{
    
    /**
     * Returns the unique identifier of the supported stored procedure language.
     * 
     */
    public String getSPLangId();
    
    
    /**
     * Registers an entry found to be contaning a stored procedure unit which this engine can operate on.
     *
     * <p>
     * This method should be called before an attempt to invoke a stored procedure via this Engine.
     */
    public void setSPUnitEntry( final Attributes spUnit );
    
    
    /**
     * Invokes the stored procedure handled by the engine.
     * 
     * @param rootDSE A handle on Root DSE to invoke the stored procedure over.
     * @param fullSPName A fully qualified name of the stored procedure including its unit name.
     * @param spArgs A list or arguments to be passed to the stored procedure. It should be an empty array if there aren't any parameters defined.
     * @return The value obtained from invoked procedure. The client should know what will return exactly so that it can downcast to the appropriate type.
     * @throws NamingException If an error occurs during invocation.
     */
    public Object invokeProcedure( LdapContext rootDSE, String fullSPName, Object[] spArgs ) throws NamingException;

}
