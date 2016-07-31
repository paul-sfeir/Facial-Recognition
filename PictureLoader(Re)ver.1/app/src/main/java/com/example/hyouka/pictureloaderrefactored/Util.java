package com.example.hyouka.pictureloaderrefactored;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.File;
import java.io.FileInputStream;
import java.security.MessageDigest;

/**
 * Created by Hyouka on 7/29/2016.
 */
public class Util {
   public static String MD5(File file){
       try {
           MessageDigest md = MessageDigest.getInstance("MD5");
           FileInputStream fis = new FileInputStream(file.getAbsolutePath());

           byte[] dataBytes = new byte[1024];

           int nread = 0;
           while ((nread = fis.read(dataBytes)) != -1) {
               md.update(dataBytes, 0, nread);
           }
           ;
           byte[] mdbytes = md.digest();

           //convert the byte to hex format method 1
           StringBuffer sb = new StringBuffer();
           for (int i = 0; i < mdbytes.length; i++) {
               sb.append(Integer.toString((mdbytes[i] & 0xff) + 0x100, 16).substring(1));
           }

           return sb.toString();
       }catch (Exception e){}
       return null;
/*
       //convert the byte to hex format method 2
       StringBuffer hexString = new StringBuffer();
       for (int i=0;i<mdbytes.length;i++) {
           String hex=Integer.toHexString(0xff & mdbytes[i]);
           if(hex.length()==1) hexString.append('0');
           hexString.append(hex);
       }
       System.out.println("Digest(in hex format):: " + hexString.toString());
*/
   }
    public Bitmap decodeFromFile(String path){
        BitmapFactory.Options opt = new BitmapFactory.Options();
        opt.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, opt);
        opt.inSampleSize = PictureManager.calculateInSampleSize(opt,350,400);
        opt.inJustDecodeBounds = false;
        opt.inPreferredConfig = Bitmap.Config.RGB_565;
        Bitmap image = BitmapFactory.decodeFile(path, opt);
        return image;
    }

    public static boolean isImage(String name){
        String[] extension = {".jpg", ".png", ".jpeg"};
        for(int i=0;i<extension.length;++i){
            if(name.endsWith(extension[i]))return true;
        }
        return false;
    }
/*
    public static void test(MainActivity activity, ImageView iv){
        PictureManager myPicture = new PictureManager(activity,iv);
        String path = Environment.getExternalStorageDirectory().getAbsolutePath();
        path+="/com.example.hyouka.pictureloaderrefactored";
        File[] files = new File(path).listFiles();
        for(int i=0;i<files.length;++i){
            if(files[i].isFile()&&isImage(files[i].getName())){
                myPicture.setPicture(files[i].getAbsolutePath());
                myPicture.detectFaces(10);
            }
        }
    }
    */
}

