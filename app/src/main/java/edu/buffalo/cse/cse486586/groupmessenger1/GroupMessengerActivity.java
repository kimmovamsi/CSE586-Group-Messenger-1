package edu.buffalo.cse.cse486586.groupmessenger1;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemClock;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * GroupMessengerActivity is the main Activity for the assignment.
 * 
 * @author stevko
 *
 */
public class GroupMessengerActivity extends Activity {
    static final String TAG = GroupMessengerActivity.class.getSimpleName();
    private static final String KEY_FIELD = "key";
    private static final String VALUE_FIELD = "value";
    ContentResolver mContentResolver;
    ContentValues cv = new ContentValues();
    Uri mUri = getmUri("content", "edu.buffalo.cse.cse486586.groupmessenger1.provider");

    public Uri getmUri(String scheme, String authority) {
        Uri.Builder uriBuilder = new Uri.Builder();
        uriBuilder.authority(authority);
        uriBuilder.scheme(scheme);
        return uriBuilder.build();
    }

    static final String remote_port[] = {"11108", "11112", "11116", "11120", "11124"};
    static final int SERVER_PORT = 10000;
    String sec = "null";
    FileOutputStream rollfo = null;
    FileInputStream rollfi = null;
    String serialfilename = "rollcall";



    public GroupMessengerActivity() {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        System.out.println(mUri);
        setContentView(R.layout.activity_group_messenger);
        mContentResolver = getContentResolver();
        File file = new File(serialfilename);
        if (!file.exists()) {
            Log.d("File mila?", "nai");
            try {
                rollfo = openFileOutput(serialfilename, Context.MODE_PRIVATE);
                rollfo.write(sec.getBytes());
                rollfo.close();
                sec = "-1";
            } catch (Exception e) {
                Log.e("no make rollfile", "File write failed");
            }
        }else try {
            Log.d("File mila?", "ha");
            rollfi = openFileInput(serialfilename);
            byte[] input = new byte[rollfi.available()];
            while (rollfi.read(input) != -1) {
                sec = new String(input);
                Log.d("sec?", sec);
                if (sec.equals("null")) {
                    sec = "-1";
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                rollfi.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


        TelephonyManager tel = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        final String myPort = String.valueOf((Integer.parseInt(portStr) * 2));
        try {
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "Can't create a ServerSocket");
            return;
        }

        final EditText editText = (EditText) findViewById(R.id.editText1);
        final Button button = (Button) findViewById(R.id.button4);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String msg = editText.getText().toString() + "\n";
                editText.setText(""); // This is one way to reset the input box.
                //TextView localTextView = (TextView) findViewById(R.id.textView1);
                //localTextView.append("\t\t\t"+msg); // This is one way to display a string.

                new ClientTask().execute(msg, myPort);
                        //AsyncTask.SERIAL_EXECUTOR, msg, myPort);

            }
        });

        /*
         * TODO: Use the TextView to display your messages. Though there is no grading component
         * on how you display the messages, if you implement it, it'll make your debugging easier.
         */
        TextView tv = (TextView) findViewById(R.id.textView1);
        tv.setMovementMethod(new ScrollingMovementMethod());

        /*
         * Registers OnPTestClickListener for "button1" in the layout, which is the "PTest" button.
         * OnPTestClickListener demonstrates how to access a ContentProvider.
         */
        findViewById(R.id.button1).setOnClickListener(
                new OnPTestClickListener(tv, getContentResolver()));

        /*
         * TODO: You need to register and implement an OnClickListener for the "Send" button.
         * In your implementation you need to get the message from the input box (EditText)
         * and send it to other AVDs.
         */
    }


        private class ServerTask extends AsyncTask<ServerSocket, String, Void> {

            @Override
            protected Void doInBackground(final ServerSocket... sockets) {
                final ServerSocket serverSocket = sockets[0];

                Socket socket;
                String msglere;
                //int ineedthistoo;

                try {
                    while (true) {
                        socket = serverSocket.accept();
                        DataInputStream in = new DataInputStream(new
                                BufferedInputStream(socket.getInputStream()));
                        msglere = in.readUTF();
                        SystemClock.sleep(10);
                        if (msglere != null) {
                            //FileOutputStream fos;
                            sec = Integer.toString(Integer.parseInt(sec) + 1);
                            //ineedthistoo = Integer.parseInt(sec);
                            //Log.d("ineedthistoo ka value", (Integer.toString(ineedthistoo)));
                            Log.d("(S)ki hai sec?", sec);
                            cv = new ContentValues();
                            cv.put(KEY_FIELD, sec);
                            cv.put(VALUE_FIELD, msglere);
                            mContentResolver.insert(mUri, cv);
                            try {
                                rollfo = openFileOutput(serialfilename, Context.MODE_PRIVATE);
                                rollfo.write(sec.getBytes());
                                rollfo.close();
                            } catch (Exception e) {
                                Log.e("server", "File write failed");
                            }

                            publishProgress(msglere);
                        }
                    }
                }catch (IOException e ) {
                    e.printStackTrace();
                }

                return null;
            }

            protected void onProgressUpdate(String...strings) {

                String strReceived = strings[0].trim();
                TextView localTextView = (TextView) findViewById(R.id.textView1);
                try {
                    localTextView.append(strReceived + "\n");
                } catch (Exception e) {
                    e.printStackTrace();
                }

                return;
            }
        }

        private class ClientTask extends AsyncTask<String, Void, Void> {

            Socket socket;
            DataOutputStream out;

            @Override
            protected Void doInBackground(String... msgs) {
                try {
                    String msgToSend = msgs[0];
                    for (int i = 0; i < 5; i++) {
                            socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                                    Integer.parseInt(remote_port[i]));
                            out = new DataOutputStream(new
                                    BufferedOutputStream(socket.getOutputStream()));
                            out.writeUTF(msgToSend);
                            SystemClock.sleep(10);
                            out.flush();
                            out.close();
                            socket.close();

                    }

                } catch (UnknownHostException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                return null;
            }
        }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_group_messenger, menu);
        return true;
    }
}
