package test;

import static com.firefly.template.support.RPNUtils.getReversePolishNotation;
import static org.hamcrest.Matchers.is;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.firefly.template.exception.ExpressionError;
import com.firefly.template.parser.StatementExpression;
import com.firefly.template.support.RPNUtils;
import com.firefly.template.support.RPNUtils.Fragment;



public class TestRPN {
	
	@Test
	public void test() {
		Assert.assertThat(getReversePolishNotation("(${i} += +-3 + + + + -${i} ++ - -+${i}  --) >= 2").toString(), is("[${i}, -3, -${i} ++, +, -${i}  --, -, +=, 2, >=]"));
		Assert.assertThat(getReversePolishNotation("${login}").toString(), is("[${login}]"));
		Assert.assertThat(getReversePolishNotation("(- ${user.age} += (-3 + -  2) * 4) > 22").toString(), is("[-${user.age}, -3, -2, +, 4, *, +=, 22, >]"));
		Assert.assertThat(getReversePolishNotation("(${user.age} += 3 + 2 * 4) > 22").toString(), is("[${user.age}, 3, 2, 4, *, +, +=, 22, >]"));
		Assert.assertThat(getReversePolishNotation("1*2+3").toString(), is("[1, 2, *, 3, +]"));
		Assert.assertThat(getReversePolishNotation("1*2+3>>2+1").toString(), is("[1, 2, *, 3, +, 2, 1, +, >>]"));
		Assert.assertThat(getReversePolishNotation("1 + ((2 + 3) * 3) * 5").toString(), is("[1, 2, 3, +, 3, *, 5, *, +]"));
		Assert.assertThat(getReversePolishNotation("${user.age} > 1 + (2 + 3) * 5").toString(), is("[${user.age}, 1, 2, 3, +, 5, *, +, >]"));
		Assert.assertThat(getReversePolishNotation("${user.age} + 3 > 1 + (2 + 3) * 5").toString(), is("[${user.age}, 3, +, 1, 2, 3, +, 5, *, +, >]"));
		Assert.assertThat(getReversePolishNotation("${user.age} + 3 == ${user1.age} + (2 + 3) * 5").toString(), is("[${user.age}, 3, +, ${user1.age}, 2, 3, +, 5, *, +, ==]"));
		Assert.assertThat(getReversePolishNotation("${apple.price} > 7f && -(${apple.price} + 2) * 0.4 + 4 <= 3").toString(), is("[${apple.price}, 7, >, 0, ${apple.price}, 2, +, -, 0.4, *, 4, +, 3, <=, &&]"));
		Assert.assertThat(getReversePolishNotation("!(${apple.price} > 7f && -(${apple.price} + 2) * 0.4 + 4 <= 3)").toString(), is("[${apple.price}, 7, >, 0, ${apple.price}, 2, +, -, 0.4, *, 4, +, 3, <=, &&, false, ==]"));
		
		List<Fragment> list = getReversePolishNotation("!${login} != !false ");
		Assert.assertThat(list.toString(), is("[!${login}, !false, !=]"));
		Assert.assertThat(list.get(0).type, is(RPNUtils.Type.VARIABLE));
		Assert.assertThat(list.get(1).type, is(RPNUtils.Type.BOOLEAN));
		Assert.assertThat(list.get(2).type, is(RPNUtils.Type.CONDITIONAL_OPERATOR));
		
		list = getReversePolishNotation("${name} != \"Pengtao Qiu\"");
		Assert.assertThat(list.get(1).type, is(RPNUtils.Type.STRING));
		
		list = getReversePolishNotation("${user.age} > 18");
		Assert.assertThat(list.get(1).type, is(RPNUtils.Type.INTEGER));
		
		list = getReversePolishNotation("${user.id} > 18L");
		Assert.assertThat(list.get(1).type, is(RPNUtils.Type.LONG));
		
		list = getReversePolishNotation("${food.price} > 3.3f");
		Assert.assertThat(list.get(1).type, is(RPNUtils.Type.FLOAT));
		
		list = getReversePolishNotation("${food.price} > 3.3");
		Assert.assertThat(list.get(1).type, is(RPNUtils.Type.DOUBLE));
	}
	
