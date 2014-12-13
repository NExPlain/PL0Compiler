package com.company;

/**
 * Created by lizhen on 14/12/4.
 */
public class Pl0Exception extends Exception{
    public int errType;
    public Pl0Exception(int errType){
        super();
        this.errType = errType;
    }
}
