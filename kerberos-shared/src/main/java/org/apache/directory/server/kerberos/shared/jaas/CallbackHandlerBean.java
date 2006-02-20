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
package org.apache.directory.server.kerberos.shared.jaas;


import java.io.IOException;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;


public class CallbackHandlerBean implements CallbackHandler
{
    private String name;
    private String password;


    public CallbackHandlerBean(String name, String password)
    {
        this.name = name;
        this.password = password;
    }


    public void handle( Callback[] callbacks ) throws UnsupportedCallbackException, IOException
    {
        for ( int ii = 0; ii < callbacks.length; ii++ )
        {
            Callback callBack = callbacks[ii];

            // Handles username callback.
            if ( callBack instanceof NameCallback )
            {
                NameCallback nameCallback = ( NameCallback ) callBack;
                nameCallback.setName( name );
                // Handles password callback.
            }
            else if ( callBack instanceof PasswordCallback )
            {
                PasswordCallback passwordCallback = ( PasswordCallback ) callBack;
                passwordCallback.setPassword( password.toCharArray() );
            }
            else
            {
                throw new UnsupportedCallbackException( callBack, "Callback not supported" );
            }
        }
    }
}
