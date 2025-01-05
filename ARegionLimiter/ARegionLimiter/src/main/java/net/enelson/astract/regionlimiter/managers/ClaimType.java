package net.enelson.astract.regionlimiter.managers;

public enum ClaimType {

	IN_GLOBAL("in-global"),
	IN_OWN_REGION("in-own-region");
	
	private String path;
	
	ClaimType(String path) {
		this.path = path;
	}
	
	public String getPath() {
		return this.path;
	}
}
