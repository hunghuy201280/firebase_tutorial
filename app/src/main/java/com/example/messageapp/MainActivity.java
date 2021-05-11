/**
 * Copyright Google Inc. All Rights Reserved.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.messageapp;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

//
/*
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
*/

//
import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserInfo;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    //region create var
    private static final String TAG = "MainActivity";

    public static final String ANONYMOUS = "anonymous";
    public static final int DEFAULT_MSG_LENGTH_LIMIT = 1000;
    private static final int RC_SIGN_IN = 1;
    private static final int RC_PHOTO_PICKER = 101;

    private ListView mMessageListView;
    private MessageAdapter mMessageAdapter;
    private ProgressBar mProgressBar;
    private ImageButton mPhotoPickerButton;
    private EditText mMessageEditText;
    private Button mSendButton;
    private Context mContext = MainActivity.this;
    //endregion
    //TODO: storage 1 tạo instance
    private FirebaseStorage mFirebaseStorage;
    private StorageReference mChatPhotosStorageReference;

    private String mUsername;
    //TODO: auth 1 tạo instance
    private FirebaseAuth mFirebaseAuth;
    private FirebaseAuth.AuthStateListener mAuthStateListener;


    //TODO 1: tạo instance
    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference mMessageRef;
    private ChildEventListener mChildEventListener;
    //


    @Override
    protected void onCreate(Bundle savedInstanceState) {


         
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //region init
        // Initialize references to views
        mProgressBar = (ProgressBar) findViewById(R.id.progressBar);
        mMessageListView = (ListView) findViewById(R.id.messageListView);
        mPhotoPickerButton = (ImageButton) findViewById(R.id.photoPickerButton);
        mMessageEditText = (EditText) findViewById(R.id.messageEditText);
        mSendButton = (Button) findViewById(R.id.sendButton);

        // Initialize message ListView and its adapter
        final List<FriendlyMessage> friendlyMessages = new ArrayList<>();
        mMessageAdapter = new MessageAdapter(this, R.layout.item_message_left, friendlyMessages);
        mMessageListView.setAdapter(mMessageAdapter);

        // Initialize progress bar
        mProgressBar.setVisibility(ProgressBar.INVISIBLE);

        // ImagePickerButton shows an image picker to upload a image for a message
        mPhotoPickerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");
                intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
                startActivityForResult(Intent.createChooser(intent, "Complete action using"), RC_PHOTO_PICKER);
            }
        });

        // Enable Send button when there's text to send
        mMessageEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                mSendButton.setEnabled(charSequence.toString().trim().length() > 0);
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });
        mMessageEditText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(DEFAULT_MSG_LENGTH_LIMIT)});
        //endregion

        //TODO: storage 2 khởi tạo và ref tới firebase storage database
        mFirebaseStorage=FirebaseStorage.getInstance();
        mChatPhotosStorageReference=mFirebaseStorage.getReference().child("chat_photos");
        //TODO: auth 2 khởi tạo instance
        mFirebaseAuth = FirebaseAuth.getInstance();
        //


        //TODO 2: khởi tạo instance
        mFirebaseDatabase = FirebaseDatabase.getInstance();
        mMessageRef = mFirebaseDatabase.getReference().child("messages");

        mUsername = ANONYMOUS;


        // SendButton up data lên database, clear edit text
        mSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // TODO 3: Up message lên database khi click vào send button
                String key = mMessageRef.push().getKey();
                if (key != null) {
                    FriendlyMessage messageToSave = new FriendlyMessage(mMessageEditText.getText().toString().trim()
                            , mUsername, null, key);
                    mMessageRef.child(key).setValue(messageToSave);
                }
                // Clear input box
                mMessageEditText.setText("");
            }
        });

        //TODO 4: add child event lisener cho DatabaseReference
        mChildEventListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                mMessageAdapter.add(snapshot.getValue(FriendlyMessage.class));
                mMessageListView.setSelection(mMessageListView.getCount() - 1);
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                FriendlyMessage updatedMessage = snapshot.getValue(FriendlyMessage.class);
                mMessageAdapter.updateItem(mMessageAdapter.findMessageWithID(snapshot.getKey()), updatedMessage);
                mMessageAdapter.notifyDataSetChanged();
                mMessageListView.setSelection(mMessageListView.getCount() - 1);
            }
            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {

            }
            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        };

        mMessageRef.addChildEventListener(mChildEventListener);
        //
        //TODO: auth 3 tạo AuthStateListener cho FirebaseAuth
        //region auth
        mAuthStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser currentUser = mFirebaseAuth.getCurrentUser();
                if (currentUser == null) {
                    onSignedOutCleanup();
                } else {
                    onSignedInInitialize(currentUser.getDisplayName());
                    Log.v("LoginError", currentUser.getDisplayName() + "\n" + currentUser.getUid());
                }
            }
        };
        mFirebaseAuth.addAuthStateListener(mAuthStateListener);
        //endregion


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.sign_in_menu:
                if (mFirebaseAuth.getCurrentUser() != null) {
                    Toast.makeText(mContext, "Already Signed In", Toast.LENGTH_LONG).show();
                    return true;
                }
                //TODO: auth 4 bắt đầu activity đăng nhập
                startActivityForResult(
                        AuthUI.getInstance()
                                .createSignInIntentBuilder()
                                .setIsSmartLockEnabled(false)
                                .setAvailableProviders(Arrays.asList(
                                        new AuthUI.IdpConfig.EmailBuilder().build()
                                ))
                                .build(),
                        RC_SIGN_IN);

                return true;
            case R.id.sign_out_menu:
                mFirebaseAuth.signOut();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }


    private void onSignedInInitialize(String username) {
        mUsername = username;
        mMessageAdapter.notifyDataSetChanged();
        attachDatabaseReadListener();
    }

    private void onSignedOutCleanup() {
        mUsername = ANONYMOUS;
        mMessageAdapter.notifyDataSetChanged();

    }

    private void attachDatabaseReadListener() {
        if (mChildEventListener == null) {
            mChildEventListener = new ChildEventListener() {
                @Override
                public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                    mMessageAdapter.add(snapshot.getValue(FriendlyMessage.class));
                }
                @Override
                public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                    FriendlyMessage updatedMessage = snapshot.getValue(FriendlyMessage.class);
                    mMessageAdapter.updateItem(mMessageAdapter.findMessageWithID(snapshot.getKey()), updatedMessage);
                    mMessageAdapter.notifyDataSetChanged();
                }
                @Override
                public void onChildRemoved(@NonNull DataSnapshot snapshot) {
                }
                @Override
                public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            };
            mMessageRef.addChildEventListener(mChildEventListener);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_PHOTO_PICKER && resultCode == RESULT_OK) {
            //TODO: storage 3 xử lí sau khi người dùng chọn hình
            Uri selectedImageUri = data.getData();

            //reference tới chat_photos/<FILENAME>
            StorageReference photoRef = mChatPhotosStorageReference.child(selectedImageUri.getLastPathSegment());

            // upload file lên firebase storage
            photoRef.putFile(selectedImageUri)
                    .addOnSuccessListener(this, taskSnapshot -> photoRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        //Lấy uri của image khi đã upload thành công lên firebase storage
                        Uri downloadUrl = uri;

                        // set photo uri lên firebase database
                        String key=mMessageRef.push().getKey();
                        FriendlyMessage friendlyMessage = new FriendlyMessage(null, mUsername, downloadUrl.toString(),key);
                        mMessageRef.child(key).setValue(friendlyMessage);

                    }));
        }

    }
}
