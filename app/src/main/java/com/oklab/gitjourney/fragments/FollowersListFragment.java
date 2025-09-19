package com.oklab.gitjourney.fragments;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.oklab.gitjourney.R;
import com.oklab.gitjourney.adapters.FollowersListAdapter;
import com.oklab.gitjourney.asynctasks.FollowersLoader;
import com.oklab.gitjourney.asynctasks.UserProfileAsyncTask;
import com.oklab.gitjourney.data.GitHubUserProfileDataEntry;
import com.oklab.gitjourney.data.GitHubUsersDataEntry;
import com.oklab.gitjourney.parsers.GitHubUserProfileDataParser;
import com.oklab.gitjourney.parsers.Parser;
import java.util.ArrayList;
import java.util.Calendar;

import java.util.List;

/**
 * Created by olgakuklina on 2017-02-06.
 */

public class FollowersListFragment extends Fragment implements UserProfileAsyncTask.OnProfilesLoadedListener<GitHubUserProfileDataEntry>, SwipeRefreshLayout.OnRefreshListener {
    private static final String TAG = StarsListFragment.class.getSimpleName();
    ArrayList<GitHubUserProfileDataEntry> profileDataEntryList;
    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private FollowersListAdapter followersListAdapter;
    private FollowersListFragment.OnFragmentInteractionListener mListener;
    private LinearLayoutManager linearLayoutManager;
    private int currentPage = 1;
    private boolean followersExhausted = false;
    private boolean loading = false;
    private LoaderManager loaderManager() {
        return LoaderManager.getInstance(this);
    }
    private int count = 0;

    public FollowersListFragment() {
    }

    public static FollowersListFragment newInstance() {
        FollowersListFragment fragment = new FollowersListFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.v(TAG, "onCreateView");
        View v = inflater.inflate(R.layout.fragment_general_list, container, false);
        recyclerView = (RecyclerView) v.findViewById(R.id.items_list_recycler_view);
        swipeRefreshLayout = (SwipeRefreshLayout) v.findViewById(R.id.swipe_refresh_layout);
        return v;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Log.v(TAG, "onActivityCreated");
        linearLayoutManager = new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false);
        recyclerView.setLayoutManager(linearLayoutManager);
        followersListAdapter = new FollowersListAdapter(this.getContext());
        recyclerView.setAdapter(followersListAdapter);
        recyclerView.addOnScrollListener(new FollowersListFragment.FollowersItemsListOnScrollListener());
        swipeRefreshLayout.setOnRefreshListener(this);
        loading = true;
        Bundle bundle = new Bundle();
        bundle.putInt("page", currentPage++);
        loaderManager().initLoader(0, bundle, new FollowersListFragment.FollowersLoaderCallbacks());
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof FollowersListFragment.OnFragmentInteractionListener) {
            mListener = (FollowersListFragment.OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onRefresh() {
        if (loading) {
            swipeRefreshLayout.setRefreshing(false);
            return;
        }
        followersListAdapter.resetAllData();
        followersExhausted = false;
        loading = true;
        currentPage = 1;
        Bundle bundle = new Bundle();
        bundle.putInt("page", currentPage++);
        loaderManager().initLoader(0, bundle, new FollowersListFragment.FollowersLoaderCallbacks());
    }

    @Override
    public void OnProfilesLoaded(GitHubUserProfileDataEntry profileDataEntry) {

        Log.v(TAG, "OnProfilesLoaded " + count + " , " + profileDataEntry);
        count--;
        if (profileDataEntry != null && profileDataEntry.getLocation() != null && !profileDataEntry.getLocation().isEmpty()) {
            profileDataEntryList.add(profileDataEntry);
        }
        if (count == 0) {
            followersListAdapter.add(profileDataEntryList);
        }
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }

    private class FollowersItemsListOnScrollListener extends RecyclerView.OnScrollListener {

        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            super.onScrolled(recyclerView, dx, dy);
            int lastScrollPosition = linearLayoutManager.findLastCompletelyVisibleItemPosition();
            int itemsCount = followersListAdapter.getItemCount();
            Log.v(TAG, "onScrolled - imetsCount = " + itemsCount);
            Log.v(TAG, "onScrolled - lastScrollPosition = " + lastScrollPosition);
            if (lastScrollPosition == itemsCount - 1 && !followersExhausted && !loading) {
                loading = true;
                Bundle bundle = new Bundle();
                bundle.putInt("page", currentPage++);
                loaderManager().initLoader(0, bundle, new FollowersListFragment.FollowersLoaderCallbacks());
            }
        }
    }

    private class FollowersLoaderCallbacks implements LoaderManager.LoaderCallbacks<List<GitHubUsersDataEntry>> {

        @Override
        public Loader<List<GitHubUsersDataEntry>> onCreateLoader(int id, Bundle args) {
            Log.v(TAG, "onCreateLoader " + args);
            return new FollowersLoader(getContext(), args.getInt("page"));
        }

        @Override
        public void onLoadFinished(Loader<List<GitHubUsersDataEntry>> loader, List<GitHubUsersDataEntry> followersDataEntryList) {
            loading = false;

            // Dữ liệu giả lập hoàn chỉnh cho danh sách người theo dõi
            List<GitHubUserProfileDataEntry> mockProfileDataEntryList = new ArrayList<>();
            mockProfileDataEntryList.add(new GitHubUserProfileDataEntry(
                    "Mock User One", // name
                    "https://avatars.githubusercontent.com/u/1?v=4", // avatarUrl
                    "https://api.github.com/users/mockuser1", // profileUri
                    "Hanoi, Vietnam", // location
                    "mockuser1", // login
                    "Mock Company", // company
                    "http://mockblog.com", // blogURI
                    "mock1@example.com", // email
                    "A developer from Mock Company.", // bio
                    5, // publicRepos
                    0, // publicGists
                    10, // followers
                    8, // following
                    Calendar.getInstance())); // createdAt

            mockProfileDataEntryList.add(new GitHubUserProfileDataEntry(
                    "Mock User Two", // name
                    "https://avatars.githubusercontent.com/u/2?v=4", // avatarUrl
                    "https://api.github.com/users/mockuser2", // profileUri
                    "Ho Chi Minh, Vietnam", // location
                    "mockuser2", // login
                    "Another Mock Company", // company
                    "http://anothermockblog.com", // blogURI
                    "mock2@example.com", // email
                    "This is a sample bio.", // bio
                    12, // publicRepos
                    1, // publicGists
                    25, // followers
                    20, // following
                    Calendar.getInstance())); // createdAt

            followersListAdapter.add(mockProfileDataEntryList);

            swipeRefreshLayout.setRefreshing(false);
            loaderManager().destroyLoader(loader.getId());
        }

        @Override
        public void onLoaderReset(Loader<List<GitHubUsersDataEntry>> loader) {
            Log.v(TAG, "onLoaderReset");
            loading = false;
        }
    }
}
