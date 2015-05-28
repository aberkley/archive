//------------------------------------------------------------------------------
// <copyright project="BEmu_maven" file="/BEmu_maven/bemu/src/main/java/com/bloomberglp/blpapi/IntradayBarRequestElementTime.java" company="Jordan Robinson">
//     Copyright (c) 2013 Jordan Robinson. All rights reserved.
//
//     The use of this software is governed by the Microsoft Public License
//     which is included with this distribution.
// </copyright>
//------------------------------------------------------------------------------

package sra_london.bloomberg_emulator;

import sra_london.bloomberg_emulator.Datetime;
import sra_london.bloomberg_emulator.Schema;

public class IntradayBarRequestElementTime extends IntradayBarRequestElementString
{
    private final Datetime _instance;

    IntradayBarRequestElementTime(String elementName)
    {
    	super(elementName, "");
        this._instance = null;
    }

    IntradayBarRequestElementTime(String elementName, Datetime date)
    {
    	super(elementName, date.toString());
        this._instance = date;
    }

    //I can't override GetElementAsDatetime here because the Bloomberg Request object stores dates as strings, not Datetimes.  You can't convert the string to a Datetime
    protected Datetime getDate()
    {
    	return this._instance;
    }

    public Schema.Datatype datatype()
    {
    	return Schema.Datatype.STRING;
    }
}
