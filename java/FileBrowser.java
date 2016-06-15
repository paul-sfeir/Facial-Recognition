package com.example.hyouka.pictureloaderrefactored;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Hyouka on 4/2/2016.
 */
public class FileBrowser extends Activity {
    ArrayList<String> LevelName;
    ListView list;
    TextView path_text;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.filebrowser);

        // initialize
        path_text = (TextView) findViewById(R.id.path_text);
        list = (ListView) findViewById(R.id.browser);
        LevelName = new ArrayList<String>();
        LevelName.add(Environment.getExternalStorageDirectory().getPath());

        Button UpBtn = (Button) findViewById(R.id.up_dir_btn);
        UpBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (LevelName.size() > 1) {
                    LevelName.remove(LevelName.size() - 1);

                    String currentPath = LevelName.get(0);
                    for (int i = 1; i < LevelName.size(); ++i) {
                        currentPath = currentPath + '/' + LevelName.get(i);
                    }
                    File file = new File(currentPath);
                    updateList(file);
                }
            }
        });


        // implement listview onitemclicker
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String selected = ((TextView) view.findViewById(R.id.file_name)).getText().toString();
                LevelName.add(selected);

                String currentPath = LevelName.get(0);
                for (int i = 1; i < LevelName.size(); ++i) {
                    currentPath = currentPath + '/' + LevelName.get(i);
                }
                File file = new File(currentPath);
                if (file.isFile()) {
                    if (file.exists()) {
                        Intent i = new Intent();
                        i.putExtra("ImageFilePath", currentPath);
                        setResult(RESULT_OK, i);
                        finish();
                    } else {
                        Toast.makeText(getApplicationContext(),"File not exist",Toast.LENGTH_SHORT).show();
                    }
                } else {
                    updateList(file);
                }
            }
        });
        updateList(Environment.getExternalStorageDirectory());
    }

    public void updateList(File file){
        path_text.setText(file.getPath());
        File[] files = file.listFiles();
        try {
            if (!files.equals(null)) {
                List<Map<String, Object>> filelist = new ArrayList<Map<String, Object>>();
                for(int i=0;i<files.length;++i){
                    Map<String, Object> filelistmap = new HashMap<String, Object>();
                    String name = files[i].getName();
                    if(files[i].isFile()){
                        if(isImage(name)){
                            filelistmap.put("file", R.drawable.image);
                        }else{
                            filelistmap.put("file", R.drawable.file);
                        }
                    }else{
                        filelistmap.put("file", R.drawable.folder);
                    }
                    filelistmap.put("filename", name);
                    filelist.add(filelistmap);
                }
                SimpleAdapter listviewsimpleadapter = new SimpleAdapter(this, filelist, R.layout.filebrowser_listview,
                        new String[]{"file", "filename"}, new int[]{R.id.file_image, R.id.file_name});
                list.setAdapter(listviewsimpleadapter);
            }
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    public boolean isImage(String name){
        String[] extension = {".jpg", ".png", ".jpeg"};
        for(int i=0;i<extension.length;++i){
            if(name.endsWith(extension[i]))return true;
        }
        return false;
    }
}
