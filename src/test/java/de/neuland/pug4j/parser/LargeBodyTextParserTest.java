package de.neuland.pug4j.parser;


import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.FileNotFoundException;
import java.net.URISyntaxException;

import org.junit.Test;

import de.neuland.pug4j.parser.node.Node;
import de.neuland.pug4j.parser.node.TagNode;

public class LargeBodyTextParserTest extends ParserTest {

    private Node block;
    private TagNode pTag;
    

    @Test
    public void test() throws FileNotFoundException, URISyntaxException {
        loadInParser("large_body_text_with_pipes.jade");
        pTag = (TagNode) root.pollNode();
        block = pTag.getBlock();
        assertThat(block.getNodes(), notNullValue());
        assertThat(pTag, notNullValue());
        
        block = pTag.getBlock();
        assertThat(block.getNodes(), notNullValue());
        
        assertThat(block.pollNode().getValue(), equalTo("Hello World!"));
        assertThat(block.pollNode().getValue(), equalTo("\n"));
        assertThat(block.pollNode().getValue(), equalTo(" Here comes the Message!"));
        
        assertThat(block.hasNodes(), equalTo(false));
    }

//    private void loadInParser(String fileName) {
//        try {
//        	FileReader reader =  new FileReader(fileName);
//			parser = new Parser(reader);
//	        root = (BlockNode) parser.parse();
//		} catch (FileNotFoundException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//    }

}
