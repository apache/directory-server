package org.apache.ldap.server.jndi.request.processor;

import javax.naming.NamingException;

import org.apache.ldap.server.jndi.request.Request;

public interface NextRequestProcessor {
    void process( Request request ) throws NamingException;
}
