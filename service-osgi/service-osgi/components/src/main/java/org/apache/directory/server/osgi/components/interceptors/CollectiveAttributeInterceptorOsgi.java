package org.apache.directory.server.osgi.components.interceptors;


import org.apache.directory.server.component.handler.DirectoryInterceptor;
import org.apache.directory.server.core.collective.CollectiveAttributeInterceptor;
import org.apache.directory.server.hub.api.component.util.InterceptionPoint;
import org.apache.directory.server.hub.api.component.util.InterceptorOperation;
import org.apache.felix.ipojo.annotations.Component;


@DirectoryInterceptor(interceptionPoint = InterceptionPoint.COLLAT, operations =
    {
        InterceptorOperation.ADD,
        InterceptorOperation.LIST,
        InterceptorOperation.LOOKUP,
        InterceptorOperation.MODIFY,
        InterceptorOperation.SEARCH })
@Component(name = "ads-interceptor-collattrib")
public class CollectiveAttributeInterceptorOsgi extends CollectiveAttributeInterceptor
{

}
