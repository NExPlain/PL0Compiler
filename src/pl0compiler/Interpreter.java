package pl0compiler;

import java.io.*;
import java.util.Arrays;

/**
 * Created by lizhen on 14/12/13.
 */
public class Interpreter {
    //运行栈上限
    private static final int stackSize = 500;
    //pcode数组上限
    private static final int cxmax = 500;
    //虚拟机代码指针，存放数据范围[0,cx-1]
    public int cx = 0;
    //存放虚拟机代码的数组
    public Pcode[] code;
    //显示虚拟代码与否
    public static boolean listswitch = true;


    public Interpreter() {
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
        if (cx >= cxmax) {                                                                          //超出堆栈的上限
            throw new PL0Exception(38);
        }
        code[cx++] = new Pcode(f, l, a);
        listcode(cx-1);
    }


    /**
     * 输出pcode符号表
     */
    public void listcode(int cx0) {
        if (listswitch) {                                                                         //是否显示P-code代码
            for (int i = cx0; i < cx; i++) {
                try {
                    String msg = i + "  " + Pcode.pcode[code[i].f] + "  " + code[i].l + " " + code[i].a;                //形如: lit l,a
                    //System.out.println(msg);
                    PL0.pcodeWriter.write(msg + '\n');
                    PL0.pcodeWriter.flush();
                } catch (Exception e) {
                    e.printStackTrace();
                    System.out.println("***list pcode meet with error***");
                }
            }
        }
    }

