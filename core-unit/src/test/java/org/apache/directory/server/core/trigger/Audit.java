package org.apache.directory.server.core.trigger;

import javax.naming.Name;
import javax.naming.directory.Attributes;

import org.apache.directory.server.core.jndi.ServerLdapContext;


public class Audit
{
    public static void userDeletedAnEntry( ServerLdapContext ctx, Attributes entry, Name name )
    {
        System.out.println( "Auditing deletion of entry " + name + " by user " + ctx.getPrincipal().getName() );
    }
}
