package com.firefly.template.support;

import java.util.Arrays;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import com.firefly.template.exception.ExpressionError;
import com.firefly.utils.VerifyUtils;

public class RPNUtils {
	
	private static final Set<String> CONDITIONAL_OPERATOR = new HashSet<String>(Arrays.asList("==", "!=", ">", "<", ">=", "<="));
	private static final Set<String> ARITHMETIC_OR_LOGICAL_OPERATOR = new HashSet<String>(Arrays.asList("&", "|"));
	private static final Set<String> LOGICAL_OPERATOR = new HashSet<String>(Arrays.asList("&&", "||"));
	private static final Set<String> ASSIGNMENT_OPERATOR = new HashSet<String>(Arrays.asList("=", "+=", "-=", "*=", "/=", "%=", "^=", "&=", "|=", "<<=", ">>=", ">>>="));
	
	/**
	 * This method parses the expression and creates Reverse Polish notation 
	 * symbol priority：
	 * 10: "*", "/", "%"
	 *	9: "+", "-" 
	 *	8: ">>", ">>>", "<<"
	 *	7: ">", "<", ">=", "<="
	 *	6: "==", "!="
	 *	5: "&"
	 *	4: "|"
	 *	3: "^"
	 *	2: "&&"
	 *	1: "||"
	 *	0: "=", "+=", "-=", "*=", "/=", "%=", "^=", "&=", "|=", "<<=", ">>=", ">>>=" //0
	 * @param text the infix notation 
	 * @return A list has many tokens and the sequence of postfix notation
	 */
	public static List<Fragment> getReversePolishNotation(String text) {
		String content = preprocessing(text);
		StringBuilder pre = new StringBuilder();
		Deque<Fragment> symbolDeque = new LinkedList<Fragment>();
		List<Fragment> list = new LinkedList<Fragment>();
		char c, n, n1, n2;
		
		for (int i = 0; i < content.length(); i++) {
			switch (content.charAt(i)) {
			case '\'':
			case '"':
				int next = content.indexOf(content.charAt(i), i + 1);
				while(content.charAt(next - 1) == '\\')
					next = content.indexOf(content.charAt(i), next + 1);

				pre.append(content.substring(i, next + 1));
				i += next - i;
				break;
			case '(':
				pre.delete(0, pre.length());
				
				Fragment f0 = new Fragment();
				f0.priority = -1000;
				f0.value = "(";
				symbolDeque.push(f0);
				break;
			case '*':
			case '/':
			case '%':
				n = content.charAt(i + 1);
				if(n == '=') { // *= /= %=
					outValue(pre, list);
					outSymbol(String.valueOf(content.charAt(i)) + "=", 0, symbolDeque, list);
					i++;
					break;
				}
				
				// * / %
				outValue(pre, list);
				outSymbol(String.valueOf(content.charAt(i)), 10, symbolDeque, list);
				break;
			case '+':
			case '-':
				n = content.charAt(i + 1);
				if(n == '=') { // += -=
					outValue(pre, list);
					outSymbol(String.valueOf(content.charAt(i)) + "=", 0, symbolDeque, list);
					i++;
					break;
				}
				
				if(n == content.charAt(i)) {
					pre.append(content.charAt(i)).append(content.charAt(i + 1));
					i++;
					break;
				}
				
				// 正负号判断
				boolean s = false;
				String left0 = "*/%+-><=&|(^";
				if(i == 0) {
					s = true;
				} else {
					for(int j = i - 1; j >= 0; j--) {
						char ch = content.charAt(j);
						if(!Character.isWhitespace(ch) ) {
							if(left0.indexOf(ch) >= 0) {
								int _n = j - 1;
								s = _n < 0 || !(ch == '+' && content.charAt(_n) == '+' || ch == '-' && content.charAt(_n) == '-');
							}
							break;
						}
					}
				}
				
				
				// + -
				if(s) {
					pre.append(content.charAt(i));
				} else {
					outValue(pre, list);
					outSymbol(String.valueOf(content.charAt(i)), 9, symbolDeque, list);
				}
				break;
				
			case '>':
			case '<':
				c = content.charAt(i);
				n = content.charAt(i + 1);
				
				if(c == n) {
					if(i + 2 < content.length()) {
						n1 = content.charAt(i + 2);
						if(n1 == '=') { // <<= >>=
							outValue(pre, list);
							outSymbol(String.valueOf(content.charAt(i)) + content.charAt(i + 1) + "=", 0, symbolDeque, list);
							i += 2;
							break;
						}
					}
					
					// << >>
					outValue(pre, list);
					outSymbol(String.valueOf(content.charAt(i)) + content.charAt(i + 1), 8, symbolDeque, list);
					i++;
					break;
				}
				
				if(i + 2 < content.length()) {
					n1 = content.charAt(i + 2);
					if(c == '>' && n == '>' && n1 == '>') { 
						n2 = content.charAt(i + 3);
						if(i + 3 < content.length()) {
							if(n2 == '=') { // >>>=
								outValue(pre, list);
								outSymbol(">>>=", 0, symbolDeque, list);
								i += 3;
								break;
							}
						}
						
						// >>>
						outValue(pre, list);
						outSymbol(">>>", 8, symbolDeque, list);
						i += 2;
						break;
					}
				}
				
				if(n == '=') { // <= >=
					outValue(pre, list);
					outSymbol(String.valueOf(content.charAt(i)) + "=", 7, symbolDeque, list);
					i++;
					break;
				}
				
				// < >
				outValue(pre, list);
				outSymbol(String.valueOf(content.charAt(i)), 7, symbolDeque, list);
				break;
			
			case '=':
				n = content.charAt(i + 1);
				if(n == '=') { // ==
					outValue(pre, list);
					outSymbol(String.valueOf(content.charAt(i)) + "=", 6, symbolDeque, list);
					i++;
					break;
				}
				
				// =
				outValue(pre, list);
				outSymbol(String.valueOf(content.charAt(i)), 0, symbolDeque, list);
				break;
			
			case '!':
				n = content.charAt(i + 1);
				if(n == '=') { // !=
					outValue(pre, list);
					outSymbol(String.valueOf(content.charAt(i)) + "=", 6, symbolDeque, list);			
					i++;
					break;
				}
				pre.append('!');
				break;
				
			case '&':
				n = content.charAt(i + 1);
				if(n == '&') { // &&
					outValue(pre, list);
					outSymbol("&&", 2, symbolDeque, list);
					i++;
					break;
				}
				
				if(n == '=') { // &=
					outValue(pre, list);
					outSymbol("&=", 0, symbolDeque, list);
					i++;
					break;
				}
				
				// &
				outValue(pre, list);
				outSymbol("&", 5, symbolDeque, list);
				break;
			case '|':
				n = content.charAt(i + 1);
				if(n == '|') { // ||
					outValue(pre, list);
					outSymbol("||", 1, symbolDeque, list);
					i++;
					break;
				}
				
				if(n == '=') { // |=
					outValue(pre, list);
					outSymbol("|=", 0, symbolDeque, list);
					i++;
					break;
				}
				
				// |
				outValue(pre, list);
				outSymbol("|", 4, symbolDeque, list);
				break;
			case '^':
				n = content.charAt(i + 1);
				if(n == '=') {// ^=
					outValue(pre, list);
					outSymbol(String.valueOf(content.charAt(i)) + "=", 0, symbolDeque, list);
					i++;
					break;
				}
				
				// ^
				outValue(pre, list);
				outSymbol("^", 3, symbolDeque, list);
				break;
			case ')':
				outValue(pre, list);
				outSymbol(")", -1000, symbolDeque, list);
				break;
			default:
				pre.append(content.charAt(i));
				break;
			}
		}
		
		outValue(pre, list);
		while(!symbolDeque.isEmpty())
			list.add(symbolDeque.pop());
		
		return list;
	}
	