	@Test
	public void testELParse() {
		StatementExpression se = new StatementExpression();
		Assert.assertThat(se.parse("3 + 3 * 5 / 2"), is("10"));
		Assert.assertThat(se.parse("3L + 3L * 5L / 2L"), is("10"));
		Assert.assertThat(se.parse("3.0 + 3.0 * 5.0 / 2.0"), is("10.5"));
		Assert.assertThat(se.parse("3f + 3f * 5f / 2f"), is("10.5"));
		Assert.assertThat(se.parse("3.0 + 3 * 5.0 / 2.0"), is("10.5"));
		Assert.assertThat(se.parse("3 + 3f * 5 / 2f"), is("10.5"));
		Assert.assertThat(se.parse("1L +" + Integer.MAX_VALUE), is("2147483648"));
		Assert.assertThat(se.parse("1 +" + Integer.MAX_VALUE), is("-2147483648"));
		
		Assert.assertThat(se.parse("\"hello \" + \"firefly \\\" ***!! \""), is("\"hello firefly \\\" ***!! \""));
		Assert.assertThat(se.parse("\"hello \" + \"firefly \" + \"!\""), is("\"hello firefly !\""));
		Assert.assertThat(se.parse("'hello ' + 'firefly ' + '!'"), is("\"hello firefly !\""));
		Assert.assertThat(se.parse("'hello ' + 'firefly ' + ${i} + '!'"), is("\"hello firefly \" + objNav.getValue(model ,\"i\") + \"!\""));
		Assert.assertThat(se.parse("(3f + ${j}) / 2 + ${i} + 1.0"), is("(((3 + objNav.getFloat(model ,\"j\")) / 2 + objNav.getFloat(model ,\"i\")) + 1.0)"));
		
		Assert.assertThat(se.parse("true"), is("true"));
		Assert.assertThat(se.parse("false"), is("false"));
		Assert.assertThat(se.parse("! true"), is("! true"));
		Assert.assertThat(se.parse("1|2"), is("3"));
		Assert.assertThat(se.parse("!${user.pass}"), is("!objNav.getBoolean(model ,\"user.pass\")"));
		Assert.assertThat(se.parse("${user.pass}"), is("objNav.getBoolean(model ,\"user.pass\")"));
		Assert.assertThat(se.parse("1 | 2 & ${i}"), is("(1 | (2 & objNav.getInteger(model ,\"i\")))"));
		Assert.assertThat(se.parse("!${i} || !${j} && ${k}"), is("(!objNav.getBoolean(model ,\"i\") || (!objNav.getBoolean(model ,\"j\") && objNav.getBoolean(model ,\"k\")))"));
		Assert.assertThat(se.parse("${i} & ${j}"), is("(objNav.getBoolean(model ,\"i\") & objNav.getBoolean(model ,\"j\"))"));
		Assert.assertThat(se.parse("${apple.price} > 7f && ${apple.price} <= 3"), is("(objNav.getFloat(model ,\"apple.price\") > 7) && (objNav.getInteger(model ,\"apple.price\") <= 3)"));
		Assert.assertThat(se.parse("${i} != 'pt1 !'"), is("!((Object)(\"pt1 !\")).equals(objNav.find(model ,\"i\"))"));
		Assert.assertThat(se.parse("'pt1 !'!='pt1 !'"), is("false"));
		Assert.assertThat(se.parse("${i} == ${j} && ${i} != ${k}"), is("objNav.find(model ,\"i\").equals(objNav.find(model ,\"j\")) && !objNav.find(model ,\"i\").equals(objNav.find(model ,\"k\"))"));
		Assert.assertThat(se.parse("${i} != null && null == ${j} && ${i} != ${k}"), is("objNav.find(model ,\"i\") != null && null == objNav.find(model ,\"j\") && !objNav.find(model ,\"i\").equals(objNav.find(model ,\"k\"))"));
		Assert.assertThat(se.parse("3 + 4 +-( -(2 - 1) + 1)"), is("7"));
		Assert.assertThat(se.parse("-(3 + 4) +-( -(2 - 1) + 1)"), is("-7"));
		Assert.assertThat(se.parse("!(${apple.price} > 7f && -(${apple.price} + 2) * 0.4 + 4 <= 3)"), is("((Object)((objNav.getFloat(model ,\"apple.price\") > 7) && (((0 - (objNav.getInteger(model ,\"apple.price\") + 2)) * 0.4 + 4) <= 3))).equals(false)"));
		Assert.assertThat(se.parse("${i} != null && ${i.size} > 0"), is("objNav.find(model ,\"i\") != null && (objNav.getInteger(model ,\"i.size\") > 0)"));
	}
	
