/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sf.janalyzers.igoanalyzer;

import net.sf.janalyzers.igoanalyzer.IgoAnalyzer;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.HashMap;
import java.util.HashSet;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.util.Version;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author hideaki
 */
public class IgoAnalyzerTest {

    public IgoAnalyzerTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of tokenStream method, of class IgoAnalyzer.
     */
    @Test
    public void testTokenStream() {
        System.out.println("tokenStream");
        String fieldName = "";
        Reader reader = new StringReader("これはテストの文字列です。解析が出来たら成功です。おわり  ");
        try {
            IgoAnalyzer instance = new IgoAnalyzer(Version.LUCENE_29,
                    "c:\\users\\hideaki\\dev\\ipadic", new HashSet<String>());
            TokenStream expResult = null;
            TokenStream result = instance.tokenStream(fieldName, reader);
            System.out.println(result);
            assertEquals(expResult, result);
        } catch (IOException ioe) {
            ioe.printStackTrace();
            fail();
        }
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of reusableTokenStream method, of class IgoAnalyzer.
     */
    @Test
    public void testReusableTokenStream() throws Exception {
        System.out.println("reusableTokenStream");
        String fieldName = "";
        Reader reader = new StringReader("これはテストの文字列です。解析が出来たら成功です。おわり");
        try {
            IgoAnalyzer instance = new IgoAnalyzer(Version.LUCENE_29,
                    "c:\\users\\hideaki\\dev\\ipadic", new HashSet<String>());
            TokenStream expResult = null;
            TokenStream result = instance.reusableTokenStream(fieldName, reader);
            while (result.incrementToken()) {
                System.out.println(result);
            }
            assertEquals(expResult, result);
        } catch (IOException ioe) {
            ioe.printStackTrace();
            fail();
        }
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
}
