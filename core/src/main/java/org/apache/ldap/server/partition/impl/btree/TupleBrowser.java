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
package org.apache.ldap.server.partition.impl.btree;


import javax.naming.NamingException;


/**
 * TupleBrowser interface used to abstract 
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public interface TupleBrowser
{
    /**
     * Gets the next value deemed greater than the last using the key 
     * comparator.
     *
     * @param tuple the tuple to populate with a key/value pair
     * @return true if there was a next that was populated or false otherwise
     * @throws NamingException @todo
     */
    boolean getNext( Tuple tuple ) throws NamingException;

    /**
     * Gets the previous value deemed greater than the last using the key 
     * comparator.
     *
     * @param tuple the tuple to populate with a key/value pair
     * @return true if there was a previous value populated or false otherwise
     * @throws NamingException @todo
     */
    boolean getPrevious( Tuple tuple ) throws NamingException;
}
