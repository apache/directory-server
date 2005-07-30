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
package org.apache.ldap.server.schema;


import javax.naming.NamingException;
import javax.naming.ldap.LdapContext;

import org.apache.ldap.server.jndi.ServerDirObjectFactory;


/**
 * A registry used for looking up JNDI object factories based on meta data
 * regarding the objectClass and Class associations with the object factory.
 * Unlike other registries which often throw exceptions when they cannot find
 * an object, this one does not.  It returns null if an 'optional' object
 * factory cannot be found.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public interface ObjectFactoryRegistry
{
    /**
     * Gets the list of ObjectFactories associated with an entry.  Several object
     * factories could be associated with an entry due to the presence of
     * auxiliary objectClasses.
     *
     * @param ctx the context of the entry
     * @return the ObjectFactories that could be used for the entry
     */
    ServerDirObjectFactory getObjectFactories( LdapContext ctx ) throws NamingException;

    /**
     * Registers a server-side object factory with this registry.
     *
     * @param factory the factory to register.
     */
    void register( ServerDirObjectFactory factory ) throws NamingException;
}
