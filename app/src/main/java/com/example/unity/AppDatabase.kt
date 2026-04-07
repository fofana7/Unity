package com.example.unity

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log

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
            val authorIndex = cursor.getColumnIndex(COLUMN_AUTHOR)
            val contentIndex = cursor.getColumnIndex(COLUMN_CONTENT)
            val uriIndex = cursor.getColumnIndex(COLUMN_URI)
            val isImageIndex = cursor.getColumnIndex(COLUMN_IS_IMAGE)
            val timeIndex = cursor.getColumnIndex(COLUMN_TIME)

            do {
                postsList.add(Post(
                    if (authorIndex != -1) cursor.getString(authorIndex) else "Inconnu",
                    if (contentIndex != -1) cursor.getString(contentIndex) else "",
                    if (uriIndex != -1) cursor.getString(uriIndex) else null,
                    if (isImageIndex != -1) cursor.getInt(isImageIndex) == 1 else false,
                    if (timeIndex != -1) cursor.getString(timeIndex) else ""
                ))
            } while (cursor.moveToNext())
        }
        cursor.close()
        return postsList
    }

    // --- OUTILS DE VÉRIFICATION ET NETTOYAGE ---

    /**
     * Affiche tout le contenu de la base de données dans le Logcat (Filtre: DB_DEBUG)
     */
    fun debugLogDatabase() {
        Log.d("DB_DEBUG", "--- CONTENU DE LA BASE DE DONNÉES ---")
        
        val emails = getAllEmails()
        Log.d("DB_DEBUG", "> Utilisateurs enregistrés (${emails.size}) :")
        emails.forEach { Log.d("DB_DEBUG", "  - $it") }

        val posts = getAllPosts()
        Log.d("DB_DEBUG", "> Nombre de posts locaux : ${posts.size}")
        if (posts.isNotEmpty()) {
            Log.d("DB_DEBUG", "  Dernier post de : ${posts[0].author}")
        }
        Log.d("DB_DEBUG", "--------------------------------------")
    }

    /**
     * Vide toute la base de données locale
     */
    fun clearAllData() {
        val db = this.writableDatabase
        db.delete(TABLE_USERS, null, null)
        db.delete(TABLE_POSTS, null, null)
        Log.d("DB_DEBUG", "Base de données vidée.")
    }
}
