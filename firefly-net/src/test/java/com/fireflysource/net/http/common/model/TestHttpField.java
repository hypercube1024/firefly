package com.fireflysource.net.http.common.model;


import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;


class TestHttpField {

    @Test
    void testContainsSimple() {
        HttpField field = new HttpField("name", "SomeValue");
        assertTrue(field.contains("somevalue"));
        assertTrue(field.contains("sOmEvAlUe"));
        assertTrue(field.contains("SomeValue"));
        assertFalse(field.contains("other"));
        assertFalse(field.contains("some"));
        assertFalse(field.contains("Some"));
        assertFalse(field.contains("value"));
        assertFalse(field.contains("v"));
        assertFalse(field.contains(""));
        assertFalse(field.contains(null));

        field = new HttpField(HttpHeader.CONNECTION, "Upgrade, HTTP2-Settings");
        assertTrue(field.contains("Upgrade"));
        assertTrue(field.contains("HTTP2-Settings"));
    }

    @Test
    void testCaseInsensitiveHashcode_KnownField() {
        HttpField fieldFoo1 = new HttpField("Cookie", "foo");
        HttpField fieldFoo2 = new HttpField("cookie", "foo");

        assertEquals(fieldFoo2.hashCode(), fieldFoo1.hashCode());
    }

    @Test
    void testCaseInsensitiveHashcode_UnknownField() {
        HttpField fieldFoo1 = new HttpField("X-Foo", "bar");
        HttpField fieldFoo2 = new HttpField("x-foo", "bar");

        assertEquals(fieldFoo2.hashCode(), fieldFoo1.hashCode());
    }

    @Test
    void testContainsList() {
        HttpField field = new HttpField("name", ",aaa,Bbb,CCC, ddd , e e, \"\\\"f,f\\\"\", ");
        assertTrue(field.contains("aaa"));
        assertTrue(field.contains("bbb"));
        assertTrue(field.contains("ccc"));
        assertTrue(field.contains("Aaa"));
        assertTrue(field.contains("Bbb"));
        assertTrue(field.contains("Ccc"));
        assertTrue(field.contains("AAA"));
        assertTrue(field.contains("BBB"));
        assertTrue(field.contains("CCC"));
        assertTrue(field.contains("ddd"));
        assertTrue(field.contains("e e"));
        assertTrue(field.contains("\"f,f\""));
        assertFalse(field.contains(""));
        assertFalse(field.contains("aa"));
        assertFalse(field.contains("bb"));
        assertFalse(field.contains("cc"));
        assertFalse(field.contains(null));
    }

    @Test
    void testQualityContainsList() {
        HttpField field;

        field = new HttpField("name", "yes");
        assertTrue(field.contains("yes"));
        assertFalse(field.contains("no"));

        field = new HttpField("name", ",yes,");
        assertTrue(field.contains("yes"));
        assertFalse(field.contains("no"));

        field = new HttpField("name", "other,yes,other");
        assertTrue(field.contains("yes"));
        assertFalse(field.contains("no"));

        field = new HttpField("name", "other,  yes  ,other");
        assertTrue(field.contains("yes"));
        assertFalse(field.contains("no"));

        field = new HttpField("name", "other,  y s  ,other");
        assertTrue(field.contains("y s"));
        assertFalse(field.contains("no"));

        field = new HttpField("name", "other,  \"yes\"  ,other");
        assertTrue(field.contains("yes"));
        assertFalse(field.contains("no"));

        field = new HttpField("name", "other,  \"\\\"yes\\\"\"  ,other");
        assertTrue(field.contains("\"yes\""));
        assertFalse(field.contains("no"));

        field = new HttpField("name", ";no,yes,;no");
        assertTrue(field.contains("yes"));
        assertFalse(field.contains("no"));

        field = new HttpField("name", "no;q=0,yes;q=1,no; q = 0");
        assertTrue(field.contains("yes"));
        assertFalse(field.contains("no"));

        field = new HttpField("name", "no;q=0.0000,yes;q=0.0001,no; q = 0.00000");
        assertTrue(field.contains("yes"));
        assertFalse(field.contains("no"));

        field = new HttpField("name", "no;q=0.0000,Yes;Q=0.0001,no; Q = 0.00000");
        assertTrue(field.contains("yes"));
        assertFalse(field.contains("no"));

    }

    @Test
    void testValues() {
        String[] values = new HttpField("name", "value").getValues();
        assertEquals(1, values.length);
        assertEquals("value", values[0]);

        values = new HttpField("name", "a,b,c").getValues();
        assertEquals(3, values.length);
        assertEquals("a", values[0]);
        assertEquals("b", values[1]);
        assertEquals("c", values[2]);

        values = new HttpField("name", "a,\"x,y,z\",c").getValues();
        assertEquals(3, values.length);
        assertEquals("a", values[0]);
        assertEquals("x,y,z", values[1]);
        assertEquals("c", values[2]);

        values = new HttpField("name", "a,\"x,\\\"p,q\\\",z\",c").getValues();
        assertEquals(3, values.length);
        assertEquals("a", values[0]);
        assertEquals("x,\"p,q\",z", values[1]);
        assertEquals("c", values[2]);

    }
}
