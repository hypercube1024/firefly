package com.firefly.template2.utils;

import com.firefly.template2.Configuration;
import com.firefly.template2.parser.Template2Lexer;
import com.firefly.template2.parser.Template2Parser;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Lexer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;

/**
 * @author Pengtao Qiu
 */
public class Template2ParserHelper {

    private static final Logger log = LoggerFactory.getLogger("firefly-system");

    private Configuration configuration;

    public Template2ParserHelper() {
    }

    public Template2ParserHelper(Configuration configuration) {
        this.configuration = configuration;
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }

    public CharStream createCharStream(File file) {
        try (Reader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), configuration.getTemplateCharset()))) {
            return new ANTLRInputStream(reader);
        } catch (IOException e) {
            log.error("create char stream exception", e);
            throw new RuntimeException("create char stream exception", e);
        }
    }

    public Template2Lexer createLexer(CharStream input) {
        return new Template2Lexer(input);
    }

    public CommonTokenStream createCommonTokenStream(Lexer lexer) {
        return new CommonTokenStream(lexer);
    }

    public Template2Parser createParser(CommonTokenStream tokenStream) {
        return new Template2Parser(tokenStream);
    }

    public Template2ParserWrap createTemplate2ParserWrap(File file) {
        Template2ParserWrap parserWrap = new Template2ParserWrap();
        parserWrap.setFile(file);
        parserWrap.setCharStream(createCharStream(file));
        parserWrap.setLexer(createLexer(parserWrap.getCharStream()));
        parserWrap.setTokenStream(createCommonTokenStream(parserWrap.getLexer()));
        parserWrap.setParser(createParser(parserWrap.getTokenStream()));
        return parserWrap;
    }

}
