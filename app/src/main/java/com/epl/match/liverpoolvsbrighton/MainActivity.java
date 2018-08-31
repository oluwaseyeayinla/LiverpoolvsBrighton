package com.epl.match.liverpoolvsbrighton;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;

import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Arrays;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {

    static int NONE = 0;
    static int MATCH = 1;
    static int EXTRA = 2;
    static int BREAK = 3;
    static int OVER = 4;

    static long ONE_MILLISECOND = 1000;
    static int BREAK_TIME = 15;
    static int HALF_TIME = 45;
    static int FULL_TIME = 90;
    static int RED_CARD_LIMIT = 5;
    static int NUMBER_OF_PLAYERS = 11;

    long quitTime = 0;
    int matchTime = 0;
    int extraTime = 0;
    int overTime = 0;
    int breakTime = 0;

    int homeTeamScore = 0;
    int awayTeamScore = 0;

    int gameMode = OVER;

    int[] homeTeamStats = new int[]{};
    int[] awayTeamStats = new int[]{};

    String[] homeTeamPlayers = new String[]{};
    String[] homeTeamScorers = new String[]{};
    String[] homeTeamPlayersSentOff = new String[]{};

    String[] awayTeamPlayers = new String[]{};
    String[] awayTeamScorers = new String[]{};
    String[] awayTeamPlayersSentOff = new String[]{};

    String[] teamStats = new String[]{};

    private Timer timer;
    private Handler timerHandler = new Handler();

    TextView competitionTextView;
    TextView stadiumTextView;

    LinearLayout goalScorersLayout;
    LinearLayout playersSentOffLayout;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        homeTeamPlayers = getResources().getStringArray(R.array.home_team_players);
        awayTeamPlayers = getResources().getStringArray(R.array.away_team_players);

        teamStats = new String[]{
                getString(R.string.shots),
                getString(R.string.shots_on_target),
                getString(R.string.possession),
                getString(R.string.fouls),
                getString(R.string.yellow_cards),
                getString(R.string.red_cards),
                getString(R.string.offsides),
                getString(R.string.corners),
        };

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        goalScorersLayout = findViewById(R.id.goal_scorers_layout);
        setViewVisibility(goalScorersLayout, false);

        playersSentOffLayout = findViewById(R.id.players_sent_off_layout);
        setViewVisibility(playersSentOffLayout, false);

        competitionTextView = findViewById(R.id.competition_text_view);
        competitionTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                Intent browser= new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.competition_url)));
                startActivity(browser);
            }

        });

        stadiumTextView = findViewById(R.id.stadium_text_view);
        stadiumTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                Intent browser= new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.stadium_url)));
                startActivity(browser);
            }

        });

        displayMatchTime("");

        displayHomeTeamScore(homeTeamScore);
        displayHomeTeamScorers(homeTeamScorers);
        displayHomeTeamPlayersSentOff(homeTeamPlayersSentOff);

        displayAwayTeamScore(awayTeamScore);
        displayAwayTeamScorers(awayTeamScorers);
        displayAwayTeamPlayersSentOff(awayTeamPlayersSentOff);

        goalScorersLayout = findViewById(R.id.goal_scorers_layout);
        setViewVisibility(goalScorersLayout, false);

        playersSentOffLayout = findViewById(R.id.players_sent_off_layout);
        setViewVisibility(playersSentOffLayout, false);

        gameMode = MATCH;
        startMatchTime();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_quit) {
            quitApp();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (quitTime < System.currentTimeMillis()) {
            // on hardware back button pressed, confirm notification by toast
            confirmQuit();
        } else {
            // on hardware back button pressed, quit the app
            quitApp();
        }
    }

    /**
     *
     */
    void confirmQuit() {
        // display confirm toast
        Toast.makeText(getApplicationContext(), "Press back again to exit", Toast.LENGTH_LONG).show();
        quitTime = System.currentTimeMillis() + (ONE_MILLISECOND * 5);
    }

    /**
     * Quit and exit the application.
     */
    void quitApp() {
        // drop activity from memory
        finish();

        // kill the current activity
        int pid = android.os.Process.myPid();
        android.os.Process.killProcess(pid);

        System.exit(0);
    }

    /**
     * Add a player to the score board and increase the score for the home team by 1.
     */
    public void scoreGoalForHomeTeam(View v) {

        if (isInteractionLocked(gameMode)) {
            return;
        }

        String playerName = homeTeamPlayers[getRandomValue(1, NUMBER_OF_PLAYERS - 1)];

        while (hasValue(homeTeamPlayersSentOff, playerName)) {
            playerName = homeTeamPlayers[getRandomValue(1, NUMBER_OF_PLAYERS - 1)];
        }

        //Log.d("HomePlayer", "Scorer: " + playerName);

        String scorerAndTime;
        int index = indexOfValue(homeTeamScorers, playerName);

        if (index >= 0)
        {
            scorerAndTime = getScorerRecord(homeTeamScorers, playerName);
            scorerAndTime = modifyMatchTime(scorerAndTime, matchTime, overTime);
            homeTeamScorers[index] = scorerAndTime;
        }
        else
        {
            scorerAndTime = addMatchTime(playerName, matchTime, overTime);
            homeTeamScorers = addValue(homeTeamScorers, scorerAndTime);
        }

        displayHomeTeamScorers(homeTeamScorers);

        homeTeamScore = homeTeamScore + 1;
        displayHomeTeamScore(homeTeamScore);
    }

    /**
     * Add a player to the send off board and increase the red card score for the home team by 1.
     */
    public void sendOffPlayerInHomeTeam(View v) {

        if (isInteractionLocked(gameMode)) {
            return;
        }

        String playerName = homeTeamPlayers[getRandomValue(1, NUMBER_OF_PLAYERS - 1)];

        while (hasValue(homeTeamPlayersSentOff, playerName)) {
            playerName = homeTeamPlayers[getRandomValue(1, NUMBER_OF_PLAYERS - 1)];
        }

        //Log.d("HomePlayer", "Player Sent Off: " + playerName);

        playerName = addMatchTime(playerName, matchTime, overTime);
        homeTeamPlayersSentOff = addValue(homeTeamPlayersSentOff, playerName);
        displayHomeTeamPlayersSentOff(homeTeamPlayersSentOff);

        if (homeTeamPlayersSentOff.length >= RED_CARD_LIMIT)
        {
            forfeitMatch(0, 3);
        }
    }

    /**
     * Displays the given score for the home team.
     */
    public void displayHomeTeamScore(int score) {
        TextView scoreView = findViewById(R.id.home_team_score_text_view);
        scoreView.setText(String.valueOf(score));
    }

    /**
     *
     * @param values
     */
    public void displayHomeTeamScorers(String[] values) {
        TextView scorersTextView = findViewById(R.id.home_team_scorers_text_view);

        if (values.length > 0) {
            setTextView(scorersTextView, values);
            setViewVisibility(goalScorersLayout, true);
        } else {
            clearTextView(scorersTextView);
        }
    }

    /**
     *
     * @param values
     */
    public void displayHomeTeamPlayersSentOff(String[] values) {
        TextView sentOffTextView = findViewById(R.id.home_team_players_sent_off_text_view);
        if (values.length > 0) {
            setTextView(sentOffTextView, values);
            setViewVisibility(playersSentOffLayout, true);
        } else {
            clearTextView(sentOffTextView);
        }
    }

    /**
     * Increase the score for the away team by 1.
     */
    public void scoreGoalForAwayTeam(View v) {

        if (isInteractionLocked(gameMode)) {
            return;
        }

        String playerName = awayTeamPlayers[getRandomValue(1, NUMBER_OF_PLAYERS - 1)];

        while (hasValue(awayTeamPlayersSentOff, playerName)) {
            playerName = awayTeamPlayers[getRandomValue(1, NUMBER_OF_PLAYERS - 1)];
        }

        //Log.d("AwayPlayer", "Scorer: " + playerName);

        String scorerAndTime;
        int index = indexOfValue(awayTeamScorers, playerName);

        if (index >= 0)
        {
            scorerAndTime = getScorerRecord(awayTeamScorers, playerName);
            scorerAndTime = modifyMatchTime(scorerAndTime, matchTime, overTime);
            awayTeamScorers[index] = scorerAndTime;
        }
        else
        {
            scorerAndTime = addMatchTime(playerName, matchTime, overTime);
            awayTeamScorers = addValue(awayTeamScorers, scorerAndTime);
        }

        displayAwayTeamScorers(awayTeamScorers);

        awayTeamScore = awayTeamScore + 1;
        displayAwayTeamScore(awayTeamScore);
    }

    /**
     * Send off a random player in the away team.
     */
    public void sendOffPlayerInAwayTeam(View v) {

        if (isInteractionLocked(gameMode)) {
            return;
        }

        String playerName = awayTeamPlayers[getRandomValue(1, NUMBER_OF_PLAYERS - 1)];

        while (hasValue(awayTeamPlayersSentOff, playerName)) {
            playerName = awayTeamPlayers[getRandomValue(1, NUMBER_OF_PLAYERS - 1)];
        }

        //Log.d("AwayPlayer", "Player Sent Off: " + playerName);

        playerName = addMatchTime(playerName, matchTime, overTime);
        awayTeamPlayersSentOff = addValue(awayTeamPlayersSentOff, playerName);
        displayAwayTeamPlayersSentOff(awayTeamPlayersSentOff);

        if (awayTeamPlayersSentOff.length >= RED_CARD_LIMIT) {
            forfeitMatch(3,0);
        }
    }

    /**
     *
     * @param value
     */
    public void displayMatchTime(String value) {
        TextView matchTimeTextView = findViewById(R.id.match_time_text_view);
        matchTimeTextView.setText(String.valueOf(value));
    }

    /**
     * Displays the given score for Team B.
     */
    public void displayAwayTeamScore(int value) {
        TextView scoreView = findViewById(R.id.away_team_score_text_view);
        scoreView.setText(String.valueOf(value));
    }

    /**
     *
     * @param values
     */
    public void displayAwayTeamScorers(String[] values) {
        TextView scorersTextView = findViewById(R.id.away_team_scorers_text_view);

        if (values.length > 0) {
            setTextView(scorersTextView, values);
            setViewVisibility(goalScorersLayout, true);
        } else {
            clearTextView(scorersTextView);
        }
    }

    /**
     *
     * @param values
     */
    public void displayAwayTeamPlayersSentOff(String[] values) {
        TextView sentOffTextView = findViewById(R.id.away_team_players_sent_off_text_view);

        if (values.length > 0) {
            setTextView(sentOffTextView, values);
            setViewVisibility(playersSentOffLayout, true);
        } else {
            clearTextView(sentOffTextView);
        }
    }

    /**
     *
     * @param view
     * @param flag
     */
    void setViewVisibility(View view, boolean flag) {
        if (flag)
            view.setVisibility(View.VISIBLE);
        else
            view.setVisibility(View.GONE);
    }

    /**
     *
     * @param textView
     */
    void clearTextView(TextView textView) {
        textView.setText("");
    }

    /**
     *
     * @param textView
     * @param values
     */
    void setTextView(TextView textView, String[] values) {
        clearTextView(textView);
        for (int i = 0; i < values.length; i++) {
            if (i > 0) {
                textView.append("\n");
            }

            textView.append(values[i]);
        }
    }

    /**
     *
     * @param min
     * @param max
     * @return
     */
    int getRandomValue(int min, int max) {
        return min + new Random().nextInt(max - min);
    }

    /**
     *
     * @param value
     * @param sessionTime
     * @param extraTime
     * @return
     */
    String addMatchTime(String value, int sessionTime, int extraTime) {
        String time = String.valueOf(sessionTime);
        time = extraTime > 0 ? time + "+" + extraTime : time;
        time += "'";
        return value + " " + time;
    }

    /**
     *
     * @param value
     * @param sessionTime
     * @param extraTime
     * @return
     */
    String modifyMatchTime(String value, long sessionTime, long extraTime) {
        String time = String.valueOf(sessionTime);
        time = extraTime > 0 ? time + "+" + extraTime : time;
        time += "'";
        return value + ", " + time;
    }

    /**
     *
     * @param values
     * @param value
     * @return
     */
    boolean hasValue(String[] values, String value) {
        for (int i = 0; i < values.length; i++) {
            if (values[i].contains(value)) {
                return true;
            }
        }

        return false;
    }

    /**
     *
     * @param values
     * @param value
     * @return
     */
    int indexOfValue(String[] values, String value) {
        for (int i = 0; i < values.length; i++) {
            if (values[i].contains(value)) {
                return i;
            }
        }

        return -1;
    }

    /**
     *
     * @param values
     * @param value
     * @return
     */
    String[] addValue(String[] values, String value) {
        int newIndex = values.length;

        values = Arrays.copyOf(values, newIndex + 1);
        values[newIndex] = value;

        return values;
    }

    /**
     *
     * @param gameMode
     * @return
     */
    boolean isInteractionLocked(int gameMode) {
        if (gameMode == BREAK) {
            Toast.makeText(getApplicationContext(), "Cannot influence the game while in half time.", Toast.LENGTH_LONG).show();
            return true;
        }

        if (gameMode == OVER) {
            Toast.makeText(getApplicationContext(), "Cannot influence the game any longer. Match is over.", Toast.LENGTH_LONG).show();
            return true;
        }

        return false;
    }

    /**
     *
     * @param values
     * @param value
     * @return
     */
    String getScorerRecord(String values[], String value)
    {
        for (String item:values)
        {
            if(item.contains(value))
            {
                return item;
            }
        }

        return "";
    }

    /**
     * https://en.wikipedia.org/wiki/Forfeit_(sport)#Association%20football
     * As of Law 3 in the FIFA Law Book, https://www.fifa.com/mm/document/afdeveloping/refereeing/law_3_the_number_of_players_en_47419.pdf
     * there cannot be less than 7 players of a team, on the field during play
     * @param homeScore
     * @param awayScore
     */
    void forfeitMatch(int homeScore, int awayScore)
    {
        gameMode = OVER;

        homeTeamScorers = new String[]{};
        displayHomeTeamScorers(homeTeamScorers);

        awayTeamScorers = new String[]{};
        displayAwayTeamScorers(awayTeamScorers);

        homeTeamScore = homeScore;
        displayHomeTeamScore(homeTeamScore);

        awayTeamScore = awayScore;
        displayAwayTeamScore(awayTeamScore);

        setViewVisibility(goalScorersLayout, false);

        String homeTeam = getString(R.string.home_team);
        String awayTeam = getString(R.string.away_team);

        String winningTeam = homeScore > awayScore ? homeTeam : awayTeam;
        String losingTeam = homeScore < awayScore ? homeTeam : awayTeam;
        String message = getString(R.string.toast_forfeit_message, winningTeam, losingTeam);

        //Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
        showToast(message, Toast.LENGTH_SHORT, Gravity.BOTTOM);
    }

    void showToast(String text, int duration, int gravity)
    {
        Toast toast = Toast.makeText(getApplicationContext(), text, duration);
        toast.setGravity(gravity, 0, 0);
        toast.show();
    }

    /**
     * Resets the scores for the home team and away team
     */
    public void resetAll(View v) {

        stopMatchTime();
        gameMode = NONE;
        matchTime = overTime = breakTime = 0;

        homeTeamScore = 0;
        homeTeamPlayersSentOff = homeTeamScorers = new String[]{};

        awayTeamScore = 0;
        awayTeamPlayersSentOff = awayTeamScorers = new String[]{};

        displayMatchTime("");

        displayHomeTeamScore(homeTeamScore);
        displayHomeTeamScorers(homeTeamScorers);
        displayHomeTeamPlayersSentOff(homeTeamPlayersSentOff);

        displayAwayTeamScore(awayTeamScore);
        displayAwayTeamScorers(awayTeamScorers);
        displayAwayTeamPlayersSentOff(awayTeamPlayersSentOff);

        setViewVisibility(goalScorersLayout, false);
        setViewVisibility(playersSentOffLayout, false);

        gameMode = MATCH;
        startMatchTime();
    }

    /**
     *
     */
    private void startMatchTime() {
        timer = new Timer();
        TimerTask timerTask = new TimerTask() {
            public void run() {
                timerHandler.post(new Runnable() {
                    public void run() {
                        if (gameMode == MATCH) {
                            matchTime += 1;
                            if (matchTime % HALF_TIME == 0) {
                                overTime = 0;
                                extraTime = getRandomValue(1, 5);
                                gameMode = EXTRA;
                            }

                            displayMatchTime(addMatchTime("", matchTime, overTime));
                        } else if (gameMode == EXTRA) {
                            if (overTime >= extraTime) {
                                overTime = 0;
                                if (matchTime == HALF_TIME) {
                                    breakTime = 0;
                                    gameMode = BREAK;
                                } else if (matchTime == FULL_TIME) {
                                    gameMode = OVER;
                                }
                            } else {
                                overTime += 1;
                                displayMatchTime(addMatchTime("", matchTime, overTime));
                            }
                        } else if (gameMode == BREAK) {
                            breakTime += 1;
                            if (breakTime > BREAK_TIME) {
                                matchTime = HALF_TIME;
                                gameMode = MATCH;
                            }

                            displayMatchTime(getString(R.string.half_time));
                        } else if (gameMode == OVER) {
                            displayMatchTime(getString(R.string.full_time));
                            stopMatchTime();
                        } else {
                            displayMatchTime("");
                        }
                    }
                });
            }
        };

        timer.schedule(timerTask, ONE_MILLISECOND, ONE_MILLISECOND);
    }

    /**
     *
     */
    private void stopMatchTime() {
        if (timer != null) {
            timer.cancel();
            timer.purge();
            timer = null;
        }
    }
}
