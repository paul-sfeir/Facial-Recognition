package com.example.hyouka.pictureloaderrefactored;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by Hyouka on 6/11/2016.
 */
public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "facedata.db";
    private static final int SCHEMA_VERSION = 1;
    private Context context;
    public DatabaseHelper(Context context){
        super(context,DATABASE_NAME,null,SCHEMA_VERSION);
        this.context = context;
    }
    public static final class TableDefinition{
        public static final String TABLE_NAME = "FaceData";
        public static final String COLUMN_NAME_1 = "Time";
        public static final String COLUMN_TYPE_1 = "DATETIME";
        public static final String COLUMN_NULL_1 = "NOT NULL";
        public static final String COLUMN_NAME_2 = "File";
        public static final String COLUMN_TYPE_2 = "TEXT";
        public static final String COLUMN_NULL_2 = "NOT NULL";
    }
    private static final String COMMON_SEP = ",";
    private static final String CREATE_TABLE_STATEMENT =
            "CREATE TABLE "+TableDefinition.TABLE_NAME+"("+
                    TableDefinition.COLUMN_NAME_1+" "+TableDefinition.COLUMN_TYPE_1+" "+TableDefinition.COLUMN_NULL_1+COMMON_SEP+
                    TableDefinition.COLUMN_NAME_2+" "+TableDefinition.COLUMN_TYPE_2+" "+TableDefinition.COLUMN_NULL_2+
                    ");";
    private static final String DROP_TABLE_STATEMENT = "DROP TABLE IF EXISTS "+TableDefinition.TABLE_NAME;
    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_STATEMENT);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(DROP_TABLE_STATEMENT);
        onCreate(db);
    }

    public void insert(long time,String file){
        ContentValues cv = new ContentValues();
        cv.put(TableDefinition.COLUMN_NAME_1,time);
        cv.put(TableDefinition.COLUMN_NAME_2,file);
        getWritableDatabase().insert(TableDefinition.TABLE_NAME,TableDefinition.COLUMN_NAME_1,cv);
    }
    public void deleteAll(){
        getWritableDatabase().execSQL("DELETE * FROM "+TableDefinition.TABLE_NAME);
    }
    public Cursor getAll(){
        return getReadableDatabase().rawQuery("SELECT FROM "+TableDefinition.TABLE_NAME,null);
    }
    public Cursor getTime(){
        return getReadableDatabase().rawQuery("SELECT "+TableDefinition.COLUMN_NAME_1+" FROM "+TableDefinition.TABLE_NAME,null);
    }
    public Cursor getFileDescription(){
        return getReadableDatabase().rawQuery("SELECT "+TableDefinition.COLUMN_NAME_2+" FROM "+TableDefinition.TABLE_NAME,null);
    }
    public void executeSqlStatement(String sql){
        getWritableDatabase().execSQL(sql);
    }
    public Cursor get(String sql){
        return getReadableDatabase().rawQuery(sql,null);
    }
}
