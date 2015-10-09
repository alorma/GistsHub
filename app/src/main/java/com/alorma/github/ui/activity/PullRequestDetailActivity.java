package com.alorma.github.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ProgressBar;

import com.afollestad.materialdialogs.MaterialDialog;
import com.alorma.github.R;
import com.alorma.gitskarios.core.client.BaseClient;
import com.alorma.gitskarios.core.client.StoreCredentials;
import com.alorma.github.sdk.Head;
import com.alorma.github.sdk.PullRequest;
import com.alorma.github.sdk.bean.dto.request.CreateMilestoneRequestDTO;
import com.alorma.github.sdk.bean.dto.request.EditIssueAssigneeRequestDTO;
import com.alorma.github.sdk.bean.dto.request.EditIssueBodyRequestDTO;
import com.alorma.github.sdk.bean.dto.request.EditIssueLabelsRequestDTO;
import com.alorma.github.sdk.bean.dto.request.EditIssueMilestoneRequestDTO;
import com.alorma.github.sdk.bean.dto.request.EditIssueRequestDTO;
import com.alorma.github.sdk.bean.dto.request.EditIssueTitleRequestDTO;
import com.alorma.github.sdk.bean.dto.request.MergeButtonRequest;
import com.alorma.github.sdk.bean.dto.response.Contributor;
import com.alorma.github.sdk.bean.dto.response.GithubComment;
import com.alorma.github.sdk.bean.dto.response.Issue;
import com.alorma.github.sdk.bean.dto.response.IssueState;
import com.alorma.github.sdk.bean.dto.response.Label;
import com.alorma.github.sdk.bean.dto.response.MergeButtonResponse;
import com.alorma.github.sdk.bean.dto.response.Milestone;
import com.alorma.github.sdk.bean.dto.response.MilestoneState;
import com.alorma.github.sdk.bean.dto.response.Repo;
import com.alorma.github.sdk.bean.dto.response.User;
import com.alorma.github.sdk.bean.info.IssueInfo;
import com.alorma.github.sdk.bean.info.RepoInfo;
import com.alorma.github.sdk.bean.issue.PullRequestStory;
import com.alorma.github.sdk.services.issues.ChangeIssueStateClient;
import com.alorma.github.sdk.services.issues.CreateMilestoneClient;
import com.alorma.github.sdk.services.issues.EditIssueClient;
import com.alorma.github.sdk.services.issues.GetMilestonesClient;
import com.alorma.github.sdk.services.issues.GithubIssueLabelsClient;
import com.alorma.github.sdk.services.issues.NewIssueCommentClient;
import com.alorma.github.sdk.services.pullrequest.MergePullRequestClient;
import com.alorma.github.sdk.services.pullrequest.story.PullRequestStoryLoader;
import com.alorma.github.sdk.services.repo.GetRepoClient;
import com.alorma.github.sdk.services.repo.GetRepoContributorsClient;
import com.alorma.github.sdk.utils.GitskariosSettings;
import com.alorma.github.ui.ErrorHandler;
import com.alorma.github.ui.activity.base.BackActivity;
import com.alorma.github.ui.adapter.issues.PullRequestDetailAdapter;
import com.alorma.github.ui.adapter.users.UsersAdapterSpinner;
import com.alorma.github.ui.listeners.IssueDetailRequestListener;
import com.alorma.github.ui.view.pullrequest.PullRequestDetailView;
import com.alorma.github.utils.ShortcutUtils;
import com.mikepenz.iconics.IconicsDrawable;
import com.mikepenz.octicons_typeface_library.Octicons;
import com.nineoldandroids.animation.Animator;
import com.nineoldandroids.animation.AnimatorSet;
import com.nineoldandroids.animation.ArgbEvaluator;
import com.nineoldandroids.animation.ValueAnimator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import retrofit.RetrofitError;
import retrofit.client.Response;

