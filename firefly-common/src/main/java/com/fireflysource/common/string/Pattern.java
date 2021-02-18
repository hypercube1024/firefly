package com.fireflysource.common.string;

public abstract class Pattern {

    private static class Holder {
        private static final AllMatch ALL_MATCH = new AllMatch();
    }

    /**
     * Matches a string according to the specified pattern
     *
     * @param str Target string
     * @return If it returns null, that represents matching failure,
     * else it returns an array contains all strings matched.
     */
    abstract public String[] match(String str);

    public static Pattern compile(String pattern, String wildcard) {
        final boolean startWith = pattern.startsWith(wildcard);
        final boolean endWith = pattern.endsWith(wildcard);
        final String[] array = StringUtils.split(pattern, wildcard);

        switch (array.length) {
            case 0:
                return Holder.ALL_MATCH;
            case 1:
                if (startWith && endWith)
                    return new HeadAndTailMatch(array[0]);

                if (startWith)
                    return new HeadMatch(array[0]);

                if (endWith)
                    return new TailMatch(array[0]);

                return new EqualsMatch(pattern);
            default:
                return new MultipartMatch(startWith, endWith, array);
        }
    }


    private static class MultipartMatch extends Pattern {

        private final boolean startWith, endWith;
        private final String[] parts;
        private int num;

        MultipartMatch(boolean startWith, boolean endWith, String[] parts) {
            super();
            this.startWith = startWith;
            this.endWith = endWith;
            this.parts = parts;
            num = parts.length - 1;
            if (startWith)
                num++;
            if (endWith)
                num++;
        }

        @Override
        public String[] match(String str) {
            int currentIndex = -1;
            int lastIndex = -1;
            String[] ret = new String[num];

            for (int i = 0; i < parts.length; i++) {
                String part = parts[i];
                int j = startWith ? i : i - 1;
                currentIndex = str.indexOf(part, lastIndex + 1);

                if (currentIndex > lastIndex) {
                    if (i != 0 || startWith)
                        ret[j] = str.substring(lastIndex + 1, currentIndex);

                    lastIndex = currentIndex + part.length() - 1;
                    continue;
                }
                return null;
            }

            if (endWith)
                ret[num - 1] = str.substring(lastIndex + 1);

            return ret;
        }

    }

    private static class TailMatch extends Pattern {
        private final String part;

        TailMatch(String part) {
            this.part = part;
        }

        @Override
        public String[] match(String str) {
            int currentIndex = str.indexOf(part);
            if (currentIndex == 0) {
                return new String[]{str.substring(part.length())};
            }
            return null;
        }
    }

    private static class HeadMatch extends Pattern {
        private final String part;

        HeadMatch(String part) {
            this.part = part;
        }

        @Override
        public String[] match(String str) {
            int currentIndex = str.indexOf(part);
            if (currentIndex != -1 && currentIndex + part.length() == str.length()) {
                try {
                    return new String[]{str.substring(0, currentIndex)};
                } catch (Exception e) {
                    e.printStackTrace();
                    System.out.println(str + ", " + currentIndex + ", " + part);
                }
            }
            return null;
        }


    }

    private static class HeadAndTailMatch extends Pattern {
        private final String part;

        HeadAndTailMatch(String part) {
            this.part = part;
        }

        @Override
        public String[] match(String str) {
            int currentIndex = str.indexOf(part);
            if (currentIndex >= 0) {
                return new String[]{str.substring(0, currentIndex), str.substring(currentIndex + part.length())};
            }
            return null;
        }
    }

    private static class EqualsMatch extends Pattern {
        private final String pattern;

        EqualsMatch(String pattern) {
            this.pattern = pattern;
        }

        @Override
        public String[] match(String str) {
            return pattern.equals(str) ? new String[0] : null;
        }
    }

    private static class AllMatch extends Pattern {

        @Override
        public String[] match(String str) {
            return new String[]{str};
        }
    }
}
