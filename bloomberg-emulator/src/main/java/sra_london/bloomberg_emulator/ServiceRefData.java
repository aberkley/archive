//------------------------------------------------------------------------------
// <copyright project="BEmu_maven" file="/BEmu_maven/bemu/src/main/java/com/bloomberglp/blpapi/ServiceRefData.java" company="Jordan Robinson">
//     Copyright (c) 2013 Jordan Robinson. All rights reserved.
//
//     The use of this software is governed by the Microsoft Public License
//     which is included with this distribution.
// </copyright>
//------------------------------------------------------------------------------

package sra_london.bloomberg_emulator;

public class ServiceRefData extends Service
{
	public String name() { return "//blp/refdata"; }
	
	public Request createRequest(String operationName) throws Exception
	{
		if(operationName.equals("HistoricalDataRequest"))
			return new sra_london.bloomberg_emulator.HistoricRequest();
		
		else if(operationName.equals("ReferenceDataRequest"))
			return new sra_london.bloomberg_emulator.ReferenceRequest();
		
		else if(operationName.equals("IntradayBarRequest"))
			return new sra_london.bloomberg_emulator.IntradayBarRequest(this);
		
		else if(operationName.equals("IntradayTickRequest"))
			return new sra_london.bloomberg_emulator.IntradayTickRequest(this);
		
		else
			throw new Exception("not implemented. names are case-sensitive.");
	}
}
