package com.m;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Date;
import java.util.Hashtable;
import java.util.Random;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;

/**
 * 白箱弱點範例
 *
 */
public class SampleServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;

	private static final Log LOGGER = LogFactory.getLog(SampleServlet.class);

	public SampleServlet() {
		super();
	}

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		ServletOutputStream out = response.getOutputStream();
		try{			
			HttpSession session = request.getSession();

			//--[DataSource1]網站請求 Web Request
			call(request.getParameter("username"),out,response,session);
			
			//--[DataSource4]檔案 File
			call(FileUtils.readFileToString(new File("")),out,response,session);
			
			//--[DataSource7]資料庫 Database
			Connection connection = DriverManager.getConnection("jdbc:mysql://127.0.0.1:3306/crm", "root", "1234");
			Statement stmt = connection.createStatement();
			ResultSet rs = stmt.executeQuery("");
			call(rs.getString(1),out,response,session);
			rs.close();
			stmt.close();
			connection.close();						
			
			//--[DataSource11]System Environment 
			call(System.getenv("username"),out,response,session);
			
			//--[DataSource13]Session
			call((String)request.getSession().getAttribute("username"),out,response,session);
			
			//----------------------------------------//

			//--[defect8]明文密碼缺失  Hard-Coded Password			
			DriverManager.getConnection("", "scott", "tiger");
			
			//--[defect14]不安全的密碼演算法  Risky Cryptographic Algorithm
			//--使用了像是 MD5/SHA1/RC3/RC4/DES 這類脆弱的密碼演算法來保護機敏資料
			MessageDigest digester = MessageDigest.getInstance("MD5");
			digester.reset();
			digester.update("test".getBytes());			
			byte[] digest = digester.digest();
						
			//--[defect15]不安全的亂數  Insecure Randomness
			Random ranGen = new Random();
			ranGen.setSeed((new Date()).getTime());
			int value = ranGen.nextInt();
			out.println(value);
		}
		catch(Exception e){
			//--[defect11]Information Leak of System Data
			//--系統資料或除錯資訊從輸出資料流或紀錄函數流出程式之外，這將有助於攻擊者了解系統並組織攻擊計畫
			e.printStackTrace(new PrintStream(out));
			out.println(e.getMessage());
			
			//--[defect19]Information Leak Through Log Files
			//--程式將機密資料寫至日誌檔內，因而導致攻擊者取得機密資料或者環境資訊
			LOGGER.debug("Error: Unable to create network folders dir with path " + System.getProperty("user.home"));
		}
	}
	
	private void call(String untrustedValue,ServletOutputStream out, HttpServletResponse response,HttpSession session) throws Exception{
		
		//--[defect1]Reflection Injection	
		//--藉由操弄傳入值，攻擊者可以造成預期之外的類別被載入，或是改變被物件存取的方法或欄位
		String ctl = untrustedValue;
		Class cmdClass = Class.forName(ctl + "Command");
		Object ao = cmdClass.newInstance();
		
		//--[defect2]Cross-Site Scripting
		//--允許攻擊者植入惡意程式碼，並讓瀏覽器執行的弱點
		//--<script>alert('1');</script>
		String username = untrustedValue;
		out.print(username);
		
		//--[defect3]HTTP Response Splitting
		//--未經驗證的資料被寫入HTTP標頭時，這樣可能會允許攻擊者設定整個傳送到瀏覽器的HTTP應答
		String display = untrustedValue;
		Cookie cookie = new Cookie("display", display);
		response.addCookie(cookie);	
		
		String username2 = untrustedValue;
		String password = untrustedValue;
		
		//--[defect4]XPath Injection
		//--允許攻擊者可以透過修改 XPath 敘述來獲得 XML 檔案中受保護資料的弱點
		//-- ' or '1'='1
		XPathFactory factory = XPathFactory.newInstance();
		XPath xpath = factory.newXPath();
		XPathExpression expr = xpath.compile("//users/user[login/text()='" +
				username2 + "' and password/text()='" + password + "' ]");
		
		DocumentBuilderFactory factory1 = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory1.newDocumentBuilder();;
        Document doc = builder.parse("smartphone.xml");
		expr.evaluate(doc, XPathConstants.NODESET);
		
		//--[defect5]Resource Injection
		//--應用程式將從其他元件所取得的輸入當做資源的識別代碼 (像是物件編號、檔案名稱等) 時，卻未事先做好任何的限制，或是限制不夠嚴格。
		//--資源注入將導致攻擊者可以存取應該受到保護的系統資源。
		//-- ../../tomcat/conf/server.xml
		String rName = untrustedValue;
		File rFile = new File("/usr/local/apfr/reports/" + rName);
		rFile.mkdir();
		
		//--[defect6]SQL Injection		
		//--惡意的 SQL 指令被插入於事先定義好的 SQL 指令中，並試圖改變執行的結果
		//-- ' or '1'='1
		Class.forName("com.mysql.jdbc.Driver");
		Connection connection = DriverManager.getConnection("jdbc:mysql://127.0.0.1:3306/crm", "root", "1234");
		
		String query = "SELECT * FROM USERS WHERE " + "" +
				"username = '" + username2 + "' AND password = '" + password + "'";
		Statement stmt = connection.createStatement();
		ResultSet rs = stmt.executeQuery(query);
		rs.close();
		stmt.close();
		connection.close();
		
		//--[defect12]LDAP注入  LDAP Injection
		Hashtable<String, String>  env = new Hashtable<String, String>();
		env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
		DirContext dctx = new InitialDirContext(env);

		SearchControls sc = new SearchControls();
		String[] attributeFilter = {"cn", "mail"};
		sc.setReturningAttributes(attributeFilter);
		sc.setSearchScope(SearchControls.SUBTREE_SCOPE);
		String base = "dc=example,dc=com";
		String filter = "(&(sn=" + username2 + ")(userPassword=" + password + "))";			 
		NamingEnumeration<?> results = dctx.search(base, filter, sc);
		
		//--[defect13]Open Redirect
		//--將不可信任之使用者輸入作為網頁轉址的目的地
		String url3 = untrustedValue;
		response.sendRedirect(url3);
		
		//--[defect16]Log Forging
		//--程式未將使用者輸入的資料做好驗證工作就寫入到日誌檔之中，因而導致攻擊者可以偽造日誌的紀錄或是在日誌檔內插入惡意的內容
		//--攻擊者可以藉由插入包含合適字元 (CRLF 注入) 在內的訊息以產生假冒的日誌記錄
		String token = untrustedValue;
		LOGGER.info(token);
		
		//--[defect18]Session Variable Poisoning
		//--應用程式忽略或未適當地驗證使用者的輸入，並將這些資訊存放在 Session 中以用來決定應用程式的控制流程或資料流程
		String url = untrustedValue;
		session.setAttribute("source", url);

		String url2 = (String)session.getAttribute("source");
		response.sendRedirect(url2);														
		
		// 深度 2
		SampleUtil.call2(untrustedValue,out,response,session);
	}
	
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

	}

}
