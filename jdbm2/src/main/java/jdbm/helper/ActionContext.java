/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *  
 *    http://www.apache.org/licenses/LICENSE-2.0
 *  
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License. 
 *  
 */
package jdbm.helper;

/**
 * Used to store Action specific context.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class ActionContext
{
    /** track whether action is read only */ 
    boolean readOnly;
    
    /** Version associated with the context */
    ActionVersioning.Version version;
    
    /** Who started the action. Usefule for debugging */
    String whoStarted;
    
    public void beginAction( boolean readOnly, ActionVersioning.Version version, String whoStarted )
    {
        this.readOnly = readOnly;
        this.version = version;
        this.whoStarted = whoStarted;
    }
    
    
    public void endAction()
    {
        if ( version == null )
        {
            throw new IllegalStateException( "Unexpected action state during endAction: " + this );
        }
        
        version = null;
    }
    
    
    public boolean isReadOnlyAction()
    {
        return ( readOnly && ( version != null ) );
    }
    
    
    public boolean isWriteAction()
    {
        return ( !readOnly && ( version != null ) );
    }
    
    
    public boolean isActive()
    {
        return ( version != null );
    }
    
    
    public ActionVersioning.Version getVersion()
    {
        return version;
    }
    
    
    public String getWhoStarted()
    {
        return whoStarted;
    }
    
    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append( "ActionContext: " );
        sb.append( "(readOnly: " ).append( readOnly );
        sb.append( ", version: " ).append( version );
        sb.append( ", whoStarted: " ).append( whoStarted );
        sb.append( ")\n" );
        
        return sb.toString();
    }
}
