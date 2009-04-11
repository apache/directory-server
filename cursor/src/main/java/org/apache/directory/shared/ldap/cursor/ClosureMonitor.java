/*
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 *
 */
package org.apache.directory.shared.ldap.cursor;


/**
 * A monitor used by Cursors to detect conditions when they should stop 
 * performing some work during advance operations such as next(), previous(),
 * first() etc, and release resources.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public interface ClosureMonitor
{
    /**
     * Sets monitor state to closed, and sets the cause to a 
     * CursorClosedException without an error message string.
     */
    void close();

    
    /**
     * Sets monitor state to closed, and sets the cause to a 
     * CursorClosedException with a specific error message string.
     * 
     * @param cause error message string
     */
    void close( String cause );
    
    
    /**
     * Sets monitor state to closed, and sets the cause to a specific 
     * Exception.
     * 
     * @param cause the exception to associate with the closure
     */
    void close( Exception cause );
    
    
    /**
     * Gets whether the state of this ClosureMonitor is set to closed.
     *
     * @return true if state is closed, false if open
     */
    boolean isClosed();
    
    
    /**
     * Checks if state of this ClosureMonitor is set to closed and if so, 
     * throws the causing Exception.
     *
     * @throws Exception the cause of the closure
     */
    void checkNotClosed() throws Exception;
    
    
    /**
     * Gets the cause of the closure.
     *
     * @return the causing Exception
     */
    Exception getCause();
}
