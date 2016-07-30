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
        public static final String IMAGE_FILE_TABLE_NAME = "ImageFileData";
        public static final String[][] IMAGE_FILE_TABLE_COLUMNS =
                {
                        {"md5","TEXT","NOT NULL"},
                        {"time","INTEGER","NOT NULL"},
                        {"path","TEXT","NOT NULL"},
                        {"face_num","INTEGER","NOT NULL"}
                };
        public static final String FACE_TABLE_NAME = "FaceData";
        public static final String[][] FACE_TABLE_COLUMNS =
                {
                        {"md5", "TEXT", "NOT NULL"},
                        {"name","TEXT", "NULL"},
                        {"sample","INTEGER","NOT NULL"},
                        {"eye_dis","TEXT","NOT NULL"},
                        {"mid_point_x","TEXT","NOT NULL"},
                        {"mid_point_y","TEXT","NOT NULL"},
                        {"confidence","TEXT","NOT NULL"},
                        {"euler_x","TEXT","NOT NULL"},
                        {"euler_y","TEXT","NOT NULL"},
                        {"euler_z","TEXT","NOT NULL"}
                };
    }
    private String generateCreateTableStatement(String name,String[][] columns){
        String statement = "CREATE TABLE "+name+"(";
        for(int i=0;i<columns.length-1;++i){
            for(int j=0;j<columns[i].length;++j){
                statement += columns[i][j] + " ";
            }
            statement += ",";
        }
        for(int j=0;j<columns[columns.length-1].length;++j){
            statement += columns[columns.length-1][j] + " ";
        }
        statement += ");";
        return statement;
    }
    private String generateDropTableStatement(String name){
        return "DROP TABLE IF EXISTS " + name;
    }
    @Override
    public void onCreate(SQLiteDatabase db) {
        String IMAGE_FILE_TABLE_STATEMENT =
                generateCreateTableStatement(TableDefinition.IMAGE_FILE_TABLE_NAME,TableDefinition.IMAGE_FILE_TABLE_COLUMNS);
        String FACE_TABLE_STATEMENT =
                generateCreateTableStatement(TableDefinition.FACE_TABLE_NAME,TableDefinition.FACE_TABLE_COLUMNS);
        db.execSQL(IMAGE_FILE_TABLE_STATEMENT);
        db.execSQL(FACE_TABLE_STATEMENT);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        String DROP_IMAGE_FILE_TABLE_STATEMENT = generateDropTableStatement(TableDefinition.IMAGE_FILE_TABLE_NAME);
        String DROP_FACE_TABLE_STATEMENT = generateDropTableStatement(TableDefinition.FACE_TABLE_NAME);
        db.execSQL(DROP_IMAGE_FILE_TABLE_STATEMENT);
        db.execSQL(DROP_FACE_TABLE_STATEMENT);
        onCreate(db);
    }

    public void addImageFileRecord(String md5,long time,String path,int face_num){
        ContentValues cv = new ContentValues();
        cv.put(TableDefinition.IMAGE_FILE_TABLE_COLUMNS[0][0],md5);
        cv.put(TableDefinition.IMAGE_FILE_TABLE_COLUMNS[1][0],time);
        cv.put(TableDefinition.IMAGE_FILE_TABLE_COLUMNS[2][0],path);
        cv.put(TableDefinition.IMAGE_FILE_TABLE_COLUMNS[3][0],face_num);
        getWritableDatabase().insert(TableDefinition.IMAGE_FILE_TABLE_NAME,TableDefinition.IMAGE_FILE_TABLE_COLUMNS[0][0],cv);
    }
    public void addFaceRecord(String md5,String name,int sample,String eye_dis,String mid_x,String mid_y,String confidence,String euler_x,String euler_y,String euler_z){
        ContentValues cv  = new ContentValues();
        cv.put(TableDefinition.FACE_TABLE_COLUMNS[0][0],md5);
        cv.put(TableDefinition.FACE_TABLE_COLUMNS[1][0],name);
        cv.put(TableDefinition.FACE_TABLE_COLUMNS[2][0],sample);
        cv.put(TableDefinition.FACE_TABLE_COLUMNS[3][0],eye_dis);
        cv.put(TableDefinition.FACE_TABLE_COLUMNS[4][0],mid_x);
        cv.put(TableDefinition.FACE_TABLE_COLUMNS[5][0],mid_y);
        cv.put(TableDefinition.FACE_TABLE_COLUMNS[6][0],confidence);
        cv.put(TableDefinition.FACE_TABLE_COLUMNS[7][0],euler_x);
        cv.put(TableDefinition.FACE_TABLE_COLUMNS[8][0],euler_y);
        cv.put(TableDefinition.FACE_TABLE_COLUMNS[9][0],euler_z);
        getWritableDatabase().insert(TableDefinition.FACE_TABLE_NAME,TableDefinition.FACE_TABLE_COLUMNS[0][0],cv);
    }
    public void deleteImageFileRecord(String columnName,String para){
        String sql = "DELETE * FROM "+TableDefinition.IMAGE_FILE_TABLE_NAME+" WHERE "+columnName+"='"+para+"'";
        getWritableDatabase().execSQL(sql);
    }
    public void deleteFaceRecord(String columnName,String para){
        String sql = "DELETE * FROM "+TableDefinition.FACE_TABLE_NAME+" WHERE "+columnName+"='"+para+"'";
        getWritableDatabase().execSQL(sql);
    }
    public void deleteAllImageFileRecord(){
        getWritableDatabase().execSQL("DELETE * FROM "+TableDefinition.IMAGE_FILE_TABLE_NAME);
    }
    public void deleteAllFaceRecord(){
        getWritableDatabase().execSQL("DELETE * FROM "+TableDefinition.FACE_TABLE_NAME);
    }
    public void executeSqlStatement(String sql){
        getWritableDatabase().execSQL(sql);
    }
    public Cursor query(String sql){
        return getReadableDatabase().rawQuery(sql,null);
    }
}
