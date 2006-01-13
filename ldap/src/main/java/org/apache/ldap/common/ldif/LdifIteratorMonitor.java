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
package org.apache.ldap.common.ldif ;


/**
 * A monitor for an LDIF iterator.
 *
 * @author <a href="mailto:dev@directory.apache.org">
 * Apache Directory Project</a>
 * @version $Rev$
 */
public interface LdifIteratorMonitor
{
    /**
     * Monitors fatal Iterator failures.
     * 
     * @param msg the message associated with the failure
     * @param cause the throwable that caused the failure
     */
    void fatalFailure( String msg, Throwable cause ) ; 
    
    /**
     * Monitors recoverable Iterator failures.
     * 
     * @param msg the message associated with the failure
     * @param cause the throwable that caused the failure
     */
    void failure( String msg, Throwable cause ) ;
    
    /**
     * Monitors the availablity of information.
     * 
     * @param msg the information
     */ 
    void infoAvailable( String msg ) ;
}
