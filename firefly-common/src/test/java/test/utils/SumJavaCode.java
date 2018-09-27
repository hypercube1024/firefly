package test.utils;

import java.io.*;

public class SumJavaCode {
    private long normalLines = 0; // 空行
    private long commentLines = 0; // 注释行
    private long whiteLines = 0; // 代码行

    public static void main(String[] args) {
        System.out.println(args[0]);
        SumJavaCode sjc = new SumJavaCode();
        File f = new File(args[0] + "/firefly-project/firefly-common");
        System.out.println(f.getName());
        sjc.treeFile(f);
        System.out.println("空行：" + sjc.getWhiteLines());
        System.out.println("注释行：" + sjc.getCommentLines());
        System.out.println("代码行：" + sjc.getNormalLines());

        sjc = new SumJavaCode();
        f = new File(args[0] + "/firefly-project/firefly");
        System.out.println(f.getName());
        sjc.treeFile(f);
        System.out.println("空行：" + sjc.getWhiteLines());
        System.out.println("注释行：" + sjc.getCommentLines());
        System.out.println("代码行：" + sjc.getNormalLines());

        sjc = new SumJavaCode();
        f = new File(args[0] + "/firefly-project/firefly-nettool");
        System.out.println(f.getName());
        sjc.treeFile(f);
        System.out.println("空行：" + sjc.getWhiteLines());
        System.out.println("注释行：" + sjc.getCommentLines());
        System.out.println("代码行：" + sjc.getNormalLines());

    }

    /**
     * 查找出一个目录下所有的.java文件
     *
     * @param f 要查找的目录
     */
    private void treeFile(File f) {
        f.listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                if (!file.isDirectory()) {
                    if (file.getName().matches(".*\\.java$")) {
                        sumCode(file);
                    }
                } else {
                    treeFile(file);
                }
                return false;
            }
        });
    }

    /**
     * 计算一个.java文件中的代码行，空行，注释行
     *
     * @param file 要计算的.java文件
     */
    private void sumCode(File file) {
        BufferedReader br = null;
        boolean comment = false;
        try {
            br = new BufferedReader(new FileReader(file));
            String line = "";
            try {
                while ((line = br.readLine()) != null) {
                    line = line.trim();
                    if (line.matches("^[\\s&&[^\\n]]*$")) {
                        whiteLines++;
                    } else if (line.startsWith("/*") && !line.endsWith("*/")) {
                        commentLines++;
                        comment = true;
                    } else if (true == comment) {
                        commentLines++;
                        if (line.endsWith("*/")) {
                            comment = false;
                        }
                    } else if (line.startsWith("//")) {
                        commentLines++;
                    } else {
                        normalLines++;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                    br = null;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    public long getNormalLines() {
        return normalLines;
    }

    public long getCommentLines() {
        return commentLines;
    }

    public long getWhiteLines() {
        return whiteLines;
    }


}
