package uk.me.parabola.splitter.disk;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 *
 */
public class TestKeyLookups {
	@Test
	public void test() throws IOException {
		ByteArrayOutputStream result = new ByteArrayOutputStream();
		KeyLookupWriter writer = new KeyLookupWriter(result);
		writer.set("Testing1");
		writer.set("Testing2");
		writer.set("Testing3");
		writer.set("Testing4");
		writer.set("Euro: \u20AC  Pound: \u00A3");
		writer.close();

	  byte[] data = result.toByteArray();
		InputStream in = new ByteArrayInputStream(data);
		KeyLookupReader reader = new KeyLookupReader(in);

		Assert.assertEquals(reader.get(0), null);
		Assert.assertEquals(reader.get(1), "Testing1");
		Assert.assertEquals(reader.get(2), "Testing2");
		Assert.assertEquals(reader.get(3), "Testing3");
		Assert.assertEquals(reader.get(4), "Testing4");
		Assert.assertEquals(reader.get(5), "Euro: \u20AC  Pound: \u00A3");
		Assert.assertEquals(reader.get(6), null);
	}
}