	private static String preprocessing(String content) {
		StringBuilder ret = new StringBuilder();
		if(preprocessing0(content, ret))
			return ret.toString();
		else
			return preprocessing(ret.toString());
	}
	
	private static boolean preprocessing0(String content, StringBuilder ret) {
		boolean t = true;
		StringBuilder pre = new StringBuilder();
		int c = 0;
		int start = 0;
		for (int i = 0; i < content.length(); i++) {
			char ch = content.charAt(i);
			switch (ch) {
			case '!':
			case '+':
			case '-':
				boolean l = false;
				boolean r = false;
				String left0 = "*/%+-><=&|(^";
				if(i == 0 || ch == '!') {
					l = true;
				} else {
					for(int j = i - 1; j >= 0; j--) {
						char c0 = content.charAt(j);
						if(!Character.isWhitespace(c0) ) {
							if(left0.indexOf(c0) >= 0) {
								int _n = j - 1;
								l = _n < 0 || !(c0 == '+' && content.charAt(_n) == '+' || c0 == '-' && content.charAt(_n) == '-');
							}
							break;
						}
					}
				}
				
				if(l) {
					for (int j = i + 1; j < content.length(); j++) {
						char c0 = content.charAt(j);
						if(!Character.isWhitespace(c0)) {
							if(c0 == '(') {
								start = j + 1;
								r = true;
								pre.append(c0);
							}
							break;
						}
					}
				}
				if(l && r) {
					t = false;
					c += 1;
					while(c != 0) {
						char c0 = content.charAt(start++);
						if(c0 == '(')
							c++;
						else if(c0 == ')')
							c--;
						pre.append(c0);
					}
					if(ch == '!')
						ret.append("(").append(pre).append(" == false) ");
					else
						ret.append("(0 ").append(ch).append(' ').append(pre).append(") ");

					pre.delete(0, pre.length());
					i = start;
				} else
					ret.append(ch);
				break;
			default:
				ret.append(ch);
				break;
			}
		}
		return t;
	}
	
