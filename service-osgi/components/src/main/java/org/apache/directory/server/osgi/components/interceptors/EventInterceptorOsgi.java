package org.apache.directory.server.osgi.components.interceptors;


import org.apache.directory.server.component.handler.DirectoryInterceptor;
import org.apache.directory.server.core.event.EventInterceptor;
import org.apache.directory.server.hub.api.component.util.InterceptionPoint;
import org.apache.directory.server.hub.api.component.util.InterceptorOperation;
import org.apache.felix.ipojo.annotations.Component;


@DirectoryInterceptor(interceptionPoint = InterceptionPoint.EVENT, operations =
    {
        InterceptorOperation.ADD,
        InterceptorOperation.DELETE,
        InterceptorOperation.MODIFY,
        InterceptorOperation.MOVE,
        InterceptorOperation.MOVEANDRENAME,
        InterceptorOperation.RENAME })
@Component(name = "ads-interceptor-event")
public class EventInterceptorOsgi extends EventInterceptor
{

}
