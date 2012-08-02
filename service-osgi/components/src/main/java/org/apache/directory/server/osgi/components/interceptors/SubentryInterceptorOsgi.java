package org.apache.directory.server.osgi.components.interceptors;


import org.apache.directory.server.component.handler.DirectoryInterceptor;
import org.apache.directory.server.core.subtree.SubentryInterceptor;
import org.apache.directory.server.hub.api.component.util.InterceptionPoint;
import org.apache.directory.server.hub.api.component.util.InterceptorOperation;
import org.apache.felix.ipojo.annotations.Component;


@DirectoryInterceptor(interceptionPoint = InterceptionPoint.SUBENTRY, operations =
    {
        InterceptorOperation.ADD,
        InterceptorOperation.DELETE,
        InterceptorOperation.LIST,
        InterceptorOperation.MODIFY,
        InterceptorOperation.SEARCH,
        InterceptorOperation.MOVE,
        InterceptorOperation.MOVEANDRENAME,
        InterceptorOperation.RENAME })
@Component(name = "ads-interceptor-subentry")
public class SubentryInterceptorOsgi extends SubentryInterceptor
{

}
