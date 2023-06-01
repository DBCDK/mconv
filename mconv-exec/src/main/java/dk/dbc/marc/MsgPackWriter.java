package dk.dbc.marc;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dk.dbc.marc.binding.ControlField;
import dk.dbc.marc.binding.DataField;
import dk.dbc.marc.binding.Field;
import dk.dbc.marc.binding.Leader;
import dk.dbc.marc.binding.MarcRecord;
import dk.dbc.marc.binding.SubField;
import dk.dbc.marc.writer.MarcWriter;
import dk.dbc.marc.writer.MarcWriterException;
import org.msgpack.jackson.dataformat.MessagePackMapper;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;


/**
 * MarcWriter implementation for transforming MarcRecord instances into MarcJson.
 * <p>
 * This class is thread safe.
 */
public class MsgPackWriter implements MarcWriter {
    ObjectMapper objectMapper = new MessagePackMapper();

    @Override
    public boolean canOutputCollection() {
        return true;
    }

    @Override
    public byte[] write(MarcRecord marcRecord, Charset encoding) throws MarcWriterException {
        if (marcRecord == null) {
            return new byte[0];
        }
        if (encoding == null) {
            throw new IllegalArgumentException("encoding argument can not be null");
        }
        try {
            final dk.dbc.marc.MsgPackWriter.RecordNode recordNode = fromMarcRecord(marcRecord);
            return objectMapper.writeValueAsBytes(recordNode);
        } catch (RuntimeException | JsonProcessingException e) {
            throw new MarcWriterException("Error while marshalling MarcRecord", e);
        }
    }

    @Override
    public byte[] writeCollection(Collection<MarcRecord> marcRecords, Charset encoding) throws MarcWriterException {
        if (marcRecords == null) {
            return new byte[0];
        }
        if (encoding == null) {
            throw new IllegalArgumentException("encoding argument can not be null");
        }
        try {
            final ArrayNode arrayNode = objectMapper.createArrayNode();
            marcRecords.stream()
                    .filter(Objects::nonNull)
                    .forEach(marcRecord -> arrayNode.add(fromMarcRecord(marcRecord)));
            return objectMapper.writeValueAsBytes(arrayNode);
        } catch (RuntimeException | JsonProcessingException e) {
            throw new MarcWriterException("Error while marshalling MarcRecord", e);
        }
    }

    private RecordNode fromMarcRecord(MarcRecord marcRecord) {
        final dk.dbc.marc.MsgPackWriter.RecordNode recordNode = new dk.dbc.marc.MsgPackWriter.RecordNode(objectMapper);
        recordNode.setFormat(marcRecord.getFormat());
        recordNode.setType(marcRecord.getType());
        recordNode.setLeader(marcRecord.getLeader());
        recordNode.setFields(marcRecord.getFields());
        return recordNode;
    }

    /**
     * Handles serializing of objects of type {@link MarcRecord} into MarcJson
     */
    public static class MarcRecordSerializer extends JsonSerializer<MarcRecord> {
        private static final dk.dbc.marc.MsgPackWriter JSON_WRITER = new dk.dbc.marc.MsgPackWriter();

        @Override
        public void serialize(MarcRecord marcRecord, JsonGenerator jsonGenerator, SerializerProvider serializerProvider)
                throws IOException {

            jsonGenerator.writeTree(JSON_WRITER.fromMarcRecord(marcRecord));
        }
    }

    private static class RecordNode extends ObjectNode {
        final ObjectMapper objectMapper;

        RecordNode(ObjectMapper _objectMapper ) {
            super(_objectMapper.getNodeFactory());
            objectMapper = _objectMapper;
        }

        void setFormat(String format) {
            if (format != null) {
                this.put("format", format);
            }
        }

        void setType(String type) {
            if (type != null) {
                this.put("type", type);
            }
        }

        void setLeader(Leader leader) {
            if (leader != null) {
                final String leaderData = leader.getData();
                if (leaderData != null) {
                    final ArrayNode leaderArray = this.putArray("leader");
                    Arrays.stream(leaderData.split("")).forEach(leaderArray::add);
                }
            }
        }

        void setFields(List<Field> fields) {
            final ArrayNode fieldsNode = this.putArray("fields");
            for (Field field : fields) {
                if (field instanceof DataField) {
                    setDataField(fieldsNode, (DataField) field);
                } else {
                    setControlField(fieldsNode, (ControlField) field);
                }
            }
        }

        private void setDataField(ArrayNode fieldsNode, DataField dataField) {
            // instanceof check in setFields makes null check unnecessary here
            final ObjectNode dataFieldNode = objectMapper.createObjectNode();
            dataFieldNode.put("name", dataField.getTag());
            setIndicator(Arrays.asList(dataField.getInd1(), dataField.getInd2(), dataField.getInd3()), dataFieldNode);
            setSubfields(dataField.getSubFields(), dataFieldNode);
            fieldsNode.add(dataFieldNode);
        }

        private void setIndicator(List<Character> indicators, ObjectNode dataFieldNode) {
            final ArrayNode indicatorNode = dataFieldNode.putArray("indicator");
            for (Character ind : indicators) {
                if (ind == null) {
                    break;
                }
                indicatorNode.add(ind.toString());
            }
        }

        private void setSubfields(List<SubField> subfields, ObjectNode dataFieldNode) {
            final ArrayNode subfieldsNode = dataFieldNode.putArray("subfields");
            for (SubField subfield : subfields) {
                final ObjectNode subfieldNode = objectMapper.createObjectNode();
                subfieldNode.put("name", Character.toString(subfield.getCode()));
                subfieldNode.put("value", subfield.getData());
                subfieldsNode.add(subfieldNode);
            }
        }

        private void setControlField(ArrayNode fieldsNode, ControlField controlField) {
            if (controlField != null) {
                final ObjectNode controlFieldNode = objectMapper.createObjectNode();
                controlFieldNode.put("name", controlField.getTag());
                controlFieldNode.put("value", controlField.getData());
                fieldsNode.add(controlFieldNode);
            }
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            if (!super.equals(o)) return false;
            dk.dbc.marc.MsgPackWriter.RecordNode jsonNodes = (dk.dbc.marc.MsgPackWriter.RecordNode) o;
            return Objects.equals(objectMapper, jsonNodes.objectMapper);
        }

        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(), objectMapper);
        }
    }
}
