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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 *
 */
public class TestStores {
	@Test
	public void testNodeStore() throws IOException {
		ByteArrayOutputStream nodeOut = new ByteArrayOutputStream();
		ByteArrayOutputStream keyOut = new ByteArrayOutputStream();
		KeyLookupWriter keyWriter = new KeyLookupWriter(keyOut);

		NodeStoreWriter nodeWriter = new NodeStoreWriter(nodeOut, keyWriter);
		nodeWriter.write(1, 0.1234d, 99.125d);
		nodeWriter.writeTag("key1", "value1");
		nodeWriter.writeTag("key2", "value2");
		nodeWriter.writeTag("key3", "Euro: \u20AC  Pound: \u00A3");
		nodeWriter.closeTags();
		nodeWriter.next();
		nodeWriter.write(2, 5432.12d, 48484.48484d);
		nodeWriter.writeTag("key2", "Euro: \u20AC");
		nodeWriter.writeTag("key4", "Pound: \u00A3");
		nodeWriter.closeTags();
		nodeWriter.next();

		nodeWriter.close();

		byte[] nodeBuf = nodeOut.toByteArray();
		byte[] keyBuf = keyOut.toByteArray();
		KeyLookupReader keyReader = new KeyLookupReader(new ByteArrayInputStream(keyBuf));
		ByteArrayInputStream nodeIn = new ByteArrayInputStream(nodeBuf);
		NodeStoreReader nodeReader = new NodeStoreReader(nodeIn, keyReader);

		Assert.assertTrue(nodeReader.next());
		Assert.assertEquals(nodeReader.getId(), 1);
		Assert.assertEquals(nodeReader.getLat(), 0.1234d);
		Assert.assertEquals(nodeReader.getLon(), 99.125d);
		Map<String, String> tags = nodeReader.getTags();
		Assert.assertEquals(tags.size(), 3);
		Assert.assertEquals(tags.get("key1"), "value1");
		Assert.assertEquals(tags.get("key2"), "value2");
		Assert.assertEquals(tags.get("key3"), "Euro: \u20AC  Pound: \u00A3");
		Assert.assertNull(tags.get("key4"));

		Assert.assertTrue(nodeReader.next());
		Assert.assertEquals(nodeReader.getId(), 2);
		Assert.assertEquals(nodeReader.getLat(), 5432.12d);
		Assert.assertEquals(nodeReader.getLon(), 48484.48484d);
		tags = nodeReader.getTags();
		Assert.assertEquals(tags.size(), 2);
		Assert.assertNull(tags.get("key1"));
		Assert.assertEquals(tags.get("key2"), "Euro: \u20AC");
		Assert.assertNull(tags.get("key3"));
		Assert.assertEquals(tags.get("key4"), "Pound: \u00A3");

		Assert.assertFalse(nodeReader.next());
	}

	@Test
	public void testWayStore() throws IOException {
		ByteArrayOutputStream nodeOut = new ByteArrayOutputStream();
		ByteArrayOutputStream keyOut = new ByteArrayOutputStream();
		KeyLookupWriter keyWriter = new KeyLookupWriter(keyOut);

		WayStoreWriter wayWriter = new WayStoreWriter(nodeOut, keyWriter);
		wayWriter.write(1);
		wayWriter.writeNodeRef(10);
		wayWriter.writeNodeRef(11);
		wayWriter.writeNodeRef(12);
		wayWriter.closeNodeRefs();
		wayWriter.writeTag("key1", "value1");
		wayWriter.writeTag("key2", "value2");
		wayWriter.writeTag("key3", "Euro: \u20AC  Pound: \u00A3");
		wayWriter.closeTags();
		wayWriter.next();
		wayWriter.write(2);
		wayWriter.closeNodeRefs();
		wayWriter.writeTag("key2", "Euro: \u20AC");
		wayWriter.writeTag("key4", "Pound: \u00A3");
		wayWriter.closeTags();
		wayWriter.next();

		wayWriter.close();

		byte[] wayBuf = nodeOut.toByteArray();
		byte[] keyBuf = keyOut.toByteArray();
		KeyLookupReader keyReader = new KeyLookupReader(new ByteArrayInputStream(keyBuf));
		ByteArrayInputStream wayIn = new ByteArrayInputStream(wayBuf);
		WayStoreReader wayReader = new WayStoreReader(wayIn, keyReader);

		Assert.assertTrue(wayReader.next());
		Assert.assertEquals(wayReader.getId(), 1);
		int[] nodeIds = wayReader.getNodeIds();
		Assert.assertEquals(nodeIds.length, 3);
		Assert.assertEquals(nodeIds[0], 10);
		Assert.assertEquals(nodeIds[1], 11);
		Assert.assertEquals(nodeIds[2], 12);
		Map<String, String> tags = wayReader.getTags();
		Assert.assertEquals(tags.size(), 3);
		Assert.assertEquals(tags.get("key1"), "value1");
		Assert.assertEquals(tags.get("key2"), "value2");
		Assert.assertEquals(tags.get("key3"), "Euro: \u20AC  Pound: \u00A3");
		Assert.assertNull(tags.get("key4"));

		Assert.assertTrue(wayReader.next());
		Assert.assertEquals(wayReader.getId(), 2);
		nodeIds = wayReader.getNodeIds();
		Assert.assertEquals(nodeIds.length, 0);
		tags = wayReader.getTags();
		Assert.assertEquals(tags.size(), 2);
		Assert.assertNull(tags.get("key1"));
		Assert.assertEquals(tags.get("key2"), "Euro: \u20AC");
		Assert.assertNull(tags.get("key3"));
		Assert.assertEquals(tags.get("key4"), "Pound: \u00A3");

		Assert.assertFalse(wayReader.next());
	}

