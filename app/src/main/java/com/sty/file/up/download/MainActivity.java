package com.sty.file.up.download;

import android.content.Context;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.lidroid.xutils.HttpUtils;
import com.lidroid.xutils.exception.HttpException;
import com.lidroid.xutils.http.ResponseInfo;
import com.lidroid.xutils.http.callback.RequestCallBack;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.sty.file.up.download.utils.SharedUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import cz.msebera.android.httpclient.Header;

/**
 * XUtils GitHub 地址：https://github.com/wyouflf/xUtils
 */
public class MainActivity extends AppCompatActivity implements View.OnClickListener{
    private EditText etFilePath;
    private Button btnFileUpload;

    private EditText etThreadCount;
    private Button btnDownload;
    private Button btnDeleteSp;
    private LinearLayout llProgressLayout;
    private EditText etFileDownloadPathXUtils;
    private Button btnDownloadXUtils;

    private Context mContext;

    private int threadCount = 0; // 开启3个线程
    private int blockSize = 0; //每个线程下载的大小
    private int runningThreadCount = 0; //当前运行的线程数
    private String path = "http://192.168.1.8/newsServiceHM/beauty.mp4";
    //private String path2 = "http://192.168.1.8/newsServiceHM/ColorCop.exe";
    private Map<Integer, ProgressBar> map = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mContext = this;

