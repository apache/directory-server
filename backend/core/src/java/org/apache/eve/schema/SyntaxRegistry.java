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
package org.apache.eve.schema;


import javax.naming.NamingException;

import org.apache.ldap.common.schema.Syntax;


/**
 * Manages the lookup and registration of Syntaxes within the system by OID.
 *
 * @author <a href="mailto:directory-dev@incubator.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public interface SyntaxRegistry
{
    /**
     * Looks up a Syntax by its unique Object Identifier.
     * 
     * @param oid the object identifier
     * @return the Syntax for the oid
     * @throws NamingException if there is a backing store failure or the Syntax
     * does not exist.
     */
    Syntax lookup( String oid ) throws NamingException;
    
    /**
     * Registers a Syntax with this registry.  
     * 
     * @param syntax the Syntax to register
     * @throws NamingException if the syntax is already registered or the 
     * registration operation is not supported
     */
    void register( Syntax syntax ) throws NamingException;

    /**
     * Checks to see if a Syntax exists.  Backing store failures simply return
     * false.
     * 
     * @param oid the object identifier
     * @return true if a Syntax definition exists for the oid, false otherwise
     */
    boolean hasSyntax( String oid );
}
