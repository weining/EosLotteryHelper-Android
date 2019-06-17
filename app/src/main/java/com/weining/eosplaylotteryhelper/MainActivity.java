package com.weining.eosplaylotteryhelper;

import android.app.Service;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.ArrayMap;
import android.util.SparseArray;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.weining.eosplaylotteryhelper.web.OkHttpUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import okhttp3.Response;

public class MainActivity extends AppCompatActivity {
    private TextView mGetLotteryInfoTv;
    private TextView mBsTv;
    private TextView mSdTv;
    private TextView mSnumTv;
    private TextView mOpenNumListTv;
    private ProgressBar mProgressBar;

    private static final int BIG = 1;
    private static final int SMALL = 2;
    private static final int SINGLE = 1;
    private static final int DOUBLE = 2;

    private int mBsType = 0;   // 1大 2小
    private int mSdType = 0;   // 1单 2双
    private int mBsNum;
    private int mSdNum;

    private int mSnum = -1;
    private int mSnumNum = 0;

    private int mLastBsNum;
    private int mLastSdNum;
    private int mLastSnumNum;

    private boolean mBsDone = false;
    private boolean mSdDone = false;

    private NumberTypeState numberTypeState;
    private boolean isLoading = false;
    private TimerHanlder mTimerHanlder;
    private Vibrator mVibrator;

    private static final int LIMIT = 7;
    private static final int SNUM_LIMIT = 4;

