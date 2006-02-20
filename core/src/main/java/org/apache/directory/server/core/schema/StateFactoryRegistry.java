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

import org.apache.directory.server.core.jndi.ServerDirStateFactory;


/**
 * A registry used for looking up JNDI state factories based on meta data
 * regarding the objectClass and Class associations with the factory. Unlike
 * other registries which often throw exceptions when they cannot find an
 * object, this one does not.  It returns null if an 'optional' state factory
 * cannot be found.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public interface StateFactoryRegistry
{
    /**
     * Gets the list of StateFactories associated with a class.  Several state
     * factories may be associated with a class or interface depending on its
     * ancestry.  Also more specific factories may be registered for subclasses
     * of the class.  So a request for a general class may result in several
     * factories which could persist the state of an object although more
     * specifically.
     *
     * @param obj the object to be persisted by the factories
     * @return the set of state factories which persist objects of the specified class
     */
    ServerDirStateFactory getStateFactories( Object obj ) throws NamingException;


    /**
     * Registers a server-side state factory with this registry.
     *
     * @param factory the factory to register.
     */
    void register( ServerDirStateFactory factory );
}
