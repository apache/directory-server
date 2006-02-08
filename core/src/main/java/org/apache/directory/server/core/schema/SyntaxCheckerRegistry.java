/*
 *   Copyright 2004 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package org.apache.directory.server.core.schema;


import javax.naming.NamingException;

import org.apache.directory.shared.ldap.schema.SyntaxChecker;


/**
 * SyntaxChecker registry component's service interface.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public interface SyntaxCheckerRegistry
{
    /**
     * Registers a SyntaxChecker with this registry.
     * 
     * @param schema the name of the schema the SyntaxChecker is associated with
     * @param syntaxChecker the SyntaxChecker to register
     * @throws NamingException if the SyntaxChecker is already registered or the
     *      registration operation is not supported
     */
    void register( String schema, String oid, SyntaxChecker syntaxChecker )
        throws NamingException;
    
    /**
     * Looks up a SyntaxChecker by its unique Object Identifier.
     * 
     * @param oid the object identifier
     * @return the SyntaxChecker for the oid
     * @throws NamingException if there is a backing store failure or the 
     *      SyntaxChecker does not exist.
     */
    SyntaxChecker lookup( String oid ) throws NamingException;

    /**
     * Gets the name of the schema this schema object is associated with.
     *
     * @param oid the object identifier
     * @return the schema name
     * @throws NamingException if the schema object does not exist
     */
    String getSchemaName( String oid ) throws NamingException;

    /**
     * Checks to see if a SyntaxChecker exists.  Backing store failures simply 
     * return false.
     * 
     * @param oid the object identifier
     * @return true if a SyntaxChecker definition exists for the oid, false 
     *      otherwise
     */
    boolean hasSyntaxChecker( String oid );
}
