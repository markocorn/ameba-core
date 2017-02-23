package ameba.core.blocks.nodes;

/**
 * Created by marko on 1/10/17.
 */
public interface INode {

    void processNode() throws Exception;

    void clearNode();

    void rstNode();

    void clcNode() throws Exception;
}