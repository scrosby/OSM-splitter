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

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Persists a collection of relations to binary format on disk for later retrieval
 */
public class RelationStoreWriter extends AbstractStoreWriter {
	private final KeyLookupWriter roles;

	public RelationStoreWriter(String filename) throws IOException {
		this(new FileOutputStream(filename), new KeyLookupWriter(filename + ".keys"), new KeyLookupWriter(filename + ".roles"));
	}

	public RelationStoreWriter(OutputStream out, KeyLookupWriter keys, KeyLookupWriter roles) {
		super(out, keys);
		this.roles = roles;
	}

	public void write(int id) throws IOException {
		getOut().writeInt(id);
	}

	public void writeMember(MemberType type, int id, CharSequence role) throws IOException {
		getOut().writeByte(type.ordinal() + 1);
		getOut().writeInt(id);
		getOut().writeShort(roles.set(role));
	}

	public void closeMembers() throws IOException {
		getOut().writeByte(0);
	}

	@Override
	public void close() throws IOException {
		super.close();
		roles.close();
	}
}