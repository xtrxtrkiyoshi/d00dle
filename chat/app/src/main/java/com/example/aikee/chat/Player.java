package com.example.aikee.chat;

import java.io.Serializable;

public class Player implements Serializable{
    private int number;
    private String displayName;
    private boolean IT;
    private int points;

    public Player() {}

    public Player(String name, int points) {
//        this.number = -1;
        this.displayName = name;
        this.IT = false;
        this.points = points;
    }

    public void setNumber(int number){
        this.number = number;
    }

    public int getNumber(){
        return this.number;
    }

    public String getDisplayName() {
        return this.displayName;
    }

    public int getPoints() {
        return this.points;
    }

    public void setPoints(int points){
        this.points = points;
    }

    public void setITPoints(){
        this.points = points+4;
    }

    public boolean isIT() {
        return this.IT;
    }

    public void setIT(boolean IT) {
        this.IT = IT;
    }

    @Override
    public String toString(){
        return "{name: " + displayName + ", it: " + IT + ", points: " + points + "}";
    }
}