public class PullRequestDetailActivity extends BackActivity
        implements BaseClient.OnResultCallback<PullRequestStory>,
        View.OnClickListener, PullRequestDetailView.PullRequestActionsListener, IssueDetailRequestListener {

    public static final String ISSUE_INFO = "ISSUE_INFO";
    public static final String ISSUE_INFO_REPO_NAME = "ISSUE_INFO_REPO_NAME";
    public static final String ISSUE_INFO_REPO_OWNER = "ISSUE_INFO_REPO_OWNER";
    public static final String ISSUE_INFO_NUMBER = "ISSUE_INFO_NUMBER";

    private static final int NEW_COMMENT_REQUEST = 1243;
    private static final int ISSUE_BODY_EDIT = 4252;

    private boolean shouldRefreshOnBack;
    private IssueInfo issueInfo;
    private RecyclerView recyclerView;
    private FloatingActionButton fab;
    private PullRequestStory pullRequestStory;
    private int primary;
    private int primaryDark;
    private AppBarLayout appbarLayout;
    private ProgressBar loadingView;
    private Repo repository;

    public static Intent createLauncherIntent(Context context, IssueInfo issueInfo) {
        Bundle bundle = new Bundle();

        bundle.putParcelable(ISSUE_INFO, issueInfo);

        Intent intent = new Intent(context, PullRequestDetailActivity.class);
        intent.putExtras(bundle);
        return intent;
    }

    public static Intent createShortcutLauncherIntent(Context context, IssueInfo issueInfo) {
        Bundle bundle = new Bundle();

        bundle.putString(ISSUE_INFO_REPO_NAME, issueInfo.repoInfo.name);
        bundle.putString(ISSUE_INFO_REPO_OWNER, issueInfo.repoInfo.owner);
        bundle.putInt(ISSUE_INFO_NUMBER, issueInfo.num);

        Intent intent = new Intent(context, PullRequestDetailActivity.class);
        intent.putExtras(bundle);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pullrequest_detail);

        if (getIntent().getExtras() != null) {

            issueInfo = getIntent().getExtras().getParcelable(ISSUE_INFO);

            if (issueInfo == null && getIntent().getExtras().containsKey(ISSUE_INFO_NUMBER)) {
                String name = getIntent().getExtras().getString(ISSUE_INFO_REPO_NAME);
                String owner = getIntent().getExtras().getString(ISSUE_INFO_REPO_OWNER);

                RepoInfo repoInfo = new RepoInfo();
                repoInfo.name = name;
                repoInfo.owner = owner;

                int num = getIntent().getExtras().getInt(ISSUE_INFO_NUMBER);

                issueInfo = new IssueInfo();
                issueInfo.repoInfo = repoInfo;
                issueInfo.num = num;
            }

            primary = getResources().getColor(R.color.primary);
            primaryDark = getResources().getColor(R.color.primary_dark_alpha);

            findViews();
        }
    }

    private void checkEditTitle() {
        if (issueInfo != null && pullRequestStory != null && pullRequestStory.pullRequest != null) {

            StoreCredentials credentials = new StoreCredentials(this);

            GitskariosSettings settings = new GitskariosSettings(this);
            if (settings.shouldShowDialogEditIssue()) {
                if (issueInfo.repoInfo.permissions != null && issueInfo.repoInfo.permissions.push) {
                    showEditDialog(R.string.dialog_edit_issue_edit_title_and_body_by_owner);
                } else if (pullRequestStory.pullRequest.user.login.equals(credentials.getUserName())) {
                    showEditDialog(R.string.dialog_edit_issue_edit_title_and_body_by_author);
                }
            }
        }
    }

    private void showEditDialog(int content) {
        new MaterialDialog.Builder(this)
                .title(R.string.dialog_edit_issue)
                .content(content)
                .positiveText(R.string.ok)
                .show();
    }

    private void findViews() {
        appbarLayout = (AppBarLayout) findViewById(R.id.appbarLayout);

        recyclerView = (RecyclerView) findViewById(R.id.recycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        fab = (FloatingActionButton) findViewById(R.id.fabButton);

        loadingView = (ProgressBar) findViewById(R.id.loading_view);

        IconicsDrawable drawable = new IconicsDrawable(this, Octicons.Icon.oct_comment_discussion).color(Color.WHITE).sizeDp(24);

        fab.setImageDrawable(drawable);
    }

    @Override
    protected void getContent() {
        super.getContent();
        loadingView.setVisibility(View.VISIBLE);
        GetRepoClient repoClient = new GetRepoClient(this, issueInfo.repoInfo);
        repoClient.setOnResultCallback(new BaseClient.OnResultCallback<Repo>() {
            @Override
            public void onResponseOk(Repo repo, Response r) {
                repository = repo;
                issueInfo.repoInfo.permissions = repo.permissions;
                PullRequestStoryLoader pullRequestStoryLoader = new PullRequestStoryLoader(PullRequestDetailActivity.this, issueInfo);
                pullRequestStoryLoader.setOnResultCallback(PullRequestDetailActivity.this);
                pullRequestStoryLoader.execute();
            }

            @Override
            public void onFail(RetrofitError error) {

            }
        });
        repoClient.execute();
    }

    @Override
    public void onResponseOk(PullRequestStory pullRequestStory, Response r) {
        hideProgressDialog();
        this.pullRequestStory = pullRequestStory;
        this.pullRequestStory.pullRequest.repository = repository;
        applyIssue();
    }

    private void applyIssue() {
        loadingView.setVisibility(View.GONE);
        checkEditTitle();
        changeColor(pullRequestStory.pullRequest);

        fab.setVisibility(pullRequestStory.pullRequest.locked ? View.GONE : View.VISIBLE);
        fab.setOnClickListener(pullRequestStory.pullRequest.locked ? null : this);

        String status = getString(R.string.issue_status_open);
        if (IssueState.closed == pullRequestStory.pullRequest.state) {
            status = getString(R.string.issue_status_close);
        } else if (pullRequestStory.pullRequest.merged) {
            status = getString(R.string.pullrequest_status_merged);
        }
        setTitle("#" + pullRequestStory.pullRequest.number + " " + status);
        PullRequestDetailAdapter adapter = new PullRequestDetailAdapter(this, getLayoutInflater(), pullRequestStory, issueInfo.repoInfo, issueInfo.repoInfo.permissions, this);
        adapter.setIssueDetailRequestListener(this);
        recyclerView.setAdapter(adapter);

        invalidateOptionsMenu();
    }

    private void changeColor(PullRequest pullRequest) {
        int colorState = getResources().getColor(R.color.pullrequest_state_close);
        int colorStateDark = getResources().getColor(R.color.pullrequest_state_close_dark);
        if (IssueState.open == pullRequest.state) {
            colorState = getResources().getColor(R.color.pullrequest_state_open);
            colorStateDark = getResources().getColor(R.color.pullrequest_state_open_dark);
        } else if (pullRequest.merged) {
            colorState = getResources().getColor(R.color.pullrequest_state_merged);
            colorStateDark = getResources().getColor(R.color.pullrequest_state_merged);
        }

        ValueAnimator colorAnimation = ValueAnimator.ofObject(new ArgbEvaluator(), primary, colorState);
        colorAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {

            @Override
            public void onAnimationUpdate(ValueAnimator animator) {
                int color = (Integer) animator.getAnimatedValue();
                if (appbarLayout != null) {
                    appbarLayout.setBackgroundColor(color);
                }
                if (getToolbar() != null) {
                    getToolbar().setBackgroundColor(color);
                }
            }

        });

        ValueAnimator colorAnimationStatus = ValueAnimator.ofObject(new ArgbEvaluator(), primaryDark, colorStateDark);
        colorAnimationStatus.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {

            @Override
            public void onAnimationUpdate(ValueAnimator animator) {
                int color = (Integer) animator.getAnimatedValue();

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    getWindow().setStatusBarColor(color);
                }
            }
        });

        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.setDuration(750);
        animatorSet.setInterpolator(new AccelerateDecelerateInterpolator());

        animatorSet.playTogether(colorAnimation, colorAnimationStatus);


        final int finalColorState = colorState;
        final int finalColorStateDark = colorStateDark;
        animatorSet.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                primary = finalColorState;
                primaryDark = finalColorStateDark;
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
        animatorSet.start();
    }

    @Override
    public void onFail(RetrofitError error) {
        hideProgressDialog();
        MaterialDialog.Builder builder = new MaterialDialog.Builder(this);
        builder.title(R.string.ups);
        builder.content(getString(R.string.issue_detail_error, issueInfo.toString(), error.getResponse().getReason()));
        builder.positiveText(R.string.retry);
        builder.negativeText(R.string.accept);
        builder.callback(new MaterialDialog.ButtonCallback() {
            @Override
            public void onPositive(MaterialDialog dialog) {
                super.onPositive(dialog);
                getContent();
            }

            @Override
            public void onNegative(MaterialDialog dialog) {
                super.onNegative(dialog);
                finish();
            }
        });
        builder.show();
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == fab.getId()) {
            if (!pullRequestStory.pullRequest.locked) {
                onAddComment();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (this.pullRequestStory != null) {
            if (issueInfo.repoInfo.permissions != null && issueInfo.repoInfo.permissions.push) {
                getMenuInflater().inflate(R.menu.pullrequest_detail, menu);
            } else {
                getMenuInflater().inflate(R.menu.pullrequest_detail_no_permissions, menu);
            }

            MenuItem itemShare = menu.findItem(R.id.share_issue);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                itemShare.setIcon(getResources().getDrawable(R.drawable.ic_menu_share_mtrl_alpha, getTheme()));
            } else {
                itemShare.setIcon(getResources().getDrawable(R.drawable.ic_menu_share_mtrl_alpha));
            }

        }
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (this.pullRequestStory != null) {

            if (issueInfo.repoInfo.permissions != null && issueInfo.repoInfo.permissions.push) {
                if (menu.findItem(R.id.action_close_issue) != null) {
                    menu.removeItem(R.id.action_close_issue);
                }
                if (menu.findItem(R.id.action_reopen_issue) != null) {
                    menu.removeItem(R.id.action_reopen_issue);
                }
                if (pullRequestStory.pullRequest.state == IssueState.closed) {
                    MenuItem menuItem = menu.add(0, R.id.action_reopen_issue, 1, getString(R.string.reopenPullrequst));
                    menuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
                } else {
                    MenuItem menuItem = menu.add(0, R.id.action_close_issue, 1, getString(R.string.closePullRequest));
                    menuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
                }
            }
        }

        return true;
    }

    private Intent getShareIntent() {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.putExtra(Intent.EXTRA_SUBJECT, issueInfo.toString());
        intent.putExtra(Intent.EXTRA_TEXT, pullRequestStory.pullRequest.html_url);
        return intent;
    }

    public void onAddComment() {
        String hint = getString(R.string.add_comment);
        Intent intent = ContentEditorActivity.createLauncherIntent(this, issueInfo.repoInfo, issueInfo.num, hint, null, false, false);
        startActivityForResult(intent, NEW_COMMENT_REQUEST);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);

        switch (item.getItemId()) {
            case android.R.id.home:
                setResult(shouldRefreshOnBack ? RESULT_FIRST_USER : RESULT_OK);
                finish();
                break;
            case R.id.action_close_issue:
                closeIssueDialog();
                break;
            case R.id.action_reopen_issue:
                reopenIssue();
                break;
            case R.id.issue_edit_milestone:
                editMilestone();
                break;
            case R.id.issue_edit_assignee:
                openAssignee();
                break;
            case R.id.issue_edit_labels:
                openLabels();
                break;
            case R.id.share_issue:
                if (pullRequestStory != null && pullRequestStory.pullRequest != null) {
                    Intent intent = getShareIntent();
                    startActivity(intent);
                }
                break;
            case R.id.action_add_shortcut:
                ShortcutUtils.addShortcut(this, issueInfo);
                break;
        }

        return true;
    }

    private void editMilestone() {
        GetMilestonesClient milestonesClient = new GetMilestonesClient(this, issueInfo.repoInfo, MilestoneState.open);
        milestonesClient.setOnResultCallback(new MilestonesCallback());
        milestonesClient.execute();

        showProgressDialog(R.style.SpotDialog_loading_milestones);
    }


    @Override
    public void onTitleEditRequest() {
        new MaterialDialog.Builder(this)
                .title(R.string.edit_issue_title)
                .input(null, pullRequestStory.pullRequest.title, false, new MaterialDialog.InputCallback() {
                    @Override
                    public void onInput(MaterialDialog materialDialog, CharSequence charSequence) {

                        EditIssueTitleRequestDTO editIssueTitleRequestDTO = new EditIssueTitleRequestDTO();
                        editIssueTitleRequestDTO.title = charSequence.toString();
                        executeEditIssue(editIssueTitleRequestDTO, R.string.issue_change_title);
                    }
                })
                .positiveText(R.string.edit_issue_button_ok)
                .neutralText(R.string.edit_issue_button_neutral)
                .show();
    }

    @Override
    public void onContentEditRequest() {
        String body = pullRequestStory.pullRequest.body != null ? pullRequestStory.pullRequest.body.replace("\n", "<br />") : "";
        Intent launcherIntent = ContentEditorActivity.createLauncherIntent(this, issueInfo.repoInfo, issueInfo.num, getString(R.string.edit_issue_body_hint), body, true, false);
        startActivityForResult(launcherIntent, ISSUE_BODY_EDIT);
    }

    @Override
    public void mergeRequest(final Head head, Head base) {
        MaterialDialog.Builder builder = new MaterialDialog.Builder(this);
        builder.title(R.string.merge_title);
        builder.content(head.label);
        builder.input(R.string.merge_message, 0, false, new MaterialDialog.InputCallback() {
            @Override
            public void onInput(MaterialDialog materialDialog, CharSequence charSequence) {
                merge(charSequence.toString(), head.sha, issueInfo);
            }
        });
        builder.inputType(InputType.TYPE_CLASS_TEXT);
        builder.show();
    }

    private void merge(String message, String sha, IssueInfo issueInfo) {
        showProgressDialog(R.style.SpotDialog_loading_merging);
        MergeButtonRequest mergeButtonRequest = new MergeButtonRequest();
        mergeButtonRequest.commit_message = message;
        mergeButtonRequest.sha = sha;
        MergePullRequestClient mergePullRequestClient = new MergePullRequestClient(this, issueInfo, mergeButtonRequest);
        mergePullRequestClient.setOnResultCallback(new BaseClient.OnResultCallback<MergeButtonResponse>() {
            @Override
            public void onResponseOk(MergeButtonResponse mergeButtonResponse, Response r) {
                hideProgressDialog();
                Snackbar.make(fab, R.string.pull_requests_merged, Snackbar.LENGTH_SHORT).show();
                reload();
            }

            @Override
            public void onFail(RetrofitError error) {
                hideProgressDialog();
                Snackbar.make(fab, R.string.pull_requests_merged_failed, Snackbar.LENGTH_SHORT).show();
            }
        });
        mergePullRequestClient.execute();
    }

    private class MilestonesCallback implements BaseClient.OnResultCallback<List<Milestone>> {
        @Override
        public void onResponseOk(final List<Milestone> milestones, Response r) {
            hideProgressDialog();
            if (milestones.size() == 0) {
                showCreateMilestone();
            } else {
                String[] itemsMilestones = new String[milestones.size()];

                for (int i = 0; i < milestones.size(); i++) {
                    itemsMilestones[i] = milestones.get(i).title;
                }

                int selectedMilestone = -1;
                for (int i = 0; i < milestones.size(); i++) {
                    if (PullRequestDetailActivity.this.pullRequestStory.pullRequest.milestone != null) {
                        String currentMilestone = PullRequestDetailActivity.this.pullRequestStory.pullRequest.milestone.title;
                        if (currentMilestone != null && currentMilestone.equals(milestones.get(i).title)) {
                            selectedMilestone = i;
                            break;
                        }
                    }
                }

                MaterialDialog.Builder builder = new MaterialDialog.Builder(PullRequestDetailActivity.this)
                        .title(R.string.select_milestone)
                        .items(itemsMilestones)
                        .itemsCallbackSingleChoice(selectedMilestone, new MaterialDialog.ListCallbackSingleChoice() {
                            @Override
                            public boolean onSelection(MaterialDialog materialDialog, View view, int i, CharSequence text) {

                                addMilestone(milestones.get(i));

                                return false;
                            }
                        })
                        .forceStacking(true)
                        .widgetColorRes(R.color.primary)
                        .negativeText(R.string.add_milestone);

                if (selectedMilestone != -1) {
                    builder.neutralText(R.string.clear_milestone);
                }

                builder.callback(new MaterialDialog.ButtonCallback() {

                    @Override
                    public void onNegative(MaterialDialog dialog) {
                        super.onNegative(dialog);
                        showCreateMilestone();
                    }

                    @Override
                    public void onNeutral(MaterialDialog dialog) {
                        super.onNeutral(dialog);
                        clearMilestone();
                    }
                });

                builder.show();
            }
        }

        @Override
        public void onFail(RetrofitError error) {

        }
    }

    private void showCreateMilestone() {
        MaterialDialog.Builder builder = new MaterialDialog.Builder(this);
        builder.title(R.string.add_milestone);
        builder.content(R.string.add_milestone_description);
        builder.input(R.string.add_milestone_hint, 0, new MaterialDialog.InputCallback() {
            @Override
            public void onInput(MaterialDialog materialDialog, CharSequence milestoneName) {
                createMilestone(milestoneName.toString());
            }
        })
                .negativeText(R.string.cancel);

        builder.show();
    }

    private void createMilestone(String milestoneName) {
        CreateMilestoneRequestDTO createMilestoneRequestDTO = new CreateMilestoneRequestDTO(milestoneName);

        CreateMilestoneClient createMilestoneClient = new CreateMilestoneClient(this, issueInfo.repoInfo, createMilestoneRequestDTO);
        createMilestoneClient.setOnResultCallback(new BaseClient.OnResultCallback<Milestone>() {
            @Override
            public void onResponseOk(Milestone milestone, Response r) {
                addMilestone(milestone);
            }

            @Override
            public void onFail(RetrofitError error) {
                hideProgressDialog();
            }
        });
        createMilestoneClient.execute();
    }

    private void addMilestone(Milestone milestone) {
        showProgressDialog(R.style.SpotDialog_loading_adding_milestones);
        EditIssueMilestoneRequestDTO editIssueRequestDTO = new EditIssueMilestoneRequestDTO();
        editIssueRequestDTO.milestone = milestone.number;
        executeEditIssue(editIssueRequestDTO, R.string.issue_change_add_milestone);
    }

    private void clearMilestone() {
        showProgressDialog(R.style.SpotDialog_clear_milestones);
        EditIssueMilestoneRequestDTO editIssueRequestDTO = new EditIssueMilestoneRequestDTO();
        editIssueRequestDTO.milestone = null;
        executeEditIssue(editIssueRequestDTO, R.string.issue_change_clear_milestone);
    }


    /**
     * Assignee
     */

    private void openAssignee() {
        GetRepoContributorsClient contributorsClient = new GetRepoContributorsClient(getApplicationContext(), issueInfo.repoInfo);
        contributorsClient.setOnResultCallback(new ContributorsCallback());
        contributorsClient.execute();
    }

    private class ContributorsCallback implements BaseClient.OnResultCallback<List<Contributor>> {
        @Override
        public void onResponseOk(List<Contributor> contributors, Response r) {
            final List<User> users = new ArrayList<>();
            String owner = issueInfo.repoInfo.owner;
            boolean exist = false;
            if (contributors != null) {
                for (Contributor contributor : contributors) {
                    exist = contributor.author.login.equals(owner);
                    users.add(contributor.author);
                }
            }

            if (!exist) {
                User user = new User();
                user.login = owner;
                users.add(user);
            }

            Collections.reverse(users);
            UsersAdapterSpinner assigneesAdapter = new UsersAdapterSpinner(PullRequestDetailActivity.this, users);
            MaterialDialog.Builder builder = new MaterialDialog.Builder(PullRequestDetailActivity.this);
            builder.adapter(assigneesAdapter, new MaterialDialog.ListCallback() {
                @Override
                public void onSelection(MaterialDialog materialDialog, View view, int i, CharSequence charSequence) {
                    User user = users.get(i);
                    setAssigneeUser(user);
                    materialDialog.dismiss();
                }
            });
            builder.negativeText(R.string.no_assignee);
            builder.callback(new MaterialDialog.ButtonCallback() {
                @Override
                public void onNegative(MaterialDialog dialog) {
                    super.onNegative(dialog);
                    setAssigneeUser(null);
                }
            });
            builder.show();
        }

        @Override
        public void onFail(RetrofitError error) {

        }
    }

    private void setAssigneeUser(User user) {
        showProgressDialog(R.style.SpotDialog_loading_adding_assignee);
        EditIssueAssigneeRequestDTO editIssueRequestDTO = new EditIssueAssigneeRequestDTO();
        if (user != null) {
            editIssueRequestDTO.assignee = user.login;
        } else {
            editIssueRequestDTO.assignee = null;
        }
        executeEditIssue(editIssueRequestDTO, R.string.issue_change_assignee);
    }

    private void executeEditIssue(EditIssueRequestDTO editIssueRequestDTO, final int changedText) {
        EditIssueClient client = new EditIssueClient(PullRequestDetailActivity.this, issueInfo, editIssueRequestDTO);
        client.setOnResultCallback(new BaseClient.OnResultCallback<Issue>() {
            @Override
            public void onResponseOk(Issue issue, Response r) {
                shouldRefreshOnBack = true;
                hideProgressDialog();
                getContent();

                Snackbar.make(fab, changedText, Snackbar.LENGTH_SHORT).show();
            }

            @Override
            public void onFail(RetrofitError error) {
                ErrorHandler.onError(PullRequestDetailActivity.this, "Issue detail", error);
                hideProgressDialog();
            }
        });
        client.execute();
    }

    /**
     * Labels
     */

    private void openLabels() {
        GithubIssueLabelsClient labelsClient = new GithubIssueLabelsClient(this, issueInfo.repoInfo);
        labelsClient.setOnResultCallback(new LabelsCallback());
        labelsClient.execute();
    }

    private class LabelsCallback implements BaseClient.OnResultCallback<List<Label>> {

        private CharSequence[] selectedLabels;
        private Integer[] positionsSelectedLabels;

        @Override
        public void onResponseOk(List<Label> labels, Response r) {
            if (labels != null) {
                List<String> items = new ArrayList<>();
                List<String> selectedLabels = new ArrayList<>();
                List<Integer> positionsSelectedLabels = new ArrayList<>();

                List<String> currentLabels = new ArrayList<>();
                for (Label label : pullRequestStory.pullRequest.labels) {
                    currentLabels.add(label.name);
                }

                int i = 0;
                for (Label label : labels) {
                    items.add(label.name);
                    if (currentLabels.contains(label.name)) {
                        selectedLabels.add(label.name);
                        positionsSelectedLabels.add(i);
                    }
                    i++;
                }

                LabelsCallback.this.selectedLabels = selectedLabels.toArray(new String[selectedLabels.size()]);

                MaterialDialog.Builder builder = new MaterialDialog.Builder(PullRequestDetailActivity.this);
                builder.items(items.toArray(new String[items.size()]));
                builder.alwaysCallMultiChoiceCallback();
                builder.itemsCallbackMultiChoice(positionsSelectedLabels.toArray(new Integer[positionsSelectedLabels.size()]), new MaterialDialog.ListCallbackMultiChoice() {
                    @Override
                    public boolean onSelection(MaterialDialog materialDialog, Integer[] integers, CharSequence[] charSequences) {
                        LabelsCallback.this.selectedLabels = charSequences;
                        LabelsCallback.this.positionsSelectedLabels = integers;
                        return true;
                    }
                });
                builder.forceStacking(true);
                builder.positiveText(R.string.ok);
//                builder.neutralText(R.string.add_new_label);
                builder.negativeText(R.string.clear_labels);
                builder.callback(new MaterialDialog.ButtonCallback() {
                    @Override
                    public void onPositive(MaterialDialog dialog) {
                        super.onPositive(dialog);
                        setLabels(LabelsCallback.this.selectedLabels);
                    }

                    @Override
                    public void onNegative(MaterialDialog dialog) {
                        super.onNegative(dialog);
                        LabelsCallback.this.selectedLabels = null;
                        LabelsCallback.this.positionsSelectedLabels = null;
                        setLabels(null);
                    }

//                    @Override
//                    public void onNeutral(MaterialDialog dialog) {
//                        super.onNeutral(dialog);
//                    }
                });
                builder.show();
            }
        }

        @Override
        public void onFail(RetrofitError error) {
            ErrorHandler.onError(PullRequestDetailActivity.this, "Issue detail", error);
        }
    }

    private void setLabels(CharSequence[] selectedLabels) {
        if (selectedLabels != null) {

            if (selectedLabels.length > 0) {
                String[] labels = new String[selectedLabels.length];
                for (int i = 0; i < selectedLabels.length; i++) {
                    labels[i] = selectedLabels[i].toString();
                }
                EditIssueLabelsRequestDTO labelsRequestDTO = new EditIssueLabelsRequestDTO();
                labelsRequestDTO.labels = labels;
                executeEditIssue(labelsRequestDTO, R.string.issue_change_labels);
            } else {
                EditIssueLabelsRequestDTO labelsRequestDTO = new EditIssueLabelsRequestDTO();
                labelsRequestDTO.labels = new String[]{};
                executeEditIssue(labelsRequestDTO, R.string.issue_change_labels_clear);
            }
        } else {
            EditIssueLabelsRequestDTO labelsRequestDTO = new EditIssueLabelsRequestDTO();
            labelsRequestDTO.labels = new String[]{};
            executeEditIssue(labelsRequestDTO, R.string.issue_change_labels_clear);
        }
    }

    /**
     * Close issue
     */

    private void closeIssueDialog() {
        String title = getString(R.string.closeIssue);
        String accept = getString(R.string.accept);
        String cancel = getString(R.string.cancel);

        MaterialDialog dialog = new MaterialDialog.Builder(this)
                .title(title)
                .positiveText(accept)
                .negativeText(cancel)
                .callback(new MaterialDialog.ButtonCallback() {
                    @Override
                    public void onPositive(MaterialDialog materialDialog) {
                        super.onPositive(materialDialog);
                        closeIssue();
                    }
                })
                .build();

        dialog.show();
    }

    private void closeIssue() {
        changeIssueState(IssueState.closed);
    }

    private void reopenIssue() {
        changeIssueState(IssueState.open);
    }

    private void changeIssueState(IssueState state) {
        ChangeIssueStateClient changeIssueStateClient = new ChangeIssueStateClient(this, issueInfo, state);
        changeIssueStateClient.setOnResultCallback(new BaseClient.OnResultCallback<Issue>() {
            @Override
            public void onResponseOk(Issue issue, Response r) {
                if (issue != null) {
                    getContent();
                    shouldRefreshOnBack = true;

                    Snackbar.make(fab, R.string.pullrequest_change, Snackbar.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFail(RetrofitError error) {

            }
        });
        changeIssueStateClient.execute();
    }

    @Override
    public void onBackPressed() {
        setResult(shouldRefreshOnBack ? RESULT_FIRST_USER : RESULT_OK);
        finish();
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null) {
            if (requestCode == NEW_COMMENT_REQUEST) {
                showProgressDialog(R.style.SpotDialog_loading_adding_comment);
                String body = data.getStringExtra(ContentEditorActivity.CONTENT);
                final NewIssueCommentClient client = new NewIssueCommentClient(this, issueInfo, body);
                client.setOnResultCallback(new BaseClient.OnResultCallback<GithubComment>() {
                    @Override
                    public void onResponseOk(GithubComment githubComment, Response r) {
                        getContent();
                        Snackbar.make(fab, R.string.add_comment_issue_ok, Snackbar.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFail(RetrofitError error) {
                        // TODO on comment fail

                        Snackbar.make(fab, R.string.add_comment_issue_fail, Snackbar.LENGTH_SHORT).setAction(R.string.retry, new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                client.execute();
                            }
                        }).show();
                    }
                });
                client.execute();

            } else if (requestCode == ISSUE_BODY_EDIT) {
                EditIssueBodyRequestDTO bodyRequestDTO = new EditIssueBodyRequestDTO();
                bodyRequestDTO.body = data.getStringExtra(ContentEditorActivity.CONTENT);

                executeEditIssue(bodyRequestDTO, R.string.issue_change_body);
            }
        }
    }
}
