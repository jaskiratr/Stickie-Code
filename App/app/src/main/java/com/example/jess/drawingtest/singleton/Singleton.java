package com.example.jess.drawingtest.singleton;

/**
 * Created by sanya on 01-Feb-16.
 */
public enum Singleton {
    INSTANCE;

    private int paintColor;
    private boolean isColorSet = false;
    private int width, height;

    public void setPaintColor(int color){
        paintColor = color;
    }

    public int getPaintColor(){

        return paintColor;
    }

    public void setPaintColorFlag(boolean bool){
        isColorSet = bool;
    }

    public boolean getPaintColorFlag(){

        return isColorSet;
    }

    public void setWidth(int wd){

        width = wd;
    }
    public int getWidth(){
        return width;
    }

    public void setHeight(int ht){

        height = ht;
    }

    public int getHeight(){
        return height;
    }
}
