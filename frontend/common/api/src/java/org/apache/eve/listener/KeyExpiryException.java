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
 * An exception that is raised when the accessor methods on an expired ClientKey
 * are used.
 * 
 * @author <a href="mailto:directory-dev@incubator.apache.org">
 * Apache Directory Project</a>
 * @version $Rev$
 */
public class KeyExpiryException extends IOException
{
    /** the key that caused this exception by being accessed after expiring */
    private final ClientKey m_key ;
    
    /** 
     * Constructs an Exception without a message.
     * 
     * @param a_key the unique key for the client which expired. 
     */
    public KeyExpiryException( ClientKey a_key ) 
    {
        super() ;
        m_key = a_key ;
    }

    
    /**
     * Constructs an Exception with a detailed message.
     * 
     * @param a_key the unique key for the client which expired. 
     * @param a_message The message associated with the exception.
     */
    public KeyExpiryException( ClientKey a_key, String a_message ) 
    {
        super( a_message );
        m_key = a_key ;
    }


    /**
     * Gets the expired key which caused this exception when it was accessed.
     * 
     * @return the expired ClientKey.
     */
    public ClientKey getClientKey()
    {
        return m_key ;
    }
}
