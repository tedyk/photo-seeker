package org.tedka.photoseeker.channel.flickr;

import org.tedka.photoseeker.channel.base.ChannelModel;

import java.io.Serializable;

/**
 * Model to hold a Flickr result item
 */
public class FlickrModel extends ChannelModel implements Serializable {

	private static final long serialVersionUID = 1L;

	private String id, secret, server, farm, title;

	public String getFarmId() {
		return farm;
	}

	public void setFarm(String farm) {
		this.farm = farm;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getSecretId() {
		return secret;
	}

	public void setSecret(String secret) {
		this.secret = secret;
	}

	public String getServerId() {
		return server;
	}

	public void setServer(String server) {
		this.server = server;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

}
