package com.example.ishan.googledrivedemo;

import org.apache.commons.codec.binary.Hex;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

public class HashFile {
    public String hashFile(File file){
        try{
            FileInputStream fis = new FileInputStream(file);
            byte data[] = org.apache.commons.codec.digest.DigestUtils.md5(fis);
            char md5Chars[] = Hex.encodeHex(data);

            String md5 = String.valueOf(md5Chars);
            return md5;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
