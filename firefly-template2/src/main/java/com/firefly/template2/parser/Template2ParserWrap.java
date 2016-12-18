package com.firefly.template2.parser;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CommonTokenStream;

import java.io.File;

/**
 * @author Pengtao Qiu
 */
public class Template2ParserWrap {

    private File file;
    private CharStream charStream;
    private Template2Lexer lexer;
    private CommonTokenStream tokenStream;
    private Template2Parser parser;

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }

    public CharStream getCharStream() {
        return charStream;
    }

    public void setCharStream(CharStream charStream) {
        this.charStream = charStream;
    }

    public Template2Lexer getLexer() {
        return lexer;
    }

    public void setLexer(Template2Lexer lexer) {
        this.lexer = lexer;
    }

    public CommonTokenStream getTokenStream() {
        return tokenStream;
    }

    public void setTokenStream(CommonTokenStream tokenStream) {
        this.tokenStream = tokenStream;
    }

    public Template2Parser getParser() {
        return parser;
    }

    public void setParser(Template2Parser parser) {
        this.parser = parser;
    }

}
