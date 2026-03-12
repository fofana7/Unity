package com.example.unity

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class AppDatabase(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "UnitySocial.db"
        private const val DATABASE_VERSION = 1

        // Table Utilisateurs
        private const val TABLE_USERS = "users"
        private const val COLUMN_EMAIL = "email"
        private const val COLUMN_PASSWORD = "password"
        private const val COLUMN_USERNAME = "username"
        private const val COLUMN_FIRSTNAME = "firstname"
        private const val COLUMN_LASTNAME = "lastname"
        private const val COLUMN_ROLE = "role"

        // Table Posts
        private const val TABLE_POSTS = "posts"
        private const val COLUMN_ID = "id"
        private const val COLUMN_AUTHOR = "author"
        private const val COLUMN_CONTENT = "content"
        private const val COLUMN_URI = "uri"
        private const val COLUMN_IS_IMAGE = "is_image"
        private const val COLUMN_TIME = "time"
    }

    override fun onCreate(db: SQLiteDatabase?) {
        val createUsersTable = ("CREATE TABLE " + TABLE_USERS + "("
                + COLUMN_EMAIL + " TEXT PRIMARY KEY,"
                + COLUMN_PASSWORD + " TEXT,"
                + COLUMN_USERNAME + " TEXT,"
                + COLUMN_FIRSTNAME + " TEXT,"
                + COLUMN_LASTNAME + " TEXT,"
                + COLUMN_ROLE + " TEXT" + ")")
        
        val createPostsTable = ("CREATE TABLE " + TABLE_POSTS + "("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_AUTHOR + " TEXT,"
                + COLUMN_CONTENT + " TEXT,"
                + COLUMN_URI + " TEXT,"
                + COLUMN_IS_IMAGE + " INTEGER,"
                + COLUMN_TIME + " TEXT" + ")")

        db?.execSQL(createUsersTable)
        db?.execSQL(createPostsTable)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS)
        db?.execSQL("DROP TABLE IF EXISTS " + TABLE_POSTS)
        onCreate(db)
    }

    // --- GESTION DES UTILISATEURS ---

    fun saveUser(email: String, password: String, username: String, firstName: String, lastName: String, role: String) {
        val db = this.writableDatabase
        val values = ContentValues()
        values.put(COLUMN_EMAIL, email)
        values.put(COLUMN_PASSWORD, password)
        values.put(COLUMN_USERNAME, username)
        values.put(COLUMN_FIRSTNAME, firstName)
        values.put(COLUMN_LASTNAME, lastName)
        values.put(COLUMN_ROLE, role)
        
        db.insertWithOnConflict(TABLE_USERS, null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun isUserExists(email: String): Boolean {
        val db = this.readableDatabase
        val cursor = db.query(TABLE_USERS, arrayOf(COLUMN_EMAIL), "$COLUMN_EMAIL=?", arrayOf(email), null, null, null)
        val exists = cursor.count > 0
        cursor.close()
        return exists
    }

    fun getUserPassword(email: String): String? {
        val db = this.readableDatabase
        val cursor = db.query(TABLE_USERS, arrayOf(COLUMN_PASSWORD), "$COLUMN_EMAIL=?", arrayOf(email), null, null, null)
        var password: String? = null
        if (cursor.moveToFirst()) {
            password = cursor.getString(0)
        }
        cursor.close()
        return password
    }

    fun getUserName(email: String): String {
        val db = this.readableDatabase
        val cursor = db.query(TABLE_USERS, arrayOf(COLUMN_USERNAME), "$COLUMN_EMAIL=?", arrayOf(email), null, null, null)
        var username = "Utilisateur"
        if (cursor.moveToFirst()) {
            username = cursor.getString(0)
        }
        cursor.close()
        return username
    }

    fun getUserRole(email: String): String {
        val db = this.readableDatabase
        val cursor = db.query(TABLE_USERS, arrayOf(COLUMN_ROLE), "$COLUMN_EMAIL=?", arrayOf(email), null, null, null)
        var role = "Étudiant"
        if (cursor.moveToFirst()) {
            role = cursor.getString(0)
        }
        cursor.close()
        return role
    }

    fun getUserFullName(email: String): String {
        val db = this.readableDatabase
        val cursor = db.query(TABLE_USERS, arrayOf(COLUMN_FIRSTNAME, COLUMN_LASTNAME), "$COLUMN_EMAIL=?", arrayOf(email), null, null, null)
        var fullName = ""
        if (cursor.moveToFirst()) {
            fullName = "${cursor.getString(0)} ${cursor.getString(1)}".trim()
        }
        cursor.close()
        return fullName
    }

    fun getAllEmails(): List<String> {
        val list = mutableListOf<String>()
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT $COLUMN_EMAIL FROM $TABLE_USERS", null)
        if (cursor.moveToFirst()) {
            do {
                list.add(cursor.getString(0))
            } while (cursor.moveToNext())
        }
        cursor.close()
        return list
    }

    // --- GESTION DES POSTS ---

    fun savePost(author: String, content: String, uri: String?, isImage: Boolean, time: String) {
        val db = this.writableDatabase
        val values = ContentValues()
        values.put(COLUMN_AUTHOR, author)
        values.put(COLUMN_CONTENT, content)
        values.put(COLUMN_URI, uri)
        values.put(COLUMN_IS_IMAGE, if (isImage) 1 else 0)
        values.put(COLUMN_TIME, time)
        
        db.insert(TABLE_POSTS, null, values)
    }

    fun getAllPosts(): List<Post> {
        val postsList = mutableListOf<Post>()
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_POSTS ORDER BY $COLUMN_ID DESC", null)
        
        if (cursor.moveToFirst()) {
            do {
                postsList.add(Post(
                    cursor.getString(1), // author
                    cursor.getString(2), // content
                    cursor.getString(3), // uri
                    cursor.getInt(4) == 1, // is_image
                    cursor.getString(5) // time
                ))
            } while (cursor.moveToNext())
        }
        cursor.close()
        return postsList
    }

    fun clearAllPosts() {
        val db = this.writableDatabase
        db.delete(TABLE_POSTS, null, null)
    }
}
