/*
 * Copyright (c) 2025 Bart Vullings <dev@windkracht8.com>
 *
 * This file is part of FitoTrack
 *
 * FitoTrack is free software: you can redistribute it and/or modify
 * 	it under the terms of the GNU General Public License as published by
 * 	the Free Software Foundation, either version 3 of the License, or
 * 	(at your option) any later version.
 *
 * 	FitoTrack is distributed in the hope that it will be useful,
 * 	but WITHOUT ANY WARRANTY; without even the implied warranty of
 * 	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * 	GNU General Public License for more details.
 *
 * 	You should have received a copy of the GNU General Public License
 * 	along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.tadris.fitness.data;

import static android.content.Context.CONNECTIVITY_SERVICE;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.util.Log;

import androidx.annotation.NonNull;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPFileFilter;

import de.tadris.fitness.R;
import de.tadris.fitness.ui.Main;
import de.tadris.fitness.ui.settings.MapDownload;

public class MapStore{
    private static final String url = "ftp-stud.hs-esslingen.de";
    private static final String username = "anonymous";
    private static final String password = "de.tadris.fitness.wearos";
    private static final String rootPath = "pub/.3/download.mapsforge.org/maps/v5/";

    private FTPClient ftpClient;

    public void close(){
        try{
            if(ftpClient != null) ftpClient.disconnect();
        }catch(Exception e){
            Log.e(Main.LOG_TAG, "close Exception: " + e);
            Log.e(Main.LOG_TAG, "close Exception: " + e.getMessage());
        }
        ftpClient = null;
    }
    public static List<File> getLocalMaps(Context context){
        List<File> localMaps = new ArrayList<>();
        try{
            java.io.File[] files = context.getFilesDir().listFiles();
            if(files == null) return localMaps;
            Arrays.sort(files);
            for(int i = files.length-1; i>=0; i--){
                String filename = files[i].getName();
                if(filename.endsWith(".map")){
                    localMaps.add(new File(filename, files[i].length(), files[i].lastModified()));
                }
            }
        }catch(Exception e){
            Log.e(Main.LOG_TAG, "getLocalMaps Exception: " + e);
            Log.e(Main.LOG_TAG, "getLocalMaps Exception: " + e.getMessage());
        }
        return localMaps;
    }
    public boolean deleteLocalMap(Context context, File file){
        Log.d(Main.LOG_TAG, "deleteLocalMap: " + file.name);
        java.io.File fileLocal = file.getLocalFile(context);
        if(!fileLocal.exists()){
            Log.d(Main.LOG_TAG, "deleteLocalMap file does not exist");
            return false;
        }
        return fileLocal.delete();
    }

    public List<Dir> getRemoteDirs(String path){
        List<Dir> dirs = new ArrayList<>();
        try{
            for(FTPFile ftpFile : getFTPClient().listDirectories(rootPath + path)){
                dirs.add(new Dir(ftpFile.getName(), path));
            }
        }catch(Exception e){
            Log.e(Main.LOG_TAG, "getRemoteDirs Exception: " + e);
            Log.e(Main.LOG_TAG, "getRemoteDirs Exception: " + e.getMessage());
        }
        return dirs;
    }
    public List<File> getRemoteFiles(String path){
        List<File> files = new ArrayList<>();
        try{
            FTPFileFilter filter = ftpFile->ftpFile.isFile() && ftpFile.getName().endsWith(".map");
            for(FTPFile ftpFile : getFTPClient().listFiles(rootPath + path, filter)){
                files.add(new File(ftpFile.getName(), path, ftpFile.getSize(), ftpFile.getTimestamp().getTimeInMillis()));
            }
        }catch(Exception e){
            Log.e(Main.LOG_TAG, "getRemoteFiles Exception: " + e);
            Log.e(Main.LOG_TAG, "getRemoteFiles Exception: " + e.getMessage());
        }
        return files;
    }
    public void getRemoteFile(MapDownload mapDownload, File fileRemote){
        Log.d(Main.LOG_TAG, "getRemoteFile: " + fileRemote.path);
        java.io.File fileLocal = new java.io.File(fileRemote.toLocal());
        if(fileLocal.exists() && (fileLocal.lastModified() > fileRemote.lastModified || !fileLocal.delete())){
            mapDownload.onError(R.string.error_maps_file_exists);
            return;
        }
        try{
            ConnectivityManager connectivityManager = (ConnectivityManager) mapDownload.getSystemService(CONNECTIVITY_SERVICE);
            connectivityManager.requestNetwork(
                    new NetworkRequest.Builder()
                            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                            .build(),
                    new ConnectivityManager.NetworkCallback(){
                        public void onAvailable(@NonNull Network network){
                            super.onAvailable(network);
                            Log.d(Main.LOG_TAG, "MapStore.getRemoteFile WIFI is available");
                            getRemoteFileDownload(mapDownload, fileRemote);
                        }
                        public void onUnavailable(){
                            super.onUnavailable();
                            Log.d(Main.LOG_TAG, "MapStore.getRemoteFile WIFI is not available");
                            getRemoteFileDownload(mapDownload, fileRemote);
                            mapDownload.onWarning(R.string.fail_fast);
                        }
                    },
                    2000
            );
        }catch(Exception e){
            Log.d(Main.LOG_TAG, "MapStore.getRemoteFile.exception " + e);
            Log.d(Main.LOG_TAG, "MapStore.getRemoteFile.exception " + e.getMessage());
            getRemoteFileDownload(mapDownload, fileRemote);
            mapDownload.onWarning(R.string.fail_fast);
        }
    }
    private void getRemoteFileDownload(MapDownload mapDownload, File fileRemote){
        try(InputStream inputStream = getFTPClient().retrieveFileStream(rootPath + fileRemote.path)){
            try(FileOutputStream fileOutputStream = mapDownload.openFileOutput(fileRemote.toLocal(), Context.MODE_PRIVATE)){
                long bytesDone = 0;
                long last_read_time = System.currentTimeMillis();
                while(System.currentTimeMillis() - last_read_time < 3000){
                    if(inputStream.available() == 0){
                        sleep100();
                        continue;
                    }
                    byte[] buffer = new byte[2048];
                    int numBytes = inputStream.read(buffer);
                    if(numBytes < 0){
                        mapDownload.onError(R.string.error_maps_download);
                        return;
                    }
                    fileOutputStream.write(buffer, 0, numBytes);
                    bytesDone += numBytes;

                    mapDownload.onProgress(bytesDone);
                    if(inputStream.available() == 0 && bytesDone >= fileRemote.size){
                        mapDownload.onDone();
                        return;
                    }
                    last_read_time = System.currentTimeMillis();
                }
                Log.e(Main.LOG_TAG, "getRemoteFile not enough bytes and no new data after 3 sec");
            }catch(Exception e){
                Log.e(Main.LOG_TAG, "getRemoteFile.FileOutputStream exception: " + e.getMessage());
            }
        }catch(Exception e){
            Log.e(Main.LOG_TAG, "getRemoteFile Exception: " + e);
            Log.e(Main.LOG_TAG, "getRemoteFile Exception: " + e.getMessage());
        }
        mapDownload.onError(R.string.error_maps_download);
    }
    public boolean checkHasUpdate(File file) throws IOException{
        for(FTPFile ftpFile : getFTPClient().listFiles(rootPath + file.path)){
            return file.lastModified < ftpFile.getTimestamp().getTimeInMillis();
        }
        return true;
    }
    private FTPClient getFTPClient() throws IOException{
        if(ftpClient != null) return ftpClient;
        ftpClient = new FTPClient();
        ftpClient.connect(url);
        ftpClient.login(username, password);
        ftpClient.enterLocalPassiveMode();
        ftpClient.setFileType(FTPClient.BINARY_FILE_TYPE);
        return ftpClient;
    }
    private static void sleep100(){
        try{
            Thread.sleep(100);
        }catch(Exception e){
            Log.e(Main.LOG_TAG, "sleep100 exception: " + e.getMessage());
        }
    }

    public static class Dir{
        public final String name;
        public final String path;
        private Dir(String name, String dir){
            this.name = name;
            path = dir + name;
        }
    }
    public static class File{
        public String name;
        public final String path;
        public final long size;
        private final long lastModified;
        private File(String path, long size, long lastModified){
            this.path = path.replaceAll("_", "/");
            name = this.path;
            int index = this.path.lastIndexOf("/");
            if(index > 0) name = path.substring(index+1);
            this.size = size;
            this.lastModified = lastModified;
        }
        private File(String name, String dir, long size, long lastModified){
            this.name = name;
            path = dir + name;
            this.size = size;
            this.lastModified = lastModified;
        }
        private String toLocal(){return path.replaceAll("/", "_");}
        public java.io.File getLocalFile(Context context){return new java.io.File(context.getFilesDir(), toLocal());}
    }
}
