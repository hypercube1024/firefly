package test.utils.json.parser;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import test.utils.json.ArrayObj;
import test.utils.json.Bar;
import test.utils.json.Book;
import test.utils.json.CollectionObj;
import test.utils.json.DateFormatObject;
import test.utils.json.DateObj;
import test.utils.json.MapObj;
import test.utils.json.Profile;
import test.utils.json.SimpleObj;
import test.utils.json.SimpleObj2;
import test.utils.json.SpecialPropertyObject;
import test.utils.json.User;
import test.utils.json.github.MediaContent;
import test.utils.json.github.Player;
import test.utils.json.github.Size;

import com.firefly.utils.json.Json;
import com.firefly.utils.json.JsonArray;
import com.firefly.utils.json.JsonObject;
import com.firefly.utils.json.io.JsonStringWriter;
import com.firefly.utils.time.SafeSimpleDateFormat;

public class TestParser {
	
	@Test
	public void testStr() {
		SimpleObj i = new SimpleObj();
		i.setName("PengtaoQiu\nAlvin\nhttp://fireflysource.com");
		String jsonStr = Json.toJson(i);
		System.out.println(jsonStr);
		SimpleObj i2 = Json.toObject(jsonStr, SimpleObj.class);
		Assert.assertThat(i2.getName(), is("PengtaoQiu\nAlvin\nhttp://fireflysource.com"));
	}
	
	@Test
	public void testControlChar() {

		char ch = (char)31, 
				ch1 = (char)1, 
				ch2 = (char)0,
				ch3 = (char)15,
				ch4 = (char)16;
		
		Assert.assertThat(JsonStringWriter.escapeSpecialCharacter(ch), is("\\u001f"));
		Assert.assertThat(JsonStringWriter.escapeSpecialCharacter(ch1), is("\\u0001"));
		Assert.assertThat(JsonStringWriter.escapeSpecialCharacter(ch2), is("\\u0000"));
		Assert.assertThat(JsonStringWriter.escapeSpecialCharacter(ch3), is("\\u000f"));
		Assert.assertThat(JsonStringWriter.escapeSpecialCharacter(ch4), is("\\u0010"));
		
		SimpleObj i = new SimpleObj();
		i.setName("PengtaoQiu\nAlvin\nhttp://fireflysource.com" + String.valueOf(ch1) + String.valueOf(ch2) + String.valueOf(ch3) + String.valueOf(ch4) + String.valueOf(ch));
		String jsonStr = Json.toJson(i);
		System.out.println(jsonStr);
		SimpleObj i2 = Json.toObject(jsonStr, SimpleObj.class);
		Assert.assertThat((int)i2.getName().charAt(i2.getName().length() - 1), is(31));
		Assert.assertThat((int)i2.getName().charAt(i2.getName().length() - 2), is(16));
		Assert.assertThat((int)i2.getName().charAt(i2.getName().length() - 3), is(15));
		Assert.assertThat((int)i2.getName().charAt(i2.getName().length() - 4), is(0));
		Assert.assertThat((int)i2.getName().charAt(i2.getName().length() - 5), is(1));
		
		System.out.println(Json.toJson(i2));
	}
	
	@Test
	public void test() {
		SimpleObj i = new SimpleObj();
		i.setAge(10);
		i.setId(33442);
		i.setNumber(30);
		i.setName("PengtaoQiu\nAlvin");
		i.setType((short)-33);
		i.setWeight(55.47f);
		i.setHeight(170.5);
		String jsonStr = Json.toJson(i);
		
		SimpleObj i2 = Json.toObject(jsonStr, SimpleObj.class);
		Assert.assertThat(i2.getAge(), is(10));
		Assert.assertThat(i2.getId(), is(33442));
		Assert.assertThat(i2.getNumber(), is(30));
		Assert.assertThat(i2.getDate(), is(0L));
		Assert.assertThat(i2.getName(), is("PengtaoQiu\nAlvin"));
		Assert.assertThat(i2.getType(), is((short)-33));
		Assert.assertThat(i2.getHeight(), is(170.5));
		Assert.assertThat(i2.getWeight(), is(55.47f));
	}
	
