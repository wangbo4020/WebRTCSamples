package com.adups.remotecare.model;

/**
 * Created by Dylan on 2017/11/23.
 */

public class MessageDescription {

	public static final int TYPE_NOTICE = 0;
	public static final int TYPE_OTHER = 1;
	public static final int TYPE_SELF = 2;

	private CharSequence nickname;
	private CharSequence content;
	private int type;

	public MessageDescription() {
	}

	public MessageDescription(CharSequence nickname, CharSequence content, int type) {
		this.nickname = nickname;
		this.content = content;
		this.type = type;
	}

	public CharSequence getNickname() {
		return nickname;
	}

	public void setNickname(CharSequence nickname) {
		this.nickname = nickname;
	}

	public CharSequence getContent() {
		return content;
	}

	public void setContent(CharSequence content) {
		this.content = content;
	}

	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}
}
