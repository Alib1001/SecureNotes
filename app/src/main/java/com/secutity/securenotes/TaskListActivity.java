package com.secutity.securenotes;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import net.sqlcipher.Cursor;
import net.sqlcipher.database.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;

public class TaskListActivity extends AppCompatActivity {
    private static final int REQUEST_CODE_ADD_NOTE = 1;
    private static final int REQUEST_CODE_EDIT_NOTE = 2;
    private List<Note> notes;
    private NoteAdapter adapter;

    SharedPreferences preferences;
    String username;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_list);

        preferences = getSharedPreferences("user_prefs", MODE_PRIVATE);
        username = preferences.getString("current_username", "");

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Notes List");

        notes = new ArrayList<>();
        adapter = new NoteAdapter(this, notes);

        ListView listView = findViewById(R.id.listView);
        listView.setAdapter(adapter);
        registerForContextMenu(listView);

        loadNotesFromDatabase(username);
        adapter.notifyDataSetChanged();

        findViewById(R.id.fab).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(TaskListActivity.this, AddNoteActivity.class);
                startActivityForResult(intent, REQUEST_CODE_ADD_NOTE);
            }
        });

        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                showPopupMenu(view, position);
                return true;
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_ADD_NOTE || requestCode == REQUEST_CODE_EDIT_NOTE) {
            if (resultCode == RESULT_OK) {
                loadNotesFromDatabase(username);
                adapter.notifyDataSetChanged();
            }
        }
    }

    @SuppressLint("Range")
    private void loadNotesFromDatabase(String username) {
        SQliteHelper helper = new SQliteHelper(this);
        SQLiteDatabase db = helper.getReadableDatabase("password");

        Cursor cursor = helper.getAllNotesByUsername(username, db);
        List<Note> loadedNotes = new ArrayList<>();

        if (cursor != null) {
            try {
                while (cursor.moveToNext()) {
                    int id = cursor.getInt(cursor.getColumnIndex("_id"));
                    String title = cursor.getString(cursor.getColumnIndex("title"));
                    String encryptedContent = cursor.getString(cursor.getColumnIndex("content"));

                    String content = AES.decrypt(encryptedContent);

                    Note note = new Note(id, title, content);
                    loadedNotes.add(note);
                }
            } finally {
                cursor.close();
            }
        }

        notes.clear();
        notes.addAll(loadedNotes);
        adapter.notifyDataSetChanged();
    }




    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        int position = info.position;

        if (item.getItemId() == R.id.menu_edit) {
            editNote(position);
            return true;
        } else if (item.getItemId() == R.id.menu_delete) {
            deleteNote(position);
            return true;
        } else {
            return super.onContextItemSelected(item);
        }
    }
    private void editNote(int position) {
        long noteId = notes.get(position).getId();

        Intent intent = new Intent(TaskListActivity.this, EditNoteActivity.class);
        intent.putExtra("noteId", noteId);
        startActivityForResult(intent, REQUEST_CODE_EDIT_NOTE);
    }



    private void showPopupMenu(View view, final int position) {
        PopupMenu popupMenu = new PopupMenu(this, view);
        MenuInflater inflater = popupMenu.getMenuInflater();
        inflater.inflate(R.menu.menu_item, popupMenu.getMenu());

        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if (item.getItemId() == R.id.menu_delete) {
                    deleteNote(position);
                    return true;
                } else if (item.getItemId() == R.id.menu_edit) {
                    editNote(position);
                    return true;
                } else {
                    return false;
                }
            }
        });

        popupMenu.show();
    }

    private void deleteNote(int position) {
        SQliteHelper helper = new SQliteHelper(this);
        SQLiteDatabase db = helper.getWritableDatabase("password");

        long noteId = notes.get(position).getId();

        int rowsDeleted = helper.deleteNoteById(noteId, db);

        if (rowsDeleted > 0) {
            notes.remove(position);
            adapter.notifyDataSetChanged();

            Toast.makeText(this, "Note deleted", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Failed to delete note", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_exit, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.menu_exit) {
            logoutUser();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void logoutUser() {
        preferences.edit().remove("current_username").apply();
        Intent intent = new Intent(TaskListActivity.this, FingerActivity.class);
        startActivity(intent);
        finish();
    }
}
