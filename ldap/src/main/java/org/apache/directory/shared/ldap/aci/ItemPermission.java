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

/**
 * Represents permissions to be applied to all {@link ProtectedItem}s in
 * {@link ItemFirstACIItem}. 
 *
 * @author The Apache Directory Project
 * @version $Rev$, $Date$
 */
public class ItemPermission extends Permission
{
    private static final long serialVersionUID = 3940100745409337694L;

    private final Collection userClasses;

    /**
     * Creates a new instance
     * 
     * @param precedence the precedence of this permission (<tt>-1</tt> to use the default)
     * @param grantsAndDenials the set of {@link GrantAndDenial}s
     * @param userClasses the collection of {@link UserClass}es
     */
    public ItemPermission( int precedence, Collection grantsAndDenials, Collection userClasses )
    {
        super( precedence, grantsAndDenials );
        
        for( Iterator i = userClasses.iterator(); i.hasNext(); )
        {
            Object val = i.next();
            if( !( val instanceof UserClass ) )
            {
                throw new IllegalArgumentException(
                        "userClasses contains a wrong element." );
            }
        }
        
        this.userClasses = Collections.unmodifiableCollection( new ArrayList( userClasses ) );
    }

    /**
     * Returns the collection of {@link UserClass}es.
     */
    public Collection getUserClasses()
    {
        return userClasses;
    }
    
    public String toString()
    {
        return "itemPermission: precedence=" + getPrecedence() + ", " +
               "userClasses=" + userClasses + ", " +
               "grantsAndDenials=" + getGrantsAndDenials();
    }
}
