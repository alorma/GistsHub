package com.alorma.github.ui.adapter.events.views;

import android.content.Context;
import android.text.Html;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.TextView;

import com.alorma.github.R;
import com.alorma.github.sdk.bean.dto.response.GithubEvent;
import com.alorma.github.sdk.bean.dto.response.IssueState;
import com.alorma.github.sdk.bean.dto.response.events.payload.IssueEventPayload;
import com.alorma.github.utils.TimeUtils;
import com.google.gson.Gson;

/**
 * Created by Bernat on 04/10/2014.
 */
public class IssueEventView extends GithubEventView<IssueEventPayload> {
    public IssueEventView(Context context) {
        super(context);
    }

    public IssueEventView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public IssueEventView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void inflate() {
        inflate(getContext(), R.layout.payload_issue, this);
    }

    @Override
    protected void populateView(GithubEvent event) {
        int textRes = R.string.event_issue_opened_by;
        if (eventPayload.issue.state == IssueState.closed) {
            textRes = R.string.event_issue_closed_by;
        }

        ImageView authorAvatar = (ImageView) findViewById(R.id.authorAvatar);

        //load the profile image from url with optimal settings
        handleImage(authorAvatar, event);

        TextView authorName = (TextView) findViewById(R.id.authorName);
        authorName.setText(Html.fromHtml(getContext().getResources().getString(textRes, event.actor.login, event.repo.name)));

        TextView textTitle = (TextView) findViewById(R.id.textTitle);

        String title = eventPayload.issue.title;

        textTitle.setText(title);

        TextView textDate = (TextView) findViewById(R.id.textDate);

        String timeString = TimeUtils.getTimeAgoString(event.created_at);

        textDate.setText(timeString);
    }

    @Override
    protected IssueEventPayload convert(Gson gson, String s) {
        return gson.fromJson(s, IssueEventPayload.class);
    }
}