	@Test
	public void test2() {
		SimpleObj i = new SimpleObj();
		i.setAge(10);
		i.setId(33442);
		i.setNumber(30);
		i.setName("PengtaoQiu\nAlvin");
		
		SimpleObj i2 = new SimpleObj();
		i2.setAge(20);
		i2.setId(12341);
		i2.setNumber(33);
		i2.setName("Tom");
		i.setContact1(i2);
		String jsonStr = Json.toJson(i);
		
		SimpleObj temp = Json.toObject(jsonStr, SimpleObj.class);
		Assert.assertThat(temp.getId(), is(33442));
		Assert.assertThat(temp.getContact1().getId(), is(12341));
		Assert.assertThat(temp.getContact1().getName(), is("Tom"));
		Assert.assertThat(temp.getContact1().getAge(), is(20));
		Assert.assertThat(temp.getContact2(), nullValue());
	}
	
	@Test
	public void test3() {
		String jsonStr = "{\"id\":33442,\"date\":null,\"add1\":{}, \"add2\":{}, \"add3\":{}, \"add4\":{}, \"add5\":null,\"add6\":\"sdfsdf\",\"contact2\":{}, \"number\":30,\"height\":\" 33.24 \",\"name\":\"PengtaoQiu\nAlvin\",\"type\":null,\"weight\":40.3}";
		SimpleObj temp = Json.toObject(jsonStr, SimpleObj.class);
		Assert.assertThat(temp.getName(), is("PengtaoQiu\nAlvin"));
		Assert.assertThat(temp.getId(), is(33442));
		Assert.assertThat(temp.getWeight(), is(40.3F));
		Assert.assertThat(temp.getHeight(), is(33.24));
	}
	
	@Test
	public void test4() {
		SimpleObj2 so2 = new SimpleObj2();
		so2.setId(334);
		
		User user = new User();
		user.setId(2434L);
		user.setName("Pengtao");
		so2.setUser(user);
		
		Book book = new Book();
		book.setId(23424);
		book.setPrice(3.4);
		book.setSell(true);
		book.setText("cccccccc");
		book.setTitle("ddddd");
		so2.setBook(book);
		
		String jsonStr = Json.toJson(so2);
		
		SimpleObj2 temp = Json.toObject(jsonStr, SimpleObj2.class);
		Assert.assertThat(temp.getBook().getPrice(), is(3.4));
		Assert.assertThat(temp.getBook().getTitle(), nullValue());
		Assert.assertThat(temp.getId(), is(334));
	}
	
	@Test
	public void test5() {
		List<LinkedList<SimpleObj>> list = new LinkedList<LinkedList<SimpleObj>>();
		
		LinkedList<SimpleObj> list1 = new LinkedList<SimpleObj>();
		for (int j = 0; j < 10; j++) {
			SimpleObj i = new SimpleObj();
			i.setAge(10);
			i.setId(33442 + j);
			i.setNumber(30);
			i.setName("PengtaoQiu\nAlvin");
			
			SimpleObj i2 = new SimpleObj();
			i2.setAge(20);
			i2.setId(12341);
			i2.setNumber(33);
			i2.setName("Tom");
			i.setContact1(i2);
			list1.add(i);
		}
		list.add(list1);
		
		list1 = new LinkedList<SimpleObj>();
		for (int j = 0; j < 10; j++) {
			SimpleObj i = new SimpleObj();
			i.setAge(10);
			i.setId(1000 + j);
			i.setNumber(30);
			i.setName("PengtaoQiu\nAlvin");
			
			SimpleObj i2 = new SimpleObj();
			i2.setAge(20);
			i2.setId(12341);
			i2.setNumber(33);
			i2.setName("Tom");
			i.setContact1(i2);
			list1.add(i);
		}
		list.add(list1);
		
		CollectionObj o = new CollectionObj();
		o.setList(list);
		String json = Json.toJson(o);
		
		CollectionObj o2 = Json.toObject(json, CollectionObj.class);
		Assert.assertThat(o2.getList().size(), is(2));
		Assert.assertThat(o2.getList().get(0).size(), is(10));
		Assert.assertThat(o2.getList().get(0).get(1).getId(), is(33443));
		Assert.assertThat(o2.getList().get(1).get(1).getId(), is(1001));
	}
	
