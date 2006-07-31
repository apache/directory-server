package org.apache.directory.server.core.trigger;

import java.security.Principal;

import javax.naming.InvalidNameException;
import javax.naming.NamingException;

import org.apache.directory.server.core.invocation.Invocation;
import org.apache.directory.server.core.invocation.InvocationStack;
import org.apache.directory.server.core.jndi.ServerContext;
import org.apache.directory.server.core.partition.PartitionNexusProxy;
import org.apache.directory.shared.ldap.name.LdapDN;

public class SimpleTriggerExecutionAuthorizer implements TriggerExecutionAuthorizer
{
    private static LdapDN adminName;
    
    static
    {
        try
        {
            adminName = new LdapDN( PartitionNexusProxy.ADMIN_PRINCIPAL );
        }
        catch ( InvalidNameException e )
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
    public boolean hasPermission() throws NamingException
    {
        Invocation invocation = InvocationStack.getInstance().peek();
        Principal principal = ( ( ServerContext ) invocation.getCaller() ).getPrincipal();
        LdapDN principalName = new LdapDN( principal.getName() );
        
        return principalName.equals( adminName );
    }

}
