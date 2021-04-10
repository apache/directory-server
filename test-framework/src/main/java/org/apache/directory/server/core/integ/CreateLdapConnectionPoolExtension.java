package org.apache.directory.server.core.integ;

import org.apache.directory.server.annotations.CreateLdapConnectionPool;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CreateLdapConnectionPoolExtension implements BeforeEachCallback, AfterEachCallback, BeforeAllCallback, AfterAllCallback
{
    private static final Logger LOG = LoggerFactory.getLogger( CreateLdapConnectionPoolExtension.class );
    private CreateLdapConnectionPool createLdapConnectionPool;

    @Override
    public void afterAll( ExtensionContext context ) throws Exception
    {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void beforeAll( ExtensionContext context ) throws Exception
    {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void afterEach( ExtensionContext context ) throws Exception
    {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void beforeEach( ExtensionContext context ) throws Exception
    {
        // TODO Auto-generated method stub
        
    }

    /*
    private Statement buildStatement( Statement base, final AnnotatedElement annotations )
    {
        createLdapConnectionPool = annotations.getAnnotation( CreateLdapConnectionPool.class );
        
        if ( createLdapConnectionPool == null )
        {
            return new Statement()
            {
                @Override
                public void evaluate() throws Throwable
                {
                    LdapServer ldapServer = getLdapServer();
                    
                    if ( classCreateLdapConnectionPoolRule != null
                        && classCreateLdapConnectionPoolRule.getLdapServer() != ldapServer )
                    {
                        LOG.trace( "Creating connection pool to new ldap server" );

                        LdapConnectionPool oldLdapConnectionPool = ldapConnectionPool;
                        LdapConnectionTemplate oldLdapConnectionTemplate = ldapConnectionTemplate;

                        Class<? extends PooledObjectFactory<LdapConnection>> factoryClass =
                                classCreateLdapConnectionPoolRule.createLdapConnectionPool.factoryClass();
                        Class<? extends LdapConnectionFactory> connectionFactoryClass =
                                classCreateLdapConnectionPoolRule.createLdapConnectionPool.connectionFactoryClass();
                        Class<? extends LdapConnectionValidator> validatorClass =
                                classCreateLdapConnectionPoolRule.createLdapConnectionPool.validatorClass();
                        ldapConnectionPool = classCreateLdapConnectionPoolRule
                                .createLdapConnectionPool( ldapServer, factoryClass, 
                                    connectionFactoryClass, validatorClass );
                        ldapConnectionTemplate = new LdapConnectionTemplate( ldapConnectionPool );

                        try
                        {
                            base.evaluate();
                        }
                        finally
                        {
                            LOG.trace( "Reverting to old connection pool" );
                            ldapConnectionPool = oldLdapConnectionPool;
                            ldapConnectionTemplate = oldLdapConnectionTemplate;
                        }
                    }
                    else
                    {
                        LOG.trace( "no @CreateLdapConnectionPool on: {}", description );
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
                    LOG.trace( "Creating ldap connection pool" );
                    Class<? extends PooledObjectFactory<LdapConnection>> factoryClass =
                            createLdapConnectionPool.factoryClass();
                    Class<? extends LdapConnectionFactory> connectionFactoryClass =
                            createLdapConnectionPool.connectionFactoryClass();
                    Class<? extends LdapConnectionValidator> validatorClass =
                            createLdapConnectionPool.validatorClass();
                    ldapConnectionPool = createLdapConnectionPool( getLdapServer(), factoryClass, 
                            connectionFactoryClass, validatorClass );
                    ldapConnectionTemplate = new LdapConnectionTemplate( ldapConnectionPool );

                    try
                    {
                        base.evaluate();
                    }
                    finally
                    {
                        LOG.trace( "Closing ldap connection pool" );
                        ldapConnectionPool.close();
                        ldapConnectionTemplate = null;
                    }
                }
            };
        }
    }
    */
}
