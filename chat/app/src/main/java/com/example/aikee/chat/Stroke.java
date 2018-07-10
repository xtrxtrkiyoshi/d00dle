package com.example.aikee.chat;

import android.graphics.PointF;

import java.io.Serializable;
import java.util.ArrayList;


public class Stroke implements Serializable {

    private ArrayList<PointF> pathPoints;
    private int color = PaintView.DEFAULT_COLOR;//default values
    private int strokeWidth = PaintView.BRUSH_SIZE;//default values

    public Stroke(){
        pathPoints = new ArrayList<>();
    }

    public void setPaths(ArrayList<PointF> points){
        this.pathPoints = points;
    }

    public ArrayList<PointF> getPathPoints() {
        return pathPoints;
    }

    public int getColor() {
        return color;
    }

    public int getStrokeWidth() {
        return strokeWidth;
    }

    public void setStrokeProperties(int color, int strokeWidth){
        this.color = color;
        this.strokeWidth = strokeWidth;
    }
}
