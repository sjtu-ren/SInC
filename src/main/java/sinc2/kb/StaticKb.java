package sinc2.kb;

import java.io.File;
import java.io.IOException;

/**
 * This class is for a static unmodifiable KB, which requires less memory resources.
 *
 * @since 2.0
 */
public class StaticKb extends NumeratedKb {

    public StaticKb(String name, String basePath) throws KbException, IOException {
        super(name);
        File kb_dir = NumeratedKb.getKbPath(name, basePath).toFile();
        String kb_dir_path = kb_dir.getAbsolutePath();
        File[] files = kb_dir.listFiles();
        if (null != files) {
            for (File f: files) {
                KbRelation.RelationInfo rel_info = KbRelation.parseRelFilePath(f.getName());
                if (null != rel_info) {
                    KbRelation relation = new KbRelation(
                            rel_info.name, numMap.mapName(rel_info.name), rel_info.arity, rel_info.totalRecords,
                            kb_dir_path, null
                    );
                    relationMap.put(relation.getNumeration(), relation);
                }
            }
        }
    }

    @Override
    public void dump(String basePath) throws IOException {
        throw new Error("Not supported");
    }

    @Override
    public KbRelation createRelation(String relName, int arity) throws KbException {
        throw new Error("Not supported");
    }

    @Override
    public KbRelation loadRelation(String relBasePath, String relName, int arity, int totalRecords, boolean check) throws KbException, IOException {
        throw new Error("Not supported");
    }

    @Override
    protected KbRelation deleteRelationHandler(int relNum) {
        throw new Error("Not supported");
    }

    @Override
    protected void addRecordHandler(KbRelation relation, Record record) throws KbException {
        throw new Error("Not supported");
    }

    @Override
    protected void removeRecordHandler(KbRelation relation, Record record) {
        throw new Error("Not supported");
    }

    @Override
    public boolean hasRecord(int relNum, String[] argNames) {
        throw new Error("Not supported");
    }

    @Override
    public boolean hasRecord(String relName, String[] argNames) {
        throw new Error("Not supported");
    }

    @Override
    public int mapName(String name) {
        throw new Error("Not supported");
    }

    @Override
    public int unmapName(String name) {
        throw new Error("Not supported");
    }

    @Override
    public String unmapNumeration(int num) {
        throw new Error("Not supported");
    }

    @Override
    public int totalMappings() {
        throw new Error("Not supported");
    }

    @Override
    public void setAsEntailed(int relNum, int[] record) throws KbException {
        throw new Error("Not supported");
    }

    @Override
    public void updatePromisingConstants() {
        throw new Error("Not supported");
    }
}
