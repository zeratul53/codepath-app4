package com.david.simpletweets.adapters;

import android.content.Context;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.david.simpletweets.R;
import com.david.simpletweets.TwitterApplication;
import com.david.simpletweets.activities.ProfileActivity;
import com.david.simpletweets.activities.SearchActivity;
import com.david.simpletweets.activities.TimelineActivity;
import com.david.simpletweets.activities.TweetDetailsActivity;
import com.david.simpletweets.databinding.FooterProgressBinding;
import com.david.simpletweets.databinding.ItemTweetBinding;
import com.david.simpletweets.databinding.ItemTweetImageBinding;
import com.david.simpletweets.databinding.ItemTweetVideoBinding;
import com.david.simpletweets.fragments.ComposeTweetFragment;
import com.david.simpletweets.models.Tweet;
import com.david.simpletweets.models.User;
import com.david.simpletweets.utils.PatternEditableBuilder;
import com.loopj.android.http.JsonHttpResponseHandler;

import org.json.JSONObject;

import java.util.List;
import java.util.regex.Pattern;

import cz.msebera.android.httpclient.Header;

/**
 * Created by David on 3/23/2017.
 */
// taking Tweet objects and turning them into Views displayed in the list
public class TweetsArrayAdapter extends FooterArrayAdapter<RecyclerView.ViewHolder> {

    public abstract class ViewHolderBase extends RecyclerView.ViewHolder implements View.OnClickListener {
        public ImageView ivProfileImage;
        public TextView tvUserName;
        public TextView tvBody;
        public TextView tvName;
        public TextView tvDate;
        public ImageButton ibReply;
        public ImageButton ibRetweet;
        public ImageButton ibFavorite;
        public TextView tvRetweetCount;
        public TextView tvFavsCount;

