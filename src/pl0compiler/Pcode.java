package pl0compiler;

/**
 * Created by lizhen on 14/12/13.
 */
public class Pcode {
    /**
     * pcode的指令格式f l, a
     * @param f 操作码(用枚举类型表示)
     * @param l 变量 | 过程被引用的分程序与说明该变量或过程的分程序之间的层次差
     * @param a 对于不同的指令有不同的含义
     */
    public Pcode(int f, int l, int a) {
        this.f = f;
        this.l = l;
        this.a = a;
    }

    /**
     * f的枚举类型
     */
    public static final int LIT = 0;            // LIT 0,a 将常量a放到数据栈栈顶
    public static final int OPR = 1;            // OPR 0,a 执行运算，a表示执行何种运算 TODO a的具体表示
    public static final int LOD = 2;            // LOD 1,a 取变量（相对地址为a，层次差为1）放到数据栈栈顶
    public static final int STO = 3;            // STO 1,a 将数据栈栈顶内容存入变量（相对地址为a，层次差为1）
    public static final int CAL = 4;            // CAL 1,a 调用过程
    public static final int INT = 5;            // INT 0,a 数据栈栈顶指针增加a
    public static final int JMP = 6;            // JMP 0,a 无条件跳转到a
    public static final int JPC = 7;            // JPC 0,a 条件转移到指令地址a
    public static final int RED = 8;            // RED 1,a 读数据并存入变量（相对地址为a，层次差为1）
    public static final int WRT = 9;            // WRT 0,0 将栈顶内容输出

    //各符号的名字
    public static final String[] pcode = new String[]{"LIT", "OPR", "LOD", "STO", "CAL", "INT", "JMP", "JPC","RED","WRT"};
    //虚拟机代码指令
    public int f;
    //引用层与声明层的层次差
    public int l;
    //指令参数
    public int a;
}
