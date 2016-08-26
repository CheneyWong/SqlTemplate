package club.fullstack.sqltemplate;

import java.util.List;

/**
 * 
 * @author Wen
 *
 */
public class SqlMeta {
	
	
	private String sql  ;
	
	private List<Object> parameter ;

	public SqlMeta(String sql, List<Object> parameter) {
		super();
		this.sql = sql;
		this.parameter = parameter;
	}

	public String getSql() {
		return sql;
	}

	public String getSqlP1() {
		int index = this.sql.toLowerCase().indexOf("from");
		if(index > 0){
			return this.sql.substring(0,index);
		}else{
			return this.sql;
		}
	}

	public String getSqlP2() {
		int index = this.sql.toLowerCase().indexOf("from");
		if(index > 0){
			return this.sql.substring(index,this.sql.length());
		}else{
			return this.sql;
		}
	}

	public void setSql(String sql) {
		this.sql = sql;
	}

	public Object[] getParams(){
		return parameter.toArray();
	}

	public List<Object> getParameter() {
		return parameter;
	}

	public void setParameter(List<Object> parameter) {
		this.parameter = parameter;
	}

	@Override
	public String toString() {

		String debugSql = sql;
		for (Object obj : parameter){
			debugSql = debugSql.replaceFirst("\\?",obj.toString());
		}

		return "SqlMeta { \r\n" +
				"\tsql=" + sql + "\r\n" +
				"\tparams=" + parameter + "\r\n" +
				"\tdebug_sql=" + debugSql + "\r\n" +
				"}";
	}
	
	

}
