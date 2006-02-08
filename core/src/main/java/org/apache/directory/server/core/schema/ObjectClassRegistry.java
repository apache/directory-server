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


import java.util.Iterator;

import javax.naming.NamingException;

import org.apache.directory.shared.ldap.schema.ObjectClass;


/**
 * ObjectClass registry service interface.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public interface ObjectClassRegistry
{
    /**
     * Registers an ObjectClass with this registry.
     *
     * @param schema the name of the schema the ObjectClass is associated with
     * @param objectClass the objectClass to register
     * @throws NamingException if the ObjectClass is already registered or the
     * registration operation is not supported
     */
    void register( String schema, ObjectClass objectClass ) throws NamingException;

    /**
     * Looks up an objectClass by its unique Object Identifier or by name.
     *
     * @param id the object identifier or name
     * @return the ObjectClass instance for the id
     * @throws NamingException if the ObjectClass does not exist
     */
    ObjectClass lookup( String id ) throws NamingException;

    /**
     * Gets the name of the schema this schema object is associated with.
     *
     * @param id the object identifier or the name
     * @return the schema name
     * @throws NamingException if the schema object does not exist
     */
    String getSchemaName( String id ) throws NamingException;

    /**
     * Checks to see if an objectClass exists.
     *
     * @param id the object identifier or name
     * @return true if an objectClass definition exists for the id, false
     * otherwise
     */
    boolean hasObjectClass( String id );

    /**
     * Gets an Iterator over the ObjectClasses within this ObjectClassRegistry.
     *
     * @return an iterator over all ObjectClasses in registry
     */
    Iterator list();
}
