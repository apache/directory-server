package org.apache.ldap.server.jndi.invocation.interceptor;


import junit.framework.Assert;
import org.apache.ldap.server.AbstractServerTest;
import org.apache.ldap.server.jndi.EnvKeys;
import org.apache.ldap.server.jndi.invocation.Invocation;

import javax.naming.NamingException;
import java.util.HashMap;
import java.util.Map;


/**
 * Test case for interceptor configurations.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class ConfigurationTest extends AbstractServerTest
{

    private TestInterceptorChain rootChain = new TestInterceptorChain();

    private TestInterceptorChain childChain = new TestInterceptorChain();

    private TestInterceptor interceptorA = new TestInterceptor();

    private TestInterceptor interceptorB = new TestInterceptor();


    protected void setUp() throws Exception
    {
        rootChain.addLast( "A", interceptorA );
        rootChain.addLast( "child", childChain );
        childChain.addBefore( InterceptorChain.NEXT_INTERCEPTOR, "B", interceptorB );
        rootChain.addLast( "default", InterceptorChain.newDefaultChain() );

        extras.put( EnvKeys.INTERCEPTORS, rootChain );
        extras.put( EnvKeys.INTERCEPTORS + "#root", "1" );
        extras.put( EnvKeys.INTERCEPTORS + ".A", "2" );
        extras.put( EnvKeys.INTERCEPTORS + ".A#A", "3" );
        extras.put( EnvKeys.INTERCEPTORS + ".A#A.A", "4" );
        extras.put( EnvKeys.INTERCEPTORS + ".child#child", "5" );
        extras.put( EnvKeys.INTERCEPTORS + ".child.B", "6" );
        extras.put( EnvKeys.INTERCEPTORS + ".child.B#B", "7" );
        extras.put( EnvKeys.INTERCEPTORS + ".child.B#B.B", "8" );

        super.setUp();
    }


    public void testRootChain() throws Exception
    {
        Map expected = new HashMap();
        expected.put( "root", "1" );
        expected.put( "A#A", "3" );
        expected.put( "A#A.A", "4" );
        expected.put( "child#child", "5" );
        expected.put( "child.B#B", "7" );
        expected.put( "child.B#B.B", "8" );
        Assert.assertEquals( expected, rootChain.config );
    }


    public void testChildChain() throws Exception
    {
        Map expected = new HashMap();
        expected.put( "child", "5" );
        expected.put( "B#B", "7" );
        expected.put( "B#B.B", "8" );
        Assert.assertEquals( expected, childChain.config );
    }


    public void testA() throws Exception
    {
        Map expected = new HashMap();
        expected.put( "A", "3" );
        expected.put( "A.A", "4" );
        Assert.assertEquals( expected, interceptorA.config );
    }


    public void testB() throws Exception
    {
        Map expected = new HashMap();
        expected.put( "B", "7" );
        expected.put( "B.B", "8" );
        Assert.assertEquals( expected, interceptorB.config );
    }


    private static class TestInterceptorChain extends InterceptorChain
    {
        private Map config;


        public synchronized void init( InterceptorContext ctx ) throws NamingException
        {
            config = ctx.getConfig();
            super.init( ctx );
        }

    }

    private static class TestInterceptor implements Interceptor
    {
        private Map config;


        public void init( InterceptorContext context ) throws NamingException
        {
            config = context.getConfig();
        }


        public void destroy()
        {
        }


        public void process( NextInterceptor nextInterceptor, Invocation invocation ) throws NamingException
        {
            nextInterceptor.process( invocation );
        }
    }

}
