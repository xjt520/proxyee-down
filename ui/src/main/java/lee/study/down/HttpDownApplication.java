package lee.study.down;

import javax.swing.JOptionPane;
import lee.study.down.constant.HttpDownConstant;
import lee.study.down.content.ContentManager;
import lee.study.down.gui.HttpDownTray;
import lee.study.down.intercept.HttpDownHandleInterceptFactory;
import lee.study.down.task.HttpDownErrorCheckTask;
import lee.study.down.task.HttpDownProgressEventTask;
import lee.study.down.util.OsUtil;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.embedded.ConfigurableEmbeddedServletContainer;
import org.springframework.boot.context.embedded.EmbeddedServletContainerCustomizer;
import org.springframework.context.ApplicationContext;

@SpringBootApplication
public class HttpDownApplication implements InitializingBean, EmbeddedServletContainerCustomizer {

  static {
    //设置slf4j日志打印目录
    System.setProperty("LOG_PATH", HttpDownConstant.HOME_PATH);
  }

  public static void main(String[] args) throws Exception {

    //启动前端页面web server
    ApplicationContext context = new SpringApplicationBuilder(HttpDownApplication.class)
        .headless(false).build().run(args);
    //获取application实例
    HttpDownApplication application = context.getBean(HttpDownApplication.class);
    //上下文初始化
    ContentManager.init();
    //代理服务器启动
    int proxyPort = ContentManager.CONFIG.get().getProxyPort();
    if (OsUtil.isBusyPort(proxyPort)) {
      JOptionPane
          .showMessageDialog(null, "端口(" + proxyPort + ")被占用，请关闭占用端口的软件或设置新的端口号",
              "运行警告", JOptionPane.WARNING_MESSAGE);
    } else {
      application.proxyServer = new HttpDownProxyServer(
          ContentManager.CONFIG.get().getSecProxyConfig(),
          new HttpDownHandleInterceptFactory(application.viewServerPort));
      new Thread(() -> application.proxyServer.start(proxyPort)).start();
    }
    //托盘初始化
    new HttpDownTray(application.homeUrl).init();
    //打开浏览器访问前端页面
    OsUtil.openBrowse(application.homeUrl);
    //启动线程
    new HttpDownErrorCheckTask().start();
    new HttpDownProgressEventTask().start();
  }

  @Value("${spring.profiles.active}")
  private String active;

  @Value("${view.server.port}")
  private int viewServerPort;

  @Value("${tomcat.server.port}")
  private int tomcatServerPort;

  private HttpDownProxyServer proxyServer;
  private String homeUrl;

  public HttpDownProxyServer getProxyServer() {
    return proxyServer;
  }

  @Override
  public void afterPropertiesSet() throws Exception {
    if (!"dev".equalsIgnoreCase(active.trim())) {
      viewServerPort = tomcatServerPort = OsUtil.getFreePort(tomcatServerPort);
    }
    homeUrl = "http://127.0.0.1:" + viewServerPort;
  }

  @Override
  public void customize(ConfigurableEmbeddedServletContainer container) {
    container.setPort(tomcatServerPort);
  }

}
