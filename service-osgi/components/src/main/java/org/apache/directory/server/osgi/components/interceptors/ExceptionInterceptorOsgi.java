package org.apache.directory.server.osgi.components.interceptors;


import org.apache.directory.server.component.handler.DirectoryInterceptor;
import org.apache.directory.server.core.exception.ExceptionInterceptor;
import org.apache.directory.server.hub.api.component.util.InterceptionPoint;
import org.apache.directory.server.hub.api.component.util.InterceptorOperation;
import org.apache.felix.ipojo.annotations.Component;


@DirectoryInterceptor(interceptionPoint = InterceptionPoint.EXCEPTION, operations =
    {
        InterceptorOperation.ADD,
        InterceptorOperation.DELETE,
        InterceptorOperation.LIST,
        InterceptorOperation.LOOKUP,
        InterceptorOperation.MODIFY,
        InterceptorOperation.MOVE,
        InterceptorOperation.MOVEANDRENAME,
        InterceptorOperation.RENAME })
@Component(name = "ads-interceptor-exception")
public class ExceptionInterceptorOsgi extends ExceptionInterceptor
{

}
