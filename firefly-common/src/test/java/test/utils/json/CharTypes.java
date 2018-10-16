package test.utils.json;

public class CharTypes {
    private final static boolean[] specicalFlags_doubleQuotes = new boolean[((int) '\\' + 1)];
    private final static char[] replaceChars = new char[((int) '\\' + 1)];

    static {
        specicalFlags_doubleQuotes['\b'] = true;
        specicalFlags_doubleQuotes['\n'] = true;
        specicalFlags_doubleQuotes['\t'] = true;
        specicalFlags_doubleQuotes['\f'] = true;
        specicalFlags_doubleQuotes['\r'] = true;
        specicalFlags_doubleQuotes['\"'] = true;
        specicalFlags_doubleQuotes['\\'] = true;
        specicalFlags_doubleQuotes['/'] = true;

        replaceChars['\b'] = 'b';
        replaceChars['\n'] = 'n';
        replaceChars['\t'] = 't';
        replaceChars['\f'] = 'f';
        replaceChars['\r'] = 'r';
        replaceChars['\"'] = '"';
        replaceChars['\''] = '\'';
        replaceChars['\\'] = '\\';
        replaceChars['/'] = '/';

    }

    public static String replaceSpecicalFlags(String s) {
        StringBuilder sb = new StringBuilder(s.length() + 10);
        char[] cs = s.toCharArray();
        for (char ch : cs) {
            if (isSpecicalFlags(ch)) {
                sb.append('\\');
                sb.append(replaceChar(ch));
            } else {
                sb.append(ch);
            }
        }
        return sb.toString();
    }

    public static String replaceSpecicalFlags2(String s) {
        StringBuilder sb = new StringBuilder(s.length() + 10);
        char[] cs = s.toCharArray();
        for (char c : cs) {
            switch (c) {
                case '"':
                    sb.append("\\\"");
                    break;
                case '\b':
                    sb.append("\\b");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                case '\f':
                    sb.append("\\f");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\\':
                    sb.append("\\\\");
                    break;
                default:
                    sb.append(c);
            }
        }
        return sb.toString();
    }

    public static boolean isSpecicalFlags(char ch) {
        return ch < specicalFlags_doubleQuotes.length
                && specicalFlags_doubleQuotes[ch];
    }

    public static char replaceChar(char ch) {
        return replaceChars[(int) ch];
    }

    public static void main(String[] args) {
        // System.out.println(replaceChars.length);
//		for (char c : replaceChars) {
//			if ((int) c != 0)
//				System.out.print((int) c + " ");
//		}
        String s = "dfdf\t";
        String r = null;
        long start = System.currentTimeMillis();
        for (int i = 0; i < 100000; i++) {
            r = replaceSpecicalFlags(s);
        }
        long end = System.currentTimeMillis();
        System.out.println(end - start);
        System.out.println(r);

        start = System.currentTimeMillis();
        for (int i = 0; i < 100000; i++) {
            r = replaceSpecicalFlags2(s);
        }
        end = System.currentTimeMillis();
        System.out.println(end - start);
        System.out.println(r);

    }
}
