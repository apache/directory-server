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
package org.apache.eve.auth;


import javax.naming.Name;
import java.security.Principal;

import org.apache.ldap.common.name.LdapName;


/**
 * An alternative X500 user implementation that has access to the distinguished
 * name of the principal as well as the String representation.
 *
 * @author <a href="mailto:directory-dev@incubator.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class LdapPrincipal implements Principal
{
    /** the normalized distinguished name of the principal */
    private final Name name;
    /** the no name anonymous user whose DN is the empty String */
    public static final LdapPrincipal ANONYMOUS = new LdapPrincipal();


    /**
     * Creates a new LDAP/X500 principal.
     *
     * @param name the normalized distinguished name of the principal
     */
    public LdapPrincipal( Name name )
    {
        this.name = name;
    }


    /**
     * Creates a principal for the no name anonymous user whose DN is the empty
     * String.
     */
    private LdapPrincipal()
    {
        this.name = new LdapName();
    }



    /**
     * Gets a cloned copy of the normalized distinguished name of this
     * principal as a JNDI Name.  It must be cloned to protect this Principal
     * from alteration.
     *
     * @return the normalized distinguished name of the principal as a JNDI Name
     */
    public Name getDn()
    {
        return ( Name ) name.clone();
    }


    /**
     * Gets the normalized distinguished name of the principal as a String.
     *
     * @see Principal#getName()
     * @return
     */
    public String getName()
    {
        return name.toString();
    }
}