	@Test
	public void test6() {
		 MediaContent record = MediaContent.createRecord();
		 String json = Json.toJson(record);
	     
	     MediaContent r = Json.toObject(json, MediaContent.class);
	     Assert.assertThat(r.getMedia().getPlayer(), is(Player.JAVA));
	     Assert.assertThat(r.getImages().size(), is(2));
	     Assert.assertThat(r.getImages().get(0).getSize(), is(Size.LARGE));
	     Assert.assertThat(r.getImages().get(0).getHeight(), is(768));
	}
	
	@Test
	public void test7() {
		ArrayObj obj = new ArrayObj();
		Integer[] i = new Integer[]{2,3,4,5,6,332};
		obj.setNumbers(i);
		
		long[][] map = new long[][]{{3L, 44L, 55L}, {24, 324, 3}};
		obj.setMap(map);
		
		List<User> users = new ArrayList<User>();
		for (int j = 0; j < 3; j++) {
			User user = new User();
			user.setId((long)j);
			user.setName("user" + j);
			users.add(user);
		}
		obj.setUsers(users.toArray(new User[0]));
		
		String json = Json.toJson(obj);
		
		ArrayObj obj2 = Json.toObject(json, ArrayObj.class);
		Assert.assertThat(obj2.getNumbers()[3], is(5));
		Assert.assertThat(obj2.getNumbers().length, is(6));
		Assert.assertThat(obj2.getMap().length, is(2));
		Assert.assertThat(obj2.getMap()[0][1], is(44L));
		Assert.assertThat(obj2.getUsers().length, is(3));
		Assert.assertThat(obj2.getUsers()[0].getId(), is(0L));
		Assert.assertThat(obj2.getUsers()[1].getName(), is("user1"));
	}
	
	@Test
	public void test8() {
		List<User> users = new ArrayList<User>();
		for (int j = 0; j < 3; j++) {
			User user = new User();
			user.setId((long)j);
			user.setName("user" + j);
			users.add(user);
		}
		User[] u = users.toArray(new User[0]);
		String json = Json.toJson(u);
		
		User[] u2 = Json.toObject(json, User[].class);
		Assert.assertThat(u2.length, is(3));
		Assert.assertThat(u2[0].getId(), is(0L));
		Assert.assertThat(u2[1].getName(), is("user1"));
	}
	
	@Test
	public void test9() {
		MapObj m = new MapObj();
		Map<String, Integer> map = new HashMap<String, Integer>();
		map.put("a1", 40);
		m.setMap(map);
		
		Map<String, User[]> userMap = new HashMap<String, User[]>();
		List<User> users = new ArrayList<User>();
		for (int j = 0; j < 3; j++) {
			User user = new User();
			user.setId((long)j);
			user.setName("user" + j);
			users.add(user);
		}
		User[] u = users.toArray(new User[0]);
		userMap.put("user1", u);
		
		users = new ArrayList<User>();
		for (int j = 10; j < 12; j++) {
			User user = new User();
			user.setId((long)j);
			user.setName("user_b" + j);
			users.add(user);
		}
		u = users.toArray(new User[0]);
		userMap.put("user2", u);
		m.setUserMap(userMap);
		
		Map<String, int[]> map3 = new HashMap<String, int[]>();
		map3.put("m31", new int[]{3,4,5,6});
		map3.put("m32", new int[]{7,8,9});
		m.map3 = map3;
		
		String json = Json.toJson(m);
		
		MapObj m2 = Json.toObject(json, MapObj.class);
		Assert.assertThat(m2.getMap().get("a1"), is(40));
		Assert.assertThat(m.getUserMap().get("user1").length, is(3));
		Assert.assertThat(m.getUserMap().get("user2").length, is(2));
		Assert.assertThat(m.getUserMap().get("user2")[0].getName(), is("user_b10"));
		Assert.assertThat(m2.map3.get("m31")[3], is(6));
	}
	
	@Test
	public void test10() throws Throwable {
		DateObj obj = new DateObj();
		obj.setDate(new Date());
		
		StringBuilder strBuilder = new StringBuilder(100);
		for (int i = 0; i < 100; i++) {
			strBuilder.append(i).append("+");
		}
		obj.setByteArr(strBuilder.toString().getBytes("utf-8"));
		
		String json = Json.toJson(obj);
		
		DateObj obj2 = Json.toObject(json, DateObj.class);
		System.out.println(SafeSimpleDateFormat.defaultDateFormat.format(obj2.getDate()));
		Assert.assertThat(new String(obj2.getByteArr(), "utf-8"), is("0+1+2+3+4+5+6+7+8+9+10+11+12+13+14+15+16+17+18+19+20+21+22+23+24+25+26+27+28+29+30+31+32+33+34+35+36+37+38+39+40+41+42+43+44+45+46+47+48+49+50+51+52+53+54+55+56+57+58+59+60+61+62+63+64+65+66+67+68+69+70+71+72+73+74+75+76+77+78+79+80+81+82+83+84+85+86+87+88+89+90+91+92+93+94+95+96+97+98+99+"));
	}
	