	@Test
	public void testELParseError() {
		StatementExpression se = new StatementExpression();
		Assert.assertThat(se.parse("${i} + ${j} + ${k}"), is("objNav.getValue(model ,\"i\") + objNav.getValue(model ,\"j\") + objNav.getValue(model ,\"k\")"));
	}
	
	@Test
	public void testELParseError2() {
		StatementExpression se = new StatementExpression();
		Assert.assertThat(se.parse("${i} + ${j} + 2"),is(se.parse("${i}-- + ${j} + 2")));
	}
	
	@Test(expected = ExpressionError.class)
	public void testELParseError3() {
		StatementExpression se = new StatementExpression();
		se.parse("${i} >= ${j} < ${k}");
	}
	
	public static void main(String[] args) {
		System.out.println(getReversePolishNotation("9 - 5 + 2"));
		System.out.println(getReversePolishNotation("9 - (5 + 2)"));
		StatementExpression se = new StatementExpression();
		System.out.println(se.parse("9 - 5 + 2"));
		System.out.println(se.parse("9 - (5 + 2)"));
		System.out.println(se.parse("${i} + ${j} + ${k}"));
		System.out.println(se.parse("${i} + ${j} + 2"));
		System.out.println(se.parse("${i}-- + ${j} + 2"));
		System.out.println(se.parse("${i} >= ${j} < ${k}"));
	}
	
	public static void main6(String[] args) {
		int i = 3 + 4 + -(-(2 - 1) + 1);
		System.out.println(i);
//		System.out.println(preprocessing("3 + 4 + (0 -(-(2 - 1) + 1))"));
//		System.out.println(preprocessing("3 + 4 + -(-(2 - 1) + 1)"));
//		System.out.println(getReversePolishNotation("3 + 4 + -(-(2 - 1) + 1)"));
//		System.out.println(preprocessing("${apple.price} > 7f && -(${apple.price} + 2) * 0.4 + 4 <= 3"));
		System.out.println(getReversePolishNotation("!(${apple.price} > 7f && -(${apple.price} + 2) * 0.4 + 4 <= 3)"));
		
		StatementExpression se = new StatementExpression();
		System.out.println(se.parse("-(3 + 4) > 8 && !(${i} > 0)"));
		System.out.println(se.parse("(0-(0 - ${i})) * 4"));
		System.out.println(se.parse("-(3 + 4) +-( -(2 - 1) + 1)"));
		System.out.println(se.parse("!(${apple.price} > 7f && -(${apple.price} + 2) * 0.4 + 4 <= 3)"));
		System.out.println(se.parse("${i} != null && ${i.size} > 0"));
	}
	
	public static void main5(String[] args) {
		System.out.println(((Object)"Bob").equals("Bob"));
		StatementExpression se = new StatementExpression();
		System.out.println(se.parse("'pt1 !'!= ${i}"));
		System.out.println(se.parse("${i}!= 'pt1 !'"));
		System.out.println(se.parse("'pt1 !' != 'pt1 !'"));
//		String str = null;
//		System.out.println("".equals(null));
		List<Fragment> list = getReversePolishNotation("${i} != null && ${i} != ${k}");
		System.out.println(list.toString());
		for(Fragment f : list) {
			System.out.print(f.type + ", ");
		}
		System.out.println();
		System.out.println(se.parse("${i} != null && null == ${j} && ${i} != ${k}"));
		
	}
	
