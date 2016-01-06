package com.alorma.github.ui.renderers.user;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import butterknife.Bind;
import butterknife.ButterKnife;
import com.alorma.github.R;
import com.alorma.github.sdk.bean.dto.response.User;
import com.alorma.github.ui.utils.UniversalImageLoaderUtils;
import com.musenkishi.atelier.Atelier;
import com.musenkishi.atelier.ColorType;
import com.musenkishi.atelier.swatch.DarkVibrantSwatch;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.listener.ImageLoadingListener;
import com.pedrogomez.renderers.Renderer;

/**
 * Created by a557114 on 30/07/2015.
 */
public class UserRender extends Renderer<User> {

  @Bind(R.id.avatarAuthorImage) ImageView avatarAuthorImage;
  @Bind(R.id.textRootView) TextView textRootView;
  @Bind(R.id.textAuthorLogin) TextView textAuthorLogin;
  private Context context;

  public UserRender(Context context) {
    this.context = context;
  }

  @Override
  protected void setUpView(View view) {

  }

  @Override
  protected void hookListeners(View view) {
    view.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {

      }
    });
  }

  @Override
  protected View inflate(LayoutInflater layoutInflater, ViewGroup viewGroup) {
    View inflatedView = layoutInflater.inflate(R.layout.row_user_square, viewGroup, false);
    ButterKnife.bind(this, inflatedView);
    return inflatedView;
  }

  @Override
  public void render() {
    User user = getContent();
    UniversalImageLoaderUtils.loadUserAvatar(avatarAuthorImage, user);
    textAuthorLogin.setText(user.login);
  }
}
