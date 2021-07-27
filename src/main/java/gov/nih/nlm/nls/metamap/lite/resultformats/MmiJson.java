//
package gov.nih.nlm.nls.metamap.lite.resultformats;

import gov.nih.nlm.nls.metamap.lite.metamap.MetaMapIvfIndexes;
import gov.nih.nlm.nls.metamap.lite.types.*;
import gov.nih.nlm.nls.metamap.mmi.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.NumberFormat;
import java.util.*;


/**
 * Fielded MetaMap Indexing (MMI) Output
 * <p>
 * Sample record (pipe separated):
 * <pre>
 * id|MMI|score|preferredname|cui|semtypelist|triggerinfo|location|posinfo|treecodes
 * </pre>
 * Triggerinfo (dash separated):  (ex:  "UMLS concept-loc-locPos-text-Part of Speech-Negation Flag". )
 * <p>
 * UMLS Concept (Preferred or Synonym Text)
 * <ul>
 *  <li>loc - Location in the text if identifiable. ti - Title, ab - Abstract, and tx - Free Text
 *  <li>locPos - Number of the utterance within the loc starting with one (1). For example, "ti-1" denotes first utterance in title.
 *  <li>text - The actual text mapped to this UMLS concept identification.
 *  <li>Part of Speech - N/A
 *  <li>Negation Flag
 * </ul>
 */

public class MmiJson implements ResultFormatter {

    NumberFormat scoreFormat = NumberFormat.getInstance();

    public MmiJson() {
        scoreFormat.setMaximumFractionDigits(2);
        scoreFormat.setMinimumFractionDigits(2);
        scoreFormat.setGroupingUsed(false);
    }

    public static JSONObject renderPosition(Ev ev) {
        JSONObject jsonObj = new JSONObject();
        jsonObj.put("start", ev.getStart());
        jsonObj.put("length", ev.getLength());
        return jsonObj;
    }

