package pl0compiler;

import java.io.IOException;

/**
 * Created by lizhen on 14/12/3.
 */
public class SymbolTable {

    /**
     * 当前符号表指针
     */
    public int tablePtr = 0;

    private static final int MaxTableSize = 1000;
    public static final int levMax = 3;
    public static final int addrMax = 1000000;      // 最大允许的数值
    private boolean debugging = true;

    public record[] tab;// 栈式符号表

    public SymbolTable(){
        tablePtr = 0;
        tab = new record[MaxTableSize];
    }
    public static enum Kind {
        constant(0),
        variable(1),
        procedure(2);

        private int enumValue;

        private Kind(int enumValue) {
            this.enumValue = enumValue;
        }

        public int val() {
            return enumValue;
        }
    }

    public class record {
        String name;            // 名字
        Kind kind;          // 种类(constant, variable, procedure)
        int value;                // 值，当kind为常量时
        int level;              // 嵌套层次
        int addr;               // 地址，当kind为常量或过程时
        int size;               // 该item的大小

        public record(String name, Kind kind, int value, int level, int addr) {
            this.name = name;
            this.kind = kind;
            this.value = value;
            this.level = level;
            this.addr = addr;
        }
        public record(){
            name = "";
        }
        public void reDirectAddr(int addr) {
            this.addr = addr;
        }
    }

    /**
     * 访问在栈中位置为idx的Item
     *
     * @param idx 要访问的符号表对象的对应标号
     * @return 返回位置为idx的Item
     */
    public record at(int idx) {
        if (idx > MaxTableSize || idx < 0)
            try {
                throw new Exception("****Access Violation in Symbol Table.");
            } catch (Exception e) {
                e.printStackTrace();
            }
        if(tab[idx] == null)tab[idx] = new record();
        return tab[idx];
    }

    /**
     * 对于一个符号名字，在栈式符号表中查找其最近的位置，无法找到则返回 -1
     *
     * @param s 要查找的符号名
     * @return  返回要查找的符号名的Item离栈顶最近的位置，找不到则返回 -1
     * @throws Exception
     */
    public int position(String s) {
        try {
            tab[0].name = s;
            for (int i = tablePtr; i >= 0; i--) {
                if (at(i).name.equals(s)) {
                    return i;
                }
            }
        }catch (Exception e){
            System.out.println(e.getMessage());
        }
        return 0;
    }

    /**
     * 向符号表中插入新的符号（包含判断是否重复）
     *
     * @param sym   要插入的符号
     * @param kind  符号种类
     * @param level 嵌套层次
     * @param dx    分配变量的相对地址
     * @return
     * @throws Exception
     */
    public void enter(Symbol sym, Kind kind, int level, Integer dx) throws PL0Exception {
        if (tablePtr == MaxTableSize){
            throw new PL0Exception(39);     // 符号表溢出
        }
        for(int i = tablePtr ; i > 0 ; i --){
            if(tab[i].level != level)break;
            if(tab[i].name == sym.name){
                throw new PL0Exception(29);
            }
        }
        record record = new record();
        record.name = sym.name;
        record.kind = kind;
        if(kind.val() == Kind.constant.val()){          // 常量
            record.value = Integer.parseInt(sym.content);                        // const 变量不需要level
        }else if(kind.val() == Kind.variable.val()){    // 变量
            record.level = level;
            record.addr = dx;                                                 // 相对此过程的偏移量
            dx = dx + 1;                                                      // TODO 确认向外传递dx的变化
        }else if(kind.val() == Kind.procedure.val()){   // 过程名
            record.level = level;
            record.addr = 0;
        }else{
            try {
                throw new Exception("Unknow Item kind in the SymbolTable!!!!");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        tab[++tablePtr] = record;
    }


    /**
     * 输出符号表内容，摘自block()函数
     *
     * @param start 当前符号表区间的左端
     */
    void debugTable(int start) {
        if (!debugging)
        {
            return;
        }
        try {
            PL0.tableWriter.write("**** Symbol Table ****\n");
            PL0.tableWriter.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (start > tablePtr) {
            System.out.println("  NULL");
        }
        for (int i = start; i < tablePtr; i++) {
            try {
                String msg = "unknown table item !";
                if(tab[i].kind == Kind.constant){
                    msg = "   " + i + "  const: " + tab[i].name + "  val: " + tab[i].value;
                }else if(tab[i].kind == Kind.variable){
                    msg = "    " + i + "  var: " + tab[i].name + "  lev: " + tab[i].level + "  addr: " + tab[i].addr;
                }else if(tab[i].kind == Kind.procedure){
                    msg = "    " + i + " proc: " + tab[i].name + "  lev: " + tab[i].level + "  addr: " + tab[i].size;
                }
                System.out.println(msg);
                PL0.tableWriter.write(msg + '\n');
                PL0.tableWriter.flush();
            } catch (IOException ex) {
                ex.printStackTrace();
                System.out.println("***write table intfo meet with error***");
            }
        }
    }

}
