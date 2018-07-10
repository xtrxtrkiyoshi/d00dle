package com.example.aikee.chat;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.PointF;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.FirebaseApp;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import flexjson.JSONDeserializer;
import flexjson.JSONSerializer;

public class GameActivity extends Activity {

    private static final String TAG = "taggy";

    public static final int DEFAULT_MSG_LENGTH_LIMIT = 50;

    private ListView mMessageListView;
    private LinearLayout mLinearLayout;
    private MessageAdapter mMessageAdapter;
    private ProgressBar mProgressBar;
    private EditText mMessageEditText;
    private TextView mPointsTextView;
    private Button mSendButton;
    private Button mNextTurn;
    private Button mSurrenderButton;
    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference mMessagesDatabaseReference;
    private DatabaseReference mDrawingDatabaseReference;
    private DatabaseReference mCurrentRoomDatabaseReference;
    private DatabaseReference mCurrentPlayerDatabaseReference;
    private ChildEventListener mMessageEventListener;
    private ChildEventListener mDrawingEventListener;
    private PopupWindow mPopupWindow;
    private PaintView paintView;
    private Player player;
    private Context mContext;
    private boolean found;
    private Room room;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        mContext = getApplicationContext();
        Bundle intent = getIntent().getExtras();
        room = (Room) intent.getSerializable("Room");
        player = (Player) intent.getSerializable("Player");

        Log.d(TAG, "my room is: " + room);
        Log.d(TAG, "my player is: " + player);

        FirebaseApp.initializeApp(this);
        mFirebaseDatabase = FirebaseDatabase.getInstance();
        mCurrentRoomDatabaseReference = mFirebaseDatabase.getReference().child("room").child(room.getId());
        mMessagesDatabaseReference = mCurrentRoomDatabaseReference.child("messages");
        mCurrentPlayerDatabaseReference = mCurrentRoomDatabaseReference.child("players").child(String.valueOf(player.getNumber()));

        // Initialize references to views

        mProgressBar = (ProgressBar) findViewById(R.id.progressBar);
        mMessageListView = (ListView) findViewById(R.id.messageListView);
        mMessageEditText = (EditText) findViewById(R.id.messageEditText);
        mSendButton = (Button) findViewById(R.id.sendButton);
        mLinearLayout = findViewById(R.id.cl);
        mSurrenderButton = (Button) findViewById(R.id.surrenderButton);
        mPointsTextView = (TextView) findViewById(R.id.pointsTextView);

        // Initialize message ListView and its adapter
        List<Message> messages = new ArrayList<>();
        mMessageAdapter = new MessageAdapter(this, R.layout.item_message, messages);
        mMessageListView.setAdapter(mMessageAdapter);
        initializeMessageEvent();

        // Initialize progress bar
        mProgressBar.setVisibility(ProgressBar.INVISIBLE);

        // Enable Send button when there's text to send
        mMessageEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
//                if (charSequence.toString().trim().length() > 0 && charSequence.toString().trim().length() <= DEFAULT_MSG_LENGTH_LIMIT && found) {
//                    mSendButton.setEnabled(true);
//                } else {
//                    mSendButton.setEnabled(false);
//                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });
        mMessageEditText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(DEFAULT_MSG_LENGTH_LIMIT)});
        mCurrentRoomDatabaseReference.child("word").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.getValue()!=null){
                    String word = dataSnapshot.getValue().toString();
                    room.setWord(word);
                    Log.d(TAG,"OBTAINED WORD: " + word);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        //VALUE EVENT LISTENER FOR SURRENDER COUNT
        mCurrentRoomDatabaseReference.child("surrenderCount").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if (dataSnapshot.getValue() != null) {
                        int surrenderCount = ((Long) dataSnapshot.getValue()).intValue();
                        Log.d(TAG, "datachange for surrendercount: " + surrenderCount);
                        room.setSurrenderCount(surrenderCount);

                        if (room.getSurrenderCount() == (room.getPlayers().size() - 1)) {
                            Log.d(TAG, "all players have surrendered");
                            Toast.makeText(getBaseContext(), "All players have surrendered.", Toast.LENGTH_LONG).show();
                            //TODO: Call endTurn here
                        }
                    }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }

        });

        //VALUE EVENT LISTENER FOR PLAYER POINTS
        mCurrentPlayerDatabaseReference.child("points").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.getValue()!=null)
                {
                    int currentPoints = ((Long)dataSnapshot.getValue()).intValue();
                    player.setPoints(currentPoints);
                    Log.d(TAG,"datachange points: " + currentPoints);
                    if(mPointsTextView!=null)
                        mPointsTextView.setText("POINTS: " + currentPoints);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        mCurrentRoomDatabaseReference.child("turnWinner").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.getValue()!=null) {
                    int winner = ((Long)dataSnapshot.getValue()).intValue();
                    if(winner>=0 && winner<=room.getOccupants()) {
                        if (player.isIT()) {
                            addPointsToPlayer(3);
                        }
                        Toast.makeText(getBaseContext(),"Player " + winner + " guess the correct answer",Toast.LENGTH_LONG).show();
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });




