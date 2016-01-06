package com.alorma.github.ui.view.issue;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.text.Html;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.alorma.github.R;
import com.alorma.github.sdk.bean.issue.PullRequestStoryCommit;
import com.alorma.github.ui.utils.UniversalImageLoaderUtils;
import com.alorma.github.utils.AttributesUtils;
import com.mikepenz.iconics.IconicsDrawable;
import com.mikepenz.octicons_typeface_library.Octicons;
import com.nostra13.universalimageloader.core.ImageLoader;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Created by Bernat on 20/07/2015.
 */
public class IssueTimelineSecondaryView extends LinearLayout {

  private ImageView profileIcon;
  private TextView shaContent;

  public IssueTimelineSecondaryView(Context context) {
    super(context);
    init();
  }

  public IssueTimelineSecondaryView(Context context, AttributeSet attrs) {
    super(context, attrs);
    init();
  }

  public IssueTimelineSecondaryView(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    init();
  }

  @TargetApi(Build.VERSION_CODES.LOLLIPOP)
  public IssueTimelineSecondaryView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
    super(context, attrs, defStyleAttr, defStyleRes);
    init();
  }

  private void init() {
    inflate(getContext(), R.layout.issue_detail_issue_timeline_secondary_view, this);
    profileIcon = (ImageView) findViewById(R.id.profileIcon);
    shaContent = (TextView) findViewById(R.id.shaContent);
  }

  public void setCommit(PullRequestStoryCommit issueStoryDetail) {
    shaContent.setText(Html.fromHtml("<b>" + issueStoryDetail.commit.shortSha() + "</b> " + issueStoryDetail.commit.commit.shortMessage()));

    UniversalImageLoaderUtils.loadUserAvatar(profileIcon, issueStoryDetail.user());
  }
}
