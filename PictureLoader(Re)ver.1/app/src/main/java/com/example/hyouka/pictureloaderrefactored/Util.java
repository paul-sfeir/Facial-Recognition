package com.example.hyouka.pictureloaderrefactored;

import java.io.File;
import java.io.FileInputStream;
import java.security.MessageDigest;

/**
 * Created by Hyouka on 7/29/2016.
 */
public class Util {
   public String MD5(File file){
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
}
