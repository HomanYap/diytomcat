package cn.how2j.diytomcat;

import cn.how2j.diytomcat.http.Request;
import cn.how2j.diytomcat.http.Response;
import cn.how2j.diytomcat.util.Constant;
import cn.how2j.diytomcat.util.ThreadPoolUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.NetUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.ZipUtil;
import cn.hutool.log.LogFactory;
import cn.hutool.system.SystemUtil;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class Bootstrap {

    public static void main(String[] args) {
        try {
            logJVM();

            int port = 18080;

//            if(!NetUtil.isUsableLocalPort(port)) {
//                System.out.println(port +" 端口已经被占用了，排查并关闭本端口的办法请用：\r\nhttps://how2j.cn/k/tomcat/tomcat-portfix/545.html");
//                return;
//            }
            ServerSocket ss = new ServerSocket(port);

            while(true) {
                Socket s =  ss.accept();
                Runnable run=new Runnable() {
                    @Override
                    public void run() {
                        Request request = null;
                        try {
                            request = new Request(s);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                        System.out.println("浏览器的输入信息： \r\n" + request.getRequestString());
                        System.out.println("uri:" + request.getUri());

                        Response response = new Response();

                        String uri = request.getUri();
                        if(null==uri) {
                            return;
                        }
                        System.out.println(uri);
                        if("/".equals(uri)){
                            String html = "Hello DIY Tomcat from how2j.cn";
                            response.getWriter().println(html);
                        }
                        else{
                            String fileName = StrUtil.removePrefix(uri, "/");
                            File file = FileUtil.file(Constant.rootFolder,fileName);
                            if(file.exists()){
                                String fileContent = FileUtil.readUtf8String(file);
                                response.getWriter().println(fileContent);
                                if(fileName.equals("timeConsume.html")){
                                    ThreadUtil.sleep(1000);
                                }
                            }
                            else{
                                response.getWriter().println("File Not Found");
                            }
                        }
                        try {
                            handle200(s, response);
                        } catch (IOException e) {
                            LogFactory.get().error(e);
                            throw new RuntimeException(e);
                        }
                    }
                };
                ThreadPoolUtil.run(run);
            }
        } catch (IOException e) {
            LogFactory.get().error(e);
            e.printStackTrace();
        }

    }

    private static void logJVM() {
        Map<String,String> infos = new LinkedHashMap<>();
        infos.put("Server version", "Henry DiyTomcat/1.0.1");
        infos.put("Server built", "2022-06-15 10:20:22");
        infos.put("Server number", "1.0.1");
        infos.put("OS Name\t", SystemUtil.get("os.name"));
        infos.put("OS Version", SystemUtil.get("os.version"));
        infos.put("Architecture", SystemUtil.get("os.arch"));
        infos.put("Java Home", SystemUtil.get("java.home"));
        infos.put("JVM Version", SystemUtil.get("java.runtime.version"));
        infos.put("JVM Vendor", SystemUtil.get("java.vm.specification.vendor"));

        Set<String> keys = infos.keySet();
        for (String key : keys) {
            LogFactory.get().info(key+":\t\t" + infos.get(key));
        }
    }

    private static void handle200(Socket s, Response response) throws IOException {
        String contentType = response.getContentType();
        String headText = Constant.response_head_202;
        headText = StrUtil.format(headText, contentType);
        byte[] head = headText.getBytes();

        byte[] body = response.getBody();

        byte[] responseBytes = new byte[head.length + body.length];
        ArrayUtil.copy(head, 0, responseBytes, 0, head.length);
        ArrayUtil.copy(body, 0, responseBytes, head.length, body.length);

        OutputStream os = s.getOutputStream();
        os.write(responseBytes);
        s.close();
    }
}
