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
import com.oklab.gitjourney.adapters.FollowingListAdapter;
import com.oklab.gitjourney.asynctasks.FollowingLoader;
import com.oklab.gitjourney.asynctasks.UserProfileAsyncTask;
import com.oklab.gitjourney.data.GitHubUserProfileDataEntry;
import com.oklab.gitjourney.data.GitHubUsersDataEntry;
import com.oklab.gitjourney.parsers.GitHubUserProfileDataParser;
import com.oklab.gitjourney.parsers.Parser;
import java.util.Calendar;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by olgakuklina on 2017-02-06.
 */

public class FollowingListFragment extends Fragment implements UserProfileAsyncTask.OnProfilesLoadedListener<GitHubUserProfileDataEntry>, SwipeRefreshLayout.OnRefreshListener {
    private static final String TAG = FollowingListFragment.class.getSimpleName();
    ArrayList<GitHubUserProfileDataEntry> profileDataEntryList;
    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private FollowingListAdapter followingListAdapter;
    private FollowingListFragment.OnFragmentInteractionListener mListener;
    private LinearLayoutManager linearLayoutManager;
    private int count = 0;
    private int currentPage = 1;
    private boolean followingExhausted = false;
    private boolean loading = false;
    private LoaderManager loaderManager() {
        return LoaderManager.getInstance(this);
    }

    public FollowingListFragment() {
    }

    public static FollowingListFragment newInstance() {
        FollowingListFragment fragment = new FollowingListFragment();
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
        followingListAdapter = new FollowingListAdapter(this.getContext());
        recyclerView.setAdapter(followingListAdapter);
        recyclerView.addOnScrollListener(new FollowingListFragment.FollowingItemsListOnScrollListener());
        swipeRefreshLayout.setOnRefreshListener(this);
        loading = true;
        Bundle bundle = new Bundle();
        bundle.putInt("page", currentPage++);
        loaderManager().initLoader(0, bundle, new FollowingLoaderCallbacks());
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
        if (context instanceof FollowingListFragment.OnFragmentInteractionListener) {
            mListener = (FollowingListFragment.OnFragmentInteractionListener) context;
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
            Log.v(TAG, "onRefresh loading true");
            swipeRefreshLayout.setRefreshing(false);
            return;
        }
        Log.v(TAG, "onRefresh loading false");
        followingListAdapter.resetAllData();
        followingExhausted = false;
        loading = true;
        currentPage = 1;
        Bundle bundle = new Bundle();
        bundle.putInt("page", currentPage++);
        loaderManager().initLoader(0, bundle, new FollowingLoaderCallbacks());
    }

    @Override
    public void OnProfilesLoaded(GitHubUserProfileDataEntry profileDataEntry) {
        Log.v(TAG, "OnProfilesLoaded " + count + " , " + profileDataEntry);
        count--;
        if (profileDataEntry != null && profileDataEntry.getLocation() != null && !profileDataEntry.getLocation().isEmpty()) {
            profileDataEntryList.add(profileDataEntry);
        }
        if (count == 0) {
            followingListAdapter.add(profileDataEntryList);
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

    private class FollowingItemsListOnScrollListener extends RecyclerView.OnScrollListener {

        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            super.onScrolled(recyclerView, dx, dy);
            int lastScrollPosition = linearLayoutManager.findLastCompletelyVisibleItemPosition();
            int itemsCount = followingListAdapter.getItemCount();
            if (lastScrollPosition == itemsCount - 1 && !followingExhausted && !loading) {
                loading = true;
                Bundle bundle = new Bundle();
                bundle.putInt("page", currentPage++);
                loaderManager().initLoader(0, bundle, new FollowingLoaderCallbacks());
            }
        }
    }

    private class FollowingLoaderCallbacks implements LoaderManager.LoaderCallbacks<List<GitHubUsersDataEntry>> {

        @Override
        public Loader<List<GitHubUsersDataEntry>> onCreateLoader(int id, Bundle args) {
            Log.v(TAG, "onCreateLoader " + args);
            return new FollowingLoader(getContext(), args.getInt("page"));
        }

        @Override
        public void onLoadFinished(Loader<List<GitHubUsersDataEntry>> loader, List<GitHubUsersDataEntry> followingDataEntryList) {
//            loading = false;
//            if (followingDataEntryList != null && followingDataEntryList.isEmpty()) {
//                followingExhausted = true;
//                loaderManager().destroyLoader(loader.getId());
//                return;
//            }
//            Parser<GitHubUserProfileDataEntry> parser = new GitHubUserProfileDataParser();
//            count = followingDataEntryList.size();
//            profileDataEntryList = new ArrayList<>(count);
//            for (GitHubUsersDataEntry entry : followingDataEntryList) {
//                new UserProfileAsyncTask<GitHubUserProfileDataEntry>(getContext(), FollowingListFragment.this, parser).execute(entry.getLogin());
//            }
//            swipeRefreshLayout.setRefreshing(false);
//            loaderManager().destroyLoader(loader.getId());

            loading = false;

            // Dữ liệu giả lập hoàn chỉnh cho danh sách người đang theo dõi
            List<GitHubUserProfileDataEntry> mockProfileDataEntryList = new ArrayList<>();
            mockProfileDataEntryList.add(new GitHubUserProfileDataEntry(
                    "Following User One", // name
                    "https://avatars.githubusercontent.com/u/3?v=4", // avatarUrl
                    "https://api.github.com/users/following1", // profileUri
                    "Danang, Vietnam", // location
                    "following1", // login
                    "Following Company", // company
                    "http://followingblog.com", // blogURI
                    "following1@example.com", // email
                    "A dedicated follower.", // bio
                    3, // publicRepos
                    1, // publicGists
                    5, // followers
                    2, // following
                    Calendar.getInstance())); // createdAt

            mockProfileDataEntryList.add(new GitHubUserProfileDataEntry(
                    "Following User Two", // name
                    "https://avatars.githubusercontent.com/u/4?v=4", // avatarUrl
                    "https://api.github.com/users/following2", // profileUri
                    "Nha Trang, Vietnam", // location
                    "following2", // login
                    "Another Following Company", // company
                    "http://anotherfollowingblog.com", // blogURI
                    "following2@example.com", // email
                    "A follower of many projects.", // bio
                    7, // publicRepos
                    2, // publicGists
                    15, // followers
                    10, // following
                    Calendar.getInstance())); // createdAt

            followingListAdapter.add(mockProfileDataEntryList);

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