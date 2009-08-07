package uk.me.parabola.splitter;

import org.testng.annotations.Test;
import org.testng.Assert;

/**
 * 
 */
public class TestIntList {
	@Test
	public void testIntList() {
		SplitIntList list = new SplitIntList(10);
		for (int i = 0; i < 100; i++) {
			list.add(i);
		}

		Assert.assertEquals(list.size(), 100);

		for (int i = 0; i < 100; i++) {
			Assert.assertEquals(list.get(i), i);
		}

		SplitIntList.Iterator it = list.getIterator();
		int i = 0;
		while (it.hasNext()) {
			Assert.assertEquals(it.next(), i++);
		}

		it = list.getDeletingIterator();
		i = 0;
		while (it.hasNext()) {
			Assert.assertEquals(it.next(), i++);
		}
	}
}
