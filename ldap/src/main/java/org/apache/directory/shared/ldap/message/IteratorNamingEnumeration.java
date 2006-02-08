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
 * $Id: IteratorNamingEnumeration.java,v 1.2 2003/07/31 21:44:48 akarasulu Exp $
 *
 * -- (c) LDAPd Group                                                    --
 * -- Please refer to the LICENSE.txt file in the root directory of      --
 * -- any LDAPd project for copyright and distribution information.      --
 *
 */

package org.apache.directory.shared.ldap.message;


import java.util.Iterator;
import javax.naming.NamingEnumeration;


/**
 * A NamingEnumeration over an Iterator.
 * 
 * @author <a href="mailto:aok123@bellsouth.net">Alex Karasulu</a>
 * @author <a href="mailto:jmachols@attbi.com">Jeff Machols</a>
 * @author $Author: akarasulu $
 * @version $Revision$
 */
public class IteratorNamingEnumeration implements NamingEnumeration
{
    /** the iterator to wrap as in the enumeration */
    private final Iterator m_iterator;


    /**
     * Creates a NamingEnumeration over an Iterator.
     * 
     * @param a_iterator
     *            the Iterator the NamingEnumeration is based on.
     */
    public IteratorNamingEnumeration(final Iterator a_iterator)
    {
        m_iterator = a_iterator;
    }


    // --------------------------------------------------------------------
    // Enumeration Interface Method Implementations
    // --------------------------------------------------------------------

    /**
     * @see java.util.Enumeration#hasMoreElements()
     */
    public boolean hasMoreElements()
    {
        return m_iterator.hasNext();
    }


    /**
     * @see java.util.Enumeration#nextElement()
     */
    public Object nextElement()
    {
        return m_iterator.next();
    }


    // --------------------------------------------------------------------
    // NamingEnumeration Interface Method Implementations
    // --------------------------------------------------------------------

    /**
     * @see javax.naming.NamingEnumeration#close()
     */
    public void close()
    {
        // Does nothing!
    }


    /**
     * @see javax.naming.NamingEnumeration#hasMore()
     */
    public boolean hasMore()
    {
        return m_iterator.hasNext();
    }


    /**
     * @see javax.naming.NamingEnumeration#next()
     */
    public Object next()
    {
        return m_iterator.next();
    }
}
