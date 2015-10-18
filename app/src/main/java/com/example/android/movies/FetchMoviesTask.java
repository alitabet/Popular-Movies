package com.example.android.movies;

import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

/**
 * AsyncTask to run background thread and fetch data from
 * the MovieDB API. The task wil return an ArrayList of
 * <em>MovieItem</em> objects
 */
public class FetchMoviesTask extends AsyncTask<String, Void, ArrayList<MovieItem>> {

    // LOG_TAG used for debugging
    private final String LOG_TAG = FetchMoviesTask.class.getSimpleName();

    private Context mContext;
    private ImageAdapter mImageAdapter;

    public FetchMoviesTask(Context context, ImageAdapter imageAdapter) {
        mContext = context;
        mImageAdapter = imageAdapter;
    }

    // this function parses a JSON string containing movie
    // information into an ArrayList of <em>MovieItem</em>
    // objects
    private ArrayList<MovieItem> getMoviesFromJsonStr(String movieJsonStr)
            throws JSONException {

        // These are the names of the JSON objects that need to be extracted.
        final String MDB_TOTAL_PAGES = "total_pages";
        final String MDB_LIST = "results";
        final String MDB_ID = "id";
        final String MDB_TITLE = "original_title";
        final String MDB_THUMB = "backdrop_path";
        final String MDB_SYNP = "overview";
        final String MDB_REL_DATE = "release_date";
        final String MDB_POSTER = "poster_path";
        final String MDB_RATING = "vote_average";

        JSONObject movieJson = new JSONObject(movieJsonStr);

        JSONArray movieArray = movieJson.getJSONArray(MDB_LIST);

        // The JSON object returns a list of the most popular movies
        // and for each movie provides some useful information. We are
        // interested in id, tile, poster and thumbnail, synopsis,
        // release date, and popularity rating. The poster and thumbnail
        // are in relative format so we need to add
        // "http://image.tmdb.org/t/p/w185" to get the absolute URL
        // We will store the results of the MDB
        // in a Lis of <em>MovieItem</em> objects

        ArrayList<MovieItem> movieList = new ArrayList<>();

        for (int i = 0; i < movieArray.length(); i++) {

            MovieItem temp = new MovieItem();

            JSONObject movie = movieArray.getJSONObject(i); // get the current movie data
            temp.setId(movie.getInt(MDB_ID)); // movie id
            temp.setTitle(movie.getString(MDB_TITLE)); // original title
            if (movie.getString(MDB_POSTER) == "null") {
                temp.setPosterPath(null);
            } else {
                temp.setPosterPath(mContext.getString(R.string.poster_url)
                        + movie.getString(MDB_POSTER)); // URL to poster
            }
            if (movie.getString(MDB_THUMB) == "null") {
                temp.setThumbPath(null);
            } else {
                temp.setThumbPath(mContext.getString(R.string.thumb_url)
                        + movie.getString(MDB_THUMB)); // URL to thumbnail
            }
            temp.setReleaseDate(movie.getString(MDB_REL_DATE)); // release date
            temp.setSynopsis(movie.getString(MDB_SYNP)); // synopsis
            temp.setRating(movie.getDouble(MDB_RATING)); // get user rating

            movieList.add(temp); // add the extracted movie to the return list
        }

        return movieList;
    }

    @Override
    protected ArrayList<MovieItem> doInBackground(String... params) {

        String pageRequested = "1";
        String sortBy = mContext.getString(R.string.pref_sort_popular_api);
        // Check the input
        if (params.length > 1) sortBy = params[1];
        if (params.length > 0) pageRequested = params[0];

        // if requested page is out of bound then return
        if (Integer.parseInt(pageRequested) > PopularMoviesFragment.MAX_PAGES) return new ArrayList<>();

        // These two need to be declared outside the try/catch
        // so that they can be closed in the finally block.
        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;

        // Will contain the raw JSON response as a string.
        String moviesJsonStr = null;

        String apiKey = mContext.getString(R.string.api_key);
        try {
            // Construct the URL for the MovieDB API query
            // using the API Key and sorting parameters
            final String FORECAST_BASE_URL = mContext.getString(R.string.moviedb_url);
            final String SORT_PARAM = mContext.getString(R.string.moviedb_sort_param);
            final String PAGE_PARAM = mContext.getString(R.string.moviedb_page_param);
            final String KEY_PARAM = mContext.getString(R.string.moviedb_api_key_param);

            Uri builtUri = Uri.parse(FORECAST_BASE_URL).buildUpon()
                    .appendQueryParameter(SORT_PARAM, sortBy)
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
                return null;
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
                return null;
            }
            moviesJsonStr = buffer.toString();
        } catch (IOException e) {
            Log.e(LOG_TAG, "Error ", e);
            // if the code couldn't get the movie data then no need
            // to parse it
            return null;
        } finally {
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
        try {
            return getMoviesFromJsonStr(moviesJsonStr);
        } catch (JSONException e) {
            Log.e(LOG_TAG, e.getMessage(), e);
            e.printStackTrace();
        }

        // This will only happen if there was an error getting or parsing the movie data.
        return null;
    }

    @Override
    protected void onPostExecute(ArrayList<MovieItem> results) {
        // if the task managed to read data from
        // the MovieDB API, then store it in the
        // custom adapter
        if (results != null) {
            mImageAdapter.addAll(results);
        }
    }
}