	@Test
	public void testRelStore() throws IOException {
		ByteArrayOutputStream relOut = new ByteArrayOutputStream();
		ByteArrayOutputStream keyOut = new ByteArrayOutputStream();
		KeyLookupWriter keyWriter = new KeyLookupWriter(keyOut);
		ByteArrayOutputStream roleOut = new ByteArrayOutputStream();
		KeyLookupWriter roleWriter = new KeyLookupWriter(roleOut);

		RelationStoreWriter relWriter = new RelationStoreWriter(relOut, keyWriter, roleWriter);
		relWriter.write(1);
		relWriter.writeMember(MemberType.Node, 5, "testRole1");
		relWriter.writeMember(MemberType.Node, 4, "testRole2");
		relWriter.writeMember(MemberType.Node, 3, "Euro: \u20AC");
		relWriter.writeMember(MemberType.Way, 3, "Pound: \u00A3");
		relWriter.closeMembers();
		relWriter.writeTag("key1", "value1");
		relWriter.writeTag("key2", "value2");
		relWriter.writeTag("key3", "Euro: \u20AC  Pound: \u00A3");
		relWriter.closeTags();
		relWriter.next();
		relWriter.write(2);
		relWriter.closeMembers();
		relWriter.writeTag("key2", "Euro: \u20AC");
		relWriter.writeTag("key4", "Pound: \u00A3");
		relWriter.closeTags();
		relWriter.next();

		relWriter.close();


		byte[] relBuf = relOut.toByteArray();
		byte[] keyBuf = keyOut.toByteArray();
		KeyLookupReader keyReader = new KeyLookupReader(new ByteArrayInputStream(keyBuf));
		byte[] roleBuf = roleOut.toByteArray();
		KeyLookupReader roleReader = new KeyLookupReader(new ByteArrayInputStream(roleBuf));

		ByteArrayInputStream relIn = new ByteArrayInputStream(relBuf);
		RelationStoreReader relReader = new RelationStoreReader(relIn, keyReader, roleReader);

		Assert.assertTrue(relReader.next());
		Assert.assertEquals(relReader.getId(), 1);
		List<Member> members = relReader.getMembers();
		Assert.assertEquals(members.size(), 4);
		Assert.assertEquals(members.get(0).getType(), MemberType.Node);
		Assert.assertEquals(members.get(0).getId(), 5);
		Assert.assertEquals(members.get(0).getRole(), "testRole1");
		Assert.assertEquals(members.get(1).getType(), MemberType.Node);
		Assert.assertEquals(members.get(1).getId(), 4);
		Assert.assertEquals(members.get(1).getRole(), "testRole2");
		Assert.assertEquals(members.get(2).getType(), MemberType.Node);
		Assert.assertEquals(members.get(2).getId(), 3);
		Assert.assertEquals(members.get(2).getRole(), "Euro: \u20AC");
		Assert.assertEquals(members.get(3).getType(), MemberType.Way);
		Assert.assertEquals(members.get(3).getId(), 3);
		Assert.assertEquals(members.get(3).getRole(), "Pound: \u00A3");
		Map<String, String> tags = relReader.getTags();
		Assert.assertEquals(tags.size(), 3);
		Assert.assertEquals(tags.get("key1"), "value1");
		Assert.assertEquals(tags.get("key2"), "value2");
		Assert.assertEquals(tags.get("key3"), "Euro: \u20AC  Pound: \u00A3");
		Assert.assertNull(tags.get("key4"));

		Assert.assertTrue(relReader.next());
		Assert.assertEquals(relReader.getId(), 2);
		members = relReader.getMembers();
		Assert.assertEquals(members.size(), 0);
		tags = relReader.getTags();
		Assert.assertEquals(tags.size(), 2);
		Assert.assertNull(tags.get("key1"));
		Assert.assertEquals(tags.get("key2"), "Euro: \u20AC");
		Assert.assertNull(tags.get("key3"));
		Assert.assertEquals(tags.get("key4"), "Pound: \u00A3");

		Assert.assertFalse(relReader.next());
	}
}