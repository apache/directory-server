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
package org.apache.directory.shared.ldap.codec.extended.operations;


import org.apache.directory.shared.asn1.ber.AbstractContainer;
import org.apache.directory.shared.asn1.ber.IAsn1Container;
import org.apache.directory.shared.asn1.ber.grammar.IGrammar;


/**
 * A container for the GracefulDisconnect codec.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class GracefulDisconnectContainer extends AbstractContainer implements IAsn1Container
{
    /** GracefulShutdown */
    private GracefulDisconnect gracefulDisconnect;


    /**
     * Creates a new GracefulDisconnectContainer object. We will store one
     * grammar, it's enough ...
     */
    public GracefulDisconnectContainer()
    {
        super();
        currentGrammar = 0;
        grammars = new IGrammar[GracefulDisconnectStatesEnum.NB_GRAMMARS];
        grammarStack = new IGrammar[1];
        stateStack = new int[1];
        nbGrammars = 0;

        grammars[GracefulDisconnectStatesEnum.GRACEFUL_DISCONNECT_GRAMMAR] = GracefulDisconnectGrammar.getInstance();
        grammarStack[currentGrammar] = grammars[GracefulDisconnectStatesEnum.GRACEFUL_DISCONNECT_GRAMMAR];
        states = GracefulDisconnectStatesEnum.getInstance();
    }


    /**
     * @return Returns the Graceful Shutdown object.
     */
    public GracefulDisconnect getGracefulDisconnect()
    {
        return gracefulDisconnect;
    }


    /**
     * Set a GracefulDisconnect Object into the container. It will be completed
     * by the ldapDecoder.
     * 
     * @param control
     *            the GracefulShutdown to set.
     */
    public void setGracefulDisconnect( GracefulDisconnect gracefulDisconnect )
    {
        this.gracefulDisconnect = gracefulDisconnect;
    }


    /**
     * Clean the container for the next decoding.
     */
    public void clean()
    {
        super.clean();
        gracefulDisconnect = null;
    }
}
