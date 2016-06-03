package com.alorma.github.ui.fragment.pullrequest;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import com.alorma.github.R;
import com.alorma.github.sdk.bean.dto.response.Commit;
import com.alorma.github.sdk.bean.info.IssueInfo;
import com.alorma.github.sdk.bean.issue.IssueStoryDetail;
import com.alorma.github.sdk.bean.issue.PullRequestStoryCommit;
import com.alorma.github.sdk.services.client.GithubListClient;
import com.alorma.github.sdk.services.pullrequest.GetPullRequestCommits;
import com.alorma.github.ui.adapter.commit.PullRequestCommitsReviewCommentsAdapter;
import com.alorma.github.ui.fragment.base.LoadingListFragment;
import com.alorma.github.ui.fragment.detail.repo.BackManager;
import com.alorma.github.ui.fragment.detail.repo.PermissionsManager;
import com.alorma.gitskarios.core.Pair;
import com.mikepenz.octicons_typeface_library.Octicons;
import java.util.ArrayList;
import java.util.List;
import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import rx.Observer;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

public class PullRequestCommitsListFragment
    extends LoadingListFragment<PullRequestCommitsReviewCommentsAdapter>
    implements PermissionsManager, BackManager, Observer<List<Commit>> {

  private static final String ISSUE_INFO = "ISSUE_INFO";

  private List<Commit> commits;

  private IssueInfo issueInfo;

  public static PullRequestCommitsListFragment newInstance(IssueInfo issueInfo) {
    Bundle bundle = new Bundle();
    bundle.putParcelable(ISSUE_INFO, issueInfo);

    PullRequestCommitsListFragment fragment = new PullRequestCommitsListFragment();
    fragment.setArguments(bundle);
    return fragment;
  }

  @Override
  public void onViewCreated(View view, Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    if (getAdapter() != null) {
      getAdapter().clear();
    }
    this.commits = null;
  }

  @Override
  public void onNext(final List<Commit> commits) {
    if (this.commits == null || refreshing) {
      this.commits = new ArrayList<>();
    }
    if (commits != null && commits.size() > 0) {
      orderCommits(commits);

      //getReviewComments();

      List<IssueStoryDetail> issueStoryDetails = new ArrayList<>();

      for (Commit commit : this.commits) {
        issueStoryDetails.add(new PullRequestStoryCommit(commit));
      }

      if (refreshing || getAdapter() == null) {
        PullRequestCommitsReviewCommentsAdapter commitsAdapter =
            new PullRequestCommitsReviewCommentsAdapter(LayoutInflater.from(getActivity()), false,
                issueInfo.repoInfo);

        commitsAdapter.addAll(issueStoryDetails);
        setAdapter(commitsAdapter);
        stopRefresh();
      } else {
        getAdapter().addAll(issueStoryDetails);
      }
    } else if (getAdapter() == null || getAdapter().getItemCount() == 0) {
      setEmpty();
    }
  }

  @Override
  public void onError(Throwable error) {
    if (getAdapter() == null || getAdapter().getItemCount() == 0) {
      setEmpty();
    }
  }

  @Override
  public void setEmpty(boolean withError, int statusCode) {
    super.setEmpty(withError, statusCode);
    if (fab != null) {
      fab.setVisibility(View.INVISIBLE);
    }
  }

  @Override
  public void hideEmpty() {
    super.hideEmpty();
    if (fab != null) {
      fab.setVisibility(View.VISIBLE);
    }
  }

  @Override
  public void setPermissions(boolean admin, boolean push, boolean pull) {

  }

  @Override
  public boolean onBackPressed() {
    return true;
  }

  private void orderCommits(List<Commit> commits) {

    for (Commit commit : commits) {
      if (commit.commit.author.date != null) {
        DateTimeFormatter formatter = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");
        DateTime dt = formatter.parseDateTime(commit.commit.committer.date);

        Days days = Days.daysBetween(dt.withTimeAtStartOfDay(),
            new DateTime(System.currentTimeMillis()).withTimeAtStartOfDay());

        commit.days = days.getDays();

        this.commits.add(commit);
      }
    }
  }

  @Override
  protected void executeRequest() {
    super.executeRequest();
    setAction(new GetPullRequestCommits(issueInfo));
  }

  @Override
  protected void executePaginatedRequest(int page) {
    super.executePaginatedRequest(page);
    setAction(new GetPullRequestCommits(issueInfo, page));
  }

  private void setAction(GithubListClient<List<Commit>> client) {
    client.observable()
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .map(new Func1<Pair<List<Commit>, Integer>, List<Commit>>() {
          @Override
          public List<Commit> call(Pair<List<Commit>, Integer> listIntegerPair) {
            setPage(listIntegerPair.second);
            return listIntegerPair.first;
          }
        })
        .subscribe(this);
  }

  @Override
  protected void loadArguments() {
    if (getArguments() != null) {
      issueInfo = (IssueInfo) getArguments().getParcelable(ISSUE_INFO);
    }
  }

  @Override
  protected Octicons.Icon getNoDataIcon() {
    return Octicons.Icon.oct_diff;
  }

  @Override
  protected int getNoDataText() {
    return R.string.no_commits;
  }

  @Override
  protected boolean useFAB() {
    return false;
  }

  @Override
  public void onCompleted() {

  }

  // TODO

    /*@Override
    public void onListItemClick(final ListView l, final View v, final int position, final long id) {
        Commit item = commitsAdapter.getItem(position);

        CommitInfo info = new CommitInfo();
        info.repoInfo = issueInfo.repoInfo;
        info.sha = item.sha;

        Intent intent = CommitDetailActivity.newInstance(getActivity(), info);
        startActivity(intent);
    }*/
}
