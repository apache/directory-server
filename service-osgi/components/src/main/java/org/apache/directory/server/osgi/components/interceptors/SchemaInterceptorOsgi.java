package org.apache.directory.server.osgi.components.interceptors;


import org.apache.directory.server.component.handler.DirectoryInterceptor;
import org.apache.directory.server.core.schema.SchemaInterceptor;
import org.apache.directory.server.hub.api.component.util.InterceptionPoint;
import org.apache.directory.server.hub.api.component.util.InterceptorOperation;
import org.apache.felix.ipojo.annotations.Component;


@DirectoryInterceptor(interceptionPoint = InterceptionPoint.SCHEMA, operations =
    {
        InterceptorOperation.ADD,
        InterceptorOperation.COMPARE,
        InterceptorOperation.LIST,
        InterceptorOperation.LOOKUP,
        InterceptorOperation.MODIFY,
        InterceptorOperation.RENAME,
        InterceptorOperation.SEARCH })
@Component(name = "ads-interceptor-schema")
public class SchemaInterceptorOsgi extends SchemaInterceptor
{

}
