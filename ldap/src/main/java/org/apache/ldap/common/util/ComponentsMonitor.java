package org.apache.ldap.common.util;

import java.util.List;

public interface ComponentsMonitor
{
    public ComponentsMonitor useComponent( String component ) throws IllegalArgumentException;
    public boolean allComponentsUsed();
    public boolean finalStateValid();
    public List getRemainingComponents();
    
}
