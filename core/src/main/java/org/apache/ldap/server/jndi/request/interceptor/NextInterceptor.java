package org.apache.ldap.server.jndi.request.interceptor;

import javax.naming.NamingException;

import org.apache.ldap.server.jndi.request.Request;

public interface NextInterceptor {
    void process( Request request ) throws NamingException;
}