    /**
     * render tuple without positional information.
     *
     * @param tuple seven item Tuple.
     * @return tuple string representation with positional information
     */
    public JSONObject renderTupleInfo(Tuple tuple) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("term", tuple.getTerm());
        jsonObject.put("field", tuple.getField());
        jsonObject.put("nsent", tuple.getNSent());
        jsonObject.put("text", tuple.getText());
        jsonObject.put("lexcat", tuple.getLexCat());
        jsonObject.put("neg", tuple.getNeg());
        return jsonObject;
    }

    /**
     * render positional information from tuple to string.
     *
     * @param tuple seven item Tuple.
     * @return positional information in formatted string form.
     */
    public JSONArray renderPositionInfo(Tuple tuple) {
        JSONArray jsonArray = new JSONArray();
        for (Position i : tuple.getPosInfo()) {
            JSONObject jsonObj = new JSONObject();
            jsonObj.put("start", i.getStart());
            jsonObj.put("length", i.getLength());
            jsonArray.put(jsonObj);
        }
        return jsonArray;
    }

    /**
     * @param pw         printwriter used for output
     * @param docid      document identifier
     * @param entityList entitylist to be rendered for output
     */
    public JSONArray renderEntityList(PrintWriter pw, String docid, List<Entity> entityList) {
        List<TermFrequency> tfList = this.entityToTermFrequencyInfo(entityList);
        List<AATF> aatfList = Ranking.processTF(tfList, 1000);
        Collections.sort(aatfList);
        JSONArray jsonArray = new JSONArray();
        for (AATF aatf : aatfList) {
            JSONArray result = new JSONArray();
            for (Tuple tuple : aatf.getTuplelist()) {
                String field = tuple.getField();
                result.put(field);
            }
            JSONObject jsonObj = new JSONObject();
            jsonObj.put("docid", docid);
            jsonObj.put("concept", aatf.getConcept());
            jsonObj.put("cui", aatf.getCui());
            jsonObj.put("score", scoreFormat.format(-10000 * aatf.getNegNRank()));
            jsonObj.put("semantictypes", aatf.getSemanticTypes());
            jsonObj.put("tupleinfo", aatf.getTuplelist().stream().map(this::renderTupleInfo).toArray());
            jsonObj.put("fieldset", result);
            jsonObj.put("positioninfo", aatf.getTuplelist().stream().map(this::renderPositionInfo).toArray());
            jsonObj.put("treecodes", aatf.getTreeCodes());
            jsonArray.put(jsonObj);
        }
        return jsonArray;
    }

    /**
     * map entities by document id.
     *
     * @param entityList input entitylist
     * @return map of entities key by document id.
     */
    public static Map<String, List<Entity>> genDocidEntityMap(List<Entity> entityList) {
        Map<String, List<Entity>> docidEntityMap = new HashMap<String, List<Entity>>();
        for (Entity entity : entityList) {
            if (docidEntityMap.containsKey(entity.getDocid())) {
                docidEntityMap.get(entity.getDocid()).add(entity);
            } else {
                List<Entity> newEntityList = new ArrayList<Entity>();
                newEntityList.add(entity);
                docidEntityMap.put(entity.getDocid(), newEntityList);
            }
        }
        return docidEntityMap;
    }


    /**
     * @param writer     printwriter used for output
     * @param entityList entitylist to be rendered for output
     */
    public void entityListFormatter(PrintWriter writer,
                                    List<Entity> entityList) {

        JSONArray entityArray = new JSONArray();
        for (Map.Entry<String, List<Entity>> entry : genDocidEntityMap(entityList).entrySet()) {
            entityArray.put(this.renderEntityList(writer, entry.getKey(), entry.getValue()));
        }
        writer.println(entityArray.toString());
    }

    /**
     * @param sb         string builder used for output
     * @param docid      document identifier
     * @param entityList entitylist to be rendered for output
     */
    public JSONArray renderEntityList(StringBuilder sb, String docid, List<Entity> entityList) {
        List<TermFrequency> tfList = this.entityToTermFrequencyInfo(entityList);
        List<AATF> aatfList = Ranking.processTF(tfList, 1000);
        Collections.sort(aatfList);
        JSONArray jsonArray = new JSONArray();
        for (AATF aatf : aatfList) {
            JSONArray result = new JSONArray();
            for (Tuple tuple : aatf.getTuplelist()) {
                String field = tuple.getField();
                result.put(field);
            }
            JSONObject jsonObj = new JSONObject();
            jsonObj.put("docid", docid);
            jsonObj.put("concept", aatf.getConcept());
            jsonObj.put("cui", aatf.getCui());
            jsonObj.put("score", scoreFormat.format(-10000 * aatf.getNegNRank()));
            jsonObj.put("semantictypes", aatf.getSemanticTypes());
            jsonObj.put("tupleinfo", aatf.getTuplelist().stream().map(this::renderTupleInfo).toArray());
            jsonObj.put("fieldset", result);
            jsonObj.put("positioninfo", aatf.getTuplelist().stream().map(this::renderPositionInfo).toArray());
            jsonObj.put("treecodes", aatf.getTreeCodes());
            jsonArray.put(jsonObj);
        }
        return jsonArray;
    }

    public String entityListFormatToString(List<Entity> entityList) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, List<Entity>> entry : genDocidEntityMap(entityList).entrySet()) {
            renderEntityList(sb, entry.getKey(), entry.getValue());
        }
        return sb.toString();
    }

    public List<TermFrequency> entityToTermFrequencyInfo(List<Entity> entityList) {
        Map<String, TermFrequency> termFreqMap = new HashMap<String, TermFrequency>();
        for (Entity entity : entityList) {
            for (Ev ev : entity.getEvList()) {
                String cui = ev.getConceptInfo().getCUI();
                String conceptString = ev.getConceptInfo().getConceptString();
                String tfKey = cui;
                if (termFreqMap.containsKey(tfKey)) {
                    TermFrequency tf = termFreqMap.get(tfKey);
                    List<Position> posInfo = new ArrayList<Position>();
                    int start = ev.getStart();
                    int end = ev.getStart() + ev.getLength();
                    posInfo.add(new PositionImpl(start, end));
                    Tuple tuple = new Tuple7(ev.getConceptInfo().getConceptString(),
                            entity.getFieldId() == null ? "text" : entity.getFieldId(), // section/location field needs to be added to Entity or Ev (or both)
                            entity.getSentenceNumber(), // sentence number needs to be added to Entity or Ev (or both)
                            ev.getMatchedText(), // text?
                            entity.getLexicalCategory(), // lexical category needs to be added to Entity or Ev (or both)
                            entity.isNegated() ? 1 : 0, // neg?
                            posInfo);
                    tf.getTupleSet().add(tuple);
                    tf.setFrequencyCount(tf.getFrequencyCount() + 1);
                } else {
                    List<Position> posInfo = new ArrayList<Position>();
                    int start = ev.getStart();
                    int end = ev.getStart() + ev.getLength();
                    posInfo.add(new PositionImpl(start, end));
                    Set<Tuple> tupleSet = new LinkedHashSet<Tuple>();
                    Tuple tuple = new Tuple7(ev.getConceptInfo().getConceptString(),
                            entity.getFieldId() == null ? "text" : entity.getFieldId(), // section/location field needs to be added to Entity or Ev (or both)
                            entity.getSentenceNumber(), // sentence number needs to be added to Entity or Ev (or both)
                            ev.getMatchedText(), // text?
                            entity.getLexicalCategory(), // lexical category needs to be added to Entity or Ev (or both)
                            entity.isNegated() ? 1 : 0, // neg?
                            posInfo);
                    tupleSet.add(tuple);
                    String preferredName = ev.getConceptInfo().getPreferredName();
                    termFreqMap.put(tfKey,
                            new TermFrequency(preferredName,
                                    new ArrayList<String>(ev.getConceptInfo().getSemanticTypeSet()),
                                    tupleSet,
                                    entity.getFieldId() == null ? false :
                                            (entity.getFieldId().equals("title") ||
                                                    entity.getFieldId().equals("TI"))
                                    ,
                                    cui,
                                    1,
                                    ev.getScore(),
                                    getTreecodes(preferredName)));
                }
            }
        }
        return new ArrayList<TermFrequency>(termFreqMap.values());
    }

    public MetaMapIvfIndexes mmIndexes;

    public List<String> getTreecodes(String term) {
        try {
            List<String> treecodeList = new ArrayList<String>();
            for (String hit : this.mmIndexes.meshTcRelaxedIndex.lookup(term, 0)) {
                String[] fields = hit.split("\\|");
                treecodeList.add(fields[1]);
            }
            return treecodeList;
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    public void initProperties(Properties properties) {
        try {
            this.mmIndexes = new MetaMapIvfIndexes(properties);
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }
}
