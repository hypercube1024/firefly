package com.firefly.template.parser;

import static com.firefly.template.support.RPNUtils.Type.*;

import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

import com.firefly.template.exception.ExpressionError;
import com.firefly.template.support.RPNUtils;
import com.firefly.template.support.RPNUtils.Fragment;

public class StatementExpression implements Statement {

	@Override
	public void parse(String content, JavaFileBuilder javaFileBuilder) {
		javaFileBuilder.writeStringValue(parse(content));
	}

	public String parse(String content) {
		List<Fragment> list = RPNUtils.getReversePolishNotation(content);
		if (list.size() == 1) {
			Fragment f = list.get(0);
			return f.type == VARIABLE ? getVariable(f.value, "Boolean") : f.value;
		}
		Deque<Fragment> d = new LinkedList<Fragment>();
		for (Fragment f : list) {
			if (isSymbol(f.type)) {
				Fragment right = d.pop();
				Fragment left = d.pop();

				Fragment ret = new Fragment();
				switch (f.type) {
				case ARITHMETIC_OPERATOR:
					if (left.type == STRING || right.type == STRING) {
						ret.type = STRING;
						if (f.value.equals("+")) {
							ret.value = getStringArithmeticResult(left, right);
						} else {
							throw new ExpressionError("The operation is not supported: " + left.type + " " + f.value + " " + right.type);
						}
					} else if (left.type == DOUBLE || right.type == DOUBLE) {
						ret.type = DOUBLE;
						ret.value = getFloatArithmeticResult(left, right, f.value, false);
					} else if (left.type == FLOAT || right.type == FLOAT) {
						ret.type = FLOAT;
						ret.value = getFloatArithmeticResult(left, right, f.value, true);
					} else if (left.type == LONG || right.type == LONG) {
						ret.type = LONG;
						ret.value = getIntegerArithmeticResult(left, right, f.value, false);
					} else if (left.type == INTEGER || right.type == INTEGER) {
						ret.type = INTEGER;
						ret.value = getIntegerArithmeticResult(left, right, f.value, true);
					} else {
						if (f.value.equals("+")) {
							ret.type = STRING;
							ret.value = getStringArithmeticResult(left, right);
						} else if (f.value.equals("/")) {
							ret.type = DOUBLE;
							ret.value = getFloatArithmeticResult(left, right, f.value, false);
						} else {
							ret.type = LONG;
							ret.value = getIntegerArithmeticResult(left, right, f.value, false);
						}
					}
					break;
				case LOGICAL_OPERATOR:
					ret.type = BOOLEAN;
					ret.value = getLogicalResult(left, right, f.value);
					if (ret.value == null)
						throw new ExpressionError("The operation is not supported: " + left.type + " " + f.value + " " + right.type);
					break;
				case ARITHMETIC_OR_LOGICAL_OPERATOR:
					if (left.type == LONG || right.type == LONG) {
						ret.type = LONG;
						ret.value = getIntegerArithmeticResult(left, right,
								f.value, false);
					} else if (left.type == INTEGER || right.type == INTEGER) {
						ret.type = INTEGER;
						ret.value = getIntegerArithmeticResult(left, right,
								f.value, true);
					} else {
						ret.type = BOOLEAN;
						ret.value = getLogicalResult(left, right, f.value);
					}
					if (ret.value == null)
						throw new ExpressionError(
								"The operation is not supported: " + left.type
										+ " " + f.value + " " + right.type);
					break;
				case CONDITIONAL_OPERATOR:
					ret.type = BOOLEAN;
					if (f.value.equals("==") || f.value.equals("!=")) {
						ret.value = getEqResult(left, right, f.value);
					} else {
						if (left.type == DOUBLE || right.type == DOUBLE) {
							ret.value = getFloatArithmeticResult(left, right,
									f.value, false);
						} else if (left.type == FLOAT || right.type == FLOAT) {
							ret.value = getFloatArithmeticResult(left, right,
									f.value, true);
						} else if (left.type == LONG || right.type == LONG) {
							ret.value = getIntegerArithmeticResult(left, right,
									f.value, false);
						} else if (left.type == INTEGER
								|| right.type == INTEGER) {
							ret.value = getIntegerArithmeticResult(left, right,
									f.value, true);
						} else {
							throw new ExpressionError(left.type + " and "
									+ right.type + " ​​can not do arithmetic.");
						}
					}
					break;
				default:
					throw new ExpressionError(
							"The operation is not supported: " + left.value
									+ " " + f.value + " " + right.value);
				}
				d.push(ret);
			} else {
				d.push(f);
			}
		}
		if (d.size() != 1)
			throw new ExpressionError("RPN error: " + content);
		return d.pop().value;
	}

	private boolean isSymbol(RPNUtils.Type type) {
		return type == ARITHMETIC_OPERATOR || type == LOGICAL_OPERATOR
				|| type == ASSIGNMENT_OPERATOR
				|| type == ARITHMETIC_OR_LOGICAL_OPERATOR
				|| type == CONDITIONAL_OPERATOR;
	}

