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

    public static int top = 0;

    public Item[] tab = new Item[MaxTableSize]; // 栈式符号表

    public SymbolTable(){
        tablePtr = top = 0;
    }
    public static enum ItemKind {
        constant(0),
        variable(1),
        procedure(2);

        private int enumValue;

        private ItemKind(int enumValue) {
            this.enumValue = enumValue;
        }

        public int getIntValue() {
            return enumValue;
        }
    }

    public class Item {
        String name;            // 名字
        ItemKind kind;          // 种类(constant, variable, procedure)
        int value;                // 值，当kind为常量时
        int level;              // 嵌套层次
        int addr;               // 地址，当kind为常量或过程时
        int size;               // 该item的大小

        public Item(String name, ItemKind kind, int value, int level, int addr) {
            this.name = name;
            this.kind = kind;
            this.value = value;
            this.level = level;
            this.addr = addr;
        }
        public Item(){
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
    public Item getItemAt(int idx) {
        if (idx > top || idx < 0)
            try {
                throw new Exception("***Access Violation in Symbol Table.***");
            } catch (Exception e) {
                e.printStackTrace();
            }
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
            for (int i = top - 1; i >= 0; i--) {
                if (getItemAt(i).name.equals(s)) {
                    return i;
                }
            }
        }catch (Exception e){
            System.out.println(e.getMessage());
        }
        return -1;
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
    public void enter(Symbol sym, ItemKind kind, int level, int dx) throws Pl0Exception {
        if (top == MaxTableSize){
            try {
                throw new Exception("符号表溢出错误！");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        for(int i = top-1 ; i >= 0 ; i --){
            if(tab[i].level != level)break;
            if(tab[i].name == sym.content){
                throw new Pl0Exception(29);
            }
        }
        Item item = new Item();
        item.name = sym.content;
        item.kind = kind;
        if(kind.getIntValue() == ItemKind.constant.getIntValue()){  // 常量名
            item.value = Integer.parseInt(sym.content);             // TODO 不需要加level?
        }else if(kind.getIntValue() == ItemKind.variable.getIntValue()){    // 变量名
            item.level = level;
            item.addr = dx;                                                 // TODO 地址为偏移量？
        }else if(kind.getIntValue() == ItemKind.procedure.getIntValue()){   // 过程名
            item.level = level;
        }else{
            try {
                throw new Exception("Unknow Item kind in the SymbolTable");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        tab[top++] = item;
    }


    /**
     * 输出符号表内容，摘自block()函数
     *
     * @param start 当前符号表区间的左端
     */
    void debugTable(int start) {
        if (!debugging) //显示名字表与否
        {
            return;
        }
        System.out.println("**** Symbol Table ****");
        if (start > tablePtr) {
            System.out.println("  NULL");
        }
        for (int i = start + 1; i <= tablePtr; i++) {
            try {
                String msg = "unknown table item !";
                if(tab[i].kind == ItemKind.constant){
                    msg = "   " + i + "  const: " + tab[i].name + "  val: " + tab[i].value;
                }else if(tab[i].kind == ItemKind.variable){
                    msg = "    " + i + "  var: " + tab[i].name + "  lev: " + tab[i].level + "  addr: " + tab[i].addr;
                }else if(tab[i].kind == ItemKind.procedure){
                    msg = "    " + i + " proc: " + tab[i].name + "  lev: " + tab[i].level + "  addr: " + tab[i].size;
                }
                System.out.println(msg);
                PL0.tableWriter.write(msg + '\n');
            } catch (IOException ex) {
                ex.printStackTrace();
                System.out.println("***write table intfo meet with error***");
            }
        }
    }

}
