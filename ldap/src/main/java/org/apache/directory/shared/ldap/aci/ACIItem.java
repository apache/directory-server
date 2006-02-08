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
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * An abstract class that provides common properties and operations for
 * {@link ItemFirstACIItem} and {@link UserFirstACIItem} as specified X.501
 * specification.
 *
 * @author The Apache Directory Project
 * @version $Rev$, $Date$
 */
public abstract class ACIItem implements Serializable
{
    private String identificationTag;
    /* 0 ~ 255 */
    private int precedence = 0;
    private AuthenticationLevel authenticationLevel;
    
    /**
     * Creates a new instance
     * 
     * @param identificationTag the id string of this item
     * @param precedence the precedence of this item
     * @param authenticationLevel the level of authentication required to this item
     */
    protected ACIItem(
            String identificationTag,
            int precedence,
            AuthenticationLevel authenticationLevel )
    {
        if( identificationTag == null )
        {
            throw new NullPointerException( "identificationTag" );
        }
        if( precedence < 0 || precedence > 255 )
        {
            throw new IllegalArgumentException( "precedence: " + precedence );
        }
        if( authenticationLevel == null )
        {
            throw new NullPointerException( "authenticationLevel" );
        }
        
        this.identificationTag = identificationTag;
        this.precedence = precedence;
        this.authenticationLevel = authenticationLevel;
    }
    
    /**
     * Returns the id string of this item.
     */
    public String getIdentificationTag()
    {
        return identificationTag;
    }
    
    /**
     * Returns the precedence of this item.
     */
    public int getPrecedence()
    {
        return precedence;
    }
    
    /**
     * Returns the level of authentication required to this item.
     */
    public AuthenticationLevel getAuthenticationLevel()
    {
        return authenticationLevel;
    }
    
    /**
     * Converts this item into a collection of {@link ACITuple}s and
     * returns it.
     */
    public abstract Collection toTuples();

    /**
     * Converts a set of {@link GrantAndDenial}s into a set of
     * {@link MicroOperation}s and returns it.
     */
    protected static Set toMicroOperations( Set grantsAndDenials )
    {
        Set microOps = new HashSet();
        for( Iterator j = grantsAndDenials.iterator(); j.hasNext(); )
        {
            microOps.add( ( ( GrantAndDenial ) j.next() ).getMicroOperation() );
        }
        return microOps;
    }
}
