package pl0compiler;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class PL0 {

    public static final String pcodeFilePrefix = "./src/test/pcode";
    public static final String tableFilePrefix = "./src/test/table";
    public static final String runtimeFilePrefix = "./src/test/runtime";
    public static final String errFilePrefix = "./src/test/error";
    public static final String inputFilePrefix ="./src/test/input";
    public static BufferedWriter pcodeWriter;                   //输出虚拟机代码
    public static BufferedWriter outputWriter;                 //输出结果
    public static BufferedWriter tableWriter;                   //输出名字表
    public static BufferedWriter errWriter;                     //输出错误信息

    public static Parser parser;

    public PL0() {
    }

    public boolean compile() {
        File file = new File(inputFilePrefix);
        String[] filelist = file.list();
        for(int i = 0 ;i < filelist.length ; i ++) {
            System.out.println(filelist[i] + "!!!!!");
            try {
                String add = "/" + filelist[i];
                parser = new Parser(inputFilePrefix + add);
                pcodeWriter = new BufferedWriter(new FileWriter(pcodeFilePrefix + add));
                tableWriter = new BufferedWriter(new FileWriter(tableFilePrefix + add));
                outputWriter = new BufferedWriter(new FileWriter(runtimeFilePrefix + add));
                errWriter = new BufferedWriter(new FileWriter(errFilePrefix + add));
                parser.scan.getch();
                parser.getsym();                                      //前瞻分析需要预先读入一个符号
                parser.start();                                        //开始语法分析过程（连同语法检查，目标代码生成）
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("compile error");
            }
            System.out.println("compile completed");
            if (parser.err.errCnt == 0) {
                try {
                    errWriter.write("Accepted\n");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            try {
                pcodeWriter.close();
                tableWriter.close();
                outputWriter.close();
                errWriter.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return (parser.err.errCnt == 0);
    }
}
