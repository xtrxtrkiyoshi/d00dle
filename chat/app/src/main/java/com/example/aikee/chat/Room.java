package com.example.aikee.chat;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Room implements Serializable{

//    private ArrayList<Stroke> board;
    private String id;
    private int it;
    private List<Message> messages;
    private int occupants;
    private List<Player> players;
    private int round;
    private int surrenderCount;
    private int turn;
    private String word;

    public String getId() {
        return id;
    }

    public int getIt() {
        return this.it;
    }

    public List<Message> getMessages() {
        return messages;
    }

    public int getOccupants() {
        return occupants;
    }

    public List<Player> getPlayers() {
        return players;
    }

    public int getRound() {
        return round;
    }

    public int getSurrenderCount() {
        return surrenderCount;
    }

    public int getTurn() {
        return turn;
    }

    public String getWord() {
        return word;
    }

    public Room() {
//        this.board = new ArrayList<>();
        this.id = "";
        this.it = 0;
        this.messages = new ArrayList<>();
        this.occupants = 0;
        this.players = new ArrayList<>();
        this.round = 1;
        this.surrenderCount = 0;
        this.turn = 1;
        this.word = "null";
    }

    public void setId(String id){
        this.id = id;
    }

    public void addMessages(Message message) {
        this.messages.add(message);
    }

    public void addPlayer(Player player) {
        this.players.add(player);
        this.occupants++;
    }

    public void setIt() {
        this.it++;
    }

    public void setWord(String word) {
        this.word = word;
    }

    public void setTurn(){
        this.turn++;
    }

    public void setRound(){
        this.round++;
    }

    public void setOccupants(int occupants){
        this.occupants = occupants;
    }

    public void setSurrenderCount(int surrenderCount){
        this.surrenderCount = surrenderCount;
    }

    public void resetSurrenderCount(){
        this.surrenderCount = 0;
    }

    public boolean isReadyToStartGame(){
        return occupants <= 6 && occupants >= 2;
    }

    public void setPlayers(List<Player> players){
        this.players = players;
    }

//    public ArrayList<Stroke> getBoard() {
//        return this.board;
//    }
//
//    public void setBoard(ArrayList<Stroke> board) {
//        this.board = board;
//    }
//
//    public void addStroke(Stroke stroke){
//        board.add(stroke);
//    }

    @Override
    public String toString(){
        return "{ id: \"" + id + "\", it : " + it + ", messages: " +
                messages + ", occupants: " + occupants + ", round: " + round +
                ", surrenderCount: " + surrenderCount + ", turn: " + turn + ", word:" + word + "}";
    }


}