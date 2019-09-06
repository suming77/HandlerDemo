package com.example.handlerdemo;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.ref.WeakReference;

public class MainActivity extends AppCompatActivity implements OnClickListener {
    private String TAG = getClass().getSimpleName();

    private TextView mTextView;
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 1:
                    mTextView.setText((String) msg.obj);
                    Toast.makeText(MainActivity.this, (String) msg.obj, Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mTextView = findViewById(R.id.tv_msg);
        findViewById(R.id.btn_1).setOnClickListener(this);
        findViewById(R.id.btn_2).setOnClickListener(this);
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_1://常规模式
                sendMsgToMainThreadByWorkThread();
                break;
            case R.id.btn_2://任意线程
                handlerWorkThread();

                break;
            default:
                break;
        }
    }

    /**
     * 子线程通知主线程更新UI
     */
    private void sendMsgToMainThreadByWorkThread() {
        //创建子线程
        new Thread(new Runnable() {
            @Override
            public void run() {
                //获取消息载体
                Message msg = Message.obtain();
                msg.what = 1;
                msg.obj = "子线程通知主线程更新UI";
                mHandler.sendMessage(msg);
            }
        }).start();
    }


    private Handler handler;

    /**
     * 任意线程间的通信
     */
    private void handlerWorkThread() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Looper.prepare();//不写此代码会报异常
                // 注意：一定要在两个方法之间创建绑定当前Looper的Handler对象，
                // 否则一旦线程开始进入死循环就没法再创建Handler处理Message了
                handler = new Handler() {
                    @Override
                    public void handleMessage(final Message msg) {
                        super.handleMessage(msg);
                        switch (msg.what) {
                            case 2:
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        mTextView.setText((String) msg.obj);
                                        Toast.makeText(MainActivity.this, (String) msg.obj, Toast.LENGTH_SHORT).show();
                                        System.out.println("Str=============================" + msg.obj);
                                    }
                                });

                                break;
                        }
                    }
                };
                //开始循坏处理消息队列
                Looper.loop();//不写此代码会无法接受数据
            }
        }).start();

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                //不需要直接new Message，通过handler.obtainMessage()获取Message实例，底层直接是
                //调用了Message.obtain()方法，实现消息的复用，Message内部维护有数据缓存池
                Message message = handler.obtainMessage();
                message.what = 2;
                message.obj = "任意线程间的通信";
                handler.sendMessage(message);
            }
        }).start();
    }

    @Override
    protected void onDestroy() {
        //清空消息
        mHandler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    /**
     * @类描述 ${TODO}防止内存泄漏的handler
     */
    public static class ProtectHandler extends Handler {
        private WeakReference<Activity> mActivity;

        public ProtectHandler(Activity activity) {
            mActivity = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            Activity activity = mActivity.get();
            if (activity != null && !activity.isFinishing()) {
                //todo
            }
        }
    }
}
