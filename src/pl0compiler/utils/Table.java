package pl0compiler.utils;

import pl0compiler.Compiler;
import pl0compiler.errorHandler.PL0Exception;
import pl0compiler.syntaxAnalysis.Parser;

import java.io.IOException;

/**
 * Created by lizhen on 14/12/3.
 */
public class Table {

    /**
     * 当前符号表指针
     */
    public int tx = 0;

    private static final int MaxTableSize = 500;
    public static final int levMax = 3;
    public static final int addrMax = 1000000;      // 最大允许的数值

    public record[] tab;// 栈式符号表

    public Table(){
        tx = 0;
        tab = new record[MaxTableSize];
    }
    public static enum type {
        constant(0),
        variable(1),
        procedure(2);

        private int enumValue;

        private type(int enumValue) {
            this.enumValue = enumValue;
        }

        public int val() {
            return enumValue;
        }
    }

    public class record {
        public String name;            // 名字
        public Table.type type;                // 种类(constant, variable, procedure)
        public int value;                // 值，当kind为常量时
        public int level;                // 嵌套层次
        public int adr;                 // 地址，当kind为常量或过程时
        public int size;               // 该item的大小

        public record(String name, Table.type kind, int value, int level, int adr) {
            this.name = name;
            this.type = kind;
            this.value = value;
            this.level = level;
            this.adr = adr;
        }
        public record(){
            name = "";
        }
        public void reDirectAddr(int addr) {
            this.adr = addr;
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
            for (int i = tx; i >= 0; i--) {
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
     * @param parser 正在运行的语法分析器，用于获取dx（相对地址）
     * @return
     * @throws Exception
     */
    public void enter(Symbol sym, type kind, int level, Parser parser) throws PL0Exception {
        if (tx == MaxTableSize-1){
            throw new PL0Exception(39);     // 符号表溢出
        }
        for(int i = tx; i > 0 ; i --){
            if(tab[i].level != level)break;
            if(tab[i].name == sym.name){
                throw new PL0Exception(29);
            }
        }
        record record = new record();
        record.name = sym.name;
        record.type = kind;
        if(kind.val() == type.constant.val()){                                          // 常量
            record.value = Integer.parseInt(sym.content);                               // const 变量不需要level
        }else if(kind.val() == type.variable.val()){                                    // 变量
            record.level = level;
            if(record.adr == 0)
                record.adr = parser.dx;                                                 // 相对此过程的偏移量
            parser.dx = parser.dx + 1;
        }else if(kind.val() == type.procedure.val()){                                   // 过程名
            record.level = level;
            record.adr = 0;
        }else{
            try {
                throw new Exception("Error, unknown item in symbol table!");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        tab[++tx] = record;
        //printTable(tx);
    }


    /**
     * 输出符号表内容
     *
     * @param start 当前符号表区间的左端
     */
    void printTable(int start) {
        if (start > tx) {
            System.out.println("  NULL");
        }
        for (int i = start; i <= tx; i++) {
                String msg = "table error !";
                if(tab[i].type == type.constant){
                    msg = i + "  const: " + tab[i].name + "  val: " + tab[i].value;
                }else if(tab[i].type == type.variable){
                    msg = i + "  var: " + tab[i].name + "  lev: " + tab[i].level + "  adr: " + tab[i].adr;
                }else if(tab[i].type == type.procedure){
                    msg = i + "  proc: " + tab[i].name + "  lev: " + tab[i].level + "  adr: " + tab[i].size;
                }
                System.out.println(msg);

        }
    }

}
