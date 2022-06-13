package ai.elitefit.elitefitexample;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.utils.YouTubePlayerTracker;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView;

import ai.elitefit.pose.ElitePoseActivity;
import ai.elitefit.pose.models.AccessTokenResponse;
import ai.elitefit.pose.models.UploadPoseResponse;
import ai.elitefit.pose.models.Video;
import ai.elitefit.pose.services.IDataResponse;
import ai.elitefit.pose.services.OperatorService;

public class MainActivity extends ElitePoseActivity {

    private final String TAG = "MainActivity";
    private OperatorService service;
    private TextView textView;
    private TextView responseTextView;
    private TextView accuracyTextView;
    private TextView visibleTextView;

    private YouTubePlayer youTubePlayer;
    private YouTubePlayerView youTubePlayerView;
    private YouTubePlayerTracker tracker;

    /**
     * Add your API_KEY & API_SECRET
     * */
    private final String API_KEY = "";
    private final String API_SECRET = "";
    private final String API_URL = "https://elitefitforyou.com/api/";

    /**
     * Add your EliteFit VIDEO_ID
     * */
    private final int VIDEO_ID = 431;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        textView = findViewById(R.id.text_view);
        responseTextView = findViewById(R.id.response_text_view);
        accuracyTextView = findViewById(R.id.accuracy_text_view);
        visibleTextView = findViewById(R.id.visible_text_view);

        /*
         * will Ask for camera permission and load pose detection
         * */
        startRendering(R.id.elite_pose_layout);

        youTubePlayerView = findViewById(R.id.youtube_player_view);
        getLifecycle().addObserver(youTubePlayerView);

        /*
         * Creating service object
         * */
        service = new OperatorService(
                API_KEY,
                API_SECRET,
                API_URL
        );

        /*
         * getAccessToken will login user, you can use email, UUID, User ID etc as userIdentity
         * Which will help you identify the user at your end.
         * */
        service.getAccessToken("vishal@elitefit.ai", new IDataResponse<AccessTokenResponse>() {
            @Override
            public void onSuccess(AccessTokenResponse response) {
                loadVideo(VIDEO_ID);
            }
            @Override
            public void onError(String error) {
                textView.setText(error);
            }
        });
    }

    /**
     * @return return your activity layout ID
     * */
    @Override
    protected int getContentViewId() {
        return R.layout.activity_main;
    }

    /**
     * @return service object
     * */
    @Override
    protected OperatorService getService() {
        return service;
    }

    /**
     * @return current time in seconds preferably with 4 digit decimal
     * */
    @Override
    protected double getCurrentTime() {
        if (tracker != null) {
            return tracker.getCurrentSecond();
        }
        return 0;
    }

    /**
     * @return if trainer video is playing or not, You will only get accuracy when video is playing
     * */
    @Override
    protected boolean isVideoPlaying() {
        if (tracker != null) {
            return (tracker.getState() == PlayerConstants.PlayerState.PLAYING);
        }
        return false;
    }

    /**
     * This function will get called every time we have a new accuracy & calories
     * */
    @SuppressLint("SetTextI18n")
    @Override
    protected void onResult(UploadPoseResponse response) {
        try {
            String avgAccStr = response.getAvgAccuracy();
            String avgAcc = Integer.valueOf(Double.valueOf(avgAccStr).intValue()).toString();
            accuracyTextView.setText(avgAcc);
        }
        catch (Exception e) {
            Log.e(TAG, "Avg Acc Error: " + e.getMessage());
        }
        responseTextView.setText("AA: " + response.getAvgAccuracy() + " | IA: " + response.getInstanceScore() + " | CB: " + response.getCalorieBurnt());
    }

    /**
     * This function will get called every if user comes in or goes out of frame
     * */
    @Override
    protected void onVisibilityChange(boolean isVisible) {
        String msg = isVisible ? "User in Frame" : "User not in Frame";
        Log.v(TAG, msg);
        visibleTextView.setText(msg);
    }

    private void loadVideo(int videoId) {
        service.getVideoById(videoId, new IDataResponse<Video>() {
            @Override
            public void onSuccess(Video response) {

               /*
                * Create a new workout session and start the workout
                * */
                createSession(response);

                youTubePlayerView.getYouTubePlayerWhenReady(player -> {
                    player.loadVideo(response.getEmbedId(), 0);
                    youTubePlayer = player;
                    tracker = new YouTubePlayerTracker();
                    youTubePlayer.addListener(tracker);
                });
            }

            @SuppressLint("SetTextI18n")
            @Override
            public void onError(String error) {
                textView.setText("Video " + error);
            }
        });
    }

    private void createSession(Video video) {
        service.createSession(video.getId(), new IDataResponse<String>() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onSuccess(String sessionId) {
                textView.setText("Session ID: " + sessionId);
                /*
                 * Set user weight to calculate calories burned
                 * */
                setUserWeightKg(77);
                /*
                 * startWorkout will start the workout
                 * */
                startWorkout(sessionId, video);
            }

            @SuppressLint("SetTextI18n")
            @Override
            public void onError(String error) {
                textView.setText("Session " + error);
            }
        });
    }
}