	public static void main4(String[] args) {
		List<Fragment> list = getReversePolishNotation("!(${apple.price} > 7f && ${apple.price} <= 3)");
		System.out.println(list.toString());
		for(Fragment f : list) {
			System.out.print(f.type + ", ");
		}
		System.out.println();
		
		StatementExpression se = new StatementExpression();
		
		System.out.println(se.parse("!(${apple.price} > 7f && ${apple.price} <= 3)"));
		System.out.println(se.parse("${apple.price} > 7f && ${apple.price} <= 3"));
		System.out.println(se.parse("!${i} || !${j} && ${k}"));
		System.out.println(se.parse("5 > 3 && 5 > 2"));
		System.out.println(se.parse("${i} || 5 < 3 && 5 > 2"));
	}
	
	
	public static void main3(String[] args) {
		System.out.println(((Object)(3)).equals(3));
		List<Fragment> list = getReversePolishNotation("(${i} += +-3 + + + + -${i} -- - -+${i}  --) >= 2");
		System.out.println(list.toString());
		for(Fragment f : list) {
			System.out.print(f.type + ", ");
		}
		System.out.println();
		
//		list = getReversePolishNotation("!(3f + ${apple.price} > 7)");
//		System.out.println(list.toString());
//		for(Fragment f : list) {
//			System.out.print(f.type + ", ");
//		}
//		System.out.println();
//		
//		StatementExpression se = new StatementExpression();
//		System.out.println(se.parse("\"hello \" + \"firefly \\\" ***!! \""));
		
	}
	
	public static void main2(String[] args) {
		System.out.println(Long.parseLong("3"));
		System.out.println(Float.parseFloat("2"));
		System.out.println(Boolean.parseBoolean("!false"));
		
		List<Fragment> list = getReversePolishNotation("! ${login} != ! false");
		System.out.println(list.toString());
		for(Fragment f : list) {
			System.out.print(f.type + ", ");
		}
		System.out.println();
		
		list = getReversePolishNotation("${name} != \"Pengtao Qiu\"");
		System.out.println(list.toString());
		for(Fragment f : list) {
			System.out.print(f.type + ", ");
		}
		System.out.println();
		
		list = getReversePolishNotation("1*2+3>>2+1f");
		System.out.println(list.toString());
		for(Fragment f : list) {
			System.out.print(f.type + ", ");
		}
		System.out.println();
		
		System.out.println(getReversePolishNotation("\"Pengtao Qiu\" == ${user.name}"));
		System.out.println(getReversePolishNotation("(- ${user.age} += (-3 + -  2) * 4) > 22"));
		System.out.println(getReversePolishNotation("(${i} += +-3 + + + + -${i} -- - -+${i}  --) >= 2"));
		System.out.println(getReversePolishNotation("1*2+3>>2+1"));
		System.out.println(Float.parseFloat("3.5") + Long.parseLong("4"));
		
		System.out.println(getReversePolishNotation("3 + 3 * 5 / 2"));
		System.out.println(getReversePolishNotation("3 + 3 * 5 / 2"));
		System.out.println("================================================");
		StatementExpression se = new StatementExpression();
		
		System.out.println(se.parse("3L + 3L * 5L / 2L"));
		System.out.println(se.parse("3 + 3 * 5 / 2"));
		System.out.println(se.parse("3.0 + 3 * 5.0 / 2.0"));
		System.out.println(se.parse("3 + 3f * 5 / 2f"));
		System.out.println(se.parse("\"hello \" + \"firefly \""));
		System.out.println(se.parse("'hello ' + 'firefly ' + '!'"));
		System.out.println(se.parse("'hello ' + 'firefly ' + ${i} + '!'"));
//		System.out.println(se.parse("${i} + ${j} + ${k}"));
		
		System.out.println(se.parse("${i} + 3 + 5 + 2 / 1.0"));
		System.out.println(se.parse("(3f + ${j}) / 2 + ${i} + 1.0"));
		System.out.println(se.parse("1L +" + Integer.MAX_VALUE));
		System.out.println(1 + Integer.MAX_VALUE);
		System.out.println(se.parse("(3f + ${apple.price}) / 2 + ${i} + 1.0"));
		System.out.println(se.parse("(3f + ${apple.price}) / 2 + ${i} + 1.0 >= 2"));
		System.out.println(se.parse("!${i} || !${j} && ${k}"));
		System.out.println(se.parse("1 | 2 & ${i}"));
		System.out.println(se.parse("${i} & ${j}"));
//		System.out.println(se.parse("${apple.price} + 1f >= 5 && ${apple.price} + 1f < 10"));
//		System.out.println(se.parse("! ${user1.pass} == !true && ${user2.pass} == true "));
		
		System.out.println(se.parse("!${user.pass}"));
//		System.out.println(se.parse("(3f + ${j} --) / 2 + ${i}++ + 1.0"));
		
	}
}
