// Copyright (c) 2010, Nikolaus Augsten. All rights reserved.
// This software is released under the 2-clause BSD license.

package approxlib.util;
import java.util.LinkedList;
import java.util.Vector;
import java.util.Random;
import java.util.Date;

public class FormatUtilities {
	
	/**
	 * Parses line and returns contents of field number fieldNr, where fields
	 * are separated by seperator.
	 * 
	 * @param fieldNr number of field you want to get; numbering starts with 0
	 * @param line string to parse
	 * @param seperator field seperator
	 * @return if the field exists, the value of the field without 
	 *         leading and tailing spaces is returned; if the field does not
	 *         exist or the parameter line is null than null is returned
	 */
	public static String getField(int fieldNr, String line, 
			char seperator) {
		if (line != null) {
			int pos = 0;
			for (int i = 0; i < fieldNr; i++) {
				pos = line.indexOf(seperator, pos);
				if (pos == -1) {
					return null;
				}
				pos = pos + 1;
			}
			int pos2 = line.indexOf(seperator, pos);
			String res;
			if (pos2 == -1) {
				res = line.substring(pos);
			} else {
				res = line.substring(pos, pos2);
			}
			return res.trim();
		} else {
			return null;
		}
	}
	
	/**
	 * 
	 * @param line
	 * @param separator
	 * @return new String[0] for empty or null lines, string-array containing fields, otherwise
	 */
	@SuppressWarnings("unchecked")
	public static String[] getFields(String line, 
			char separator) {
		if ((line != null) && (!line.equals(""))) {
			StringBuffer field = new StringBuffer();
			LinkedList fieldArr = new LinkedList();
			for (int i = 0; i < line.length(); i++) {
				char ch = line.charAt(i);
				if (ch == separator) {
					fieldArr.add(field.toString().trim());
					field = new StringBuffer();
				} else {
					field.append(ch);
				}
			}
			fieldArr.add(field.toString().trim());
			return (String[])fieldArr.toArray(new String[fieldArr.size()]);
		} else {
			return new String[0];
		}
	}
	
	public static String[] getFields(String line, char separator, char quote) {
		String [] parse = getFields(line, separator);
		for (int i = 0; i < parse.length; i++) {
			parse[i] = stripQuotes(parse[i], quote);
		}
		return (parse);
	}
	
	public static String stripQuotes(String s, char quote) {
		if ((s.length() >= 2) && (s.charAt(0) == quote) && (s.charAt(s.length() - 1) == quote)) {
			return s.substring(1, s.length() - 1);
		} else {
			return s;
		}
	}
	
	public static String resizeEnd(String s, int size) {
		return resizeEnd(s, size, ' ');
	}
	
	public static String getRandomString(int length) {
		Date d = new Date();
		Random r = new Random(d.getTime());
		String str = "";
		for (int i = 0; i < length; i++) {
			str += (char)(65 + r.nextInt(26));
		}
		return str;
	}
	
	public static String resizeEnd(String s, int size, char fillChar) {
		String res;
		try {
			res = s.substring(0, size);
		} 
		catch (IndexOutOfBoundsException e) {
			res = s;
			for (int i = s.length(); i < size; i++) {
				res += fillChar;
			}
		}
		return res;
	}
	
	public static String resizeFront(String s, int size) {
		return resizeFront(s, size, ' ');
	}
	
	public static String resizeFront(String s, int size, char fillChar) {
		String res;
		try {
			res = s.substring(0, size);
		} 
		catch (IndexOutOfBoundsException e) {
			res = s;
			for (int i = s.length(); i < size; i++) {
				res = fillChar + res;
			}
		}
		return res;
	}
	
	public static int matchingBracket(String s, int pos) {
		if ((s == null) || (pos > s.length() - 1)) {
			return -1;
		}
		char open = s.charAt(pos);
		char close;
		switch (open) {
		case '{': 
			close = '}';
			break;
		case '(':
			close = ')';
			break;
		case '[':
			close = ']';
			break;
		case '<':
			close = '>';
			break;
		default:
			return -1;
		}	
		
		pos++;
		int count = 1;
		while ((count != 0) && (pos < s.length())) {
			if (s.charAt(pos) == open) {
				count++;
			} else if (s.charAt(pos) == close) {
				count--;
			}
			pos++;
		}
		if (count != 0) {
			return -1;
		} else {
			return pos - 1;
		}
	}
	
	
	public static int getTreeID(String s) {
		if ((s != null) && (s.length() > 0)) {
			int end = s.indexOf(':', 1);
			if (end == -1) {
				return -1;
			} else {
				return Integer.parseInt(s.substring(0, end));
			}
		} else {
			return -1;
		}
	}
	
