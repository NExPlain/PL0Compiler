package pl0compiler.pcode;

import pl0compiler.Compiler;
import pl0compiler.errorHandler.PL0Exception;
import pl0compiler.utils.Pcode;

import java.io.IOException;

/**
 * 生成Pcode的虚拟机
 * Created by lizhen on 14/12/13.
 */
public class PcodeVM {
    //pcode数组上限
    private static final int cxmax = 1000000;               // 为了编译大数据设置的较大
    //虚拟机代码指针，存放数据范围[0,cx-1]
    public int cx = 0;
    //存放虚拟机代码的数组
    public Pcode[] code;

    public PcodeVM() {
        code = new Pcode[cxmax];
        cx = 0;
    }

    /**
     * 生成虚拟机代码
     *
     * @param f Pcodeuction.f
     * @param l Pcodeuction.l
     * @param a Pcodeuction.a
     */
    public void gen(int f, int l, int a) throws PL0Exception {
        if(pl0compiler.Compiler.parser.scan.isfileEneded)return;
        if (cx >= cxmax) {                                                                          //超出堆栈的上限
            throw new PL0Exception(38);
        }
        code[cx] = new Pcode(f, l, a);
        cx ++;
    }

    /**
     * 输出pcode符号表
     */
    public void listcode(int cx0) {
        try {
            Compiler.outputWriter.write("\n\nPCODE :\n");
            Compiler.outputWriter.write("****************************\n");
            Compiler.outputWriter.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }for (int i = cx0; i < cx; i++) {
            try {
                String buff = i + " " + Pcode.pcode[code[i].f] + " " + code[i].l + " " + code[i].a;                //形如: lit l,a
                Compiler.pcodeWriter.write(buff + '\n');
                Compiler.pcodeWriter.flush();
                Compiler.outputWriter.write(buff + '\n');
                Compiler.outputWriter.flush();
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("error in pcode out put!");
            }
        }
    }
}
