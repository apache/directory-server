package org.apache.directory.server.osgi.components.interceptors;


import org.apache.directory.server.component.handler.DirectoryInterceptor;
import org.apache.directory.server.core.kerberos.KeyDerivationInterceptor;
import org.apache.directory.server.hub.api.component.util.InterceptionPoint;
import org.apache.directory.server.hub.api.component.util.InterceptorOperation;
import org.apache.felix.ipojo.annotations.Component;


@DirectoryInterceptor(interceptionPoint = InterceptionPoint.KEYDRV, operations =
    { InterceptorOperation.ADD, InterceptorOperation.MODIFY })
@Component(name = "ads-interceptor-keydrv")
public class KeyDerivationInterceptorOsgi extends KeyDerivationInterceptor
{

}
