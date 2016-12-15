package com.sap.lvm.util;


import java.util.Collection;
import java.util.Map;



@SuppressWarnings("nls")
public class MiscUtil
{
	
	
	public static final String EMPTY_STRING = "";
	

	
	/**
	 * Avoids NPEs.
	 * Works in case s1 or s2 is null.
	 * null is not equal to any not null string and null equals null.
	 * 
	 * @param s1 might be null
	 * @param s2 might be null
	 * 
	 * @return s1.equals.s2 in case both are not null
	 */
	public final static boolean equals(String s1, String s2)
	{
		//$JL-STRING$ this is an optimization, so disable jlin warning
		if(s1==s2)
			return true;
		else if(s1==null || s2==null)
			return false;
		
		return s1.equals(s2);
	}



        /**
	 * (S is not null) AND ( S is not empty)
	 *  
	 *  Empty in the sense of not white space content.
	 *  
	 * @param s 
	 * 
	 * @return (!(s==null)) &and; (!s.isEmpty())
	 */
    public final static boolean notNullAndEmpty(String s)
    {
		if (s==null)
			return false;
		if (s.trim().length()<=0)
			return false;
		return true;
	}	
	
    /**
     * (S is null) OR ( S is empty)
     *  
     *  Empty in the sense of only white space content.
     *  
     * @param s 
     * 
     * @return ((s==null)) &or; (s.isEmpty())
     */
	public final static boolean nullOrEmpty(String s) 
	{
		return !notNullAndEmpty(s);
	}	
	
	public final static boolean notNullAndEmpty(Collection<?> coll)
	{
		return coll!=null && !coll.isEmpty();
	}
	
	public final static boolean nullOrEmpty(Collection<?> coll)
	{
		return coll==null || coll.isEmpty();
	}
	
	public final static boolean notNullAndEmpty(Map<?,?> coll)
	{
		return coll!=null && !coll.isEmpty();
	}
	
	public final static boolean nullOrEmpty(Map<?,?> coll)
	{
		return coll==null || coll.isEmpty();
	}
	
	public final static boolean isSameInteger(String s1, String s2)
	{
		try
		{
			return Integer.parseInt(s1)==Integer.parseInt(s2);
		}
		catch(Exception e)
		{
			// $JL-EXC$ disable exception caught
			return false;
		}
	}
	
}
