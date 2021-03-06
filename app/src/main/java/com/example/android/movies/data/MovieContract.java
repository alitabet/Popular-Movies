package com.example.android.movies.data;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.net.Uri;

/**
 * Define table and column names for Popular Movies DB
 *
 * TODO: Merge all 3 tables into one and add foreign key constraints
 *
 * @author Ali K Thabet
 */
public class MovieContract {
    // projection of data to retrieve from DB
    public static final String[] MOVIE_COLUMNS = {
            GeneralEntry._ID,
            GeneralEntry.COLUMN_TITLE,
            GeneralEntry.COLUMN_RELEASE_DATE,
            GeneralEntry.COLUMN_SYNOPSIS,
            GeneralEntry.COLUMN_RATING,
            GeneralEntry.COLUMN_POPULARITY,
            GeneralEntry.COLUMN_POSTER,
            GeneralEntry.COLUMN_THUMB
    };

    // corresponding indices of projection
    public static final int COL_MOVIE_ID = 0;
    public static final int COL_MOVIE_TITLE = 1;
    public static final int COL_MOVIE_RELEASE_DATE = 2;
    public static final int COL_MOVIE_SYNOPSIS = 3;
    public static final int COL_MOVIE_RATING = 4;
    public static final int COL_MOVIE_POPULARITY = 5;
    public static final int COL_MOVIE_POSTER = 6;
    public static final int COL_MOVIE_THUMB = 7;

    // The content authority for the Popular Movies App
    public static final String CONTENT_AUTHORITY = "com.example.android.movies.app";

    // The base of all URI's which apps will use to contact the content provider.
    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);

    // Path to append to base content.
    // content://com.example.android.movies.app/movie/
    public static final String PATH_MOVIE          = "movie";
    public static final String PATH_MOVIE_RATING   = "movie_rating";
    public static final String PATH_MOVIE_FAVORITE = "movie_favorite";

//    Inner class that defines the contents of the movie table
//    sorted by popularity
    public static final class MovieEntry extends GeneralEntry {

        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_MOVIE).build();

        public static final String CONTENT_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_MOVIE;
        public static final String CONTENT_ITEM_TYPE =
                ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_MOVIE;

        // Table name
        public static final String TABLE_NAME = "movie";

        public static Uri buildMovieUri(long id) {
            return ContentUris.withAppendedId(CONTENT_URI, id);
        }
    }

    //    Inner class that defines the contents of the movie table
    //    sorted by user rating
    public static final class MovieRatingEntry extends GeneralEntry {

        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_MOVIE_RATING).build();

        public static final String CONTENT_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_MOVIE_RATING;
        public static final String CONTENT_ITEM_TYPE =
                ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_MOVIE_RATING;

        // Table name
        public static final String TABLE_NAME = "movie_rating";

        public static Uri buildMovieUri(long id) {
            return ContentUris.withAppendedId(CONTENT_URI, id);
        }
    }

    //    Inner class that defines the contents of the movie table
    //    of user's favorite movies
    public static final class MovieFavoriteEntry extends GeneralEntry {

        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_MOVIE_FAVORITE).build();

        public static final String CONTENT_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_MOVIE_FAVORITE;
        public static final String CONTENT_ITEM_TYPE =
                ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_MOVIE_FAVORITE;

        // Table name
        public static final String TABLE_NAME = "movie_favorite";

        public static Uri buildMovieUri(long id) {
            return ContentUris.withAppendedId(CONTENT_URI, id);
        }
    }
}
