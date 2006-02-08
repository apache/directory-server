/*
 *   @(#) $Id$
 *
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
package org.apache.directory.shared.ldap.aci;


import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;


/**
 * Represents permissions to be applied to all {@link UserClass}es in
 * {@link UserFirstACIItem}.
 * 
 * @author The Apache Directory Project
 * @version $Rev$, $Date$
 */
public class UserPermission extends Permission
{
    private static final long serialVersionUID = 3940100745409337694L;

    private final Collection protectedItems;


    /**
     * Creates a new instance
     * 
     * @param precedence
     *            the precedence of this permission (<tt>-1</tt> to use the
     *            default)
     * @param grantsAndDenials
     *            the set of {@link GrantAndDenial}s
     * @param protectedItems
     *            the collection of {@link ProtectedItem}s
     */
    public UserPermission(int precedence, Collection grantsAndDenials, Collection protectedItems)
    {
        super( precedence, grantsAndDenials );

        for ( Iterator i = protectedItems.iterator(); i.hasNext(); )
        {
            Object val = i.next();
            if ( !( val instanceof ProtectedItem ) )
            {
                throw new IllegalArgumentException( "protectedItems contains a wrong element." );
            }
        }

        this.protectedItems = Collections.unmodifiableCollection( protectedItems );
    }


    /**
     * Returns the collection of {@link ProtectedItem}s.
     */
    public Collection getProtectedItems()
    {
        return protectedItems;
    }


    public String toString()
    {
        return "itemPermission: precedence=" + getPrecedence() + ", " + "protectedItems=" + protectedItems + ", "
            + "grantsAndDenials=" + getGrantsAndDenials();
    }
}
