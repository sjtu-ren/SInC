package sinc2.exp.hint.predefined;

import sinc2.kb.KbRelation;
import sinc2.kb.NumeratedKb;
import sinc2.kb.Record;
import sinc2.util.LittleEndianIntIO;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * This KB class simply loads the numerated relations and records the entailed records. The ith elements of the arrays
 * are the ith relation and its name and covered records.
 *
 * @since 2.0
 */
public class SimpleKb {
    public final List<Set<Record>> relations = new ArrayList<>();
    public final List<String> relationNames = new ArrayList<>();
    public final List<Set<Record>> relationEntailments = new ArrayList<>();

    public SimpleKb(String kbName, String kbPath) {
        File kb_dir = NumeratedKb.getKbPath(kbName, kbPath).toFile();
        File[] files = kb_dir.listFiles();
        if (null != files) {
            for (File f: files) {
                KbRelation.RelationInfo rel_info = KbRelation.parseRelFilePath(f.getName());
                if (null != rel_info) {
                    Set<Record> relation = new HashSet<>();
                    try (FileInputStream fis = new FileInputStream(f)) {
                        byte[] buffer = new byte[Integer.BYTES];
                        int read_args;
                        while (true) {
                            int[] args = new int[rel_info.arity];
                            for (read_args = 0; read_args < rel_info.arity && Integer.BYTES == fis.read(buffer); read_args++) {
                                args[read_args] = LittleEndianIntIO.byteArray2LeInt(buffer);
                            }
                            if (read_args < rel_info.arity) {
                                break;
                            }
                            relation.add(new Record(args));
                        }
                    } catch (IOException e) {
                        System.err.println("Relation file load failed: " + f.getAbsolutePath());
                        e.printStackTrace();
                        continue;
                    }
                    relations.add(relation);
                    relationNames.add(rel_info.name);
                    relationEntailments.add(new HashSet<>());
                }
            }
        }
    }
}
