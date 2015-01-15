package pl0compiler.symbolTables;


/**
 * Created by lizhen on 15/1/14.
 */
public class StackTable extends Table {
    public StackTable(){
        super();
    }

    /**
     * 对于一个符号名字，在栈式符号表中查找其最近的位置，无法找到则返回 0
     * 采用顺序查找的方式
     *
     * @param s 要查找的符号名
     * @return  返回要查找的符号名的Item离栈顶最近的位置，找不到则返回 0
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

    public void enterTable(Record record){
        tab[++tx] = record;
    }

    public void pop(){
        if(tx <= 0){
            try{
                throw new Exception();
            }catch (Exception ex){
                ex.printStackTrace();
            }
        }
        tx--;
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
