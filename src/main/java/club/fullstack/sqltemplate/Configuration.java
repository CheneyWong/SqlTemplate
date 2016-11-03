package club.fullstack.sqltemplate;

import java.nio.charset.Charset;
/**
 * 
 * @author Wen
 * 
 */

public class Configuration {

	private transient boolean cacheTemplate;


	private Charset charset;

	public Configuration() {
		this(true, Charset.defaultCharset());
	}

	public Configuration(boolean cacheTemplate, Charset charset) {
		super();

		this.cacheTemplate = cacheTemplate;
		this.charset = charset;
	}

	public boolean isCacheTemplate() {
		return cacheTemplate;
	}

	public void setCacheTemplate(boolean cacheTemplate) {
		this.cacheTemplate = cacheTemplate;
	}

	public Charset getCharset() {
		return charset;
	}

	public void setCharset(Charset charset) {
		this.charset = charset;
	}

}
