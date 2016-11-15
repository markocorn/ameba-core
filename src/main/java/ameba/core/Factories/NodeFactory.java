package ameba.core.Factories;

import ameba.core.blocks.Node;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

/**
 * Created by marko on 10/20/16.
 */
public class NodeFactory {
    /**
     * Map Node object to josn string.
     * @param node
     * @return
     */
    public static String toString(Node node) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();

        return mapper.writeValueAsString(node);
    }

    /**
     * Parse Node object from json type string.
     * @param jsonString Object as json string.
     * @return
     * @throws IOException
     */
    public static Node parseNode(String jsonString) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(jsonString,Node.class);
    }
}
