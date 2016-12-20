package com.firefly.template2.generator;

import com.firefly.template2.Configuration;
import com.firefly.template2.parser.Template2Parser;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

/**
 * @author Pengtao Qiu
 */
public class OutputGenerator extends AbstractJavaGenerator<Template2Parser.OutputContext> {

    public OutputGenerator(Configuration configuration) {
        super(configuration);
    }

    @Override
    public void enter(Template2Parser.OutputContext node, Writer writer, Object... args) {
        Integer stringId = (Integer) args[1];
        List<String> list = (List<String>) args[2];
        for (ParseTree o : node.children) {
            if (o instanceof TerminalNode) {
                TerminalNode terminalNode = (TerminalNode) o;
                Token token = terminalNode.getSymbol();
                try {
                    String s = null;
                    switch (token.getType()) {
                        case Template2Parser.OutputString:
                            s = strEscape(token.getText().substring(2, token.getText().length() - 2));
                            break;
                        case Template2Parser.OutputStringWithNewLine:
                            s = strEscape(token.getText().substring(3, token.getText().length() - 3));
                            break;
                        case Template2Parser.OutputNewLine:
                            s = strEscape(configuration.getLineSeparator());
                            break;
                    }
                    if (s != null) {
                        generateBlank(writer, args);
                        writer.append("out.write(_s").append(stringId.toString()).append(");").append(configuration.getLineSeparator());
                        list.add("private byte[] _s" + stringId + " = toBytes(" + s + ", \"" + configuration.getOutputJavaFileCharset() + "\");" + configuration.getLineSeparator());
                    }
                } catch (IOException e) {
                    log.error("generate output string exception", e);
                }
            }
        }
    }

    @Override
    public void exit(Template2Parser.OutputContext node, Writer writer, Object... args) {

    }

    public String strEscape(String str) {
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
