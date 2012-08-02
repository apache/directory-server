package org.apache.directory.server.osgi.components.interceptors;



import org.apache.directory.server.component.handler.DirectoryInterceptor;
import org.apache.directory.server.core.authz.AciAuthorizationInterceptor;
import org.apache.directory.server.hub.api.component.util.InterceptionPoint;
import org.apache.directory.server.hub.api.component.util.InterceptorOperation;
import org.apache.felix.ipojo.annotations.Component;


@DirectoryInterceptor(interceptionPoint = InterceptionPoint.ACI, operations =
    {
        InterceptorOperation.ADD,
        InterceptorOperation.COMPARE,
        InterceptorOperation.DELETE,
        InterceptorOperation.HASENTRY,
        InterceptorOperation.LIST,
        InterceptorOperation.LOOKUP,
        InterceptorOperation.MODIFY,
        InterceptorOperation.MOVE,
        InterceptorOperation.MOVEANDRENAME,
        InterceptorOperation.RENAME,
        InterceptorOperation.SEARCH })
@Component(name = "ads-interceptor-aciauthorization")
public class AciAuthorizationInterceptorOsgi extends AciAuthorizationInterceptor
{

}
