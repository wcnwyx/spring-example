package com.wcnwyx.spring.aop.example.introduction;

public class DefaultFruit implements FruitInterface {
    @Override
    public void printColor() {
        System.out.println("my color is black");
    }
}
