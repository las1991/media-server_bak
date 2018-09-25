package com.sengled.media;

import java.io.Serializable;

import org.apache.commons.io.FilenameUtils;

public final class Key implements Comparable<Key>, Serializable {
	private static final long serialVersionUID = -8217237160389087057L;
	
	private String uri;
	private String token;
	
	
	public static Key valueOf(String uri) {
		return new Key(uri, FilenameUtils.getBaseName(uri));
	}

	private Key(String uri, String token) {
		super();
		this.uri = uri;
		this.token = token;
	}


	public String getUri() {
		return uri;
	}
	
	@Override
	public int hashCode() {
		return token.hashCode();
	}
	
	@Override
	public int compareTo(Key o) {
		return token.compareTo(o.token);
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Key) {
			Key new_name = (Key) obj;
			return this.compareTo(new_name) == 0;
		}
		
		return false;
	}

	public String getToken() {
		return token;
	}
}