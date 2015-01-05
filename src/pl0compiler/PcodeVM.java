package pl0compiler;

import java.io.*;
import java.util.Arrays;

/**
 * Created by lizhen on 14/12/13.
 */
public class PcodeVM {
    //pcode数组上限
    private static final int cxmax = 500;
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
        if(PL0.parser.scan.isfileEneded)return;
        if (cx >= cxmax) {                                                                          //超出堆栈的上限
            throw new PL0Exception(38);
        }
        code[cx++] = new Pcode(f, l, a);
    }

    /**
     * 输出pcode符号表
     */
    public void listcode(int cx0) {
        for (int i = cx0; i < cx; i++) {
            try {
                String msg = i + "  " + Pcode.pcode[code[i].f] + "  " + code[i].l + " " + code[i].a;                //形如: lit l,a
                PL0.pcodeWriter.write(msg + '\n');
                PL0.pcodeWriter.flush();
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("***list pcode meet with error***");
            }
        }

    }
}
