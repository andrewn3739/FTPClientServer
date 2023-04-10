package webserver; /**
 * Assignment 1
 * Andrew Naughton
 **/
import java.io.* ;
import java.net.* ;
import java.util.* ;
public final class WebServer
{
    public static void main(String argv[]) throws Exception
    {
        // set the port number
        int port = 6789;

        // establish the listen socket
        ServerSocket sock = new ServerSocket(port);

        // process incoming HTTP messages
        while(true){
            // listen for TCP connection request
            Socket connection = sock.accept();
            HttpRequest request = new HttpRequest(connection);
            Thread thread = new Thread(request);
            thread.start();
        }
    }
}
final class HttpRequest implements Runnable
{
    final static String CRLF = "\r\n";
    Socket socket;
    String USER;
    String PASS;

    // Constructor
    public HttpRequest(Socket socket){
        this.socket = socket;
    }

    // implement Run command of Runnable
    public void run(){
        try{
            processRequest();
        } catch (Exception e){
            System.out.println(e);
        }
    }

    private static void sendBytes(FileInputStream fis, OutputStream os) throws Exception {
        // construct a 1k buffer to hold bytes on their way to the socket
        byte [] buffer = new byte[1024];
        int bytes = 0;

        // copy requested file into the socket's output stream
        while((bytes = fis.read(buffer)) != -1){
            os.write(buffer, 0, bytes);
        }
    }

    private static String contentType(String filename){
        if(filename.endsWith(".htm") || filename.endsWith(".html")){
            return "text/html";
        }
        if(filename.endsWith(".gif")){
            return "image/gif";
        }
        if(filename.endsWith(".jpeg") || filename.endsWith(".jpg")){
            return "image/JPEG";
        }
        if(filename.endsWith(".pdf")){
            return "application/pdf";
        }
        if(filename.endsWith(".txt")){
            return "text/plain";
        }
        return "application/octet-stream";
    }

    private void processRequest() throws Exception{
        // get a reference to the socket's input and output streams
        InputStream is = this.socket.getInputStream();
        DataOutputStream os = new DataOutputStream(this.socket.getOutputStream());

        // set up input stream filters
        BufferedReader br = new BufferedReader(new InputStreamReader(is));

        // get the request line of HTTP message
        String requestLine = br.readLine();

        //display the request line
        System.out.println();
        System.out.println("Request: \n" + requestLine);

        // get the header lines
        String headerLine = null;
        while((headerLine = br.readLine()).length() != 0){
            System.out.println(headerLine);
        }

        // extract the filenames from the GET request message
        StringTokenizer tokens = new StringTokenizer(requestLine);
        tokens.nextToken(); // since we assume the only message will be a GET, skip this field
        String filename = tokens.nextToken();

        // add a '.' to the beginning of the filename
        filename = "." + filename;

        if(contentType(filename).equalsIgnoreCase("text/plain")){
            // ask for credentials
            Scanner scan = new Scanner(System.in);
            System.out.println("enter your username: \n\t");
            USER = scan.nextLine();
            System.out.println("enter your password; \n\t");
            PASS = scan.nextLine();
        }

        // open requested file
        FileInputStream fis = null;
        boolean fileExists = true;
        try{
            fis = new FileInputStream(filename);
        } catch (FileNotFoundException e){
            fileExists = false;
        }

        // construct the response message
        String statusLine = null;
        String contentTypeLine = null;
        String entityBody = null;

        if(fileExists) {
            statusLine = "HTTP/1.0 200 OK" + CRLF;
            contentTypeLine = "Content-type: " + contentType(filename) + CRLF;
        }
        else{
            if (!contentType(filename).equalsIgnoreCase("text/plain")) {
                statusLine = "HTTP/1.0 404 Not Found " + CRLF;
                contentTypeLine = "Content-type: Unavailable " + CRLF;
                entityBody = "<HTML>" +
                        "<HEAD><TITLE>Not Found</TITLE></HEAD>" +
                        "<BODY>Not Found</BODY></HTML>";
            } else { // else retrieve the text (.txt) file from your local FTP server

                // create an instance of ftp client
                FtpClient client = new FtpClient();

                // connect to the ftp server
                client.connect(USER, PASS);

                // retrieve the file from the ftp server, remember you need to
                // first upload this file to the ftp server under your user
                // ftp directory
                client.getFile(filename);

                // disconnect from ftp server
                client.disconnect();

                statusLine = "HTTP/1.0 200 OK " + CRLF;
                contentTypeLine = "Content-type: " + contentType(filename) + CRLF;

                // assign input stream to read the recently ftp-downloaded file
                fis = new FileInputStream(filename);
            }
        }

        // print to terminal
        System.out.println("\nResponse: \nStatus Line: " +  statusLine + "Content Type Line: " + contentTypeLine);

        // send the status line
        os.writeBytes(statusLine);

        // send the content type line
        os.writeBytes(contentTypeLine);

        // send a blank line to indicate the end of the header lines
        os.writeBytes(CRLF);

        // Send the entity body.
        if (fileExists) {
            sendBytes(fis, os);
            fis.close();
        } else {
            if (!contentType(filename).equalsIgnoreCase("text/plain")) {
                os.writeBytes(entityBody);
            } else {
                sendBytes(fis, os);
            }
        }

        // close the streams and sockets
        os.close();
        br.close();
        socket.close();
    }
}