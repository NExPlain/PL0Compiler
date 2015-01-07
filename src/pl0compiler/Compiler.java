package pl0compiler;

import pl0compiler.syntaxAnalysis.Parser;

import java.io.*;

public class Compiler {

    public static final String pcodeFilePrefix = "./src/samples/pcode";
    public static final String outputFilePrefix = "./src/samples/output";
    public static final String inputFilePrefix ="./src/samples/input";
    public static BufferedWriter pcodeWriter;                   //输出虚拟机代码
    public static BufferedWriter outputWriter;                 //输出结果

    public static Parser parser;

    public Compiler() {
    }

    void clean(String filepath)
    {
        File cleanfile = new File(filepath);
        String[] cleanfilelist = cleanfile.list();
        for(int i = 0 ; i < cleanfilelist.length ; i ++){
            String fp = cleanfilelist[i];
            String path = filepath + "/" + fp;
            File tf = new File(path);
            tf.delete();
        }
    }

    public boolean compile() {
        clean(outputFilePrefix);
        clean(pcodeFilePrefix);
        File file = new File(inputFilePrefix);
        String[] filelist = file.list();
        for(int i = 0 ;i < filelist.length ; i ++) {
            try {
                String add = "/" + filelist[i];
                parser = new Parser(inputFilePrefix + add);
                pcodeWriter = new BufferedWriter(new FileWriter(pcodeFilePrefix + add));
                outputWriter = new BufferedWriter(new FileWriter(outputFilePrefix + add));
                parser.scan.getch();
                parser.getsym();
                parser.start();
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("compile error");
            }
            System.out.println("compile completed");
            if (parser.err.errCnt == 0) {
                try {
                    outputWriter.write("Accepted\n");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            try {
                pcodeWriter.close();
                outputWriter.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return (parser.err.errCnt == 0);
    }
}
