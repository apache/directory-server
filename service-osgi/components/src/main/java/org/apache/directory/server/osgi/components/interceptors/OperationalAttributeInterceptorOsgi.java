package org.apache.directory.server.osgi.components.interceptors;


import org.apache.directory.server.component.handler.DirectoryInterceptor;
import org.apache.directory.server.core.operational.OperationalAttributeInterceptor;
import org.apache.directory.server.hub.api.component.util.InterceptionPoint;
import org.apache.directory.server.hub.api.component.util.InterceptorOperation;
import org.apache.felix.ipojo.annotations.Component;


@DirectoryInterceptor(interceptionPoint = InterceptionPoint.OPERAT, operations =
    {
        InterceptorOperation.ADD,
        InterceptorOperation.LOOKUP,
        InterceptorOperation.LIST,
        InterceptorOperation.MODIFY,
        InterceptorOperation.MOVE,
        InterceptorOperation.MOVEANDRENAME,
        InterceptorOperation.RENAME,
        InterceptorOperation.SEARCH })
@Component(name = "ads-interceptor-operattrib")
public class OperationalAttributeInterceptorOsgi extends OperationalAttributeInterceptor
{

}
