package net.sf.regain.ui.server;

import java.io.IOException;
import com.sun.mail.imap.IMAPFolder;
import com.sun.mail.imap.IMAPStore;
import com.sun.mail.imap.IMAPSSLStore;
import java.io.ByteArrayOutputStream;
import net.sf.regain.ImapToolkit;
import java.util.regex.Matcher;
import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Session;
import javax.mail.URLName;
import javax.mail.internet.MimeMessage;
import java.util.Properties;
import java.util.zip.CRC32;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.JspFactory;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.PageContext;
import org.apache.commons.codec.binary.Base64;
import net.sf.regain.RegainException;
import net.sf.regain.search.IndexSearcherManager;
import net.sf.regain.search.SearchToolkit;
import net.sf.regain.util.sharedtag.PageRequest;
import net.sf.regain.util.sharedtag.PageResponse;
import net.sf.regain.util.sharedtag.taglib.JspPageRequest;
import net.sf.regain.util.sharedtag.taglib.JspPageResponse;
import java.io.PrintWriter;
import java.util.Enumeration;

/**
 * 
 * A servlet providing imap mails as eml files
 * 
 * @author frenchie71
 * 
 */
public class ImapServlet extends HttpServlet {

  protected void doPost(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    handleRequest(req, resp);
  }

  private void loadIMAPMessage(String messageURL, OutputStream theOutStream,
      String clearTextauthToken) throws RegainException {

    try {
      Matcher matcher = ImapToolkit.getMessagePattern().matcher(messageURL);
      matcher.find();
      if (matcher.groupCount() > 0) {
        // We found a message url. Determine the message UID from the url.
        int messageUID = Integer.parseInt(matcher.group(3));
        // mLog.debug("Read mime message uid: " + messageUID + " for IMAP url: "
        // + url);
        Session session = Session.getInstance(new Properties());

        // URLName originURLName = new
        // URLName(ImapToolkit.cutMessageIdentifier(messageURL).replaceAll("%3A",
        // ":").replaceAll("///", "//").replaceAll("\\+", " "));
        URLName originURLName = new URLName(java.net.URLDecoder.decode(
            ImapToolkit.cutMessageIdentifier(messageURL), "UTF-8").replaceAll(
            "///", "//"));

        String folder = "";

        if (originURLName.getFile() != null) {

          // If you are not using dovecot, you need to remove the portion
          // .replaceAll("/",".")

          folder = originURLName.getFile().replaceAll("%20", " ")
              .replaceAll("/", ".");
          ;
        }

        // this only works with basic authentication

        String[] userPass = clearTextauthToken.split(":");

        URLName urlName = new URLName(originURLName.getProtocol(),
            originURLName.getHost(), originURLName.getPort(), folder,
            userPass[0], userPass[1]);

        IMAPStore imapStore;

        if (urlName.toString().startsWith("imaps:")) {
          imapStore = new IMAPSSLStore(session, urlName);
        } else {
          imapStore = new IMAPStore(session, urlName);
        }

        imapStore.connect();
        IMAPFolder currentFolder;

        if (urlName.getFile() == null) {
          // There is no folder given
          currentFolder = (IMAPFolder) imapStore.getDefaultFolder();
        } else {
          currentFolder = (IMAPFolder) imapStore.getFolder(folder);
        }

        currentFolder.open(Folder.READ_WRITE);
        MimeMessage cplMessage = (MimeMessage) currentFolder
            .getMessageByUID(messageUID);

        if (cplMessage != null) {
          cplMessage.setFlag(Flags.Flag.SEEN, true);
          cplMessage.writeTo(theOutStream);
          theOutStream.flush();
          theOutStream.close();
        }

        currentFolder.close(false);
        imapStore.close();

      }

    } catch (Throwable thr) {
      throw new RegainException(thr.getMessage(), thr);
    }
  }

  protected void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    handleRequest(req, resp);
  }

  /**
   * Handles a HTTP request.
   * 
   * @param req
   *          The request.
   * @param resp
   *          The response.
   * @throws ServletException
   *           If handling the request failed.
   * @throws IOException
   *           If writing to the result page failed.
   */
  private void handleRequest(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    // Create a page context
    // JspFactory factory = JspFactory.getDefaultFactory();
    // String errorPageURL = null;
    // int bufferSize = (JspWriter.DEFAULT_BUFFER <= 0) ? 1024 :
    // JspWriter.DEFAULT_BUFFER;
    // boolean needsSession = false;
    // boolean autoFlush = true;
    // PageContext pageContext = null;
    try {
      req.setCharacterEncoding("UTF-8");
      // pageContext = factory.getPageContext(this, req, resp,
      // errorPageURL, needsSession, bufferSize, autoFlush);

      // Create a shared wrapper
      // PageRequest request = new JspPageRequest(pageContext);
      // PageResponse response = new JspPageResponse(pageContext);

      // Extract the file name
      String encoding = "utf-8"; // We use utf-8 in our JSPs
      String fileUrl = SearchToolkit.extractFileUrl(req.getRequestURI(),
          encoding);

      // this works for basic authentication

      String authToken[] = req.getHeader("Authorization").split(" ");

      Base64 decoder = new Base64();
      String clearTextAuthToken = new String(decoder.decode(authToken[1]));

      // In order to be able to open the mail on the client
      // we send it as .eml file

      resp.setHeader("Content-Type", "message/rfc822");

      // Send the file

      OutputStream out = resp.getOutputStream();
      loadIMAPMessage(fileUrl, out, clearTextAuthToken);
    }

    catch (RegainException exc) {
      throw new ServletException("Checking imap access failed: ", exc);
    }
  }

  public void destroy() {
    try {
      IndexSearcherManager.closeAll();
    } catch (IOException e) {
    }
    super.destroy();
  }

}
