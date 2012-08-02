package org.apache.directory.server.osgi.components.interceptors;


import org.apache.directory.server.component.handler.DirectoryInterceptor;
import org.apache.directory.server.core.authz.DefaultAuthorizationInterceptor;
import org.apache.directory.server.hub.api.component.util.InterceptionPoint;
import org.apache.directory.server.hub.api.component.util.InterceptorOperation;
import org.apache.felix.ipojo.annotations.Component;


@DirectoryInterceptor(interceptionPoint = InterceptionPoint.AUTHZ, operations =
    {
        InterceptorOperation.DELETE,
        InterceptorOperation.LIST,
        InterceptorOperation.LOOKUP,
        InterceptorOperation.MODIFY,
        InterceptorOperation.MOVE,
        InterceptorOperation.MOVEANDRENAME,
        InterceptorOperation.RENAME,
        InterceptorOperation.SEARCH })
@Component(name = "ads-interceptor-authz")
public class AuthorizataionInterceptorOsgi extends DefaultAuthorizationInterceptor
{

}
