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


/**
 * An enumeration that represents the level of authentication.
 * 
 * @author The Apache Directory Project
 * @version $Rev$, $Date$
 */
public class AuthenticationLevel implements Comparable, Serializable
{
    private static final long serialVersionUID = -6757937682267073130L;

    /**
     * No authentication (anonymous access)
     */
    public static final AuthenticationLevel NONE = new AuthenticationLevel( 0, "none" );

    /**
     * Simple authentication (bound with plain-text credentials)
     */
    public static final AuthenticationLevel SIMPLE = new AuthenticationLevel( 1, "simple" );

    /**
     * Strong authentication (bound with encrypted cerdentials)
     */
    public static final AuthenticationLevel STRONG = new AuthenticationLevel( 2, "strong" );

    private final int level;

    private final String name;


    private AuthenticationLevel(int level, String name)
    {
        this.level = level;
        this.name = name;
    }


    /**
     * Returns the integet value of this level (greater value, stronger level).
     */
    public int getLevel()
    {
        return level;
    }


    /**
     * Returns the name of this level.
     */
    public String getName()
    {
        return name;
    }


    public String toString()
    {
        return name;
    }


    public boolean equals( Object o )
    {
        if ( this == o )
        {
            return true;
        }

        if ( o instanceof AuthenticationLevel )
        {
            AuthenticationLevel that = ( AuthenticationLevel ) o;
            return this.level == that.level;
        }

        return false;
    }


    public int compareTo( Object o )
    {
        AuthenticationLevel that = ( AuthenticationLevel ) o;
        return this.level - that.level;
    }
}
