package com.android305.lights;

import com.android305.lights.util.EncryptionUtils;
import com.android305.lights.util.Log;
import com.android305.lights.util.encryption.Encryption;
import com.android305.lights.util.schedule.TimerScheduler;

import org.apache.commons.io.output.TeeOutputStream;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.codec.textline.TextLineCodecFactory;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;
import org.apache.mina.util.Base64;
import org.jasypt.util.text.BasicTextEncryptor;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.net.BindException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Properties;
import java.util.Scanner;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

public class Server {

    public static boolean DEMO = false;

    private static final int DEFAULT_PORT = 7123;
    private static SecureRandom random = new SecureRandom();
    static Encryption enc;
    static Server server;

    public static void main(String[] args) {
        loadArgs(args);
        int port = -1;
        String sKey = "";
        String password = "";
        String host = "";
        Properties prop = new Properties();
        InputStream input = null;
        try {
            File file = new File("config.properties");
            if (!file.exists()) {
                Log.w("Unable to find " + file + "; Generating...");
                try {
                    KeyGenerator kg = KeyGenerator.getInstance("AES");
                    kg.init(128);
                    SecretKey k = kg.generateKey();
                    genConfigFile(InetAddress.getLocalHost().getHostAddress(), DEFAULT_PORT, EncryptionUtils.toHex(k.getEncoded()), randomPwd());
                } catch (NoSuchAlgorithmException e) {
                    Log.e(e);
                }
                if (!file.exists()) {
                    Log.e("Still can't find " + file + "; Exiting...");
                    System.exit(1);
                }
            }
            input = new FileInputStream(file);
            prop.load(input);
            port = Integer.parseInt(prop.getProperty("port"));
            host = prop.getProperty("host");
            sKey = prop.getProperty("secret_key");
            password = prop.getProperty("password");
        } catch (IOException ex) {
            Log.e(ex);
            System.exit(1);
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    Log.e(e);
                }
            }
        }
        enc = new Encryption(sKey);
        try {
            start(host, port, password, sKey);
        } catch (IOException e) {
            Log.e(e);
        }
    }

    private static void loadArgs(String[] args) {
        Log.DEBUG = false;
        Log.VERBOSE = false;
        if (args.length > 0) {
            try {
                for (String arg : args) {
                    switch (arg) {
                        case "-d":
                        case "-debug":
                            Log.DEBUG = true;
                            break;
                        case "-v":
                        case "-verbose":
                            Log.VERBOSE = true;
                            break;
                        case "-log":
                        case "-l":
                            try {
                                String fileName = new SimpleDateFormat("yyyy-MM-dd- hh-mm-ss'.txt'", Locale.US).format(new Date());
                                File f = new File("logs/" + fileName);
                                if (f.getParentFile().mkdir() || f.getParentFile().exists()) {
                                    FileOutputStream fos = new FileOutputStream(f);
                                    TeeOutputStream myOut = new TeeOutputStream(System.out, fos);
                                    PrintStream ps = new PrintStream(myOut);
                                    System.setOut(ps);
                                    System.setErr(ps);
                                } else {
                                    Log.w("Could not create log directory, falling back to System.in");
                                }
                            } catch (Exception e) {
                                Log.e(e);
                            }
                            break;
                        case "-test":
                            DEMO = true;
                            com.android305.lights.util.sqlite.SQLConnection.insertTestData();
                            break;
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException("Usage: [-port|-p #] [-debug|-d] [-verbose|-v] [-log|-l]");
            }
        }
    }

    public static String randomPwd() {
        return new BigInteger(130, random).toString(32);
    }

    private static void genConfigFile(String host, int port, String sKey, String password) {
        Properties prop = new Properties();
        OutputStream output = null;
        try {
            output = new FileOutputStream("config.properties");
            prop.setProperty("host", host);
            prop.setProperty("port", Integer.toString(port));
            prop.setProperty("secret_key", sKey);
            prop.setProperty("password", password);
            prop.store(output, null);
        } catch (IOException io) {
            Log.e(io);
        } finally {
            if (output != null) {
                try {
                    output.close();
                } catch (IOException e) {
                    Log.e(e);
                }
            }

        }
    }

    private static void start(String host, int port, String password, String sKey) throws IOException {
        if (port == DEFAULT_PORT) {
            Log.d("Using default port: " + port);
        } else {
            Log.d("Using custom port: " + port);
        }
        try {
            server = new Server(port, password);
        } catch (BindException e) {
            Log.i("Address in use. Please try again. If issue persists, please reboot your computer.");
            Log.e(e);
        } catch (Exception e) {
            Log.e("Unexpected error, program exiting... ", e);
            System.exit(1);
        }
        Scanner in = new Scanner(System.in);
        while (in.hasNext()) {
            String data = in.nextLine();
            switch (data.trim()) {
                case "stop":
                    if (server != null) {
                        server.closeConn();
                    } else {
                        Log.w("Server not started");
                    }
                    break;
                case "start":
                    try {
                        if (server != null && !server.isStopped()) {
                            Log.w("You must stop the server if you would like to start it again.");
                        } else
                            server = new Server(port, password);
                    } catch (BindException e) {
                        Log.w("Address in use. Please try again. If issue persists, please reboot your computer.");
                        Log.e(e);
                    } catch (Exception e) {
                        Log.e("Unexpected error, program exiting... ", e);
                        System.exit(1);
                    }
                    break;
                case "restart":
                    try {
                        if (server != null && !server.isStopped()) {
                            server.closeConn();
                        }
                        server = new Server(port, password);
                    } catch (BindException e) {
                        Log.w("Address in use. Please try again. If issue persists, please reboot your computer.");
                        Log.e(e);
                    } catch (Exception e) {
                        Log.e("Unexpected error, program exiting... ", e);
                        System.exit(1);
                    }
                    break;
                case "qr":
                    String qrData = String.format("%s:%d|%s|%s", host, port, password, sKey);
                    Log.v("QR data: " + qrData);
                    BasicTextEncryptor textEncryptor = new BasicTextEncryptor();
                    textEncryptor.setPassword("deadpoolisawesome");
                    qrData = textEncryptor.encrypt(qrData);
                    Log.v("Encrypted qr data: " + qrData);
                    qrData = new String(Base64.encodeBase64(qrData.getBytes()));
                    Log.v("Base 64 Encoded qr data: " + qrData);
                    String qr = "https://chart.googleapis.com/chart?cht=qr&chld=M|4&chs=547x547&chl=" + qrData;
                    Log.i(qr);
                    PrintWriter out = new PrintWriter("qr.txt");
                    out.write(qr);
                    out.close();

                    //TODO: add email qr url functionality
                    break;
                default:
                    Log.i("Unknown command");
                    break;
            }
        }
    }

    private NioSocketAcceptor acceptor;
    private TimerScheduler timer;

    public Server(int port, String password) throws IOException {
        Log.i("Starting server...");
        acceptor = new NioSocketAcceptor();
        TextLineCodecFactory factory = new TextLineCodecFactory(Charset.forName("UTF-8"));
        factory.setEncoderMaxLineLength(8192);
        factory.setDecoderMaxLineLength(8192);
        acceptor.getFilterChain().addLast("codec", new ProtocolCodecFilter(factory));
        ServerHandler handler = new ServerHandler(password);
        acceptor.setHandler(handler);
        acceptor.getSessionConfig().setReadBufferSize(8192);
        acceptor.getSessionConfig().setIdleTime(IdleStatus.BOTH_IDLE, 1);
        acceptor.bind(new InetSocketAddress(port));
        Log.i("Server started.");
        timer = new TimerScheduler();
    }

    private void closeConn() {
        Log.i("Stopping server...");
        if (acceptor != null && acceptor.isActive()) {
            acceptor.dispose();
        }
        if (timer.isRunning()) {
            timer.stop();
        }
        Log.i("Server stopped");
    }

    public TimerScheduler getTimer() {
        return timer;
    }

    private boolean isStopped() {
        return acceptor == null || acceptor.isDisposed() || acceptor.isDisposing();
    }
}