//        enableUIComponents(player.isIT() ? PlayerType.IT_PLAYER : PlayerType.CHAT_PLAYER);
        gameplay();
    }

    public void startRound(){
        //TODO
        //set turn to 0
        //set it to 0

    }

    public void checkTurn(){
        //check turn status
        if (room.getTurn() != 0){
            FirebaseDatabase.getInstance().getReference().child("room").child(room.getId()).child("turn").setValue(room.getTurn());
            Log.d(TAG, "Round turn: " + room.getTurn());
        }

        //get turn from firebase

        //choose IT
        if (player.getNumber() == room.getPlayers().get(room.getIt()).getNumber()) {
            player.setIT(true);
            room.getPlayers().get(room.getIt()).setIT(true);
        }

    }
    // for correct answer or matched the correct answer
    boolean isAnswered;

    public void showWord(String word){
        new AlertDialog.Builder(GameActivity.this)
                .setTitle("D00DLE THIS:")
                .setMessage(word)
                .setCancelable(true)
                .show();
    }

    public void onClickSendButton(View view) {
        Message message = new Message(mMessageEditText.getText().toString(), player.getDisplayName());
        mMessagesDatabaseReference.push().setValue(message);
        // Clear input box
        mMessageEditText.setText("");

        //check if answer is correct.
        boolean check = checkAnswer(message.getText());
        Toast.makeText(getBaseContext(),check ? "Correct answer" : "Wrong answer",Toast.LENGTH_LONG).show();

        //TODO: Call endTurn if checkAnswer is true
        if(check)
        {
            mCurrentRoomDatabaseReference.child("turnWinner").setValue(player.getNumber());
            addPointsToPlayer(5);
        }
    }

    public void gameplay() {
        mCurrentRoomDatabaseReference = FirebaseDatabase.getInstance().getReference().child("room").child(room.getId());
        //choose IT
        /*Log.e("ERRORRRRR", ((room.getPlayers().get(0)).toString()));

        if (player.getNumber() == (room.getPlayers().get(room.getIt())).getNumber()) {
            player.setIT(true);
            room.getPlayers().get(room.getIt()).setIT(true);
        }*/

        enableUIComponents(player.isIT() ? PlayerType.IT_PLAYER : PlayerType.CHAT_PLAYER);


        if (player.isIT()) {
            generateWord();


            //return to default all players IT value
            player.setIT(false);
            room.setIt();
            room.setWord("");
        }
    }

    boolean checkAnswer(String aMessage) {
        String theAnswer = room.getWord();
        return aMessage.equals(theAnswer);
    }

    void endTurn() {
//        mCurrentRoomDatabaseReference = FirebaseDatabase.getInstance().getReference().child("room").child(room.getId());
        Log.d(TAG, "Player's points surrender: " + player.getPoints());
        //DISPLAY POINTS and LEAD BOARD (fragment)
        //REMOVE TURN DATA after click next turn
        //popup window here
        int turnNumber;

        if (room.getTurn() != 0) {
            mCurrentRoomDatabaseReference.child("turn").setValue(room.getTurn());
            Log.d(TAG, "Round turn: " + room.getTurn());

            turnNumber = room.getTurn();

            if (/**answerFound &&**/ room.getTurn() <= room.getOccupants()) {
                // show popup window with scores
                mNextTurn = (Button) findViewById(R.id.turnButton);
                LayoutInflater inflater = (LayoutInflater) getApplicationContext().getSystemService(LAYOUT_INFLATER_SERVICE);
                View customView = inflater.inflate(R.layout.popup_turn, null);
                //instantiate popup window
                mPopupWindow = new PopupWindow(
                    customView,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                );

                
                //when next turn is clicked
                mNextTurn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if(!(room.getTurn() == room.getOccupants())){
                            mCurrentRoomDatabaseReference.child("messages").removeValue();
                            mCurrentRoomDatabaseReference.child("surrenderCount").removeValue();
                            //mCurrentRoomDatabaseReference.child("players").child(player.isIT());
                            mPopupWindow.dismiss();
                            gameplay();
                            
                        }else{
                            //get scores
                            //if tie then
                            //new round
                        }
                    }
                });
                mPopupWindow.showAtLocation(mLinearLayout, Gravity.CENTER, 0, 0);
            }

        } else {
            room.setTurn();
            mCurrentRoomDatabaseReference.child("turn").setValue(room.getTurn());
            Log.d(TAG, "Round turn: " + room.getTurn());

            turnNumber = room.getTurn();

            if (/**answerFound &&**/ room.getTurn() <= room.getOccupants()) {
                // show popup window with scores
                mNextTurn = (Button) findViewById(R.id.turnButton);

                LayoutInflater inflater = (LayoutInflater) getApplicationContext().getSystemService(LAYOUT_INFLATER_SERVICE);
                View customView = inflater.inflate(R.layout.popup_turn, null);
                //instantiate popup window
                mPopupWindow = new PopupWindow(
                    customView,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                );
                
                //when next turn is clicked
                mNextTurn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if(!(room.getTurn() == room.getOccupants())){
                            mCurrentRoomDatabaseReference.child("messages").removeValue();
                            mCurrentRoomDatabaseReference.child("surrenderCount").removeValue();
                            //mCurrentRoomDatabaseReference.child("players").child(player.isIT());
                            mPopupWindow.dismiss();
                            gameplay();

                        }else{
                            //get scores
                            //if tie then
                            //new round
                        }
                    }
                });

                mPopupWindow.showAtLocation(mLinearLayout, Gravity.CENTER, 0, 0);
            }
        }
        Log.d(TAG, "turn: " + room.getTurn());
    }

    public Boolean isAnswerCorrect(String message) {
        String answer = mCurrentRoomDatabaseReference.child("word").toString();
        if (answer.equals(message)) {
            return true;
        } else {
            return false;
        }
    }

    //ONLY CALL IF PLAYER IS "IT"
    //GET RANDOMIZED WORD AND SET VALUE TO FIREBASE
    private void generateWord() {
        String word = "";
        DatabaseReference mWordListDatabaseReference = mFirebaseDatabase.getReference().child("wordList");
        mWordListDatabaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Random rand = new Random();
                int value = rand.nextInt((int) dataSnapshot.getChildrenCount());
                String word = dataSnapshot.child(String.valueOf(value)).getValue().toString();
                Log.d(TAG,"generated word is: " + word);

                mCurrentRoomDatabaseReference.child("word").setValue(word);
                room.setWord(word);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });


    }

    public void  addPointsToPlayer(int points){
        Log.d(TAG,"before add points your player has " + player.getPoints() + " points.");
        mCurrentRoomDatabaseReference.child("players").child(String.valueOf(player.getNumber())).child("points").setValue(player.getPoints()+points);
        Log.d(TAG,"after add points your player has " + player.getPoints() + " points.");
    }

