package org.apache.directory.server.core.trigger;

import javax.naming.Name;

import org.apache.directory.server.core.jndi.ServerLdapContext;


public class Logger
{
    public static void logDelete( ServerLdapContext ctx, Name name )
    {
        System.out.println( "Deleted: " + name );
    }
}
