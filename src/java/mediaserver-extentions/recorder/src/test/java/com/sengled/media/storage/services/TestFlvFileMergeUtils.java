package com.sengled.media.storage.services;


import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;

import com.google.common.io.Files;

import junit.framework.TestCase;

public class TestFlvFileMergeUtils extends TestCase {
	File abcFile = new File("/tmp", "abc_FS_1522726918395_FS_1522726979060_FS_24_FS_Asia$Shanghai_FS_12_FS_1.flv");
	File efghFile = new File("/tmp", "efgh_FS_1522726918395_FS_1522726979060_FS_24_FS_Asia$Shanghai_FS_12_FS_2.flv");
	File iFile = new File("/tmp", "i_FS_1522726918395_FS_1522726979060_FS_24_FS_Asia$Shanghai_FS_12_FS_3.flv");
	

	File dstFile = new File("/tmp/out");
	
	@Override
	protected void setUp() throws Exception {
		clean();
	}

	@Override
	protected void tearDown() throws Exception {
		clean();
	}

	private void clean() {
		abcFile.delete();
		efghFile.delete();
		iFile.delete();
		dstFile.delete();
	}
	
	public void testMerge() throws IOException {
		Files.write("abc", abcFile, Charset.defaultCharset());
		Files.write("efgh", efghFile, Charset.defaultCharset());
		Files.write("i", iFile, Charset.defaultCharset());
		
		
		
		FlvFileMergeUtils.mergeFlvFiles(Arrays.asList(abcFile, efghFile, iFile), dstFile);
		assertEquals("abcefghi", Files.readFirstLine(dstFile, Charset.defaultCharset()));
	}
}
