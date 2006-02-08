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

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * An abstract base class for {@link ItemPermission} and {@link UserPermission}.
 *
 * @author The Apache Directory Project
 * @version $Rev$, $Date$
 *
 */
public abstract class Permission implements Serializable
{
    private final int precedence;
    private final Set grantsAndDenials;
    private final Set grants;
    private final Set denials;
    
    /**
     * Creates a new instance
     * 
     * @param precedence the precedence of this permission (<tt>-1</tt> to use the default)
     * @param grantsAndDenials the set of {@link GrantAndDenial}s
     */
    protected Permission( int precedence, Collection grantsAndDenials )
    {
        if( precedence < 0 || precedence > 255 )
        {
            precedence = -1;
        }
        
        this.precedence = precedence;
        
        Set tmpGrantsAndDenials = new HashSet();
        Set tmpGrants = new HashSet();
        Set tmpDenials = new HashSet();
        for( Iterator i = grantsAndDenials.iterator(); i.hasNext(); )
        {
            Object val = i.next();
            if( !( val instanceof GrantAndDenial ) )
            {
                throw new IllegalArgumentException(
                        "grantsAndDenials contains a wrong element." );
            }
            
            GrantAndDenial gad = ( GrantAndDenial ) val;
            if( gad.isGrant() )
            {
                tmpGrants.add( gad );
            }
            else
            {
                tmpDenials.add( gad );
            }
            tmpGrantsAndDenials.add( gad );
        }
        
        this.grants = Collections.unmodifiableSet( tmpGrants );
        this.denials = Collections.unmodifiableSet( tmpDenials );
        this.grantsAndDenials = Collections.unmodifiableSet( tmpGrantsAndDenials );
    }
    
    /**
     * Returns the precedence of this permission.
     */
    public int getPrecedence()
    {
        return precedence;
    }
    
    /**
     * Returns the set of {@link GrantAndDenial}s.
     */
    public Set getGrantsAndDenials()
    {
        return grantsAndDenials;
    }
    
    /**
     * Returns the set of grants only.
     */
    public Set getGrants()
    {
        return grants;
    }
    
    /**
     * Returns the set of denials only.
     */
    public Set getDenials()
    {
        return denials;
    }
}
