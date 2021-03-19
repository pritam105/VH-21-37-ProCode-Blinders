package com.spithackathon.Krishi.utilities;

import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.util.Log;

public class TextToSpeech {
    MediaPlayer mp = null;
    String kVoiceRssServer = "http://api.voicerss.org";
    String kVoiceRSSAppKey = "097398c17f554f11a641ddbbb435a6c3";

    public void initializeMediaPlayer(String str) {

//        String url = buildSpeechUrl(" खेती की उन्नत विधियां के लिए पहला कार्ड चुने | प्रमुख समस्याएं एवं निवारण के लिए दूसरा कार्ड चुने | सरकार की योजनाएं के लिए तीसरा कार्ड चुने | कृषि चैटबॉट के लिए चौथा कार्ड चुने", "hi-in");
        String url = buildSpeechUrl(str, "hi-in");
        Log.d("output", url);

        MediaPlayer player = new MediaPlayer();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            player.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setLegacyStreamType(AudioManager.STREAM_MUSIC)
                    .build());
        } else {
            player.setAudioStreamType(AudioManager.STREAM_MUSIC);
        }
        try {
            //player.prepare();
            player.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    mp.start();
                }
            });
//            player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
//                @Override
//                public void onCompletion(MediaPlayer mp) {
//                    if(mp!=null)
//                        mp.release();
//                }
//            });
            player.setDataSource(url);
            player.prepareAsync();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private String buildSpeechUrl(String words, String language)
    {
        String url = "";

        url = kVoiceRssServer + "/?key=" + kVoiceRSSAppKey + "&t=text&hl=" + language + "&src=" + words;
        Log.d("url", url);
        return url;
    }
}
