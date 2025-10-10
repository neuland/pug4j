package de.neuland.pug4j.parser.node;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import de.neuland.pug4j.PugConfiguration;
import de.neuland.pug4j.compiler.NodeVisitor;
import org.apache.commons.collections4.IteratorUtils;

import de.neuland.pug4j.compiler.IndentWriter;
import de.neuland.pug4j.exceptions.ExpressionException;
import de.neuland.pug4j.exceptions.PugCompilerException;
import de.neuland.pug4j.model.PugModel;

public class EachNode extends Node {

    private String key;
    private String code;
    private Node elseNode;

    @SuppressWarnings("unchecked")
    public void run(IndentWriter writer, PugModel model, Object result, PugConfiguration configuration, final Consumer<Node> nodeConsumer) {
        if (result instanceof Iterable<?>) {
            runIterator(((Iterable<?>) result).iterator(), model, writer, configuration, nodeConsumer);
        } else if (result.getClass().isArray()) {
            Iterator<?> iterator = IteratorUtils.arrayIterator(result);
            runIterator(iterator, model, writer, configuration, nodeConsumer);
        } else if (result instanceof Map) {
            runMap((Map<Object, Object>) result, model, writer, configuration, nodeConsumer);
        }
    }

    private void runIterator(Iterator<?> iterator, PugModel model, IndentWriter writer, PugConfiguration configuration, final Consumer<Node> nodeConsumer) {
        int index = 0;

        if (!iterator.hasNext()) {
            executeElseNode(model, writer, configuration, nodeConsumer);
            return;
        }

        while (iterator.hasNext()) {
            model.putLocal(getValue(), iterator.next());
            model.putLocal(getKey(), index);
            nodeConsumer.accept(getBlock());
            index++;
        }
    }

    private void runMap(Map<Object, Object> result, PugModel model, IndentWriter writer, PugConfiguration configuration, final Consumer<Node> nodeConsumer) {
        Set<Object> keys = result.keySet();
        if (keys.isEmpty()) {
            executeElseNode(model, writer, configuration, nodeConsumer);
            return;
        }
        for (Object key : keys) {
            model.putLocal(getValue(), result.get(key));
            model.putLocal(getKey(), key);
            nodeConsumer.accept(getBlock());
        }
    }

    private void executeElseNode(PugModel model, IndentWriter writer, PugConfiguration configuration, final Consumer<Node> nodeConsumer) {
        if (elseNode != null) {
            nodeConsumer.accept(elseNode);
        }
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getKey() {
        return key == null ? "$index" : key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public void setElseNode(Node elseNode) {
        this.elseNode = elseNode;
    }

    @Override
    public void accept(NodeVisitor visitor, IndentWriter writer, PugModel model) {
        visitor.visit(this, writer, model);
    }
}
