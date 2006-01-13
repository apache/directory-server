/*
 *   Copyright 2005 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */


package org.apache.ldap.common.util;


import java.util.Collections;
import java.util.LinkedList;
import java.util.List;


public class MandatoryAndOptionalComponentsMonitor implements ComponentsMonitor
{
    private ComponentsMonitor mandatoryComponentsMonitor;
    private ComponentsMonitor optionalComponentsMonitor;
    
    public MandatoryAndOptionalComponentsMonitor( String [] mandatoryComponents,
            String [] optionalComponents ) throws IllegalArgumentException
    {
        // check for common elements
        for ( int i = 0; i < mandatoryComponents.length; i++ )
        {
            for ( int j = 0; j < optionalComponents.length; j++ )
            {
                if ( mandatoryComponents[i].equals( optionalComponents[j] ) )
                {
                    throw new IllegalArgumentException( 
                            "Common element, \"" + mandatoryComponents[i] + 
                            "\" detected for Mandatory and Optional components." );
                }
            }
        }
        
        mandatoryComponentsMonitor = new MandatoryComponentsMonitor( mandatoryComponents );
        optionalComponentsMonitor = new OptionalComponentsMonitor( optionalComponents );
    }

    public ComponentsMonitor useComponent( String component )
    {
        try
        {
            mandatoryComponentsMonitor.useComponent( component );
        }
        catch ( IllegalArgumentException e1 )
        {
            try
            {
                optionalComponentsMonitor.useComponent( component );
            }
            catch ( IllegalArgumentException e2 )
            {
                throw new IllegalArgumentException( 
                        "Unregistered or previously used component: " + component );
            }
        }
        
        return this;
    }

    public boolean allComponentsUsed()
    {
        return ( mandatoryComponentsMonitor.allComponentsUsed() && 
            optionalComponentsMonitor.allComponentsUsed() );
    }

    public boolean finalStateValid()
    {
        return ( mandatoryComponentsMonitor.finalStateValid() && 
                optionalComponentsMonitor.finalStateValid() );
    }

    public List getRemainingComponents()
    {
        List remainingComponents = new LinkedList();
        
        remainingComponents.addAll( mandatoryComponentsMonitor.getRemainingComponents() );
        remainingComponents.addAll( optionalComponentsMonitor.getRemainingComponents() );
        
        return Collections.unmodifiableList( remainingComponents );
    }

}