	private String getVariable(String var) {
		int start = var.indexOf("${") + 2;
		int end = var.indexOf('}');
		return "objNav.getValue(model ,\"" + var.substring(start, end) + "\")";
	}

	private String getVariable(String var, String t) {
		StringBuilder ret = new StringBuilder();
		int start = var.indexOf("${") + 2;
		int end = var.indexOf('}');
		ret.append(var.substring(0, start - 2)).append("objNav.get" + t + "(model ,\"" + var.substring(start, end) + "\")");
		if (end < var.length() - 1)
			throw new ExpressionError("Variable format error: " + var);
		return ret.toString();
	}

	private String getVariableObj(String var) {
		StringBuilder ret = new StringBuilder();
		int start = var.indexOf("${") + 2;
		int end = var.indexOf('}');
		ret.append(var.substring(0, start - 2)).append(
				"objNav.find(model ,\"" + var.substring(start, end) + "\")");
		if (end < var.length() - 1)
			throw new ExpressionError("Variable format error: " + var);
		return ret.toString();
	}

	private String getArithmeticOrLogicalResult(Fragment left, Fragment right, String s, String type) {
		char f0 = s.charAt(0);
		left.value = left.type == VARIABLE ? getVariable(left.value, type) : left.value;
		right.value = right.type == VARIABLE ? getVariable(right.value, type) : right.value;
		return f0 == '*' || f0 == '/' || f0 == '%' ? (left.value + " " + s + " " + right.value)
				: ("(" + left.value + " " + s + " " + right.value + ")");
	}

	private String getEqResult(Fragment left, Fragment right, String s) {
		boolean eq = s.equals("==");
		String ret = null;
		if (left.type == VARIABLE && right.type == VARIABLE)
			ret = (eq ? "" : "!") + getVariableObj(left.value) + ".equals("
					+ getVariableObj(right.value) + ")";
		else if (left.type == VARIABLE) {
			if (right.type == NULL)
				ret = getVariableObj(left.value) + " " + s + " " + right.value;
			else
				ret = (eq ? "" : "!") + "((Object)(" + right.value
						+ ")).equals(" + getVariableObj(left.value) + ")";
		} else if (right.type == VARIABLE) {
			if (left.type == NULL)
				ret = left.value + " " + s + " " + getVariableObj(right.value);
			else
				ret = (eq ? "" : "!") + "((Object)(" + left.value
						+ ")).equals(" + getVariableObj(right.value) + ")";
		} else if (left.value.indexOf("objNav") >= 0
				|| right.value.indexOf("objNav") >= 0)
			ret = (eq ? "" : "!") + "((Object)(" + left.value + ")).equals("
					+ right.value + ")";
		else
			ret = String.valueOf(eq ? left.value.equals(right.value)
					: !left.value.equals(right.value));
		return ret;
	}

	private String getLogicalResult(Fragment left, Fragment right, String s) {
		String ret = null;

		if (left.type == VARIABLE && right.type == VARIABLE)
			ret = "(" + getVariable(left.value, "Boolean") + " " + s + " "
					+ getVariable(right.value, "Boolean") + ")";
		else if (left.type == VARIABLE || right.type == VARIABLE)
			ret = getArithmeticOrLogicalResult(left, right, s, "Boolean");
		else if (left.value.indexOf("objNav") >= 0
				|| right.value.indexOf("objNav") >= 0)
			ret = left.value + " " + s + " " + right.value;
		else
			ret = String.valueOf(s.equals("&&") ? getBooleanValue(left.value)
					&& getBooleanValue(right.value)
					: getBooleanValue(left.value)
							|| getBooleanValue(right.value));
		return ret;
	}

	private boolean getBooleanValue(String v) {
		if (v.charAt(0) == '!')
			return !new Boolean(v.substring(1).trim());
		else
			return new Boolean(v.trim());
	}
	
	private String getStringArithmeticResult(Fragment left, Fragment right) {
		left.value = left.type == VARIABLE ? getVariable(left.value) : left.value;
		right.value = right.type == VARIABLE ? getVariable(right.value) : right.value;
		if (left.value.charAt(0) == '"'
				&& left.value.indexOf("objNav.getValue(model ,\"") < 0
				&& right.value.charAt(0) == '"'
				&& right.value.indexOf("objNav.getValue(model ,\"") < 0)
			return "\""
					+ left.value.substring(1, left.value.length() - 1)
					+ right.value.substring(1, right.value.length() - 1)
					+ "\"";
		else
			return left.value + " + " + right.value;
	}

	private String getFloatArithmeticResult(Fragment left, Fragment right,
			String s, boolean isFloat) {
		String ret = null;
		char f0 = s.charAt(0);
		if (left.type == VARIABLE || right.type == VARIABLE)
			ret = getArithmeticOrLogicalResult(left, right, s,
					isFloat ? "Float" : "Double");
		else if (left.value.indexOf("objNav") >= 0
				|| right.value.indexOf("objNav") >= 0)
			ret = f0 == '*' || f0 == '/' || f0 == '%' ? (left.value + " " + s
					+ " " + right.value) : ("(" + left.value + " " + s + " "
					+ right.value + ")");
		else
			ret = getConstFloatArithmeticResult(left, right, s, isFloat);
		return ret;
	}