    private ArrayList<String> prizeNumList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mGetLotteryInfoTv = findViewById(R.id.get_lottery_state_tv);
        mGetLotteryInfoTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkNowState();
            }
        });
        mBsTv = findViewById(R.id.get_lottery_info_bs_tv);
        mSdTv = findViewById(R.id.get_lottery_info_sd_tv);
        mSnumTv = findViewById(R.id.get_lottery_info_snum_tv);
        mOpenNumListTv = findViewById(R.id.get_lottery_snumlist_tv);
        numberTypeState = new NumberTypeState();
        mProgressBar = findViewById(R.id.loading_pb);
        mVibrator = (Vibrator) getApplication().getSystemService(Service.VIBRATOR_SERVICE);
        checkNowState();
        mTimerHanlder = new TimerHanlder();
        mTimerHanlder.removeMessages(888);
        mTimerHanlder.sendEmptyMessageDelayed(888, 5000);
    }

    private void checkNowState() {
        if (!isLoading) {
            isLoading = true;
            mProgressBar.setVisibility(View.VISIBLE);
            new WebSendTask().execute("https://eosjs.eosplay.co/v1/chain/get_table_rows");
        }
    }

    class TimerHanlder extends Handler {
        @Override
        public void handleMessage(Message msg) {
            checkNowState();
            mTimerHanlder.sendEmptyMessageDelayed(888, 5000);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mTimerHanlder.removeMessages(888);
    }

    private class WebSendTask extends AsyncTask<String, Void, Response> {
        protected Response doInBackground(String... url) {
            prizeNumList.clear();
            mBsDone = false;
            mSdDone = false;
            mBsType = 0;
            mSdType = 0;
            long highNo = (System.currentTimeMillis() / 1000 - 1560592800) / 60 + 26009879 + 3;
            JSONObject jsonObject = new JSONObject();
            try {
                jsonObject.put("code", "eosplaybrand");
                jsonObject.put("json", true);
                jsonObject.put("limit", 30);
                jsonObject.put("lower_bound", highNo - 30);
                jsonObject.put("scope", "eosplaybrand");
                jsonObject.put("table", "game");
                jsonObject.put("table_key", "id");
                jsonObject.put("upper_bound", highNo);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return OkHttpUtils.doPostJson(url[0], jsonObject.toString());
        }

        protected void onPostExecute(Response result) {
            if (result.code() == 200) {
                try {
                    JSONObject jsonObject = new JSONObject(result.body().string());
                    JSONArray rowsArray = jsonObject.getJSONArray("rows");
                    boolean snumChanged = false;
                    for (int i = rowsArray.length() - 1; i >= 0; i--) {
                        JSONObject row = rowsArray.getJSONObject(i);
                        getNumberType(row.getInt("result"));
                        prizeNumList.add(row.getString("result") + " " + (numberTypeState.bsType == BIG ? "大" : "小") + " " + (numberTypeState.sdType == SINGLE ? "单" : "双"));
                        if (i == rowsArray.length() - 1) {
                            mBsType = numberTypeState.bsType;
                            mSdType = numberTypeState.sdType;
                            mBsNum = 1;
                            mSdNum = 1;
                            mSnumNum = 1;
                            mSnum = numberTypeState.sNum;
                        } else {
                            if (!mBsDone) {
                                if (mBsType == numberTypeState.bsType) {
                                    mBsNum++;
                                } else {
                                    mBsDone = true;
                                }
                            }
                            if (!mSdDone) {
                                if (mSdType == numberTypeState.sdType) {
                                    mSdNum++;
                                } else {
                                    mSdDone = true;
                                }
                            }
                            if (!snumChanged && mSnum == numberTypeState.sNum) {
                                mSnumNum++;
                            } else {
                                snumChanged = true;
                            }
                            if (mSdDone && mBsDone) {
                                break;
                            }
                        }
                    }
                    if (mBsNum >= LIMIT || mSdNum >= LIMIT || mSnumNum >= SNUM_LIMIT) {
                        if (mLastBsNum != mBsNum || mLastSdNum != mSdNum || mLastSnumNum != mSnumNum) {
                            warmUp();
                        }
                    }
                    String tempStr = "";
                    for (int i = 0; i < prizeNumList.size(); i++) {
                        tempStr = tempStr + prizeNumList.get(i) + "\n";
                    }
                    mOpenNumListTv.setText(tempStr);
                    mBsTv.setText("连续" + (mBsType == BIG ? "大" : "小") + " " + mBsNum + " 次");
                    mSdTv.setText("连续" + (mSdType == SINGLE ? "单" : "双") + " " + mSdNum + " 次");
                    mSnumTv.setText("数字 "+ mSnum+ " 连续出现 " + mSnumNum + " 次");
                    mLastSdNum = mSdNum;
                    mLastBsNum = mBsNum;
                    mLastSnumNum = mSnumNum;
                } catch (JSONException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                Toast.makeText(MainActivity.this, "没获取到结果啊", Toast.LENGTH_LONG).show();
            }
            isLoading = false;
            mProgressBar.setVisibility(View.GONE);
        }
    }

    private void warmUp() {
        AudioManager am = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
        final int ringerMode = am.getRingerMode();
        switch (ringerMode) {
            case AudioManager.RINGER_MODE_NORMAL://普通模式
                playFromRawFile();
                mVibrator.vibrate(new long[]{100, 1000, 100, 1000, 100, 1000, 100, 1000}, -1);
                break;
            case AudioManager.RINGER_MODE_VIBRATE://静音模式
                break;
            case AudioManager.RINGER_MODE_SILENT://震动模式
                mVibrator.vibrate(new long[]{100, 1000, 100, 1000, 100, 1000, 100, 1000}, -1);
                break;

        }

    }

    private void playFromRawFile() {
        try {
            MediaPlayer player = new MediaPlayer();
            AssetFileDescriptor file = getResources().openRawResourceFd(R.raw.ring);
            try {
                player.setDataSource(file.getFileDescriptor(), file.getStartOffset(), file.getLength());
                file.close();
                if (!player.isPlaying()){
                    player.prepare();
                    player.start();
                }
            } catch (IOException e) {
                player = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    class NumberTypeState {
        int bsType = 0;
        int sdType = 0;
        int sNum = -1;
    }

    private void getNumberType(int prizeNum) {
        int lastNum = prizeNum % 10;
        numberTypeState.sNum = lastNum;
        switch (lastNum) {
            case 0:
                numberTypeState.bsType = SMALL;
                numberTypeState.sdType = DOUBLE;
                break;
            case 1:
                numberTypeState.bsType = SMALL;
                numberTypeState.sdType = SINGLE;
                break;
            case 2:
                numberTypeState.bsType = SMALL;
                numberTypeState.sdType = DOUBLE;
                break;
            case 3:
                numberTypeState.bsType = SMALL;
                numberTypeState.sdType = SINGLE;
                break;
            case 4:
                numberTypeState.bsType = SMALL;
                numberTypeState.sdType = DOUBLE;
                break;
            case 5:
                numberTypeState.bsType = BIG;
                numberTypeState.sdType = SINGLE;
                break;
            case 6:
                numberTypeState.bsType = BIG;
                numberTypeState.sdType = DOUBLE;
                break;
            case 7:
                numberTypeState.bsType = BIG;
                numberTypeState.sdType = SINGLE;
                break;
            case 8:
                numberTypeState.bsType = BIG;
                numberTypeState.sdType = DOUBLE;
                break;
            case 9:
                numberTypeState.bsType = BIG;
                numberTypeState.sdType = SINGLE;
                break;
        }
    }
}
