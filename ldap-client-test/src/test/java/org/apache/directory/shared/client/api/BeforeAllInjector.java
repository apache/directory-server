package org.apache.directory.shared.client.api;


import org.apache.directory.server.annotations.CreateLdapServer;
import org.apache.directory.server.core.api.DirectoryService;
import org.apache.directory.server.core.factory.DefaultDirectoryServiceFactory;
import org.apache.directory.server.factory.ServerAnnotationProcessor;
import org.apache.directory.server.ldap.LdapServer;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;


public class BeforeAllInjector implements BeforeAllCallback
{
    @Override
    public void beforeAll( ExtensionContext context ) throws Exception
    {
        DefaultDirectoryServiceFactory factory = new DefaultDirectoryServiceFactory();
        factory.init( "test" );
        DirectoryService directoryService = factory.getDirectoryService();

        Class<?> testClass = context.getTestClass().get();

        CreateLdapServer createLdapServer = testClass.getAnnotation( CreateLdapServer.class );
        LdapServer ldapServer = ServerAnnotationProcessor.createLdapServer( createLdapServer, directoryService );

        testClass.getField( "service" ).set( null, directoryService );
        testClass.getField( "ldapServer" ).set( null, ldapServer );
    }
}
