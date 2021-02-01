package com.wcnwyx.spring.aop.example.pointcut;

public class MyInt implements Cloneable{
    public Object clone(){
        Object o = null;
        try {
            o = super.clone();
        }catch (CloneNotSupportedException e){
            e.printStackTrace();
        }
        return o;
    }
    private int a;

    public MyInt(int a) {
        this.a = a;
    }

    public int getA() {
        return a;
    }

    public void setA(int a) {
        this.a = a;
    }

    @Override
    public String toString() {
        return "MyInt{" +
                "a=" + a +
                '}'+super.toString();
    }
}
