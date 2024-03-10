package com.secutity.securenotes;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import net.sqlcipher.Cursor;
import net.sqlcipher.database.SQLiteDatabase;
import net.sqlcipher.database.SQLiteOpenHelper;
import android.content.ContentValues;
import android.content.Context;
import net.sqlcipher.Cursor;
import net.sqlcipher.database.SQLiteDatabase;
import net.sqlcipher.database.SQLiteOpenHelper;
public class SQliteHelper extends SQLiteOpenHelper {
    public static final String dbName = "notepaddb";
    public static final int version = 9;

    public SQliteHelper(Context context) {
        super(context, dbName, null, version);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String userTableQuery = "CREATE TABLE IF NOT EXISTS user(_id INTEGER PRIMARY KEY AUTOINCREMENT, username TEXT, password TEXT, created_at DATETIME DEFAULT CURRENT_TIMESTAMP)";
        db.execSQL(userTableQuery);

        String notesTableQuery = "CREATE TABLE IF NOT EXISTS notes(_id INTEGER PRIMARY KEY AUTOINCREMENT, username TEXT, title TEXT, content TEXT, created_at DATETIME DEFAULT CURRENT_TIMESTAMP)";
        db.execSQL(notesTableQuery);
    }



    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (newVersion > oldVersion) {
            String notesTableQuery = "create table if not exists notes(_id integer primary key autoincrement, title text, content text, created_at DATETIME DEFAULT CURRENT_TIMESTAMP)";
            db.execSQL(notesTableQuery);
        }
    }



    public boolean checkUserLogin(String username, String enteredPassword, SQLiteDatabase db) {
        Cursor cursor = db.rawQuery("SELECT password FROM user WHERE username = ?", new String[]{username});

        if (cursor != null && cursor.getCount() > 0) {
            cursor.moveToFirst();
            String storedPassword = cursor.getString(cursor.getColumnIndex("password"));
            cursor.close();
            return AES.decrypt(storedPassword).equals(enteredPassword);
        }

        return false;
    }


    public long insertUser(String username, String password, SQLiteDatabase db) {
        ContentValues values = new ContentValues();
        values.put("username", username);
        values.put("password", AES.encrypt(password));
        return db.insert("user", null, values);
    }

    public boolean checkUserExists(String username, SQLiteDatabase db) {
        Cursor cursor = db.rawQuery("SELECT * FROM user WHERE username = ?", new String[]{username});
        boolean exists = cursor != null && cursor.getCount() > 0;
        if (cursor != null) {
            cursor.close();
        }
        return exists;
    }

    public long insertUserWithoutPassword(String username, SQLiteDatabase db) {
        ContentValues values = new ContentValues();
        values.put("username", username);
        return db.insert("user", null,values);
    }



    public long insertNote(String username, ContentValues values, SQLiteDatabase db) {
        values.put("username", username);
        return db.insert("notes", null, values);
    }



    public Cursor getAllNotes(SQLiteDatabase db) {
        return db.rawQuery("select * from notes", null);
    }

    public Cursor getAllNotesByUsername(String username, SQLiteDatabase db) {
        return db.rawQuery("select * from notes where username = ?", new String[]{username});
    }


    @SuppressLint("Range")
    public String retrieveStoredPassword(String username) {
        SQLiteDatabase db = this.getReadableDatabase("password");
        Cursor cursor = db.rawQuery("SELECT password FROM user WHERE username = ?", new String[]{username});

        String storedPassword = "";

        if (cursor != null && cursor.getCount() > 0) {
            cursor.moveToFirst();
            storedPassword = cursor.getString(cursor.getColumnIndex("password"));
            cursor.close();
        }

        return storedPassword;
    }


    public boolean updatePassword(String username, String newPassword) {
        SQLiteDatabase db = this.getWritableDatabase("password");

        ContentValues values = new ContentValues();
        values.put("password", AES.encrypt(newPassword));

        String whereClause = "username=?";
        String[] whereArgs = {username};

        int rowsAffected = db.update("user", values, whereClause, whereArgs);

        return rowsAffected > 0;
    }


    public int deleteNoteById(long noteId, SQLiteDatabase db) {
        return db.delete("notes", "_id=?", new String[]{String.valueOf(noteId)});
    }

    public Cursor getNoteById(String username, long noteId, SQLiteDatabase db) {
        return db.rawQuery("SELECT * FROM notes WHERE _id = ? AND username = ?", new String[]{String.valueOf(noteId), username});
    }


    public int updateNoteById(long noteId, ContentValues values, String username, SQLiteDatabase db) {
        return db.update("notes", values, "_id=? AND username=?", new String[]{String.valueOf(noteId), username});
    }
}
