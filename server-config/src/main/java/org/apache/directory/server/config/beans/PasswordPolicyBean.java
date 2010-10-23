/*
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 *
 */

package org.apache.directory.server.config.beans;


import org.apache.directory.shared.ldap.constants.SchemaConstants;


/**
 * A simple pojo holding the password policy configuration base on 
 * <a href="http://tools.ietf.org/html/draft-behera-ldap-password-policy-10">this draft</a>.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class PasswordPolicyBean extends AdsBaseBean
{
    /**
     * The PasswordPolicy unique identifier
     */
    private String pwdid;
    
    /** the name of the attribute to which the password policy is applied. 
     * Currently only "userPassword" attribute is supported
     */
    private String pwdattribute = SchemaConstants.USER_PASSWORD_AT;

    /** 
     * holds the number of seconds that must elapse between modifications to the password. 
     * Default value is 0 
     */
    private int pwdminage = 0;

    /**
     *  holds the number of seconds after which a modified password will expire.
     *  Default value is 0, does not expire.  If not 0, the value must be greater than or equal
     *  to the value of the pwdMinAge.
     */
    private int pwdmaxage;

    /**
     *  specifies the maximum number of used passwords stored in the pwdHistory attribute.
     *  Default value is 0, no password history maintained
     */
    private int pwdinhistory = 0;

    /** indicates how the password quality will be verified while being modified or added.
     *  Default value 0, do not check 
     */
    private int pwdcheckquality = 0;

    /** this attribute holds the minimum number of characters that must be used in a password. 
     *  Default value 0, no minimum length enforced
     */
    private int pwdminlength = 0;

    /**
     * this attribute holds the maximum number of characters that may be used in a password.
     * Default value 0, no maximum length enforced
     */
    private int pwdmaxlength = 0;

    /**
     * the maximum number of seconds before a password is due to expire that expiration warning
     * messages will be returned to an authenticating user.
     * Default value is 0, never send a warning message.
     */
    private int pwdexpirewarning = 0;

    /** 
     * the number of times an expired password can be used to authenticate.
     * Default value is 0, do not allow a expired password for authentication.
     */
    private int pwdgraceauthnlimit = 0;

    /** 
     * specifies the number of seconds the grace authentications are valid
     * Default value is 0, no limit.
     */
    private int pwdgraceexpire = 0;

    /**
     * flag to indicate if the account needs to be locked after a specified number of
     * consecutive failed bind attempts. The maximum number of consecutive
     * failed bind attempts is specified in {@link #pwdmaxfailure}
     */
    private boolean pwdlockout;

    /**
     * the number of seconds that the password cannot be used to authenticate due to 
     * too many failed bind attempts.
     * Default value is 300 seconds.
     */
    private int pwdlockoutduration = 300;

    /**
     * the number of consecutive failed bind attempts after which the password may not 
     * be used to authenticate.
     * Default value is 0, no limit on the number of authentication failures
     */
    private int pwdmaxfailure;

    /**
     * the number of seconds after which the password failures are purged from the failure counter.
     * Default value is 0, reset all pwdFailureTimes after a successful authentication.
     */
    private int pwdfailurecountinterval;

    /** 
     * flag to indicate if the password must be changed by the user after they bind to the 
     * directory after a password is set or reset by a password administrator.
     * Default value is false, no need to change the password by user.
     */
    private boolean pwdmustchange = false;

    /** indicates whether users can change their own passwords. Default value is true, allow change */
    private boolean pwdallowuserchange = true;

    /**
     *  flag to specify whether or not the existing password must be sent along with the
     *  new password when being changed.
     *  Default value is false.
     */
    private boolean pwdsafemodify = false;

    /** 
     * the number of seconds to delay responding to the first failed authentication attempt
     * Default value 0, no delay.
     */
    private int pwdmindelay = 0;

    /** the maximum number of seconds to delay when responding to a failed authentication attempt.*/
    private int pwdmaxdelay;

    /** 
     * the number of seconds an account may remain unused before it becomes locked
     * Default value is 0, no check for idle time.
     */
    private int pwdmaxidle;

    public String getPwdAttribute()
    {
        return pwdattribute;
    }


    public void setPwdAttribute( String pwdAttribute )
    {
        this.pwdattribute = pwdAttribute;
    }


    public int getPwdMinAge()
    {
        return pwdminage;
    }


    public void setPwdMinAge( int pwdMinAge )
    {
        this.pwdminage = pwdMinAge;
    }


    public int getPwdMaxAge()
    {
        return pwdmaxage;
    }


    public void setPwdMaxAge( int pwdMaxAge )
    {
        this.pwdmaxage = pwdMaxAge;
    }


    public int getPwdInHistory()
    {
        return pwdinhistory;
    }


    public void setPwdInHistory( int pwdInHistory )
    {
        this.pwdinhistory = pwdInHistory;
    }


    public int getPwdCheckQuality()
    {
        return pwdcheckquality;
    }


    public void setPwdCheckQuality( int pwdCheckQuality )
    {
        this.pwdcheckquality = pwdCheckQuality;
    }


    public int getPwdMinLength()
    {
        return pwdminlength;
    }


    public void setPwdMinLength( int pwdMinLength )
    {
        this.pwdminlength = pwdMinLength;
    }


    public int getPwdMaxLength()
    {
        return pwdmaxlength;
    }


    public void setPwdMaxLength( int pwdMaxLength )
    {
        this.pwdmaxlength = pwdMaxLength;
    }


    public int getPwdExpireWarning()
    {
        return pwdexpirewarning;
    }


    public void setPwdExpireWarning( int pwdExpireWarning )
    {
        this.pwdexpirewarning = pwdExpireWarning;
    }


    public int getPwdGraceAuthNLimit()
    {
        return pwdgraceauthnlimit;
    }


    public void setPwdGraceAuthNLimit( int pwdGraceAuthNLimit )
    {
        this.pwdgraceauthnlimit = pwdGraceAuthNLimit;
    }


    public int getPwdGraceExpire()
    {
        return pwdgraceexpire;
    }


    public void setPwdGraceExpire( int pwdGraceExpire )
    {
        this.pwdgraceexpire = pwdGraceExpire;
    }


    public boolean isPwdLockout()
    {
        return pwdlockout;
    }


    public void setPwdLockout( boolean pwdLockout )
    {
        this.pwdlockout = pwdLockout;
    }


    public int getPwdLockoutDuration()
    {
        return pwdlockoutduration;
    }


    public void setPwdLockoutDuration( int pwdLockoutDuration )
    {
        this.pwdlockoutduration = pwdLockoutDuration;
    }


    public int getPwdMaxFailure()
    {
        return pwdmaxfailure;
    }


    public void setPwdMaxFailure( int pwdMaxFailure )
    {
        this.pwdmaxfailure = pwdMaxFailure;
    }


    public int getPwdFailureCountInterval()
    {
        return pwdfailurecountinterval;
    }


    public void setPwdFailureCountInterval( int pwdFailureCountInterval )
    {
        this.pwdfailurecountinterval = pwdFailureCountInterval;
    }


    public boolean isPwdMustChange()
    {
        return pwdmustchange;
    }


    public void setPwdMustChange( boolean pwdMustChange )
    {
        this.pwdmustchange = pwdMustChange;
    }


    public boolean isPwdAllowUserChange()
    {
        return pwdallowuserchange;
    }


    public void setPwdAllowUserChange( boolean pwdAllowUserChange )
    {
        this.pwdallowuserchange = pwdAllowUserChange;
    }


    public boolean isPwdSafeModify()
    {
        return pwdsafemodify;
    }


    public void setPwdSafeModify( boolean pwdSafeModify )
    {
        this.pwdsafemodify = pwdSafeModify;
    }


    public int getPwdMinDelay()
    {
        return pwdmindelay;
    }


    public void setPwdMinDelay( int pwdMinDelay )
    {
        this.pwdmindelay = pwdMinDelay;
    }


    public int getPwdMaxDelay()
    {
        return pwdmaxdelay;
    }


    public void setPwdMaxDelay( int pwdMaxDelay )
    {
        this.pwdmaxdelay = pwdMaxDelay;
    }


    public int getPwdMaxIdle()
    {
        return pwdmaxidle;
    }


    public void setPwdMaxIdle( int pwdMaxIdle )
    {
        this.pwdmaxidle = pwdMaxIdle;
    }


    /**
     * @return the pwdId
     */
    public String getPwdId()
    {
        return pwdid;
    }


    /**
     * @param pwdId the pwdId to set
     */
    public void setPwdId( String pwdId )
    {
        this.pwdid = pwdId;
    }


    /**
     * {@inheritDoc}
     */
    public String toString( String tabs )
    {
        StringBuilder sb = new StringBuilder();
        
        sb.append( tabs ).append( "PasswordPolicy :\n" );
        sb.append( super.toString( tabs + "  " ) );
        sb.append( tabs ).append( "  identifier : " ).append( pwdid ).append( '\n' );
        sb.append( toString( tabs, "  password attribute", pwdattribute ) );
        sb.append( tabs ).append( "  password min age : " ).append( pwdminage ).append( '\n' );
        sb.append( tabs ).append( "  password max age : " ).append( pwdmaxage ).append( '\n' );
        sb.append( tabs ).append( "  password min length : " ).append( pwdminlength ).append( '\n' );
        sb.append( tabs ).append( "  password max length : " ).append( pwdmaxlength ).append( '\n' );
        sb.append( tabs ).append( "  password min delay : " ).append( pwdmindelay ).append( '\n' );
        sb.append( tabs ).append( "  password max delay : " ).append( pwdmaxdelay ).append( '\n' );
        sb.append( tabs ).append( "  password max idle : " ).append( pwdmaxidle ).append( '\n' );
        sb.append( tabs ).append( "  password max failure : " ).append( pwdmaxfailure ).append( '\n' );
        sb.append( tabs ).append( "  password lockout duration : " ).append( pwdlockoutduration ).append( '\n' );
        sb.append( tabs ).append( "  password expire warning : " ).append( pwdexpirewarning ).append( '\n' );
        sb.append( tabs ).append( "  password grace expire : " ).append( pwdgraceexpire ).append( '\n' );
        sb.append( tabs ).append( "  password grace Auth N limit : " ).append( pwdgraceauthnlimit ).append( '\n' );
        sb.append( tabs ).append( "  password in history : " ).append( pwdinhistory ).append( '\n' );
        sb.append( tabs ).append( "  password check quality : " ).append( pwdcheckquality ).append( '\n' );
        sb.append( tabs ).append( "  password failure count interval : " ).append( pwdfailurecountinterval ).append( '\n' );
        sb.append( toString( tabs, "  password lockout", pwdlockout ) );
        sb.append( toString( tabs, "  password must change", pwdmustchange ) );
        sb.append( toString( tabs, "  password allow user change", pwdallowuserchange ) );
        sb.append( toString( tabs, "  password safe modify", pwdsafemodify ) );

        return sb.toString();
    }
    
    
    /**
     * {@inheritDoc}
     */
    public String toString()
    {
        return toString( "" );
    }
}
