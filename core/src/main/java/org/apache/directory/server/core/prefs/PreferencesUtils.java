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
package org.apache.directory.server.core.prefs;


import javax.naming.Name;
import javax.naming.NamingException;

import org.apache.directory.shared.ldap.name.LdapName;


/**
 * Document this class.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
class PreferencesUtils
{
    /** the dn base of the system preference hierarchy */
    static final String SYSPREF_BASE = "prefNodeName=sysPrefRoot,ou=system";


    /**
     * Translates an absolute system preferences node name into the distinguished
     * name of the entry corresponding to the preferences node.
     *
     * @param absPrefPath the absolute path to the system preferences node
     * @return the distinguished name of the entry representing the system preferences node
     * @throws NamingException if there are namespace problems while translating the path
     */
    public static Name toSysDn( String absPrefPath ) throws NamingException
    {
        LdapName dn = new LdapName( SYSPREF_BASE );

        String[] comps = absPrefPath.split( "/" );

        for ( int ii = 0; ii < comps.length; ii++ )
        {
            if ( comps[ii] != null && ! comps[ii].trim().equals( "" ) )
            {
                dn.add( "prefNodeName=" + comps[ii] );
            }
        }

        return dn;
    }
}
