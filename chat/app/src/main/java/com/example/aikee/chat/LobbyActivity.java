package com.example.aikee.chat;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.FirebaseApp;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.apache.commons.lang3.RandomStringUtils;
import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.List;

public class LobbyActivity extends AppCompatActivity {

    //activity components
    private ListView mPlayerListView;
    private Button mStartButton;
    private TextView mRoomCodeTextView;
    //adapter for player list
    private PlayerAdapter mPlayerAdapter;
    //firebase variables
    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference mPlayerDatabaseReference;
    private DatabaseReference mRoomsDatabaseReference;
    private DatabaseReference mCurrentRoomDatabaseReference;
    private DatabaseReference mGameStartDatabaseReference;
    private ChildEventListener mPlayerEventListener;
    private ChildEventListener mCurrentRoomEventListener;
    //logic variables
    private Room room;
    private Player player;
    private String mUsername;
    private boolean created;
    private String TAG = "taggy";

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lobby);


        //moving variables to this activity and player enters room
        Bundle intent = getIntent().getExtras();
        mUsername = intent.getString("username");
        created = intent.getBoolean("create");
        player = new Player(mUsername,0);
        mRoomCodeTextView = (TextView) findViewById(R.id.roomCodeTextView);
        //list of players
        mPlayerListView = (ListView) findViewById(R.id.playerListView);
        mStartButton = (Button) findViewById(R.id.startButton);
        List<Player> players = new ArrayList<>();
        mPlayerAdapter = new PlayerAdapter(this, R.layout.item_message, players);
        mPlayerListView.setAdapter(mPlayerAdapter);

        //Firebase stuff
        FirebaseApp.initializeApp(this);
        mFirebaseDatabase = FirebaseDatabase.getInstance();
        mRoomsDatabaseReference = mFirebaseDatabase.getReference().child("room");

        final String temp; //for room id
        //create room
        if(created) {
            //get the key of the one pushed
            temp = RandomStringUtils.randomAlphanumeric(4).toLowerCase();
//            temp = mRoomsDatabaseReference.push().getKey(); //old push firebase uuid
            mCurrentRoomDatabaseReference =  mFirebaseDatabase.getReference().child("room").child(temp);

            mGameStartDatabaseReference = mCurrentRoomDatabaseReference.child("gameStarted");

            room = new Room();
            room.setId(temp);
            room.addPlayer(player);
            player.setIT(true);
            player.setNumber(room.getOccupants()-1);
            mRoomCodeTextView.setText("Your room code is:" + temp);
            //update values in database
            mCurrentRoomDatabaseReference.child("occupants").setValue(room.getOccupants());
            mCurrentRoomDatabaseReference.child("players").setValue(room.getPlayers());
            mPlayerDatabaseReference = mCurrentRoomDatabaseReference.child("players");
            initializePlayerEvents();

            mStartButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Log.d(TAG,"clicked start button isReady:" + room.isReadyToStartGame());
                    if(room!=null && room.isReadyToStartGame()  && room.getOccupants()>2) {
                        Log.d(TAG,"entered if in start button");
                        startGame();
                        mGameStartDatabaseReference.setValue(true);
                    } else {
                        Toast.makeText(getApplicationContext(),"Room must have 3-6 players",Toast.LENGTH_LONG);
                    }


                }
            });
        }
        else {
            mStartButton.setVisibility(View.GONE);
            mStartButton.setEnabled(false);
            temp = intent.getString("roomId");
            mRoomCodeTextView.setText("Currently in room " + temp);
            mCurrentRoomDatabaseReference =  mFirebaseDatabase.getReference().child("room").child(temp);
            mGameStartDatabaseReference = mCurrentRoomDatabaseReference.child("gameStarted");
            mCurrentRoomDatabaseReference.child("players");
            mCurrentRoomDatabaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    //if(dataSnapshot.getValue(Room.class) != null) {
//                    Log.e("meep", dataSnapshot.getValue().toString());
                    room = dataSnapshot.getValue(Room.class);
                    room.setId(temp);
                    Log.d(TAG,"onDataChange: room: " + room);
                    room.addPlayer(player);
                    player.setNumber(room.getOccupants()-1);
                    //update values in database
                    mRoomsDatabaseReference.child(temp).child("occupants").setValue(room.getOccupants());
                    mRoomsDatabaseReference.child(temp).child("players").setValue(room.getPlayers());
                    //}
                    mPlayerDatabaseReference = mCurrentRoomDatabaseReference.child("players");
                    initializePlayerEvents();
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {}
            });

            mGameStartDatabaseReference.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if(dataSnapshot.getValue()!=null) {
                        boolean gameState = (boolean) dataSnapshot.getValue();
                        if (gameState) {
                            startGame();
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });
        }
    }


    public void startGame(){
        if(room!=null && room.isReadyToStartGame()) {
            Log.d(TAG,"about to start game");
//            Toast.makeText(this,"ABOUT TO START GAME",Toast.LENGTH_LONG);
            Intent intent = new Intent(LobbyActivity.this, GameActivity.class);
            intent.putExtra("Room", room);
            intent.putExtra("Player", player);
            startActivity(intent);
        }
        else{
            Toast.makeText(getApplicationContext(),"Not ready to start game. Room must occupy between 3-6 players only.",Toast.LENGTH_LONG);
            Log.d(TAG,"not ready to start");
        }
    }

    void initializePlayerEvents(){
        mCurrentRoomEventListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                String key = dataSnapshot.getKey();
                Log.d(TAG,"datasnapshot childchanged: {" + dataSnapshot.getKey() + ": " + dataSnapshot.getValue() + "}");
                if(key.contentEquals("occupants")){
                    room.setOccupants(((Long) dataSnapshot.getValue()).intValue());
                    mStartButton.setEnabled(room.isReadyToStartGame());
                    Log.d(TAG,"room strutcture changed: " + room.getOccupants());
                }
                else if(key.contentEquals("players")){
                    room.setPlayers((List<Player>) dataSnapshot.getValue());
                    Log.d(TAG,"room strutcture changed: " + room.getPlayers().size());
                }

            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        };
        mCurrentRoomDatabaseReference.addChildEventListener(mCurrentRoomEventListener);
        mPlayerEventListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                Player player = dataSnapshot.getValue(Player.class);
                mPlayerAdapter.add(player);

            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        };
        mPlayerDatabaseReference.addChildEventListener(mPlayerEventListener);
    }
}
