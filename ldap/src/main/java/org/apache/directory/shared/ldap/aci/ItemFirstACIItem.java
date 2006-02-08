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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;

/**
 * An {@link ACIItem} which specifies {@link ProtectedItem}s first and then
 * {@link UserClass}es each {@link ProtectedItem} will have.  (18.4.2.4. X.501)
 *
 * @author The Apache Directory Project
 * @version $Rev$, $Date$
 */
public class ItemFirstACIItem extends ACIItem
{
    private static final long serialVersionUID = -8199453391060356463L;
    
    private final Collection protectedItems;
    private final Collection itemPermissions;

    /**
     * Creates a new instance.
     * 
     * @param identificationTag the id string of this item
     * @param precedence the precedence of this item
     * @param authenticationLevel the level of authentication required to this item
     * @param protectedItems the collection of {@link ProtectedItem}s this item protects
     * @param itemPermissions the collection of {@link ItemPermission}s each <tt>protectedItems</tt> will have
     */
    public ItemFirstACIItem(
            String identificationTag,
            int precedence,
            AuthenticationLevel authenticationLevel,
            Collection protectedItems,
            Collection itemPermissions )
    {
        super( identificationTag, precedence, authenticationLevel );
        
        for( Iterator i = protectedItems.iterator(); i.hasNext(); )
        {
            if( !ProtectedItem.class.isAssignableFrom( i.next().getClass() ) )
            {
                throw new IllegalArgumentException(
                        "protectedItems contains an element which is not a protected item." );
            }
        }

        for( Iterator i = itemPermissions.iterator(); i.hasNext(); )
        {
            if( !ItemPermission.class.isAssignableFrom( i.next().getClass() ) )
            {
                throw new IllegalArgumentException(
                        "itemPermissions contains an element which is not an item permission." );
            }
        }

        this.protectedItems = Collections.unmodifiableCollection( new ArrayList( protectedItems ) );
        this.itemPermissions = Collections.unmodifiableCollection( new ArrayList( itemPermissions ) );
    }

    /**
     * Returns the collection of {@link ProtectedItem}s.
     */
    public Collection getProtectedItems()
    {
        return protectedItems;
    }

    /**
     * Returns the collection of {@link ItemPermission}s.
     */
    public Collection getItemPermissions()
    {
        return itemPermissions;
    }
    
    public String toString()
    {
        return "itemFirstACIItem: " +
               "identificationTag=" + getIdentificationTag() + ", " +
               "precedence=" + getPrecedence() + ", " +
               "authenticationLevel=" + getAuthenticationLevel() + ", " +
               "protectedItems=" + protectedItems + ", " +
               "itemPermissions=" + itemPermissions;
    }

    public Collection toTuples()
    {
        Collection tuples = new ArrayList();
        for( Iterator i = itemPermissions.iterator(); i.hasNext(); )
        {
            ItemPermission itemPermission = ( ItemPermission ) i.next();
            Set grants = itemPermission.getGrants();
            Set denials = itemPermission.getDenials();
            int precedence = itemPermission.getPrecedence() >= 0?
                    itemPermission.getPrecedence() : this.getPrecedence();
                    
            if( grants.size() > 0 )
            {
                tuples.add( new ACITuple(
                        itemPermission.getUserClasses(),
                        getAuthenticationLevel(),
                        protectedItems,
                        toMicroOperations( grants ),
                        true,
                        precedence ) );
            }
            if( denials.size() > 0 )
            {
                tuples.add( new ACITuple(
                        itemPermission.getUserClasses(),
                        getAuthenticationLevel(),
                        protectedItems,
                        toMicroOperations( denials ),
                        false,
                        precedence ) );
            }
        }
        return tuples;
    }
}
