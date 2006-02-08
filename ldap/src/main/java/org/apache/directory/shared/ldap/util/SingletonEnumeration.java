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

/*
 * $Id: SingletonEnumeration.java,v 1.1 2003/09/16 05:29:43 akarasulu Exp $
 *
 * -- (c) LDAPd Group                                                    --
 * -- Please refer to the LICENSE.txt file in the root directory of      --
 * -- any LDAPd project for copyright and distribution information.      --
 *
 * Created on Aug 11, 2003
 */
package org.apache.directory.shared.ldap.util;


import javax.naming.NamingEnumeration ;

import java.util.NoSuchElementException ;


/**
 * A NamingEnumeration over a single element.
 * 
 * @author <a href="mailto:aok123@bellsouth.net">Alex Karasulu</a>
 * @author $Author: akarasulu $
 * @version $Revision$
 */
public class SingletonEnumeration implements NamingEnumeration
{
    /** The singleton element to return */
    private final Object m_element ;
    /** Can we return a element */
    private boolean m_hasMore = true ;
    

    /**
     * Creates a NamingEnumeration over a single element.
     *
     * @param a_element TODO
     */
    public SingletonEnumeration( final Object a_element )
    {
        m_element = a_element ;
    }
    

    /**
     * Makes calls to hasMore to false even if we had more.
     *
     * @see javax.naming.NamingEnumeration#close()
     */
    public void close() 
    {
        m_hasMore = false ;
    }
    

    /**
     * @see javax.naming.NamingEnumeration#hasMore()
     */
    public boolean hasMore() 
    {
        return m_hasMore ;
    }


    /**
     * @see javax.naming.NamingEnumeration#next()
     */
    public Object next() 
    {
        if ( m_hasMore )
        {
            m_hasMore = false ;
            return m_element ;
        }
        
        throw new NoSuchElementException() ;
    }
    

    /**
     * @see java.util.Enumeration#hasMoreElements()
     */
    public boolean hasMoreElements()
    {
        return m_hasMore ;
    }


    /**
     * @see java.util.Enumeration#nextElement()
     */
    public Object nextElement()
    {
        return next() ;
    }
}
