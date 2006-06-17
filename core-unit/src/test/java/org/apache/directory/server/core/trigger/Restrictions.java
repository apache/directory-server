package org.apache.directory.server.core.trigger;

import javax.naming.Name;

import org.apache.directory.server.core.jndi.ServerLdapContext;


public class Restrictions
{
    public static void noDelete( ServerLdapContext ctx, Name name )
    {
        System.out.println( "Delete restriction on: " + name );
    }


    public static void noAdd( ServerLdapContext ctx, Name name )
    {
        System.out.println( "Add restriction on: " + name );
    }
}