	public static String getRoot(String s) {
		if ((s != null) && (s.length() > 0) && s.startsWith("{") && s.endsWith("}")) {
			int end = s.indexOf('{', 1);
			if (end == -1) {
				end = s.indexOf('}', 1);
			}
			return s.substring(1, end);
		} else {
			return null;
		}
	}
	
	@SuppressWarnings("unchecked")
	public static Vector getChildren(String s) {
		if ((s != null) && (s.length() > 0) && s.startsWith("{") && s.endsWith("}")) {
			Vector children = new Vector();
			int end = s.indexOf('{', 1);
			if (end == -1) {
				return children;
			}
			String rest = s.substring(end, s.length() - 1);
			int match = 0;
			while ((rest.length() > 0) && ((match = matchingBracket(rest, 0)) != -1)) {
				children.add(rest.substring(0, match + 1));
				if (match + 1 < rest.length()) {
					rest = rest.substring(match + 1);
				} else {
					rest = "";
				}
			}
			return children;
		} else {
			return null;
		}
	}
	
	@SuppressWarnings("unchecked")
	public static String parseTree(String s, Vector children) {
		children.clear();
		String root;
		if ((s != null) && (s.length() > 0) && s.startsWith("{") && s.endsWith("}")) {
			int end = s.indexOf('{', 1);
			if (end == -1) {
				end = s.indexOf('}', 1);
				return s.substring(1, end);
			}
			root = s.substring(1, end);
			String rest = s.substring(end, s.length() - 1);
			int match = 0;
			while ((rest.length() > 0) && (match = matchingBracket(rest, 0)) != -1) {
				children.add(rest.substring(0, match + 1));
				if (match + 1 < rest.length()) {
					rest = rest.substring(match + 1);
				} else {
					rest = "";
				}
			}
			return root;
		} else {
			return null;
		}
	}

	public static String commaSeparatedList(String[] list) {
		StringBuffer s = new StringBuffer();
		for (int i = 0; i < list.length; i++) {
			s.append(list[i]);
			if (i != list.length - 1) {
				s.append(",");
			}
		}
		return s.toString();
	}

	/**
	 * Encloses the strings of a list in quotes and separates them with
	 * a comma.
	 * 
	 * @param list
	 * @param quote
	 * @return
	 */
	public static String commaSeparatedList(String[] list, char quote) {
		StringBuffer s = new StringBuffer();
		for (int i = 0; i < list.length; i++) {
			s.append(quote + list[i] + quote);
			if (i != list.length - 1) {
				s.append(",");
			}
		}
		return s.toString();
	}
	
	/**
	 * Replaces the numbers 0..9 in a string with the 
	 * respective English word. All other characters will
	 * not be changed, e.g. '12.3' --> 'onetwo.three'. 
	 * @param num input string with numeric characters
	 * @return num with numeric characters replaced
	 */
	public static String spellOutNumber(String num) {
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < num.length(); i++) {
			char ch = num.charAt(i);
			switch (ch) {
			case '0': sb.append("zero"); break;
			case '1': sb.append("one"); break;
			case '2': sb.append("two"); break;
			case '3': sb.append("three"); break;
			case '4': sb.append("four"); break;
			case '5': sb.append("five"); break;
			case '6': sb.append("six"); break;
			case '7': sb.append("seven"); break;
			case '8': sb.append("eight"); break;
			case '9': sb.append("nine"); break;
			default:
				sb.append(ch);
			}
		}
		return sb.toString();		
	}
	
	public static String substituteBlanks(String s, String subst) {
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < s.length(); i++) {
			if (s.charAt(i) != ' ') {
				sb.append(s.charAt(i));
			} else {
				sb.append(subst);
			}
		}
		return sb.toString();
	}
	
	public static String escapeLatex(String s) {
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < s.length(); i++) {
			String c = s.charAt(i) + "";
			if (c.equals("#")) {
				c = "\\#";
			}
			if (c.equals("&")) {
				c = "\\&";
			}
			if (c.equals("$")) {
				c = "\\$";
			}
			if (c.equals("_")) {
				c = "\\_";
			}
			sb.append(c);
		}
		return sb.toString();
		
	}
		
}
