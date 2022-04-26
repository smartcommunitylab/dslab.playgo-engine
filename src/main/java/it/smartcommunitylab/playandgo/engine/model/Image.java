package it.smartcommunitylab.playandgo.engine.model;

public class Image {
	private String contentType;
	private byte[] image;
	
	public String getContentType() {
		return contentType;
	}
	public void setContentType(String contentType) {
		this.contentType = contentType;
	}
	public byte[] getImage() {
		return image;
	}
	public void setImage(byte[] image) {
		this.image = image;
	}
}
