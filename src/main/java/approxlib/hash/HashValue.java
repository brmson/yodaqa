// Copyright (c) 2010, Nikolaus Augsten. All rights reserved.
// This software is released under the 2-clause BSD license.

/*
 * Created on Jun 22, 2005
 */
package approxlib.hash;

/**
 * Simple class that stores a hash value.
 * 
 * @author augsten
 */
public class HashValue implements Comparable {

	
	/**
	 * Mask used to retrieve the value of the least significant byte of an integer number.  
	 */
	public final int LSByteMask = 255;
		
	private String hashvalue;
	
	/**
	 * Initialize the hash value with a string.
	 * 
	 * @param hashvalue hash value as a string
	 */
	public HashValue(String hashvalue) {
		this.hashvalue = hashvalue;
	}

	/**
	 * Initialize the hash value with an integer number.
	 * 
	 * @param h hash value as an integer 
	 * @param length length of the hash value in bytes
	 */
	public HashValue(long h, int length) {
		StringBuffer s = new StringBuffer(length);
		for (int i = 0; i < length; i++) {
			char c = (char)(h & LSByteMask);
			s.append(c);
			h = h >>> 8;
		}
		hashvalue = s.toString();
		
	}
	
	/**
	 * @return hash value as a string
	 */
	@Override
	public String toString() {
		if (hashvalue.charAt(hashvalue.length() -1) == ' ') {
			return hashvalue.substring(0, hashvalue.length() - 1) + (char)0;
		} else {
			return hashvalue;
		}
	}

	/* (non-Javadoc)
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	public int compareTo(Object arg0) {
		return this.hashvalue.compareTo(((HashValue)arg0).toString());
	}
	
	public static HashValue maxValue(int length) {
		String s = "";
		for (int i = 0; i < length; i++) {
			s += Character.MAX_VALUE; 
		}
		return new HashValue(s);
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object arg0) {
		return (this.toString()).equals(((HashValue)arg0).toString());
	}
	
	
	
}
