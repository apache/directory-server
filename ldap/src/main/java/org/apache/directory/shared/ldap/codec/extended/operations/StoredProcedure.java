/*
 *   Copyright 2006 The Apache Software Foundation
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

package org.apache.directory.shared.ldap.codec.extended.operations;


import java.util.ArrayList;
import java.util.List;


/**
 * Stored Procedure Extended Operation bean
 * 
 * <pre>
 * StoredProcedure ::= SEQUENCE {
 *    language OCTETSTRING,
 *    procedure OCTETSTRING,
 *    parameters SEQUENCE OF Parameter {
 *       Parameter ::= SEQUENCE OF {
 *          type OCTETSTRING,
 *          value OCTETSTRING
 *       }
 *    }
 * } 
 * </pre>
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class StoredProcedure
{
    private String language;

    private byte[] procedure;

    private ArrayList parameters;

    private transient StoredProcedureParameter currentParameter;


    public String getLanguage()
    {
        return language;
    }


    public void setLanguage( String language )
    {
        this.language = language;
    }


    public byte[] getProcedure()
    {
        return procedure;
    }


    public void setProcedure( byte[] procedure )
    {
        this.procedure = procedure;
    }


    public List getParameters()
    {
        return parameters;
    }


    public void addParameter( StoredProcedureParameter parameter )
    {
        if ( parameters == null )
        {
            parameters = new ArrayList();
        }

        parameters.add( parameter );
    }


    public StoredProcedureParameter getCurrentParameter()
    {
        return currentParameter;
    }


    public void setCurrentParameter( StoredProcedureParameter currentParameter )
    {
        this.currentParameter = currentParameter;
    }

    /**
     * Bean for representing a Stored Procedure Parameter
     */
    public static class StoredProcedureParameter
    {
        private byte[] type;

        private byte[] value;


        public byte[] getType()
        {
            return type;
        }


        public void setType( byte[] type )
        {
            this.type = type;
        }


        public byte[] getValue()
        {
            return value;
        }


        public void setValue( byte[] value )
        {
            this.value = value;
        }
    }

}
