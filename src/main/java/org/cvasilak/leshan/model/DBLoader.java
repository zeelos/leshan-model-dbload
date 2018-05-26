package org.cvasilak.leshan.model;

import org.apache.tinkerpop.gremlin.orientdb.OrientGraphFactory;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.T;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.eclipse.leshan.core.model.ObjectLoader;
import org.eclipse.leshan.core.model.ObjectModel;
import org.eclipse.leshan.core.model.ResourceModel;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.Optional;

public class DBLoader {

    public static void main(String[] args) throws Exception {
        int resourceCount = 0; // count resources loaded to print statistic upon finish

        // load OMA standard object models
        List<ObjectModel> models = ObjectLoader.loadDefault();

        if (args.length > 0) { // user supplied directory of models
            models.addAll(ObjectLoader.loadObjectsFromDir(new File(args[0])));
        } else { // load all other models found in 'resources/' directory
            URL modelsFolderPath = DBLoader.class.getResource("/models");
            if (modelsFolderPath != null) {
                models.addAll(ObjectLoader.loadObjectsFromDir(new File(modelsFolderPath.getFile())));
            }
        }

        Graph g = new OrientGraphFactory("remote:localhost/zeelosdb", "root", "secret").getNoTx();

        for (ObjectModel model : models) {
            System.out.println(String.format("processing object '%s'", model.name));

            Vertex object = g.addVertex(T.label, "Objects",
                    "id", model.id, "name", model.name, "description",
                    Optional.ofNullable(model.description).orElse(""), // care for field missing from source 'xml'
                    "multiple", model.multiple, "mandatory", model.mandatory);

            for (ResourceModel res : model.resources.values()) {
                System.out.println(String.format("\tprocessing resource '%s'", res.name));
                Vertex resource = g.addVertex(T.label, "Resources",
                        "id", res.id, "name", res.name,
                        "description", Optional.ofNullable(res.description).orElse(""),
                        "multiple", res.multiple,
                        "mandatory", res.mandatory,
                        "operations", res.operations, "type", res.type,
                        "range", Optional.ofNullable(res.rangeEnumeration).orElse(""),
                        "units", Optional.ofNullable(res.units).orElse(""));

                object.addEdge("hasResource", resource);
                resourceCount++;
            }
        }

        System.out.println(String.format("\ninserted '%d objects', '%d resources'", models.size(), resourceCount));

        // close and exit
        g.close(); System.exit(0);
    }
}
