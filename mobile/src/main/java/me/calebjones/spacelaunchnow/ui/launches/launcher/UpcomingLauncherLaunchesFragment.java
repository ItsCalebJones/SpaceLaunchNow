package me.calebjones.spacelaunchnow.ui.launches.launcher;


import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import cz.kinst.jakub.view.SimpleStatefulLayout;
import me.calebjones.spacelaunchnow.R;
import me.calebjones.spacelaunchnow.common.BaseFragment;
import me.calebjones.spacelaunchnow.common.customviews.SimpleDividerItemDecoration;
import me.calebjones.spacelaunchnow.content.data.callbacks.Callbacks;
import me.calebjones.spacelaunchnow.content.data.upcoming.UpcomingDataRepository;
import me.calebjones.spacelaunchnow.data.models.main.Agency;
import me.calebjones.spacelaunchnow.data.models.main.Launch;
import me.calebjones.spacelaunchnow.ui.main.launches.ListAdapter;
import me.calebjones.spacelaunchnow.utils.views.EndlessRecyclerViewScrollListener;
import timber.log.Timber;


public class UpcomingLauncherLaunchesFragment extends BaseFragment {

    private static final String SEARCH_TERM = "searchTerm";
    private static final String LSP_NAME = "lspName";
    private static final String LAUNCHER_ID = "launcherId";

    @BindView(R.id.recycler_view)
    RecyclerView recyclerView;
    @BindView(R.id.stateful_view)
    SimpleStatefulLayout statefulView;
    @BindView(R.id.coordinator)
    CoordinatorLayout coordinatorLayout;

    private LinearLayoutManager linearLayoutManager;
    private ListAdapter adapter;
    private String searchTerm = null;
    private String lspName = null;
    private UpcomingDataRepository upcomingDataRepository;
    private int nextOffset = 0;
    private EndlessRecyclerViewScrollListener scrollListener;
    private ArrayList<String> agencyList;
    private List<Agency> agencies;
    public boolean canLoadMore;
    private boolean statefulStateContentShow = false;
    private Context context;
    private Integer launcherId = null;

    private UpcomingLauncherLaunchesFragment.OnFragmentInteractionListener mListener;

    public UpcomingLauncherLaunchesFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param searchTerm Parameter 1.
     * @param lspName    Parameter 2.
     * @return A new instance of fragment PreviousLauncherLaunchesFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static UpcomingLauncherLaunchesFragment newInstance(String searchTerm, String lspName, Integer launcherId) {
        UpcomingLauncherLaunchesFragment fragment = new UpcomingLauncherLaunchesFragment();
        Bundle args = new Bundle();
        args.putString(SEARCH_TERM, searchTerm);
        args.putString(LSP_NAME, lspName);
        args.putInt(LAUNCHER_ID, launcherId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            searchTerm = getArguments().getString(SEARCH_TERM);
            lspName = getArguments().getString(LSP_NAME);
            launcherId = getArguments().getInt(LAUNCHER_ID);
        }
        context = getActivity();
        upcomingDataRepository = new UpcomingDataRepository(context, getRealm());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_launch_list, container, false);
        ButterKnife.bind(this, view);

        adapter = new ListAdapter(context);
        linearLayoutManager = new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false);
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.addItemDecoration(new SimpleDividerItemDecoration(context));
        recyclerView.setAdapter(adapter);
        statefulView.showProgress();
        scrollListener = new EndlessRecyclerViewScrollListener(linearLayoutManager) {
            @Override
            public void onLoadMore(int page, int totalItemsCount, RecyclerView view) {
                // Triggered only when new data needs to be appended to the list
                // Add whatever code is needed to append new items to the bottom of the list
                if (canLoadMore) {
                    fetchData(false);
                    mListener.showUpcomingLoading(true);
                }
            }
        };
        recyclerView.addOnScrollListener(scrollListener);
        fetchData(true);
        // Inflate the layout for this fragment
        return view;
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof UpcomingLauncherLaunchesFragment.OnFragmentInteractionListener) {
            mListener = (UpcomingLauncherLaunchesFragment.OnFragmentInteractionListener) context;
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

    public void fetchData(boolean forceRefresh) {
        Timber.v("Sending GET_UP_LAUNCHES");
        if (forceRefresh) {
            nextOffset = 0;
            adapter.clear();
        }
        upcomingDataRepository.getUpcomingLaunches(nextOffset, searchTerm, lspName, launcherId, new Callbacks.ListCallback() {
            @Override
            public void onLaunchesLoaded(List<Launch> launches, int next) {
                Timber.v("Offset - %s", next);
                nextOffset = next;
                canLoadMore = next > 0;
                updateAdapter(launches);
            }

            @Override
            public void onNetworkStateChanged(boolean refreshing) {
                mListener.showUpcomingLoading(refreshing);
            }

            @Override
            public void onError(String message, @Nullable Throwable throwable) {
                if (throwable != null) {
                    Timber.e(throwable);
                } else {
                    Timber.e(message);
                }
            }
        });
    }

    private void updateAdapter(List<Launch> launches) {

        if (launches.size() > 0) {
            if (!statefulStateContentShow) {
                statefulView.showContent();
                statefulStateContentShow = true;
            }
            adapter.addItems(launches);
            adapter.notifyDataSetChanged();

        } else {
            statefulView.showEmpty();
            statefulStateContentShow = false;
            if (adapter != null) {
                adapter.clear();
            }
        }
        scrollListener.resetState();
    }

    public void onRefresh(String lspName, String searchTerm) {
        this.searchTerm = searchTerm;
        this.lspName = lspName;
        fetchData(true);
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

        void showUpcomingLoading(boolean loading);

    }
}