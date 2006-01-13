/*
 *   Copyright 2005 The Apache Software Foundation
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

package org.apache.kerberos.store.operations;

import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;

// this is a jdk 1.5 dep which would make us 1.4 incompatible 
// reverted to using LdapName for now until a better alt is found
// import javax.naming.ldap.LdapName;
import javax.naming.spi.DirStateFactory;
import javax.naming.spi.DirStateFactory.Result;

import org.apache.kerberos.store.PrincipalStoreEntry;
import org.apache.ldap.common.name.LdapName;
import org.apache.protocol.common.store.ContextOperation;

/**
 * Command for adding a principal to a JNDI context.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class AddPrincipal implements ContextOperation
{
    private static final long serialVersionUID = -1032737167622217786L;

    /** The Kerberos principal who is to be added. */
    protected PrincipalStoreEntry entry;

    /**
     * Creates the action to be used against the embedded ApacheDS DIT.
     */
    public AddPrincipal( PrincipalStoreEntry entry )
    {
        this.entry = entry;
    }

    public Object execute( DirContext ctx, Name searchBaseDn )
    {
        if ( entry == null )
        {
            return null;
        }

        try
        {
            DirStateFactory factory = new PrincipalStateFactory();
            Result result = factory.getStateToBind( entry, null, null, null, null );

            Attributes attrs = result.getAttributes();

            LdapName name = new LdapName( "uid=" + entry.getUserId() + ",ou=Users" );

            ctx.rebind( name, null, attrs );

            return name.toString();
        }
        catch ( NamingException ne )
        {
            ne.printStackTrace();
        }

        return null;
    }
}

/*
 dn: uid=akarasulu, ou=Users, dc=example,dc=com
 cn: Alex Karasulu
 sn: Karasulu
 givenname: Alex
 objectclass: top
 objectclass: person
 objectclass: organizationalPerson
 objectclass: inetOrgPerson
 objectclass: krb5Principal
 objectclass: krb5KDCEntry
 ou: Directory
 ou: Users
 uid: akarasulu
 krb5PrincipalName: akarasulu@EXAMPLE.COM
 krb5KeyVersionNumber: 0
 */

