package org.wzy.sqltemplate;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.FutureTask;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.wzy.sqltemplate.script.*;
import org.wzy.sqltemplate.token.GenericTokenParser;
import org.wzy.sqltemplate.token.TokenHandler;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * 本类提供 sql 获取接口
 * 如果获取失败触发 xml 文件载入和解析
 * @author Wen ,Cheney
 *
 */
public class SqlTemplateEngin {

	// 按照 sql.xml 文件页 - id 两层查找
	private static ConcurrentHashMap<String, ConcurrentHashMap<String, FutureTask<SqlTemplate>>> templateCache;

	// 全局配置
	private static Configuration cfg;



	public SqlTemplateEngin(Configuration cfg) {
		super();
		this.cfg = cfg;
	}


	/**
	 *
	 * @param data
	 * @return
	 */
	public SqlMeta process(Object data) {

		Context context = new Context(cfg, data);

		calculate(context);

		parseParameter(context);

		return new SqlMeta(context.getSql(), context.getParameter());
	}

	private void parseParameter(final Context context) {

		String sql = context.getSql();

		GenericTokenParser parser1 = new GenericTokenParser("#{", "}",
				new TokenHandler() {

					public String handleToken(String content) {

						Object value = OgnlCache.getValue(content,
								context.getBinding());

						if (value == null) {
							throw new RuntimeException("Can not found "
									+ content + " value");
						}

						context.addParameter(value);

						return "?";
					}
				});

		sql = parser1.parse(sql);


		context.setSql(sql);

	}


	/**
	 * 获取指定的 sql
	 * @param xpath eg. club.fullstack.sql#getById
	 * @return
	 */
	public SqlTemplate getTemplate(final String xpath) throws IOException {

		SqlTemplatePage xmlPage = SqlTemplatePage.fromXPath(xpath);
		String sqlId = SqlTemplatePage.sqlId(xpath);
		String content;

		if ( cfg.isCacheTemplate() ) {

			if (f == null) {
				FutureTask<SqlTemplate> ft = new FutureTask<SqlTemplate>(
						new Callable<SqlTemplate>() {

							public SqlTemplate call() throws Exception {
								return createTemplate(content);
							}
						});

				f = templateCache.putIfAbsent(content, ft);

				if (f == null) {
					ft.run();
					f = ft;
				}
			}

			try {
				return f.get();
			} catch (Exception e) {
				templateCache.remove(content);
				throw new RuntimeException(e);
			}

		}else{
			callParse()
		}

		return createTemplate(content);

	}


	public callParse(){
		ConcurrentHashMap<String, FutureTask<SqlTemplate>> pageTemp = templateCache.get(xmlPage);

		if( null == pageTemp ){
			content = xmlPage.loadFile();
			pageTemp = new SqlTemplateParse(cfg,content).buildAll();
		}

		FutureTask<SqlTemplate> f = pageTemp.get(sqlId);

		if( null == f ){
			throw new RuntimeException("no thus tmpl");
		}
	}



	public static class SqlTemplatePage {
		// 命名空间 对应于 xml
		private String namespace = "/";

		// xml 文件
		private SqlFragment root;

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
			try {
				content = readerContent(this.getClass().getResourceAsStream(
						cfg.getMapperPath() + getNamespace()
				));
			} catch (IOException e) {
				throw new IOException("Error reading template ", e);
			}

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
			}

			bufferedReader.close();

			return sb.toString();
		}

		public String getNamespace() {
			return namespace.replace(".","/");
		}

		public void setNamespace(String namespace) {
			this.namespace = namespace;
		}
	}

}