//    public void playerPoints(){
//        player.getPoints();
//        if (player.getPoints() == 0){
//            player.setPoints();
//            player.setITPoints();
//            Log.d(TAG, "datachange points: " + player.getPoints());
//            Toast.makeText(getBaseContext(), "Your current points is: " + player.getPoints(), Toast.LENGTH_LONG).show();
//        }else{
//            player.setPoints();
//            Log.d(TAG, "datachange points: " + player.getPoints());
//            Toast.makeText(getBaseContext(), "Your current points is: " + player.getPoints(), Toast.LENGTH_LONG).show();
//        }
//    }

    ArrayList<Stroke> strokes = new ArrayList<>();

    public void initializePaintView(PaintView.DrawingMode mode) {
        if(mDrawingDatabaseReference == null){
            initializeDrawingEvent();
        }

        // Initialize PaintView
        paintView = (PaintView) findViewById(R.id.paintView);
        paintView.setMode(mode);
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        paintView.init(metrics.heightPixels,metrics.widthPixels);
        if(mode.equals(PaintView.DrawingMode.DRAW)){
            mDrawingDatabaseReference.child("height").setValue(metrics.heightPixels);
            mDrawingDatabaseReference.child("width").setValue(metrics.widthPixels);
            paintView.setCanvasDimensions(metrics.heightPixels,metrics.widthPixels);
        } else{
            mDrawingDatabaseReference.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if(dataSnapshot.getValue()!=null){
//                    if(dataSnapshot.child("height").getValue()!=null && dataSnapshot.child("weight").getValue()!=null) {
                        Log.d(TAG, "getting hw: " + dataSnapshot.getKey() + " - " + dataSnapshot.getValue());

                        if(dataSnapshot.child("height").getValue()!=null && dataSnapshot.child("width").getValue()!=null){
                        int height = ((Long) dataSnapshot.child("height").getValue()).intValue();
                        int width = ((Long) dataSnapshot.child("width").getValue()).intValue();

                        paintView.setCanvasDimensions(height, width);
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });

        }
        paintView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                if (paintView.mode == PaintView.DrawingMode.DRAW) {
                    float x = event.getX();
                    float y = event.getY();

                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            paintView.touchStart(x, y);
                            paintView.getCurrentPathPoints().add(new PointF(x, y));
                            paintView.invalidate();
                            break;
                        case MotionEvent.ACTION_MOVE:
                            paintView.touchMove(x, y);
                            paintView.invalidate();
                            paintView.getCurrentPathPoints().add(new PointF(x, y));
                            break;
                        case MotionEvent.ACTION_UP:
                            paintView.touchUp();
                            paintView.invalidate();
                            JSONSerializer jsonSerializer = new JSONSerializer();
                            String jsonString = jsonSerializer.deepSerialize(paintView.getCurrentStroke());
                            mDrawingDatabaseReference.child("path").push().setValue(jsonString);
                            break;
                    }
                    return true;
                }
                return false;
            }
        });
    }

    public enum PlayerType {IT_PLAYER, CHAT_PLAYER, SURRENDERED_PLAYER}

    void enableUIComponents(PlayerType player_type) {
        switch(player_type){
            case IT_PLAYER:
            case CHAT_PLAYER:
                boolean isIT = (player_type == PlayerType.IT_PLAYER);
                int visibility = isIT ? View.GONE : View.VISIBLE;

                initializePaintView(isIT ? PaintView.DrawingMode.DRAW : PaintView.DrawingMode.VIEW_ONLY);
                paintView.setEnabled(isIT);

                //enable messagelist
                mMessageListView.setEnabled(!isIT);
                mMessageListView.setItemsCanFocus(!isIT);
                mMessageListView.setClickable(!isIT);
                mSurrenderButton.setVisibility(visibility);
                mMessageEditText.setVisibility(visibility);
                mSendButton.setVisibility(visibility);

                break;
            case SURRENDERED_PLAYER:
                mMessageEditText.setEnabled(false);
                mSendButton.setEnabled(false);
                mSurrenderButton.setEnabled(false);
                break;
        }
    }

    void initializeDrawingEvent(){
        mDrawingDatabaseReference = mCurrentRoomDatabaseReference.child("board");
        mDrawingEventListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                String serializedJsonString = dataSnapshot.getValue().toString();
                Log.d(TAG, "intent get string: " + serializedJsonString);
                retrieveDrawing(serializedJsonString);
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
        mDrawingDatabaseReference.child("path").addChildEventListener(mDrawingEventListener);
    }

    //CALLED IF PLAYER IS NOT "IT"
    //RETRIEVES DRAWINGS FROM FIREBASE
    void retrieveDrawing(String str) {
        if (paintView != null && paintView.isInitialized()) {
            Stroke stroke = new JSONDeserializer<Stroke>().deserialize(str);
            Log.d(TAG, "stroke properties " + stroke.getPathPoints());

            if (stroke != null) {
//                room.addStroke(stroke);
                strokes.add(stroke);
                ArrayList<Stroke> strokes = new ArrayList<>();
                strokes.add(stroke);

                paintView.drawPainting(strokes);
            } else
                Log.d(TAG, "stroke is null");
        } else
            Log.d("Debuggg", "paintview is null");
    }

    //SETS MESSAGE EVENT LISTENER
    void initializeMessageEvent(){
        mMessageEventListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                Message message = dataSnapshot.getValue(Message.class);
                mMessageAdapter.add(message);
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
        mMessagesDatabaseReference.addChildEventListener(mMessageEventListener);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        Button bOk, bCancel;
        LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(LAYOUT_INFLATER_SERVICE);
        View customView = inflater.inflate(R.layout.popup_confirm, null);
        //instantiate popup window
        mPopupWindow = new PopupWindow(
                customView,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );

        //chat activity shall start after tapping the enter button
        bOk = (Button)customView.findViewById(R.id.ok);
        bOk.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.e("pressed","press");
                Intent intent = new Intent(GameActivity.this, RegisterActivity.class);
                startActivity(intent);
                finish();
            }
        });

        //chat activity shall start after tapping the enter button
        bCancel = (Button)customView.findViewById(R.id.cancel);
        bCancel.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mPopupWindow.dismiss();
            }
        });

        mPopupWindow.showAtLocation(this.findViewById(R.id.main), Gravity.CENTER, 0, 0);
    }

    public void onClickSurrenderButton(View view){
        //increment value in firebase
        mCurrentRoomDatabaseReference.child("surrenderCount").setValue(room.getSurrenderCount() + 1);
        //disable chat
        enableUIComponents(PlayerType.SURRENDERED_PLAYER);
    }
}