	@Test
	public void test11() {
		SimpleObj2 obj = new SimpleObj2();
		obj.setSex('c');
		obj.setSymbol("测试一下".toCharArray());
		
		String json = Json.toJson(obj);
		SimpleObj2 obj2 = Json.toObject(json, SimpleObj2.class);
		Assert.assertThat(obj2.getSex(), is('c'));
		Assert.assertThat(new String(obj2.getSymbol()), is("测试一下"));
	}
	
	@Test
	public void test12() {
		String json = "{\"totalreadtime\":5,\"notecount\":27,\"timeintervalreadtime\":[0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1,4,0,0,0,0,0,0,0],\"bookcollect\":0,\"screenshotshare\":0,\"readbooktype\":{\"测试\":1,\"测试一下\":23},\"bookshare\":0,\"readbookcount\":0,\"noteshare\":0}";
		Profile p = Json.toObject(json, Profile.class);
		Assert.assertThat(p.getTotalreadtime(), is(5));
		Assert.assertThat(p.getNotecount(), is(27));
		Assert.assertThat(p.getTimeintervalreadtime().length, is(24));
		Assert.assertThat(p.getTimeintervalreadtime()[16], is(4));
		Assert.assertThat(p.getReadbooktype().get("测试一下"), is(23));
	}
	
	@Test
	public void testBigNumber() {
		SimpleObj2 s = new SimpleObj2();
		s.setBigDecimal(new BigDecimal("-3.34"));
		s.setBigInteger(new BigInteger("-4"));
		String json = Json.toJson(s);
		System.out.println(json);
		
		SimpleObj2 s2 = Json.toObject(json, SimpleObj2.class);
		Assert.assertThat(s2.getBigDecimal(), is(new BigDecimal("-3.34")));
		Assert.assertThat(s2.getBigInteger(), is(new BigInteger("-4")));
	}
	
	@Test
	public void testEmptyArray() {
		Assert.assertTrue(Json.toObject("[]", Bar[].class) instanceof Bar[]);
	}
	
	@Test
	public void testGeneralJsonObject() {
		String json = "{\"key1\":333, \"key2\" : {\"key3\" : \"hello\", \"key4\":\"world\" }, \"booleanKey\" : true }   ";
		JsonObject jsonObject = Json.toJsonObject(json);
		
		Assert.assertThat(jsonObject.getInteger("key1"), is(333));
		Assert.assertThat(jsonObject.getJsonObject("key2").getString("key3"), is("hello"));
		Assert.assertThat(jsonObject.getJsonObject("key2").getString("key4"), is("world"));
		Assert.assertThat(jsonObject.getBoolean("booleanKey"), is(true));
	}
	
	@Test
	public void testGeneralJsonArray() {
		String json = "[333,444,{\"key\" : \"hello\"},666]";
		JsonArray array = Json.toJsonArray(json);
		
		Assert.assertThat(array.getInteger(0), is(333));
		Assert.assertThat(array.getInteger(1), is(444));
		Assert.assertThat(array.getJsonObject(2).getString("key"), is("hello"));
		Assert.assertThat(array.getInteger(3), is(666));
	}
	
	@Test
	public void testMixedGeneralJsonArrayAndJsonObject() {
		String json = "[333,444,{\"key\" : \"hello\", \"keyObject\" : [\"object0\",\"object1\"  ]},666]";
		JsonArray array = Json.toJsonArray(json);
		
		Assert.assertThat(array.getJsonObject(2).getJsonArray("keyObject").getString(0), is("object0"));
		Assert.assertThat(array.getJsonObject(2).getJsonArray("keyObject").getString(1), is("object1"));
		
		json = "{\"key1\":333, \"arrayKey\":[444, \"array\"], \"key2\" :  {\"key3\" : \"hello\", \"key4\":\"world\" }, \"booleanKey\" : true }   ";
		JsonObject jsonObject = Json.toJsonObject(json);
		Assert.assertThat(jsonObject.getJsonArray("arrayKey").getString(1), is("array"));
		Assert.assertThat(jsonObject.getJsonObject("key2").getString("key4"), is("world"));
	}
	