        initViews();
        setListeners();
    }

    private void initViews(){
        etFilePath = (EditText) findViewById(R.id.et_filepath);
        btnFileUpload = (Button) findViewById(R.id.btn_upload);

        etThreadCount = (EditText) findViewById(R.id.et_thread_count);
        btnDownload = (Button) findViewById(R.id.btn_download);
        btnDeleteSp = (Button) findViewById(R.id.btn_delete_sp);
        llProgressLayout = (LinearLayout) findViewById(R.id.ll_progress_layout);

        etFileDownloadPathXUtils = (EditText) findViewById(R.id.et_file_download_path_xutils);
        btnDownloadXUtils = (Button) findViewById(R.id.btn_download_xutils);
    }

    private void setListeners(){
        btnFileUpload.setOnClickListener(this);
        btnDownload.setOnClickListener(this);
        btnDeleteSp.setOnClickListener(this);
        btnDownloadXUtils.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.btn_upload:
                fileUpload();
                break;
            case R.id.btn_download:
                fileDownload();
                break;
            case R.id.btn_delete_sp:
                SharedUtils.deleteSharedPreferences(mContext);
                Toast.makeText(mContext, "清除sp成功", Toast.LENGTH_SHORT).show();
                break;
            case R.id.btn_download_xutils:
                fileDownloadByXUtils();
                break;
            default:
                break;
        }
    }

    private void fileUpload(){
        try {
            //获取输入的文件地址
            String filePath = etFilePath.getText().toString().trim();
            Log.i("Tag", filePath + "--" + Environment.getExternalStorageDirectory());
            //使用开源的Utils做上传操作
            AsyncHttpClient asyncHttpClient = new AsyncHttpClient();
            //URL:请求服务器的url
            String url = "http://192.168.1.8/newsServiceHM/servlet/UploaderServlet";
            RequestParams params = new RequestParams();
            params.put("filename", new File(filePath));

            Log.i("Tag", filePath);
            asyncHttpClient.post(url, params, new AsyncHttpResponseHandler() {

                @Override
                public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                    if(statusCode == 200){
                        Toast.makeText(MainActivity.this, "上传成功", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable throwable) {
                    Toast.makeText(MainActivity.this, "上传失败", Toast.LENGTH_SHORT).show();
                }
            });

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }


    private void fileDownload(){
        //获取用户输入的线程数
        String threadCountStr = etThreadCount.getText().toString().trim();
        threadCount = Integer.parseInt(threadCountStr);

        //清空控件中的所有子控件
        llProgressLayout.removeAllViews();
        //根据线程数添加相应数量的ProgressBar
        for(int i = 0; i < threadCount; i++){
            ProgressBar progressBar = (ProgressBar) View.inflate(mContext, R.layout.child_progressbar_layout, null);
            map.put(i, progressBar); //将progressBar放入Map中，方便在线程中获取并设置进度
            llProgressLayout.addView(progressBar);
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                startDownload();
            }
        }).start();
    }

    private void startDownload(){
        try {
            // 1.请求url地址获取服务器端资源的大小
            URL url = new URL(path);
            HttpURLConnection openConnection = (HttpURLConnection) url.openConnection();
            openConnection.setRequestMethod("GET");
            openConnection.setConnectTimeout(10 * 1000);

            int code = openConnection.getResponseCode();
            if (code == 200) {
                // 获取资源的大小
                int fileLength = openConnection.getContentLength();
                //2.在本地创建一个与服务器资源同样大小的文件（占位）
                RandomAccessFile randomAccessFile = new RandomAccessFile(
                        new File(getFileName(path)), "rw");
                randomAccessFile.setLength(fileLength); //设置随机访问文件的大小

                //3.分配每个线程下载文件的开始位置和结束位置
                blockSize = fileLength / threadCount;
                for(int threadId = 0; threadId < threadCount; threadId++){
                    int startIndex = threadId * blockSize; //计算每个线程下载的开始位置
                    int endIndex = (threadId + 1) * blockSize - 1; //计算每个线程下载的结束位置

                    //如果是最后一个线程，结束位置要单独计算
                    if(threadId == threadCount - 1){
                        endIndex = fileLength - 1;
                    }

                    //4.开启线程去执行下载
                    new DownloadThread(threadId, startIndex, endIndex).start();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public class DownloadThread extends Thread {
        private int threadId;
        private int startIndex;
        private int endIndex;
        private int lastPosition;
        private int currentTreadTotalProgress;

        public DownloadThread(int threadId, int startIndex, int endIndex) {
            this.threadId = threadId;
            this.startIndex = startIndex;
            this.endIndex = endIndex;
            this.currentTreadTotalProgress = endIndex - startIndex + 1;
        }

        @Override
        public void run() {
            //获取当前线程对应的progressBar
            ProgressBar progressBar = map.get(threadId);

            synchronized (DownloadThread.class) {
                runningThreadCount++; //开启一线程，线程数加1
            }
            //分段请求网络连接，分段保存文件到本地
            try {
                URL url = new URL(path);
                HttpURLConnection openConnection = (HttpURLConnection) url.openConnection();
                openConnection.setRequestMethod("GET");
                openConnection.setConnectTimeout(10 * 1000);

                System.out.println("理论下载--线程" + threadId + " 开始位置：" + startIndex + " 结束位置：" + endIndex);

                if(SharedUtils.getLastPosition(mContext, threadId) > 0){ //表示未下载完
               /* //读取上次下载结束的位置,本次从这个位置开始直接下载
                File file = new File(getFilePath() + threadId + ".txt");
                if (file.exists()) {
                     //读取文件获取上次下载的位置
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
                    String lastPositionStr = bufferedReader.readLine();
                    lastPosition = Integer.parseInt(lastPositionStr); */
                    //从sp中读取
                    lastPosition = SharedUtils.getLastPosition(mContext, threadId);

                    //设置分段下载的头信息，Range:做分段数据请求用的   bytes:0-500请求服务器资源中0-500之间的字节信息
                    openConnection.setRequestProperty("Range", "bytes:" + lastPosition + "-" + endIndex);
                    System.out.println("实际下载--线程" + threadId + " 开始位置：" + startIndex + " 结束位置：" + endIndex);
                    //bufferedReader.close();
                //} else {
                } else if(SharedUtils.getLastPosition(mContext, threadId) == -1){ //表示未开始下载
                    lastPosition = startIndex;
                    //设置分段下载的头信息，Range:做分段数据请求用的   bytes:0-500请求服务器资源中0-500之间的字节信息
                    openConnection.setRequestProperty("Range", "bytes:" + lastPosition + "-" + endIndex);
                    System.out.println("实际下载--线程" + threadId + " 开始位置：" + startIndex + " 结束位置：" + endIndex);
                } else{ //-2 表示已经下载完了
                    //progressBar.setMax(currentTreadTotalProgress);
                    progressBar.setProgress(currentTreadTotalProgress);

                    synchronized (DownloadThread.class) {
                        runningThreadCount--;
                        if(runningThreadCount == 0) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(mContext, "已经下载完毕了", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    }
                    return;
                }


                //200:请求全部资源成功       206：请求部分资源成功
                if (openConnection.getResponseCode() == 206) {
                    //请求成功后将流写入本地文件中：已经创建的占位的那个文件
                    InputStream inputStream = openConnection.getInputStream();
                    RandomAccessFile randomAccessFile = new RandomAccessFile(new File(getFileName(path)), "rw");
                    randomAccessFile.seek(lastPosition); //设置随机文件从哪个位置开始写
                    //将流中的数据写入文件
                    byte[] buffer = new byte[1024 * 10];
                    int length = -1;
                    int total = 0; //记录本次线程下载的总大小
                    while ((length = inputStream.read(buffer)) != -1) {
                        randomAccessFile.write(buffer, 0, length);

                        total = total + length;
                        //保存当前线程下载的位置，保存到文件中
                        int currentThreadPosition = lastPosition + total; //计算出当前线程本次下载的位置
                      /*//创建随机文件保存当前线程下载的位置
                        File file2 = new File(getFilePath() + threadId + ".txt");
                        RandomAccessFile accessFile = new RandomAccessFile(file2, "rwd"); //直接写到硬盘上
                        accessFile.write(String.valueOf(currentThreadPosition).getBytes());
                        accessFile.close();*/
                      //保存到sp中
                        SharedUtils.setLastPosition(mContext, threadId, currentThreadPosition);

                        int currentProgress = currentThreadPosition - startIndex;
                        progressBar.setMax(currentTreadTotalProgress);
                        progressBar.setProgress(currentProgress);
                    }
                    //关闭相关的流信息
                    inputStream.close();
                    randomAccessFile.close();

                    System.out.println("线程" + threadId + " 下载完毕");
                    //当所有线程下载结束，删除存放下载位置的文件
                    synchronized (DownloadThread.class) {
                        runningThreadCount--; //标志着一个线程下载结束
                        if (runningThreadCount == 0) {

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(mContext, "文件下载完毕", Toast.LENGTH_SHORT).show();
                                }
                            });

                           /* for (int i = 0; i < threadCount; i++) {
                                File tempFile = new File(getFilePath() + i + ".txt");
                                tempFile.delete();
                            }*/

                            for (int i = 0; i < threadCount; i++) {
                                SharedUtils.deleteLastPosition(mContext, i);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            super.run();
        }
    }

    public String getFileName(String url){
        String fileName = Environment.getExternalStorageDirectory() + File.separator + "aapic"
                + File.separator + url.substring(url.lastIndexOf("/") + 1);
        return fileName;
    }

    public String getFilePath(){
        return Environment.getExternalStorageDirectory() + File.separator + "aapic" + File.separator;
    }


    private void fileDownloadByXUtils(){
        String downloadUrl = etFileDownloadPathXUtils.getText().toString().trim();
        String downloadDir = getFilePath() + "ColorCop.exe";
        Log.i("Tag", downloadUrl + "\n" + downloadDir);
        if(!TextUtils.isEmpty(downloadUrl)){
            //1.创建httpUtils对象
            HttpUtils httpUtils = new HttpUtils();
            //2.调用download方法    url:下载的地址   target:下载的目录    callback:回调
            httpUtils.download(downloadUrl, downloadDir, new RequestCallBack<File>() {
                @Override
                public void onLoading(long total, long current, boolean isUploading) {
                    Toast.makeText(mContext, "total:" + total + "---current:" + current, Toast.LENGTH_SHORT).show();
                    super.onLoading(total, current, isUploading);
                }

                @Override
                public void onSuccess(ResponseInfo<File> responseInfo) {
                    Toast.makeText(mContext, "下载成功", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onFailure(HttpException error, String msg) {
                    Toast.makeText(mContext, "下载失败:" + msg, Toast.LENGTH_SHORT).show();
                    Log.i("Tag", msg);
                }
            });
        }
    }
}
