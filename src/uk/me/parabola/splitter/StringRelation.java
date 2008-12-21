/*
 * Copyright (C) 2008 Steve Ratcliffe
 * 
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License version 2 as
 *  published by the Free Software Foundation.
 * 
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 * 
 * 
 * Author: Steve Ratcliffe
 * Create date: 21-Dec-2008
 */
package uk.me.parabola.splitter;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Steve Ratcliffe
 */
public class StringRelation extends Element {
	private List<Member> members = new ArrayList<Member>();

	public StringRelation(String stringId) {
		super(stringId);
	}

	public void addMember(String type, String ref, String role) {
		Member mem = new Member();
		mem.type = type;
		mem.ref = ref;
		mem.role = role;

		members.add(mem);
	}

	public List<Member> getMembers() {
		return members;
	}

	static class Member {
		private String type;
		private String ref;
		private String role;

		public String getType() {
			return type;
		}

		public String getRef() {
			return ref;
		}

		public String getRole() {
			return role;
		}
	}
}
