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
package org.apache.ldap.server.authz.acdf.userclass;

import org.apache.ldap.common.name.LdapName;

public class NameUserClass extends UserClass
{
    private static final long serialVersionUID = -4168412030168359882L;

    private final LdapName userName;
    
    public NameUserClass( LdapName username )
    {
        this.userName = ( LdapName ) username.clone();
    }
    
    public LdapName getUserName()
    {
        return ( LdapName ) userName.clone();
    }

    public boolean equals( Object o )
    {
        if( this == o )
        {
            return true;
        }
        
        if( o instanceof NameUserClass )
        {
            NameUserClass that = ( NameUserClass ) o;
            return this.userName.equals( that.userName );
        }
        
        return false;
    }
    
    public String toString()
    {
        return "name: " + userName;
    }
}
