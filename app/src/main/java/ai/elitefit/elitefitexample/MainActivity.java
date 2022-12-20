package ai.elitefit.elitefitexample;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.ui.StyledPlayerView;
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

    private ExoPlayer player;
    private StyledPlayerView playerView;

    /**
     * Add your API_KEY & API_SECRET
     * */
    private final String API_KEY = "";
    private final String API_SECRET = "";
    private final String API_URL = "https://elitefitforyou.com/api/";

    /**
     * Add your EliteFit VIDEO_ID
     * */
    private final int VIDEO_ID = 2393;
    private final String VIDEO_URL = "https://s3.ap-southeast-1.amazonaws.com/www.elitefit.ai/Ice+Skaters.mp4";

    private final boolean useYoutube = true;

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        textView = findViewById(R.id.text_view);
        responseTextView = findViewById(R.id.response_text_view);
        accuracyTextView = findViewById(R.id.accuracy_text_view);
        visibleTextView = findViewById(R.id.visible_text_view);

        /*
         * will Ask for camera permission and load pose detection
         * */
        startRendering(R.id.elite_pose_layout);

        if (useYoutube) {
            youTubePlayerView = findViewById(R.id.youtube_player_view);
            youTubePlayerView.setVisibility(View.VISIBLE);
            getLifecycle().addObserver(youTubePlayerView);
        }
        else {
            playerView = findViewById(R.id.exo_player);
            playerView.setVisibility(View.VISIBLE);
            player = new ExoPlayer.Builder(this).build();
            playerView.setPlayer(player);
        }

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
    public int getContentViewId() {
        return R.layout.activity_main;
    }

    /**
     * @return service object
     * */
    @Override
    public OperatorService getService() {
        return service;
    }

    /**
     * @return current time in seconds preferably with 4 digit decimal
     * */
    @Override
    public double getCurrentTime() {
        if (useYoutube && tracker != null) {
            return tracker.getCurrentSecond();
        }
        if (!useYoutube && player != null) {
            return player.getCurrentPosition() / 1000d;
        }
        return 0;
    }

    /**
     * @return if trainer video is playing or not, You will only get accuracy when video is playing
     * */
    @Override
    public boolean isVideoPlaying() {
        if (useYoutube && tracker != null) {
            return (tracker.getState() == PlayerConstants.PlayerState.PLAYING);
        }
        if (!useYoutube && player != null) {
            return player.isPlaying();
        }
        return false;
    }

    /**
     * This function will get called every time we have a new accuracy & calories
     * */
    @SuppressLint("SetTextI18n")
    @Override
    public void onResult(UploadPoseResponse response) {
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
    public void onVisibilityChange(boolean isVisible) {
        String msg = isVisible ? "User in Frame" : "User not in Frame";
        Log.v(TAG, msg);
        visibleTextView.setText(msg);
    }

    private void loadVideo(int videoId) {
        service.getVideoById(videoId, new IDataResponse<Video>() {
            @Override
            public void onSuccess(Video response) {
                if (useYoutube) {
                    youTubePlayerView.getYouTubePlayerWhenReady(player -> {
                        player.loadVideo(response.getEmbedId(), 0);
                        youTubePlayer = player;
                        tracker = new YouTubePlayerTracker();
                        youTubePlayer.addListener(tracker);
                    });
                }
                else {
                    try {
                        MediaItem mediaItem = MediaItem.fromUri(VIDEO_URL);
                        player.setMediaItem(mediaItem);
                        player.prepare();
                        player.play();
                    }
                    catch (Exception e) {
                        Log.e(TAG, e.getMessage());
                    }
                }
                /*
                 * Create a new workout session and start the workout
                 * */
                createSession(response);
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