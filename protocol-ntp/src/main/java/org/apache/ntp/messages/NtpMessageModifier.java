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

package org.apache.ntp.messages;

public class NtpMessageModifier
{
    private LeapIndicatorType leapIndicator;
    private int versionNumber;
    private ModeType mode;
    private StratumType stratumType;
    private byte pollInterval;
    private byte precision;
    private int rootDelay;
    private int rootDispersion;

    private ReferenceIdentifier referenceIdentifier;

    private NtpTimeStamp referenceTimestamp;
    private NtpTimeStamp originateTimestamp;
    private NtpTimeStamp receiveTimestamp;
    private NtpTimeStamp transmitTimestamp;

    public NtpMessage getNtpMessage()
    {
        return new NtpMessage( leapIndicator, versionNumber, mode, stratumType, pollInterval, precision, rootDelay,
                rootDispersion, referenceIdentifier, referenceTimestamp, originateTimestamp, receiveTimestamp,
                transmitTimestamp );
    }

    /**
     * @param leapIndicator The Leap Indicator to set.
     */
    public void setLeapIndicator( LeapIndicatorType leapIndicator )
    {
        this.leapIndicator = leapIndicator;
    }

    /**
     * @param mode The Mode to set.
     */
    public void setMode( ModeType mode )
    {
        this.mode = mode;
    }

    /**
     * @param originateTimestamp The Originate Timestamp to set.
     */
    public void setOriginateTimestamp( NtpTimeStamp originateTimestamp )
    {
        this.originateTimestamp = originateTimestamp;
    }

    /**
     * @param pollInterval The Poll Interval to set.
     */
    public void setPollInterval( byte pollInterval )
    {
        this.pollInterval = pollInterval;
    }

    /**
     * @param precision The Precision to set.
     */
    public void setPrecision( byte precision )
    {
        this.precision = precision;
    }

    /**
     * @param receiveTimestamp The Receive Timestamp to set.
     */
    public void setReceiveTimestamp( NtpTimeStamp receiveTimestamp )
    {
        this.receiveTimestamp = receiveTimestamp;
    }

    /**
     * @param referenceIdentifier The Reference Identifier to set.
     */
    public void setReferenceIdentifier( ReferenceIdentifier referenceIdentifier )
    {
        this.referenceIdentifier = referenceIdentifier;
    }

    /**
     * @param referenceTimestamp The Reference Timestamp to set.
     */
    public void setReferenceTimestamp( NtpTimeStamp referenceTimestamp )
    {
        this.referenceTimestamp = referenceTimestamp;
    }

    /**
     * @param rootDelay The Root Delay to set.
     */
    public void setRootDelay( int rootDelay )
    {
        this.rootDelay = rootDelay;
    }

    /**
     * @param rootDispersion The Root Dispersion to set.
     */
    public void setRootDispersion( int rootDispersion )
    {
        this.rootDispersion = rootDispersion;
    }

    /**
     * @param stratumType The Stratum to set.
     */
    public void setStratum( StratumType stratumType )
    {
        this.stratumType = stratumType;
    }

    /**
     * @param transmitTimestamp The Transmit Timestamp to set.
     */
    public void setTransmitTimestamp( NtpTimeStamp transmitTimestamp )
    {
        this.transmitTimestamp = transmitTimestamp;
    }

    /**
     * @param versionNumber The Version Number to set.
     */
    public void setVersionNumber( int versionNumber )
    {
        this.versionNumber = versionNumber;
    }
}
