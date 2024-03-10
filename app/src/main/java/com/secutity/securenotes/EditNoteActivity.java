package com.secutity.securenotes;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import net.sqlcipher.Cursor;
import net.sqlcipher.database.SQLiteDatabase;

public class EditNoteActivity extends AppCompatActivity {
    private EditText editTextTitle;
    private EditText editTextContent;
    private Button buttonSave;
    private long noteId;
    private String username;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_note);

        editTextTitle = findViewById(R.id.editTextTitle);
        editTextContent = findViewById(R.id.editTextContent);
        buttonSave = findViewById(R.id.buttonSave);

        noteId = getIntent().getLongExtra("noteId", -1);

        SharedPreferences preferences = getSharedPreferences("user_prefs", MODE_PRIVATE);
        username = preferences.getString("current_username", "");

        if (noteId != -1) {
            loadNoteFromDatabase();
        }

        buttonSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                updateNoteInDatabase();

                Intent resultIntent = new Intent();
                setResult(RESULT_OK, resultIntent);
                finish();
            }
        });
    }

    @SuppressLint("Range")
    private void loadNoteFromDatabase() {
        SQliteHelper helper = new SQliteHelper(this);
        SQLiteDatabase db = helper.getWritableDatabase("password");

        Cursor cursor = helper.getNoteById(username, noteId, db);

        if (cursor != null && cursor.moveToFirst()) {
            String title = cursor.getString(cursor.getColumnIndex("title"));
            String encryptedContent = cursor.getString(cursor.getColumnIndex("content"));
            String content = AES.decrypt(encryptedContent);

            editTextTitle.setText(title);
            editTextContent.setText(content);

            cursor.close();

            Log.d("EditNoteActivity", "Loaded note: NoteId" + noteId  +  " Title=" + title + ", Content=" + content);
        }
    }

    private void updateNoteInDatabase() {
        String updatedTitle = editTextTitle.getText().toString();
        String updatedContent = editTextContent.getText().toString();

        SQliteHelper helper = new SQliteHelper(this);
        SQLiteDatabase db = helper.getWritableDatabase("password");

        ContentValues values = new ContentValues();
        values.put("title", updatedTitle);
        values.put("content", AES.encrypt(updatedContent));

        int rowsUpdated = helper.updateNoteById(noteId, values, username, db);

        if (rowsUpdated > 0) {
            // Добавим логирование для отслеживания успешного обновления
            Log.d("EditNoteActivity", "Note updated successfully");

            Intent resultIntent = new Intent();
            setResult(RESULT_OK, resultIntent);
        } else {
            // Добавим логирование для отслеживания неудачного обновления
            Log.d("EditNoteActivity", "Failed to update note");
        }

        finish();
    }

}