    /**
     * 这个过程模拟了一台可以运行类PCODE指令的栈式计算机。 它拥有一个栈式数据段用于存放运行期数据, 拥有一个代码段用于存放类PCODE程序代码。
     * 同时还拥用数据段分配指针、指令指针、指令寄存器、局部段基址指针等寄存器。
     *  @param stdin 从键盘输入无符号整数
     * @param stdout 显示pcode运行过程
     */
    public void interpret(InputStream stdin, PrintStream stdout) {
        int[] runtimeStack = new int[stackSize];                  // 程序运行栈
        Arrays.fill(runtimeStack, 0);                                   //初始化
        try {
            PL0.pcodeWriter.write("***Start PL/0***\n");
            PL0.pcodeWriter.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }

        int pc = 0, // pc:指令指针，
                bp = 1,                                                                //bp:指令基址 TODO 初始值为0？
                sp = 0; 												    		   //sp:栈顶指针

        do {
            Pcode index = code[pc++];// index :存放当前指令, 读当前指令
            String pcode = pc + "  " + Pcode.pcode[index.f] + " " + index.l + " " + index.a;
            try {
                PL0.pcodeWriter.write(pcode + '\n');
                PL0.pcodeWriter.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
            switch (index.f) {
                case Pcode.LIT:                                                   // 将a的值取到栈顶
                    runtimeStack[sp++] = index.a;
                    break;
                case Pcode.OPR:                                                   // 数学、逻辑运算
                    switch (index.a) {
                        case 0:                                                          //OPR 0 0;RETURN 返回
                            sp = bp-1;
                            pc = runtimeStack[sp + 3];
                            bp = runtimeStack[sp + 2];
                            break;
                        case 1:                                                           //OPR 0 1 ;NEG取反
                            runtimeStack[sp] = -runtimeStack[sp];
                            break;
                        case 2:                                                             //OPR 0 2;ADD加法
                            sp--;
                            runtimeStack[sp] += runtimeStack[sp+1];
                            break;
                        case 3:                                                             //OPR 0 3;SUB减法
                            sp--;
                            runtimeStack[sp] -= runtimeStack[sp+1];
                            break;
                        case 4:                                                             //OPR 0 4;MUL乘法
                            sp--;
                            runtimeStack[sp] *= runtimeStack[sp+1];
                            break;
                        case 5:                                                             //OPR 0 5;DIV除法
                            sp--;
                            runtimeStack[sp] /= runtimeStack[sp+1];
                            break;
                        case 6:                                                              //OPR 0 6;ODD对2取模mod 2
                            runtimeStack[sp] %= 2;
                            break;
                        case 7:                                                             //OPR 0 7;==判断相等
                            sp--;
                            runtimeStack[sp] = (runtimeStack[sp] == runtimeStack[sp + 1] ? 1 : 0);
                            break;
                        case 8:                                                                //OPR 0 8;!=判断不相等
                            sp--;
                            runtimeStack[sp] = (runtimeStack[sp] != runtimeStack[sp + 1] ? 1 : 0);
                            break;
                        case 9:                                                               //OPR 0 9;<判断小于
                            sp--;
                            runtimeStack[sp] = (runtimeStack[sp] < runtimeStack[sp + 1] ? 1 : 0);
                            break;
                        case 10:                                                                //OPR 0 10;>=判断大于等于
                            sp--;
                            runtimeStack[sp] = (runtimeStack[sp] >= runtimeStack[sp + 1] ? 1 : 0);
                            break;
                        case 11:                                                                //OPG 0 11;>判断大于
                            sp--;
                            runtimeStack[sp] = (runtimeStack[sp] > runtimeStack[sp + 1] ? 1 : 0);
                            break;
                        case 12:                                                                 //OPG 0 12;<=判断小于等于
                            sp--;
                            runtimeStack[sp] = (runtimeStack[sp] <= runtimeStack[sp + 1] ? 1 : 0);
                            break;
                    }
                    break;
                case Pcode.LOD:                                          //取相对当前过程的数据基地址为a的内存的值到栈顶
                    sp ++;
                    runtimeStack[sp] = runtimeStack[base(index.l, runtimeStack, bp) + index.a];
                    break;
                case Pcode.STO:                                         //栈顶的值存到相对当前的过程的数据基地址为a的内存
                    runtimeStack[base(index.l, runtimeStack, bp) + index.a] = runtimeStack[sp];
                    sp--;
                    break;
                case Pcode.CAL:                                                                 //调用子程序
                    runtimeStack[sp + 1] = base(index.l, runtimeStack, bp);            //将静态作用域基地址入栈
                    runtimeStack[sp + 2] = bp;                                                //将动态作用域基地址
                    runtimeStack[sp + 3] = pc;                                               //将当前指针入栈
                    bp = sp;                                                                        //改变基地址指针值为新过程的基地址
                    pc = index.a;                                                                 //跳转至地址a
                    break;
                case Pcode.INT:                                                               //开辟空间大小为a
                    sp += index.a;
                    break;
                case Pcode.JMP:                                                               //直接跳转至a
                    pc = index.a;
                    break;
                case Pcode.JPC:
                    if (runtimeStack[sp] == 0) //条件跳转至a(当栈顶指针为0时)
                    {
                        pc = index.a;
                    }
                    sp--;
                    break;
                case Pcode.RED:
                    try {
                        runtimeStack[base(index.l, runtimeStack, bp)] = stdin.read();
                        break;
                    } catch (IOException ex){
                        ex.printStackTrace();
                    }
                case Pcode.WRT:
                    stdout.write(runtimeStack[sp]);
                    sp ++;
                    break;
            }
        } while (pc != 0);
        System.out.println("END PL/0");
    }

    /**
     * 通过给定的层次差来获得该层的堆栈帧基址
     *
     * @param l 目标层次与当前层次的层次差
     * @param runtimeStack 运行栈
     * @param b 当前层堆栈帧基地址
     * @return 目标层次的堆栈帧基地址
     */
    private int base(int l, int[] runtimeStack, int b) {
        while (l > 0) {                                                       //向上找l层
            b = runtimeStack[b];
            l--;
        }
        return b;
    }

    public void debugPcodeArray() throws IOException {
        System.out.println("***Auto-Generated Pcode Array***");
        String msg = null;
        for (int i = 0; code[i] != null; i++) {
            msg = "" + i + "  " + Pcode.pcode[code[i].f] + "  " + code[i].l + "  " + code[i].a;
            System.out.println(msg);
            PL0.pcodeWriter.write(msg + '\n');
        }
    }
}
