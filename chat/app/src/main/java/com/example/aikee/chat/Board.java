package com.example.aikee.chat;

import java.util.ArrayList;

import flexjson.JSONSerializer;

public class Board {
    private ArrayList<Stroke> strokes;

    public Board() {
        strokes = new ArrayList<>();
    }

    public Board(ArrayList<Stroke> strokes){
        this.strokes = strokes;
    }

    public ArrayList<Stroke> getStrokes() {
        return strokes;
    }

    public void addStroke(Stroke stroke){
        strokes.add(stroke);

    }

    public void setStrokes(ArrayList<Stroke> strokes) {
        this.strokes = strokes;
    }
}
