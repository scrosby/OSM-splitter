/**
 * Copyright 2009 Chris Miller
 */
package uk.me.parabola.splitter.disk;

/**
 * Represents a member of a relation
 */
public class Member {
	private MemberType type;
	private int id;
	private String role;

	public Member(MemberType type, int id, String role) {
		this.type = type;
		this.id = id;
		this.role = role;
	}

	public MemberType getType() {
		return type;
	}

	public int getId() {
		return id;
	}

	public String getRole() {
		return role;
	}
}
