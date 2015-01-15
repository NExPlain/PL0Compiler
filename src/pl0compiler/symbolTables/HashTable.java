package pl0compiler.symbolTables;

import javafx.util.Pair;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

/**
 * Created by lizhen on 15/1/14.
 */
public class HashTable extends Table {

    /**
     * 采用HashTable套HashSet的方法，存放<变量名，层次> -> 栈序号的键值对
     */
    HashMap<String, HashSet<Pair<Integer,Integer> > > hashTable;
    public HashTable(){
        super();
        hashTable = new HashMap<String, HashSet<Pair<Integer,Integer> > >();
    }

    @Override
    public int position(String name) {
        if(hashTable.get(name) == null || hashTable.get(name).size() == 0){
            return 0;
        }
        Iterator i = hashTable.get(name).iterator();
        return ((Pair<Integer,Integer>)i.next()).getValue();
    }

    @Override
    public void enterTable(Record record) {
        String name = record.name;
        Integer level = record.level;
        if(hashTable.get(name) == null){
            HashSet<Pair<Integer, Integer>> hashSet = new HashSet<Pair<Integer, Integer>>();
            hashSet.add(new Pair<Integer,Integer>(level,tx+1));
            hashTable.put(name, hashSet);
        }else{
            hashTable.get(name).add(new Pair<Integer,Integer>(level,tx+1));
        }
        tab[++tx] = record;
    }

    @Override
    public void pop() {
        Record record = at(tx);
        if(hashTable.get(record.name).contains(new Pair<Integer, Integer>(record.level,tx)))
            hashTable.get(record.name).remove(new Pair<Integer, Integer>(record.level,tx));
        if(tx <= 0){
            try{
                throw new Exception();
            }catch (Exception ex){
                ex.printStackTrace();
            }
        }
        tx--;
    }

    @Override
    public void printTable(int start) {

    }
}