	@Test
	public void testDateFormat() {
		Calendar cal = Calendar.getInstance();
		cal.set(2015, Calendar.JANUARY, 25, 14, 53, 12);
		
		DateFormatObject obj = new DateFormatObject();
		System.out.println(obj);
		obj.init(cal);
		String json = Json.toJson(obj);
		System.out.println(json);
		
		DateFormatObject obj2 = Json.toObject(json, DateFormatObject.class);
		System.out.println(obj2);
		
		Assert.assertThat(obj2.title, is(obj.title));
		
		Calendar calDateDefault = Calendar.getInstance();
		calDateDefault.setTime(obj2.getDateDefault());
		Assert.assertThat(calDateDefault.get(Calendar.YEAR), is(cal.get(Calendar.YEAR)));
		Assert.assertThat(calDateDefault.get(Calendar.MONTH), is(cal.get(Calendar.MONTH)));
		Assert.assertThat(calDateDefault.get(Calendar.DAY_OF_MONTH), is(cal.get(Calendar.DAY_OF_MONTH)));
		Assert.assertThat(calDateDefault.get(Calendar.HOUR_OF_DAY), is(cal.get(Calendar.HOUR_OF_DAY)));
		Assert.assertThat(calDateDefault.get(Calendar.MINUTE), is(cal.get(Calendar.MINUTE)));
		Assert.assertThat(calDateDefault.get(Calendar.SECOND), is(cal.get(Calendar.SECOND)));
		
		Calendar dateFieldDefaultFormat = Calendar.getInstance();
		dateFieldDefaultFormat.setTime(obj2.getDateFieldDefaultFormat());
		Assert.assertThat(dateFieldDefaultFormat.get(Calendar.YEAR), is(cal.get(Calendar.YEAR)));
		Assert.assertThat(dateFieldDefaultFormat.get(Calendar.MONTH), is(cal.get(Calendar.MONTH)));
		Assert.assertThat(dateFieldDefaultFormat.get(Calendar.DAY_OF_MONTH), is(cal.get(Calendar.DAY_OF_MONTH)));
		Assert.assertThat(dateFieldDefaultFormat.get(Calendar.HOUR_OF_DAY), is(cal.get(Calendar.HOUR_OF_DAY)));
		Assert.assertThat(dateFieldDefaultFormat.get(Calendar.MINUTE), is(cal.get(Calendar.MINUTE)));
		Assert.assertThat(dateFieldDefaultFormat.get(Calendar.SECOND), is(cal.get(Calendar.SECOND)));

		Calendar dateFieldTimestamp = Calendar.getInstance();
		dateFieldTimestamp.setTime(obj2.getDateFieldTimestamp());
		Assert.assertThat(dateFieldTimestamp.get(Calendar.YEAR), is(cal.get(Calendar.YEAR)));
		Assert.assertThat(dateFieldTimestamp.get(Calendar.MONTH), is(cal.get(Calendar.MONTH)));
		Assert.assertThat(dateFieldTimestamp.get(Calendar.DAY_OF_MONTH), is(cal.get(Calendar.DAY_OF_MONTH)));
		Assert.assertThat(dateFieldTimestamp.get(Calendar.HOUR_OF_DAY), is(cal.get(Calendar.HOUR_OF_DAY)));
		Assert.assertThat(dateFieldTimestamp.get(Calendar.MINUTE), is(cal.get(Calendar.MINUTE)));
		Assert.assertThat(dateFieldTimestamp.get(Calendar.SECOND), is(cal.get(Calendar.SECOND)));

		Calendar dateFieldFormat1 = Calendar.getInstance();
		dateFieldFormat1.setTime(obj2.dateFieldFormat1);
		Assert.assertThat(dateFieldFormat1.get(Calendar.YEAR), is(cal.get(Calendar.YEAR)));
		Assert.assertThat(dateFieldFormat1.get(Calendar.MONTH), is(cal.get(Calendar.MONTH)));
		Assert.assertThat(dateFieldFormat1.get(Calendar.DAY_OF_MONTH), is(cal.get(Calendar.DAY_OF_MONTH)));
		Assert.assertThat(dateFieldFormat1.get(Calendar.HOUR_OF_DAY), is(cal.get(Calendar.HOUR_OF_DAY)));
		Assert.assertThat(dateFieldFormat1.get(Calendar.MINUTE), is(cal.get(Calendar.MINUTE)));
		Assert.assertThat(dateFieldFormat1.get(Calendar.SECOND), is(cal.get(Calendar.SECOND)));
		
		Calendar cal2 = Calendar.getInstance();
		cal2.setTime(obj2.getDateMethodFormat());
		Assert.assertThat(cal2.get(Calendar.YEAR), is(cal.get(Calendar.YEAR)));
		Assert.assertThat(cal2.get(Calendar.MONTH), is(cal.get(Calendar.MONTH)));
		Assert.assertThat(cal2.get(Calendar.DAY_OF_MONTH), is(cal.get(Calendar.DAY_OF_MONTH)));
		Assert.assertThat(cal2.get(Calendar.HOUR_OF_DAY), is(0));
		Assert.assertThat(cal2.get(Calendar.MINUTE), is(0));
		Assert.assertThat(cal2.get(Calendar.SECOND), is(0));
	}
	