	/**
	 * remove redundant symbols, such as +, -
	 * @param v
	 * @return
	 */
	private static String getSimpleValue(String v) {
		int left = 0;
		boolean n = false;
		for (int i = 0; i < v.length(); i++) {
			char ch = v.charAt(i);
			if(ch == '-')
				n = true;
			if(ch != '+' && ch != '-' && !Character.isWhitespace(ch)) {
				left = i;
				break;
			}
		}
		String s = v.substring(left);
		return n ? "-" + s : s;
	}
	
	private static boolean isString(String v) {
		int start = v.charAt(0);
		int end = v.charAt(v.length() - 1);
		return (start == '"' && end == '"') || (start == '\'' && end == '\'');
	}
	
	private static boolean isBoolean(String v) {
		int start = v.charAt(0) == '!' ? 1 : 0;
		return v.substring(start).trim().equals("true") || v.substring(start).trim().equals("false");
	}
	
	private static boolean isVariable(String v) {
		int start = v.indexOf("${");
		int end = v.indexOf('}');
		return start >= 0 && start < end;
	}
	
	private static void outValue(StringBuilder pre, List<Fragment> list) {
		String v = pre.toString().trim();
		if(v.length() > 0) {
			Fragment f= new Fragment();
			f.priority = -200;
			f.value = getSimpleValue(v);
			
			if(isVariable(f.value)) {
				f.type = Type.VARIABLE;
			} else if(isBoolean(f.value)) {
				f.type = Type.BOOLEAN;
			} else if(isString(f.value)) {
				f.type = Type.STRING;
				f.value = "\"" + f.value.substring(1, f.value.length() - 1) + "\"";
			} else if(VerifyUtils.isFloat(f.value)) {
				f.type = Type.FLOAT;
				char end = f.value.charAt(f.value.length() - 1);
				if(end == 'f' || end == 'F')
					f.value = f.value.substring(0, f.value.length() - 1);
			} else if(VerifyUtils.isDouble(f.value)) {
				f.type = Type.DOUBLE;
			} else if(VerifyUtils.isInteger(f.value)) {
				f.type = Type.INTEGER;
			} else if(VerifyUtils.isLong(f.value)) {
				f.type = Type.LONG;
				char end = f.value.charAt(f.value.length() - 1);
				if(end == 'l' || end == 'L')
					f.value = f.value.substring(0, f.value.length() - 1);
			} else if(f.value.equals("null")) { 
				f.type = Type.NULL;
			} else
				throw new ExpressionError("Can not determine the type: " + f.value);
			
			list.add(f);
		}
		pre.delete(0, pre.length());
	}
	
	private static void outSymbol(String value, int priority, Deque<Fragment> symbolDeque, List<Fragment> list) {
		Fragment f = new Fragment();
		f.value = value;
		f.priority = priority;
		if(ARITHMETIC_OR_LOGICAL_OPERATOR.contains(value)) {
			f.type = Type.ARITHMETIC_OR_LOGICAL_OPERATOR;
		} else if(LOGICAL_OPERATOR.contains(value)) {
			f.type = Type.LOGICAL_OPERATOR;
		} else if(ASSIGNMENT_OPERATOR.contains(value)) {
			f.type = Type.ASSIGNMENT_OPERATOR;
		} else if(CONDITIONAL_OPERATOR.contains(value)) {
			f.type = Type.CONDITIONAL_OPERATOR;
		} else
			f.type = Type.ARITHMETIC_OPERATOR;
		
		if(f.value.equals(")")) {
			for(Fragment top = null; !symbolDeque.isEmpty() 
					&& !(top = symbolDeque.pop()).value.equals("("); )
				list.add(top);
		} else {
			for(Fragment top = null; !symbolDeque.isEmpty() 
					&& (top = symbolDeque.peek()).priority >= f.priority; ) {
				list.add(top);
				symbolDeque.pop();
			}
			symbolDeque.push(f);
		}
	}
	
	public static class Fragment {
		public int priority;
		public String value;
		public Type type;
		
		public String toString() {
			return value;
		}
	}
	
	public static enum Type {
		VARIABLE,
		INTEGER,
		LONG,
		FLOAT,
		DOUBLE,
		BOOLEAN,
		STRING,
		NULL,
		
		ARITHMETIC_OPERATOR,
		LOGICAL_OPERATOR,
		ASSIGNMENT_OPERATOR,
		ARITHMETIC_OR_LOGICAL_OPERATOR,
		CONDITIONAL_OPERATOR
	}
}
