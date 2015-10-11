package com.mig.cpsudev.wordquizgame;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Random;

public class GameActivity extends AppCompatActivity {

    private static final String TAG = "GameActivity";

    private int mNumChoices;

    private ArrayList<String> mFileNameList;
    private ArrayList<String> mQuizWordList;
    private ArrayList<String> mChoiceWordList;

    private int mScore;
    private int mTotalGuesses;
    private String mAnswerFileName;

    private Random mRandom;
    private Handler mHandler;

    private TextView mQuestionNumberTextView;
    private ImageView mQuestionImageView;
    private TableLayout mButtionTabelLayout;
    private TextView mAnswerTextView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        Intent i = getIntent();
        int diff = i.getIntExtra(MainActivity.KEY_DIFFICULTY, 0);

        switch (diff) {
            case 0:
                mNumChoices = 2;
                break;
            case 1:
                mNumChoices = 4;
                break;
            case 2:
                mNumChoices = 6;
                break;
        }

        mFileNameList = new ArrayList<>();
        mQuizWordList = new ArrayList<>();
        mChoiceWordList = new ArrayList<>();

        mRandom = new Random();
        mHandler = new Handler();

        setupViews();

        getImageFileName();
    }

    private void setupViews() {
        mQuestionNumberTextView = (TextView) findViewById(R.id.questionNumberTextView);
        mQuestionImageView = (ImageView) findViewById(R.id.questionImageView);
        mButtionTabelLayout = (TableLayout) findViewById(R.id.buttonTableLayout);
        mAnswerTextView = (TextView) findViewById(R.id.answerTextView);
    }

    private void getImageFileName() {
        String[] categories = {"animals", "body", "colors", "numbers", "objects"};

        AssetManager assets = getAssets();
        for (String category : categories) {
            try {
                String[] fileNames = assets.list(category);
                for (String f : fileNames) {
                    mFileNameList.add(f.replace(".png", ""));
                }
            } catch (IOException e) {
                e.printStackTrace();
                Log.e(TAG, "Error listing file in " + category);
            }
        }

        startQuiz();

    }

    private void startQuiz() {
        mTotalGuesses = 0;
        mScore = 0;
        mQuizWordList.clear();

        while (mQuizWordList.size() < 3) {
            int randomIndex = mRandom.nextInt(mFileNameList.size());
            String fileName = mFileNameList.get(randomIndex);

            if (mQuizWordList.contains(fileName) == false) {
                mQuizWordList.add(fileName);
            }
        }

        loadNaxtQuestion();

    }

    private void loadNaxtQuestion() {
        mAnswerTextView.setText(null);
        mAnswerFileName = mQuizWordList.remove(0);

        String msg = String.format("Question %d form %d question", mScore + 1, 3);
        mQuestionNumberTextView.setText(msg);

        loadQuestionImage();
        prepareChoiceWords();
    }

    private void loadQuestionImage() {
        String category = mAnswerFileName.substring(0, mAnswerFileName.indexOf('-'));
        String filePath = category + "/" + mAnswerFileName + ".png";

        AssetManager assets = getAssets();
        InputStream stream;

        try {
            stream = assets.open(filePath); // ctrl + alt + t     for surrounding something with something
            Drawable image = Drawable.createFromStream(stream, filePath);
            mQuestionImageView.setImageDrawable(image);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void prepareChoiceWords() {
        mChoiceWordList.clear();

        while (mChoiceWordList.size() < mNumChoices) {
            int randomIndex = mRandom.nextInt(mFileNameList.size());
            String randomWord = getWord(mFileNameList.get(randomIndex));
            String answerWord = getWord(mAnswerFileName);

            if (mChoiceWordList.contains(randomWord) == false &&
                    randomWord.equals(answerWord) == false) {
                mChoiceWordList.add(randomWord);
            }
        }

        int randomIndex = mRandom.nextInt(mChoiceWordList.size());
        mChoiceWordList.set(randomIndex, getWord(mAnswerFileName));

        Log.i(TAG, "######### random word #########");
        for (String w : mChoiceWordList) {
            Log.i(TAG, w);
        }

        createChoiceButtons();
    }

    private String getWord(String fileName) {
        String word = fileName.substring(fileName.indexOf('-') + 1);
        return word;
    }

    private void createChoiceButtons() {
        for (int row = 0; row < mButtionTabelLayout.getChildCount(); row++) {
            TableRow tr = (TableRow) mButtionTabelLayout.getChildAt(row);
            tr.removeAllViews();
        }

        LayoutInflater inflater = (LayoutInflater) getSystemService(
                Context.LAYOUT_INFLATER_SERVICE
        );


        for (int row = 0; row < mNumChoices / 2; row++) {
            TableRow tr = (TableRow) mButtionTabelLayout.getChildAt(row);
            for (int col = 0; col < 2; col++) {
                Button guessButton = (Button) inflater.inflate(R.layout.guess_button, tr, false);
                guessButton.setText(mChoiceWordList.remove(0));
                guessButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        submitGuess((Button) v);
                    }
                });

                tr.addView(guessButton);
            }
        }

    }

    private void submitGuess(Button guessButton) {
        String guessWord = guessButton.getText().toString();
        String answer = getWord(mAnswerFileName);

        mTotalGuesses++;

        if (answer.equals(guessWord)) {
            MediaPlayer mp = MediaPlayer.create(this, R.raw.applause);
            mp.start();

            mScore++;
            String msg = guessWord + " You Right!!!";
            mAnswerTextView.setText(msg);
            mAnswerTextView.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
            disableAllButtons();

            // ตอบถูก และจบเกม
            if (mScore == 3) {
                String msgResult = String.format(
                        "You guess : %d\n Percent of Correct : %.1f",
                        mTotalGuesses,
                        100 * 3 / (double) mTotalGuesses
                );
                new AlertDialog.Builder(this)
                        .setTitle("Finish...")
                        .setCancelable(false)
                        .setMessage(msgResult)
                        .setPositiveButton("New Game", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                startQuiz();
                            }
                        })
                        .setNegativeButton("Back to Home", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                finish();
                            }
                        })
                        .show();
            }
            // ตอบถูกแต่ยังไม่จบเกม
            else {
                //หน่วงเวลา 2 วิ
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        loadNaxtQuestion();
                    }
                }, 2000);
            }
        } else {
            MediaPlayer mp = MediaPlayer.create(this, R.raw.fail3);
            mp.start();

            String msg = guessWord + " You Wrong!!!";
            mAnswerTextView.setText(msg);
            mAnswerTextView.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
            guessButton.setEnabled(false);
        }
    }

    private void disableAllButtons() {
        for (int row = 0; row < mButtionTabelLayout.getChildCount(); row++) {
            TableRow tr = (TableRow) mButtionTabelLayout.getChildAt(row);

            for (int col = 0; col < tr.getChildCount(); col++) {
                tr.getChildAt(col).setEnabled(false);
            }
        }
    }
}
