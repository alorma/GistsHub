package com.alorma.github.ui.activity;

import android.accounts.AccountAuthenticatorActivity;
import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.StringRes;
import android.support.design.widget.TextInputLayout;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Toast;
import butterknife.BindView;
import butterknife.ButterKnife;
import com.afollestad.materialdialogs.MaterialDialog;
import com.alorma.github.R;
import com.alorma.github.ui.activity.login.AlternateLoginActivity;
import com.alorma.github.ui.activity.login.OtpCodeActivity;
import com.alorma.github.utils.KeyboardUtils;
import core.User;

public class WelcomeActivity extends AccountAuthenticatorActivity implements WelcomePresenterViewInterface {

  private static final int OTP_REQUEST = 1121;

  @BindView(R.id.openLogin) View buttonLogin;
  @BindView(R.id.openAlternateLogin) View buttonLoginToken;
  @BindView(R.id.login_username) TextInputLayout loginUsername;
  @BindView(R.id.login_password) TextInputLayout loginPassword;
  private WelcomePresenter welcomePresenter;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_welcome);
    ButterKnife.bind(this);

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      getWindow().addFlags(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
    }

    welcomePresenter = new WelcomePresenter(this);

    buttonLogin.setEnabled(false);

    TextWatcher buttonEnablerTextWatcher = new ButtonEnablerTextWatcher();
    addTextWatcher(loginUsername, buttonEnablerTextWatcher);
    addTextWatcher(loginPassword, buttonEnablerTextWatcher);

    buttonLogin.setOnClickListener(v -> {
      String username = getFromTextInputLayout(loginUsername);
      String passwords = getFromTextInputLayout(loginPassword);
      if (!TextUtils.isEmpty(username) && !TextUtils.isEmpty(passwords)) {
        login(username, passwords);
      }
    });
  }

  private void login(String username, String passwords) {
    welcomePresenter.login(username, passwords);
  }

  @Override
  public void willLogin() {
    buttonLogin.setEnabled(false);
  }

  @Override
  public void onErrorUnauthorized() {
    loginPassword.setError(getString(R.string.unauthorized_login));
    showAlternateLogin();
  }

  private void showAlternateLogin() {
    buttonLoginToken.setVisibility(View.VISIBLE);
    buttonLoginToken.setOnClickListener(view -> openAlternateLogin());
  }

  private void openAlternateLogin() {
    Intent intent = new Intent(this, AlternateLoginActivity.class);
    startActivity(intent);
    finish();
  }

  @Override
  public void onErrorTwoFactorException() {
    show2faRequest(R.string.write_otp_code_sms);
  }

  @Override
  public void onErrorTwoFactorAppException() {
    show2faRequest(R.string.write_otp_code_app);
  }

  private void show2faRequest(@StringRes int message) {

    new MaterialDialog.Builder(this).input("Otp code", null, true, (dialog, input) -> {

    }).positiveText("Ok").negativeText("Cancel").onPositive((dialog, which) -> {
      if (dialog.getInputEditText() != null && dialog.getInputEditText().getText() != null) {
        String otpCode = dialog.getInputEditText().getText().toString();
        welcomePresenter.setOtpCode(otpCode);
      }
    }).title(message).show();

  }

  @Override
  public void onGenericError() {
    Toast.makeText(this, "Error login", Toast.LENGTH_SHORT).show();
  }

  @Override
  public void finishAccess(final User user) {
    setResult(Activity.RESULT_OK);

    MainActivity.startActivity(this);
    finish();
  }

  @Override
  public void didLogin() {
    buttonLogin.setEnabled(true);
    if (buttonLogin != null) {
      KeyboardUtils.lowerKeyboard(this);
    }
  }

  public String getFromTextInputLayout(TextInputLayout inputLayout) {
    if (inputLayout != null && inputLayout.getEditText() != null && inputLayout.getEditText().getText() != null) {
      return inputLayout.getEditText().getText().toString();
    }
    return null;
  }

  public void addTextWatcher(TextInputLayout inputLayout, TextWatcher textWatcher) {
    if (inputLayout != null && inputLayout.getEditText() != null && textWatcher != null) {
      inputLayout.getEditText().addTextChangedListener(textWatcher);
    }
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);

    if (resultCode == RESULT_OK) {
      if (requestCode == OTP_REQUEST) {
        String code = data.getStringExtra(OtpCodeActivity.EXTRA_MESSAGE);
        if (code != null) {
          welcomePresenter.setOtpCode(code);
        }
      }
    }
  }

  @Override
  protected void onStart() {
    super.onStart();
    welcomePresenter.start(this);
  }

  @Override
  protected void onStop() {
    welcomePresenter.stop();
    super.onStop();
  }

  private class ButtonEnablerTextWatcher implements TextWatcher {
    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
      String username = getFromTextInputLayout(loginUsername);
      String passwords = getFromTextInputLayout(loginPassword);

      buttonLogin.setEnabled(!TextUtils.isEmpty(username) && !TextUtils.isEmpty(passwords));
    }

    @Override
    public void afterTextChanged(Editable s) {

    }
  }
}
