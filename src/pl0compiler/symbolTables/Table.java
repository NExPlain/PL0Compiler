package pl0compiler.symbolTables;

import pl0compiler.errorHandler.PL0Exception;
import pl0compiler.syntaxAnalysis.Parser;
import pl0compiler.utils.Symbol;


/**
 * 符号表类，这个类是符号表类的基类，3种符号表实现都集成自这个类
 * Created by lizhen on 14/12/3.
 */
public abstract class Table {

    /**
     * 当前符号表指针
     */
    public int tx = 0;

    public static final int MaxTableSize = 1000000;    // 为了编译大数据设置的较大
    public static final int levMax = 3;             // 递归层数上限
    public static final int addrMax = 1000000;      // 最大允许的数值

    public Record[] tab;                                // 栈式符号表

    public Table(){
        tx = 0;
        tab = new Record[MaxTableSize];
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

    public class Record {
        public String name;                    // 名字
        public Table.type type;                // 种类(constant, variable, procedure)
        public int value;                      // 值，当kind为常量时
        public int level;                      // 嵌套层次
        public int adr;                        // 地址，当kind为常量或过程时
        public int size;                       // 该item的大小

        public Record(String name, Table.type kind, int value, int level, int adr) {
            this.name = name;
            this.type = kind;
            this.value = value;
            this.level = level;
            this.adr = adr;
        }
        public Record(){
            name = "";
        }
        public void reDirectAddr(int addr) {
            this.adr = addr;
        }
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
        Record record = new Record();
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
        enterTable(record);
    }

    /**
     * 访问在栈中位置为idx的Item
     *
     * @param idx 要访问的符号表对象的对应标号
     * @return 返回位置为idx的Item
     */
    public Record at(int idx) {
        if (idx > MaxTableSize || idx < 0)
            try {
                throw new Exception("Symbol Table Error: RuntimeError(AccessViolation)");
            } catch (Exception e) {
                e.printStackTrace();
            }
        if(tab[idx] == null)tab[idx] = new Record();
        return tab[idx];
    }
    /**
     * 抽象方法，查找该名字的符号最近的位置（没有则返回0）
     * 对于一个符号名字，在栈式符号表中查找其最近的位置，无法找到则返回 0
     * 查找方式按照不同的符号表实现方式不一样
     */
    public abstract int position(String s);

    /**
     * 把record插入表中，根据不同的表类型有不同的实现
     * @param record
     */
    public abstract void enterTable(Record record);

    /**
     * 把当前符号从表中删除，同样根据不同的表类型实现不同
     */
    public abstract void pop();

    /**
     * 将位置为idx的符号修改为record
     * @param record
     * @param idx
     */
    public void modify(Record record, int idx) {
        tab[idx] = record;
    }

    /**
     * 输出符号表内容
     *
     * @param start 当前符号表区间的左端
     */
    public void printTable(int start) {
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
