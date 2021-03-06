package com.example.android.movies.sync;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.SyncRequest;
import android.content.SyncResult;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import com.example.android.movies.BuildConfig;
import com.example.android.movies.PopularMoviesFragment;
import com.example.android.movies.R;
import com.example.android.movies.Utility;
import com.example.android.movies.api.MovieDBApi;
import com.example.android.movies.api.results.MovieResults;
import com.example.android.movies.models.MovieItem;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import retrofit.Call;
import retrofit.Callback;
import retrofit.GsonConverterFactory;
import retrofit.Response;
import retrofit.Retrofit;

/**
 * This class implements a {@link AbstractThreadedSyncAdapter}
 * It syncs all the movie data in a regural interval of 1 day.
 *
 * @author Ali K Thabet
 */
public class MoviesSyncAdapter extends AbstractThreadedSyncAdapter {
    public static final String PAGE_QUERY_EXTRA = "pqe";
    // Interval at which to sync with the weather, in seconds.
    // 60 seconds/minute * 60 minutes/hour * 24 hours/day = 1 day (24 hours)
    public static final int SYNC_INTERVAL = 60 * 60 * 24;
    public static final int SYNC_FLEXTIME = SYNC_INTERVAL/3;
    public final String LOG_TAG = MoviesSyncAdapter.class.getSimpleName();