	private String getConstFloatArithmeticResult(Fragment lf, Fragment rf,
			String s, boolean isFloat) {
		float l = Float.parseFloat(lf.value), r = Float.parseFloat(rf.value);
		double l0 = Double.parseDouble(lf.value), r0 = Double
				.parseDouble(rf.value);
		String ret = null;
		char f0 = s.charAt(0);
		switch (f0) {
		case '+':
			ret = String.valueOf(isFloat ? l + r : l0 + r0);
			break;
		case '-':
			ret = String.valueOf(isFloat ? l - r : l0 - r0);
			break;
		case '*':
			ret = String.valueOf(isFloat ? l * r : l0 * r0);
			break;
		case '/':
			ret = String.valueOf(isFloat ? l / r : l0 / r0);
			break;
		case '%':
			ret = String.valueOf(isFloat ? l % r : l0 % r0);
			break;
		case '<':
			if (s.length() == 2 && s.charAt(1) == '=')
				ret = String.valueOf(isFloat ? l <= r : l0 <= r0);
			else if (s.length() == 1)
				ret = String.valueOf(isFloat ? l < r : l0 < r0);
			else
				throw new ExpressionError("The operation is not supported: "
						+ lf.type + " " + s + " " + rf.type);
			break;
		case '>':
			if (s.length() == 2 && s.charAt(1) == '=')
				ret = String.valueOf(isFloat ? l >= r : l0 >= r0);
			else if (s.length() == 1)
				ret = String.valueOf(isFloat ? l > r : l0 > r0);
			else
				throw new ExpressionError("The operation is not supported: "
						+ lf.type + " " + s + " " + rf.type);
			break;
		default:
			throw new ExpressionError("The operation is not supported: "
					+ lf.type + " " + s + " " + rf.type);
		}
		return ret;
	}

	private String getIntegerArithmeticResult(Fragment left, Fragment right, String s, boolean isInteger) {
		String ret = null;
		char f0 = s.charAt(0);
		if (left.type == VARIABLE || right.type == VARIABLE)
			ret = getArithmeticOrLogicalResult(left, right, s, isInteger ? "Integer" : "Long");
		else if (left.value.indexOf("objNav") >= 0 || right.value.indexOf("objNav") >= 0)
			ret = f0 == '*' || f0 == '/' || f0 == '%' ? 
					(left.value + " " + s + " " + right.value) 
					: ("(" + left.value + " " + s + " " + right.value + ")");
		else
			ret = getConstIntegerArithmeticResult(left, right, s, isInteger);
		return ret;
	}

	private String getConstIntegerArithmeticResult(Fragment lf, Fragment rf,
			String s, boolean isInteger) {
		int l = Integer.parseInt(lf.value), r = Integer.parseInt(rf.value);
		long l0 = Long.parseLong(lf.value), r0 = Long.parseLong(rf.value);
		String ret = null;
		char f0 = s.charAt(0);
		switch (f0) {
		case '+':
			ret = String.valueOf(isInteger ? l + r : l0 + r0);
			break;
		case '-':
			ret = String.valueOf(isInteger ? l - r : l0 - r0);
			break;
		case '*':
			ret = String.valueOf(isInteger ? l * r : l0 * r0);
			break;
		case '/':
			ret = String.valueOf(isInteger ? l / r : l0 / r0);
			break;
		case '%':
			ret = String.valueOf(isInteger ? l % r : l0 % r0);
			break;
		case '<':
			if (s.length() == 2 && s.charAt(1) == '=')
				ret = String.valueOf(isInteger ? l <= r : l0 <= r0);
			else if (s.length() == 2 && s.charAt(1) == '<')
				ret = String.valueOf(isInteger ? l << r : l0 << r0);
			else if (s.length() == 1)
				ret = String.valueOf(isInteger ? l < r : l0 < r0);
			else
				throw new ExpressionError("The operation is not supported: "
						+ lf.type + " " + s + " " + rf.type);
			break;
		case '>':
			if (s.length() == 3 && s.charAt(1) == '>' && s.charAt(2) == '>')
				ret = String.valueOf(isInteger ? l >>> r : l0 >>> r0);
			else if (s.length() == 2 && s.charAt(1) == '>')
				ret = String.valueOf(isInteger ? l >> r : l0 >> r0);
			else if (s.length() == 2 && s.charAt(1) == '=')
				ret = String.valueOf(isInteger ? l >= r : l0 >= r0);
			else if (s.length() == 1)
				ret = String.valueOf(isInteger ? l > r : l0 > r0);
			else
				throw new ExpressionError("The operation is not supported: "
						+ lf.type + " " + s + " " + rf.type);
			break;
		case '&':
			ret = String.valueOf(isInteger ? l & r : l0 & r0);
			break;
		case '|':
			ret = String.valueOf(isInteger ? l | r : l0 | r0);
			break;
		case '^':
			ret = String.valueOf(isInteger ? l ^ r : l0 ^ r0);
			break;
		default:
			throw new ExpressionError("The operation is not supported: "
					+ lf.type + " " + s + " " + rf.type);
		}
		return ret;
	}

}
