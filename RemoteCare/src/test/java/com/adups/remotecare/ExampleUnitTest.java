package com.adups.remotecare;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
	@Test
	public void addition_isCorrect() throws Exception {
		assertEquals(4, 2 + 2);
		String str = new String(new byte[]{-112, -42, -17, 96, -112, -42, -17, 96}, "GBK");
		System.out.println(str);
	}
}