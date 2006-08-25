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
package org.apache.directory.server.core.jndi;


import javax.naming.spi.DirStateFactory;


/**
 * A specialized StateFactory that is optimized for our server-side JNDI
 * provider.  This factory reports the id of the objectClass that it
 * is associated with.  This makes it easier for the server side provider to
 * find the required factory rather than attempt several others within the list
 * of state factories.  JNDI SPI methods are inefficient since they are designed
 * to try all state factories to produce an object.  Our provider looks up
 * the most specific state factories based on additional information.  This
 * makes a huge difference when the number of StateFactories becomes large.
 * <br/>
 * Eventually, it is highly feasible for generated schemas, to also include
 * state and object factories for various objectClasses.  This means the number
 * of factories will increase.  By associating object and state factories with
 * their respective objectClasses we can integrate this into the schema
 * subsystem making factory lookups extremely fast and efficient without costing
 * the user too much to create and store objects within the directory.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public interface ServerDirStateFactory extends DirStateFactory
{
    /**
     * Gets either the OID for the objectClass or the human readable name for
     * the objectClass this DirStateFactory is associated with.  Note
     * that associating this factory with an objectClass automatically
     * associates this DirStateFactory with all descendents of the objectClass.
     *
     * @return the OID or human readable name of the objectClass associated with this StateFactory
     */
    String getObjectClassId();


    /**
     * Gets the Class instance associated with this StateFactory.  Objects to
     * be persisted by this StateFactory must be of this type, a subclass of
     * this type, or implement this type if it is an interface.
     *
     * @return the class associated with this factory.
     */
    Class getAssociatedClass();
}
