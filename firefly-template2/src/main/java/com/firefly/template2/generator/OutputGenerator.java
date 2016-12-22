package com.firefly.template2.generator;

import com.firefly.template2.Configuration;
import com.firefly.template2.parser.Template2Parser;
import com.firefly.utils.StringUtils;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.Optional;

/**
 * @author Pengtao Qiu
 */
public class OutputGenerator extends AbstractJavaGenerator<Template2Parser.OutputContext> {

    public OutputGenerator(Configuration configuration) {
        super(configuration);
    }

    @Override
    public void enter(Template2Parser.OutputContext node, Writer writer, Object... args) {
        Optional<Token> opt = node.children.stream()
                                           .filter(n -> n instanceof TerminalNode)
                                           .map(n -> (TerminalNode) n)
                                           .map(TerminalNode::getSymbol)
                                           .findFirst();

        if (opt.isPresent()) {
            Token token = opt.get();
            token2OutputString(token, writer, args);
        }
    }

    @Override
    public void exit(Template2Parser.OutputContext node, Writer writer, Object... args) {

    }

    private void token2OutputString(Token token, Writer writer, Object... args) {
        switch (token.getType()) {
            case Template2Parser.OutputString: {
                String c = token.getText().substring(2, token.getText().length() - 2);
                if (StringUtils.hasText(c)) {
                    generateOutputJava(strEscape(c), writer, args); // TODO process bean access
                }
            }
            break;
            case Template2Parser.EscapeOutputString: {
                String c = token.getText().substring(3, token.getText().length() - 3);
                if (StringUtils.hasText(c)) {
                    generateOutputJava(strEscape(c), writer, args);
                }
            }
            break;
            case Template2Parser.OutputNewLine:
                generateOutputJava(strEscape(configuration.getLineSeparator()), writer, args);
                break;
            case Template2Parser.OutputSpace:
                generateOutputJava(strEscape(" "), writer, args);
                break;
        }
    }

    private void generateOutputJava(String str, Writer writer, Object... args) {
        try {
            generateBlank(writer, args);
            Integer stringId = (Integer) args[1];
            List<String> list = (List<String>) args[2];
            writer.append("out.write(_s").append(stringId.toString()).append(");")
                  .append(configuration.getLineSeparator());
            list.add("private static final byte[] _s" + stringId
                    + " = getBytes(" + str + ", \"" + configuration.getOutputJavaFileCharset() + "\");"
                    + configuration.getLineSeparator());
        } catch (IOException e) {
            log.error("generate output string exception", e);
        }
    }

    private String strEscape(String str) {
        StringBuilder sb = new StringBuilder(str.length() * 2);
        sb.append('"');

        for (int i = 0; i < str.length(); i++) {
            char ch = str.charAt(i);
            switch (ch) {
                case '\b':
                    sb.append('\\');
                    sb.append('b');
                    break;
                case '\n':
                    sb.append('\\');
                    sb.append('n');
                    break;
                case '\r':
                    sb.append('\\');
                    sb.append('r');
                    break;
                case '\f':
                    sb.append('\\');
                    sb.append('f');
                    break;
                case '\\':
                    sb.append('\\');
                    sb.append('\\');
                    break;
                case '"':
                    sb.append('\\');
                    sb.append('"');
                    break;
                case '\t':
                    sb.append('\\');
                    sb.append('t');
                    break;

                default:
                    sb.append(ch);
                    break;
            }
        }

        sb.append('"');
        return sb.toString();
    }
}
