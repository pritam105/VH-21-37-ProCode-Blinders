package com.spithackathon.Krishi.activities;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import androidx.appcompat.widget.Toolbar;
import android.util.Log;
import android.view.View;

import com.spithackathon.Krishi.R;

import java.util.ArrayList;
import java.util.List;

import io.kommunicate.KmConversationBuilder;
import io.kommunicate.Kommunicate;
import io.kommunicate.callbacks.KmCallback;

public class ChatbotActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chatbot);

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

       // Kommunicate.init(this, "29ba0df0f86066036fdb23157eff8766c");
//        Kommunicate.init(this, "371bb2d90b418fc5aec4b5d810fffea03");
//
//        new KmConversationBuilder(this)
//                .launchConversation(new KmCallback() {
//                    @Override
//                    public void onSuccess(Object message) {
//                        Log.d("Conversation", "Success : " + message);
//                    }
//
//                    @Override
//                    public void onFailure(Object error) {
//                        Log.d("Conversation", "Failure : " + error);
//                    }
//                });

        Kommunicate.init(ChatbotActivity.this, "371bb2d90b418fc5aec4b5d810fffea03");
        Kommunicate.openConversation(ChatbotActivity.this);



        List<String> agentList = new ArrayList();
        //agentList.add("hexception.cfg@gmail.com"); //add your agentID

        agentList.add("oculus.sargam2019g@gmail.com"); //add your agentID
        List<String> botList = new ArrayList();
//        botList.add("ilovemycity-bvnpty"); //enter your integrated bot Ids
        botList.add("krishi-d6gar"); //enter your integrated bot Ids
        new KmConversationBuilder(ChatbotActivity.this)
                .setAgentIds(agentList)
                .setBotIds(botList)
                .createConversation(new KmCallback() {
                    @Override
                    public void onSuccess(Object message) {
                        Log.d("ChatLaunch", "Success : " + message);

                    }

                    @Override
                    public void onFailure(Object error) {
                        Log.d( "ChatLaunch", "Failure : " + error);

                    }
                });

    }
}