package com.david.simpletweets.fragments;

import android.os.Bundle;

import com.david.simpletweets.models.Tweet;
import com.david.simpletweets.models.Tweet_Table;
import com.david.simpletweets.models.User;
import com.raizlabs.android.dbflow.sql.language.SQLite;

/**
 * Created by David on 3/30/2017.
 */

public class HomeTimelineFragment extends TweetsListFragment {

    public static final String NAME = "home";

    public static HomeTimelineFragment newInstance(User currentUser) {
        HomeTimelineFragment frag = new HomeTimelineFragment();
        Bundle args = new Bundle();
        args.putParcelable("currentUser", currentUser);
        frag.setArguments(args);
        return frag;
    }

    //send api request to get timeline json
    //fill listview by creating the tweet objects from json
    protected void populateTimeline(final long oldestId, final boolean refreshing) {
        super.populateTimeline(oldestId, refreshing);

        if (client.isNetworkAvailable()) {
            aTweets.showFooterProgressBar();
            client.getHomeTimeline(oldestId, tweetsHandler);
        } else {
            showNetworkUnavailableMessage();
            //load from db if network not available
            tweets.addAll(SQLite.select()
                    .from(Tweet.class)
                    .orderBy(Tweet_Table.uid, false)
                    .queryList());
            aTweets.notifyItemRangeInserted(0, tweets.size());
        }
    }

    @Override
    public String getFragmentName() {
        return NAME;
    }
}