	@Test
	public void testSpecialPropertyObject() {
		SpecialPropertyObject s = new SpecialPropertyObject();
		System.out.println(s);
		s.init();
		String json = Json.toJson(s);
		System.out.println(json);
		
		SpecialPropertyObject s2 = Json.toObject(json, SpecialPropertyObject.class);
		System.out.println(s2);
		
		Assert.assertThat(s2.getiOS(), is(s.getiOS()));
		Assert.assertThat(s2.getiPad(), is(s.getiPad()));
		Assert.assertThat(s2.getiPhone(), is(s.getiPhone()));
		Assert.assertThat(s2.isiText(), is(s.isiText()));
		
		Assert.assertThat(s2.aOS, is(s.aOS));
		Assert.assertThat(s2.aPad, is(s.aPad));
		Assert.assertThat(s2.aPhone, is(s.aPhone));
		Assert.assertThat(s2.aText, is(s.aText));
	}
	
	public static void main3(String[] args) {
		String json = "{  \"key1\":333, \"arrayKey\":[444, \"array\"], \"key2\" :  {\"key3\" : \"hello\", \"key4\":\"world\" }, \"booleanKey\" : true }   ";
		JsonObject jsonObject = Json.toJsonObject(json);
		System.out.println(jsonObject.getJsonArray("arrayKey"));
		System.out.println(jsonObject.getJsonObject("key2").getString("key4"));
	}
	
	public static void main4(String[] args) {
		Date date = new Date();
		System.out.println(date.getClass() == Date.class);
		System.out.println(Date.class.isAssignableFrom(java.sql.Date.class));
		System.out.println(date.getClass() == new Date().getClass());
	}
	
	public static void main5(String[] args) {
		Calendar cal = Calendar.getInstance();
		cal.set(2015, Calendar.JANUARY, 25, 14, 53, 12);
		System.out.println(cal.get(Calendar.HOUR_OF_DAY));
		
		DateFormatObject obj = new DateFormatObject();
		System.out.println(obj);
		obj.init(cal);
		String json = Json.toJson(obj);
		System.out.println(json);
		
		DateFormatObject obj2 = Json.toObject(json, DateFormatObject.class);
		System.out.println(obj2);
	}
	
	public static void main(String[] args) {
		SpecialPropertyObject s = new SpecialPropertyObject();
		System.out.println(s);
		s.init();
		String json = Json.toJson(s);
		System.out.println(json);
		
		SpecialPropertyObject s2 = Json.toObject(json, SpecialPropertyObject.class);
		System.out.println(s2);
	}
	
	public static void main2(String[] args) {
//		new TestParser().testBigNumber();
//		Bar[] arr = Json.toObject("[]", Bar[].class);
//		System.out.println(arr);
		
//		char ch = (char)31, 
//			ch1 = (char)1, 
//			ch2 = (char)0,
//			ch3 = (char)15,
//			ch4 = (char)16;
//		System.out.println(JsonStringWriter.escapeSpecialCharacter(ch3));
		
	}

}
