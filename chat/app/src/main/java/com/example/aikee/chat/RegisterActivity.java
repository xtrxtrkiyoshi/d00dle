package com.example.aikee.chat;

import android.app.Activity;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.Toast;

import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;


public class RegisterActivity extends AppCompatActivity {

    private Context mContext;
    private Activity mActivity;

    //    private RelativeLayout mRelativeLayout;
    private LinearLayout mLinearLayout;
    private Button mButtonCreate;
    private Button mButtonJoin;
    private PopupWindow mPopupWindow;

    //for popup window attributes
    private Button bEnter;
    private EditText mtextfield;
    private EditText mroomfield;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        FirebaseApp.initializeApp(this);

        mContext = getApplicationContext();
        mActivity = RegisterActivity.this;

//        mRelativeLayout = (RelativeLayout) findViewById(R.id.rl);
        mLinearLayout = findViewById(R.id.cl);
        mButtonCreate = (Button)findViewById(R.id.btnCreateRoom);
        mButtonJoin = (Button)findViewById(R.id.btnJoinRoom);

        //when join button is clicked, popup window will prompt for username
        mButtonCreate.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(LAYOUT_INFLATER_SERVICE);
                View customView = inflater.inflate(R.layout.popup, null);

                //instantiate popup window
                mPopupWindow = new PopupWindow(
                        customView,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );

                //additional methods needed to make edittext work
                mPopupWindow.setFocusable(true);
                mPopupWindow.update();

                mtextfield = (EditText)customView.findViewById(R.id.textfield);


                //chat activity shall start after tapping the enter button
                bEnter = (Button)customView.findViewById(R.id.enter);
                bEnter.setOnClickListener( new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent intent = new Intent(RegisterActivity.this, LobbyActivity.class);
                        intent.putExtra("username", mtextfield.getText().toString());
                        intent.putExtra("create", true);
                        //intent.putExtra("username", "whatever");

                        startActivity(intent);
                    }
                });

                mPopupWindow.showAtLocation(mLinearLayout, Gravity.CENTER, 0, 0);
            }
        });

        //when join button is clicked, popup window will prompt for username
        mButtonJoin.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(LAYOUT_INFLATER_SERVICE);
                View customView = inflater.inflate(R.layout.popup2, null);

                //instantiate popup window
                mPopupWindow = new PopupWindow(
                        customView,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );

                //additional methods needed to make edittext work
                mPopupWindow.setFocusable(true);
                mPopupWindow.update();

                mtextfield = (EditText)customView.findViewById(R.id.textfield1);


                mroomfield = (EditText)customView.findViewById(R.id.textfield2);

                //chat activity shall start after tapping the enter button
                bEnter = (Button)customView.findViewById(R.id.enter);
                bEnter.setOnClickListener( new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        //read from firebase if room is full
                        FirebaseDatabase.getInstance().getReference().child("room").child(mroomfield.getText().toString()).addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {

                                Room room = dataSnapshot.getValue(Room.class);
                                if(room!=null) {
                                    if (room.getOccupants() >= 6) {
                                        Toast.makeText(getApplicationContext(), "Room Full", Toast.LENGTH_LONG).show();
                                    } else {
                                        Intent intent = new Intent(RegisterActivity.this, LobbyActivity.class);
                                        intent.putExtra("username", mtextfield.getText().toString());
                                        intent.putExtra("create", false);
                                        intent.putExtra("roomId", mroomfield.getText().toString());

                                        startActivity(intent);
                                    }
                                }
                                else
                                    Toast.makeText(getApplicationContext(),"Invalid room code",Toast.LENGTH_LONG).show();

                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {}
                        });
                    }

                });

                mPopupWindow.showAtLocation(mLinearLayout, Gravity.CENTER, 0, 0);
            }
        });
    }

}
