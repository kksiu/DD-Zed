import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONObject;


import java.io.*;
import java.net.URI;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


public class Main {

    static int number;
    static String url = "http://kksiu.com:3000/DD/state";
    static String write_state = "/sys/kernel/ece453_dd/write_state";
    static String read_state = "/sys/kernel/ece453_dd/read_state";
    static BufferedWriter output;
    static File fileWrite;
    static BufferedReader input;
    static File fileRead;

    //write to file
    public static void main(String[] args) {

        //init files
        fileWrite = new File(write_state);
        fileRead = new File(read_state);

        //init buffered
        try {
            output = new BufferedWriter(new FileWriter(fileWrite));
            input = new BufferedReader(new FileReader(fileRead));
        } catch (Exception e) {
            System.out.println("Can't get files " + e.toString());
        }


        //get a new thread that gets state and writes it to zed board
        new Thread(new Runnable() {
            @Override
            public void run() {
                while(true) {
                    boolean returned = getState();
                    if(returned) {
                        setZedState(number);
                    }
                    try {
                        Thread.sleep(2000);
                    } catch(Exception e) {
                        System.out.println("Thread 2 sleep failed " + e.toString());
                    }

                }
            }
        });

        //now that new thread is constantly getting the state, get the state of zedboard and use that ot set the state

        while(true) {
            int currZedState = Integer.parseInt(getZedState());
            boolean returned = setState(currZedState);

            try {
                Thread.sleep(2000);
            } catch (Exception e) {
                System.out.println("Thread 1 sleep failed " + e.toString());
            }

        }

    }

    public static void setZedState(int num) {
        String zedNum = "" + num;

        try {
            output.write(zedNum);
            output.flush();
        } catch ( IOException e ) {
            e.printStackTrace();
        }
    }

    public static String getZedState() {

        String zedNum = null;

        try {
            zedNum = input.readLine();
        } catch(Exception e) {
            System.out.println("Exception: " + e);
        }

        return zedNum;
    }

    public static boolean getState() {
        JSONObject obj = webAPI(url + "/get");

        boolean returned = false;

        try {
            number = obj.getInt("state");
            returned = true;
        } catch (Exception e) {
            System.out.println("Exception: " + e.toString());
        }

        return returned;
    }

    public static boolean setState(int num) {
        JSONObject obj = webAPI(url + "/set/" + num);

        boolean returned = false;

        try {
           returned =  obj.getBoolean("success");
        } catch (Exception e) {
            System.out.println("Exception: " + e.toString());
        }

        return returned;
    }

    public static JSONObject webAPI(String websiteURL) {

        JSONObject obj = null;

        try{
            HttpClient client = new DefaultHttpClient();
            HttpGet get = new HttpGet();
            URI website = new URI(websiteURL);
            get.setURI(website);
            HttpResponse response = client.execute(get);
            BufferedReader in = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
            StringBuilder builder = new StringBuilder();
            String ss = "";

            while((ss = in.readLine()) != null) {
                builder.append(ss);
            }

            obj = new JSONObject(builder.toString());

        }catch (Exception e){
            System.out.println("Exception: " + e.toString());
        }

        return obj;
    }
}
