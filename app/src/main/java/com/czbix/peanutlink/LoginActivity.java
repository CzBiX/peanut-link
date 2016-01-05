package com.czbix.peanutlink;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.support.v7.app.AppCompatActivity;

import android.os.Build;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.czbix.peanutlink.network.RequestHelper;
import com.czbix.peanutlink.network.model.LoginParams;
import com.czbix.peanutlink.network.model.RegResult;
import com.czbix.peanutlink.network.model.User;

import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

public class LoginActivity extends AppCompatActivity {
    private EditText mEtPhone;
    private EditText mEtCode;
    private View mProgressView;
    private View mLoginFormView;
    private Subscription mSubscribe;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        // Set up the login form.
        mEtPhone = (EditText) findViewById(R.id.phone);

        mEtCode = (EditText) findViewById(R.id.code);
        mEtCode.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == R.id.login || id == EditorInfo.IME_NULL) {
                    attemptLogin();
                    return true;
                }
                return false;
            }
        });

        final Button btnGetCode = (Button) findViewById(R.id.get_reg_code);
        btnGetCode.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                final String phone = mEtPhone.getText().toString();
                if (!isValidPhone(phone)) {
                    return;
                }

                btnGetCode.setEnabled(false);
                RequestHelper.getRegCode(phone)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new Action1<Void>() {
                            @Override
                            public void call(Void aVoid) {
                                // empty
                            }
                        }, onError);
            }
        });

        Button btnSignIn = (Button) findViewById(R.id.sign_in_button);
        btnSignIn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptLogin();
            }
        });

        mLoginFormView = findViewById(R.id.login_form);
        mProgressView = findViewById(R.id.login_progress);
    }

    private static boolean isValidPhone(String phone) {
        return !phone.isEmpty() && phone.length() == 11;
    }


    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    private void attemptLogin() {
        if (mSubscribe != null) {
            return;
        }

        // Reset errors.
        mEtPhone.setError(null);
        mEtCode.setError(null);

        // Store values at the time of the login attempt.
        String phone = mEtPhone.getText().toString();
        String code = mEtCode.getText().toString();

        TextView focusView = null;
        if (!isValidPhone(phone)) {
            focusView = mEtPhone;
        } else if (code.isEmpty() || code.length() < 4) {
            focusView = mEtCode;
        }

        if (focusView != null) {
            focusView.requestFocus();
            focusView.setError(getString(R.string.error_invalid_input));
            return;
        }

        showProgress(true);

        mSubscribe = RequestHelper.regUser(phone, code)
                .subscribeOn(Schedulers.io())
                .flatMap(new Func1<RegResult, Observable<LoginParams>>() {
                    @Override
                    public Observable<LoginParams> call(final RegResult regResult) {
                        return RequestHelper.getUTime()
                                .map(new Func1<Long, LoginParams>() {
                                    @Override
                                    public LoginParams call(Long aLong) {
                                        final String macAddress = MiscUtils.getMacAddress(LoginActivity.this);

                                        return new LoginParams(Long.toString(aLong), regResult.uuid,
                                                regResult.ucode, macAddress);
                                    }
                                });
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
                        PrefHelper.getInstance(LoginActivity.this).setUser(user);
                        setResult(RESULT_OK);
                        finish();
                    }
                }, onError);
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
            mLoginFormView.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
                }
            });

            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mProgressView.animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    private final Action1<Throwable> onError = new Action1<Throwable>() {
        @Override
        public void call(Throwable throwable) {
            Toast.makeText(LoginActivity.this, throwable.getMessage(), Toast.LENGTH_LONG).show();
            mSubscribe = null;
        }
    };
}