        //profile image click listener
        protected View.OnClickListener onProfileClick = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int position = getAdapterPosition(); // gets item position
                if (position != RecyclerView.NO_POSITION) {
                    Tweet tweet = tweets.get(position);
                    //show profile of tweet's user
                    showProfile(tweet.getUser());
                }
            }
        };

        //reply listener
        protected View.OnClickListener onReply = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int position = getAdapterPosition();
                Tweet tweet = tweets.get(position);
                FragmentManager fm = ((AppCompatActivity) context).getSupportFragmentManager();
                ComposeTweetFragment frag = ComposeTweetFragment.newInstance(currentUser, tweet);
                frag.show(fm, "fragment_reply");
            }
        };

        //retweet listener
        protected View.OnClickListener onRetweet = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int position = getAdapterPosition();
                final Tweet tweet = tweets.get(position);
                if (tweet.isRetweeted()) {
                    //update UI immediately for response UX
                    undoRetweet(tweet);
                    TwitterApplication.getRestClient().postUnretweet(tweet.getUid(), new JsonHttpResponseHandler() {
                        @Override
                        public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                            //we don't update the tweet from response here because we don't want a weird UX
                        }

                        @Override
                        public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                            Log.d("DEBUG", "undoRetweet failed: " + errorResponse.toString());
                            //revert
                            doRetweet(tweet);
                        }
                    });
                } else {
                    //update UI immediately for response UX
                    doRetweet(tweet);
                    TwitterApplication.getRestClient().postRetweet(tweet.getUid(), new JsonHttpResponseHandler() {
                        @Override
                        public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                            //we don't update the tweet from response here because we don't want a weird UX
                        }

                        @Override
                        public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                            Log.d("DEBUG", "retweet failed: " + errorResponse.toString());
                            //revert
                            undoRetweet(tweet);
                        }
                    });
                }
            }
        };

        //favorite listener
        protected View.OnClickListener onFavorite = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int position = getAdapterPosition();
                final Tweet tweet = tweets.get(position);
                if (tweet.isFavorited()) {
                    //update UI immediately for response UX
                    undoFavorite(tweet);
                    TwitterApplication.getRestClient().postUnfavorite(tweet.getUid(), new JsonHttpResponseHandler() {
                        @Override
                        public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                            //we don't update the tweet from response here because we don't want a weird UX
                        }

                        @Override
                        public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                            Log.d("DEBUG", "undoFavorite failed: " + errorResponse.toString());
                            //revert
                            doFavorite(tweet);
                        }
                    });
                } else {
                    //update UI immediately for response UX
                    doFavorite(tweet);
                    TwitterApplication.getRestClient().postFavorite(tweet.getUid(), new JsonHttpResponseHandler() {
                        @Override
                        public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                            //we don't update the tweet from response here because we don't want a weird UX
                        }

                        @Override
                        public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                            Log.d("DEBUG", "favorite failed: " + errorResponse.toString());
                            //revert
                            undoFavorite(tweet);
                        }
                    });
                }
            }
        };

        protected PatternEditableBuilder patternBuilder = new PatternEditableBuilder().
                addPattern(Pattern.compile("\\@(\\w+)"), ContextCompat.getColor(context, R.color.controlActivated),
                        new PatternEditableBuilder.SpannableClickedListener() {
                            @Override
                            public void onSpanClicked(String text) {
                                TwitterApplication.getRestClient().getUser(text, new JsonHttpResponseHandler() {
                                    @Override
                                    public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                                        User user = User.fromJSON(response);
                                        showProfile(user);
                                    }

                                    @Override
                                    public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                                        Log.d("DEBUG", "Getting user failed: " + errorResponse.toString());
                                    }
                                });
                            }
                        })
                .addPattern(Pattern.compile("\\#(\\w+)"), ContextCompat.getColor(context, R.color.controlActivated),
                        new PatternEditableBuilder.SpannableClickedListener() {
                            @Override
                            public void onSpanClicked(String text) {
                                //search for hashtag
                                Intent i = new Intent(context, SearchActivity.class);
                                i.putExtra("currentUser", currentUser);
                                i.putExtra("query", text);
                                context.startActivity(i);
                            }
                        });

        public ViewHolderBase(View itemView) {
            super(itemView);

            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            int position = getAdapterPosition(); // gets item position
            if (position != RecyclerView.NO_POSITION) { // Check if an item was deleted, but the user clicked it before the UI removed it
                Tweet tweet = tweets.get(position);
                Intent i = new Intent(context, TweetDetailsActivity.class);
                i.putExtra("tweet", tweet);
                i.putExtra("pos", position);
                i.putExtra("currentUser", currentUser);
                i.putExtra("fragmentName", fragmentName);
                ((AppCompatActivity) context).startActivityForResult(i, TimelineActivity.REQUEST_CODE_DETAILS);
            }
        }

        public void bindTweet(Tweet tweet) {
            //update button images
            if (tweet.isRetweeted()) {
                ibRetweet.setImageResource(R.drawable.ic_repeat_on);
            } else {
                ibRetweet.setImageResource(R.drawable.ic_repeat);
            }
            if (tweet.isFavorited()) {
                ibFavorite.setImageResource(R.drawable.ic_star_on);
            } else {
                ibFavorite.setImageResource(R.drawable.ic_star);
            }

            //add clickable spans
            patternBuilder.into(tvBody);
        }

        protected void showProfile(User user) {
            Intent i = new Intent(context, ProfileActivity.class);
            i.putExtra("currentUser", currentUser);
            i.putExtra("user", user);
            context.startActivity(i);
        }

        protected void doRetweet(Tweet tweet) {
            tweet.setRetweetCount(tweet.getRetweetCount() + 1);
            tweet.setRetweeted(true);
            tweet.save();
            ibRetweet.setImageResource(R.drawable.ic_repeat_on);
            tvRetweetCount.setText(String.valueOf(tweet.getRetweetCount()));
        }

        protected void undoRetweet(Tweet tweet) {
            tweet.setRetweetCount(tweet.getRetweetCount() - 1);
            tweet.setRetweeted(false);
            tweet.save();
            ibRetweet.setImageResource(R.drawable.ic_repeat);
            tvRetweetCount.setText(String.valueOf(tweet.getRetweetCount()));
        }

        protected void doFavorite(Tweet tweet) {
            tweet.setFavoritesCount(tweet.getFavoritesCount() + 1);
            tweet.setFavorited(true);
            tweet.save();
            ibFavorite.setImageResource(R.drawable.ic_star_on);
            tvFavsCount.setText(String.valueOf(tweet.getFavoritesCount()));
        }

        protected void undoFavorite(Tweet tweet) {
            tweet.setFavoritesCount(tweet.getFavoritesCount() - 1);
            tweet.setFavorited(false);
            tweet.save();
            ibFavorite.setImageResource(R.drawable.ic_star);
            tvFavsCount.setText(String.valueOf(tweet.getFavoritesCount()));
        }
    }

    public class ViewHolder extends ViewHolderBase {
        private ItemTweetBinding binding;

        public ViewHolder(ItemTweetBinding itemView) {
            super(itemView.getRoot());

            this.binding = itemView;

            //set bindings
            ivProfileImage = binding.ivProfileImage;
            tvName = binding.tvName;
            tvUserName = binding.tvUserName;
            tvBody = binding.tvBody;
            tvDate = binding.tvDate;
            ibReply = binding.incTweetActions.ibReply;
            ibRetweet = binding.incTweetActions.ibRetweet;
            ibFavorite = binding.incTweetActions.ibFavorite;
            tvRetweetCount = binding.incTweetActions.tvRetweetCount;
            tvFavsCount = binding.incTweetActions.tvFavsCount;

            //set click listeners
            ivProfileImage.setOnClickListener(onProfileClick);
            ibReply.setOnClickListener(onReply);
            ibRetweet.setOnClickListener(onRetweet);
            ibFavorite.setOnClickListener(onFavorite);
        }

        public void bindTweet(Tweet tweet) {
            binding.setTweet(tweet);
            binding.incTweetActions.setTweet(tweet);
            binding.executePendingBindings();

            super.bindTweet(tweet);
        }
    }

    public class ImageViewHolder extends ViewHolderBase {
        ItemTweetImageBinding binding;
        ImageView ivEmbedImage;

        public ImageViewHolder(ItemTweetImageBinding itemView) {
            super(itemView.getRoot());
            this.binding = itemView;

            ivProfileImage = binding.ivProfileImage;
            tvName = binding.tvName;
            tvUserName = binding.tvUserName;
            tvBody = binding.tvBody;
            tvDate = binding.tvDate;
            ivEmbedImage = binding.ivEmbedImage;
            ibReply = binding.incTweetActions.ibReply;
            ibRetweet = binding.incTweetActions.ibRetweet;
            ibFavorite = binding.incTweetActions.ibFavorite;
            tvRetweetCount = binding.incTweetActions.tvRetweetCount;
            tvFavsCount = binding.incTweetActions.tvFavsCount;

            //set click listeners
            ivProfileImage.setOnClickListener(onProfileClick);
            ibReply.setOnClickListener(onReply);
            ibRetweet.setOnClickListener(onRetweet);
            ibFavorite.setOnClickListener(onFavorite);
        }

        public void bindTweet(Tweet tweet) {
            binding.setTweet(tweet);
            binding.incTweetActions.setTweet(tweet);
            binding.executePendingBindings();

            ivEmbedImage.setImageResource(0);
            Glide.with(getContext()).load(tweet.getMediaUrl())
                    .into(ivEmbedImage);

            super.bindTweet(tweet);
        }
    }

    public class VideoViewHolder extends ViewHolderBase {
        ItemTweetVideoBinding binding;
        ImageView ivEmbedImage;

        public VideoViewHolder(ItemTweetVideoBinding itemView) {
            super(itemView.getRoot());
            this.binding = itemView;

            ivProfileImage = binding.ivProfileImage;
            tvName = binding.tvName;
            tvUserName = binding.tvUserName;
            tvBody = binding.tvBody;
            tvDate = binding.tvDate;
            ivEmbedImage = binding.ivEmbedImage;
            ibReply = binding.incTweetActions.ibReply;
            ibRetweet = binding.incTweetActions.ibRetweet;
            ibFavorite = binding.incTweetActions.ibFavorite;
            tvRetweetCount = binding.incTweetActions.tvRetweetCount;
            tvFavsCount = binding.incTweetActions.tvFavsCount;

            //set click listeners
            ivProfileImage.setOnClickListener(onProfileClick);
            ibReply.setOnClickListener(onReply);
            ibRetweet.setOnClickListener(onRetweet);
            ibFavorite.setOnClickListener(onFavorite);
        }

        public void bindTweet(Tweet tweet) {
            binding.setTweet(tweet);
            binding.incTweetActions.setTweet(tweet);
            binding.executePendingBindings();

            ivEmbedImage.setImageResource(0);
            Glide.with(getContext()).load(tweet.getMediaUrl())
                    .into(ivEmbedImage);

            super.bindTweet(tweet);
        }
    }

    private final int GENERIC = 0, IMAGE = 1, VIDEO = 2;

    // Store a member variable for the tweet
    private List<Tweet> tweets;
    // Store the context for easy access
    private Context context;
    private String fragmentName;
    private User currentUser;

    public TweetsArrayAdapter(@NonNull Context context, @NonNull List<Tweet> tweets, @NonNull User currentUser, String fragmentName) {
        this.context = context;
        this.tweets = tweets;
        this.currentUser = currentUser;
        this.fragmentName = fragmentName;
    }

    // Easy access to the context object in the recyclerview
    private Context getContext() {
        return this.context;
    }

    // Usually involves inflating a layout from XML and returning the holder
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);
        RecyclerView.ViewHolder viewHolder;

        switch (viewType) {
            case TYPE_FOOTER:
                FooterProgressBinding footerBinding = DataBindingUtil.inflate(inflater, R.layout.footer_progress, parent, false);
                viewHolder = new FooterViewHolder(footerBinding);
                break;
            case VIDEO:
                ItemTweetVideoBinding bindingVideo = DataBindingUtil.inflate(inflater, R.layout.item_tweet_video, parent, false);
                viewHolder = new VideoViewHolder(bindingVideo);
                break;
            case IMAGE:
                ItemTweetImageBinding bindingImage = DataBindingUtil.inflate(inflater, R.layout.item_tweet_image, parent, false);
                viewHolder = new ImageViewHolder(bindingImage);
                break;
            case GENERIC:
            default:
                ItemTweetBinding binding = DataBindingUtil.inflate(inflater, R.layout.item_tweet, parent, false);
                viewHolder = new ViewHolder(binding);
                break;
        }

        return viewHolder;
    }

    // Involves populating data into the item through holder
    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int position) {
        // Get the data model based on position
        if (isPositionFooter(position)) {
            footerViewHolder = (FooterViewHolder) viewHolder;
        } else {
            Tweet tweet = this.tweets.get(position);
            configureViewHolder((ViewHolderBase) viewHolder, tweet);
        }
    }

    private void configureViewHolder(TweetsArrayAdapter.ViewHolderBase viewHolder, Tweet tweet) {
        // populate data into subviews
        viewHolder.bindTweet(tweet);

        viewHolder.ivProfileImage.setImageResource(0); //clear out old image for recycled view
        Glide.with(getContext()).load(tweet.getUser().getProfileImageUrl())
                .into(viewHolder.ivProfileImage);
    }

    // Returns the total count of items in the list
    @Override
    public int getItemCount() {
//        Log.d("DEBUG", "item count returning: " + this.tweets.size() + super.getItemCount());
        return this.tweets.size() + super.getItemCount(); //+1 for footer
    }

    @Override
    public int getItemViewType(int position) {
        if (isPositionFooter(position)) {
            return TYPE_FOOTER;
        }
        Tweet tweet = this.tweets.get(position);
        if (!TextUtils.isEmpty(tweet.getMediaType())) {
            if (tweet.getMediaType().equals("photo")) {
                return IMAGE;
            } else if (tweet.getMediaType().equals("video")) {
                return VIDEO;
            }
        }
        return GENERIC;
    }

    protected boolean isPositionFooter (int position) {
        return position == this.tweets.size();
    }

}
