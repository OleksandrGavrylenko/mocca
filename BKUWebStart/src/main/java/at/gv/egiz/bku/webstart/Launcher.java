package at.gv.egiz.bku.webstart;

import at.gv.egiz.bku.webstart.gui.AboutDialog;
import at.gv.egiz.bku.webstart.gui.BKUControllerInterface;
import at.gv.egiz.bku.webstart.gui.PINManagementInvoker;
import iaik.asn1.CodingException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.ResourceBundle;

import javax.jnlp.UnavailableServiceException;

//import com.sun.javaws.security.JavaWebStartSecurity;
import java.awt.AWTException;
import java.awt.Desktop;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SplashScreen;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.net.BindException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.text.MessageFormat;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import javax.imageio.ImageIO;
import javax.jnlp.BasicService;
import javax.jnlp.ServiceManager;
import javax.swing.JFrame;
import org.mortbay.util.MultiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Launcher implements BKUControllerInterface, ActionListener {
  public static final String HELP_COMMAND = "help";

  public static final String WEBAPP_RESOURCE = "BKULocal.war";
  public static final String CERTIFICATES_RESOURCE = "BKUCertificates.jar";
  public static final String WEBAPP_FILE = "BKULocal.war";
  /** no leading slash for messages, but for image */
  public static final String MESSAGES_RESOURCE = "at/gv/egiz/bku/webstart/messages";
  public static final String TRAYICON_RESOURCE = "/at/gv/egiz/bku/webstart/chip";
  /** resource bundle messages */
  public static final String CAPTION_DEFAULT = "tray.caption.default";
  public static final String CAPTION_ERROR = "tray.caption.error";
  public static final String MESSAGE_START = "tray.message.start";
  public static final String MESSAGE_START_OFFLINE = "tray.message.start.offline";
  public static final String MESSAGE_CONFIG = "tray.message.config";
  public static final String MESSAGE_CERTS = "tray.message.certs";
  public static final String MESSAGE_FINISHED = "tray.message.finished";
  public static final String MESSAGE_SHUTDOWN = "tray.message.shutdown";
  public static final String ERROR_START = "tray.error.start";
  public static final String ERROR_CONFIG = "tray.error.config";
  public static final String ERROR_BIND = "tray.error.bind";
  public static final String ERROR_PIN = "tray.error.pin.connect";
  public static final String ERROR_OPEN_URL = "tray.error.open.url";
  public static final String LABEL_SHUTDOWN = "tray.label.shutdown";
  public static final String LABEL_PIN = "tray.label.pin";
  public static final String LABEL_HELP = "tray.label.help";
  public static final String LABEL_ABOUT = "tray.label.about";
  public static final String TOOLTIP_DEFAULT = "tray.tooltip.default";

  /** action commands for tray menu */
  public static final String SHUTDOWN_COMMAND = "shutdown";
  public static final String PIN_COMMAND = "pin";
  public static final String ABOUT_COMMAND = "about";
  
  private static Logger log = LoggerFactory.getLogger(Launcher.class);

  
  /** local bku uri */
  public static final URL HTTP_SECURITY_LAYER_URL;
  public static final URL HTTPS_SECURITY_LAYER_URL;
  public static final URL INSTALL_CERT_URL;
  public static final URL PIN_MANAGEMENT_URL;
  public static final URL HELP_URL;
  static {
    URL http = null;
    URL https = null;
    URL pin = null;
    URL cert = null;
    URL help = null;
    try {
      http = new URL("http://localhost:" + Integer.getInteger(Container.HTTPS_PORT_PROPERTY, 3495).intValue() + '/');
      https = new URL("https://localhost:" + Integer.getInteger(Container.HTTPS_PORT_PROPERTY, 3496).intValue() + '/');
      pin = new URL(http, "/PINManagement");
      cert = new URL(http, "/ca.crt");
      help = new URL(http, "/help/");
    } catch (MalformedURLException ex) {
      log.error("Failed to create URL.", ex);
    } finally {
      HTTP_SECURITY_LAYER_URL = http;
      HTTPS_SECURITY_LAYER_URL = https;
      PIN_MANAGEMENT_URL = pin;
      INSTALL_CERT_URL = cert;
      HELP_URL = help;
    }
  }
  public static final String version;
  static {
    String tmp = Configurator.UNKOWN_VERSION;
    try {
      String bkuWebStartJar = Launcher.class.getProtectionDomain().getCodeSource().getLocation().toString();
      URL manifestURL = new URL("jar:" + bkuWebStartJar + "!/META-INF/MANIFEST.MF");
      if (log.isTraceEnabled()) {
        log.trace("read version information from " + manifestURL);
      }
      Manifest manifest = new Manifest(manifestURL.openStream());
      Attributes atts = manifest.getMainAttributes();
      if (atts != null) {
        tmp = atts.getValue("Implementation-Build");
      }
    } catch (IOException ex) {
      log.error("failed to read version", ex);
    } finally {
      version = tmp;
      log.info("BKU Web Start " + version);
    }
  }
  private Configurator config;
  private Container server;
  private BasicService basicService;
  private TrayIcon trayIcon;
  private ResourceBundle messages;
  private AboutDialog aboutDialog;

  
  public Launcher() {
    log.info("Initializing Launcher");

    // SocketPerm * required (DataURL), FilePermission * write (JFileChooser) required,
    // jetty does not allow fine-grained permission config (codeBase?)
    // ie. we don't need a security manager
    log.trace("disabling (JNLP) security manager");
    System.setSecurityManager(null);

    messages = ResourceBundle.getBundle(MESSAGES_RESOURCE, Locale.getDefault());
    //TODO replace with statusNotifier
    trayIcon = initTrayIcon();
  }

  public void launch() throws Exception {
    initStart();
    try {
      initConfig();
    } catch (Exception ex) {
      log.error("Failed to initialize configuration", ex);
      trayIcon.displayMessage(messages.getString(CAPTION_ERROR),
              messages.getString(ERROR_CONFIG), TrayIcon.MessageType.ERROR);
      throw ex;
    }
    try {
      startServer();
      initFinished();
    } catch (BindException ex) {
      log.error("Failed to launch server, " + ex.getMessage(), ex);
      trayIcon.displayMessage(messages.getString(CAPTION_ERROR),
              messages.getString(ERROR_BIND), TrayIcon.MessageType.ERROR);
      throw ex;
    } catch (MultiException ex) {
      log.error("Failed to launch server, " + ex.getMessage(), ex);
      if (ex.getThrowable(0) instanceof BindException) {
        trayIcon.displayMessage(messages.getString(CAPTION_ERROR),
                messages.getString(ERROR_BIND), TrayIcon.MessageType.ERROR);
      } else {
        trayIcon.displayMessage(messages.getString(CAPTION_ERROR),
                messages.getString(ERROR_START), TrayIcon.MessageType.ERROR);
      }
      throw ex;
    } catch (Exception ex) {
      ex.printStackTrace();
      log.error("Failed to launch server, " + ex.getMessage(), ex);
      trayIcon.displayMessage(messages.getString(CAPTION_ERROR),
              messages.getString(ERROR_START), TrayIcon.MessageType.ERROR);
      throw ex;
    }
  }

  private void browse(URL url) throws IOException, URISyntaxException {
    // don't use basicService.showDocument(), which causes a java ssl warning dialog
    if (Desktop.isDesktopSupported()) {
      Desktop desktop = Desktop.getDesktop();
      if (desktop.isSupported(Desktop.Action.BROWSE)) {
        desktop.browse(url.toURI());
        return;
      }
    }
    throw new IOException("current platform does not support Java Desktop API");
  }
  
  private TrayIcon initTrayIcon() {
    if (SystemTray.isSupported()) {
      try {
        // get the SystemTray instance
        SystemTray tray = SystemTray.getSystemTray();
        log.debug("TrayIcon size: " + tray.getTrayIconSize());

        String iconResource;
        if (tray.getTrayIconSize().height < 17) {
          iconResource = TRAYICON_RESOURCE + "16.png";
        } else if (tray.getTrayIconSize().height < 25) {
          iconResource = TRAYICON_RESOURCE + "24.png";
        } else if (tray.getTrayIconSize().height < 33) {
          iconResource = TRAYICON_RESOURCE + "32.png";
        } else {
          iconResource = TRAYICON_RESOURCE + "48.png";
        }
        Image image = ImageIO.read(getClass().getResourceAsStream(iconResource));

        PopupMenu popup = new PopupMenu();

        MenuItem helpItem = new MenuItem(messages.getString(LABEL_HELP));
        helpItem.addActionListener(this);
        helpItem.setActionCommand(HELP_COMMAND);
        popup.add(helpItem);

        MenuItem pinItem = new MenuItem(messages.getString(LABEL_PIN));
        pinItem.addActionListener(this);
        pinItem.setActionCommand(PIN_COMMAND);
        popup.add(pinItem);

        MenuItem shutdownItem = new MenuItem(messages.getString(LABEL_SHUTDOWN));
        shutdownItem.addActionListener(this);
        shutdownItem.setActionCommand(SHUTDOWN_COMMAND);
        popup.add(shutdownItem);

        popup.addSeparator();

        MenuItem aboutItem = new MenuItem(messages.getString(LABEL_ABOUT));
        aboutItem.setActionCommand(ABOUT_COMMAND);
        aboutItem.addActionListener(this);
        popup.add(aboutItem);

        TrayIcon ti = new TrayIcon(image, messages.getString(TOOLTIP_DEFAULT), popup);
        ti.setImageAutoSize(true);
        ti.addActionListener(this);
        tray.add(ti);
        return ti;
      } catch (AWTException ex) {
        log.error("Failed to init tray icon", ex);
      } catch (IOException ex) {
        log.error("Failed to load tray icon image", ex);
      }
    } else {
      log.error("No system tray support");
    }
    return null;
  }

  private void initStart() {
    try {
      trayIcon.displayMessage(messages.getString(CAPTION_DEFAULT),
            messages.getString(MESSAGE_START), TrayIcon.MessageType.INFO);
      basicService = (BasicService) ServiceManager.lookup("javax.jnlp.BasicService");
      if (basicService.isOffline()) {
        log.info("launching MOCCA Web Start offline");
        trayIcon.displayMessage(messages.getString(CAPTION_DEFAULT),
                messages.getString(MESSAGE_START_OFFLINE), TrayIcon.MessageType.INFO);
      } else {
        log.info("launching MOCCA Web Start online");
      }
    } catch (UnavailableServiceException ex) {
      log.info("Failed to obtain JNLP service: " + ex.getMessage());
    }
  }

  private void initConfig() throws IOException, CodingException, GeneralSecurityException {
    trayIcon.displayMessage(messages.getString(CAPTION_DEFAULT),
              messages.getString(MESSAGE_CONFIG), TrayIcon.MessageType.INFO);
    config = new Configurator();
    config.ensureConfiguration();
    trayIcon.displayMessage(messages.getString(CAPTION_DEFAULT),
              messages.getString(MESSAGE_CERTS), TrayIcon.MessageType.INFO);
    config.ensureCertificates();
  }

  private void startServer() throws Exception {
    log.info("init servlet container and MOCCA webapp");
//    trayIcon.displayMessage(messages.getString(CAPTION_DEFAULT),
//              messages.getString(MESSAGE_START), TrayIcon.MessageType.INFO);
    server = new Container();
    server.init();
    server.start();
  }

  private void initFinished() {
    try {
      trayIcon.displayMessage(messages.getString(CAPTION_DEFAULT),
              messages.getString(MESSAGE_FINISHED), TrayIcon.MessageType.INFO);
      // standalone (non-webstart) version has splashscreen
      if (SplashScreen.getSplashScreen() != null) {
        try {
          SplashScreen.getSplashScreen().close();
        } catch (IllegalStateException ex) {
          log.warn("Failed to close splash screen: " + ex.getMessage());
        }
      }
      if (config.isCertRenewed()) {
        try {
          if ("".equals(messages.getLocale().getLanguage())) {
            browse(HTTP_SECURITY_LAYER_URL);
          } else {
            browse(new URL(HTTP_SECURITY_LAYER_URL, messages.getLocale().getLanguage()));
          }
        } catch (Exception ex) {
          log.error("failed to open system browser, install TLS certificate manually: " + HTTPS_SECURITY_LAYER_URL, ex);
        }
      }
      log.info("BKU successfully started");
      server.join();
    } catch (InterruptedException e) {
      log.warn("failed to join server: " + e.getMessage(), e);
    }
  }

  @Override
  public void shutDown() {
    log.info("Shutting down server");
    trayIcon.displayMessage(messages.getString(CAPTION_DEFAULT), 
            messages.getString(MESSAGE_SHUTDOWN), TrayIcon.MessageType.INFO);
    if ((server != null) && (server.isRunning())) {
      try {
        if (server.isRunning()) {
          server.stop();
        }
      } catch (Exception e) {
        log.debug(e.toString());
      } finally {
        if (server.isRunning()) {
          server.destroy();
        }
      }
    }
    System.exit(0);
  }

  /**
   * Listen for TrayMenu actions (display error messages on trayIcon)
   * @param e
   */
  @Override
  public void actionPerformed(ActionEvent e) {
    if (SHUTDOWN_COMMAND.equals(e.getActionCommand())) {
      log.debug("shutdown requested via tray menu");
      this.shutDown();
    } else if (ABOUT_COMMAND.equals(e.getActionCommand())) {
      log.debug("about dialog requested via tray menu");
      if (aboutDialog == null) {
        aboutDialog = new AboutDialog(new JFrame(), true, version);
        aboutDialog.addWindowListener(new WindowAdapter() {

          @Override
          public void windowClosing(java.awt.event.WindowEvent e) {
            aboutDialog.setVisible(false);
          }
        });
      }
      aboutDialog.setLocationByPlatform(true);
      aboutDialog.setVisible(true);
    } else if (PIN_COMMAND.equals(e.getActionCommand())) {
      log.debug("pin management dialog requested via tray menu");

      new Thread(new PINManagementInvoker(trayIcon, messages)).start();
    } else if (HELP_COMMAND.equals(e.getActionCommand())) {
      log.debug("help page requested via tray menu");
      try {
        if ("".equals(messages.getLocale().getLanguage())) {
          browse(HELP_URL);
        } else {
          browse(new URL(HELP_URL, messages.getLocale().getLanguage()));
        }
      } catch (Exception ex) {
        log.error("Failed to open " + HELP_URL, ex);
        String msg = MessageFormat.format(messages.getString(ERROR_OPEN_URL), HELP_URL);
        trayIcon.displayMessage(messages.getString(CAPTION_ERROR), msg, TrayIcon.MessageType.ERROR);
      }
    } else {
      log.error("unknown tray menu command: " + e.getActionCommand());
    }
  }

  public static void main(String[] args) throws InterruptedException, IOException {
    try {
      Launcher launcher = new Launcher();
      launcher.launch();
    } catch (Exception ex) {
      ex.printStackTrace();
      log.debug("Caught exception " + ex.getMessage(), ex);
      log.info("waiting to shutdown...");
      Thread.sleep(5000);
      log.info("exit");
      System.exit(-1000);
    }
  }
}