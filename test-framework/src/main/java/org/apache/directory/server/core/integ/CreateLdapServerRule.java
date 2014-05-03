package org.apache.directory.server.core.integ;


import org.apache.directory.server.annotations.CreateLdapServer;
import org.apache.directory.server.core.api.DirectoryService;
import org.apache.directory.server.factory.ServerAnnotationProcessor;
import org.apache.directory.server.ldap.LdapServer;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class CreateLdapServerRule implements TestRule
{
    private static Logger LOG = LoggerFactory.getLogger( CreateLdapServerRule.class );

    private CreateDsRule createDsRule;
    private LdapServer ldapServer;
    private CreateLdapServerRule outerCreateLdapServerRule;


    public CreateLdapServerRule( CreateDsRule createDsRule )
    {
        this( createDsRule, null );
    }


    public CreateLdapServerRule( CreateDsRule createDsRule, CreateLdapServerRule createLdapServerRule )
    {
        this.createDsRule = createDsRule;
        this.outerCreateLdapServerRule = createLdapServerRule;
    }


    public LdapServer getLdapServer()
    {
        return ldapServer == null
            ? ( outerCreateLdapServerRule == null ? null : outerCreateLdapServerRule.getLdapServer() )
            : ldapServer;
    }


    @Override
    public Statement apply( final Statement base, final Description description )
    {
        final CreateLdapServer createLdapServer = description.getAnnotation( CreateLdapServer.class );
        if ( createLdapServer == null )
        {
            return new Statement() 
            {
                @Override
                public void evaluate() throws Throwable
                {
                    LdapServer ldapServer = getLdapServer();
                    DirectoryService directoryService = createDsRule.getDirectoryService();
                    if ( ldapServer != null && directoryService != ldapServer.getDirectoryService() ) {
                        LOG.trace( "Changing to new directory service" );
                        DirectoryService oldDirectoryService = ldapServer.getDirectoryService();
                        ldapServer.setDirectoryService( directoryService );

                        try
                        {
                            base.evaluate();
                        }
                        finally
                        {
                            LOG.trace( "Reverting to old directory service" );
                            ldapServer.setDirectoryService( oldDirectoryService );
                        }
                   
                    }
                    else {
                        LOG.trace( "no @CreateLdapServer on: {}", description );
                        base.evaluate();
                    }
                }
            };
        }
        else
        {
            return new Statement()
            {
                @Override
                public void evaluate() throws Throwable
                {
                    LOG.trace( "Creating ldap server" );
                    ldapServer = ServerAnnotationProcessor.createLdapServer( description, 
                        createDsRule.getDirectoryService() );

                    try
                    {
                        base.evaluate();
                    }
                    finally
                    {
                        LOG.trace( "Stopping ldap server" );
                        ldapServer.stop();
                    }
                }
            };
        }
    }
}
