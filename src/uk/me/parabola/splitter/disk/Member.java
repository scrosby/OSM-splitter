/*
 * Copyright (c) 2009.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 3 as
 * published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
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
