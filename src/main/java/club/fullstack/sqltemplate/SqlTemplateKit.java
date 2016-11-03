package club.fullstack.sqltemplate;

import java.io.*;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.FutureTask;

import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;


/**
 * 本类提供 sql 获取接口
 * 如果获取失败触发 xml 文件载入和解析
 * @author Wen ,Cheney
 *
 */
public class SqlTemplateKit {

	// 按照 sql.xml 文件页 - id 两层查找
	private static ConcurrentHashMap<String, HashMap<String, FutureTask<SqlTemplate>>> templateCache
			= new ConcurrentHashMap<String, HashMap<String, FutureTask<SqlTemplate>>>();

	// 全局配置
	private static Configuration cfg;

	private static SqlTemplateKit sqlTemplateKit;



	public SqlTemplateKit(Configuration cfg) {
		super();
		this.cfg = cfg;
	}


	public static SqlTemplateKit getDefIns(){
		if( null == sqlTemplateKit ){
			synchronized (SqlTemplateKit.class){
				if( null == sqlTemplateKit ){
					Configuration cfg = new Configuration();
					cfg.setCharset(Charset.forName("UTF-8"));
					return new SqlTemplateKit(cfg);
				}
			}
		}
		return sqlTemplateKit;

	}


	/**
	 * 获取指定的 sql
	 * @param xpath eg. club.fullstack.sql#getById
	 * @return
	 */
	public SqlTemplate getTemplate(final String xpath) throws IOException, ParserConfigurationException, SAXException {

		SqlTemplatePage xmlPage = SqlTemplatePage.fromXPath(xpath);
		String sqlId = SqlTemplatePage.sqlId(xpath);
		return getTemplate(xmlPage,sqlId);
	}

	/**
	 * 获取指定的 sql
	 * @return
	 */
	public SqlTemplate getTemplate(final String xmlPath,final String sqlId) throws IOException, ParserConfigurationException, SAXException {
		SqlTemplatePage xmlPage = SqlTemplatePage.fromXPath(xmlPath);
		return getTemplate(xmlPage,sqlId);
	}

	/**
	 * 获取指定的 sql
	 * @return
	 */
	public SqlTemplate getTemplate(final SqlTemplatePage xmlPage,final String sqlId) throws IOException, ParserConfigurationException, SAXException {

		String content;

		// TODO 加锁
		HashMap<String, FutureTask<SqlTemplate>> pageTemp = templateCache.get(xmlPage.getNamespace());
		if (null == pageTemp) {
			pageTemp = xmlPage.buildPage();
		}


		FutureTask<SqlTemplate> ft = pageTemp.get(sqlId);

		if (null == ft) {
			throw new RuntimeException("no thus tmpl:" + xmlPage.getNamespace() + "#" + sqlId);
		}

		ft.run();

		try {
			return ft.get();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}


	public static class SqlTemplatePage {
		// 命名空间 对应于 xml
		private String namespace = "/";

		// 一级子元素列表
		private HashMap<String,Node> nodelist;

		// xml 解析器
		private SqlTemplateEngin engin = new SqlTemplateEngin(cfg,this);



		/**
		 * 从 xpath 解析到 SqlTemplatePage
		 * @param xpath
		 * @return
		 */
		public static SqlTemplatePage fromXPath(String xpath){
			String namespace;
			int charpIndex = xpath.lastIndexOf("#");
			if ( charpIndex > 0 ){
				namespace = xpath.substring(0,charpIndex);
			}else{
				namespace = xpath;
			}
			if (! namespace.startsWith("/")){
				namespace = "/" + namespace;
			}
			return new SqlTemplatePage(namespace);
		}

		/**
		 * 提取 id 部分
		 * @param xpath
		 * @return
		 */
		public static String sqlId(String xpath){
			int charpIndex = xpath.lastIndexOf("#");
			if ( charpIndex > 0 ){
				return xpath.substring(charpIndex + 1,xpath.length());
			}else{
				return null;
			}
		}


		public SqlTemplatePage(String namespace) {
			this.namespace = namespace;
		}

		public String loadFile() throws IOException {

			String content;
			String fileName = getNamespace() + ".xml";
			InputStream in = this.getClass().getResourceAsStream(fileName);
			if (null == in ){
				String classpath =  this.getClass().getResource("/").getPath();

				throw new RuntimeException("cannot find file :" + classpath + fileName);
			}
			content = readerContent(in);

			return content;
		}

		private String readerContent(InputStream in) throws IOException {

			StringBuilder sb = new StringBuilder(in.available());

			InputStreamReader inputStreamReader = new InputStreamReader(
					new BufferedInputStream(in), cfg.getCharset());

			BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

			String line;

			while ((line = bufferedReader.readLine()) != null) {
				sb.append(line);
				sb.append(" ");
			}

			bufferedReader.close();

			return sb.toString();
		}

		public HashMap<String,FutureTask<SqlTemplate>> buildPage() throws IOException, ParserConfigurationException, SAXException {
			String content = this.loadFile();
			HashMap<String,FutureTask<SqlTemplate>> ret = new HashMap<String,FutureTask<SqlTemplate>>();
			nodelist = engin.buildPage(content);
			final SqlTemplatePage self = this;
			Iterator it = nodelist.entrySet().iterator();
			while (it.hasNext()) {
				Map.Entry entry = (Map.Entry) it.next();
				String key = (String)entry.getKey();
				final Node node = (Node)entry.getValue();
				FutureTask<SqlTemplate> ft = new FutureTask<SqlTemplate>(
					new Callable<SqlTemplate>() {
						public SqlTemplate call() throws Exception {
							return engin.buildSQLTempl(node);
						}
					});
				ret.put(key,ft);
			}

			templateCache.put(this.getNamespace(),ret);
			return ret;
		}


		public Node getChildNode(String id){
			return nodelist.get(id);
		}

		public String getNamespace() {
			return namespace.replace(".","/");
		}

		public void setNamespace(String namespace) {
			this.namespace = namespace;
		}
	}

}
