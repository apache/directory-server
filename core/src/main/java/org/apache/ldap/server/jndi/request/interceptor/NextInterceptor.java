package org.apache.ldap.server.jndi.request.interceptor;

import javax.naming.NamingException;

import org.apache.ldap.server.jndi.request.Call;

public interface NextInterceptor {
    void process( Call request ) throws NamingException;
}
