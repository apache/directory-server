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
package org.apache.eve.listener ;


import java.io.IOException ;


/**
 * Manages a set of server listeners.
 * 
 * @author <a href="mailto:aok123@bellsouth.net">Alex Karasulu</a>
 * @author $Author$
 * @version $Rev$
 */
public interface ListenerManager
{
    /** Avalon likes to have the ROLE associated with the service interface */
    String ROLE = ListenerManager.class.getName() ;
    
    /**
     * Binds and registers a server listener.
     * 
     * @param a_listener the listener to register and bind
     */
    public void bind( ServerListener a_listener ) throws IOException ;
    
    /**
     * Unbinds and unregisters a server listener.
     * 
     * @param a_listener the listener to unregister and unbind
     * @throws IOException if there is a problem unbinding a listener
     */
    public void unbind( ServerListener a_listener ) throws IOException ;
}
