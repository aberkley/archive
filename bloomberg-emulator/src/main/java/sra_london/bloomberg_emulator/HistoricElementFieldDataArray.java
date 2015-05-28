//------------------------------------------------------------------------------
// <copyright project="BEmu_maven" file="/BEmu_maven/bemu/src/main/java/com/bloomberglp/blpapi/HistoricElementFieldDataArray.java" company="Jordan Robinson">
//     Copyright (c) 2013 Jordan Robinson. All rights reserved.
//
//     The use of this software is governed by the Microsoft Public License
//     which is included with this distribution.
// </copyright>
//------------------------------------------------------------------------------

package sra_london.bloomberg_emulator;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import sra_london.bloomberg_emulator.Datetime;
import sra_london.bloomberg_emulator.Name;
import sra_london.bloomberg_emulator.Element;
import sra_london.bloomberg_emulator.SchemaTypeDefinition;

public class HistoricElementFieldDataArray extends Element
{
	private final List<HistoricElementFieldData> _fieldData;
	
	HistoricElementFieldDataArray(Map<Datetime, Map<String, Object>> fieldData) throws Exception
	{
		this._fieldData = new ArrayList<HistoricElementFieldData>();
		
		for (Map.Entry<Datetime, Map<String, Object>> item : fieldData.entrySet())
		{
			HistoricElementFieldData elmFieldData = new HistoricElementFieldData(item.getKey(), item.getValue());
			this._fieldData.add(elmFieldData);
		}
	}
    
    public SchemaTypeDefinition typeDefinition() throws Exception
    {
    	return new SchemaTypeDefinition(this.datatype(), new Name("HistoricalDataRow"));
    }
	
	public Element getValueAsElement(int index)
	{
		return this._fieldData.get(index);
	}
	
    public Name name() throws Exception
    {
    	return new Name("fieldData");
    }
    
    public int numValues()
    {
    	return this._fieldData.size();
    }
    
    public int numElements()
    {
    	return 0;
    }
    
    public boolean isComplexType()
    {
    	return true;
    }
    
    public boolean isArray()
    {
    	return true;
    }
    
    public boolean isNull()
    {
    	return false;
    }
    
    protected StringBuilder prettyPrint(int tabIndent) throws Exception
    {
        String tabs = sra_london.bloomberg_emulator.IndentType.Indent(tabIndent);
        StringBuilder result = new StringBuilder();

        result.append(String.format("%s%s[] = {%s", tabs, this.name().toString(), System.getProperty("line.separator")));
        
        for(int i = 0; i < this._fieldData.size(); i++)
        {
        	result.append(this._fieldData.get(i).prettyPrint(tabIndent + 1));
        }

        result.append(String.format("%s}%s", tabs, System.getProperty("line.separator")));
        return result;
    }
}
