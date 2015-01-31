package pl0compiler.pcode;

import pl0compiler.Compiler;
import pl0compiler.errorHandler.PL0Exception;
import pl0compiler.utils.Pcode;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Arrays;

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

    public void interpret(BufferedReader stdin, BufferedWriter stdout) {
        int[] runtimeStack = new int[cxmax];                  // 程序运行栈
        Arrays.fill(runtimeStack, 0);                                   //初始化
        System.out.println("***Start Interpret P_CODE***");

        int pc = 0, // pc:指令指针，
                bp = 1, //bp:指令基址，
                sp = 0; 												    		   //sp:栈顶指针

        do {
            Pcode index = code[pc++];// index :存放当前指令, 读当前指令
           // System.out.println(pc + "  " + Pcode.pcode[index.f] + " " + index.l + " " + index.a);
            switch (index.f) {
                case Pcode.LIT:                                                   // 将a的值取到栈顶
                    runtimeStack[++sp] = index.a;
                    break;
                case Pcode.OPR:                                                   // 数学、逻辑运算
                    switch (index.a) {
                        case 0:                                                          //OPR 0 0;RETURN 返回
                            sp = bp - 1;
                            pc = runtimeStack[sp + 3];
                            bp = runtimeStack[sp + 2];
                            break;
                        case 1:                                                           //OPR 0 1 ;NEG取反
                            runtimeStack[sp] = -runtimeStack[sp];
                            break;
                        case 2:                                                           //OPR 0 2;ADD加法
                            sp--;
                            runtimeStack[sp] += runtimeStack[sp+1];
                            break;
                        case 3:                                                             //OPR 0 3;SUB减法
                            sp--;
                            runtimeStack[sp] -= runtimeStack[sp+1];
                            break;
                        case 4:                                                             //OPR 0 4;MUL乘法
                            sp--;
                            runtimeStack[sp] =runtimeStack[sp] * runtimeStack[sp+1];
                            break;
                        case 5:                                                             //OPR 0 5;DIV除法
                            sp--;
                            runtimeStack[sp] /= runtimeStack[sp+1];
                            break;
                        case 6:                                                              //OPR 0 6;ODD对2取模mod 2
                            runtimeStack[sp] %= 2;
                            break;
                        case 7:                                                              //OPR 0 7;MOD取模
                            sp--;
                            runtimeStack[sp] %= runtimeStack[sp+1];
                            break;
                        case 8:                                                             //OPR 0 8;==判断相等
                            sp--;
                            runtimeStack[sp] = (runtimeStack[sp] == runtimeStack[sp + 1] ? 1 : 0);
                            break;
                        case 9:                                                                //OPR 0 9;!=判断不相等
                            sp--;
                            runtimeStack[sp] = (runtimeStack[sp] != runtimeStack[sp + 1] ? 1 : 0);
                            break;
                        case 10:                                                               //OPR 0 10;<判断小于
                            sp--;
                            runtimeStack[sp] = (runtimeStack[sp] < runtimeStack[sp + 1] ? 1 : 0);
                            break;
                        case 11:                                                                //OPR 0 11;>=判断大于等于
                            sp--;
                            runtimeStack[sp] = (runtimeStack[sp] >= runtimeStack[sp + 1] ? 1 : 0);
                            break;
                        case 12:                                                                //OPG 0 12;>判断大于
                            sp--;
                            runtimeStack[sp] = (runtimeStack[sp] > runtimeStack[sp + 1] ? 1 : 0);
                            break;
                        case 13:                                                                 //OPG 0 13;<=判断小于等于
                            sp--;
                            runtimeStack[sp] = (runtimeStack[sp] <= runtimeStack[sp + 1] ? 1 : 0);
                            break;
                        case 14:                                                                 //OPG 0 14;输出栈顶值
                            System.out.println("runtimeStack[sp]" + runtimeStack[sp] + ' ');
                            try {
                                stdout.write(" " + runtimeStack[sp] + ' ');
                                stdout.flush();
                            } catch (Exception ex) {
                                System.out.println("***case 14 meet with error***");
                            }
                            sp--;
                            break;
                        case 15:                                                                 //OPG 0 15;输出换行
                            System.out.print("\n");
                            try {
                                stdout.write("\n");
                            } catch (Exception ex) {
                                System.out.println("***case 15 meet with error***");
                            }
                            break;
                        case 16:                                                                 //OPG 0 16;读入一行输入，置入栈顶
                            System.out.print("Please Input a Integer : ");
                            runtimeStack[sp] = 0;
                            try {
                                runtimeStack[sp] = Integer.parseInt(stdin.readLine().trim());       //读入一个整型数字
                                System.out.println(runtimeStack[sp]);
                                sp++;
                            } catch (Exception e) {
                                e.printStackTrace();
                                System.out.println("***read data meet with error***");
                            }
                            try {
                                stdout.write("********************" + runtimeStack[sp] + '\n');
                                stdout.flush();
                            } catch (Exception ex) {
                                System.out.println("***case 16 meet with error***");
                            }
                            break;
                    }
                    break;
                case Pcode.LOD:                                          //取相对当前过程的数据基地址为a的内存的值到栈顶
                    sp++;
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
                    bp = sp + 1;                                                                        //改变基地址指针值为新过程的基地址
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
                case Pcode.WRT:
                    try {
                        stdout.write("****OUTPUT:" + runtimeStack[sp] + "****\n");
                        stdout.flush();
                        Compiler.outputWriter.write("****OUTPUT:" + runtimeStack[sp] + "****\n");
                        Compiler.outputWriter.flush();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
                case Pcode.RED:
                    try {
                        stdout.write("Please input a number: ");
                        stdout.flush();
                        runtimeStack[base(index.l,runtimeStack,bp) + index.a] = Integer.parseInt(stdin.readLine().trim());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
            }
        } while (pc != 0);
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

}
