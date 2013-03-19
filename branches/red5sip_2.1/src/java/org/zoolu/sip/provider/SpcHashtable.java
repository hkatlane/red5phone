/*
 * Copyright (C) 2008 Greg Dorfuss - mhspot.com
 * 
 * This source code is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * This source code is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this source code; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 * 
 * Based on mjsip 1.6 software and skype4java
 * 
 * Author(s):
 * Greg Dorfuss
 */


package org.zoolu.sip.provider;

import java.util.Hashtable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/* 
 * special Hashtable that allows dupelicates of specified keys
 * 
 */

public class SpcHashtable extends Hashtable<Object,Object>
{
    protected static Logger log = LoggerFactory.getLogger(SpcHashtable.class);
	static final long serialVersionUID = 2423442L;
	
	Hashtable<? extends Object,? extends Object> dupeKeysList=null;
	
	SpcHashtable (Hashtable<? extends Object,? extends Object> dupeKeys)
	{
		super();
		if (dupeKeys!=null)
			dupeKeysList=dupeKeys;
		else
			dupeKeysList=new Hashtable<Object,Object>();
	}
	
	
	public synchronized Object put(Object key,Object val)
	{
		if (dupeKeysList.containsKey(key))
		{
			Hashtable vect=null;
			if (this.containsKey(key))
			{
				vect=(Hashtable) super.get(key);
				vect.put(val.hashCode(),val);
			}
			else
			{
				vect=new Hashtable();
				vect.put(val.hashCode(),val);
			}
			return super.put(key, vect);
		}
		else
			return super.put(key, val);
	}
	
	public synchronized Object get(Object key)
	{
		if (dupeKeysList.containsKey(key))
		{
			// gets any entry
			Hashtable vect=(Hashtable) super.get(key);
			if (vect==null || vect.isEmpty())
				return null;
			else
				return vect.get(vect.keys().nextElement());
		}
		else
			return super.get(key);
	
	}

	/*
	 * Gets a specific object
	 * 
	 */
	public synchronized Object get(Object key,Object val)
	{
		if (dupeKeysList.containsKey(key))
		{
			Hashtable vect=(Hashtable) super.get(key);
			if (vect==null)
				return null;
			else
				return vect.get(val.hashCode());
		}
		else
		{
			if (super.get(key)==val)
				return super.get(key);
			else
				return null;
		}	

		
	}

	
	public synchronized Object remove(Object key)
	{
		if (dupeKeysList.containsKey(key))
		{
			// removes a single entry - could be any entry
			Hashtable vect=(Hashtable) super.get(key);
			if (vect==null || vect.isEmpty())
				return null;
			else
			{	
				Object retVal=vect.remove(vect.keys().nextElement());
				if (vect.size()==0)
					super.remove(key);
				return retVal;
			}
		}
		else
			return super.remove(key);
	
	}
	
	public synchronized boolean containsPair(Object key,Object val)
	{
		// checks for  a specific pair
		if (dupeKeysList.containsKey(key))
		{
			Hashtable vect=(Hashtable) super.get(key);
			if (vect!=null && vect.containsKey(val.hashCode()))
				return true;
		}
		else
		{
			if (super.containsKey(key) && super.get(key)==val)
				return true;
		}
		return false;
	}

	public synchronized boolean removePair(Object key,Object val)
	{
		// removes a specific pair
		if (dupeKeysList.containsKey(key))
		{
			Hashtable vect=(Hashtable) super.get(key);
			if (vect!=null && vect.containsKey(val.hashCode()))
			{
				vect.remove(val.hashCode());
				if (vect.size()==0)
					super.remove(key);
				return true;
			}
		}
		else
		{
			if (super.containsKey(key) && super.get(key)==val)
			{	
				super.remove(key);
				return true;
			}
		}
		return false;
	}
	
	public static void main( String [] args) 
	{
		Hashtable<Object,Object> dupeKeys=new Hashtable<Object,Object>();
		
		StringBuffer dupeKey=new StringBuffer("DupeKey");
		StringBuffer regKey=new StringBuffer("RegKey");
		
		dupeKeys.put(dupeKey, "");
		
		SpcHashtable myHash= new SpcHashtable(dupeKeys);

		StringBuffer ps[]=new StringBuffer[5];
		for (int o=0;o<5;o++)
		{	
			ps[o]=new StringBuffer(o+":dssfdfsd");
		}	

		
		for (int o=0;o<ps.length;o++)
		{
			myHash.put(dupeKey, ps[o]);
		}

		myHash.put(regKey, new StringBuffer("stuff"));

		log.debug(myHash.toString());
		
		log.debug("hashsize="+myHash.size());
	
		log.debug("hasdupekey="+myHash.containsKey(dupeKey));
		log.debug("hasregkey="+myHash.containsKey(regKey));
		log.debug("");
		
		StringBuffer gtps=(StringBuffer) myHash.get(dupeKey);
		log.debug("gtps="+gtps);
		
		log.debug("");
		log.debug("getPair3="+myHash.get(dupeKey,ps[3]));
	
		log.debug("removePair3="+myHash.removePair(dupeKey,ps[3]));
		log.debug("getPair3="+myHash.get(dupeKey,ps[3]));
		log.debug("");

		myHash.remove(dupeKey);
		log.debug("removedAPair");
		
		log.debug(myHash.toString());

		
		
		for (int o=0;o<ps.length;o++)
		{
			log.debug("---------------");
			StringBuffer tps=(StringBuffer) myHash.get(dupeKey);
			log.debug("ps="+tps);
			
	
			log.debug("removePair"+o+"="+myHash.removePair(dupeKey,ps[o]));
				
			log.debug("hasdupekey="+myHash.containsKey(dupeKey));
			
		}
		
		log.debug("");
		log.debug(myHash.toString());
		log.debug("hashsize="+myHash.size());
	
		log.debug("getRegKey="+myHash.get(regKey).toString());
		log.debug("hasregkey="+myHash.containsKey(regKey));
		
	}	
	
}



