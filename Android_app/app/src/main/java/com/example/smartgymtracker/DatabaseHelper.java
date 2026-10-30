package com.example.smartgymtracker;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DatabaseHelper extends SQLiteOpenHelper {

    private String TAG = "DatabaseHelper";
    public DatabaseHelper(Context context) {
        super(context, Constant.DATABASE_NAME, null, Constant.DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase){

        //Creation of user table
        String createUserTable = "CREATE TABLE " + Constant.TABLE1_NAME + " ( "
                + Constant.COLUMN_USER_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + Constant.COLUMN_USER_EMAIL + " TEXT NOT NULL UNIQUE, "
                + Constant.COLUMN_USER_PASSWORD_HASH + " TEXT NOT NULL);";
        sqLiteDatabase.execSQL(createUserTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + Constant.TABLE1_NAME);
        onCreate(db);
    }

    //Insert user profile within the user table in the database
    public boolean insertUser(User user){
        SQLiteDatabase db = getWritableDatabase();
        ContentValues contentValues = new ContentValues();

        contentValues.put(Constant.COLUMN_USER_EMAIL, user.getUserEmail());
        contentValues.put(Constant.COLUMN_USER_PASSWORD_HASH, user.getUserPasswordHash());

        try{
            db.insertOrThrow(Constant.TABLE1_NAME, null, contentValues);
            return true;
        } catch (Exception ex) {
            Log.e(TAG, "insertUser() - exception: " + ex.getMessage());
            return false;
        } finally {
//            db.close();
        }
    }

    public boolean checkEmail(String email){
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = null;
        try{
            cursor = db.rawQuery("SELECT 1 FROM " + Constant.TABLE1_NAME + " WHERE " + Constant.COLUMN_USER_EMAIL + " = ?", new  String[]{email});
            return cursor.moveToFirst();
        } catch (SQLException e) {
            Log.e(TAG, "checkEmail() - exception: " + e.getMessage());
            throw new RuntimeException("Failed to check email", e);
        } finally {
            if(cursor != null){
                cursor.close();
            }
//            db.close();
        }
    }

    public String getPasswordHash(String email){
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.rawQuery("SELECT " + Constant.COLUMN_USER_PASSWORD_HASH + " FROM " + Constant.TABLE1_NAME + " WHERE " + Constant.COLUMN_USER_EMAIL + " = ?", new String[]{email});
            if (cursor.moveToFirst()) {
                return cursor.getString(0);
            }
            return null;
        } catch (SQLException e) {
            Log.e(TAG, "getPasswordHash() - exception: " + e.getMessage());
            return null;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

}
