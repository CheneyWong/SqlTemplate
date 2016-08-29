package club.fullstack.sqltemplate;

import java.util.HashMap;

public class Bindings extends HashMap<Object,Object>{
	private static final long serialVersionUID = -7290846439659491933L;

	public Bindings(){}

	public Bindings(Object key , Object value) {
		this();
		this.put(key, value);
	}

	public Bindings bind(Object key , Object value){
		this.put(key, value);
		return this; 
	}

}
