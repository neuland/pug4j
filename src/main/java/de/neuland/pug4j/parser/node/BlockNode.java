package de.neuland.pug4j.parser.node;

import de.neuland.pug4j.parser.Parser;

public class BlockNode extends Node {

	private boolean yield = false;
	private String mode;
	private Parser parser;
	private boolean namedBlock;

	public void setYield(boolean yield) {
		this.yield = yield;
	}

	public boolean isYield() {
		return yield;
	}

	public BlockNode getYieldBlock() {
		BlockNode ret = this;
		for (Node node : getNodes()) {
			if (node instanceof BlockNode && ((BlockNode) node).isYield()) {
				return (BlockNode) node;
			}
			else if (node instanceof TagNode && ((TagNode) node).isTextOnly()) {
				continue;
			}
			else if (node instanceof BlockNode && ((BlockNode) node).getYieldBlock() != null) {
				ret =  ((BlockNode) node).getYieldBlock();
			}
			else if (node.hasBlock()) {
				ret =  ((BlockNode) node.getBlock()).getYieldBlock();
			}
			if(ret instanceof BlockNode && ((BlockNode) ret).isYield()){
				return ret;
			}
		}
		return ret;
	}

	public String getMode() {
		return mode;
	}

	public void setMode(String mode) {
		this.mode = mode;
	}

	public void setParser(Parser parser) {
		this.parser = parser;
	}

	public Parser getParser() {
		return parser;
	}

	public boolean isNamedBlock() {
		return namedBlock;
	}

	public void setNamedBlock(boolean namedBlock) {
		this.namedBlock = namedBlock;
	}
}