    public MoviesSyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
    }

    private void getMoviesFromJsonStr(String movieJsonStr, String sortType)
            throws JSONException {

        JSONObject movieJson = new JSONObject(movieJsonStr);

        JSONArray movieArray = movieJson.getJSONArray(MovieItem.MDB_LIST);

        // Insert the movie data into the database
        Vector<ContentValues> cVVector = new Vector<>(movieArray.length());

        // The JSON object returns a list of the most popular movies
        // and for each movie provides some useful information. We are
        // interested in id, tile, poster and thumbnail, synopsis,
        // release date, and popularity rating. The poster and thumbnail
        // are in relative format so we need to add
        // "http://image.tmdb.org/t/p/w185" to get the absolute URL
        // We will store the results of the MDB
        // in a Lis of <em>MovieItem</em> objects

        for (int i = 0; i < movieArray.length(); i++) {

            MovieItem temp = new MovieItem();

            JSONObject movie = movieArray.getJSONObject(i); // get the current movie data
            temp.setId(movie.getInt(MovieItem.MDB_ID)); // movie id
            temp.setTitle(movie.getString(MovieItem.MDB_TITLE)); // original title

            // only retrieve movies with a poster
            if (movie.getString(MovieItem.MDB_POSTER).equals("null")) {
                continue;
            }
            temp.setPosterPath(getContext().getString(R.string.poster_url)
                    + movie.getString(MovieItem.MDB_POSTER)); // URL to poster

            temp.setReleaseDate(movie.getString(MovieItem.MDB_REL_DATE)); // release date
            temp.setSynopsis(movie.getString(MovieItem.MDB_SYNP)); // synopsis
            temp.setRating(movie.getDouble(MovieItem.MDB_RATING)); // get user rating
            temp.setPopularity(movie.getDouble(MovieItem.MDB_POPULARITY)); // get the movie popularity

            cVVector.add(temp.getContentValues()); // Add the ContentValue of retrieved movie
        }

        // add to database
        if ( cVVector.size() > 0 ) {
            ContentValues[] cvArray = new ContentValues[cVVector.size()];
            cVVector.toArray(cvArray);
            getContext().getContentResolver().bulkInsert(Utility.getUriFromAPISort(getContext(), sortType), cvArray);
        }
    }

    private boolean getData(String sortBy, String apiKey, String pageRequested) {

        // These two need to be declared outside the try/catch
        // so that they can be closed in the finally block.
        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;

        // Will contain the raw JSON response as a string.
        String moviesJsonStr = null;

        try {
            // Construct the URL for the MovieDB API query
            // using the API Key and sorting parameters
            final String MOVIE_BASE_URL = getContext().getString(R.string.movie_db_base_url);
            final String SORT_PARAM = getContext().getString(R.string.moviedb_sort_param);
            final String PAGE_PARAM = getContext().getString(R.string.moviedb_page_param);
            final String KEY_PARAM = getContext().getString(R.string.moviedb_api_key_param);

            Uri builtUri = Uri.parse(MOVIE_BASE_URL)
                    .buildUpon().appendPath(sortBy)
//                    .appendQueryParameter(SORT_PARAM, sortBy)
                    .appendQueryParameter(KEY_PARAM, apiKey)
                    .appendQueryParameter(PAGE_PARAM, pageRequested)
                    .build();

            URL url = new URL(builtUri.toString());

            // Create the request to MovieDB API, and open the connection
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.connect();

            // Read the input stream into a String
            InputStream inputStream = urlConnection.getInputStream();
            StringBuffer buffer = new StringBuffer();
            if (inputStream == null) {
                // Nothing to do.
                return false;
            }
            reader = new BufferedReader(new InputStreamReader(inputStream));

            String line;
            while ((line = reader.readLine()) != null) {
                // Adding new line not completely necessary for JSON
                // but helps with debugging.
                buffer.append(line + "\n");
            }

            if (buffer.length() == 0) {
                // Stream was empty.  No point in parsing.
                return false;
            }
            moviesJsonStr = buffer.toString();
            Log.d(LOG_TAG, "Syncing for sort " + sortBy);
            getMoviesFromJsonStr(moviesJsonStr, sortBy);
        } catch (IOException e) {
            Log.e(LOG_TAG, "Error ", e);
            // if the code couldn't get the movie data then no need
            // to parse it
            return false;
        } catch (JSONException e) {
            Log.e(LOG_TAG, e.getMessage(), e);
            e.printStackTrace();
        }finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (final IOException e) {
                    Log.e(LOG_TAG, "Error closing stream", e);
                }
            }
        }

        return true;
    }

    @Override
    public void onPerformSync(Account account, Bundle extras, String authority, ContentProviderClient provider, SyncResult syncResult) {
        String pagesRequested = String.valueOf(PopularMoviesFragment.MAX_PAGES);
        String apiKey = BuildConfig.MOVIE_DB_API_KEY;

        String[] sortByArray = {getContext().getString(R.string.pref_sort_popular_api),
                getContext().getString(R.string.pref_sort_rated_api)};

        // Sync for both popular movies and user rating
        // for all pages specified in PopularMoviesFragment.MAX_PAGES
        for (String sortBy : sortByArray) {
            // fetch all the movies from pages 1 to pagesRequested
            for (int i = 1; i <= Integer.parseInt(pagesRequested); i++) {
//                getMovies(sortBy, String.valueOf(i));
                getData(sortBy, apiKey, String.valueOf(i));
            }
        }

        // TODO: Update data for favorite movies
    }

    // Retrofit implementation (not used at the moment)
    private void getMovies(final String sortBy, String pageNumber) {
        Map<String, String> queryMap = new HashMap<>();
        queryMap.put(getContext().getString(R.string.moviedb_sort_param),
                sortBy);

        queryMap.put(getContext().getString(R.string.moviedb_api_key_param),
                BuildConfig.MOVIE_DB_API_KEY);

        queryMap.put(getContext().getString(R.string.moviedb_page_param),
                pageNumber);

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(MovieDBApi.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        MovieDBApi service = retrofit.create(MovieDBApi.class);
        Call<MovieResults> call = service.getMovieResults(sortBy, BuildConfig.MOVIE_DB_API_KEY);
        call.enqueue(new Callback<MovieResults>() {
            private final String SORT_BY = sortBy;
            @Override
            public void onResponse(Response<MovieResults> response, Retrofit retrofit) {
                MovieResults movieResults = response.body();
                if (movieResults == null) return;

                List<MovieItem> movieList = movieResults.results;
                Vector<ContentValues> cVVector = new Vector<>(movieList.size());

                for (MovieItem mv : movieList) {
                    cVVector.add(mv.getContentValues());
                }

                // add to database
                if ( cVVector.size() > 0 ) {
                    ContentValues[] cvArray = new ContentValues[cVVector.size()];
                    cVVector.toArray(cvArray);
                    getContext().getContentResolver().bulkInsert(Utility.getUriFromAPISort(getContext(), SORT_BY), cvArray);
                }
            }

            @Override
            public void onFailure(Throwable t) {
                Log.e(LOG_TAG, "Error loading review: " + t.getMessage());
            }
        });
    }

    /**
     * Helper method to have the sync adapter sync immediately
     * @param context The context used to access the account service
     */
    public static void syncImmediately(Context context) {
        Bundle bundle = new Bundle();
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
        ContentResolver.requestSync(getSyncAccount(context),
                context.getString(R.string.content_authority), bundle);
    }

    /**
     * Helper method to schedule the sync adapter periodic execution
     */
    public static void configurePeriodicSync(Context context, int syncInterval, int flexTime) {
        Account account = getSyncAccount(context);
        String authority = context.getString(R.string.content_authority);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            // we can enable inexact timers in our periodic sync
            SyncRequest request = new SyncRequest.Builder().
                    syncPeriodic(syncInterval, flexTime).
                    setSyncAdapter(account, authority).
                    setExtras(new Bundle()).build();
            ContentResolver.requestSync(request);
        } else {
            ContentResolver.addPeriodicSync(account,
                    authority, new Bundle(), syncInterval);
        }
    }

    /**
     * Helper method to get the fake account to be used with SyncAdapter, or make a new one
     * if the fake account doesn't exist yet.  If we make a new account, we call the
     * onAccountCreated method so we can initialize things.
     *
     * @param context The context used to access the account service
     * @return a fake account.
     */
    public static Account getSyncAccount(Context context) {
        // Get an instance of the Android account manager
        AccountManager accountManager =
                (AccountManager) context.getSystemService(Context.ACCOUNT_SERVICE);

        // Create the account type and default account
        Account newAccount = new Account(
                context.getString(R.string.app_name), context.getString(R.string.sync_account_type));

        // If the password doesn't exist, the account doesn't exist
        if ( null == accountManager.getPassword(newAccount) ) {

        /*
         * Add the account and account type, no password or user data
         * If successful, return the Account object, otherwise report an error.
         */
            if (!accountManager.addAccountExplicitly(newAccount, "", null)) {
                return null;
            }
            /*
             * If you don't set android:syncable="true"
             * in your <provider> element in the manifest,
             * then call ContentResolver.setIsSyncable(account, AUTHORITY, 1)
             * here.
             */
            OnAccountCreated(newAccount, context);
        }
        return newAccount;
    }

    private static void OnAccountCreated(Account account, Context context) {
        // configure periodic sync for new account
        MoviesSyncAdapter.configurePeriodicSync(context, SYNC_INTERVAL, SYNC_FLEXTIME);
        // enable periodic sync by calling helper method
        ContentResolver.setSyncAutomatically(account, context.getString(R.string.content_authority), true);
        // do an initial sync
        syncImmediately(context);
    }

    public static void initializeSyncAdapter(Context context) {
        getSyncAccount(context);
    }
}
