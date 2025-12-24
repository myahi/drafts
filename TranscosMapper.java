package com.mycompany.transco.service;

import com.mycompany.transco.model.*;
import java.util.*;

public final class TranscosMapper {

    private TranscosMapper() {}

    public static Transcos fromRows(List<Map<String, Object>> rows) {
        if (rows == null || rows.isEmpty()) {
            // tu peux choisir: retourner null, ou un Transcos vide
            return new Transcos("", "");
        }

        String src = String.valueOf(rows.get(0).get("SRC"));
        String tgt = String.valueOf(rows.get(0).get("TGT"));

        Transcos root = new Transcos(src, tgt);

        for (Map<String, Object> r : rows) {
            String category = str(r.get("CATEGORY"));
            String srcVl = str(r.get("SRC_VL"));
            String tgtVl = str(r.get("TGT_VL"));

            Transco t = new Transco(category);
            t.getInput().add(new Input(src, srcVl));
            t.getOutput().add(new Output(tgt, tgtVl));

            root.getTransco().add(t);
        }
        return root;
    }

    private static String str(Object o) {
        return o == null ? null : String.valueOf(o);
    }
}
