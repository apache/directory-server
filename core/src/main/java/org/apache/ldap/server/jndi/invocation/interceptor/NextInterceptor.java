package org.apache.ldap.server.jndi.invocation.interceptor;

import javax.naming.NamingException;

import org.apache.ldap.server.jndi.invocation.Invocation;

public interface NextInterceptor {
    void process( Invocation call ) throws NamingException;
}
