package org.apache.directory.server.osgi.components.interceptors;


import org.apache.directory.server.component.handler.DirectoryInterceptor;
import org.apache.directory.server.component.handler.DirectoryProperty;
import org.apache.directory.server.core.hash.PasswordHashingInterceptor;
import org.apache.directory.server.hub.api.component.util.InterceptionPoint;
import org.apache.directory.server.hub.api.component.util.InterceptorOperation;
import org.apache.directory.shared.ldap.model.constants.LdapSecurityConstants;
import org.apache.felix.ipojo.annotations.Component;


@DirectoryInterceptor(interceptionPoint = InterceptionPoint.PASSHASH, operations =
    { InterceptorOperation.ADD, InterceptorOperation.MODIFY })
@Component(name = "ads-interceptor-passwordhashing")
public class PasswordHashingInterceptorOsgi extends PasswordHashingInterceptor
{

    protected PasswordHashingInterceptorOsgi( String name, LdapSecurityConstants algorithm )
    {
        super( name, algorithm );
    }


    public PasswordHashingInterceptorOsgi( @DirectoryProperty(name = "ads-hashing-algorithm") String algorithm )
    {
        this( "PasswordHashingInterceptorOsgi", LdapSecurityConstants.valueOf( algorithm ) );
    }

}
