package cgnet.swara.activity;

import java.util.Date;
import java.util.Properties;

import javax.activation.CommandMap;
import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.activation.MailcapCommandMap;
import javax.mail.Authenticator;
import javax.mail.BodyPart;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import android.util.Log;

// I based this version of something I saw online here:
// 

public class Mail extends javax.mail.Authenticator {
	private static final String TAG = "Mail";
	
	private String _user; 
	private String _pass; 

	private String[] _to; 
	private String _from; 

	private String _port; 
	private String _sport; 

	private String _host; 

	private String _subject; 
	private String _body; 
	
	private String _attachment;

	private boolean _auth; 

	private boolean _debuggable; 

	private Multipart _multipart;
 
	public Mail() { 
		_host = "smtp.gmail.com"; // default smtp server
		_port = "465"; // default smtp port
		_sport = "465"; // default socketfactory port

		_user = ""; // username 
		_pass = ""; // password 
		_from = ""; // email sent from 
		_subject = ""; // email subject 
		_body = ""; // email body 

		_debuggable = false; // debug mode on or off - default off 
		_auth = true; // smtp authentication - default on 

		_multipart = new MimeMultipart(); 

		// There is something wrong	 with MailCap, javamail can not find a handler for the multipart/mixed part, so this bit needs to be added. 
		MailcapCommandMap mc = (MailcapCommandMap) CommandMap.getDefaultCommandMap(); 
		mc.addMailcap("text/html;; x-java-content-handler=com.sun.mail.handlers.text_html"); 
		mc.addMailcap("text/xml;; x-java-content-handler=com.sun.mail.handlers.text_xml"); 
		mc.addMailcap("text/plain;; x-java-content-handler=com.sun.mail.handlers.text_plain"); 
		mc.addMailcap("multipart/*;; x-java-content-handler=com.sun.mail.handlers.multipart_mixed"); 
		mc.addMailcap("message/rfc822;; x-java-content-handler=com.sun.mail.handlers.message_rfc822"); 
		CommandMap.setDefaultCommandMap(mc); 
	} 

	public Mail(String user, String pass) { 
		this(); 

		_user = user; 
		_pass = pass; 
	} 
	

class GMailAuthenticator extends Authenticator {
     String user;
     String pw;
     public GMailAuthenticator (String username, String password)
     {
        super();
        this.user = username;
        this.pw = password;
     }
    public PasswordAuthentication getPasswordAuthentication()
    {
       return new PasswordAuthentication(user, pw);
    }
}

	public boolean send() throws Exception { 
		Properties props = _setProperties(); 

		if(!_user.equals("") && !_pass.equals("") && _to.length > 0 && !_from.equals("") && !_subject.equals("") && !_body.equals("")) {
			Session session = Session.getInstance(props, new GMailAuthenticator(_user,_pass)); //EmailLogin.email, EmailLogin.password

			MimeMessage msg = new MimeMessage(session); 

			msg.setFrom(new InternetAddress(_from)); 

			InternetAddress[] addressTo = new InternetAddress[_to.length]; 
			for (int i = 0; i < _to.length; i++) { 
				addressTo[i] = new InternetAddress(_to[i]); 
			} 
			msg.setRecipients(MimeMessage.RecipientType.TO, addressTo); 

			msg.setSubject(_subject); 
			msg.setSentDate(new Date()); 

			// setup message body 
			BodyPart messageBodyPart = new MimeBodyPart(); 
			messageBodyPart.setText(_body); 
			_multipart.addBodyPart(messageBodyPart); 

			// Put parts in message 
			msg.setContent(_multipart);   
			 
			try {
				// send email 
				Transport.send(msg); 
				Log.e(TAG, "Currently trying to send the email.");
			} catch(Exception e) { 
				Log.e(TAG, "" + e);
				 
				return false;
			}
			return true; 
		} else { 
			return false; 
		} 
	}    

	public void addAttachment(String filename) throws Exception { 
		if(filename != null && !filename.equals("/null") && !filename.equals("null") && !filename.equals(" ")) { 
			_attachment = filename;
			BodyPart messageBodyPart = new MimeBodyPart(); 
			DataSource source = new FileDataSource(filename); 
			messageBodyPart.setDataHandler(new DataHandler(source)); 
			
			String name = filename.substring(filename.lastIndexOf("/") + 1);
 
			messageBodyPart.setFileName(name);  
			_multipart.addBodyPart(messageBodyPart); 
		}
	} 

	@Override 
	public PasswordAuthentication getPasswordAuthentication() { 
		return new PasswordAuthentication(_user, _pass); 
	} 

	private Properties _setProperties() { 
		Properties props = new Properties(); 

		props.put("mail.smtp.host", _host); 

		if(_debuggable) { 
			props.put("mail.debug", "true"); 
		} 

		if(_auth) { 
			props.put("mail.smtp.auth", "true"); 
		} 

		props.put("mail.smtp.port", _port); 
		props.put("mail.smtp.socketFactory.port", _sport); 
		props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory"); 
		props.put("mail.smtp.socketFactory.fallback", "false");
		props.put("mail.smtp.connectiontimeout", 90000);
		props.put("mail.smtp.timeout", 90000);
 
		return props; 
	} 

	// the getters and setters 
	public String getTo() { 
		return _to[0];
		
	}
	public String getFrom() { 
		return _from;
	}
	
	public String getUser() { 
		return _user;
	}
	
	public String getPass() { 
		return _pass;
	}
	
	public String getAttachment() {
		return _attachment; 
	}
	
	public String getBody() { 
		return _body; 
	} 
	
	public String getSubject() { 
		return _subject;
	}
	  

	public void setBody(String _body) { 
		this._body = _body; 
	} 

	public void setTo(String[] toArr) {
		this._to = toArr;
	}

	public void setFrom(String string) {
		this._from = string;
	}

	public void setSubject(String string) {
		this._subject = string;
	}

} 