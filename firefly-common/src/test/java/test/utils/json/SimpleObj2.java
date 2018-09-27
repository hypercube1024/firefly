package test.utils.json;

import java.math.BigDecimal;
import java.math.BigInteger;

public class SimpleObj2 {
    private Integer id;
    private User user;
    private Book book;
    private char sex;
    private char[] symbol;
    private BigDecimal bigDecimal;
    private BigInteger bigInteger;

    public BigDecimal getBigDecimal() {
        return bigDecimal;
    }

    public void setBigDecimal(BigDecimal bigDecimal) {
        this.bigDecimal = bigDecimal;
    }

    public BigInteger getBigInteger() {
        return bigInteger;
    }

    public void setBigInteger(BigInteger bigInteger) {
        this.bigInteger = bigInteger;
    }

    public char[] getSymbol() {
        return symbol;
    }

    public void setSymbol(char[] symbol) {
        this.symbol = symbol;
    }

    public char getSex() {
        return sex;
    }

    public void setSex(char sex) {
        this.sex = sex;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Book getBook() {
        return book;
    }

    public void setBook(Book book) {
        this.book = book;
    }

}
