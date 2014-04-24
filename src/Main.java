import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONObject;


import java.io.*;
import java.net.URI;


public class Main {

    static int zedNumber;
    static long time;
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
        time = System.currentTimeMillis();

        //init buffered
        try {
            output = new BufferedWriter(new FileWriter(fileWrite));
        } catch (Exception e) {
            System.out.println("Can't get files " + e.toString());
        }

        //now that new thread is constantly getting the state, get the state of zedboard and use that ot set the state

        while(true) {
            int num = getZedState();
            if(zedNumber != num) {
                time = System.currentTimeMillis();
                zedNumber = num;
            }

            //try and get server time
            JSONObject obj = getState();

            try {
                long tempTime = obj.getLong("time");
                int tempNum = obj.getInt("state");

                if(tempTime > time) {
                    setZedState(tempNum);
                    zedNumber = tempNum;
                    time = tempTime;
                } else if ((time > tempTime) && (tempNum != zedNumber)) {
                    //update server with zedNumber
                    time = System.currentTimeMillis();
                    setState(zedNumber, time);
                }

            } catch(Exception e) {
                System.out.println("Json object is bad: " + e.toString());
            }

            try {
                Thread.sleep(1000);
            } catch(Exception e) {
                System.out.println("Sleep failed: " + e.toString());
            }



        }

    }

    public static void setZedState(int num) {
        String zedNum = "" + Math.pow(2, num);

        try {
            output.write(zedNum);
            output.flush();
        } catch ( IOException e ) {
            e.printStackTrace();
        }
    }


    public static int log2(double num)
    {
        return (int)(Math.log(num)/Math.log(2));
    }

    public static int getZedState() {

        int zedNum = 0;

        try {
            input = new BufferedReader(new FileReader(fileRead));
            zedNum = Integer.parseInt(input.readLine());
            input.close();
            zedNum = log2(zedNum);
        } catch(Exception e) {
            System.out.println("Exception: " + e);
        }

        return zedNum + 1;
    }

    public static JSONObject getState() {
        JSONObject obj = webAPI(url + "/get");

        return obj;
    }

    public static boolean setState(int num, long time) {
        JSONObject obj = webAPI(url + "/set/" + num + "/" + time);

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
