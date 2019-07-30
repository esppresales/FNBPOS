package com.gettingreal.bpos.model;

/**
 * Created by MinTheinWin on 30/9/16.
 * mintheinwin25@gmail.com
 */
public class MasterPrinterClass {

    private String text;
    private int value;
    public MasterPrinterClass(String text, int value){
        this.text = text;
        this.value = value;
    }

    public void setText(String text){
        this.text = text;
    }

    public String getText(){
        return this.text;
    }

    public void setValue(int value){
        this.value = value;
    }

    public int getValue(){
        return this.value;
    }
}
