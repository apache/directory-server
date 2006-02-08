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
package org.apache.directory.server.core.jndi;


import javax.naming.spi.DirObjectFactory;


/**
 * A specialized ObjectFactory that is optimized for our server-side JNDI
 * provider.  This factory reports the Class of objects that it is creates as
 * well as the objectClass corresponding to that Class.  This makes it easier
 * for the server side provider to lookup the respective factory rather than
 * attempt several others within the list of object factories in the order of
 * greatest specificity.  JNDI SPI methods are inefficient since they are
 * designed to try all object factories to produce the object.  Our provider
 * looks up the most specific object factory based on this additional
 * information.  This makes a huge difference when the number of ObjectFactory
 * instances is large.
 * <p/>
 * Eventually, it is highly feasible for generated schemas, to also include
 * state and object factories for various objectClasses, or domain objects.
 * This means the number of factories will increase.  By associating object and
 * state factories with their respective objectClasses and Classes we can
 * integrate these DAOs into the schema subsystem making factory lookups
 * extremely fast and efficient without costing the user too much to create and
 * store objects within the directory.  At the end of the day the directory
 * becomes a hierarchical object store where lookup, bind and rebind are the
 * only operations besides search to access and store objects.  That's pretty
 * PHAT!
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public interface ServerDirObjectFactory extends DirObjectFactory
{
    /**
     * Gets either the OID for the objectClass or the human readable name for
     * the objectClass this DirStateFactory is associated with.  Note
     * that associating this factory with an objectClass automatically
     * associates this DirObjectFactory with all descendents of the objectClass.
     *
     * @return the OID or human readable name of the objectClass associated with this ObjectFactory
     */
    String getObjectClassId();

    /**
     * Gets the Class instance associated with this ObjectFactory.  Objects to
     * be created by this ObjectFactory will be of this type, a subclass of
     * this type, or implement this type if it is an interface.
     *
     * @return the Class associated with this factory.
     */
    Class getAssociatedClass();
}
