package com.david.simpletweets.fragments;

import android.os.Bundle;

import com.david.simpletweets.models.Message;
import com.david.simpletweets.models.Message_Table;
import com.david.simpletweets.models.User;
import com.raizlabs.android.dbflow.sql.language.SQLite;

/**
 * Created by David on 4/2/2017.
 */

public class MessagesSentFragment extends MessagesListFragment {

    public static MessagesSentFragment newInstance(User currentUser) {
        MessagesSentFragment frag = new MessagesSentFragment();
        Bundle args = new Bundle();
        args.putParcelable("currentUser", currentUser);
        frag.setArguments(args);
        return frag;
    }

    protected void populate(final long oldestId, final boolean refreshing) {
        super.populate(oldestId, refreshing);

        if (client.isNetworkAvailable()) {
            aMessages.showFooterProgressBar();
            client.getMessagesSent(oldestId, populateHandler);
        } else {
            showNetworkUnavailableMessage();
            //load from db if network not available
            messages.addAll(SQLite.select()
                    .from(Message.class)
                    .where(Message_Table.sender_uid.eq(currentUser.getUid()))
                    .orderBy(Message_Table.uid, false)
                    .queryList());
            aMessages.notifyItemRangeInserted(0, messages.size());
        }
    }
}
