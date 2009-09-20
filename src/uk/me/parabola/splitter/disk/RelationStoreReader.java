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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads in relations from a binary format
 */
public class RelationStoreReader extends AbstractStoreReader {
	private List<Member> members = new ArrayList<Member>(10);
	private final KeyLookupReader roles;

	public RelationStoreReader(String filename) throws IOException {
		this(new FileInputStream(new File(filename)), new KeyLookupReader(filename + ".keys"), new KeyLookupReader(filename + ".roles"));
	}

	public RelationStoreReader(InputStream in, KeyLookupReader keys, KeyLookupReader roles) throws IOException {
		super(in, keys);
		this.roles = roles;
	}

	public List<Member> getMembers() {
		return members;
	}

	@Override
	protected void readHeader() throws IOException {
		members.clear();
		byte typeByte;
		while ((typeByte = getIn().readByte()) != 0) {
			MemberType type = MemberType.values()[typeByte - 1];
			int id = getIn().readInt();
			short roleId = getIn().readShort();
			String role = roles.get(roleId);
			members.add(new Member(type, id, role));
		}
	}
}