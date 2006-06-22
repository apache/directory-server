package org.apache.directory.server.core.trigger;

import javax.naming.NamingException;

public interface TriggerExecutionAuthorizer
{
    boolean hasPermission() throws NamingException;
}
