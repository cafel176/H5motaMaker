package com.ckcz123.h5mota.maker;

import android.app.DownloadManager;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.webkit.ConsoleMessage;
import android.webkit.DownloadListener;
import android.webkit.JsPromptResult;
import android.webkit.JsResult;
import android.webkit.SslErrorHandler;
import android.webkit.URLUtil;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.ckcz123.h5mota.maker.lib.CustomToast;

import org.apache.commons.io.IOUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Created by castor_v_pollux on 2018/12/30.
 */

public class WebActivity extends AppCompatActivity {

    WebView webView;
    ProgressBar progressBar;

    private ValueCallback<Uri> mUploadMessage;
    public ValueCallback<Uri[]> uploadMessage;
    public static final int REQUEST_SELECT_FILE = 100;
    public static final int JSINTERFACE_SELECT_FILE = 200;
    private final static int FILECHOOSER_RESULTCODE = 2;

    private File LOG_FILE;
    private SimpleDateFormat simpleDateFormat;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web);

        LOG_FILE = new File(Environment.getExternalStorageDirectory()+"/H5motaMaker/", "log.txt");
        simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

        setTitle(getIntent().getStringExtra("title"));

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,  WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED);

        webView = findViewById(R.id.webview);
        progressBar = findViewById(R.id.progressBar);
        progressBar.setVisibility(View.VISIBLE);

        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
        webSettings.setSupportZoom(true);
        webSettings.setUseWideViewPort(true);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.SINGLE_COLUMN);
        webView.setScrollBarStyle(View.SCROLLBARS_OUTSIDE_INSET);
        webSettings.setAllowContentAccess(true);
        webSettings.setDefaultTextEncodingName("utf-8");
        webSettings.setDomStorageEnabled(true);
        webSettings.setMediaPlaybackRequiresUserGesture(false);
        webSettings.setDatabaseEnabled(true);
        webSettings.setRenderPriority(WebSettings.RenderPriority.HIGH);
        webSettings.setLoadsImagesAutomatically(true);
        webSettings.setAppCacheEnabled(true);

        webView.addJavascriptInterface(new JSInterface(this), "jsinterface");

        webView.setWebViewClient(new WebViewClient() {
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                webView.loadUrl(url);
                return true;
            }

            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                progressBar.setVisibility(View.VISIBLE);
            }

            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                progressBar.setVisibility(View.GONE);
                setTitle(webView.getTitle());
            }

            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                handler.proceed();
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            public boolean onJsAlert(WebView view, String url, String message, final JsResult result) {
                new AlertDialog.Builder(WebActivity.this)
                        .setTitle("JsAlert")
                        .setMessage(message)
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                result.confirm();
                            }
                        })
                        .setCancelable(false)
                        .show();
                return true;
            }

            public boolean onJsConfirm(WebView view, String url, String message, final JsResult result) {
                new AlertDialog.Builder(WebActivity.this)
                        .setTitle("Javascript发来的提示")
                        .setMessage(message)
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                result.confirm();
                            }
                        })
                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                result.cancel();
                            }
                        })
                        .setCancelable(false)
                        .show();
                return true;
            }

            public boolean onJsPrompt(WebView view, String url, String message, String defaultValue, final JsPromptResult result) {
                final EditText et = new EditText(WebActivity.this);
                et.setText(defaultValue);
                new AlertDialog.Builder(WebActivity.this)
                        .setTitle(message)
                        .setView(et)
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                result.confirm(et.getText().toString());
                            }
                        })
                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                result.cancel();
                            }
                        })
                        .setCancelable(false)
                        .show();

                return true;
            }

            public void openFileChooser(ValueCallback<Uri> uploadMsg) {
                mUploadMessage = uploadMsg;
                Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                i.addCategory(Intent.CATEGORY_OPENABLE);
                i.setType("*/*");
                startActivityForResult(i, FILECHOOSER_RESULTCODE);
            }

            public void openFileChooser(ValueCallback<Uri> uploadMsg, String acceptType, String capture) {
                openFileChooser(uploadMsg);
            }

            protected void openFileChooser(ValueCallback uploadMsg, String acceptType) {
                openFileChooser(uploadMsg);
            }

            public boolean onShowFileChooser(WebView mWebView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
                if (uploadMessage != null) {
                    uploadMessage.onReceiveValue(null);
                    uploadMessage = null;
                }
                uploadMessage = filePathCallback;
                Intent intent = fileChooserParams.createIntent();
                try {
                    startActivityForResult(intent, REQUEST_SELECT_FILE);
                } catch (ActivityNotFoundException e) {
                    uploadMessage = null;
                    return false;
                }
                return true;
            }

            public void onProgressChanged(WebView view, int progress) {
                progressBar.setProgress(progress);
            }

            public boolean onConsoleMessage(ConsoleMessage message) {
                String msg = message.message();
                if (msg.equals("[object Object]") || msg.equals("localForage supported!") || msg.equals("插件编写测试") || msg.equals("开始游戏"))
                    return false;
                ConsoleMessage.MessageLevel level = message.messageLevel();
                if (level != ConsoleMessage.MessageLevel.LOG && level != ConsoleMessage.MessageLevel.ERROR)
                    return false;
                try (PrintWriter printWriter = new PrintWriter(new FileWriter(LOG_FILE, true))){
                    String s = String.format("[%s] {%s, Line %s, Source %s} %s\r\n", simpleDateFormat.format(new Date()),
                            level.toString(), message.lineNumber(), message.sourceId(), msg);
                    printWriter.write(s);
                }
                catch (Exception e) {
                    Log.i("Console", "error", e);
                }
                return false;
            }
        });
        webView.setDownloadListener(new DownloadListener() {
            public void onDownloadStart(String url, String userAgent,
                                        String contentDisposition, String mimetype, long contentLength) {
                try {
                    DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
                    Log.i("mimetype", mimetype);

                    request.setMimeType(mimetype);
                    request.allowScanningByMediaScanner();
                    request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                    String filename = URLUtil.guessFileName(url, contentDisposition, mimetype);
                    new File(Environment.getExternalStorageDirectory() + "/H5motaMaker/").mkdirs();
                    File file = new File(Environment.getExternalStorageDirectory() + "/H5motaMaker/" + filename);
                    if (file.exists()) file.delete();
                    request.setDestinationUri(Uri.fromFile(file));
                    request.setTitle("正在下载" + filename + "...");
                    request.setDescription("文件保存在" + file.getAbsolutePath());
                    DownloadManager downloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
                    downloadManager.enqueue(request);

                    CustomToast.showInfoToast(WebActivity.this, "文件下载中，请在通知栏查看进度");
                } catch (Exception e) {
                    if (url.startsWith("blob")) {
                        CustomToast.showErrorToast(WebActivity.this, "无法下载文件！");
                        return;
                    }
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                }
            }
        });
        webView.loadUrl(getIntent().getStringExtra("url"));
    }

    double exittime=0;
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode==KeyEvent.KEYCODE_BACK) {
            if (webView.canGoBack()) {
                webView.goBack();
            }
            else {
                if (System.currentTimeMillis()-exittime>2000) {
                    Toast.makeText(this, "再按一遍返回主页面", Toast.LENGTH_SHORT).show();
                    exittime=System.currentTimeMillis();
                }
                else
                {
                    exittime=0;
                    webView.loadUrl("about:blank");
                    finish();
                }
            }
            return true;
        }
        return false;
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if (resultCode == RESULT_OK) {
            Uri result = intent == null ? null : intent.getData();
            switch (requestCode) {
                case REQUEST_SELECT_FILE:
                    if (uploadMessage == null)
                        return;
                    uploadMessage.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(resultCode, intent));
                    uploadMessage = null;
                    return;
                case FILECHOOSER_RESULTCODE:
                    if (null == mUploadMessage) return;
                    mUploadMessage.onReceiveValue(result);
                    mUploadMessage = null;
                    break;
                case JSINTERFACE_SELECT_FILE:
                    if (result == null) break;
                    Log.i("Path", result.getPath());
                    ContentResolver contentResolver = getContentResolver();
                    String type = contentResolver.getType(result);
                    Log.i("Type", type);
                    if (type!=null && type.startsWith("image")) {
                        try (InputStream inputStream = getContentResolver().openInputStream(result)) {
                            byte[] bytes = IOUtils.toByteArray(inputStream);
                            String base64 = Base64.encodeToString(bytes, Base64.DEFAULT);
                            webView.loadUrl("javascript:core.readFileContent('data:"+type+";base64," + base64 +"')");
                        }
                        catch (Exception e) {
                            CustomToast.showErrorToast(this, "读取失败！");
                            e.printStackTrace();
                        }
                        return;
                    }
                    try (InputStream inputStream = getContentResolver().openInputStream(result);
                         BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                        String line;
                        StringBuilder builder = new StringBuilder();
                        while ((line=reader.readLine())!=null) builder.append(line);
                        webView.loadUrl("javascript:core.readFileContent('" + builder.toString().replace('\'', '\"') +"')");
                    }
                    catch (Exception e) {
                        CustomToast.showErrorToast(this, "读取失败！");
                        e.printStackTrace();
                    }
                    break;
            }
        }
    }

    public void onDestroy() {
        webView.destroy();
        super.onDestroy();
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        return super.onCreateOptionsMenu(menu);
    }

    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.clear();
        menu.add(Menu.NONE, 0, 0, "").setIcon(android.R.drawable.ic_menu_rotate).setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_ALWAYS);
        menu.add(Menu.NONE, 1, 1, "").setIcon(android.R.drawable.ic_menu_help).setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_ALWAYS);
        menu.add(Menu.NONE, 2, 2, "").setIcon(android.R.drawable.ic_menu_close_clear_cancel).setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);
        switch (item.getItemId()) {
            case 0: webView.reload(); break;
            case 1: loadUrl("https://h5mota.com/games/template/docs/", "查看文档"); break;
            case 2: webView.loadUrl("about:blank");finish();break;
        }
        return true;
    }

    public void loadUrl(String url, String title) {
        try {
            Intent intent=new Intent(this, getClass());
            intent.setFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
            intent.putExtra("title", title);
            intent.putExtra("url", url);
            startActivity(intent);
        }
        catch (Exception e) {
            e.printStackTrace();
            CustomToast.showErrorToast(this, "无法打开网页！");
        }
    }

}
