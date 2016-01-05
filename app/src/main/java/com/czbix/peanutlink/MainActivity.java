package com.czbix.peanutlink;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.czbix.peanutlink.network.RequestHelper;
import com.czbix.peanutlink.network.model.LoginParams;
import com.czbix.peanutlink.network.model.User;

import org.json.JSONException;

import rx.Observable;
import rx.Observer;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity {

    private TextView mTvStatus;
    private TextView mBtnConnect;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mTvStatus = (TextView) findViewById(R.id.status);
        mBtnConnect = ((TextView) findViewById(R.id.connect));

        mTvStatus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkNetwork();
            }
        });
        mBtnConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mBtnConnect.setEnabled(false);
                Toast.makeText(MainActivity.this, "Connecting...", Toast.LENGTH_SHORT).show();

                RequestHelper.openNet(1, null)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new Action1<Void>() {
                            @Override
                            public void call(Void aVoid) {
                                mBtnConnect.setEnabled(true);
                                checkNetwork();
                            }
                        }, onError);
            }
        });

        if (!MiscUtils.isPeanutAp(this)) {
            Toast.makeText(this, "Not connected to Peanut AP!", Toast.LENGTH_LONG).show();
            mBtnConnect.setEnabled(false);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        final User user = PrefHelper.getInstance(this).getUser();
        if (user == null) {
            startActivityForResult(new Intent(this, LoginActivity.class), 0);
            return;
        }

        RequestHelper.updateSession(user.session);

        RequestHelper.freshUser()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<Void>() {
                    @Override
                    public void onCompleted() {
                        checkNetwork();
                    }

                    @Override
                    public void onError(Throwable e) {
                        if (e instanceof JSONException) {
                            Toast.makeText(MainActivity.this, R.string.toast_invalid_session,
                                    Toast.LENGTH_SHORT).show();
                            autoReLogin(user);
                            return;
                        }
                        throw new RuntimeException(e);
                    }

                    @Override
                    public void onNext(Void aVoid) {

                    }
                });
    }

    private void autoReLogin(final User user) {
        RequestHelper.getUTime()
                .subscribeOn(Schedulers.io())
                .map(new Func1<Long, LoginParams>() {
                    @Override
                    public LoginParams call(Long aLong) {
                        final String macAddress = MiscUtils.getMacAddress(MainActivity.this);

                        return new LoginParams(Long.toString(aLong), user.uuid,
                                user.ucode, macAddress);
                    }
                })
                .flatMap(new Func1<LoginParams, Observable<User>>() {
                    @Override
                    public Observable<User> call(LoginParams params) {
                        return RequestHelper.loginUser(params);
                    }
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<User>() {
                    @Override
                    public void call(User user) {
                        PrefHelper.getInstance(MainActivity.this).setUser(user);
                        RequestHelper.updateSession(user.session);
                        checkNetwork();
                    }
                }, onError);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_CANCELED) {
            finish();
            return;
        }

        recreate();
    }

    private void checkNetwork() {
        mTvStatus.setText(R.string.tv_checking_network);

        RequestHelper.checkNetwork()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<Void>() {
                    @Override
                    public void call(Void aVoid) {
                        mTvStatus.setText(R.string.tv_network_connected);
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        mTvStatus.setText(throwable.getMessage());
                    }
                });
    }

    private final Action1<Throwable> onError = new Action1<Throwable>() {
        @Override
        public void call(Throwable throwable) {
            Toast.makeText(MainActivity.this, throwable.getMessage(), Toast.LENGTH_LONG).show();
        }
    };
}
