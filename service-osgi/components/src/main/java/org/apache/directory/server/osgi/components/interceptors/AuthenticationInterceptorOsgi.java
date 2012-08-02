package org.apache.directory.server.osgi.components.interceptors;


import java.util.Set;

import org.apache.directory.server.component.handler.DirectoryInterceptor;
import org.apache.directory.server.component.handler.DirectoryProperty;
import org.apache.directory.server.core.authn.AuthenticationInterceptor;
import org.apache.directory.server.core.authn.Authenticator;
import org.apache.directory.server.core.authn.ppolicy.PpolicyConfigContainer;
import org.apache.directory.server.hub.api.component.util.InterceptionPoint;
import org.apache.directory.server.hub.api.component.util.InterceptorOperation;
import org.apache.felix.ipojo.annotations.Component;


@DirectoryInterceptor(interceptionPoint = InterceptionPoint.AUTHN, operations =
    {
        InterceptorOperation.ADD,
        InterceptorOperation.BIND,
        InterceptorOperation.COMPARE,
        InterceptorOperation.DELETE,
        InterceptorOperation.GETROOTDSE,
        InterceptorOperation.HASENTRY,
        InterceptorOperation.LIST,
        InterceptorOperation.LOOKUP,
        InterceptorOperation.MODIFY,
        InterceptorOperation.MOVE,
        InterceptorOperation.MOVEANDRENAME,
        InterceptorOperation.RENAME,
        InterceptorOperation.SEARCH,
        InterceptorOperation.UNBIND })
@Component(name = "ads-interceptor-authn")
public class AuthenticationInterceptorOsgi extends AuthenticationInterceptor
{
    @DirectoryProperty(name = "authn-ppolicy", mandatory = true)
    public void setPwdPolicyContainer( PpolicyConfigContainer pwdPolicyContainer )
    {
        super.setPwdPolicyContainer( pwdPolicyContainer );
    }


    @DirectoryProperty(name = "authenticators-set", mandatory = true, containertype = Authenticator.class)
    public void setAuthenticators( Set<Authenticator> authenticators )
    {
        super.setAuthenticators( authenticators );
    }
}
