package com.example.android.movies;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.android.movies.data.MovieContract;
import com.example.android.movies.models.MovieItem;
import com.squareup.picasso.Picasso;

import butterknife.Bind;
import butterknife.ButterKnife;

public class DetailActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new DetailFragment())
                    .commit();
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.detail, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * This fragment will display the details of the
     * selected movie.
     */
    public static class DetailFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
        private static final int DETAIL_LOADER = 1;
        ViewHolder viewHolder;
        private String uriString;

        public DetailFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {

            View rootView = inflater.inflate(R.layout.fragment_detail, container, false);
            viewHolder = new ViewHolder(rootView);

            return rootView;
        }

        @Override
        public void onActivityCreated(Bundle savedInstanceState) {
            getLoaderManager().initLoader(DETAIL_LOADER, null, this);
            super.onActivityCreated(savedInstanceState);
        }

        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
            Intent intent = getActivity().getIntent();
            if (intent == null) return null;

            return new CursorLoader(getActivity(),
                    intent.getData(),
                    MovieContract.MOVIE_COLUMNS,
                    null,
                    null,
                    null);
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
            if (!data.moveToFirst()) { return; }

            MovieItem movie = new MovieItem(data);

            // display title, release date, and user rating
            viewHolder.titleText
                    .setText(movie.getTitle());
            viewHolder.dateText
                    .setText(movie.getReleaseDate());
            // user rating rounded to 2 decimal places
            viewHolder.ratingText
                    .setText(String.format("%.2f", movie.getRating()));

            // fetch thumbnail using thumbnail URL
            String thumbPath = movie.getThumbPath();

            if (thumbPath == null) thumbPath = getString(R.string.thumb_url_alt);

            Picasso.with(getActivity()).load(thumbPath).into(viewHolder.thumbImage);

            // extract synopsis
            viewHolder.synopsisText
                    .setText(movie.getSynopsis());
        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {

        }
    }

    static class ViewHolder {
        @Bind(R.id.title_text)
        TextView titleText;

        @Bind(R.id.date_text)
        TextView dateText;

        @Bind(R.id.rating_text)
        TextView ratingText;

        @Bind(R.id.synopsis_text)
        TextView synopsisText;

        @Bind(R.id.thumb_imageview)
        ImageView thumbImage;

        public ViewHolder(View view) {
            ButterKnife.bind(this, view);
        }
    }
}