package org.apache.directory.server.core.trigger;

import javax.naming.Name;

import org.apache.directory.server.core.jndi.ServerLdapContext;


public class BackupUtilities
{
    public static void backupDeleted( ServerLdapContext ctx, Name name )
    {
        System.out.println( "Backing up: " + name );
    }
}
